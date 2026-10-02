package ua.pp.shurgent.railward.client.sound;

import java.util.IdentityHashMap;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.block.entity.BlockEntity;

public class UnloadingBulkCargoAudio {

	private static final Map<BlockEntity, UnloadingBulkCargoLoopSound> LOOPS = new IdentityHashMap<>();

	public static void keepLoopAlive(BlockEntity blockEntity) {
		UnloadingBulkCargoLoopSound soundInstance = LOOPS.get(blockEntity);
		if (soundInstance == null || soundInstance.isStopped()) {
			soundInstance = new UnloadingBulkCargoLoopSound(blockEntity.getBlockPos());
			LOOPS.put(blockEntity, soundInstance);
			Minecraft.getInstance()
				.getSoundManager()
				.queueTickingSound(soundInstance);
		}
		soundInstance.keepAlive();
	}

	public static void stopLoop(BlockEntity blockEntity) {
		UnloadingBulkCargoLoopSound soundInstance = LOOPS.remove(blockEntity);
		if (soundInstance != null)
			soundInstance.stopLoop();
	}

	public static boolean isLoopActive(BlockEntity blockEntity) {
		UnloadingBulkCargoLoopSound soundInstance = LOOPS.get(blockEntity);
		return soundInstance != null && !soundInstance.isStopped();
	}
}
