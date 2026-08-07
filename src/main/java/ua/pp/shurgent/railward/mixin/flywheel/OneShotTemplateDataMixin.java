package ua.pp.shurgent.railward.mixin.flywheel;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.jozufozu.flywheel.api.vertex.VertexType;
import com.jozufozu.flywheel.core.compile.OneShotTemplateData;
import com.jozufozu.flywheel.core.source.FileIndex;

import ua.pp.shurgent.railward.client.renderer.LocomotiveSpotlightForwardShaderPatcher;

@Mixin(value = OneShotTemplateData.class, remap = false)
public class OneShotTemplateDataMixin {

	@Inject(method = "generateFooter", at = @At("RETURN"), cancellable = true, remap = false)
	private void railward$addSpotlightVaryings(FileIndex shader, VertexType vertexType,
		CallbackInfoReturnable<String> cir) {
		cir.setReturnValue(LocomotiveSpotlightForwardShaderPatcher.patchFlywheelVertexFooter(cir.getReturnValue()));
	}
}
