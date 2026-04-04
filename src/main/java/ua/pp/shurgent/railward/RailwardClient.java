package ua.pp.shurgent.railward;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(Dist.CLIENT)
public class RailwardClient {

	public static void onCtorClient(IEventBus modEventBus, IEventBus forgeEventBus) {
		
		AllPartialModels.init();
		
		modEventBus.addListener(RailwardClient::clientInit);
		
	}

	public static void clientInit(final FMLClientSetupEvent event) {
		// Ponder init... maybe...
	}

}
