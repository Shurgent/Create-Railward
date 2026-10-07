package ua.pp.shurgent.railward.mixin.flywheel;

//? if create6 {
/*import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import ua.pp.shurgent.railward.client.renderer.LocomotiveSpotlightForwardRenderer;

@Pseudo
@Mixin(targets = "dev.engine_room.flywheel.backend.gl.shader.GlProgram", remap = false)
public class WorldProgramMixin {

	@Inject(method = "bind", at = @At("TAIL"), remap = false)
	private void railward$uploadSpotlights(CallbackInfo ci) {
		LocomotiveSpotlightForwardRenderer.uploadCurrentProgram(false);
	}
}
*///?}
//? if !create6 {
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.jozufozu.flywheel.core.shader.WorldProgram;

import ua.pp.shurgent.railward.client.renderer.LocomotiveSpotlightForwardRenderer;

@Mixin(value = WorldProgram.class, remap = false)
public class WorldProgramMixin {

	@Inject(method = "bind", at = @At("TAIL"), remap = false)
	private void railward$uploadSpotlights(CallbackInfo ci) {
		LocomotiveSpotlightForwardRenderer.uploadCurrentProgram(false);
	}
}
//?}
