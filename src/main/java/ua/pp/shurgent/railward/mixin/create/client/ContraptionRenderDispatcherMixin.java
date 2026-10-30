package ua.pp.shurgent.railward.mixin.create.client;

//? if create6 {
/*import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.render.ContraptionEntityRenderer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import ua.pp.shurgent.railward.client.renderer.LocomotiveSpotlightForwardRenderer;
import ua.pp.shurgent.railward.client.renderer.TrainCouplerContraptionRenderContext;

@Mixin(value = ContraptionEntityRenderer.class, remap = false)
public class ContraptionRenderDispatcherMixin {

	// The typed render method keeps its name in production. m_7392_ is the
	// compiler-generated bridge taking Entity, not AbstractContraptionEntity.
	@Inject(method = "render(Lcom/simibubi/create/content/contraptions/AbstractContraptionEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
		at = @At("HEAD"), remap = false, require = 1)
	private void railward$setCurrentContraptionEntity(AbstractContraptionEntity entity, float yaw,
		float partialTicks, PoseStack poseStack, MultiBufferSource buffers, int overlay, CallbackInfo ci) {
		railward$beginContraptionRender(entity, buffers);
	}

	@Inject(method = "render(Lcom/simibubi/create/content/contraptions/AbstractContraptionEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
		at = @At("RETURN"), remap = false, require = 1)
	private void railward$clearCurrentContraptionEntity(AbstractContraptionEntity entity, float yaw,
		float partialTicks, PoseStack poseStack, MultiBufferSource buffers, int overlay, CallbackInfo ci) {
		railward$endContraptionRender(buffers);
	}

	private void railward$beginContraptionRender(AbstractContraptionEntity entity, MultiBufferSource buffers) {
		if (buffers instanceof BufferSource bufferSource)
			bufferSource.endBatch();
		TrainCouplerContraptionRenderContext.set(entity);
		LocomotiveSpotlightForwardRenderer.setContraptionFallbackPass(true);
	}

	private void railward$endContraptionRender(MultiBufferSource buffers) {
		try {
			if (buffers instanceof BufferSource bufferSource)
				bufferSource.endBatch();
		} finally {
			TrainCouplerContraptionRenderContext.clear();
			LocomotiveSpotlightForwardRenderer.setContraptionFallbackPass(false);
		}
	}
}
*///?}
//? if !create6 {
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
//?}
