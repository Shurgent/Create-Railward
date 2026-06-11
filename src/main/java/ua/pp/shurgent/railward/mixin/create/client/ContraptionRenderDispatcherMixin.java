package ua.pp.shurgent.railward.mixin.create.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.render.ContraptionRenderDispatcher;

import net.minecraft.client.renderer.MultiBufferSource;
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
	}
}
