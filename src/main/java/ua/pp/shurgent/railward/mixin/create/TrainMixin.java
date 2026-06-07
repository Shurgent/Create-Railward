package ua.pp.shurgent.railward.mixin.create;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.entity.TravellingPoint.IEdgePointListener;

import ua.pp.shurgent.railward.block.train.signal.SpeedSignTrainHandler;
import ua.pp.shurgent.railward.block.train.signal.WhistleSignTrainHandler;

@Mixin(value = Train.class, remap = false)
public abstract class TrainMixin {

	@Inject(method = "frontSignalListener", at = @At("RETURN"), cancellable = true, remap = false)
	private void railward$triggerWhistleSign(CallbackInfoReturnable<IEdgePointListener> cir) {
		IEdgePointListener original = cir.getReturnValue();
		Train train = (Train) (Object) this;

		cir.setReturnValue((distance, couple) -> {
			WhistleSignTrainHandler.onFrontEdgePoint(train, couple);
			SpeedSignTrainHandler.onFrontEdgePoint(train, couple);
			return original.test(distance, couple);
		});
	}
}
