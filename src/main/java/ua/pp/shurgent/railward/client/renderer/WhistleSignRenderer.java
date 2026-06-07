package ua.pp.shurgent.railward.client.renderer;

import com.jozufozu.flywheel.util.transform.TransformStack;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import com.simibubi.create.content.trains.track.ITrackBlock;
import com.simibubi.create.content.trains.track.TrackTargetingBehaviour;
import com.simibubi.create.content.trains.track.TrackTargetingBehaviour.RenderedTrackOverlayType;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import com.simibubi.create.foundation.render.CachedBufferer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import ua.pp.shurgent.railward.AllPartialModels;
import ua.pp.shurgent.railward.block.train.signal.WhistleSignBlock;
import ua.pp.shurgent.railward.block.train.signal.WhistleSignBlockEntity;
import ua.pp.shurgent.railward.block.train.signal.WhistleSignEdgePoint;

public class WhistleSignRenderer extends SmartBlockEntityRenderer<WhistleSignBlockEntity> {

	private static final float TEXT_SCALE = 1 / 24f;

	public WhistleSignRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	protected void renderSafe(WhistleSignBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffers,
		int light, int overlay) {

		BlockState state = be.getBlockState();
		int rot16 = state.getValue(WhistleSignBlock.ROTATION);
		float angle = rot16 * 360f / 16f;

		ms.pushPose();
		TransformStack.cast(ms)
			.centre()
			.rotateY(-angle)
			.unCentre();

		CachedBufferer.partial(AllPartialModels.WHISTLE_SIGN, state)
			.light(light)
			.renderInto(ms, buffers.getBuffer(RenderType.cutoutMipped()));
		renderLetters(ms, buffers, light);
		ms.popPose();

		renderTrackOverlay(be, ms, buffers, light, overlay);
	}

	private void renderLetters(PoseStack ms, MultiBufferSource buffers, int light) {
		String letter = I18n.get("railward.whistle_sign.letter");
		Font font = Minecraft.getInstance().font;
		renderFrontLetter(ms, buffers, font, letter, light);
	}

	private void renderFrontLetter(PoseStack ms, MultiBufferSource buffers, Font font, String letter, int light) {
		ms.pushPose();
		ms.translate(7.5 / 16d, 21 / 16d, 5.95 / 16d);
		ms.mulPose(Vector3f.YP.rotationDegrees(180));
		ms.scale(TEXT_SCALE, -TEXT_SCALE, TEXT_SCALE);
		font.drawInBatch(letter, -font.width(letter) / 2f, -4f, 0x101010, false, ms.last()
			.pose(), buffers, false, 0, LightTexture.FULL_BRIGHT);
		ms.popPose();
	}

	private void renderTrackOverlay(WhistleSignBlockEntity be, PoseStack ms, MultiBufferSource buffers, int light,
		int overlay) {
		BlockPos pos = be.getBlockPos();
		TrackTargetingBehaviour<WhistleSignEdgePoint> target = be.edgePoint;
		BlockPos targetPosition = target.getGlobalPosition();
		Level level = be.getLevel();
		BlockState trackState = level.getBlockState(targetPosition);
		Block block = trackState.getBlock();

		if (!(block instanceof ITrackBlock))
			return;

		ms.pushPose();
		TransformStack.cast(ms)
			.translate(targetPosition.subtract(pos));
		WhistleSignEdgePoint edgePoint = target.getEdgePoint();
		RenderedTrackOverlayType type = edgePoint != null && edgePoint.hasSignsOnBothSides()
			? RenderedTrackOverlayType.DUAL_SIGNAL
			: RenderedTrackOverlayType.SIGNAL;
		TrackTargetingBehaviour.render(level, targetPosition, target.getTargetDirection(), target.getTargetBezier(), ms,
			buffers, light, overlay, type, 1);
		ms.popPose();
	}

	@Override
	public int getViewDistance() {
		return 192;
	}
}
