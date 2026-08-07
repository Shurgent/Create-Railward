package ua.pp.shurgent.railward.client.renderer;

import static ua.pp.shurgent.railward.AllPartialModels.BOGEY_TRIPLE_AXLE_EQUALIZED_LOCOMOTIVE_FRAME;
import static ua.pp.shurgent.railward.AllPartialModels.BOGEY_TRIPLE_AXLE_EQUALIZED_LOCOMOTIVE_WHEELSET;

import com.jozufozu.flywheel.api.MaterialManager;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.content.trains.bogey.BogeyRenderer;
import com.simibubi.create.content.trains.bogey.BogeySizes;
import com.simibubi.create.content.trains.entity.CarriageBogey;

import net.minecraft.nbt.CompoundTag;

public class TripleAxleEqualizedLocomotiveBogeyRenderer extends BogeyRenderer {

	private static final float FRAME_Y_OFFSET = 12f / 16f;
	private static final float WHEEL_Y_OFFSET = 13.5f / 16f;
	private static final float[] WHEELSET_Z_OFFSETS = {-2, 0, 2};

	@Override
	public void initialiseContraptionModelData(MaterialManager materialManager, CarriageBogey carriageBogey) {
		createModelInstance(materialManager, BOGEY_TRIPLE_AXLE_EQUALIZED_LOCOMOTIVE_FRAME);
		createModelInstance(materialManager, BOGEY_TRIPLE_AXLE_EQUALIZED_LOCOMOTIVE_WHEELSET, 3);
	}

	@Override
	public BogeySizes.BogeySize getSize() {
		return BogeySizes.SMALL;
	}

	@Override
	public void render(CompoundTag bogeyData, float wheelAngle, PoseStack poseStack, int light,
			VertexConsumer vertexConsumer, boolean inContraption) {
		boolean instanced = vertexConsumer == null;

		getTransform(BOGEY_TRIPLE_AXLE_EQUALIZED_LOCOMOTIVE_FRAME, poseStack, instanced)
			.translate(0, FRAME_Y_OFFSET, 0)
			.render(poseStack, light, vertexConsumer);

		BogeyModelData[] wheelsets =
			getTransform(BOGEY_TRIPLE_AXLE_EQUALIZED_LOCOMOTIVE_WHEELSET, poseStack, instanced, 3);
		for (int index = 0; index < wheelsets.length; index++) {
			wheelsets[index]
				.translate(0, WHEEL_Y_OFFSET, WHEELSET_Z_OFFSETS[index])
				.rotateX(wheelAngle)
				.render(poseStack, light, vertexConsumer);
		}
	}
}
