package ua.pp.shurgent.railward.mixin.compat.embeddium;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import ua.pp.shurgent.railward.client.renderer.LocomotiveSpotlightForwardRenderer;

@Pseudo
@Mixin(targets = "me.jellysquid.mods.sodium.client.gl.shader.GlProgram", remap = false)
public class GlProgramMixin {

	@Inject(method = "bind", at = @At("TAIL"), remap = false)
	private void railward$uploadSpotlights(CallbackInfo ci) {
		LocomotiveSpotlightForwardRenderer.uploadCurrentProgram(false);
	}
}
