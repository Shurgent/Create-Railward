package ua.pp.shurgent.railward.client.renderer;

import com.jozufozu.flywheel.core.PartialModel;
import ua.pp.shurgent.railward.client.compat.RailwardTransformStack;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import com.simibubi.create.foundation.render.CachedBufferer;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import com.simibubi.create.content.trains.entity.Train;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraft.world.phys.Vec3;
import ua.pp.shurgent.railward.AllBlocks;
import ua.pp.shurgent.railward.AllPartialModels;
import ua.pp.shurgent.railward.block.train.parts.TrainCouplerBlock;
import ua.pp.shurgent.railward.block.train.parts.TrainCouplerBlockEntity;

public class TrainCouplerRenderer extends SmartBlockEntityRenderer<TrainCouplerBlockEntity> {
	private static final double MAX_PAIR_DISTANCE_SQ = 25;
	private static final double MIN_FORWARD_DOT = 0.05;
	private static final double MIN_SEGMENT_LENGTH = 1.0 / 64.0;
	private static final double HEAD_OVERLAP = 0.08 + 1.0 / 16.0;

	private static final Vec3 BLOCK_CENTER = new Vec3(0.5, 0.5, 0.5);
	private static final Vec3 MODEL_PIVOT = new Vec3(0.5, 0.5, 0);
	private static final Vec3 MODEL_FORWARD = new Vec3(0, 0, 1);

	private static final double DRAWBAR_START = 2.0 / 16.0;
	private static final double DRAWBAR_END = 9.0 / 16.0;
	private static final double DRAWBAR_LENGTH = DRAWBAR_END - DRAWBAR_START;
	private static final double HEAD_START = 9.0 / 16.0;
	private static final double HEAD_END = 18.2 / 16.0;
	private static final double LEVER_START = 0.5 / 16.0;
	private static final double LEVER_END = 10.5 / 16.0;
	private static final double LEVER_LENGTH = LEVER_END - LEVER_START;

	public TrainCouplerRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	protected void renderSafe(TrainCouplerBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffers,
		int light, int overlay) {
		BlockState state = be.getBlockState();
		Direction facing = state.getValue(TrainCouplerBlock.FACING);
		float facingRotation = getFacingRotation(facing);
		VertexConsumer buffer = buffers.getBuffer(RenderType.solid());
		DynamicPose dynamicPose = resolveDynamicPose(be, state, partialTicks, facingRotation);

		ms.pushPose();
		RailwardTransformStack.cast(ms).centre()
			.rotateY(facingRotation)
			.unCentre();

		renderPartial(AllPartialModels.TRAIN_COUPLER_STATIC, state, light, ms, buffer);
		renderDynamicPart(state, dynamicPose, light, ms, buffer);

		ms.popPose();
	}

	private DynamicPose resolveDynamicPose(TrainCouplerBlockEntity be, BlockState state, float partialTicks,
		float facingRotation) {
		AbstractContraptionEntity context = TrainCouplerContraptionRenderContext.get();
		if (!(context instanceof CarriageContraptionEntity currentEntity))
			return DynamicPose.DEFAULT;

		Carriage currentCarriage = currentEntity.getCarriage();
		if (currentCarriage == null || currentCarriage.train == null)
			return DynamicPose.DEFAULT;

		Train train = currentCarriage.train;
		int currentIndex = currentEntity.carriageIndex;
		if (currentIndex < 0 || currentIndex >= train.carriages.size() || train.carriages.get(currentIndex) != currentCarriage)
			currentIndex = train.carriages.indexOf(currentCarriage);
		if (currentIndex < 0)
			return DynamicPose.DEFAULT;

		Vec3 currentPivotLocal = getPivotLocal(be.getBlockPos(), state);
		Vec3 targetPivotLocal =
			findPairedPivotLocal(currentEntity, train, currentIndex, currentPivotLocal, state, partialTicks);
		if (targetPivotLocal == null)
			return DynamicPose.DEFAULT;

		Vec3 direction = targetPivotLocal.subtract(currentPivotLocal);
		if (direction.lengthSqr() < 1.0E-6)
			return DynamicPose.DEFAULT;

		Vec3 modelDirection = rotateYaw(direction, -facingRotation);
		double horizontalDistance =
			Math.sqrt(modelDirection.x * modelDirection.x + modelDirection.z * modelDirection.z);
		if (horizontalDistance < 1.0E-4)
			return DynamicPose.DEFAULT;

		float yaw = (float) Math.toDegrees(Math.atan2(modelDirection.x, modelDirection.z));
		double targetEnd = Math.max(HEAD_START + MIN_SEGMENT_LENGTH, direction.length() * 0.5 + HEAD_OVERLAP);
		double headOffset = targetEnd - HEAD_END;
		double headStart = HEAD_START + headOffset;
		double drawbarEnd = Math.max(DRAWBAR_START + MIN_SEGMENT_LENGTH, headStart);
		double drawbarScale = Math.max(MIN_SEGMENT_LENGTH / DRAWBAR_LENGTH, (drawbarEnd - DRAWBAR_START) / DRAWBAR_LENGTH);
		double leverEnd = Math.max(LEVER_START + MIN_SEGMENT_LENGTH, LEVER_END + headOffset);
		double leverScale = Math.max(MIN_SEGMENT_LENGTH / LEVER_LENGTH, (leverEnd - LEVER_START) / LEVER_LENGTH);

		return new DynamicPose(yaw, headOffset, drawbarScale, leverScale);
	}

