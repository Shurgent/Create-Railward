package ua.pp.shurgent.railward.client.renderer;

import com.jozufozu.flywheel.core.PartialModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import com.simibubi.create.foundation.render.CachedBufferer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.state.BlockState;
import ua.pp.shurgent.railward.AllPartialModels;
import ua.pp.shurgent.railward.block.train.parts.RailingBlock;
import ua.pp.shurgent.railward.block.train.parts.RailingBlockEntity;
import ua.pp.shurgent.railward.block.train.parts.RailingPart;

public class RailingRenderer extends SmartBlockEntityRenderer<RailingBlockEntity> {

	private static final double EDGE_OFFSET = 14d / 16d;
	private static final double UPPER_OFFSET = 7d / 16d;

	public RailingRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	protected void renderSafe(RailingBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffers,
			int light, int overlay) {
		BlockState state = be.getBlockState();
		if (!(state.getBlock() instanceof RailingBlock block))
			return;

		int color = block.getRenderColor();

		renderVertical(be, RailingPart.NORTH_WEST_LOWER, 0, 0, 0, state, color, ms, buffers, light);
		renderVertical(be, RailingPart.NORTH_WEST_UPPER, 0, UPPER_OFFSET, 0, state, color, ms, buffers, light);
		renderVertical(be, RailingPart.NORTH_EAST_LOWER, EDGE_OFFSET, 0, 0, state, color, ms, buffers, light);
		renderVertical(be, RailingPart.NORTH_EAST_UPPER, EDGE_OFFSET, UPPER_OFFSET, 0, state, color, ms, buffers,
			light);
		renderVertical(be, RailingPart.SOUTH_WEST_LOWER, 0, 0, EDGE_OFFSET, state, color, ms, buffers, light);
		renderVertical(be, RailingPart.SOUTH_WEST_UPPER, 0, UPPER_OFFSET, EDGE_OFFSET, state, color, ms, buffers,
			light);
		renderVertical(be, RailingPart.SOUTH_EAST_LOWER, EDGE_OFFSET, 0, EDGE_OFFSET, state, color, ms, buffers,
			light);
		renderVertical(be, RailingPart.SOUTH_EAST_UPPER, EDGE_OFFSET, UPPER_OFFSET, EDGE_OFFSET, state, color, ms,
			buffers, light);

		renderHorizontalX(be, RailingPart.NORTH_BOTTOM, 0, 0, state, color, ms, buffers, light);
		renderHorizontalX(be, RailingPart.NORTH_MIDDLE, 7d / 16d, 0, state, color, ms, buffers, light);
		renderHorizontalX(be, RailingPart.NORTH_TOP, 14d / 16d, 0, state, color, ms, buffers, light);
		renderHorizontalX(be, RailingPart.SOUTH_BOTTOM, 0, EDGE_OFFSET, state, color, ms, buffers, light);
		renderHorizontalX(be, RailingPart.SOUTH_MIDDLE, 7d / 16d, EDGE_OFFSET, state, color, ms, buffers, light);
		renderHorizontalX(be, RailingPart.SOUTH_TOP, 14d / 16d, EDGE_OFFSET, state, color, ms, buffers, light);

		renderHorizontalZ(be, RailingPart.WEST_BOTTOM, 0, 0, state, color, ms, buffers, light);
		renderHorizontalZ(be, RailingPart.WEST_MIDDLE, 0, 7d / 16d, state, color, ms, buffers, light);
		renderHorizontalZ(be, RailingPart.WEST_TOP, 0, 14d / 16d, state, color, ms, buffers, light);
		renderHorizontalZ(be, RailingPart.EAST_BOTTOM, EDGE_OFFSET, 0, state, color, ms, buffers, light);
		renderHorizontalZ(be, RailingPart.EAST_MIDDLE, EDGE_OFFSET, 7d / 16d, state, color, ms, buffers, light);
		renderHorizontalZ(be, RailingPart.EAST_TOP, EDGE_OFFSET, 14d / 16d, state, color, ms, buffers, light);

		renderConnectorColumn(be, 0, 0,
			RailingPart.NORTH_WEST_LOWER, RailingPart.NORTH_WEST_UPPER,
			RailingPart.NORTH_BOTTOM, RailingPart.NORTH_MIDDLE, RailingPart.NORTH_TOP,
			RailingPart.WEST_BOTTOM, RailingPart.WEST_MIDDLE, RailingPart.WEST_TOP,
			state, color, ms, buffers, light);
		renderConnectorColumn(be, EDGE_OFFSET, 0,
			RailingPart.NORTH_EAST_LOWER, RailingPart.NORTH_EAST_UPPER,
			RailingPart.NORTH_BOTTOM, RailingPart.NORTH_MIDDLE, RailingPart.NORTH_TOP,
			RailingPart.EAST_BOTTOM, RailingPart.EAST_MIDDLE, RailingPart.EAST_TOP,
			state, color, ms, buffers, light);
		renderConnectorColumn(be, 0, EDGE_OFFSET,
			RailingPart.SOUTH_WEST_LOWER, RailingPart.SOUTH_WEST_UPPER,
			RailingPart.SOUTH_BOTTOM, RailingPart.SOUTH_MIDDLE, RailingPart.SOUTH_TOP,
			RailingPart.WEST_BOTTOM, RailingPart.WEST_MIDDLE, RailingPart.WEST_TOP,
			state, color, ms, buffers, light);
		renderConnectorColumn(be, EDGE_OFFSET, EDGE_OFFSET,
			RailingPart.SOUTH_EAST_LOWER, RailingPart.SOUTH_EAST_UPPER,
			RailingPart.SOUTH_BOTTOM, RailingPart.SOUTH_MIDDLE, RailingPart.SOUTH_TOP,
			RailingPart.EAST_BOTTOM, RailingPart.EAST_MIDDLE, RailingPart.EAST_TOP,
			state, color, ms, buffers, light);
	}

