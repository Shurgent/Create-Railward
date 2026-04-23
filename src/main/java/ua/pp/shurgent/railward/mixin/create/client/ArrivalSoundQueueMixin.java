package ua.pp.shurgent.railward.mixin.create.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.simibubi.create.content.trains.entity.ArrivalSoundQueue;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;

import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import ua.pp.shurgent.railward.block.train.parts.ModernWhistleSoundType;
import ua.pp.shurgent.railward.client.sound.ModernWhistleTrainAudio;

@Mixin(value = ArrivalSoundQueue.class, remap = false)
public abstract class ArrivalSoundQueueMixin {

	@Inject(method = "play(Lcom/simibubi/create/content/trains/entity/CarriageContraptionEntity;Lnet/minecraft/world/level/levelgen/structure/templatesystem/StructureTemplate$StructureBlockInfo;)V",
		at = @At("HEAD"), cancellable = true, remap = false)
	private static void railward$replaceArrivalWhistle(CarriageContraptionEntity carriageEntity,
		StructureBlockInfo blockInfo, CallbackInfo ci) {
		ModernWhistleSoundType soundType = ModernWhistleTrainAudio.resolveSoundType(blockInfo);
		if (soundType == null)
			return;

		ModernWhistleTrainAudio.playArrivalSequence(carriageEntity, soundType, 5f, 1f);
		ci.cancel();
	}

}
