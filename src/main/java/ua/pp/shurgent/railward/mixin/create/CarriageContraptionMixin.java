package ua.pp.shurgent.railward.mixin.create;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.simibubi.create.content.trains.entity.CarriageContraption;
import com.simibubi.create.content.contraptions.AssemblyException;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import ua.pp.shurgent.railward.block.train.parts.BulkCargoContraptionAssembly;
import ua.pp.shurgent.railward.block.train.parts.ModernTrainControllerAssembly;

@Mixin(value = CarriageContraption.class, remap = false)
public class CarriageContraptionMixin {

	@Inject(method = "assemble", at = @At("RETURN"))
	private void railward$normalizeAssembledTrainParts(Level world, BlockPos pos, CallbackInfoReturnable<Boolean> cir)
		throws AssemblyException {
		if (cir.getReturnValueZ()) {
			BulkCargoContraptionAssembly.normalize((CarriageContraption) (Object) this);
			ModernTrainControllerAssembly.apply((CarriageContraption) (Object) this);
		}
	}

}