	private void renderVertical(RailingBlockEntity be, RailingPart part, double x, double y, double z,
			BlockState state, int color, PoseStack ms, MultiBufferSource buffers, int light) {
		if (be.isEnabled(part))
			render(AllPartialModels.RAILING_VERTICAL_CORE, x, y, z, state, color, ms, buffers, light);
	}

	private void renderHorizontalX(RailingBlockEntity be, RailingPart part, double y, double z,
			BlockState state, int color, PoseStack ms, MultiBufferSource buffers, int light) {
		if (be.isEnabled(part))
			render(AllPartialModels.RAILING_HORIZONTAL_X_CORE, 0, y, z, state, color, ms, buffers, light);
	}

	private void renderHorizontalZ(RailingBlockEntity be, RailingPart part, double x, double y,
			BlockState state, int color, PoseStack ms, MultiBufferSource buffers, int light) {
		if (be.isEnabled(part))
			render(AllPartialModels.RAILING_HORIZONTAL_Z_CORE, x, y, 0, state, color, ms, buffers, light);
	}

	private void renderConnectorColumn(RailingBlockEntity be, double x, double z,
			RailingPart lowerVertical, RailingPart upperVertical,
			RailingPart sideBottom, RailingPart sideMiddle, RailingPart sideTop,
			RailingPart crossBottom, RailingPart crossMiddle, RailingPart crossTop,
			BlockState state, int color, PoseStack ms, MultiBufferSource buffers, int light) {
		boolean lower = be.isEnabled(lowerVertical);
		boolean upper = be.isEnabled(upperVertical);
		if (lower || be.isEnabled(sideBottom) || be.isEnabled(crossBottom))
			render(AllPartialModels.RAILING_CONNECTOR, x, 0, z, state, color, ms, buffers, light);

		boolean middle = be.isEnabled(sideMiddle) || be.isEnabled(crossMiddle);
		if (middle || lower && upper) {
			render(AllPartialModels.RAILING_CONNECTOR, x, 7d / 16d, z, state, color, ms, buffers, light);
		} else if (lower) {
			render(AllPartialModels.RAILING_CONNECTOR_HALF, x, 7d / 16d, z, state, color, ms, buffers, light);
		} else if (upper) {
			render(AllPartialModels.RAILING_CONNECTOR_HALF, x, 8d / 16d, z, state, color, ms, buffers, light);
		}

		if (upper || be.isEnabled(sideTop) || be.isEnabled(crossTop))
			render(AllPartialModels.RAILING_CONNECTOR, x, 14d / 16d, z, state, color, ms, buffers, light);
	}

	private void render(PartialModel model, double x, double y, double z, BlockState state, int color,
			PoseStack ms, MultiBufferSource buffers, int light) {
		CachedBufferer.partial(model, state)
			.translate(x, y, z)
			.color(color >> 16 & 255, color >> 8 & 255, color & 255, 255)
			.light(light)
			.renderInto(ms, buffers.getBuffer(RenderType.solid()));
	}
}
