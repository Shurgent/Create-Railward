package ua.pp.shurgent.railward.compat;

import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.Create;
import com.simibubi.create.content.equipment.wrench.IWrenchable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * Restores the inherited wrench-sound helpers that became static on
 * {@link IWrenchable} in Create 6.
 */
public interface RailwardWrenchable extends IWrenchable {

	default void playRemoveSound(Level level, BlockPos pos) {
		AllSoundEvents.WRENCH_REMOVE.playOnServer(level, pos, 1,
			Create.RANDOM.nextFloat() * .5f + .5f);
	}

	default void playRotateSound(Level level, BlockPos pos) {
		AllSoundEvents.WRENCH_ROTATE.playOnServer(level, pos, 1,
			Create.RANDOM.nextFloat() + .5f);
	}
}
