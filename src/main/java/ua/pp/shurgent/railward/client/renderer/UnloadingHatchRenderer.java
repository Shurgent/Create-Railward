package ua.pp.shurgent.railward.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import ua.pp.shurgent.railward.block.train.station.UnloadingHatchBlockEntity;

public class UnloadingHatchRenderer extends SafeBlockEntityRenderer<UnloadingHatchBlockEntity> {

	public UnloadingHatchRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	protected void renderSafe(UnloadingHatchBlockEntity be, float partialTicks, PoseStack ms,
		MultiBufferSource buffer, int light, int overlay) {
		UnloadingFallingItemRenderer.render(be.getFallingItems(), be.getLevel(), partialTicks, ms, buffer, light,
			overlay);
	}

	@Override
	public boolean shouldRenderOffScreen(UnloadingHatchBlockEntity be) {
		return !be.getFallingItems()
			.isEmpty();
	}
}
