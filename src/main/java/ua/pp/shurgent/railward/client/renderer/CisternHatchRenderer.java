package ua.pp.shurgent.railward.client.renderer;

import com.jozufozu.flywheel.core.virtual.VirtualRenderWorld;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ContraptionMatrices;
import com.simibubi.create.content.contraptions.render.ContraptionRenderDispatcher;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import com.simibubi.create.foundation.render.CachedBufferer;
import com.simibubi.create.foundation.render.SuperByteBuffer;
import com.simibubi.create.foundation.utility.AnimationTickHolder;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import ua.pp.shurgent.railward.AllPartialModels;
import ua.pp.shurgent.railward.block.train.station.CisternHatchBlock;
import ua.pp.shurgent.railward.block.train.station.CisternHatchBlockEntity;
import ua.pp.shurgent.railward.block.train.station.CisternHatchMovement;

public class CisternHatchRenderer extends SafeBlockEntityRenderer<CisternHatchBlockEntity> {

	private static final Vec3 LID_PIVOT = new Vec3(.5, 3.5 / 16f, 3 / 16f);
	private static final float OPEN_ANGLE = -80;

	public CisternHatchRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	protected void renderSafe(CisternHatchBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
		int light, int overlay) {
		BlockState state = be.getBlockState();
		VertexConsumer vb = buffer.getBuffer(RenderType.solid());
		SuperByteBuffer lid = CachedBufferer.partial(AllPartialModels.CISTERN_HATCH_LID, state);
		transformLid(lid, state, be.getLidProgress(partialTicks))
			.light(light)
			.renderInto(ms, vb);
	}

	public static void renderInContraption(MovementContext context, VirtualRenderWorld renderWorld,
		ContraptionMatrices matrices, MultiBufferSource buffer) {
		BlockState state = context.state;
		VertexConsumer vb = buffer.getBuffer(RenderType.solid());
		float partialTicks = AnimationTickHolder.getPartialTicks();
		float progress = CisternHatchMovement.getAnimation(context)
			.getValue(partialTicks);

		SuperByteBuffer lid = CachedBufferer.partial(AllPartialModels.CISTERN_HATCH_LID, state);
		transformLid(lid, state, progress)
			.light(matrices.getWorld(), ContraptionRenderDispatcher.getContraptionWorldLight(context, renderWorld))
			.renderInto(matrices.getViewProjection(), vb);
	}

	private static SuperByteBuffer transformLid(SuperByteBuffer lid, BlockState state, float progress) {
		Direction facing = state.hasProperty(CisternHatchBlock.FACING) ? state.getValue(CisternHatchBlock.FACING)
			: Direction.NORTH;
		float eased = progress * progress;
		return lid
			.centre()
			.rotateY(blockStateRotation(facing))
			.unCentre()
			.translate(LID_PIVOT)
			.rotateX(OPEN_ANGLE * eased)
			.translateBack(LID_PIVOT);
	}

	private static int blockStateRotation(Direction facing) {
		return switch (facing) {
		case SOUTH -> 180;
		case WEST -> 90;
		case EAST -> 270;
		default -> 0;
		};
	}
}
