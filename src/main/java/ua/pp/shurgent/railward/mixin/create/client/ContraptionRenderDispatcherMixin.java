package ua.pp.shurgent.railward.mixin.create.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Group;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.render.ContraptionRenderDispatcher;
import com.simibubi.create.content.contraptions.render.ContraptionMatrices;
import com.jozufozu.flywheel.core.virtual.VirtualRenderWorld;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.level.Level;
import ua.pp.shurgent.railward.client.renderer.LocomotiveSpotlightForwardRenderer;
import ua.pp.shurgent.railward.client.renderer.TrainCouplerContraptionRenderContext;

@Mixin(value = ContraptionRenderDispatcher.class, remap = false)
public class ContraptionRenderDispatcherMixin {

	@Inject(method = "renderFromEntity", at = @At("HEAD"), remap = false)
	private static void railward$setCurrentContraptionEntity(AbstractContraptionEntity entity, Contraption contraption,
		MultiBufferSource buffer, CallbackInfo ci) {
		TrainCouplerContraptionRenderContext.set(entity);
	}

	@Inject(method = "renderFromEntity", at = @At("RETURN"), remap = false)
	private static void railward$clearCurrentContraptionEntity(AbstractContraptionEntity entity, Contraption contraption,
		MultiBufferSource buffer, CallbackInfo ci) {
		TrainCouplerContraptionRenderContext.clear();
		LocomotiveSpotlightForwardRenderer.setContraptionFallbackPass(false);
	}

	@Inject(method = "renderBlockEntities", at = @At("HEAD"), remap = false)
	private static void railward$beginSpecialBlockEntities(Level world, VirtualRenderWorld renderWorld, Contraption c,
		ContraptionMatrices matrices, MultiBufferSource buffer, CallbackInfo ci) {
		LocomotiveSpotlightForwardRenderer.setContraptionFallbackPass(true);
	}

	@Group(name = "railward$renderSpecialBlockEntities", min = 1, max = 1)
	@Redirect(method = "renderFromEntity", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;endBatch()V", remap = false),
		remap = false, require = 0)
	private static void railward$renderSpecialBlockEntitiesDev(MultiBufferSource.BufferSource buffers) {
		railward$renderSpecialBlockEntities(buffers);
	}

	@Group(name = "railward$renderSpecialBlockEntities", min = 1, max = 1)
	@Redirect(method = "renderFromEntity", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;m_109911_()V", remap = false),
		remap = false, require = 0)
	private static void railward$renderSpecialBlockEntitiesSrg(MultiBufferSource.BufferSource buffers) {
		railward$renderSpecialBlockEntities(buffers);
	}

	private static void railward$renderSpecialBlockEntities(MultiBufferSource.BufferSource buffers) {
		try {
			buffers.endBatch();
		} finally {
			LocomotiveSpotlightForwardRenderer.setContraptionFallbackPass(false);
		}
	}
}
