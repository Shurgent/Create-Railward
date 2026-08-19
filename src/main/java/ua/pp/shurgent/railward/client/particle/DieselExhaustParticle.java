package ua.pp.shurgent.railward.client.particle;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.Mth;
import ua.pp.shurgent.railward.particle.DieselExhaustParticleData;
import ua.pp.shurgent.railward.particle.DieselExhaustParticleData.Profile;

public class DieselExhaustParticle extends TextureSheetParticle {

	private static final int DECELERATION_TICKS = 60;
	private static final int FADE_START_TICK = 140;
	private static final int TOTAL_LIFETIME = 200;
	private static final int TEXTURE_COUNT = 5;
	private static final ParticleRenderType TRANSLUCENT_NO_DEPTH_WRITE = new ParticleRenderType() {
		@Override
		public void begin(BufferBuilder builder, TextureManager textureManager) {
			RenderSystem.depthMask(false);
			RenderSystem.setShader(GameRenderer::getParticleShader);
			RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_PARTICLES);
			RenderSystem.enableBlend();
			RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
				GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
			builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
		}

		@Override
		public void end(Tesselator tessellator) {
			tessellator.end();
			RenderSystem.depthMask(true);
		}

		@Override
		public String toString() {
			return "RAILWARD_DIESEL_EXHAUST_TRANSLUCENT";
		}
	};

	private final Profile profile;
	private final double initialX;
	private final double initialY;
	private final double initialZ;
	private final double terminalX;
	private final double terminalY;
	private final double terminalZ;
	private final float initialSpeed;
	private final float startingSize;
	private final float baseRotationSpeed;

	protected DieselExhaustParticle(ClientLevel level, DieselExhaustParticleData data, double x, double y,
		double z, double xSpeed, double ySpeed, double zSpeed, SpriteSet sprites) {
		super(level, x, y, z, 0, 0, 0);
		profile = data.profile();
		initialX = xSpeed;
		initialY = ySpeed;
		initialZ = zSpeed;
		initialSpeed = (float) Math.sqrt(xSpeed * xSpeed + ySpeed * ySpeed + zSpeed * zSpeed);
		terminalX = xSpeed * .035;
		terminalY = .006 + Math.max(0, ySpeed) * .025;
		terminalZ = zSpeed * .035;
		startingSize = .24f + Mth.clamp(initialSpeed * 1.4f, 0, .34f);
		float randomRotation = random.nextFloat() * 2 - 1;
		baseRotationSpeed = randomRotation * .072f * profile.rotationMultiplier;

		lifetime = TOTAL_LIFETIME;
		hasPhysics = true;
		friction = 1;
		gravity = 0;
		quadSize = startingSize;
		setSize(.18f, .18f);
		setColor(profile.red, profile.green, profile.blue);
		setAlpha(profile.alpha);
		roll = oRoll = random.nextFloat() * Mth.TWO_PI;
		setSprite(sprites.get(random.nextInt(TEXTURE_COUNT), TEXTURE_COUNT));
	}

	@Override
	public ParticleRenderType getRenderType() {
		return TRANSLUCENT_NO_DEPTH_WRITE;
	}

	@Override
	public void tick() {
		xo = x;
		yo = y;
		zo = z;
		oRoll = roll;
		if (age++ >= lifetime) {
			remove();
			return;
		}

		float decelerationProgress = Mth.clamp(age / (float) DECELERATION_TICKS, 0, 1);
		float retainedInitialSpeed = 1 - decelerationProgress;
		retainedInitialSpeed *= retainedInitialSpeed;
		xd = Mth.lerp(retainedInitialSpeed, terminalX, initialX);
		yd = Mth.lerp(retainedInitialSpeed, terminalY, initialY);
		zd = Mth.lerp(retainedInitialSpeed, terminalZ, initialZ);
		move(xd, yd, zd);

		float lifeProgress = age / (float) lifetime;
		float rapidSizeProgress = 1 - retainedInitialSpeed;
		float speedSize = Mth.clamp(initialSpeed * 3.5f, .25f, 1.15f);
		quadSize = startingSize + profile.rapidGrowth * speedSize * rapidSizeProgress
			+ profile.slowGrowth * lifeProgress;

		float currentSpeed = (float) Math.sqrt(xd * xd + yd * yd + zd * zd);
		float speedRatio = initialSpeed <= 1.0E-4f ? 0 : Mth.clamp(currentSpeed / initialSpeed, 0, 1);
		roll += baseRotationSpeed * (.14f + .86f * speedRatio);

		if (age >= FADE_START_TICK) {
			float fade = Mth.clamp((age - FADE_START_TICK) / (float) (lifetime - FADE_START_TICK), 0, 1);
			float smoothFade = fade * fade * (3 - 2 * fade);
			setAlpha(profile.alpha * (1 - smoothFade));
		}
	}

	public static class Factory implements ParticleProvider<DieselExhaustParticleData> {
		private final SpriteSet sprites;

		public Factory(SpriteSet sprites) {
			this.sprites = sprites;
		}

		@Override
		public Particle createParticle(DieselExhaustParticleData data, ClientLevel level, double x, double y,
			double z, double xSpeed, double ySpeed, double zSpeed) {
			return new DieselExhaustParticle(level, data, x, y, z, xSpeed, ySpeed, zSpeed, sprites);
		}
	}
}
