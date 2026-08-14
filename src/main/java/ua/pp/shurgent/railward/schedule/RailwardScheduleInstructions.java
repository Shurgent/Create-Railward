package ua.pp.shurgent.railward.schedule;

import com.simibubi.create.content.trains.schedule.Schedule;
import com.simibubi.create.foundation.utility.Pair;

import net.minecraft.resources.ResourceLocation;
import ua.pp.shurgent.railward.Railward;

public class RailwardScheduleInstructions {

	public static final ResourceLocation PRIMARY_HONK_ID = Railward.asResource("primary_honk");
	public static final ResourceLocation SECONDARY_HONK_ID = Railward.asResource("secondary_honk");

	private static boolean registered;

	private RailwardScheduleInstructions() {
	}

	public static void register() {
		if (registered)
			return;
		registered = true;
		Schedule.INSTRUCTION_TYPES.add(Pair.of(PRIMARY_HONK_ID, () -> new ModernWhistleHonkInstruction(false)));
		Schedule.INSTRUCTION_TYPES.add(Pair.of(SECONDARY_HONK_ID, () -> new ModernWhistleHonkInstruction(true)));
	}
}
