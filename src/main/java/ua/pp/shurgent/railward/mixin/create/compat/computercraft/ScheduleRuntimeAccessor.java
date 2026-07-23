package ua.pp.shurgent.railward.mixin.create.compat.computercraft;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import com.simibubi.create.content.trains.schedule.ScheduleRuntime;

@Mixin(value = ScheduleRuntime.class, remap = false)
public interface ScheduleRuntimeAccessor {

	@Accessor("ticksInTransit")
	int railward$getTicksInTransit();
}
