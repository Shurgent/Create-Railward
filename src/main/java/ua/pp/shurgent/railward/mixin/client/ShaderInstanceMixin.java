package ua.pp.shurgent.railward.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.renderer.ShaderInstance;
import ua.pp.shurgent.railward.client.renderer.LocomotiveSpotlightForwardRenderer;

@Mixin(ShaderInstance.class)
public class ShaderInstanceMixin {

	@Inject(method = { "apply", "m_173363_" }, at = @At("TAIL"), remap = false)
	private void railward$uploadSpotlights(CallbackInfo ci) {
		LocomotiveSpotlightForwardRenderer.uploadCurrentProgram(false);
	}
}
