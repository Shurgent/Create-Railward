package ua.pp.shurgent.railward.mixin.create;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.entity.TravellingPoint.IEdgePointListener;

import ua.pp.shurgent.railward.block.train.signal.SpeedSignTrainHandler;
import ua.pp.shurgent.railward.block.train.signal.WhistleSignTrainHandler;
import ua.pp.shurgent.railward.block.train.parts.CabSignalTrainState;
import ua.pp.shurgent.railward.block.train.parts.CabSignalTrainState.PassSnapshot;

@Mixin(value = Carriage.class, remap = false)
public abstract class CarriageMixin {

	@Redirect(method = "travel", at = @At(value = "INVOKE",
		target = "Lcom/simibubi/create/content/trains/entity/Train;frontSignalListener()Lcom/simibubi/create/content/trains/entity/TravellingPoint$IEdgePointListener;"),
		remap = false)
	private IEdgePointListener railward$wrapFrontSignalListener(Train train) {
		IEdgePointListener original = train.frontSignalListener();
		return (distance, couple) -> {
			PassSnapshot cabSignalSnapshot = CabSignalTrainState.snapshotFrontEdgePoint(train, couple);
			WhistleSignTrainHandler.onFrontEdgePoint(train, couple);
			SpeedSignTrainHandler.onFrontEdgePoint(train, couple);
			boolean stopped = original.test(distance, couple);
			CabSignalTrainState.finishFrontEdgePoint(train, cabSignalSnapshot, stopped);
			return stopped;
		};
	}
}
