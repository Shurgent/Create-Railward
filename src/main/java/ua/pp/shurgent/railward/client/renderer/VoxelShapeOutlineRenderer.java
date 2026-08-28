package ua.pp.shurgent.railward.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Renders every edge of a voxel shape with one uniform color. */
public final class VoxelShapeOutlineRenderer {

	private VoxelShapeOutlineRenderer() {
	}

	public static void render(PoseStack poseStack, VertexConsumer lines, VoxelShape shape,
			double offsetX, double offsetY, double offsetZ, float red, float green, float blue, float alpha) {
		PoseStack.Pose pose = poseStack.last();
		shape.forAllEdges((x1, y1, z1, x2, y2, z2) -> {
			float normalX = (float) (x2 - x1);
			float normalY = (float) (y2 - y1);
			float normalZ = (float) (z2 - z1);
			float length = Mth.sqrt(normalX * normalX + normalY * normalY + normalZ * normalZ);
			if (length == 0)
				return;
			normalX /= length;
			normalY /= length;
			normalZ /= length;

			lines.vertex(pose.pose(), (float) (x1 + offsetX), (float) (y1 + offsetY), (float) (z1 + offsetZ))
				.color(red, green, blue, alpha)
				.normal(pose.normal(), normalX, normalY, normalZ)
				.endVertex();
			lines.vertex(pose.pose(), (float) (x2 + offsetX), (float) (y2 + offsetY), (float) (z2 + offsetZ))
				.color(red, green, blue, alpha)
				.normal(pose.normal(), normalX, normalY, normalZ)
				.endVertex();
		});
	}
}
