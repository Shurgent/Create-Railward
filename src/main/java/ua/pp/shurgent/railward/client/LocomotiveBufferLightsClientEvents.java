package ua.pp.shurgent.railward.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ua.pp.shurgent.railward.Railward;
import ua.pp.shurgent.railward.block.train.parts.LocomotiveBufferLightsTrainState;

@Mod.EventBusSubscriber(modid = Railward.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class LocomotiveBufferLightsClientEvents {

	private LocomotiveBufferLightsClientEvents() {
	}

	@SubscribeEvent
	public static void onClientLoggedOut(ClientPlayerNetworkEvent.LoggedOutEvent event) {
		LocomotiveBufferLightsTrainState.clearClientDirections();
	}
}
