package ua.pp.shurgent.railward.network;

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

public class ModernWhistleArrivalPacket extends SimplePacketBase {

	private final int entityId;
	private final ModernWhistleSoundType soundType;

	public ModernWhistleArrivalPacket(CarriageContraptionEntity entity, ModernWhistleSoundType soundType) {
		this.entityId = entity.getId();
		this.soundType = soundType;
	}

	public ModernWhistleArrivalPacket(FriendlyByteBuf buffer) {
		entityId = buffer.readVarInt();
		soundType = buffer.readEnum(ModernWhistleSoundType.class);
	}

	@Override
	public void write(FriendlyByteBuf buffer) {
		buffer.writeVarInt(entityId);
		buffer.writeEnum(soundType);
	}

	@Override
	public boolean handle(Context context) {
		context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
			() -> () -> handleClient(entityId, soundType)));
		return true;
	}

	@OnlyIn(Dist.CLIENT)
	private static void handleClient(int entityId, ModernWhistleSoundType soundType) {
		if (Minecraft.getInstance().level == null)
			return;
		Entity entity = Minecraft.getInstance().level.getEntity(entityId);
		if (entity instanceof CarriageContraptionEntity carriageEntity)
			ModernWhistleTrainAudio.playArrivalSequence(carriageEntity, soundType,
				ModernWhistleTrainAudio.TRAIN_WHISTLE_VOLUME, 1f);
	}
}
