package ua.pp.shurgent.railward.mixin.create;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;

import ua.pp.shurgent.railward.block.train.parts.BulkCargoContraptionCollector;

@Mixin(value = AbstractContraptionEntity.class, remap = false)
public class AbstractContraptionEntityMixin {

	@Inject(method = { "tick", "m_8119_" }, at = @At("TAIL"), require = 0)
	private void railward$collectBulkCargoItems(CallbackInfo ci) {
		BulkCargoContraptionCollector.tick((AbstractContraptionEntity) (Object) this);
	}

}
