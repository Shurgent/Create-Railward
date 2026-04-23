package ua.pp.shurgent.railward.client.sound;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import ua.pp.shurgent.railward.block.train.parts.ModernWhistleSoundType;

public class ModernWhistleLoopSound extends AbstractTickableSoundInstance {

	private static final int FADE_OUT_TICKS = 4;

	private final ModernWhistleSoundType soundType;
	private int keepAlive;
	private boolean stopping;
	private int fadeOutTicks = -1;

	public ModernWhistleLoopSound(ModernWhistleSoundType soundType, BlockPos pos) {
		super(soundType.getLoopEvent(), SoundSource.BLOCKS);
		this.soundType = soundType;
		this.looping = true;
		this.delay = 0;
		this.volume = 1f;
		keepAlive();

		Vec3 center = Vec3.atCenterOf(pos);
		x = center.x;
		y = center.y;
		z = center.z;
	}

	public ModernWhistleSoundType getSoundType() {
		return soundType;
	}

	public void keepAlive() {
		if (stopping)
			return;
		keepAlive = 2;
	}

	public void stopLoop() {
		if (stopping)
			return;
		stopping = true;
		fadeOutTicks = FADE_OUT_TICKS;
	}

	@Override
	public void tick() {
		if (stopping) {
			float fade = Mth.clamp((float) fadeOutTicks / FADE_OUT_TICKS, 0f, 1f);
			volume = fade;
			if (fadeOutTicks-- <= 0)
				stop();
			return;
		}

		if (--keepAlive <= 0) {
			stopLoop();
			return;
		}

		volume = 1f;
	}

	@Override
	public boolean canStartSilent() {
		return true;
	}

}
