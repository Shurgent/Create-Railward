package ua.pp.shurgent.railward.block.train.parts;

import java.util.Collection;

import com.simibubi.create.AllPackets;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.contraptions.ContraptionBlockChangedPacket;
import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.CarriageContraption;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.entity.TrainPromptPacket;
import com.simibubi.create.foundation.utility.Iterate;
import com.simibubi.create.foundation.utility.VecHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraftforge.network.PacketDistributor;

public final class ModernTrainControllerState {

	private static final double STOPPED_EPSILON = 1.0E-3;

	private ModernTrainControllerState() {
	}

	public static boolean isController(CarriageContraptionEntity entity, BlockPos localPos) {
		if (entity == null || localPos == null || entity.getContraption() == null)
			return false;
		StructureBlockInfo info = entity.getContraption().getBlocks().get(localPos);
		return info != null && info.state.getBlock() instanceof ModernTrainControllerBlock;
	}

	public static void beginManualControl(CarriageContraptionEntity entity, BlockPos localPos, Player player) {
		if (entity.level.isClientSide || !isController(entity, localPos))
			return;
		Carriage carriage = entity.getCarriage();
		if (carriage == null || carriage.train == null)
			return;

		clearAllControllers(carriage.train, null);
		setLeverState(entity, localPos, true, false, false, player);
	}

	public static void updateManualControl(CarriageContraptionEntity entity, BlockPos localPos,
		Collection<Integer> heldControls, Player player) {
		if (entity.level.isClientSide || !isController(entity, localPos))
			return;
		Carriage carriage = entity.getCarriage();
		if (carriage == null || carriage.train == null)
			return;

		boolean braking = heldControls.contains(1);
		boolean throttle = heldControls.contains(0) && !braking;
		if (braking) {
			carriage.train.targetSpeed = 0;
			// Create has already approached target speed once during control(); a second
			// normal step gives the dedicated brake a clear effect without replacing the
			// rest of Create's train-control method.
			carriage.train.approachTargetSpeed(1);
		}
		setLeverState(entity, localPos, true, braking, throttle, player);
	}

	public static void endManualControl(CarriageContraptionEntity entity, BlockPos localPos) {
		if (entity.level.isClientSide)
			return;
		Carriage carriage = entity.getCarriage();
		if (carriage != null && carriage.train != null) {
			Player controllingPlayer = entity.getControllingPlayer()
				.map(entity.level::getPlayerByUUID)
				.orElse(null);
			clearAllControllers(carriage.train, controllingPlayer);
			return;
		}
		if (isController(entity, localPos))
			setLeverState(entity, localPos, false, false, false, null);
	}

	public static void syncScheduledControls(Train train) {
		if (train == null || train.carriages == null || isManuallyControlled(train))
			return;

		boolean scheduled = TrainDirectionState.hasScheduledControls(train);
		double speed = Math.abs(train.speed);
		double targetSpeed = Math.abs(train.targetSpeed);
		boolean braking = scheduled && speed > STOPPED_EPSILON
			&& (targetSpeed <= STOPPED_EPSILON
				|| Math.signum(train.speed) != Math.signum(train.targetSpeed)
				|| targetSpeed + STOPPED_EPSILON < speed);
		boolean throttle = scheduled && targetSpeed > STOPPED_EPSILON && !braking;

		for (Carriage carriage : train.carriages) {
			CarriageContraptionEntity entity = carriage.anyAvailableEntity();
			if (entity == null || entity.level.isClientSide
				|| !(entity.getContraption() instanceof CarriageContraption contraption))
				continue;

			for (StructureBlockInfo info : contraption.getBlocks().values()) {
				if (!(info.state.getBlock() instanceof ModernTrainControllerBlock))
					continue;
				boolean active = scheduled && hasNpcAtController(entity, contraption, info.pos);
				setLeverState(entity, info.pos, active, active && braking, active && throttle, null);
			}
		}
	}

