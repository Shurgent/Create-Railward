package ua.pp.shurgent.railward.network;

import java.util.UUID;

import com.simibubi.create.foundation.networking.SimplePacketBase;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent.Context;
import ua.pp.shurgent.railward.block.train.parts.LocomotiveBufferLightsTrainState;
import ua.pp.shurgent.railward.block.train.parts.LocomotiveBufferLightsTrainState.DirectionState;

public class LocomotiveBufferLightsTrainStatePacket extends SimplePacketBase {

	private final UUID trainId;
	private final DirectionState direction;

	public LocomotiveBufferLightsTrainStatePacket(UUID trainId, DirectionState direction) {
		this.trainId = trainId;
		this.direction = direction;
	}

	public LocomotiveBufferLightsTrainStatePacket(FriendlyByteBuf buffer) {
		trainId = buffer.readUUID();
		direction = buffer.readEnum(DirectionState.class);
	}

	@Override
	public void write(FriendlyByteBuf buffer) {
		buffer.writeUUID(trainId);
		buffer.writeEnum(direction);
	}

	@Override
	public boolean handle(Context context) {
		context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
			() -> () -> LocomotiveBufferLightsTrainState.setClientDirection(trainId, direction)));
		return true;
	}
}
