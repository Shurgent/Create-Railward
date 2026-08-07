package ua.pp.shurgent.railward.client.renderer;

import com.jozufozu.flywheel.core.PartialModel;
import com.jozufozu.flywheel.util.transform.TransformStack;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.trains.entity.CarriageContraption;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import com.simibubi.create.foundation.render.CachedBufferer;
import com.simibubi.create.foundation.render.RenderTypes;

import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import ua.pp.shurgent.railward.AllPartialModels;
import ua.pp.shurgent.railward.AllPartialModels.LocomotiveBufferLightsPartialModels;
import ua.pp.shurgent.railward.block.train.parts.LocomotiveBufferLightsTrainState;
import ua.pp.shurgent.railward.block.train.parts.LocomotiveBufferLightsTrainState.DirectionState;
import ua.pp.shurgent.railward.block.train.parts.LocomotiveBufferLightsVariant;

public class LocomotiveBufferLightsRenderHelper {

	public static final float PANEL_ATTACHMENT_OFFSET = -3f / 16f;

	private LocomotiveBufferLightsRenderHelper() {
	}

	public static void render(BlockState state, LocomotiveBufferLightsVariant variant, Direction facing,
		float normalOffset, PoseStack ms, MultiBufferSource buffers, int light) {
		render(state, variant, getMountedFacingRotation(facing), normalOffset, resolveLampState(facing.getOpposite()),
			ms, buffers, light, false);
	}

	public static void renderGhost(BlockState state, LocomotiveBufferLightsVariant variant, Direction facing,
		float normalOffset, PoseStack ms, MultiBufferSource buffers) {
		render(state, variant, getMountedFacingRotation(facing), normalOffset, LampState.OFF, ms, buffers,
			LightTexture.FULL_BRIGHT, true);
	}

	public static void renderPanelAttachment(BlockState state, LocomotiveBufferLightsVariant variant, Direction facing,
		PoseStack ms, MultiBufferSource buffers, int light) {
		render(state, variant, getPanelFacingRotation(facing), PANEL_ATTACHMENT_OFFSET, resolveLampState(facing), ms,
			buffers, light, false);
	}

	public static void renderPanelAttachmentGhost(BlockState state, LocomotiveBufferLightsVariant variant,
		Direction facing, PoseStack ms, MultiBufferSource buffers) {
		render(state, variant, getPanelFacingRotation(facing), PANEL_ATTACHMENT_OFFSET, LampState.OFF, ms, buffers,
			LightTexture.FULL_BRIGHT, true);
	}

	private static void render(BlockState state, LocomotiveBufferLightsVariant variant, float rotation,
		float normalOffset, LampState lampState, PoseStack ms, MultiBufferSource buffers, int light, boolean ghost) {
		LocomotiveBufferLightsPartialModels models = resolveModels(variant);

		ms.pushPose();
		TransformStack.cast(ms).centre()
			.rotateY(rotation)
			.unCentre();
		ms.translate(0, 0, normalOffset);

		if (ghost) {
			renderGhostPart(models.base(), state, ms, buffers);
			renderGhostPart(models.whiteLampOff(), state, ms, buffers);
			renderGhostPart(models.whiteLensOff(), state, ms, buffers);
			if (!variant.hasSharedLampPositions()) {
				renderGhostPart(models.redLampOff(), state, ms, buffers);
				renderGhostPart(models.redLensOff(), state, ms, buffers);
			}
			ms.popPose();
			return;
		}

		renderSolid(models.base(), state, ms, buffers, light);
		if (variant.hasSharedLampPositions())
			renderSharedLamps(lampState, models, state, ms, buffers, light);
		else {
			renderLamp(lampState == LampState.WHITE, models.whiteLampOn(), models.whiteLampOff(), models.whiteLensOff(),
				models.whiteGlow(), state, ms, buffers, light);
			renderLamp(lampState == LampState.RED, models.redLampOn(), models.redLampOff(), models.redLensOff(),
				models.redGlow(), state, ms, buffers, light);
		}

		ms.popPose();
	}

	private static void renderSharedLamps(LampState lampState, LocomotiveBufferLightsPartialModels models,
		BlockState state, PoseStack ms, MultiBufferSource buffers, int light) {
		switch (lampState) {
		case WHITE -> renderLamp(true, models.whiteLampOn(), models.whiteLampOff(), models.whiteLensOff(),
			models.whiteGlow(), state, ms, buffers, light);
		case RED -> renderLamp(true, models.redLampOn(), models.redLampOff(), models.redLensOff(), models.redGlow(),
			state, ms, buffers, light);
		case OFF -> renderLamp(false, models.whiteLampOn(), models.whiteLampOff(), models.whiteLensOff(),
			models.whiteGlow(), state, ms, buffers, light);
		}
	}

