package ua.pp.shurgent.railward.client.particle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.Tesselator;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;

/**
 * Renders diesel exhaust after Minecraft's cloud pass. The particles deliberately
 * do not write depth, so rendering them in ParticleEngine's earlier pass would
 * allow clouds to be drawn over smoke that is closer to the camera.
 */
public final class DieselExhaustParticleRenderer {

	private static final Set<DieselExhaustParticle> PARTICLES =
		Collections.newSetFromMap(new WeakHashMap<>());

	private DieselExhaustParticleRenderer() {
	}

	static void add(DieselExhaustParticle particle) {
		PARTICLES.add(particle);
	}

	public static void render(RenderLevelStageEvent event) {
		if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_WEATHER)
			return;

		Minecraft minecraft = Minecraft.getInstance();
		ClientLevel level = minecraft.level;
		if (level == null) {
			PARTICLES.clear();
			return;
		}

		PARTICLES.removeIf(particle -> !particle.isAlive() || !particle.isInLevel(level));
		if (PARTICLES.isEmpty())
			return;

		Vec3 cameraPosition = event.getCamera().getPosition();
		ArrayList<DieselExhaustParticle> sortedParticles = new ArrayList<>(PARTICLES);
		sortedParticles.sort((first, second) -> Double.compare(
			second.distanceToSqr(cameraPosition.x, cameraPosition.y, cameraPosition.z),
			first.distanceToSqr(cameraPosition.x, cameraPosition.y, cameraPosition.z)));

		LightTexture lightTexture = minecraft.gameRenderer.lightTexture();
		lightTexture.turnOnLightLayer();
		RenderSystem.enableDepthTest();
		RenderSystem.setShaderColor(1, 1, 1, 1);

		Tesselator tesselator = Tesselator.getInstance();
		BufferBuilder builder = tesselator.getBuilder();
		DieselExhaustParticle.TRANSLUCENT_NO_DEPTH_WRITE.begin(builder, minecraft.getTextureManager());
		for (DieselExhaustParticle particle : sortedParticles) {
			if (!particle.shouldCull() || event.getFrustum().isVisible(particle.getBoundingBox()))
				particle.render(builder, event.getCamera(), event.getPartialTick());
		}
		DieselExhaustParticle.TRANSLUCENT_NO_DEPTH_WRITE.end(tesselator);

		// AFTER_WEATHER normally continues with depth writes disabled.
		RenderSystem.depthMask(false);
		RenderSystem.disableBlend();
		lightTexture.turnOffLightLayer();
	}

	public static void onClientLogout(ClientPlayerNetworkEvent.LoggedOutEvent event) {
		PARTICLES.clear();
	}
}
