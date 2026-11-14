package ua.pp.shurgent.railward.mixin.create.client;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.lwjgl.opengl.GL11;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Matrix4f;
//? if create6 {
/*import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.ui.PonderUI;
*///?} else {
import com.simibubi.create.foundation.ponder.PonderScene;
import com.simibubi.create.foundation.ponder.ui.PonderUI;
//?}
//? if >=1.20.1
/*import net.minecraft.client.gui.GuiGraphics;*/
import net.minecraft.client.Minecraft;

/** Keep the offstage ends of Railward's consists inside Ponder's GUI clipping volume. */
@Mixin(value = PonderUI.class, remap = false)
public abstract class PonderUITrainDepthMixin {

	@Shadow private List<PonderScene> scenes;
	@Unique private boolean railward$extendedDepthRendered;

	@ModifyVariable(method = "renderScene", at = @At("STORE"), ordinal = 0, remap = false)
	private Matrix4f railward$extendTrainDepth(Matrix4f projection, PoseStack graphics, int mouseX,
		int mouseY, int sceneIndex, float partialTicks) {
		var id = scenes.get(sceneIndex).getId();
		if (!id.getNamespace().equals("railward") || !(id.getPath().equals("cistern_hatch")
			|| id.getPath().equals("fluid_terminal") || id.getPath().equals("bulk_transport")
			|| id.getPath().startsWith("coupler_")))
			return projection;
		railward$extendedDepthRendered = true;

		// Compress clip-space Z only. Screen position, scale, picking and depth order stay intact.
		//? if >=1.20.1 {
		/*return new Matrix4f().scaling(1, 1, .25f).mul(projection);
		*///?} else {
		Matrix4f expanded = new Matrix4f(projection);
		expanded.multiplyBackward(Matrix4f.createScaleMatrix(1, 1, .25f));
		return expanded;
		//?}
	}

	@Inject(method = "renderWidgets", at = @At("HEAD"), remap = false)
	private void railward$keepWidgetsAboveTrain(CallbackInfo ci) {
		if (!railward$extendedDepthRendered)
			return;
		// GuiGraphics render types can re-enable depth testing while drawing Ponder's widgets.
		// The scene is complete; start a fresh depth layer for the title and navigation.
		RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
		railward$extendedDepthRendered = false;
	}
}
