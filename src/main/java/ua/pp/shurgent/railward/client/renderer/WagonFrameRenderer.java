package ua.pp.shurgent.railward.client.renderer;

import com.jozufozu.flywheel.core.PartialModel;
import ua.pp.shurgent.railward.client.compat.RailwardTransformStack;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import com.simibubi.create.foundation.render.CachedBufferer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import ua.pp.shurgent.railward.AllPartialModels;
import ua.pp.shurgent.railward.block.train.parts.WagonFrameBlock;
import ua.pp.shurgent.railward.block.train.parts.WagonFrameBlockEntity;
import ua.pp.shurgent.railward.block.train.parts.WagonFrameEquipmentType;
import ua.pp.shurgent.railward.block.train.parts.WagonFrameHandBrakeValveSide;
import ua.pp.shurgent.railward.block.train.parts.WagonFrameMemberType;
import ua.pp.shurgent.railward.block.train.parts.WagonFrameSide;

public class WagonFrameRenderer extends SmartBlockEntityRenderer<WagonFrameBlockEntity> {

	public WagonFrameRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	protected void renderSafe(WagonFrameBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffers,
		int light, int overlay) {

		BlockState state = be.getBlockState();

		ms.pushPose();
		RailwardTransformStack.cast(ms).centre()
			.rotateY(getFacingRotation(state.getValue(WagonFrameBlock.FACING)))
			.unCentre();

		render(AllPartialModels.WAGON_FRAME_BASE, state, ms, buffers, light);
		for (WagonFrameSide side : WagonFrameSide.values())
			renderMember(be.getMember(side), side, state, ms, buffers, light);
		renderEquipment(be.getEquipment(), state, ms, buffers, light);
		renderHandBrakeValve(be.getHandBrakeValve(), state, ms, buffers, light);

		ms.popPose();
	}

	private void renderMember(WagonFrameMemberType member, WagonFrameSide side, BlockState state, PoseStack ms,
		MultiBufferSource buffers, int light) {
		PartialModel model = switch (member) {
		case NONE -> null;
		case THIN -> AllPartialModels.WAGON_FRAME_MEMBER_THIN;
		case THICK -> AllPartialModels.WAGON_FRAME_MEMBER_THICK;
		case TAPER_THIN_TO_THICK -> AllPartialModels.WAGON_FRAME_MEMBER_TAPER_THIN_TO_THICK;
		case TAPER_THICK_TO_THIN -> AllPartialModels.WAGON_FRAME_MEMBER_TAPER_THICK_TO_THIN;
		};
		if (model == null)
			return;

		ms.pushPose();
		RailwardTransformStack.cast(ms).translate(0.5, 0, 0.5)
			.rotateY(getSideRotation(side))
			.translate(-0.5, 0, -0.5);
		render(model, state, ms, buffers, light);
		ms.popPose();
	}

	private void renderEquipment(WagonFrameEquipmentType equipment, BlockState state, PoseStack ms,
		MultiBufferSource buffers, int light) {
		PartialModel model = switch (equipment) {
		case NONE -> null;
		case RECEIVER -> AllPartialModels.WAGON_FRAME_RECEIVER;
		case BRAKE_CYLINDER -> AllPartialModels.WAGON_FRAME_BRAKE_CYLINDER;
		case DISTRIBUTOR -> AllPartialModels.WAGON_FRAME_DISTRIBUTOR;
		case BRAKE_PIPE -> AllPartialModels.WAGON_FRAME_BRAKE_PIPE;
		case FUEL_TANK -> AllPartialModels.WAGON_FRAME_FUEL_TANK;
		};
		if (model != null)
			render(model, state, ms, buffers, light);
	}

	private void renderHandBrakeValve(WagonFrameHandBrakeValveSide side, BlockState state, PoseStack ms,
		MultiBufferSource buffers, int light) {
		if (side == WagonFrameHandBrakeValveSide.NONE)
			return;

		ms.pushPose();
		if (side == WagonFrameHandBrakeValveSide.RIGHT) {
			RailwardTransformStack.cast(ms).translate(0.5, 0, 0.5)
				.rotateY(180)
				.translate(-0.5, 0, -0.5);
		}
		render(AllPartialModels.WAGON_FRAME_HAND_BRAKE_VALVE, state, ms, buffers, light);
		ms.popPose();
	}

	private void render(PartialModel model, BlockState state, PoseStack ms, MultiBufferSource buffers, int light) {
		CachedBufferer.partial(model, state)
			.light(light)
			.renderInto(ms, buffers.getBuffer(RenderType.solid()));
	}

	private float getFacingRotation(Direction facing) {
		return switch (facing) {
		case EAST -> -90;
		case SOUTH -> 180;
		case WEST -> 90;
		default -> 0;
		};
	}

	private float getSideRotation(WagonFrameSide side) {
		return switch (side) {
		case FRONT -> 0;
		case RIGHT -> -90;
		case BACK -> 180;
		case LEFT -> 90;
		};
	}
}
