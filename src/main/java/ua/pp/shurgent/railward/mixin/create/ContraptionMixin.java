package ua.pp.shurgent.railward.mixin.create;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.StructureTransform;

import net.minecraft.world.level.Level;
import ua.pp.shurgent.railward.block.train.parts.BulkCargoContraptionCollector;
import ua.pp.shurgent.railward.block.train.parts.BulkCargoContraptionDisassembly;
import ua.pp.shurgent.railward.block.train.parts.CisternContraptionDisassembly;
import ua.pp.shurgent.railward.block.train.parts.CisternContraptionFluidSync;

@Mixin(value = Contraption.class, remap = false)
public class ContraptionMixin {

	@Inject(method = "tickStorage", at = @At("TAIL"))
	private void railward$tickTrainPartStorage(AbstractContraptionEntity contraptionEntity, CallbackInfo ci) {
		BulkCargoContraptionCollector.tick(contraptionEntity);
		CisternContraptionFluidSync.tick(contraptionEntity);
	}

	@Inject(method = "addBlocksToWorld", at = @At(value = "INVOKE",
		target = "Lcom/simibubi/create/content/contraptions/Contraption;translateMultiblockControllers(Lcom/simibubi/create/content/contraptions/StructureTransform;)V",
		shift = At.Shift.AFTER))
	private void railward$prepareBulkCargoStorageForDisassembly(Level world, StructureTransform transform,
		CallbackInfo ci) {
		BulkCargoContraptionDisassembly.prepare((Contraption) (Object) this, transform);
		CisternContraptionDisassembly.prepare((Contraption) (Object) this, transform);
	}

}
