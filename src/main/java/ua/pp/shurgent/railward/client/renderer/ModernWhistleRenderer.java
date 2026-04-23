package ua.pp.shurgent.railward.client.renderer;

import com.jozufozu.flywheel.util.transform.TransformStack;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import com.simibubi.create.foundation.render.CachedBufferer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import ua.pp.shurgent.railward.AllPartialModels;
import ua.pp.shurgent.railward.block.train.parts.ModernWhistleBlock;
import ua.pp.shurgent.railward.block.train.parts.ModernWhistleBlockEntity;
import ua.pp.shurgent.railward.block.train.parts.ModernWhistleSoundType;

public class ModernWhistleRenderer extends SmartBlockEntityRenderer<ModernWhistleBlockEntity> {

	public ModernWhistleRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	protected void renderSafe(ModernWhistleBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffers,
		int light, int overlay) {

		BlockState state = be.getBlockState();
		Direction attachedFace = state.getValue(ModernWhistleBlock.ATTACHED_FACE);
		int rotation = state.getValue(ModernWhistleBlock.ROTATION);
		ModernWhistleSoundType soundType = state.getValue(ModernWhistleBlock.SOUND_TYPE);

		ms.pushPose();

		TransformStack transform = TransformStack.cast(ms).centre();
		applyAttachmentRotation(transform, attachedFace);
		transform.unCentre();
		applyLocalRotation(transform, rotation);

		CachedBufferer.partial(AllPartialModels.MODERN_WHISTLE_BASE, state)
			.light(light)
			.renderInto(ms, buffers.getBuffer(RenderType.solid()));
		CachedBufferer.partial(resolveHead(soundType), state)
			.light(light)
			.renderInto(ms, buffers.getBuffer(RenderType.solid()));

		ms.popPose();
	}

	private void applyLocalRotation(TransformStack transform, int rotation) {
		transform.translate(0.5, 0, 0.5)
			.rotateY(-rotation * 90d)
			.translate(-0.5, 0, -0.5);
	}

	private void applyAttachmentRotation(TransformStack transform, Direction attachedFace) {
		switch (attachedFace) {
		case DOWN -> {
		}
		case UP -> transform.rotateX(180);
		case NORTH -> transform.rotateX(90);
		case SOUTH -> transform.rotateX(-90);
		case EAST -> transform.rotateZ(90);
		case WEST -> transform.rotateZ(-90);
		}
	}

	private com.jozufozu.flywheel.core.PartialModel resolveHead(ModernWhistleSoundType soundType) {
		return switch (soundType) {
		case HORN_1 -> AllPartialModels.MODERN_WHISTLE_HORN_1;
		case HORN_2 -> AllPartialModels.MODERN_WHISTLE_HORN_2;
		case WHISTLE -> AllPartialModels.MODERN_WHISTLE_WHISTLE;
		};
	}

}
