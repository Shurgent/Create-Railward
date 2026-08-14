package ua.pp.shurgent.railward.network;

import com.simibubi.create.foundation.networking.SimplePacketBase;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent.Context;
import ua.pp.shurgent.railward.block.train.parts.ModernWhistleBlockEntity;
import ua.pp.shurgent.railward.block.train.parts.ModernWhistleSoundType;

public class ModernWhistleConfigurationPacket extends SimplePacketBase {

	private static final double MAX_INTERACTION_DISTANCE_SQUARED = 64;

	private final BlockPos blockPos;
	private final int primaryOrdinal;
	private final int secondaryOrdinal;

	public ModernWhistleConfigurationPacket(BlockPos blockPos, ModernWhistleSoundType primary,
		ModernWhistleSoundType secondary) {
		this.blockPos = blockPos;
		primaryOrdinal = primary.ordinal();
		secondaryOrdinal = secondary.ordinal();
	}

	public ModernWhistleConfigurationPacket(FriendlyByteBuf buffer) {
		blockPos = buffer.readBlockPos();
		primaryOrdinal = buffer.readVarInt();
		secondaryOrdinal = buffer.readVarInt();
	}

	@Override
	public void write(FriendlyByteBuf buffer) {
		buffer.writeBlockPos(blockPos);
		buffer.writeVarInt(primaryOrdinal);
		buffer.writeVarInt(secondaryOrdinal);
	}

	@Override
	public boolean handle(Context context) {
		ServerPlayer sender = context.getSender();
		context.enqueueWork(() -> applyConfiguration(sender));
		return true;
	}

	private void applyConfiguration(ServerPlayer sender) {
		ModernWhistleSoundType[] values = ModernWhistleSoundType.values();
		if (sender == null || sender.isSpectator() || primaryOrdinal < 0 || primaryOrdinal >= values.length
			|| secondaryOrdinal < 0 || secondaryOrdinal >= values.length)
			return;
		if (!sender.level.hasChunkAt(blockPos))
			return;
		if (sender.distanceToSqr(blockPos.getX() + .5, blockPos.getY() + .5, blockPos.getZ() + .5)
			> MAX_INTERACTION_DISTANCE_SQUARED)
			return;
		if (!(sender.level.getBlockEntity(blockPos) instanceof ModernWhistleBlockEntity whistle))
			return;

		whistle.applySoundConfiguration(values[primaryOrdinal], values[secondaryOrdinal]);
	}
}
