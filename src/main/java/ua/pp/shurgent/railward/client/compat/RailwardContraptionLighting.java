package ua.pp.shurgent.railward.client.compat;

import com.jozufozu.flywheel.core.virtual.VirtualRenderWorld;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ContraptionMatrices;
//? if !create6
import com.simibubi.create.content.contraptions.render.ContraptionRenderDispatcher;
import com.simibubi.create.foundation.render.SuperByteBuffer;
//? if create6 {
/*import net.minecraft.client.renderer.LevelRenderer;
*///?}

/** Applies contraption-world lighting through the Create 5 or Create 6 API. */
public final class RailwardContraptionLighting {

	private RailwardContraptionLighting() {
	}

	public static SuperByteBuffer apply(SuperByteBuffer buffer, MovementContext context,
		VirtualRenderWorld renderWorld, ContraptionMatrices matrices) {
		//? if create6
		/*return buffer.light(LevelRenderer.getLightColor(renderWorld, context.localPos))
			.useLevelLight(context.world, matrices.getWorld());
		*///? if !create6
		return buffer.light(matrices.getWorld(), ContraptionRenderDispatcher.getContraptionWorldLight(context, renderWorld));
	}
}
