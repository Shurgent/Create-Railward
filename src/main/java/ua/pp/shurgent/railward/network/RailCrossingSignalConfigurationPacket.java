package ua.pp.shurgent.railward.network;

import com.simibubi.create.foundation.networking.SimplePacketBase;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent.Context;
import ua.pp.shurgent.railward.block.train.signal.RailCrossingSignalBlockEntity;
import ua.pp.shurgent.railward.block.train.signal.RailCrossingSignalBlockEntity.CrossingSign;
import ua.pp.shurgent.railward.block.train.signal.RailCrossingSignalBlockEntity.CrossingSound;

public class RailCrossingSignalConfigurationPacket extends SimplePacketBase {

	private final BlockPos pos;
	private final int sign;
	private final int sound;

	public RailCrossingSignalConfigurationPacket(BlockPos pos, CrossingSign sign, CrossingSound sound) {
		this.pos = pos;
		this.sign = sign.ordinal();
		this.sound = sound.ordinal();
	}

	public RailCrossingSignalConfigurationPacket(FriendlyByteBuf buffer) {
		pos = buffer.readBlockPos();
		sign = buffer.readVarInt();
		sound = buffer.readVarInt();
	}

	@Override
	public void write(FriendlyByteBuf buffer) {
		buffer.writeBlockPos(pos);
		buffer.writeVarInt(sign);
		buffer.writeVarInt(sound);
	}

	@Override
	public boolean handle(Context context) {
		ServerPlayer sender = context.getSender();
		context.enqueueWork(() -> apply(sender));
		return true;
	}

	private void apply(ServerPlayer sender) {
		CrossingSign[] signs = CrossingSign.values();
		CrossingSound[] sounds = CrossingSound.values();
		if (sender == null || sender.isSpectator() || !sender.mayBuild()
			|| sign < 0 || sign >= signs.length || sound < 0 || sound >= sounds.length)
			return;
		if (sender.distanceToSqr(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5) > 64
			|| !sender.level.isLoaded(pos) || !sender.level.mayInteract(sender, pos))
			return;
		if (sender.level.getBlockEntity(pos) instanceof RailCrossingSignalBlockEntity signal)
			signal.configure(signs[sign], sounds[sound]);
	}
}
