package ua.pp.shurgent.railward.client.renderer;

import com.jozufozu.flywheel.util.transform.TransformStack;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import com.simibubi.create.foundation.render.CachedBufferer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import ua.pp.shurgent.railward.AllPartialModels;
import ua.pp.shurgent.railward.block.train.parts.TrainCouplerBlock;
import ua.pp.shurgent.railward.block.train.parts.TrainCouplerBlockEntity;

public class TrainCouplerRenderer extends SmartBlockEntityRenderer<TrainCouplerBlockEntity> {

	public TrainCouplerRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	protected void renderSafe(TrainCouplerBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffers,
		int light, int overlay) {
		BlockState state = be.getBlockState();

		ms.pushPose();
		TransformStack.cast(ms).centre()
			.rotateY(getFacingRotation(state.getValue(TrainCouplerBlock.FACING)))
			.unCentre();

		CachedBufferer.partial(AllPartialModels.TRAIN_COUPLER, state)
			.light(light)
			.renderInto(ms, buffers.getBuffer(RenderType.solid()));

		ms.popPose();
	}

	private float getFacingRotation(Direction facing) {
		return switch (facing) {
		case EAST -> -90;
		case SOUTH -> 180;
		case WEST -> 90;
		default -> 0;
		};
	}

}
