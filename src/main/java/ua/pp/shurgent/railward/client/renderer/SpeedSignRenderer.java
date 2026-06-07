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
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import ua.pp.shurgent.railward.AllPartialModels;
import ua.pp.shurgent.railward.block.train.signal.SpeedSignBlock;
import ua.pp.shurgent.railward.block.train.signal.SpeedSignBlockEntity;
import ua.pp.shurgent.railward.block.train.signal.SpeedSignEdgePoint;

public class SpeedSignRenderer extends SmartBlockEntityRenderer<SpeedSignBlockEntity> {

	private static final float TEXT_SCALE = 1 / 32f;

	public SpeedSignRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	protected void renderSafe(SpeedSignBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffers,
		int light, int overlay) {

		BlockState state = be.getBlockState();
		int rot16 = state.getValue(SpeedSignBlock.ROTATION);
		float angle = rot16 * 360f / 16f;

		ms.pushPose();
		TransformStack.cast(ms)
			.centre()
			.rotateY(-angle)
			.unCentre();

		CachedBufferer.partial(AllPartialModels.SPEED_SIGN, state)
			.light(light)
			.renderInto(ms, buffers.getBuffer(RenderType.cutoutMipped()));
		renderSpeedLimit(be, ms, buffers);
		ms.popPose();

		renderTrackOverlay(be, ms, buffers, light, overlay);
	}

	private void renderSpeedLimit(SpeedSignBlockEntity be, PoseStack ms, MultiBufferSource buffers) {
		String speed = Integer.toString(be.getSpeedLimitKmh());
		Font font = Minecraft.getInstance().font;

		ms.pushPose();
		ms.translate(8 / 16d, 20 / 16d, 5.9 / 16d);
		ms.mulPose(Vector3f.YP.rotationDegrees(180));
		ms.scale(TEXT_SCALE, -TEXT_SCALE, TEXT_SCALE);
		font.drawInBatch(speed, -font.width(speed) / 2f, -4f, 0x101010, false, ms.last()
			.pose(), buffers, false, 0, LightTexture.FULL_BRIGHT);
		ms.popPose();
	}

	private void renderTrackOverlay(SpeedSignBlockEntity be, PoseStack ms, MultiBufferSource buffers, int light,
		int overlay) {
		BlockPos pos = be.getBlockPos();
		TrackTargetingBehaviour<SpeedSignEdgePoint> target = be.edgePoint;
		BlockPos targetPosition = target.getGlobalPosition();
		Level level = be.getLevel();
		BlockState trackState = level.getBlockState(targetPosition);
		Block block = trackState.getBlock();

		if (!(block instanceof ITrackBlock))
			return;

		ms.pushPose();
		TransformStack.cast(ms)
			.translate(targetPosition.subtract(pos));
		SpeedSignEdgePoint edgePoint = target.getEdgePoint();
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
