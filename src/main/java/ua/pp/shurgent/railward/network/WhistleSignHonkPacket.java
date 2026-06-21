package ua.pp.shurgent.railward.network;

import java.util.UUID;

import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.foundation.networking.SimplePacketBase;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent.Context;

public class WhistleSignHonkPacket extends SimplePacketBase {

	private final UUID trainId;

	public WhistleSignHonkPacket(Train train) {
		trainId = train.id;
	}

	public WhistleSignHonkPacket(FriendlyByteBuf buffer) {
		trainId = buffer.readUUID();
	}

	@Override
	public void write(FriendlyByteBuf buffer) {
		buffer.writeUUID(trainId);
	}

	@Override
	public boolean handle(Context context) {
		context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
			() -> () -> WhistleSignHonkClientHandler.handle(trainId)));
		return true;
	}
}
