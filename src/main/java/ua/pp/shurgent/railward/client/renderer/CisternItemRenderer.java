package ua.pp.shurgent.railward.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
//? if >=1.20.1
/*import net.minecraft.world.item.ItemDisplayContext;*/
//? if <1.20.1
import net.minecraft.client.renderer.block.model.ItemTransforms.TransformType;
import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

public class CisternItemRenderer extends BlockEntityWithoutLevelRenderer {

	private static final float GUI_SCALE = .68f;
	private static final float HAND_SCALE = .45f;

	public CisternItemRenderer() {
		super(Minecraft.getInstance()
			.getBlockEntityRenderDispatcher(), Minecraft.getInstance()
				.getEntityModels());
	}

	@Override
	public void renderByItem(ItemStack stack, TransformType transformType, PoseStack ms,
		MultiBufferSource buffers, int light, int overlay) {
		float scale = scaleFor(transformType);
		ms.pushPose();
		ms.translate(.5, .5, .5);
		ms.scale(scale, scale, scale);
		ms.translate(-.5, -.5, -.5);
		Block block = stack.getItem() instanceof BlockItem blockItem ? blockItem.getBlock() : null;
		CisternRenderer.renderCisternModel(ms, buffers, light, Direction.Axis.X, 1, 1,
			CisternRenderer.textureForBlock(block));
		ms.popPose();
	}

	private float scaleFor(TransformType transformType) {
		return switch (transformType) {
			case FIRST_PERSON_LEFT_HAND, FIRST_PERSON_RIGHT_HAND, THIRD_PERSON_LEFT_HAND, THIRD_PERSON_RIGHT_HAND ->
				HAND_SCALE;
			default -> GUI_SCALE;
		};
	}
}
