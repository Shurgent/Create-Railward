package ua.pp.shurgent.railward.client.renderer;

import com.jozufozu.flywheel.util.transform.TransformStack;
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
import ua.pp.shurgent.railward.block.train.signal.ModernSignalGroundBlock;
import ua.pp.shurgent.railward.block.train.signal.ModernSignalGroundBlockEntity;

public class ModernSignalGroundRenderer extends SmartBlockEntityRenderer<ModernSignalGroundBlockEntity> {

	public ModernSignalGroundRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	protected void renderSafe(ModernSignalGroundBlockEntity be, float partialTicks, PoseStack ms,
			MultiBufferSource buffers, int light, int overlay) {
		
		BlockState state = be.getBlockState();
		int rot16 = state.getValue(ModernSignalGroundBlock.ROTATION);
		float angle = rot16 * 360f / 16f;

		ms.pushPose();
		// Rotate around block center
		TransformStack.cast(ms).centre().rotateY(-angle).unCentre();
		
		// Always render the base model
		CachedBufferer.partial(AllPartialModels.SIGNAL_GROUND_BASE, state)
			.light(light)
			.renderInto(ms, buffers.getBuffer(RenderType.solid()));

		// Dynamic lamps
		SignalState signal = be.signalState;
		float renderTime = AnimationTickHolder.getRenderTime(be.getLevel());
		
		boolean blue = false;
		boolean white = false;
		
		if (signal != null) {
			blue = signal.isRedLight(renderTime);
			white = signal.isGreenLight(renderTime) || signal.isYellowLight(renderTime);

			if (blue) {
				CachedBufferer.partial(AllPartialModels.SIGNAL_GROUND_BLUE_LAMP, state)
					.light(LightTexture.FULL_BRIGHT)
					.disableDiffuse()
					.renderInto(ms, buffers.getBuffer(RenderType.translucent()));
				CachedBufferer.partial(AllPartialModels.SIGNAL_GROUND_BLUE_GLOW, state)
					.light(LightTexture.FULL_BRIGHT)
					.disableDiffuse()
					.renderInto(ms, buffers.getBuffer(RenderTypes.getAdditive()));
			} else if (white) {
				CachedBufferer.partial(AllPartialModels.SIGNAL_GROUND_WHITE_LAMP, state)
					.light(LightTexture.FULL_BRIGHT)
					.disableDiffuse()
					.renderInto(ms, buffers.getBuffer(RenderType.translucent()));
				CachedBufferer.partial(AllPartialModels.SIGNAL_GROUND_WHITE_GLOW, state)
					.light(LightTexture.FULL_BRIGHT)
					.disableDiffuse()
					.renderInto(ms, buffers.getBuffer(RenderTypes.getAdditive()));
			}
		}
		
		if (!blue) {
			CachedBufferer.partial(AllPartialModels.SIGNAL_GROUND_BLUE, state)
				.light(light)
				.renderInto(ms, buffers.getBuffer(RenderType.translucent()));
		}
		if (!white) {
			CachedBufferer.partial(AllPartialModels.SIGNAL_GROUND_WHITE, state)
				.light(light)
				.renderInto(ms, buffers.getBuffer(RenderType.translucent()));
		}
		
		ms.popPose();
	}
	
	@Override
	public int getViewDistance() {
		return 192;
	}

}
