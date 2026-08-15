package ua.pp.shurgent.railward;

import static net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT;
import static net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER;

import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent.Context;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import ua.pp.shurgent.railward.network.BulkCargoSyncPacket;
import ua.pp.shurgent.railward.network.CabSignalTrainStatePacket;
import ua.pp.shurgent.railward.network.CisternFluidSyncPacket;
import ua.pp.shurgent.railward.network.LocomotiveBufferLightsTrainStatePacket;
import ua.pp.shurgent.railward.network.ModernWhistleArrivalPacket;
import ua.pp.shurgent.railward.network.ModernTrainControllerReverserPacket;
import ua.pp.shurgent.railward.network.ModernWhistleConfigurationPacket;
import ua.pp.shurgent.railward.network.ModernWhistleSecondaryHonkPacket;
import ua.pp.shurgent.railward.network.ModernWhistleSecondaryHonkRequestPacket;
import ua.pp.shurgent.railward.network.TrainCouplerSyncPacket;
import ua.pp.shurgent.railward.network.WhistleSignHonkPacket;

import com.simibubi.create.foundation.networking.SimplePacketBase;

public class RailwardPackets {

	private static final ResourceLocation CHANNEL_NAME = Railward.asResource("main");
	private static final String NETWORK_VERSION = "4";
	private static SimpleChannel channel;
	private static int index = 0;

	public static void registerPackets() {
		channel = NetworkRegistry.ChannelBuilder.named(CHANNEL_NAME)
			.serverAcceptedVersions(NETWORK_VERSION::equals)
			.clientAcceptedVersions(NETWORK_VERSION::equals)
			.networkProtocolVersion(() -> NETWORK_VERSION)
			.simpleChannel();

		register(TrainCouplerSyncPacket.class, TrainCouplerSyncPacket::new, PLAY_TO_CLIENT);
		register(BulkCargoSyncPacket.class, BulkCargoSyncPacket::new, PLAY_TO_CLIENT);
		register(CisternFluidSyncPacket.class, CisternFluidSyncPacket::new, PLAY_TO_CLIENT);
		register(WhistleSignHonkPacket.class, WhistleSignHonkPacket::new, PLAY_TO_CLIENT);
		register(LocomotiveBufferLightsTrainStatePacket.class, LocomotiveBufferLightsTrainStatePacket::new,
			PLAY_TO_CLIENT);
		register(CabSignalTrainStatePacket.class, CabSignalTrainStatePacket::new, PLAY_TO_CLIENT);
		register(ModernWhistleArrivalPacket.class, ModernWhistleArrivalPacket::new, PLAY_TO_CLIENT);
		register(ModernWhistleSecondaryHonkRequestPacket.class, ModernWhistleSecondaryHonkRequestPacket::new,
			PLAY_TO_SERVER);
		register(ModernWhistleConfigurationPacket.class, ModernWhistleConfigurationPacket::new, PLAY_TO_SERVER);
		register(ModernWhistleSecondaryHonkPacket.class, ModernWhistleSecondaryHonkPacket::new, PLAY_TO_CLIENT);
		register(ModernTrainControllerReverserPacket.class, ModernTrainControllerReverserPacket::new, PLAY_TO_SERVER);
	}

	public static SimpleChannel getChannel() {
		return channel;
	}

	private static <T extends SimplePacketBase> void register(Class<T> type, Function<FriendlyByteBuf, T> factory,
		NetworkDirection direction) {
		BiConsumer<T, FriendlyByteBuf> encoder = T::write;
		BiConsumer<T, Supplier<Context>> consumer = (packet, contextSupplier) -> {
			Context context = contextSupplier.get();
			if (packet.handle(context))
				context.setPacketHandled(true);
		};

		channel.messageBuilder(type, index++, direction)
			.encoder(encoder)
			.decoder(factory)
			.consumer(consumer)
			.add();
	}

}
