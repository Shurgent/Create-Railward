package ua.pp.shurgent.railward.block.train.parts;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import com.simibubi.create.content.trains.entity.Train;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;
import ua.pp.shurgent.railward.RailwardPackets;
import ua.pp.shurgent.railward.network.DieselExhaustParticlePacket;
import ua.pp.shurgent.railward.particle.DieselExhaustParticleData.Profile;

/** Server-authoritative train state classifier and particle emitter. */
public final class DieselExhaustEmitter {

	private static final double STOPPED_EPSILON = 1.0E-3;
	private static final double SPEED_MATCH_EPSILON = .015;
	private static final int BURST_SIZE = 3;
	private static final int BURST_GAP = 10;
	private static final int BURST_PERIOD = BURST_SIZE + BURST_GAP;
	private static final int STARTUP_SECOND_BURST = BURST_PERIOD;
	private static final int STARTUP_FULL_BEGIN = STARTUP_SECOND_BURST + BURST_SIZE;
	private static final int STARTUP_END = STARTUP_FULL_BEGIN + 20;
	private static final Map<UUID, EmissionState> STATES = new HashMap<>();

	private DieselExhaustEmitter() {
	}

	public static void tick(Train train, Level level) {
		if (level.isClientSide || train == null)
			return;

		EmissionState state = STATES.computeIfAbsent(train.id, id -> new EmissionState());
		if (state.lastGameTime == level.getGameTime())
			return;
		state.lastGameTime = level.getGameTime();

		boolean active = isManuallyControlled(train) || TrainDirectionState.hasScheduledControls(train);
		if (!active) {
			state.reset();
			return;
		}

		if (!state.active) {
			state.active = true;
			state.startupTick = 0;
			state.cooldown = 0;
		}

		if (state.startupTick < STARTUP_END) {
			tickStartup(train, state);
			state.startupTick++;
			return;
		}

		RunningMode mode = classify(train);
		if (mode != state.mode) {
			state.mode = mode;
			state.modeTick = 0;
			state.cooldown = 0;
		}

		if (mode == RunningMode.IDLE) {
			if (state.modeTick % BURST_PERIOD < BURST_SIZE)
				emit(train, Profile.IDLE);
		} else if (state.cooldown-- <= 0) {
			emit(train, mode == RunningMode.FULL ? Profile.FULL : Profile.NORMAL);
			state.cooldown = randomInterval(train) - 1;
		}
		state.modeTick++;
	}

	public static void remove(UUID trainId) {
		STATES.remove(trainId);
	}

	public static void clear() {
		STATES.clear();
	}

	private static void tickStartup(Train train, EmissionState state) {
		int tick = state.startupTick;
		if (tick < BURST_SIZE
			|| tick >= STARTUP_SECOND_BURST && tick < STARTUP_SECOND_BURST + BURST_SIZE) {
			emit(train, Profile.STARTUP_LIGHT);
			return;
		}
		if (tick >= STARTUP_FULL_BEGIN && state.cooldown-- <= 0) {
			emit(train, Profile.FULL);
			state.cooldown = randomInterval(train) - 1;
		}
	}

	private static RunningMode classify(Train train) {
		double speed = Math.abs(train.speed);
		double targetSpeed = Math.abs(train.targetSpeed);
		if (speed <= STOPPED_EPSILON)
			return RunningMode.IDLE;
		double tolerance = Math.max(SPEED_MATCH_EPSILON, targetSpeed * .03);
		return targetSpeed > speed + tolerance ? RunningMode.FULL : RunningMode.NORMAL;
	}

	private static boolean isManuallyControlled(Train train) {
		for (Carriage carriage : train.carriages) {
			CarriageContraptionEntity entity = carriage.anyAvailableEntity();
			if (entity != null && entity.getControllingPlayer().isPresent())
				return true;
		}
		return false;
	}

	private static int randomInterval(Train train) {
		for (Carriage carriage : train.carriages) {
			CarriageContraptionEntity entity = carriage.anyAvailableEntity();
			if (entity != null)
				return entity.level.random.nextInt(3) + 1;
		}
		return 2;
	}

	private static void emit(Train train, Profile profile) {
		for (Carriage carriage : train.carriages)
			carriage.forEachPresentEntity(entity -> emitFromCarriage(entity, profile));
	}

	private static void emitFromCarriage(CarriageContraptionEntity entity, Profile profile) {
		if (!(entity.level instanceof ServerLevel level))
			return;
		Contraption contraption = entity.getContraption();
		if (contraption == null)
			return;

		for (StructureBlockInfo info : contraption.getBlocks().values()) {
			BlockState blockState = info.state;
			if (!(blockState.getBlock() instanceof DieselExhaustBlock exhaust))
				continue;
			for (Vec3 point : exhaust.getEmissionPoints(blockState))
				emitParticle(level, entity, info.pos, point, profile);
		}
	}

	private static void emitParticle(ServerLevel level, CarriageContraptionEntity entity, BlockPos localPos,
		Vec3 modelPoint, Profile profile) {
		Vec3 localEmitter = Vec3.atLowerCornerOf(localPos).add(modelPoint);
		Vec3 nozzleDirection = entity.applyRotation(new Vec3(0, 1, 0), 1).normalize();

		double launchSpeed = switch (profile) {
		case STARTUP_LIGHT -> .052;
		case IDLE -> .058;
		case NORMAL -> .09;
		case FULL -> .155;
		};
		double spread = profile == Profile.FULL ? .038 : profile == Profile.NORMAL ? .026 : .018;
		double verticalVariance = .86 + level.random.nextDouble() * .28;
		Vec3 randomMotion = new Vec3(
			Mth.clamp(level.random.nextGaussian(), -2, 2) * spread,
			level.random.nextDouble() * spread * .45,
			Mth.clamp(level.random.nextGaussian(), -2, 2) * spread);
		Vec3 velocity = nozzleDirection.scale(launchSpeed * verticalVariance).add(randomMotion);

		RailwardPackets.getChannel().send(PacketDistributor.TRACKING_ENTITY.with(() -> entity),
			new DieselExhaustParticlePacket(entity.getId(), localEmitter, profile, velocity));
	}

	private enum RunningMode {
		IDLE,
		NORMAL,
		FULL
	}

	private static class EmissionState {
		private boolean active;
		private int startupTick;
		private RunningMode mode;
		private int modeTick;
		private int cooldown;
		private long lastGameTime = Long.MIN_VALUE;

		private void reset() {
			active = false;
			startupTick = 0;
			mode = null;
			modeTick = 0;
			cooldown = 0;
		}
	}
}
