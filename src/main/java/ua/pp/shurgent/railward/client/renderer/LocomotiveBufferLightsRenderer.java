package ua.pp.shurgent.railward.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import ua.pp.shurgent.railward.block.train.parts.LocomotiveBufferLightsBlock;
import ua.pp.shurgent.railward.block.train.parts.LocomotiveBufferLightsBlockEntity;
import ua.pp.shurgent.railward.block.train.parts.LocomotiveBufferLightsVariant;

public class LocomotiveBufferLightsRenderer extends SmartBlockEntityRenderer<LocomotiveBufferLightsBlockEntity> {

	public LocomotiveBufferLightsRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	protected void renderSafe(LocomotiveBufferLightsBlockEntity be, float partialTicks, PoseStack ms,
		MultiBufferSource buffers, int light, int overlay) {
		BlockState state = be.getBlockState();
		Direction facing = state.getValue(LocomotiveBufferLightsBlock.FACING);
		LocomotiveBufferLightsVariant variant = state.getValue(LocomotiveBufferLightsBlock.VARIANT);
		LocomotiveBufferLightsRenderHelper.render(state, variant, facing, 0, ms, buffers, light);
	}
}