	public static void toggleReverser(ServerPlayer player, int entityId, BlockPos localPos) {
		if (player == null || player.isSpectator())
			return;
		Entity target = player.level.getEntity(entityId);
		if (!(target instanceof CarriageContraptionEntity entity) || !isController(entity, localPos))
			return;
		if (entity.getControllingPlayer().filter(player.getUUID()::equals).isEmpty())
			return;
		if (!entity.toGlobalVector(VecHelper.getCenterOf(localPos), 1).closerThan(player.position(), 8))
			return;

		Carriage carriage = entity.getCarriage();
		if (carriage == null || carriage.train == null)
			return;
		StructureBlockInfo info = entity.getContraption().getBlocks().get(localPos);
		BlockState state = info.state;
		if (!state.getValue(ModernTrainControllerBlock.ACTIVE))
			return;

		if (Math.abs(carriage.train.speed) > STOPPED_EPSILON
			|| state.getValue(ModernTrainControllerBlock.THROTTLE)
			|| state.getValue(ModernTrainControllerBlock.BRAKING)) {
			playDenied(entity, localPos);
			sendPrompt(player, "railward.modern_train_controller.reverser.must_stop");
			return;
		}

		BlockState updated = state
			.setValue(ModernTrainControllerBlock.FACING,
				state.getValue(ModernTrainControllerBlock.FACING).getOpposite())
			.cycle(ModernTrainControllerBlock.REVERSED);
		updateBlock(entity, localPos, info, updated, player);
		TrainDirectionState.syncManualControls(entity, localPos, player);

		boolean backwards = updated.getValue(ModernTrainControllerBlock.REVERSED);
		BlockPos soundPos = globalPos(entity, localPos);
		AllSoundEvents.CONTROLLER_CLICK.play(entity.level, null, soundPos, 1, backwards ? .75f : 1.05f);
		sendPrompt(player, backwards ? "railward.modern_train_controller.reverser.backward"
			: "railward.modern_train_controller.reverser.forward");
	}

	private static void clearAllControllers(Train train, Player directPlayer) {
		for (Carriage carriage : train.carriages) {
			CarriageContraptionEntity entity = carriage.anyAvailableEntity();
			if (entity == null || entity.level.isClientSide)
				continue;
			for (StructureBlockInfo info : entity.getContraption().getBlocks().values())
				if (info.state.getBlock() instanceof ModernTrainControllerBlock)
					setLeverState(entity, info.pos, false, false, false, directPlayer);
		}
	}

	private static void setLeverState(CarriageContraptionEntity entity, BlockPos localPos, boolean active,
		boolean braking, boolean throttle, Player directPlayer) {
		StructureBlockInfo info = entity.getContraption().getBlocks().get(localPos);
		if (info == null || !(info.state.getBlock() instanceof ModernTrainControllerBlock))
			return;
		BlockState updated = info.state
			.setValue(ModernTrainControllerBlock.ACTIVE, active)
			.setValue(ModernTrainControllerBlock.BRAKING, braking)
			.setValue(ModernTrainControllerBlock.THROTTLE, throttle);
		if (updated == info.state)
			return;
		updateBlock(entity, localPos, info, updated, directPlayer);
	}

	private static void updateBlock(CarriageContraptionEntity entity, BlockPos localPos, StructureBlockInfo oldInfo,
		BlockState updated, Player directPlayer) {
		entity.setBlock(localPos, new StructureBlockInfo(localPos, updated, oldInfo.nbt));
		if (directPlayer instanceof ServerPlayer serverPlayer)
			AllPackets.getChannel().send(PacketDistributor.PLAYER.with(() -> serverPlayer),
				new ContraptionBlockChangedPacket(entity.getId(), localPos, updated));
	}

	private static boolean hasNpcAtController(CarriageContraptionEntity entity, CarriageContraption contraption,
		BlockPos controlsPos) {
		for (Entity passenger : entity.getPassengers()) {
			if (passenger instanceof Player)
				continue;
			BlockPos seatPos = contraption.getSeatOf(passenger.getUUID());
			if (seatPos == null)
				continue;
			for (Direction direction : Iterate.directionsInAxis(contraption.getAssemblyDirection().getAxis())) {
				if (!seatPos.relative(direction).equals(controlsPos))
					continue;
				StructureBlockInfo info = contraption.getBlocks().get(controlsPos);
				if (info != null && ModernTrainControllerBlock.getPhysicalFacing(info.state) == direction.getOpposite())
					return true;
			}
		}
		return false;
	}

	private static boolean isManuallyControlled(Train train) {
		for (Carriage carriage : train.carriages) {
			CarriageContraptionEntity entity = carriage.anyAvailableEntity();
			if (entity != null && entity.getControllingPlayer().isPresent())
				return true;
		}
		return false;
	}

	private static void playDenied(CarriageContraptionEntity entity, BlockPos localPos) {
		AllSoundEvents.DENY.playOnServer(entity.level, globalPos(entity, localPos), 1, 1);
	}

	private static BlockPos globalPos(CarriageContraptionEntity entity, BlockPos localPos) {
		return new BlockPos(entity.toGlobalVector(VecHelper.getCenterOf(localPos), 1));
	}

	private static void sendPrompt(ServerPlayer player, String translationKey) {
		AllPackets.getChannel().send(PacketDistributor.PLAYER.with(() -> player),
			new TrainPromptPacket(new TranslatableComponent(translationKey), false));
	}
}
