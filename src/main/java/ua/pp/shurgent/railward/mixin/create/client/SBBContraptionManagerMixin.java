package ua.pp.shurgent.railward.mixin.create.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Group;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.simibubi.create.content.contraptions.render.SBBContraptionManager;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import ua.pp.shurgent.railward.client.renderer.LocomotiveSpotlightForwardRenderer;

@Mixin(value = SBBContraptionManager.class, remap = false)
public class SBBContraptionManagerMixin {

	@Group(name = "railward$renderContraptionBatch", min = 1, max = 1)
	@Redirect(method = "renderLayer", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;endBatch(Lnet/minecraft/client/renderer/RenderType;)V",
		remap = false), remap = false, require = 0)
	private void railward$renderContraptionBatchDev(MultiBufferSource.BufferSource buffers, RenderType type) {
		railward$renderContraptionBatch(buffers, type);
	}

	@Group(name = "railward$renderContraptionBatch", min = 1, max = 1)
	@Redirect(method = "renderLayer", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;m_109912_(Lnet/minecraft/client/renderer/RenderType;)V",
		remap = false), remap = false, require = 0)
	private void railward$renderContraptionBatchSrg(MultiBufferSource.BufferSource buffers, RenderType type) {
		railward$renderContraptionBatch(buffers, type);
	}

	private void railward$renderContraptionBatch(MultiBufferSource.BufferSource buffers, RenderType type) {
		LocomotiveSpotlightForwardRenderer.setContraptionFallbackPass(true);
		try {
			buffers.endBatch(type);
		} finally {
			LocomotiveSpotlightForwardRenderer.setContraptionFallbackPass(false);
		}
	}
}
