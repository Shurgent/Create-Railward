package ua.pp.shurgent.railward.client.renderer;

import com.jozufozu.flywheel.core.PartialModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import com.simibubi.create.foundation.render.CachedBufferer;
import com.simibubi.create.foundation.render.RenderTypes;

import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import ua.pp.shurgent.railward.AllPartialModels;
import ua.pp.shurgent.railward.block.train.parts.CabSignalAspect;
import ua.pp.shurgent.railward.block.train.parts.CabSignalTrainState;

public final class CabSignalRenderHelper {

	private static final double MODEL_CENTER_X = 1 / 16d;
	private static final double MODEL_CENTER_Y = 3.5 / 16d;

	private CabSignalRenderHelper() {
	}

	public static void render(BlockState state, Direction facing, Vec3 anchor, PoseStack ms,
		MultiBufferSource buffers, int light) {
		render(state, facing, anchor, resolveAspect(), ms, buffers, light, false);
	}

	public static void renderGhost(BlockState state, Direction facing, Vec3 anchor, PoseStack ms,
		MultiBufferSource buffers) {
		render(state, facing, anchor, CabSignalAspect.OFF, ms, buffers, LightTexture.FULL_BRIGHT, true);
	}

	private static void render(BlockState state, Direction facing, Vec3 anchor, CabSignalAspect aspect,
		PoseStack ms, MultiBufferSource buffers, int light, boolean ghost) {
		ms.pushPose();
		ms.translate(anchor.x, anchor.y, anchor.z);
		rotateFromSouth(ms, facing);
		ms.translate(-MODEL_CENTER_X, -MODEL_CENTER_Y, 0);

		if (ghost) {
			renderGhostPart(AllPartialModels.CAB_SIGNAL_BASE, state, ms, buffers);
			for (int y = 2; y <= 6; y++)
				renderOffLamp(state, y, ms, buffers, LightTexture.FULL_BRIGHT, true);
			ms.popPose();
			return;
		}

		renderSolid(AllPartialModels.CAB_SIGNAL_BASE, state, ms, buffers, light);
		renderLamp(state, CabSignalAspect.WHITE, aspect, AllPartialModels.CAB_SIGNAL_WHITE, 2, ms, buffers, light);
		renderLamp(state, CabSignalAspect.RED, aspect, AllPartialModels.CAB_SIGNAL_RED, 3, ms, buffers, light);
		renderLamp(state, CabSignalAspect.YELLOW_RED, aspect, AllPartialModels.CAB_SIGNAL_YELLOW_RED, 4, ms,
			buffers, light);
		renderLamp(state, CabSignalAspect.YELLOW, aspect, AllPartialModels.CAB_SIGNAL_YELLOW, 5, ms, buffers,
			light);
		renderLamp(state, CabSignalAspect.GREEN, aspect, AllPartialModels.CAB_SIGNAL_GREEN, 6, ms, buffers,
			light);
		ms.popPose();
	}

	private static void renderLamp(BlockState state, CabSignalAspect lamp, CabSignalAspect aspect,
		PartialModel activeModel, int y, PoseStack ms, MultiBufferSource buffers, int light) {
		if (lamp != aspect) {
			renderOffLamp(state, y, ms, buffers, light, false);
			return;
		}
		CachedBufferer.partial(activeModel, state)
			.light(LightTexture.FULL_BRIGHT)
			.disableDiffuse()
			.renderInto(ms, buffers.getBuffer(RenderType.translucent()));
		CachedBufferer.partial(activeModel, state)
			.light(LightTexture.FULL_BRIGHT)
			.disableDiffuse()
			.renderInto(ms, buffers.getBuffer(RenderTypes.getAdditive()));
	}

	private static void renderOffLamp(BlockState state, int y, PoseStack ms, MultiBufferSource buffers, int light,
		boolean ghost) {
		ms.pushPose();
		ms.translate(0, (y - 6) / 16d, 0);
		if (ghost)
			renderGhostPart(AllPartialModels.CAB_SIGNAL_OFF, state, ms, buffers);
		else
			CachedBufferer.partial(AllPartialModels.CAB_SIGNAL_OFF, state)
				.light(light)
				.renderInto(ms, buffers.getBuffer(RenderType.translucent()));
		ms.popPose();
	}

	private static CabSignalAspect resolveAspect() {
		AbstractContraptionEntity context = TrainCouplerContraptionRenderContext.get();
		if (!(context instanceof CarriageContraptionEntity carriageEntity))
			return CabSignalAspect.OFF;
		return CabSignalTrainState.resolve(carriageEntity);
	}

	private static void rotateFromSouth(PoseStack ms, Direction facing) {
		switch (facing) {
		case NORTH -> ms.mulPose(Vector3f.YP.rotationDegrees(180));
		case EAST -> ms.mulPose(Vector3f.YP.rotationDegrees(90));
		case WEST -> ms.mulPose(Vector3f.YP.rotationDegrees(-90));
		case UP -> ms.mulPose(Vector3f.XP.rotationDegrees(-90));
		case DOWN -> ms.mulPose(Vector3f.XP.rotationDegrees(90));
		default -> {
		}
		}
	}

	private static void renderSolid(PartialModel model, BlockState state, PoseStack ms, MultiBufferSource buffers,
		int light) {
		CachedBufferer.partial(model, state).light(light).renderInto(ms, buffers.getBuffer(RenderType.solid()));
	}

	private static void renderGhostPart(PartialModel model, BlockState state, PoseStack ms,
		MultiBufferSource buffers) {
		CachedBufferer.partial(model, state)
			.light(LightTexture.FULL_BRIGHT)
			.color(255, 255, 255, 96)
			.renderInto(ms, buffers.getBuffer(RenderType.translucent()));
	}
}
