package ua.pp.shurgent.railward.mixin.create;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.simibubi.create.content.decoration.steamWhistle.WhistleBlock;
import com.simibubi.create.content.trains.entity.ArrivalSoundQueue;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;

import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraftforge.network.PacketDistributor;
import ua.pp.shurgent.railward.RailwardPackets;
import ua.pp.shurgent.railward.block.train.parts.ModernWhistleSoundType;
import ua.pp.shurgent.railward.block.train.parts.ModernWhistleTrainHelper;
import ua.pp.shurgent.railward.network.ModernWhistleArrivalPacket;

@Mixin(value = ArrivalSoundQueue.class, remap = false)
public abstract class ArrivalSoundQueueMixin {

	@Inject(method = "play(Lcom/simibubi/create/content/trains/entity/CarriageContraptionEntity;Lnet/minecraft/world/level/levelgen/structure/templatesystem/StructureTemplate$StructureBlockInfo;)V",
		at = @At("HEAD"), cancellable = true, remap = false)
	private static void railward$replaceServerArrivalWhistle(CarriageContraptionEntity carriageEntity,
		StructureBlockInfo blockInfo, CallbackInfo ci) {
		if (carriageEntity == null || blockInfo == null || carriageEntity.level.isClientSide)
			return;
		if (!(blockInfo.state.getBlock() instanceof WhistleBlock))
			return;

		ModernWhistleSoundType soundType = ModernWhistleTrainHelper.resolveTrainLeadSoundType(carriageEntity);
		if (soundType == null)
			return;

		RailwardPackets.getChannel()
			.send(PacketDistributor.TRACKING_ENTITY.with(() -> carriageEntity),
				new ModernWhistleArrivalPacket(carriageEntity, soundType));
		ci.cancel();
	}
}
