package ua.pp.shurgent.railward.mixin.compat.embeddium;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.resources.ResourceLocation;
import ua.pp.shurgent.railward.client.renderer.LocomotiveSpotlightForwardShaderPatcher;

@Pseudo
@Mixin(targets = "me.jellysquid.mods.sodium.client.gl.shader.ShaderLoader", remap = false)
public class ShaderLoaderMixin {

	@Inject(method = "getShaderSource", at = @At("RETURN"), cancellable = true, remap = false)
	private static void railward$patchTerrainShader(ResourceLocation location,
		CallbackInfoReturnable<String> cir) {
		cir.setReturnValue(LocomotiveSpotlightForwardShaderPatcher.patchEmbeddium(location, cir.getReturnValue()));
	}
}
