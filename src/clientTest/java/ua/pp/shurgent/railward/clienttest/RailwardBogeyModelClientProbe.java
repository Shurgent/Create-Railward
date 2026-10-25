package ua.pp.shurgent.railward.clienttest;

import java.util.ArrayList;
import java.util.List;
//? if >=1.19.2 {
/*import net.minecraft.util.RandomSource;
*///?} else {
import java.util.Random;
//?}

import com.jozufozu.flywheel.core.PartialModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.logging.LogUtils;
//? if create6 {
/*import com.simibubi.create.foundation.render.SpecialModels;
*///?}

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ua.pp.shurgent.railward.AllPartialModels;
import ua.pp.shurgent.railward.Railward;
import ua.pp.shurgent.railward.client.renderer.TripleAxleEqualizedLocomotiveBogeyRenderer;

/** Opt-in, separate-client OBJ baking/renderer probe; never included in release jars. */
@Mod.EventBusSubscriber(modid = Railward.MOD_ID, value = Dist.CLIENT)
public final class RailwardBogeyModelClientProbe {
	private static final int FRAME_QUADS = 3788;
	private static final int WHEEL_QUADS = 200;
	private static final int FRAME_VERTICES = FRAME_QUADS * 4;
	private static boolean complete;
	private static int ticks;

	private RailwardBogeyModelClientProbe() {
	}

	@SubscribeEvent
	public static void onClientTick(TickEvent.ClientTickEvent event) {
		if (complete || event.phase != TickEvent.Phase.END || !Boolean.getBoolean("railward.clientProbe")
			|| !"bogey_models".equals(System.getProperty("railward.clientProbeMode")))
			return;
		Minecraft minecraft = Minecraft.getInstance();
		require(++ticks <= 20 * 120, "timed out waiting for the dedicated server/model checks");
		if (minecraft.level == null || minecraft.player == null)
			return;
		checkModel(AllPartialModels.BOGEY_TRIPLE_AXLE_EQUALIZED_LOCOMOTIVE_FRAME, FRAME_QUADS);
		checkModel(AllPartialModels.BOGEY_TRIPLE_AXLE_EQUALIZED_LOCOMOTIVE_WHEELSET, WHEEL_QUADS);

		var still = render(0, 0, false);
		var turning = render(90, 0, true);
		var lit = render(0, 0x00f000f0, true);
		require(still.vertices.size() == (FRAME_QUADS + 3 * WHEEL_QUADS) * 4, "renderer lost geometry");
		require(turning.vertices.size() == still.vertices.size(), "rotated geometry count changed");
		require(lit.vertices.size() == still.vertices.size(), "lit geometry count changed");
		for (int i = 0; i < still.vertices.size(); i++) {
			Vertex a = still.vertices.get(i);
			Vertex b = turning.vertices.get(i);
			require(a.light == 0 && lit.vertices.get(i).light == 0x00f000f0, "world light was overridden");
			if (i < FRAME_VERTICES) {
				near(a.x, b.x); near(a.y, b.y); near(a.z, b.z);
				near(a.nx, b.nx); near(a.ny, b.ny); near(a.nz, b.nz);
			} else {
				int wheel = (i - FRAME_VERTICES) / (WHEEL_QUADS * 4);
				double centerY = 13.5 / 16.0;
				double centerZ = (wheel - 1) * 2;
				near(a.x, b.x);
				near(centerY - (a.z - centerZ), b.y);
				near(centerZ + (a.y - centerY), b.z);
				near(a.nx, b.nx); near(-a.nz, b.ny); near(a.ny, b.nz);
			}
		}
		// Check the separate Flywheel 1 model-building path, not just the fallback.
		// This does not claim GPU backend or human visual/animation verification.
		//? if create6 {
		/*checkVisualModel(AllPartialModels.BOGEY_TRIPLE_AXLE_EQUALIZED_LOCOMOTIVE_FRAME, FRAME_VERTICES);
		checkVisualModel(AllPartialModels.BOGEY_TRIPLE_AXLE_EQUALIZED_LOCOMOTIVE_WHEELSET, WHEEL_QUADS * 4);
		*///?}
		complete = true;
		LogUtils.getLogger().info("Railward BOGEY-MODEL client probe passed: frame={} quads, wheelset={} quads, "
			+ "all outward normals/UVs/shading, three centered wheel rotations and dark/bright renderer light",
			FRAME_QUADS, WHEEL_QUADS);
		minecraft.stop();
	}

