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
		Vec3 flow = target.subtract(source);
		Vec3 middle = source.add(flow.scale(.55));

		serverLevel.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, particleStack), source.x, source.y,
			source.z, itemParticles, .18, .08, .18, .04);
		serverLevel.sendParticles(ParticleTypes.CLOUD, middle.x, middle.y, middle.z, 4, .22, .12, .22, .015);
	}
}
