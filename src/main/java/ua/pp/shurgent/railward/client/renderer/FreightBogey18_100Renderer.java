package ua.pp.shurgent.railward.client.renderer;

//? if create6 {
/*import static ua.pp.shurgent.railward.AllPartialModels.BOGEY_18_100_FRAME;
import static ua.pp.shurgent.railward.AllPartialModels.BOGEY_18_100_WHEELSET;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.trains.bogey.BogeyRenderer;
import com.simibubi.create.foundation.render.CachedBufferer;
import com.simibubi.create.foundation.render.SuperByteBuffer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;
import ua.pp.shurgent.railward.client.compat.RailwardSuperByteBufferTransform;

public class FreightBogey18_100Renderer implements BogeyRenderer {

	private static final float FRAME_Y_OFFSET = 11.25f / 16f;
	private static final float WHEEL_Y_OFFSET = 13f / 16f;

	@Override
	public void render(CompoundTag bogeyData, float wheelAngle, float partialTick, PoseStack poseStack,
		MultiBufferSource buffer, int light, int overlay, boolean inContraption) {
		var vertexConsumer = buffer.getBuffer(RenderType.cutoutMipped());
		SuperByteBuffer frame = CachedBufferer.partial(BOGEY_18_100_FRAME, Blocks.AIR.defaultBlockState());
		RailwardSuperByteBufferTransform.of(frame).translate(0, FRAME_Y_OFFSET, 0);
		frame.light(light).overlay(overlay).renderInto(poseStack, vertexConsumer);

		for (int index = 0; index < 2; index++) {
			float z = index == 0 ? -1 : 1;
			SuperByteBuffer wheel = CachedBufferer.partial(BOGEY_18_100_WHEELSET, Blocks.AIR.defaultBlockState());
			RailwardSuperByteBufferTransform.of(wheel).translate(0, WHEEL_Y_OFFSET, z).rotateX(wheelAngle);
			wheel.light(light).overlay(overlay).renderInto(poseStack, vertexConsumer);
		}
	}
}
*///?}
//? if !create6 {
import static ua.pp.shurgent.railward.AllPartialModels.BOGEY_18_100_FRAME;
import static ua.pp.shurgent.railward.AllPartialModels.BOGEY_18_100_WHEELSET;

import com.jozufozu.flywheel.api.MaterialManager;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.content.trains.bogey.BogeyRenderer;
import com.simibubi.create.content.trains.bogey.BogeySizes;
import com.simibubi.create.content.trains.entity.CarriageBogey;

import net.minecraft.nbt.CompoundTag;

public class FreightBogey18_100Renderer extends BogeyRenderer {

	private static final float FRAME_Y_OFFSET = 11.25f / 16f;
	private static final float WHEEL_Y_OFFSET = 13f / 16f;

	@Override
	public void initialiseContraptionModelData(MaterialManager materialManager, CarriageBogey carriageBogey) {
		createModelInstance(materialManager, BOGEY_18_100_FRAME);
		createModelInstance(materialManager, BOGEY_18_100_WHEELSET, 2);
	}

	@Override
	public BogeySizes.BogeySize getSize() {
		return BogeySizes.SMALL;
	}

	@Override
	public void render(CompoundTag bogeyData, float wheelAngle, PoseStack poseStack, int light,
			VertexConsumer vertexConsumer, boolean inContraption) {
		boolean instanced = vertexConsumer == null;

		getTransform(BOGEY_18_100_FRAME, poseStack, instanced)
			.translate(0, FRAME_Y_OFFSET, 0)
			.render(poseStack, light, vertexConsumer);

		BogeyModelData[] wheelsets =
			getTransform(BOGEY_18_100_WHEELSET, poseStack, instanced, 2);
		for (int index = 0; index < wheelsets.length; index++) {
			float z = index == 0 ? -1 : 1;
			wheelsets[index]
				.translate(0, WHEEL_Y_OFFSET, z)
				.rotateX(wheelAngle)
				.render(poseStack, light, vertexConsumer);
		}
	}
}
//?}
