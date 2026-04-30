package ua.pp.shurgent.railward.mixin.create;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;

import ua.pp.shurgent.railward.block.train.parts.BulkCargoContraptionCollector;

@Mixin(value = Contraption.class, remap = false)
public class ContraptionMixin {

	@Inject(method = "tickStorage", at = @At("TAIL"))
	private void railward$collectBulkCargoItems(AbstractContraptionEntity contraptionEntity, CallbackInfo ci) {
		BulkCargoContraptionCollector.tick(contraptionEntity);
	}

}
