package ua.pp.shurgent.railward.client.renderer;

import com.jozufozu.flywheel.util.transform.TransformStack;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.content.decoration.slidingDoor.SlidingDoorBlock;
import com.simibubi.create.content.decoration.slidingDoor.SlidingDoorBlockEntity;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import com.simibubi.create.foundation.render.CachedBufferer;
import com.simibubi.create.foundation.utility.animation.LerpedFloat;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.Vec3;
import ua.pp.shurgent.railward.AllPartialModels;
import ua.pp.shurgent.railward.AllPartialModels.SlidingDoorPartialModels;
import ua.pp.shurgent.railward.mixin.create.SlidingDoorBlockEntityAccessor;

public class RailwardSlidingDoorRenderer extends SafeBlockEntityRenderer<SlidingDoorBlockEntity> {

	public RailwardSlidingDoorRenderer(Context context) {}

	@Override
	protected void renderSafe(SlidingDoorBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
			int light, int overlay) {
		BlockState state = be.getBlockState();
		SlidingDoorBlockEntityAccessor accessor = (SlidingDoorBlockEntityAccessor) be;
		boolean visible = state.getOptionalValue(SlidingDoorBlock.VISIBLE)
			.orElse(true);
		if (visible && accessor.railward$getBridgeTicks() == 0)
			return;

		SlidingDoorPartialModels models = AllPartialModels.getSlidingDoorModels(state.getBlock()
				.getRegistryName()
				.getPath());
		if (models == null)
			return;

		LerpedFloat animation = accessor.railward$getAnimation();
		float value = animation.getValue(partialTicks);
		float eased = value * value;
		float forwardValue = Mth.clamp(value * 10, 0, 1);
		Direction facing = state.getValue(DoorBlock.FACING);

		Vec3 leftOffset = new Vec3(-eased * 7 / 16f, 0, -forwardValue * 1 / 32f);
		Vec3 rightOffset = new Vec3(eased * 7 / 16f, 0, -forwardValue * 1 / 32f);

		VertexConsumer vb = buffer.getBuffer(RenderType.cutout());
		for (DoubleBlockHalf half : DoubleBlockHalf.values()) {
			renderHalf(models, state, facing, half, true, leftOffset, ms, vb, light);
			renderHalf(models, state, facing, half, false, rightOffset, ms, vb, light);
		}
	}

	private static void renderHalf(SlidingDoorPartialModels models, BlockState state, Direction facing,
			DoubleBlockHalf half, boolean left, Vec3 offset, PoseStack ms, VertexConsumer vb, int light) {
		ms.pushPose();
		TransformStack.cast(ms)
				.centre()
				.rotateY(blockStateRotation(facing))
				.unCentre()
				.translate(offset)
				.translate(0, half == DoubleBlockHalf.UPPER ? 1 - 1 / 512f : 0, 0);
		CachedBufferer.partial(models.get(half, left), state)
				.fullNormalTransform()
				.diffuseCalculator(RailwardDoorDiffuse.CAMERA_CORRECTED)
				.light(light)
				.renderInto(ms, vb);
		ms.popPose();
	}

	private static int blockStateRotation(Direction facing) {
		return switch (facing) {
		case SOUTH -> 180;
		case WEST -> 90;
		case EAST -> 270;
		default -> 0;
		};
	}
}
