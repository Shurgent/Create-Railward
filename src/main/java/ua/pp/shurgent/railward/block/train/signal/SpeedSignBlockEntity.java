package ua.pp.shurgent.railward.block.train.signal;

import java.util.List;

import com.jozufozu.flywheel.util.transform.TransformStack;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.Create;
import com.simibubi.create.content.contraptions.ITransformableBlockEntity;
import com.simibubi.create.content.contraptions.StructureTransform;
import com.simibubi.create.content.trains.track.TrackTargetingBehaviour;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBoard;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsFormatter;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import com.simibubi.create.foundation.utility.Components;
import com.simibubi.create.foundation.utility.VecHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import ua.pp.shurgent.railward.AllEdgePointTypes;
import ua.pp.shurgent.railward.RailwardConfig;

public class SpeedSignBlockEntity extends SmartBlockEntity implements ITransformableBlockEntity {

	public static final int DEFAULT_SPEED_KMH = 50;

	public TrackTargetingBehaviour<SpeedSignEdgePoint> edgePoint;
	public ScrollValueBehaviour speedLimit;

	public SpeedSignBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
		behaviours.add(edgePoint = new TrackTargetingBehaviour<>(this, AllEdgePointTypes.SPEED_SIGN));

		speedLimit = new SpeedSignScrollValueBehaviour(Components.translatable("railward.speed_sign.speed_limit"), this,
			new SpeedSignValueBoxTransform());
		speedLimit.between(getConfiguredSpeedStepKmh(), getConfiguredMaxSpeedKmh());
		speedLimit.value = getDefaultSpeedKmh();
		speedLimit.withCallback($ -> updateEdgePointSpeedLimit());
		behaviours.add(speedLimit);
	}

	public int getSpeedLimitKmh() {
		if (speedLimit == null)
			return getDefaultSpeedKmh();
		return clampSpeedLimitKmh(speedLimit.getValue());
	}

	public static int getConfiguredSpeedStepKmh() {
		return Math.max(1, RailwardConfig.COMMON.speedSignStepKmh.get());
	}

	public static int getConfiguredMaxSpeedKmh() {
		int step = getConfiguredSpeedStepKmh();
		int max = Math.max(step, RailwardConfig.COMMON.speedSignMaxSpeedKmh.get());
		return Math.max(step, (max / step) * step);
	}

	public static int getDefaultSpeedKmh() {
		return clampSpeedLimitKmh(DEFAULT_SPEED_KMH);
	}

	public static int clampSpeedLimitKmh(int speedKmh) {
		return speedForIndex(speedToIndex(speedKmh));
	}

	public static int speedToIndex(int speedKmh) {
		int step = getConfiguredSpeedStepKmh();
		int maxIndex = getMaxSpeedIndex();
		int roundedStep = Math.round(speedKmh / (float) step);
		return Mth.clamp(roundedStep, 1, maxIndex + 1) - 1;
	}

	public static int speedForIndex(int index) {
		int step = getConfiguredSpeedStepKmh();
		return (Mth.clamp(index, 0, getMaxSpeedIndex()) + 1) * step;
	}

	private static int getMaxSpeedIndex() {
		return getConfiguredMaxSpeedKmh() / getConfiguredSpeedStepKmh() - 1;
	}

	private void updateEdgePointSpeedLimit() {
		if (level == null || level.isClientSide || edgePoint == null)
			return;

		SpeedSignEdgePoint point = edgePoint.getEdgePoint();
		if (point == null)
			return;

		point.updateSpeedLimit(worldPosition, edgePoint.getTargetDirection() == AxisDirection.POSITIVE,
			getSpeedLimitKmh());
		Create.RAILWAYS.markTracksDirty();
	}

	@Override
	protected AABB createRenderBoundingBox() {
		return new AABB(worldPosition, edgePoint.getGlobalPosition()).inflate(2);
	}

	@Override
	public void transform(StructureTransform transform) {
		edgePoint.transform(transform);
	}

	private static class SpeedSignScrollValueBehaviour extends ScrollValueBehaviour {

		public SpeedSignScrollValueBehaviour(Component label, SmartBlockEntity be, ValueBoxTransform slot) {
			super(label, be, slot);
		}

		@Override
		public void read(CompoundTag nbt, boolean clientPacket) {
			super.read(nbt, clientPacket);
			value = clampSpeedLimitKmh(value);
		}

		@Override
		public void setValue(int value) {
			super.setValue(clampSpeedLimitKmh(value));
		}

		@Override
		public ValueSettingsBoard createBoard(Player player, BlockHitResult hitResult) {
			return new ValueSettingsBoard(label, getMaxSpeedIndex(), getMilestoneInterval(),
				List.of(Components.literal("km/h")), new ValueSettingsFormatter(this::formatSettings));
		}

		@Override
		public void setValueSettings(Player player, ValueSettings valueSetting, boolean ctrlDown) {
			int speed = speedForIndex(valueSetting.value());
			if (speed != getValue())
				playFeedbackSound(this);
			setValue(speed);
		}

		@Override
		public ValueSettings getValueSettings() {
			return new ValueSettings(0, speedToIndex(value));
		}

		@Override
		public String formatValue() {
			return formatSpeed(value);
		}

		public MutableComponent formatSettings(ValueSettings settings) {
			return Components.literal(formatSpeed(speedForIndex(settings.value())));
		}

		@Override
		public String getClipboardKey() {
			return "SpeedLimit";
		}

		private static String formatSpeed(int speed) {
			return clampSpeedLimitKmh(speed) + " km/h";
		}

		private static int getMilestoneInterval() {
			return Math.max(1, 50 / getConfiguredSpeedStepKmh());
		}
	}

	private static class SpeedSignValueBoxTransform extends ValueBoxTransform {

		private static final Vec3 LOCATION = VecHelper.voxelSpace(8, 13.5, 5);
		private static final double HIT_HALF_SIZE = 2.5 / 16d;
		private static final double HIT_DEPTH = 1.5 / 16d;

		@Override
		public Vec3 getLocalOffset(BlockState state) {
			if (!state.hasProperty(SpeedSignBlock.ROTATION))
				return LOCATION;
			float angle = state.getValue(SpeedSignBlock.ROTATION) * 360f / 16f;
			return VecHelper.rotateCentered(LOCATION, -angle, Axis.Y);
		}

		@Override
		public boolean testHit(BlockState state, Vec3 localHit) {
			Vec3 hit = localHit;
			if (state.hasProperty(SpeedSignBlock.ROTATION)) {
				float angle = state.getValue(SpeedSignBlock.ROTATION) * 360f / 16f;
				hit = VecHelper.rotateCentered(hit, angle, Axis.Y);
			}

			return Math.abs(hit.x - LOCATION.x) < HIT_HALF_SIZE
				&& Math.abs(hit.y - LOCATION.y) < HIT_HALF_SIZE
				&& Math.abs(hit.z - LOCATION.z) < HIT_DEPTH;
		}

		@Override
		public void rotate(BlockState state, PoseStack ms) {
			float angle = state.hasProperty(SpeedSignBlock.ROTATION)
				? state.getValue(SpeedSignBlock.ROTATION) * 360f / 16f
				: 0;
			TransformStack.cast(ms)
				.rotateY(-angle);
		}

		@Override
		public float getScale() {
			return .35f;
		}
	}
}
