package ua.pp.shurgent.railward.client.sound;

import java.util.IdentityHashMap;
import java.util.Map;

import net.minecraft.client.Minecraft;
import ua.pp.shurgent.railward.block.train.parts.ModernWhistleBlockEntity;
import ua.pp.shurgent.railward.block.train.parts.ModernWhistleSoundType;

public class ModernWhistleBlockEntityAudio {

	private static final Map<ModernWhistleBlockEntity, ModernWhistleLoopSound> LOOPS = new IdentityHashMap<>();

	public static void keepLoopAlive(ModernWhistleBlockEntity blockEntity, ModernWhistleSoundType soundType) {
		ModernWhistleLoopSound soundInstance = LOOPS.get(blockEntity);
		if (soundInstance != null && !soundInstance.isStopped()) {
			if (soundInstance.getSoundType() == soundType) {
				soundInstance.keepAlive();
				return;
			}
			soundInstance.stopLoop();
		}

		soundInstance = new ModernWhistleLoopSound(soundType, blockEntity.getBlockPos());
		LOOPS.put(blockEntity, soundInstance);
		Minecraft.getInstance()
			.getSoundManager()
			.queueTickingSound(soundInstance);
	}

	public static void stopLoop(ModernWhistleBlockEntity blockEntity) {
		ModernWhistleLoopSound soundInstance = LOOPS.remove(blockEntity);
		if (soundInstance != null)
			soundInstance.stopLoop();
	}
}
