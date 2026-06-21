package ua.pp.shurgent.railward.network;

import java.util.UUID;

import com.simibubi.create.CreateClient;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import com.simibubi.create.content.trains.entity.Train;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import ua.pp.shurgent.railward.client.sound.ModernWhistleTrainAudio;

@OnlyIn(Dist.CLIENT)
public class WhistleSignHonkClientHandler {

	private WhistleSignHonkClientHandler() {
	}

	public static void handle(UUID trainId) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.level == null)
			return;

		CarriageContraptionEntity carriageEntity = findFirstCarriageEntity(trainId);
		if (carriageEntity != null && ModernWhistleTrainAudio.playWhistleSignSequence(carriageEntity, 5f, 1f))
			return;

		Train train = CreateClient.RAILWAYS.trains.get(trainId);
		if (train != null)
			train.honkTicks = train.honkTicks == 0 ? 20 : 13;
	}

	private static CarriageContraptionEntity findFirstCarriageEntity(UUID trainId) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.level == null)
			return null;

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
