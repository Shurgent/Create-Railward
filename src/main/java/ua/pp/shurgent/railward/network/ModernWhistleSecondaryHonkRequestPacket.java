package ua.pp.shurgent.railward.network;

import java.util.UUID;

import com.simibubi.create.Create;
import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.foundation.networking.SimplePacketBase;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent.Context;
import net.minecraftforge.network.PacketDistributor;
import ua.pp.shurgent.railward.RailwardPackets;
import ua.pp.shurgent.railward.block.train.parts.ModernWhistleTrainHelper;

public class ModernWhistleSecondaryHonkRequestPacket extends SimplePacketBase {

	private final UUID trainId;
	private final boolean pressed;

	public ModernWhistleSecondaryHonkRequestPacket(UUID trainId, boolean pressed) {
		this.trainId = trainId;
		this.pressed = pressed;
	}

	public ModernWhistleSecondaryHonkRequestPacket(FriendlyByteBuf buffer) {
		trainId = buffer.readUUID();
		pressed = buffer.readBoolean();
	}

	@Override
	public void write(FriendlyByteBuf buffer) {
		buffer.writeUUID(trainId);
		buffer.writeBoolean(pressed);
	}

	@Override
	public boolean handle(Context context) {
		context.enqueueWork(() -> handleServer(context.getSender()));
		return true;
	}

	private void handleServer(ServerPlayer sender) {
		if (sender == null)
			return;
		Train train = Create.RAILWAYS.sided(sender.level).trains.get(trainId);
		if (train == null || pressed && !canUseSecondaryHonk(sender, train))
			return;
		RailwardPackets.getChannel()
			.send(PacketDistributor.ALL.noArg(), new ModernWhistleSecondaryHonkPacket(trainId, pressed));
	}

	private boolean canUseSecondaryHonk(ServerPlayer player, Train train) {
		for (Carriage carriage : train.carriages) {
			CarriageContraptionEntity entity = carriage.anyAvailableEntity();
			if (entity == null || entity.getControllingPlayer().filter(player.getUUID()::equals).isEmpty())
				continue;
			return ModernWhistleTrainHelper.resolveTrainLeadSecondarySoundType(entity) != null;
		}
		return false;
	}
}
