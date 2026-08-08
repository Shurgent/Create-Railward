package ua.pp.shurgent.railward.network;

import java.util.UUID;

import com.simibubi.create.foundation.networking.SimplePacketBase;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent.Context;
import ua.pp.shurgent.railward.block.train.parts.CabSignalAspect;
import ua.pp.shurgent.railward.block.train.parts.CabSignalTrainState;

public class CabSignalTrainStatePacket extends SimplePacketBase {

	private final UUID trainId;
	private final CabSignalAspect aspect;

	public CabSignalTrainStatePacket(UUID trainId, CabSignalAspect aspect) {
		this.trainId = trainId;
		this.aspect = aspect;
	}

	public CabSignalTrainStatePacket(FriendlyByteBuf buffer) {
		trainId = buffer.readUUID();
		aspect = buffer.readEnum(CabSignalAspect.class);
	}

	@Override
	public void write(FriendlyByteBuf buffer) {
		buffer.writeUUID(trainId);
		buffer.writeEnum(aspect);
	}

	@Override
	public boolean handle(Context context) {
		context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
			() -> () -> CabSignalTrainState.setClientAspect(trainId, aspect)));
		return true;
	}
}
