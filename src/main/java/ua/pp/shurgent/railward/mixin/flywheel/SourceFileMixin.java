package ua.pp.shurgent.railward.mixin.flywheel;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.jozufozu.flywheel.core.source.SourceFile;

import ua.pp.shurgent.railward.client.renderer.LocomotiveSpotlightForwardShaderPatcher;

@Mixin(value = SourceFile.class, remap = false)
public class SourceFileMixin {

	@ModifyVariable(method = "<init>", at = @At("HEAD"), argsOnly = true, ordinal = 0)
	private static String railward$patchBlockFragmentShader(String source) {
		return LocomotiveSpotlightForwardShaderPatcher.patchFlywheelSource(source);
	}
}
