package ua.pp.shurgent.railward.mixin.create;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;

import net.minecraft.core.BlockPos;
import ua.pp.shurgent.railward.block.train.parts.BulkCargoContraptionCollector;
import ua.pp.shurgent.railward.block.train.parts.LocomotiveBufferLightsTrainState;

@Mixin(value = AbstractContraptionEntity.class, remap = false)
public class AbstractContraptionEntityMixin {

	@Inject(method = { "tick", "m_8119_" }, at = @At("TAIL"), require = 0)
	private void railward$collectBulkCargoItems(CallbackInfo ci) {
		BulkCargoContraptionCollector.tick((AbstractContraptionEntity) (Object) this);
	}

	@Inject(method = "stopControlling", at = @At("HEAD"), remap = false)
	private void railward$clearBufferLightsManualDirection(BlockPos controlsLocalPos, CallbackInfo ci) {
		if ((Object) this instanceof CarriageContraptionEntity carriageEntity)
			LocomotiveBufferLightsTrainState.clearManualControls(carriageEntity);
	}

}
