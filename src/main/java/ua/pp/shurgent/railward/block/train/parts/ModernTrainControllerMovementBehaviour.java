package ua.pp.shurgent.railward.block.train.parts;

import com.jozufozu.flywheel.core.PartialModel;
import com.jozufozu.flywheel.core.virtual.VirtualRenderWorld;
import com.simibubi.create.content.contraptions.behaviour.MovementBehaviour;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ContraptionMatrices;
import com.simibubi.create.content.contraptions.render.ContraptionRenderDispatcher;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import com.simibubi.create.foundation.render.CachedBufferer;
import com.simibubi.create.foundation.render.SuperByteBuffer;
import com.simibubi.create.foundation.utility.AnimationTickHolder;
import com.simibubi.create.foundation.utility.animation.LerpedFloat;
import com.simibubi.create.foundation.utility.animation.LerpedFloat.Chaser;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import ua.pp.shurgent.railward.AllPartialModels;
import ua.pp.shurgent.railward.block.train.parts.TrainDirectionState.DirectionState;

public class ModernTrainControllerMovementBehaviour implements MovementBehaviour {

	private static final Vec3 BRAKE_PIVOT = new Vec3(2.5 / 16d, 11 / 16d, 10.5 / 16d);
	private static final Vec3 REVERSER_PIVOT = new Vec3(12.5 / 16d, 10 / 16d, 6.5 / 16d);
	private static final Vec3 THROTTLE_PIVOT = new Vec3(13.5 / 16d, 10 / 16d, 9.5 / 16d);

	private static class LeverAnimations {
		final LerpedFloat reverser = LerpedFloat.linear().startWithValue(0);
		final LerpedFloat brake = LerpedFloat.linear().startWithValue(0);
		final LerpedFloat throttle = LerpedFloat.linear().startWithValue(0);

		void tick() {
			reverser.tickChaser();
			brake.tickChaser();
			throttle.tickChaser();
		}
	}

	@Override
	public ItemStack canBeDisabledVia(MovementContext context) {
		return null;
	}

	@Override
	public void stopMoving(MovementContext context) {
		context.contraption.entity.stopControlling(context.localPos);
		MovementBehaviour.super.stopMoving(context);
	}

	@Override
	public void tick(MovementContext context) {
		MovementBehaviour.super.tick(context);
		if (context.world.isClientSide)
			getAnimations(context).tick();
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public void renderInContraption(MovementContext context, VirtualRenderWorld renderWorld,
		ContraptionMatrices matrices, MultiBufferSource buffer) {
		if (!(context.contraption.entity instanceof CarriageContraptionEntity carriageEntity))
			return;
		StructureBlockInfo info = context.contraption.getBlocks().get(context.localPos);
		if (info == null || !(info.state.getBlock() instanceof ModernTrainControllerBlock))
			return;

		BlockState state = info.state;
		boolean active = state.getValue(ModernTrainControllerBlock.ACTIVE);
		DirectionState direction = active ? TrainDirectionState.resolveSynced(carriageEntity) : DirectionState.NONE;
		LeverAnimations animations = getAnimations(context);
		animations.reverser.chase(direction == DirectionState.FORWARD ? 1
			: direction == DirectionState.BACKWARD ? -1 : 0, .25f, Chaser.EXP);
		animations.brake.chase(active && state.getValue(ModernTrainControllerBlock.BRAKING) ? 1 : 0,
			.3f, Chaser.EXP);
		animations.throttle.chase(active && state.getValue(ModernTrainControllerBlock.THROTTLE) ? 1 : 0,
			.3f, Chaser.EXP);

		float partialTicks = AnimationTickHolder.getPartialTicks(context.world);
		Direction physicalFacing = ModernTrainControllerBlock.getPhysicalFacing(state);
		int light = ContraptionRenderDispatcher.getContraptionWorldLight(context, renderWorld);
		render(AllPartialModels.MODERN_TRAIN_CONTROLLER_REVERSER, state, physicalFacing, REVERSER_PIVOT,
			45 * animations.reverser.getValue(partialTicks), light, matrices, buffer);
		render(AllPartialModels.MODERN_TRAIN_CONTROLLER_BRAKE, state, physicalFacing, BRAKE_PIVOT,
			90 * animations.brake.getValue(partialTicks), light, matrices, buffer);
		render(AllPartialModels.MODERN_TRAIN_CONTROLLER_THROTTLE, state, physicalFacing, THROTTLE_PIVOT,
			-90 * animations.throttle.getValue(partialTicks), light, matrices, buffer);
	}

	@OnlyIn(Dist.CLIENT)
	private static void render(PartialModel partial, BlockState state, Direction physicalFacing, Vec3 pivot,
		float angle, int light, ContraptionMatrices matrices, MultiBufferSource buffer) {
		SuperByteBuffer model = CachedBufferer.partial(partial, state);
		model.transform(matrices.getModel())
			.centre()
			.rotateY(blockStateRotation(physicalFacing))
			.unCentre()
			.translate(pivot)
			.rotateY(angle)
			.translateBack(pivot)
			.light(matrices.getWorld(), light)
			.renderInto(matrices.getViewProjection(), buffer.getBuffer(RenderType.solid()));
	}

	private static int blockStateRotation(Direction facing) {
		return switch (facing) {
		case SOUTH -> 180;
		case WEST -> 90;
		case EAST -> 270;
		default -> 0;
		};
	}

	private static LeverAnimations getAnimations(MovementContext context) {
		if (context.temporaryData instanceof LeverAnimations animations)
			return animations;
		LeverAnimations animations = new LeverAnimations();
		context.temporaryData = animations;
		return animations;
	}
}
