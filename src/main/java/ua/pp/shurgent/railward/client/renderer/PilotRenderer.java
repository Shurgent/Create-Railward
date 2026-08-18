package ua.pp.shurgent.railward.client.renderer;

import com.jozufozu.flywheel.core.PartialModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import com.simibubi.create.foundation.render.CachedBufferer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import ua.pp.shurgent.railward.AllPartialModels;
import ua.pp.shurgent.railward.block.train.parts.PilotBlock;
import ua.pp.shurgent.railward.block.train.parts.PilotBlockEntity;
import ua.pp.shurgent.railward.block.train.parts.PilotSide;
import ua.pp.shurgent.railward.block.train.parts.PilotType;

public class PilotRenderer extends SmartBlockEntityRenderer<PilotBlockEntity> {

	public PilotRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	protected void renderSafe(PilotBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffers,
			int light, int overlay) {
		BlockState state = be.getBlockState();
		if (!(state.getBlock() instanceof PilotBlock block))
			return;
		Direction facing = state.getValue(PilotBlock.FACING);
		PilotSide side = state.getValue(PilotBlock.SIDE);
		PartialModel model = getModel(block.getPilotType(), side);

		CachedBufferer.partial(model, state)
			.centre()
			.rotateY(getFacingRotation(facing))
			.unCentre()
			.light(light)
			.renderInto(ms, buffers.getBuffer(RenderType.solid()));
	}

	private PartialModel getModel(PilotType type, PilotSide side) {
		if (type == PilotType.TYPE_2)
			return side == PilotSide.LEFT ? AllPartialModels.PILOT_TYPE_2_LEFT
				: AllPartialModels.PILOT_TYPE_2_RIGHT;
		return side == PilotSide.LEFT ? AllPartialModels.PILOT_TYPE_1_LEFT
			: AllPartialModels.PILOT_TYPE_1_RIGHT;
	}

	private float getFacingRotation(Direction facing) {
		return switch (facing) {
		case EAST -> -90;
		case SOUTH -> 180;
		case WEST -> 90;
		default -> 0;
		};
	}
}
