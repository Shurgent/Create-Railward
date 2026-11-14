package ua.pp.shurgent.railward.block.train.parts;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;
import ua.pp.shurgent.railward.Railward;

@Mod.EventBusSubscriber(modid = Railward.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class TrainCouplerAutoTicker {

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onServerTick(TickEvent.ServerTickEvent event) {
		if (event.phase != TickEvent.Phase.END)
			return;
		MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
		if (server == null)
			return;

		for (ServerLevel level : server.getAllLevels())
			TrainCouplerHandler.tickAutoCouplers(level);
	}

	@SubscribeEvent
	public static void onServerStopped(ServerStoppedEvent event) {
		TrainCouplerHandler.clearAutoCouplers();
	}

}