	private Vec3 findPairedPivotLocal(CarriageContraptionEntity currentEntity, Train train, int currentIndex,
		Vec3 currentPivotLocal, BlockState currentState, float partialTicks) {
		Vec3 best = null;
		double bestDistance = MAX_PAIR_DISTANCE_SQ;
		Vec3 currentPivotWorld = toInterpolatedGlobal(currentEntity, currentPivotLocal, partialTicks);
		Vec3 currentForward = getForwardVector(currentState);

		int[] adjacentIndices = { currentIndex - 1, currentIndex + 1 };
		for (int adjacentIndex : adjacentIndices) {
			if (adjacentIndex < 0 || adjacentIndex >= train.carriages.size())
				continue;

			Carriage adjacentCarriage = train.carriages.get(adjacentIndex);
			CarriageContraptionEntity adjacentEntity = adjacentCarriage.anyAvailableEntity();
			if (adjacentEntity == null || adjacentEntity == currentEntity || adjacentEntity.level != currentEntity.level)
				continue;

			Contraption adjacentContraption = adjacentEntity.getContraption();
			if (adjacentContraption == null)
				continue;

			for (StructureBlockInfo info : adjacentContraption.getBlocks()
				.values()) {
				if (!AllBlocks.TRAIN_COUPLER.has(info.state))
					continue;

				Vec3 adjacentPivotLocal = getPivotLocal(info.pos, info.state);
				Vec3 adjacentPivotWorld = toInterpolatedGlobal(adjacentEntity, adjacentPivotLocal, partialTicks);
				Vec3 adjacentPivotInCurrentLocal = toInterpolatedLocal(currentEntity, adjacentPivotWorld, partialTicks);
				if (!isFacingPair(currentPivotLocal, currentPivotWorld, currentForward, adjacentEntity,
					adjacentPivotLocal, adjacentPivotInCurrentLocal, info.state, partialTicks))
					continue;

				double distance = currentPivotLocal.distanceToSqr(adjacentPivotInCurrentLocal);
				if (distance >= bestDistance)
					continue;

				bestDistance = distance;
				best = adjacentPivotInCurrentLocal;
			}
		}

		return best;
	}

	private boolean isFacingPair(Vec3 currentPivotLocal, Vec3 currentPivotWorld, Vec3 currentForward,
		CarriageContraptionEntity adjacentEntity, Vec3 adjacentPivotLocal, Vec3 adjacentPivotInCurrentLocal,
		BlockState adjacentState, float partialTicks) {
		Vec3 toAdjacent = adjacentPivotInCurrentLocal.subtract(currentPivotLocal);
		if (!pointsForward(toAdjacent, currentForward))
			return false;

		Vec3 currentPivotInAdjacentLocal = toInterpolatedLocal(adjacentEntity, currentPivotWorld, partialTicks);
		Vec3 toCurrent = currentPivotInAdjacentLocal.subtract(adjacentPivotLocal);
		return pointsForward(toCurrent, getForwardVector(adjacentState));
	}

	private boolean pointsForward(Vec3 offset, Vec3 forward) {
		double length = offset.length();
		if (length < 1.0E-4)
			return false;
		return offset.dot(forward) / length > MIN_FORWARD_DOT;
	}

