package ua.pp.shurgent.railward.mixin.create;

import java.util.Collection;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import ua.pp.shurgent.railward.block.train.parts.TrainDirectionState;
import ua.pp.shurgent.railward.block.train.parts.ModernTrainControllerState;

@Mixin(value = CarriageContraptionEntity.class, remap = false)
public class CarriageContraptionEntityMixin {

	@Inject(method = "startControlling", at = @At("RETURN"), remap = false)
	private void railward$syncBufferLightsManualDirection(BlockPos controlsLocalPos, Player player,
		CallbackInfoReturnable<Boolean> cir) {
		if (cir.getReturnValueZ()) {
			ModernTrainControllerState.beginManualControl((CarriageContraptionEntity) (Object) this,
				controlsLocalPos, player);
			TrainDirectionState.syncManualControls((CarriageContraptionEntity) (Object) this,
				controlsLocalPos, player);
		}
	}

	@Redirect(method = "control", at = @At(value = "INVOKE",
		target = "Ljava/util/Collection;contains(Ljava/lang/Object;)Z", ordinal = 1), remap = false)
	private boolean railward$brakeOverridesThrottle(Collection<Integer> controls, Object key,
		BlockPos controlsLocalPos, Collection<Integer> heldControls, Player player) {
		CarriageContraptionEntity entity = (CarriageContraptionEntity) (Object) this;
		if (ModernTrainControllerState.isController(entity, controlsLocalPos) && heldControls.contains(1))
			return false;
		return controls.contains(key);
	}

	@Redirect(method = "control", at = @At(value = "INVOKE",
		target = "Ljava/util/Collection;contains(Ljava/lang/Object;)Z", ordinal = 2), remap = false)
	private boolean railward$replaceReverseWithBrake(Collection<Integer> controls, Object key,
		BlockPos controlsLocalPos, Collection<Integer> heldControls, Player player) {
		CarriageContraptionEntity entity = (CarriageContraptionEntity) (Object) this;
		if (ModernTrainControllerState.isController(entity, controlsLocalPos))
			return false;
		return controls.contains(key);
	}

	@Inject(method = "control", at = @At("RETURN"), remap = false)
	private void railward$refreshBufferLightsManualDirection(BlockPos controlsLocalPos, Collection<Integer> heldControls,
		Player player, CallbackInfoReturnable<Boolean> cir) {
		CarriageContraptionEntity entity = (CarriageContraptionEntity) (Object) this;
		if (heldControls.contains(5)) {
			if (!entity.level.isClientSide)
				TrainDirectionState.clearManualControls(entity);
			return;
		}
		if (cir.getReturnValueZ() && entity.getControllingPlayer()
			.filter(player.getUUID()::equals)
			.isPresent()) {
			ModernTrainControllerState.updateManualControl(entity, controlsLocalPos, heldControls, player);
			TrainDirectionState.syncManualControls(entity, controlsLocalPos, player);
		}
	}
}
