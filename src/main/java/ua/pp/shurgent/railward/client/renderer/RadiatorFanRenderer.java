package ua.pp.shurgent.railward.client.renderer;

import com.jozufozu.flywheel.core.PartialModel;
import com.jozufozu.flywheel.util.transform.TransformStack;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import com.simibubi.create.foundation.render.CachedBufferer;
import com.simibubi.create.foundation.utility.AnimationTickHolder;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import ua.pp.shurgent.railward.AllPartialModels;
import ua.pp.shurgent.railward.block.train.parts.RadiatorFanBlock;
import ua.pp.shurgent.railward.block.train.parts.RadiatorFanBlockEntity;

public class RadiatorFanRenderer extends SmartBlockEntityRenderer<RadiatorFanBlockEntity> {

	private static final float DEGREES_PER_TICK = 54;

	public RadiatorFanRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	protected void renderSafe(RadiatorFanBlockEntity be, float partialTicks, PoseStack ms,
			MultiBufferSource buffers, int light, int overlay) {
		BlockState state = be.getBlockState();
		float angle = (AnimationTickHolder.getRenderTime(be.getLevel()) * DEGREES_PER_TICK) % 360;

		ms.pushPose();
		TransformStack.cast(ms)
			.translate(0, state.getValue(RadiatorFanBlock.HALF) == Half.TOP ? .5 : 0, 0)
			.centre()
			.rotateY(getFacingRotation(state.getValue(RadiatorFanBlock.FACING)))
			.unCentre();

		render(AllPartialModels.RADIATOR_FAN_BASE, state, ms, buffers, light);

		ms.pushPose();
		TransformStack.cast(ms).centre()
			.rotateY(angle)
			.unCentre();
		render(AllPartialModels.RADIATOR_FAN_ROTOR, state, ms, buffers, light);
		ms.popPose();

		ms.popPose();
	}

	private void render(PartialModel model, BlockState state, PoseStack ms,
			MultiBufferSource buffers, int light) {
		CachedBufferer.partial(model, state)
			.light(light)
			.fullNormalTransform()
			.renderInto(ms, buffers.getBuffer(RenderType.cutoutMipped()));
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
