package ua.pp.shurgent.railward.network;

import java.util.UUID;

import com.simibubi.create.CreateClient;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.foundation.networking.SimplePacketBase;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent.Context;
import ua.pp.shurgent.railward.block.train.parts.ModernWhistleSoundType;
import ua.pp.shurgent.railward.client.sound.ModernWhistleTrainAudio;

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
		context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> handleClient(trainId)));
		return true;
	}

	@OnlyIn(Dist.CLIENT)
	private static void handleClient(UUID trainId) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.level == null)
			return;

		CarriageContraptionEntity carriageEntity = findFirstCarriageEntity(minecraft, trainId);
		ModernWhistleSoundType soundType = carriageEntity == null
			? null
			: ModernWhistleTrainAudio.resolveTrainLeadSoundType(carriageEntity);
		if (carriageEntity != null && soundType != null) {
			ModernWhistleTrainAudio.playArrivalSequence(carriageEntity, soundType, 5f, 1f);
			return;
		}

		Train train = CreateClient.RAILWAYS.trains.get(trainId);
		if (train != null)
			train.honkTicks = train.honkTicks == 0 ? 20 : 13;
	}

	@OnlyIn(Dist.CLIENT)
	private static CarriageContraptionEntity findFirstCarriageEntity(Minecraft minecraft, UUID trainId) {
		CarriageContraptionEntity first = null;
		for (Entity entity : minecraft.level.entitiesForRendering()) {
			if (!(entity instanceof CarriageContraptionEntity carriageEntity))
				continue;
			if (!trainId.equals(carriageEntity.trainId))
				continue;
			if (first == null || carriageEntity.carriageIndex < first.carriageIndex)
				first = carriageEntity;
		}
		return first;
	}
}
