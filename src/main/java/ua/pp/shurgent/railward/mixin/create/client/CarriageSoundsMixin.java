package ua.pp.shurgent.railward.mixin.create.client;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import com.simibubi.create.content.trains.entity.CarriageSounds;
import com.simibubi.create.content.trains.entity.Train;

import ua.pp.shurgent.railward.block.train.parts.ModernWhistleSoundType;
import ua.pp.shurgent.railward.client.sound.ModernWhistleTrainAudio;

@Mixin(value = CarriageSounds.class, remap = false)
public abstract class CarriageSoundsMixin {

	private static final String TICK_METHOD =
		"tick(Lcom/simibubi/create/content/trains/entity/Carriage$DimensionalCarriageEntity;)V";

	@Shadow(remap = false)
	private CarriageContraptionEntity entity;

	@Inject(method = TICK_METHOD, remap = false, cancellable = true, at = @At(value = "FIELD",
		target = "Lcom/simibubi/create/content/trains/entity/Train;honkTicks:I", opcode = Opcodes.GETFIELD,
		ordinal = 0))
	private void railward$replaceManualHonk(Carriage.DimensionalCarriageEntity dce, CallbackInfo ci) {
		ModernWhistleSoundType soundType = ModernWhistleTrainAudio.resolveTrainLeadSoundType(entity);
		if (soundType == null)
			return;

		Carriage carriage = entity.getCarriage();
		if (carriage == null || carriage.train == null) {
			ModernWhistleTrainAudio.stopManualHonk(entity);
			ci.cancel();
			return;
		}

		Train train = carriage.train;

		if (train.honkTicks == 0) {
			ModernWhistleTrainAudio.stopManualHonk(entity);
			ci.cancel();
			return;
		}

		train.honkTicks--;
		train.determineHonk(entity.level);

		if (train.lowHonk == null) {
			ModernWhistleTrainAudio.stopManualHonk(entity);
			ci.cancel();
			return;
		}

		ModernWhistleTrainAudio.tickManualHonk(entity, soundType, train.honkTicks);
		ci.cancel();
	}

}
