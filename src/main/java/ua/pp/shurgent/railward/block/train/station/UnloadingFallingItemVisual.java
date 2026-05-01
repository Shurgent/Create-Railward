package ua.pp.shurgent.railward.block.train.station;

import java.util.Random;

import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

public record UnloadingFallingItemVisual(ItemStack stack, long startTick, int distance, float offsetX, float offsetZ,
	float yaw) {

	public static final int SPAWN_INTERVAL_TICKS = 2;
	public static final int MAX_ITEMS_PER_TRANSFER = 6;
	public static final int MAX_ACTIVE_ITEMS = 72;
	public static final float GRAVITY = .018f;

	private static final float XZ_OFFSET = .33f;

	public static int itemCountFor(ItemStack stack) {
		if (stack.isEmpty())
			return 0;
		return stack.getCount() == 1 ? 1 : Mth.clamp(stack.getCount(), 2, MAX_ITEMS_PER_TRANSFER);
	}

	public static UnloadingFallingItemVisual create(ItemStack stack, long startTick, int distance, Random random) {
		ItemStack visualStack = stack.copy();
		visualStack.setCount(1);
		return new UnloadingFallingItemVisual(visualStack, startTick, Mth.clamp(distance, 1, 3),
			randomOffset(random), randomOffset(random), random.nextFloat() * 360f);
	}

	public boolean isFinished(long gameTime) {
		return gameTime > startTick + getFallDurationTicks(distance) + 2;
	}

	public static int getFallDurationTicks(int distance) {
		float fallDistance = Mth.clamp(distance, 1, 3) + .5f;
		return Mth.ceil(Mth.sqrt(2f * fallDistance / GRAVITY));
	}

	private static float randomOffset(Random random) {
		return (random.nextFloat() * 2f - 1f) * XZ_OFFSET;
	}
}
