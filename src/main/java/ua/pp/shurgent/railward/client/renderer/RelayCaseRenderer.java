package ua.pp.shurgent.railward.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.trains.track.ITrackBlock;
import com.simibubi.create.content.trains.track.TrackTargetingBehaviour;
import com.simibubi.create.content.trains.track.TrackTargetingBehaviour.RenderedTrackOverlayType;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import ua.pp.shurgent.railward.block.train.signal.RelayCaseBlockEntity;
import ua.pp.shurgent.railward.block.train.signal.RelayCaseEdgePoint;
import ua.pp.shurgent.railward.block.train.signal.RelayCaseEdgePoint.TrackMarker;
import ua.pp.shurgent.railward.client.compat.RailwardTransformStack;

public class RelayCaseRenderer extends SmartBlockEntityRenderer<RelayCaseBlockEntity> {
	public RelayCaseRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	protected void renderSafe(RelayCaseBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffers, int light,
		int overlay) {
		TrackMarker marker = be.getTrackMarker();
		if (marker == TrackMarker.HIDDEN)
			return;
		BlockPos pos = be.getBlockPos();
		TrackTargetingBehaviour<RelayCaseEdgePoint> target = be.edgePoint;
		BlockPos targetPosition = target.getGlobalPosition();
		Level level = be.getLevel();
		BlockState trackState = level.getBlockState(targetPosition);
		Block block = trackState.getBlock();

		if (!(block instanceof ITrackBlock))
			return;

		ms.pushPose();
		RailwardTransformStack.cast(ms)
			.translate(targetPosition.subtract(pos));
		RenderedTrackOverlayType type = marker == TrackMarker.DOUBLE
			? RenderedTrackOverlayType.DUAL_SIGNAL
			: RenderedTrackOverlayType.SIGNAL;
		TrackTargetingBehaviour.render(level, targetPosition, target.getTargetDirection(), target.getTargetBezier(), ms,
			buffers, light, overlay, type, 1);
		ms.popPose();
	}

}