	private static void checkModel(PartialModel partial, int expectedQuads) {
		BakedModel model = partial.get();
		require(model != null && model != Minecraft.getInstance().getModelManager().getMissingModel(),
			"missing model " + location(partial));
		//? if >=1.19.2 {
		/*var random = RandomSource.create(42);
		*///?} else {
		var random = new Random(42);
		//?}
		List<BakedQuad> quads = model.getQuads(null, null, random);
		require(quads.size() == expectedQuads, "OBJ face loss/culling: " + location(partial) + ": " + quads.size());
		for (BakedQuad quad : quads) {
			require(quad.isShade(), "unshaded OBJ surface");
			require(quad.getSprite() == model.getParticleIcon(), "unexpected/missing atlas material");
			int[] data = quad.getVertices();
			int stride = data.length / 4;
			for (int i = 0; i < 4; i++) {
				int base = i * stride;
				require(data[base + 6] == 0, "emissive OBJ surface");
				float u = Float.intBitsToFloat(data[base + 4]);
				float v = Float.intBitsToFloat(data[base + 5]);
				require(u >= quad.getSprite().getU0() - 1e-6 && u <= quad.getSprite().getU1() + 1e-6
					&& v >= quad.getSprite().getV0() - 1e-6 && v <= quad.getSprite().getV1() + 1e-6,
					"UV outside atlas sprite");
			}
			double[] a = position(data, 0, stride);
			double[] b = position(data, 1, stride);
			double[] c = position(data, 2, stride);
			double nx = (b[1]-a[1])*(c[2]-a[2]) - (b[2]-a[2])*(c[1]-a[1]);
			double ny = (b[2]-a[2])*(c[0]-a[0]) - (b[0]-a[0])*(c[2]-a[2]);
			double nz = (b[0]-a[0])*(c[1]-a[1]) - (b[1]-a[1])*(c[0]-a[0]);
			double length = Math.sqrt(nx*nx + ny*ny + nz*nz);
			require(length > 1e-9, "degenerate OBJ face");
			for (int i = 0; i < 4; i++) {
				int packed = data[i*stride + 7];
				double dot = nx*(byte)packed/127.0 + ny*(byte)(packed>>8)/127.0 + nz*(byte)(packed>>16)/127.0;
				require(dot / length > .98, "inward/mismatched OBJ normal");
			}
		}
	}

	private static double[] position(int[] data, int index, int stride) {
		int base = index * stride;
		return new double[] {Float.intBitsToFloat(data[base]), Float.intBitsToFloat(data[base+1]),
			Float.intBitsToFloat(data[base+2])};
	}

	private static String location(PartialModel partial) {
		//? if create6 {
		/*return partial.modelLocation().toString();
		*///?} else {
		return partial.getLocation().toString();
		//?}
	}

	private static Capture render(float angle, int light, boolean inContraption) {
		Capture capture = new Capture();
		var renderer = new TripleAxleEqualizedLocomotiveBogeyRenderer();
		//? if create6 {
		/*renderer.render(new CompoundTag(), angle, 0, new PoseStack(), type -> capture,
			light, OverlayTexture.NO_OVERLAY, inContraption);
		*///?} else {
		renderer.render(new CompoundTag(), angle, new PoseStack(), light, capture, inContraption);
		//?}
		return capture;
	}

	//? if create6 {
	/*private static void checkVisualModel(PartialModel partial, int vertices) {
		var model = SpecialModels.smoothLit(partial);
		int count = model.meshes().stream().mapToInt(mesh -> mesh.mesh().vertexCount()).sum();
		require(count == vertices, "Flywheel visual lost geometry");
		require(model.meshes().stream().allMatch(mesh -> mesh.material().light() != null),
			"Flywheel visual has no world-light shader");
	}
	*///?}

	private static void near(double expected, double actual) {
		require(Double.isFinite(actual) && Math.abs(expected - actual) < 1e-4, "renderer transform drift");
	}

	private static void require(boolean valid, String message) {
		if (!valid) throw new IllegalStateException("Railward BOGEY-MODEL: " + message);
	}

	private record Vertex(double x, double y, double z, float nx, float ny, float nz, int light) {
	}

	private static final class Capture implements VertexConsumer {
		private final List<Vertex> vertices = new ArrayList<>();
		private double x, y, z;
		private float nx, ny, nz;
		private int light;
		@Override public VertexConsumer vertex(double x, double y, double z) {
			this.x=x; this.y=y; this.z=z; return this;
		}
		@Override public VertexConsumer normal(float x, float y, float z) {
			nx=x; ny=y; nz=z; return this;
		}
		@Override public VertexConsumer uv2(int u, int v) { light=u | v<<16; return this; }
		@Override public void endVertex() { vertices.add(new Vertex(x, y, z, nx, ny, nz, light)); }
		@Override public VertexConsumer color(int r, int g, int b, int a) { return this; }
		@Override public VertexConsumer uv(float u, float v) { return this; }
		@Override public VertexConsumer overlayCoords(int u, int v) { return this; }
		@Override public void defaultColor(int r, int g, int b, int a) { }
		@Override public void unsetDefaultColor() { }
	}
}
