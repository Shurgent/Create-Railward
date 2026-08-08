package ua.pp.shurgent.railward.block.train.parts;

import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ua.pp.shurgent.railward.Railward;

@Mod.EventBusSubscriber(modid = Railward.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class LocomotiveBufferLightsEvents {

	private LocomotiveBufferLightsEvents() {
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
		if (event.getPlayer() instanceof ServerPlayer player) {
			TrainDirectionState.syncAllControls(player);
			CabSignalTrainState.syncAll(player);
		}
	}

	@SubscribeEvent
	public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
		TrainDirectionState.clearManualControls(event.getPlayer()
			.getUUID());
	}

	@SubscribeEvent
	public static void onStartTracking(PlayerEvent.StartTracking event) {
		if (event.getPlayer() instanceof ServerPlayer player
			&& event.getTarget() instanceof CarriageContraptionEntity entity) {
			TrainDirectionState.syncCurrentControls(entity, player);
			CabSignalTrainState.syncCurrent(entity, player);
		}
	}

	@SubscribeEvent
	public static void onServerStopping(ServerStoppingEvent event) {
		TrainDirectionState.clearServerDirections();
		CabSignalTrainState.clearServerStates();
	}
}
