package ua.pp.shurgent.railward.mixin.create;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import com.simibubi.create.content.decoration.slidingDoor.SlidingDoorBlockEntity;
import com.simibubi.create.foundation.utility.animation.LerpedFloat;

@Mixin(value = SlidingDoorBlockEntity.class, remap = false)
public interface SlidingDoorBlockEntityAccessor {

	@Accessor("animation")
	LerpedFloat railward$getAnimation();

	@Accessor("bridgeTicks")
	int railward$getBridgeTicks();

}
