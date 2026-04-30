package ua.pp.shurgent.railward;

import static net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT;

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
import ua.pp.shurgent.railward.network.TrainCouplerSyncPacket;

import com.simibubi.create.foundation.networking.SimplePacketBase;

public class RailwardPackets {

	private static final ResourceLocation CHANNEL_NAME = Railward.asResource("main");
	private static final String NETWORK_VERSION = "1";
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
