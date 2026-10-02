package ua.pp.shurgent.railward.client.renderer;

import java.util.List;

import com.jozufozu.flywheel.util.transform.TransformStack;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
//? if >=1.20.1
/*import net.minecraft.world.item.ItemDisplayContext;*/
//? if <1.20.1
import net.minecraft.client.renderer.block.model.ItemTransforms.TransformType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.level.Level;
import ua.pp.shurgent.railward.block.train.station.UnloadingFallingItemVisual;

final class UnloadingFallingItemRenderer {

	private static final float ITEM_SCALE = .5f;

	private UnloadingFallingItemRenderer() {
	}

	static void render(List<UnloadingFallingItemVisual> items, Level level, float partialTicks, PoseStack ms,
		MultiBufferSource buffer, int light, int overlay) {
		if (items.isEmpty() || level == null)
			return;

		ItemRenderer itemRenderer = Minecraft.getInstance()
			.getItemRenderer();
		long gameTime = level.getGameTime();

		for (UnloadingFallingItemVisual item : items) {
			float age = gameTime - item.startTick() + partialTicks;
			if (age < 0)
				continue;

			float startY = item.startY();
			float endY = item.endY();
			float fall = .5f * UnloadingFallingItemVisual.GRAVITY * age * age;
			float y = Math.max(endY, startY - fall);

			ms.pushPose();
			ms.translate(.5 + item.offsetX(), y, .5 + item.offsetZ());
			ms.scale(ITEM_SCALE, ITEM_SCALE, ITEM_SCALE);
			TransformStack.cast(ms)
				.rotateY(item.yaw());
			itemRenderer.renderStatic(item.stack(), TransformType.GROUND, light, overlay, ms, buffer, 0);
			ms.popPose();
		}
	}
}
