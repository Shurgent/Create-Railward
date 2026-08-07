package ua.pp.shurgent.railward.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import ua.pp.shurgent.railward.block.train.parts.TrainWallPanelBlock;
import ua.pp.shurgent.railward.block.train.parts.TrainWallPanelBlockEntity;
import ua.pp.shurgent.railward.block.train.parts.panel.LocomotiveBufferLightsAttachment;
import ua.pp.shurgent.railward.block.train.parts.panel.LocomotiveSpotlightAttachment;
import ua.pp.shurgent.railward.block.train.parts.panel.TrainPanelAttachment;

public class TrainWallPanelAttachmentRenderer extends SmartBlockEntityRenderer<TrainWallPanelBlockEntity> {

	public TrainWallPanelAttachmentRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	protected void renderSafe(TrainWallPanelBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffers,
		int light, int overlay) {
		TrainPanelAttachment attachment = be.getAttachment();
		BlockState state = be.getBlockState();
		Direction facing = state.getValue(TrainWallPanelBlock.FACING);
		if (attachment instanceof LocomotiveBufferLightsAttachment bufferLights)
			LocomotiveBufferLightsRenderHelper.renderPanelAttachment(state, bufferLights.getVariant(), facing, ms,
				buffers, light);
		else if (attachment instanceof LocomotiveSpotlightAttachment spotlight)
			LocomotiveSpotlightRenderHelper.renderPanelAttachment(state, spotlight.getVariant(), facing, ms, buffers,
				light);
	}
}
