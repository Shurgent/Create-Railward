package ua.pp.shurgent.railward.mixin.create.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueRenderer;

import ua.pp.shurgent.railward.client.renderer.ModernWhistleValueBoxRenderer;

@Mixin(value = ScrollValueRenderer.class, remap = false)
public abstract class ScrollValueRendererMixin {

	@Inject(method = "tick", at = @At("RETURN"), remap = false)
	private static void railward$renderModernWhistleValueBoxes(CallbackInfo ci) {
		ModernWhistleValueBoxRenderer.tick();
	}
}