	private void renderDynamicPart(BlockState state, DynamicPose pose, int light, PoseStack ms, VertexConsumer buffer) {
		ms.pushPose();
		translate(ms, MODEL_PIVOT);
		RailwardTransformStack.cast(ms)
			.rotateY(pose.yawDegrees());
		translate(ms, MODEL_PIVOT.scale(-1));

		renderScaledZ(AllPartialModels.TRAIN_COUPLER_DRAWBAR, state, DRAWBAR_START, pose.drawbarScale(), light, ms,
			buffer);
		renderTranslatedZ(AllPartialModels.TRAIN_COUPLER_HEAD, state, pose.headOffset(), light, ms, buffer);
		renderScaledZ(AllPartialModels.TRAIN_COUPLER_LEVER_DYNAMIC, state, LEVER_START, pose.leverScale(), light, ms,
			buffer);

		ms.popPose();
	}

	private void renderScaledZ(PartialModel partial, BlockState state, double start, double scale, int light, PoseStack ms,
		VertexConsumer buffer) {
		ms.pushPose();
		ms.translate(0, 0, start);
		ms.scale(1, 1, (float) scale);
		ms.translate(0, 0, -start);
		renderPartial(partial, state, light, ms, buffer);
		ms.popPose();
	}

	private void renderTranslatedZ(PartialModel partial, BlockState state, double offset, int light, PoseStack ms,
		VertexConsumer buffer) {
		ms.pushPose();
		ms.translate(0, 0, offset);
		renderPartial(partial, state, light, ms, buffer);
		ms.popPose();
	}

	private void renderPartial(PartialModel partial, BlockState state, int light, PoseStack ms, VertexConsumer buffer) {
		CachedBufferer.partial(partial, state)
			.light(light)
			.renderInto(ms, buffer);
	}

	private Vec3 getPivotLocal(BlockPos localPos, BlockState state) {
		float rotation = state.hasProperty(TrainCouplerBlock.FACING)
			? getFacingRotation(state.getValue(TrainCouplerBlock.FACING))
			: 0;
		Vec3 pivotInBlock = rotateAroundBlockCenter(MODEL_PIVOT, rotation);
		return Vec3.atLowerCornerOf(localPos)
			.add(pivotInBlock);
	}

	private Vec3 toInterpolatedGlobal(CarriageContraptionEntity entity, Vec3 local, float partialTicks) {
		Vec3 anchor = getInterpolatedAnchor(entity, partialTicks);
		return entity.applyRotation(local.subtract(BLOCK_CENTER), partialTicks)
			.add(BLOCK_CENTER)
			.add(anchor);
	}

	private Vec3 toInterpolatedLocal(CarriageContraptionEntity entity, Vec3 global, float partialTicks) {
		Vec3 anchor = getInterpolatedAnchor(entity, partialTicks);
		return entity.reverseRotation(global.subtract(anchor)
			.subtract(BLOCK_CENTER), partialTicks)
			.add(BLOCK_CENTER);
	}

	private Vec3 getInterpolatedAnchor(CarriageContraptionEntity entity, float partialTicks) {
		return entity.getPosition(partialTicks)
			.subtract(0.5, 0, 0.5);
	}

	private Vec3 getForwardVector(BlockState state) {
		float rotation = state.hasProperty(TrainCouplerBlock.FACING)
			? getFacingRotation(state.getValue(TrainCouplerBlock.FACING))
			: 0;
		return rotateYaw(MODEL_FORWARD, rotation)
			.normalize();
	}

	private Vec3 rotateAroundBlockCenter(Vec3 point, float yawDegrees) {
		return BLOCK_CENTER.add(rotateYaw(point.subtract(BLOCK_CENTER), yawDegrees));
	}

	private Vec3 rotateYaw(Vec3 vec, float yawDegrees) {
		return vec.yRot((float) Math.toRadians(yawDegrees));
	}

	private void translate(PoseStack ms, Vec3 vec) {
		ms.translate(vec.x, vec.y, vec.z);
	}

	private float getFacingRotation(Direction facing) {
		return switch (facing) {
		case EAST -> -90;
		case SOUTH -> 180;
		case WEST -> 90;
		default -> 0;
		};
	}

	private record DynamicPose(float yawDegrees, double headOffset, double drawbarScale, double leverScale) {
		private static final DynamicPose DEFAULT = new DynamicPose(0, 0, 1, 1);
	}

}
