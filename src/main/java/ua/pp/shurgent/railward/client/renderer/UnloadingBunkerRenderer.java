package ua.pp.shurgent.railward.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import ua.pp.shurgent.railward.block.train.station.UnloadingBunkerBlockEntity;

public class UnloadingBunkerRenderer extends SafeBlockEntityRenderer<UnloadingBunkerBlockEntity> {

	public UnloadingBunkerRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	protected void renderSafe(UnloadingBunkerBlockEntity be, float partialTicks, PoseStack ms,
		MultiBufferSource buffer, int light, int overlay) {
		UnloadingFallingItemRenderer.render(be.getFallingItems(), be.getLevel(), partialTicks, ms, buffer, light,
			overlay);
	}

	@Override
	public boolean shouldRenderOffScreen(UnloadingBunkerBlockEntity be) {
		return !be.getFallingItems()
			.isEmpty();
	}
}
