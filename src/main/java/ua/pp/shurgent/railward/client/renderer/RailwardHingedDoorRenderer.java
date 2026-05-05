package ua.pp.shurgent.railward.client.renderer;

import com.jozufozu.flywheel.util.transform.TransformStack;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.content.decoration.slidingDoor.SlidingDoorBlock;
import com.simibubi.create.content.decoration.slidingDoor.SlidingDoorBlockEntity;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import com.simibubi.create.foundation.render.CachedBufferer;
import com.simibubi.create.foundation.render.SuperByteBuffer;
import com.simibubi.create.foundation.utility.animation.LerpedFloat;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.Vec3;
import ua.pp.shurgent.railward.mixin.create.SlidingDoorBlockEntityAccessor;

public class RailwardHingedDoorRenderer extends SafeBlockEntityRenderer<SlidingDoorBlockEntity> {

	public RailwardHingedDoorRenderer(Context context) {}

	@Override
	protected void renderSafe(SlidingDoorBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
			int light, int overlay) {
		BlockState state = be.getBlockState();
		SlidingDoorBlockEntityAccessor accessor = (SlidingDoorBlockEntityAccessor) be;
		boolean visible = state.getOptionalValue(SlidingDoorBlock.VISIBLE)
			.orElse(true);
		if (visible && accessor.railward$getBridgeTicks() == 0)
			return;

		LerpedFloat animation = accessor.railward$getAnimation();
		float value = animation.getValue(partialTicks);
		float eased = value * value;
		Direction facing = state.getValue(DoorBlock.FACING);
		boolean rightHinge = state.getValue(DoorBlock.HINGE) == DoorHingeSide.RIGHT;

		Vec3 pivot = hingePivot(rightHinge);
		float angle = (rightHinge ? 90 : -90) * eased;
		VertexConsumer vb = buffer.getBuffer(RenderType.cutout());

		for (DoubleBlockHalf half : DoubleBlockHalf.values()) {
			SuperByteBuffer door = CachedBufferer.block(state.setValue(DoorBlock.FACING, Direction.NORTH)
					.setValue(DoorBlock.OPEN, false)
					.setValue(DoorBlock.HALF, half));

			ms.pushPose();
			TransformStack.cast(ms)
					.centre()
					.rotateY(blockStateRotation(facing))
					.unCentre()
					.translate(pivot)
					.rotateY(angle)
					.translateBack(pivot);
			if (half == DoubleBlockHalf.UPPER)
				ms.translate(0, 1 - 1 / 512f, 0);
			door
					.fullNormalTransform()
					.diffuseCalculator(RailwardDoorDiffuse.CAMERA_CORRECTED)
					.light(light)
					.renderInto(ms, vb);
			ms.popPose();
		}
	}

	private static Vec3 hingePivot(boolean rightHinge) {
		return new Vec3(rightHinge ? 14.5f / 16f : 1.5f / 16f, 0, 14.5f / 16f);
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
