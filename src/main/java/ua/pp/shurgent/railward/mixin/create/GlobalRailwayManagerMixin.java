package ua.pp.shurgent.railward.mixin.create;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.simibubi.create.content.trains.GlobalRailwayManager;

import net.minecraft.world.level.Level;
import ua.pp.shurgent.railward.block.train.parts.CabSignalTrainState;

@Mixin(value = GlobalRailwayManager.class, remap = false)
public class GlobalRailwayManagerMixin {

	@Inject(method = "tick", at = @At("TAIL"), remap = false)
	private void railward$tickCabSignalsAfterSignalStates(Level level, CallbackInfo ci) {
		CabSignalTrainState.tickAll(level);
	}
}
