package ua.pp.shurgent.railward.block.train.parts;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.actors.trainControls.ControlsBlock;
import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.CarriageContraption;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.schedule.ScheduleRuntime.State;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraftforge.network.PacketDistributor;
import ua.pp.shurgent.railward.RailwardPackets;
import ua.pp.shurgent.railward.network.LocomotiveBufferLightsTrainStatePacket;

public class LocomotiveBufferLightsTrainState {

	private static final double SPEED_EPSILON = 1.0E-4;
	private static final String NBT_DIRECTION_KEY = "RailwardBufferLightsDirection";
	private static final int SCHEDULED_RESYNC_INTERVAL = 40;
	private static final Map<UUID, DirectionState> CLIENT_DIRECTIONS = new HashMap<>();
	private static final Map<UUID, DirectionState> SERVER_SCHEDULED_DIRECTIONS = new HashMap<>();
	private static final Set<UUID> SERVER_FORCE_SCHEDULED_SYNC = new HashSet<>();

	private LocomotiveBufferLightsTrainState() {
	}

	public static void syncManualControls(CarriageContraptionEntity entity, BlockPos controlsLocalPos) {
		if (entity.level.isClientSide)
			return;
		Carriage carriage = entity.getCarriage();
		if (carriage == null || carriage.train == null)
			return;
		DirectionState direction = resolveManualDirection(entity, controlsLocalPos);
		RailwardPackets.getChannel().send(PacketDistributor.TRACKING_ENTITY.with(() -> entity),
			new LocomotiveBufferLightsTrainStatePacket(carriage.train.id, direction));
	}

	public static void clearManualControls(CarriageContraptionEntity entity) {
		if (entity.level.isClientSide)
			return;
		Carriage carriage = entity.getCarriage();
		if (carriage == null || carriage.train == null)
			return;
		RailwardPackets.getChannel().send(PacketDistributor.TRACKING_ENTITY.with(() -> entity),
			new LocomotiveBufferLightsTrainStatePacket(carriage.train.id, DirectionState.NONE));
	}

	public static void syncScheduledControls(Train train, Level level) {
		if (level.isClientSide || train == null || isManuallyControlled(train))
			return;

		DirectionState direction = hasScheduledControls(train) ? resolveScheduledDirection(train) : DirectionState.NONE;
		DirectionState previous = SERVER_SCHEDULED_DIRECTIONS.get(train.id);
		boolean forceSync = SERVER_FORCE_SCHEDULED_SYNC.remove(train.id);
		boolean periodicSync = direction != DirectionState.NONE && shouldResyncScheduled(train, level);
		if (direction == DirectionState.NONE) {
			if (previous == null && !forceSync)
				return;
			SERVER_SCHEDULED_DIRECTIONS.remove(train.id);
		} else {
			if (direction == previous && !forceSync && !periodicSync)
				return;
			SERVER_SCHEDULED_DIRECTIONS.put(train.id, direction);
		}

		RailwardPackets.getChannel().send(PacketDistributor.ALL.noArg(),
			new LocomotiveBufferLightsTrainStatePacket(train.id, direction));
	}

	public static void writeScheduledDirection(Train train, CompoundTag tag) {
		DirectionState direction = SERVER_SCHEDULED_DIRECTIONS.get(train.id);
		if ((direction == null || direction == DirectionState.NONE) && hasScheduledControls(train))
			direction = resolveScheduledDirection(train);
		if (direction != null && direction != DirectionState.NONE)
			tag.putString(NBT_DIRECTION_KEY, direction.name());
	}

	public static void readScheduledDirection(Train train, CompoundTag tag) {
		DirectionState direction = readDirection(tag);
		if (direction == DirectionState.NONE) {
			SERVER_SCHEDULED_DIRECTIONS.remove(train.id);
			SERVER_FORCE_SCHEDULED_SYNC.remove(train.id);
			return;
		}

		SERVER_SCHEDULED_DIRECTIONS.put(train.id, direction);
		SERVER_FORCE_SCHEDULED_SYNC.add(train.id);
	}

