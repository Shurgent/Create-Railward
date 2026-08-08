package ua.pp.shurgent.railward.mixin.create;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;

import net.minecraft.core.BlockPos;
import ua.pp.shurgent.railward.block.train.parts.BulkCargoContraptionCollector;
import ua.pp.shurgent.railward.block.train.parts.CisternContraptionFluidSync;
import ua.pp.shurgent.railward.block.train.parts.TrainDirectionState;

@Mixin(value = AbstractContraptionEntity.class, remap = false)
public class AbstractContraptionEntityMixin {

	@Inject(method = { "tick", "m_8119_" }, at = @At("TAIL"), require = 0)
	private void railward$tickTrainPartStorage(CallbackInfo ci) {
		AbstractContraptionEntity entity = (AbstractContraptionEntity) (Object) this;
		BulkCargoContraptionCollector.tick(entity);
		CisternContraptionFluidSync.tick(entity);
	}

	@Inject(method = "stopControlling", at = @At("HEAD"), remap = false)
	private void railward$clearBufferLightsManualDirection(BlockPos controlsLocalPos, CallbackInfo ci) {
		if ((Object) this instanceof CarriageContraptionEntity carriageEntity)
			TrainDirectionState.clearManualControls(carriageEntity);
	}

}
