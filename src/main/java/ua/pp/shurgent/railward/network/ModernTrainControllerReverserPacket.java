package ua.pp.shurgent.railward.network;

import com.simibubi.create.foundation.networking.SimplePacketBase;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent.Context;
import ua.pp.shurgent.railward.block.train.parts.ModernTrainControllerState;

public class ModernTrainControllerReverserPacket extends SimplePacketBase {

	private final int entityId;
	private final BlockPos controlsPos;

	public ModernTrainControllerReverserPacket(int entityId, BlockPos controlsPos) {
		this.entityId = entityId;
		this.controlsPos = controlsPos;
	}

	public ModernTrainControllerReverserPacket(FriendlyByteBuf buffer) {
		entityId = buffer.readVarInt();
		controlsPos = buffer.readBlockPos();
	}

	@Override
	public void write(FriendlyByteBuf buffer) {
		buffer.writeVarInt(entityId);
		buffer.writeBlockPos(controlsPos);
	}

	@Override
	public boolean handle(Context context) {
		context.enqueueWork(() -> ModernTrainControllerState.toggleReverser(context.getSender(), entityId, controlsPos));
		return true;
	}
}