	private static DirectionState resolveManualDirection(CarriageContraptionEntity entity, BlockPos controlsLocalPos) {
		Contraption contraption = entity.getContraption();
		if (!(contraption instanceof CarriageContraption carriageContraption))
			return DirectionState.NONE;

		StructureBlockInfo info = contraption.getBlocks()
			.get(controlsLocalPos);
		if (info == null || !info.state.hasProperty(ControlsBlock.FACING))
			return DirectionState.NONE;

		Direction controlsFacing = info.state.getValue(ControlsBlock.FACING);
		Direction assemblyDirection = carriageContraption.getAssemblyDirection();
		return controlsFacing == assemblyDirection ? DirectionState.FORWARD : DirectionState.BACKWARD;
	}

	public static void setClientDirection(UUID trainId, DirectionState direction) {
		CLIENT_DIRECTIONS.put(trainId, direction);
	}

	public static DirectionState resolve(CarriageContraptionEntity entity) {
		Carriage carriage = entity.getCarriage();
		if (carriage == null || carriage.train == null)
			return DirectionState.NONE;
		Train train = carriage.train;

		if (CLIENT_DIRECTIONS.containsKey(train.id))
			return CLIENT_DIRECTIONS.get(train.id);

		DirectionState scheduledDirection = resolveScheduledDirection(train);
		if (scheduledDirection != DirectionState.NONE)
			return scheduledDirection;

		return DirectionState.NONE;
	}

	private static DirectionState resolveScheduledDirection(Train train) {
		if (!hasScheduledControls(train))
			return DirectionState.NONE;

		DirectionState transitDirection = resolveTransitDirection(train);
		if (transitDirection != DirectionState.NONE)
			return transitDirection;

		DirectionState previous = SERVER_SCHEDULED_DIRECTIONS.get(train.id);
		if (previous != null)
			return previous;

		return train.currentlyBackwards ? DirectionState.BACKWARD : DirectionState.FORWARD;
	}

	private static DirectionState resolveTransitDirection(Train train) {
		if (train.runtime.state != State.IN_TRANSIT)
			return DirectionState.NONE;

		if (train.navigation != null && train.navigation.isActive())
			return train.navigation.destinationBehindTrain ? DirectionState.BACKWARD : DirectionState.FORWARD;

		DirectionState targetDirection = bySpeed(train.targetSpeed);
		if (targetDirection != DirectionState.NONE)
			return targetDirection;

		DirectionState speedDirection = bySpeed(train.speed);
		if (speedDirection != DirectionState.NONE)
			return speedDirection;

		DirectionState previous = SERVER_SCHEDULED_DIRECTIONS.get(train.id);
		if (previous != null)
			return previous;

		return train.currentlyBackwards ? DirectionState.BACKWARD : DirectionState.FORWARD;
	}

	private static boolean hasScheduledControls(Train train) {
		return train.runtime != null && train.runtime.getSchedule() != null && !train.runtime.paused;
	}

	private static boolean isManuallyControlled(Train train) {
		if (train.carriages == null)
			return false;
		for (Carriage carriage : train.carriages) {
			CarriageContraptionEntity entity = carriage.anyAvailableEntity();
			if (entity != null && entity.getControllingPlayer()
				.isPresent())
				return true;
		}
		return false;
	}

	private static boolean shouldResyncScheduled(Train train, Level level) {
		long offset = Math.floorMod(train.id.getLeastSignificantBits(), SCHEDULED_RESYNC_INTERVAL);
		return Math.floorMod(level.getGameTime() + offset, SCHEDULED_RESYNC_INTERVAL) == 0;
	}

	private static DirectionState bySpeed(double speed) {
		if (Math.abs(speed) <= SPEED_EPSILON)
			return DirectionState.NONE;
		return speed < 0 ? DirectionState.BACKWARD : DirectionState.FORWARD;
	}

	private static DirectionState readDirection(CompoundTag tag) {
		if (!tag.contains(NBT_DIRECTION_KEY))
			return DirectionState.NONE;
		try {
			return DirectionState.valueOf(tag.getString(NBT_DIRECTION_KEY));
		} catch (IllegalArgumentException ignored) {
			return DirectionState.NONE;
		}
	}

	public enum DirectionState {
		NONE,
		FORWARD,
		BACKWARD
	}
}
