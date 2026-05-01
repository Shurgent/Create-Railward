package ua.pp.shurgent.railward.block.train.station;

import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

final class UnloadingTransferEffects {

	private UnloadingTransferEffects() {
	}

	static void emit(Level level, Vec3 source, Vec3 target, ItemStack stack) {
		if (!(level instanceof ServerLevel serverLevel) || stack.isEmpty())
			return;

		ItemStack particleStack = stack.copy();
		particleStack.setCount(1);
		int itemParticles = Mth.clamp(stack.getCount() / 8 + 2, 3, 12);
		Vec3 direction = target.subtract(source);
		direction = direction.lengthSqr() < 1.0E-4 ? new Vec3(0, -1, 0) : direction.normalize();

		serverLevel.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, particleStack), source.x, source.y,
			source.z, itemParticles, .18, .08, .18, .04);

		int dustParticles = Mth.clamp(stack.getCount() / 16 + 3, 4, 10);
		Vec3 dustSource = source.add(0, -.45, 0);
		for (int i = 0; i < dustParticles; i++)
			sendDirectedDustParticle(serverLevel, dustSource, direction);
	}

	private static void sendDirectedDustParticle(ServerLevel level, Vec3 source, Vec3 direction) {
		Vec3 origin = source.add(randomSpread(level, .18), randomSpread(level, .03), randomSpread(level, .18));
		Vec3 velocity = direction.scale(.07 + level.random.nextDouble() * .035)
			.add(randomSpread(level, .02), -level.random.nextDouble() * .01, randomSpread(level, .02));
		level.sendParticles(ParticleTypes.CLOUD, origin.x, origin.y, origin.z, 0, velocity.x, velocity.y, velocity.z,
			1);
	}

	private static double randomSpread(ServerLevel level, double radius) {
		return (level.random.nextDouble() * 2 - 1) * radius;
	}
}
