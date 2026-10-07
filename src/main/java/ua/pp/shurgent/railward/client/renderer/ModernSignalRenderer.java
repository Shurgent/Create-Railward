package ua.pp.shurgent.railward.client.renderer;

import ua.pp.shurgent.railward.client.compat.RailwardTransformStack;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.trains.signal.SignalBlockEntity.SignalState;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import com.simibubi.create.foundation.render.CachedBufferer;
import com.simibubi.create.foundation.render.RenderTypes;
import com.simibubi.create.foundation.utility.AnimationTickHolder;

import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.state.BlockState;
import ua.pp.shurgent.railward.AllPartialModels;
import ua.pp.shurgent.railward.block.train.signal.ModernSignalBlock;
import ua.pp.shurgent.railward.block.train.signal.ModernSignalBlockEntity;

public class ModernSignalRenderer extends SmartBlockEntityRenderer<ModernSignalBlockEntity> {

	public ModernSignalRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	protected void renderSafe(ModernSignalBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffers,
			int light, int overlay) {

		BlockState state = be.getBlockState();
		int rot16 = state.getValue(ModernSignalBlock.ROTATION);
		float angle = rot16 * 360f / 16f;

		ms.pushPose();

		RailwardTransformStack.cast(ms).centre().rotateY(-angle).unCentre();

		// Center model
		RailwardTransformStack.cast(ms).translate(0.5, 0, 0.5);
		
		// Always render the base model
		CachedBufferer.partial(AllPartialModels.SIGNAL_BASE, state)
			.light(light).renderInto(ms, buffers.getBuffer(RenderType.solid()));

		// Dynamic lamps
		SignalState signal = be.signalState;
		float renderTime = AnimationTickHolder.getRenderTime(be.getLevel());

		boolean red = false;
		boolean yellow = false;
		boolean green = false;

		if (signal != null) {
			red = signal.isRedLight(renderTime);
			yellow = signal.isYellowLight(renderTime);
			green = signal.isGreenLight(renderTime);

			if (red) {
				CachedBufferer.partial(AllPartialModels.SIGNAL_RED_LAMP, state)
					.light(LightTexture.FULL_BRIGHT)
					.disableDiffuse()
					.renderInto(ms, buffers.getBuffer(RenderType.translucent()));
				CachedBufferer.partial(AllPartialModels.SIGNAL_RED_GLOW, state)
					.light(LightTexture.FULL_BRIGHT)
					.disableDiffuse()
					.renderInto(ms, buffers.getBuffer(RenderTypes.getAdditive()));
			} else if (yellow) {
				CachedBufferer.partial(AllPartialModels.SIGNAL_YELLOW_LAMP, state)
					.light(LightTexture.FULL_BRIGHT)
					.disableDiffuse()
					.renderInto(ms, buffers.getBuffer(RenderType.translucent()));
				CachedBufferer.partial(AllPartialModels.SIGNAL_YELLOW_GLOW, state)
					.light(LightTexture.FULL_BRIGHT)
					.disableDiffuse()
					.renderInto(ms, buffers.getBuffer(RenderTypes.getAdditive()));
			} else if (green) {
				CachedBufferer.partial(AllPartialModels.SIGNAL_GREEN_LAMP, state)
					.light(LightTexture.FULL_BRIGHT)
					.disableDiffuse()
					.renderInto(ms, buffers.getBuffer(RenderType.translucent()));
				CachedBufferer.partial(AllPartialModels.SIGNAL_GREEN_GLOW, state)
					.light(LightTexture.FULL_BRIGHT)
					.disableDiffuse()
					.renderInto(ms, buffers.getBuffer(RenderTypes.getAdditive()));
			}
		}

		if (!red) {
			CachedBufferer.partial(AllPartialModels.SIGNAL_RED, state)
				.light(light).renderInto(ms, buffers.getBuffer(RenderType.translucent()));
		}
		if (!yellow) {
			CachedBufferer.partial(AllPartialModels.SIGNAL_YELLOW, state)
				.light(light).renderInto(ms, buffers.getBuffer(RenderType.translucent()));
		}
		if (!green) {
			CachedBufferer.partial(AllPartialModels.SIGNAL_GREEN, state)
				.light(light).renderInto(ms, buffers.getBuffer(RenderType.translucent()));
		}

		ms.popPose();
	}

	@Override
	public int getViewDistance() {
		return 192;
	}

}
