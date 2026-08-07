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
import ua.pp.shurgent.railward.AllPartialModels.LocomotiveSpotlightPartialModels;
import ua.pp.shurgent.railward.block.train.parts.LocomotiveBufferLightsTrainState;
import ua.pp.shurgent.railward.block.train.parts.LocomotiveBufferLightsTrainState.DirectionState;
import ua.pp.shurgent.railward.block.train.parts.LocomotiveSpotlightVariant;
import ua.pp.shurgent.railward.block.train.parts.LocomotiveSpotlightMovementBehaviour;

public class LocomotiveSpotlightRenderHelper {

	private static final float PANEL_ATTACHMENT_OFFSET = -3f / 16f;

	private LocomotiveSpotlightRenderHelper() {
	}

	public static void render(BlockState state, LocomotiveSpotlightVariant variant, Direction facing,
		float normalOffset, PoseStack ms, MultiBufferSource buffers, int light) {
		render(state, variant, getMountedFacingRotation(facing), normalOffset, isActive(facing.getOpposite()), ms,
			buffers, light, false);
	}

	public static void renderPanelAttachment(BlockState state, LocomotiveSpotlightVariant variant, Direction facing,
		PoseStack ms, MultiBufferSource buffers, int light) {
		render(state, variant, getPanelFacingRotation(facing), PANEL_ATTACHMENT_OFFSET, isActive(facing), ms, buffers,
			light, false);
	}

	public static void renderPanelAttachmentGhost(BlockState state, LocomotiveSpotlightVariant variant,
		Direction facing, PoseStack ms, MultiBufferSource buffers) {
		render(state, variant, getPanelFacingRotation(facing), PANEL_ATTACHMENT_OFFSET, false, ms, buffers,
			LightTexture.FULL_BRIGHT, true);
	}

	private static void render(BlockState state, LocomotiveSpotlightVariant variant, float rotation,
		float normalOffset, boolean active, PoseStack ms, MultiBufferSource buffers, int light, boolean ghost) {
		LocomotiveSpotlightPartialModels models = resolveModels(variant);

		ms.pushPose();
		TransformStack.cast(ms).centre()
			.rotateY(rotation)
			.unCentre();
		ms.translate(0, 0, normalOffset);

		if (ghost) {
			renderGhostPart(models.base(), state, ms, buffers);
			renderGhostPart(models.lampOff(), state, ms, buffers);
			renderGhostPart(models.lensOff(), state, ms, buffers);
			ms.popPose();
			return;
		}

		renderSolid(models.base(), state, ms, buffers, light);
		if (active) {
			renderActivePart(models.lampOn(), state, ms, buffers, RenderType.translucent());
			renderActivePart(models.lensOff(), state, ms, buffers, RenderType.translucent());
			renderActivePart(models.glow(), state, ms, buffers, RenderTypes.getAdditive());
		} else {
			CachedBufferer.partial(models.lampOff(), state)
				.light(light)
				.renderInto(ms, buffers.getBuffer(RenderType.translucent()));
			CachedBufferer.partial(models.lensOff(), state)
				.light(light)
				.renderInto(ms, buffers.getBuffer(RenderType.translucent()));
		}

		ms.popPose();
	}

	private static boolean isActive(Direction blockFacing) {
		AbstractContraptionEntity context = TrainCouplerContraptionRenderContext.get();
		if (!(context instanceof CarriageContraptionEntity carriageEntity))
			return false;
		if (!(carriageEntity.getContraption() instanceof CarriageContraption carriageContraption))
			return false;
		if (LocomotiveSpotlightMovementBehaviour.isDisabled(carriageContraption))
			return false;

		DirectionState direction = LocomotiveBufferLightsTrainState.resolve(carriageEntity);
		if (direction == DirectionState.NONE)
			return false;

		Direction activeForward = direction == DirectionState.FORWARD ? carriageContraption.getAssemblyDirection()
			: carriageContraption.getAssemblyDirection().getOpposite();
		return blockFacing == activeForward;
	}

	private static void renderActivePart(PartialModel model, BlockState state, PoseStack ms,
		MultiBufferSource buffers, RenderType renderType) {
		CachedBufferer.partial(model, state)
			.light(LightTexture.FULL_BRIGHT)
			.disableDiffuse()
			.renderInto(ms, buffers.getBuffer(renderType));
	}

	private static void renderSolid(PartialModel model, BlockState state, PoseStack ms, MultiBufferSource buffers,
		int light) {
		CachedBufferer.partial(model, state)
			.light(light)
			.renderInto(ms, buffers.getBuffer(RenderType.solid()));
	}

	private static void renderGhostPart(PartialModel model, BlockState state, PoseStack ms,
		MultiBufferSource buffers) {
		CachedBufferer.partial(model, state)
			.light(LightTexture.FULL_BRIGHT)
			.color(255, 255, 255, 96)
			.renderInto(ms, buffers.getBuffer(RenderType.translucent()));
	}

	private static LocomotiveSpotlightPartialModels resolveModels(LocomotiveSpotlightVariant variant) {
		return variant == LocomotiveSpotlightVariant.LARGE ? AllPartialModels.LOCOMOTIVE_SPOTLIGHT_LARGE
			: AllPartialModels.LOCOMOTIVE_SPOTLIGHT_SMALL;
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
}
