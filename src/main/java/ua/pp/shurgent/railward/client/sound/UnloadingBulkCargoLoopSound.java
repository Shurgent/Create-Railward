package ua.pp.shurgent.railward.client.sound;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
//? if >=1.19.2
/*import net.minecraft.util.RandomSource;*/
import net.minecraft.world.phys.Vec3;
import ua.pp.shurgent.railward.RailwardSoundEvents;

public class UnloadingBulkCargoLoopSound extends AbstractTickableSoundInstance {

	private static final int FADE_OUT_TICKS = 40;
	private static final float BASE_VOLUME = .45f;

	private int keepAlive;
	private boolean stopping;
	private int fadeOutTicks = -1;

	public UnloadingBulkCargoLoopSound(BlockPos pos) {
		super(RailwardSoundEvents.UNLOADING_BULK_CARGO.get(), SoundSource.BLOCKS
				//? if >=1.19.2
				/*, RandomSource.create()*/
		);
		looping = true;
		delay = 0;
		volume = BASE_VOLUME;
		pitch = 1f;
		keepAlive();

		Vec3 center = Vec3.atCenterOf(pos);
		x = center.x;
		y = center.y;
		z = center.z;
	}

	public void keepAlive() {
		stopping = false;
		fadeOutTicks = -1;
		volume = BASE_VOLUME;
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
			float fade = Mth.clamp(fadeOutTicks / (float) FADE_OUT_TICKS, 0, 1);
			volume = BASE_VOLUME * fade;
			if (fadeOutTicks-- <= 0)
				stop();
			return;
		}

		if (--keepAlive <= 0) {
			stopLoop();
			return;
		}

		volume = BASE_VOLUME;
	}

	@Override
	public boolean canStartSilent() {
		return true;
	}
}