	private static LampState resolveLampState(Direction blockFacing) {
		AbstractContraptionEntity context = TrainCouplerContraptionRenderContext.get();
		if (!(context instanceof CarriageContraptionEntity carriageEntity))
			return LampState.OFF;
		if (!(carriageEntity.getContraption() instanceof CarriageContraption carriageContraption))
			return LampState.OFF;

		DirectionState direction = LocomotiveBufferLightsTrainState.resolve(carriageEntity);
		if (direction == DirectionState.NONE)
			return LampState.OFF;

		Direction activeForward = direction == DirectionState.FORWARD ? carriageContraption.getAssemblyDirection()
			: carriageContraption.getAssemblyDirection()
				.getOpposite();
		if (blockFacing == activeForward)
			return LampState.WHITE;
		if (blockFacing == activeForward.getOpposite())
			return LampState.RED;
		return LampState.OFF;
	}

	private static void renderLamp(boolean active, PartialModel on, PartialModel off, PartialModel lens,
		PartialModel glow, BlockState state, PoseStack ms, MultiBufferSource buffers, int light) {
		if (active) {
			CachedBufferer.partial(on, state)
				.light(LightTexture.FULL_BRIGHT)
				.disableDiffuse()
				.renderInto(ms, buffers.getBuffer(RenderType.translucent()));
			CachedBufferer.partial(lens, state)
				.light(LightTexture.FULL_BRIGHT)
				.disableDiffuse()
				.renderInto(ms, buffers.getBuffer(RenderType.translucent()));
			CachedBufferer.partial(glow, state)
				.light(LightTexture.FULL_BRIGHT)
				.disableDiffuse()
				.renderInto(ms, buffers.getBuffer(RenderTypes.getAdditive()));
			return;
		}

		CachedBufferer.partial(off, state)
			.light(light)
			.renderInto(ms, buffers.getBuffer(RenderType.translucent()));
		CachedBufferer.partial(lens, state)
			.light(light)
			.renderInto(ms, buffers.getBuffer(RenderType.translucent()));
	}

	private static void renderSolid(PartialModel model, BlockState state, PoseStack ms, MultiBufferSource buffers,
		int light) {
		CachedBufferer.partial(model, state)
			.light(light)
			.renderInto(ms, buffers.getBuffer(RenderType.solid()));
	}

	private static void renderGhostPart(PartialModel model, BlockState state, PoseStack ms, MultiBufferSource buffers) {
		CachedBufferer.partial(model, state)
			.light(LightTexture.FULL_BRIGHT)
			.color(255, 255, 255, 96)
			.renderInto(ms, buffers.getBuffer(RenderType.translucent()));
	}

	private static LocomotiveBufferLightsPartialModels resolveModels(LocomotiveBufferLightsVariant variant) {
		return switch (variant) {
		case TYPE_1 -> AllPartialModels.LOCOMOTIVE_BUFFER_LIGHTS_1;
		case TYPE_2 -> AllPartialModels.LOCOMOTIVE_BUFFER_LIGHTS_2;
		case TYPE_3 -> AllPartialModels.LOCOMOTIVE_BUFFER_LIGHTS_3;
		case TYPE_4 -> AllPartialModels.LOCOMOTIVE_BUFFER_LIGHTS_4;
		case TYPE_5 -> AllPartialModels.LOCOMOTIVE_BUFFER_LIGHTS_5;
		case TYPE_6 -> AllPartialModels.LOCOMOTIVE_BUFFER_LIGHTS_6;
		case TYPE_7 -> AllPartialModels.LOCOMOTIVE_BUFFER_LIGHTS_7;
		case TYPE_8 -> AllPartialModels.LOCOMOTIVE_BUFFER_LIGHTS_8;
		case TYPE_9 -> AllPartialModels.LOCOMOTIVE_BUFFER_LIGHTS_9;
		case TYPE_10 -> AllPartialModels.LOCOMOTIVE_BUFFER_LIGHTS_10;
		case TYPE_11 -> AllPartialModels.LOCOMOTIVE_BUFFER_LIGHTS_11;
		};
	}

	private static float getMountedFacingRotation(Direction facing) {
		return switch (facing) {
		case EAST -> -90;
		case SOUTH -> 180;
		case WEST -> 90;
		default -> 0;
		};
	}

	private static float getPanelFacingRotation(Direction facing) {
		return getMountedFacingRotation(facing.getOpposite());
	}

	private enum LampState {
		OFF,
		WHITE,
		RED
	}
}
