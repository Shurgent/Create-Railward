package ua.pp.shurgent.railward.block.train.parts;

import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ua.pp.shurgent.railward.Railward;

@Mod.EventBusSubscriber(modid = Railward.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class TrainCouplerAutoTicker {

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onWorldTick(TickEvent.WorldTickEvent event) {
		if (event.phase != TickEvent.Phase.END)
			return;
		if (event.world.isClientSide)
			return;
		if (!(event.world instanceof ServerLevel level))
			return;

		TrainCouplerHandler.tickAutoCouplers(level);
	}

}
