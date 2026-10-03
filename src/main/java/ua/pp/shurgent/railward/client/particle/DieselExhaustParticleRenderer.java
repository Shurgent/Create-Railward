package ua.pp.shurgent.railward.client.particle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
//? if >=1.20.1
/*import net.minecraftforge.client.event.RenderLevelStageEvent;*/
//? if <1.20.1
import net.minecraftforge.client.event.RenderLevelLastEvent;
import ua.pp.shurgent.railward.RailwardConfig;

/**
 * Renders diesel exhaust after clouds and Create's late train coupling pass. The
 * particles deliberately do not write depth, so all opaque geometry must have
 * populated the depth buffer before the smoke is blended.
 */
public final class DieselExhaustParticleRenderer {

	private static final Set<DieselExhaustParticle> PARTICLES =
		Collections.newSetFromMap(new WeakHashMap<>());

	private DieselExhaustParticleRenderer() {
	}

	static void add(DieselExhaustParticle particle) {
		PARTICLES.add(particle);
	}

	@SuppressWarnings("removal")
	//? if >=1.20.1
	/*public static void render(RenderLevelStageEvent event) {*/
	//? if <1.20.1
	public static void render(RenderLevelLastEvent event) {
		//? if >=1.20.1 {
		/*if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_WEATHER)
			return;*/
		//?}
		Minecraft minecraft = Minecraft.getInstance();
		ClientLevel level = minecraft.level;
		if (level == null) {
			PARTICLES.clear();
			return;
		}

		PARTICLES.removeIf(particle -> !particle.isAlive() || !particle.isInLevel(level));
		int renderDistance = RailwardConfig.CLIENT.dieselExhaustParticleRenderDistance.get();
		if (PARTICLES.isEmpty() || renderDistance == 0)
			return;

		Camera camera = minecraft.gameRenderer.getMainCamera();
		Vec3 cameraPosition = camera.getPosition();
		double renderDistanceSqr = (double) renderDistance * renderDistance;
		ArrayList<DieselExhaustParticle> sortedParticles = new ArrayList<>(PARTICLES);
		sortedParticles.removeIf(particle -> particle.distanceToSqr(
			cameraPosition.x, cameraPosition.y, cameraPosition.z) > renderDistanceSqr);
		if (sortedParticles.isEmpty())
			return;
		sortedParticles.sort((first, second) -> Double.compare(
			second.distanceToSqr(cameraPosition.x, cameraPosition.y, cameraPosition.z),
			first.distanceToSqr(cameraPosition.x, cameraPosition.y, cameraPosition.z)));

		LightTexture lightTexture = minecraft.gameRenderer.lightTexture();
		lightTexture.turnOnLightLayer();
		RenderSystem.enableDepthTest();
		RenderSystem.setShaderColor(1, 1, 1, 1);
		PoseStack modelViewStack = RenderSystem.getModelViewStack();
		modelViewStack.pushPose();
		// On 1.20.1 AFTER_WEATHER runs inside LevelRenderer's world-space
		// ModelView scope. Older RenderLevelLastEvent versions still need the
		// event pose applied here.
		//? if <1.20.1
		modelViewStack.mulPoseMatrix(event.getPoseStack().last().pose());
		RenderSystem.applyModelViewMatrix();

		Tesselator tesselator = Tesselator.getInstance();
		BufferBuilder builder = tesselator.getBuilder();
		DieselExhaustParticle.TRANSLUCENT_NO_DEPTH_WRITE.begin(builder, minecraft.getTextureManager());
		for (DieselExhaustParticle particle : sortedParticles)
			particle.render(builder, camera, event.getPartialTick());
		DieselExhaustParticle.TRANSLUCENT_NO_DEPTH_WRITE.end(tesselator);

		modelViewStack.popPose();
		RenderSystem.applyModelViewMatrix();
		RenderSystem.depthMask(true);
		RenderSystem.disableBlend();
		lightTexture.turnOffLightLayer();
	}

	public static void onClientLogout(ClientPlayerNetworkEvent.LoggedOutEvent event) {
		PARTICLES.clear();
	}
}
