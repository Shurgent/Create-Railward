package ua.pp.shurgent.railward.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import ua.pp.shurgent.railward.block.train.parts.LocomotiveSpotlightBlock;
import ua.pp.shurgent.railward.block.train.parts.LocomotiveSpotlightBlockEntity;
import ua.pp.shurgent.railward.block.train.parts.LocomotiveSpotlightVariant;

public class LocomotiveSpotlightRenderer extends SmartBlockEntityRenderer<LocomotiveSpotlightBlockEntity> {

	public LocomotiveSpotlightRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	protected void renderSafe(LocomotiveSpotlightBlockEntity be, float partialTicks, PoseStack ms,
		MultiBufferSource buffers, int light, int overlay) {
		BlockState state = be.getBlockState();
		Direction facing = state.getValue(LocomotiveSpotlightBlock.FACING);
		LocomotiveSpotlightVariant variant = state.getValue(LocomotiveSpotlightBlock.VARIANT);
		LocomotiveSpotlightRenderHelper.render(state, variant, facing, 0, ms, buffers, light);
	}
}
