package ua.pp.shurgent.railward.mixin.client;

import java.io.InputStream;
import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Group;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.mojang.blaze3d.preprocessor.GlslPreprocessor;
import com.mojang.blaze3d.shaders.Program;

import ua.pp.shurgent.railward.client.renderer.LocomotiveSpotlightForwardShaderPatcher;

@Mixin(Program.class)
public class ProgramMixin {

	@Group(name = "railward$patchTerrainShader", min = 1, max = 1)
	@Redirect(method = { "compileShaderInternal", "m_166612_" },
		at = @At(value = "INVOKE",
			target = "Lcom/mojang/blaze3d/preprocessor/GlslPreprocessor;process(Ljava/lang/String;)Ljava/util/List;"),
		remap = false, require = 0)
	private static List<String> railward$patchTerrainShaderDev(GlslPreprocessor instance, String source,
		Program.Type type, String name, InputStream input, String sourceName, GlslPreprocessor preprocessor) {
		return railward$patchTerrainShader(instance, source, type, name);
	}

	@Group(name = "railward$patchTerrainShader", min = 1, max = 1)
	@Redirect(method = { "compileShaderInternal", "m_166612_" },
		at = @At(value = "INVOKE",
			target = "Lcom/mojang/blaze3d/preprocessor/GlslPreprocessor;m_166461_(Ljava/lang/String;)Ljava/util/List;"),
		remap = false, require = 0)
	private static List<String> railward$patchTerrainShaderSrg(GlslPreprocessor instance, String source,
		Program.Type type, String name, InputStream input, String sourceName, GlslPreprocessor preprocessor) {
		return railward$patchTerrainShader(instance, source, type, name);
	}

	private static List<String> railward$patchTerrainShader(GlslPreprocessor instance, String source,
		Program.Type type, String name) {
		return instance.process(LocomotiveSpotlightForwardShaderPatcher.patchVanilla(type, name, source));
	}
}
