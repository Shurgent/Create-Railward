package ua.pp.shurgent.railward.network;

import java.util.UUID;

import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
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

public class ModernWhistleSecondaryHonkPacket extends SimplePacketBase {

	private final UUID trainId;
	private final boolean pressed;

	public ModernWhistleSecondaryHonkPacket(UUID trainId, boolean pressed) {
		this.trainId = trainId;
		this.pressed = pressed;
	}

	public ModernWhistleSecondaryHonkPacket(FriendlyByteBuf buffer) {
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
		context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> handleClient(trainId, pressed)));
		return true;
	}

	@OnlyIn(Dist.CLIENT)
	private static void handleClient(UUID trainId, boolean pressed) {
		CarriageContraptionEntity carriageEntity = findFirstCarriageEntity(trainId);
		ModernWhistleSoundType soundType = carriageEntity == null ? null
			: ModernWhistleTrainAudio.resolveTrainLeadSecondarySoundType(carriageEntity);
		ModernWhistleTrainAudio.setSecondaryHonk(trainId, carriageEntity, soundType, pressed);
	}

	@OnlyIn(Dist.CLIENT)
	private static CarriageContraptionEntity findFirstCarriageEntity(UUID trainId) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.level == null)
			return null;

		CarriageContraptionEntity first = null;
		for (Entity entity : minecraft.level.entitiesForRendering()) {
			if (!(entity instanceof CarriageContraptionEntity carriageEntity)
				|| !trainId.equals(carriageEntity.trainId))
				continue;
			if (first == null || carriageEntity.carriageIndex < first.carriageIndex)
				first = carriageEntity;
		}
		return first;
	}
}
