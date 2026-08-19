package ua.pp.shurgent.railward.network;

import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import ua.pp.shurgent.railward.particle.DieselExhaustParticleData;
import ua.pp.shurgent.railward.particle.DieselExhaustParticleData.Profile;

@OnlyIn(Dist.CLIENT)
public final class DieselExhaustParticleClientHandler {

	private DieselExhaustParticleClientHandler() {
	}

	public static void handle(int carriageEntityId, Vec3 localEmitter, Profile profile, Vec3 velocity) {
		Minecraft minecraft = Minecraft.getInstance();
		ClientLevel level = minecraft.level;
		if (level == null)
			return;

		Entity found = level.getEntity(carriageEntityId);
		if (!(found instanceof CarriageContraptionEntity entity))
			return;

		float partialTicks = minecraft.getFrameTime();
		Vec3 nozzleDirection = entity.applyRotation(new Vec3(0, 1, 0), partialTicks).normalize();
		Vec3 emitter = entity.toGlobalVector(localEmitter, partialTicks);
		Vec3 interpolatedAnchor = new Vec3(
			Mth.lerp(partialTicks, entity.xo, entity.getX()),
			Mth.lerp(partialTicks, entity.yo, entity.getY()),
			Mth.lerp(partialTicks, entity.zo, entity.getZ()));
		emitter = emitter.add(interpolatedAnchor.subtract(entity.position())).add(nozzleDirection.scale(.035));

		level.addParticle(new DieselExhaustParticleData(profile), emitter.x, emitter.y, emitter.z,
			velocity.x, velocity.y, velocity.z);
	}
}
