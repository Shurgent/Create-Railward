package ua.pp.shurgent.railward.mixin.create.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.trains.track.BezierTrackPointLocation;
import com.simibubi.create.content.trains.track.TrackTargetingBehaviour;
import com.simibubi.create.content.trains.track.TrackTargetingBehaviour.RenderedTrackOverlayType;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.world.level.LevelAccessor;
import ua.pp.shurgent.railward.client.renderer.LocomotiveSpotlightForwardRenderer;

@Mixin(value = TrackTargetingBehaviour.class, remap = false)
public class TrackTargetingBehaviourMixin {

	@Inject(method = "render", at = @At("RETURN"), remap = false)
	private static void railward$flushTrackOverlay(LevelAccessor level, BlockPos pos, AxisDirection direction,
		BezierTrackPointLocation bezier, PoseStack ms, MultiBufferSource buffer, int light, int overlay,
		RenderedTrackOverlayType type, float scale, CallbackInfo ci) {
		if (level != Minecraft.getInstance().level || !(buffer instanceof MultiBufferSource.BufferSource buffers))
			return;

		boolean previousPass = LocomotiveSpotlightForwardRenderer.isContraptionFallbackPass();
		LocomotiveSpotlightForwardRenderer.setContraptionFallbackPass(true);
		try {
			buffers.endBatch(RenderType.cutoutMipped());
		} finally {
			LocomotiveSpotlightForwardRenderer.setContraptionFallbackPass(previousPass);
		}
	}
}
