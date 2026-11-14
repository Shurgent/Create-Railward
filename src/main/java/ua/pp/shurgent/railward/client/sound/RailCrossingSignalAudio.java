package ua.pp.shurgent.railward.client.sound;

import java.util.IdentityHashMap;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundSource;
//? if >=1.19.2
/*import net.minecraft.util.RandomSource;*/
import net.minecraft.world.phys.Vec3;
import ua.pp.shurgent.railward.block.train.signal.RailCrossingSignalBlockEntity;
import ua.pp.shurgent.railward.block.train.signal.RailCrossingSignalBlockEntity.CrossingSound;

public final class RailCrossingSignalAudio extends AbstractTickableSoundInstance {

	private static final Map<RailCrossingSignalBlockEntity, RailCrossingSignalAudio> LOOPS = new IdentityHashMap<>();

	private final RailCrossingSignalBlockEntity signal;
	private final CrossingSound sound;

	private RailCrossingSignalAudio(RailCrossingSignalBlockEntity signal) {
		super(signal.getSound().getEvent(), SoundSource.BLOCKS
			//? if >=1.19.2
			/*, RandomSource.create()*/
		);
		this.signal = signal;
		sound = signal.getSound();
		looping = true;
		Vec3 center = Vec3.atCenterOf(signal.getBlockPos());
		x = center.x;
		y = center.y;
		z = center.z;
	}

	public static void update(RailCrossingSignalBlockEntity signal) {
		if (!signal.isActive()) {
			stop(signal);
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		if (signal.getLevel() != mc.level || mc.options.getSoundSourceVolume(SoundSource.BLOCKS) == 0
			|| mc.options.getSoundSourceVolume(SoundSource.MASTER) == 0)
			return;

		var manager = mc.getSoundManager();
		RailCrossingSignalAudio current = LOOPS.get(signal);
		if (current != null && current.sound == signal.getSound() && !current.isStopped() && manager.isActive(current))
			return;

		stop(signal);
		RailCrossingSignalAudio next = new RailCrossingSignalAudio(signal);
		manager.play(next);
		if (manager.isActive(next))
			LOOPS.put(signal, next);
	}

	public static void stop(RailCrossingSignalBlockEntity signal) {
		RailCrossingSignalAudio current = LOOPS.remove(signal);
		if (current != null) {
			current.stop();
			Minecraft.getInstance().getSoundManager().stop(current);
		}
	}

	@Override
	public void tick() {
		if (signal.isRemoved() || signal.getLevel() != Minecraft.getInstance().level
			|| !signal.isActive() || signal.getSound() != sound) {
			LOOPS.remove(signal, this);
			stop();
		}
	}
}
