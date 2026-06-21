package ua.pp.shurgent.railward.mixin.create;

import java.util.Collection;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import ua.pp.shurgent.railward.block.train.parts.LocomotiveBufferLightsTrainState;

@Mixin(value = CarriageContraptionEntity.class, remap = false)
public class CarriageContraptionEntityMixin {

	@Inject(method = "startControlling", at = @At("RETURN"), remap = false)
	private void railward$syncBufferLightsManualDirection(BlockPos controlsLocalPos, Player player,
		CallbackInfoReturnable<Boolean> cir) {
		if (cir.getReturnValueZ())
			LocomotiveBufferLightsTrainState.syncManualControls((CarriageContraptionEntity) (Object) this,
				controlsLocalPos, player);
	}

	@Inject(method = "control", at = @At("RETURN"), remap = false)
	private void railward$refreshBufferLightsManualDirection(BlockPos controlsLocalPos, Collection<Integer> heldControls,
		Player player, CallbackInfoReturnable<Boolean> cir) {
		CarriageContraptionEntity entity = (CarriageContraptionEntity) (Object) this;
		if (heldControls.contains(5)) {
			if (!entity.level.isClientSide)
				LocomotiveBufferLightsTrainState.clearManualControls(entity);
			return;
		}
		if (cir.getReturnValueZ() && entity.getControllingPlayer()
			.filter(player.getUUID()::equals)
			.isPresent())
			LocomotiveBufferLightsTrainState.syncManualControls(entity, controlsLocalPos, player);
	}
}
