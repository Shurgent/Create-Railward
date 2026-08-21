package ua.pp.shurgent.railward.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.foundation.render.SuperByteBuffer;

final class ContraptionDiffuseLighting {

	private ContraptionDiffuseLighting() {}

	static SuperByteBuffer apply(SuperByteBuffer buffer, float partialTicks) {
		AbstractContraptionEntity entity = TrainCouplerContraptionRenderContext.get();
		if (entity == null)
			return buffer;

		// Special block entities receive the contraption transform in the external
		// PoseStack. SuperByteBuffer intentionally ignores that stack for diffuse
		// normals, so prepend only the contraption's model normal here. Using
		// fullNormalTransform() would also include the camera/view transform.
		PoseStack contraptionTransform = new PoseStack();
		entity.applyLocalTransforms(contraptionTransform, partialTicks);
		buffer.mulNormal(contraptionTransform.last()
			.normal());
		return buffer;
	}
}
