package ua.pp.shurgent.railward.gametest.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.entity.TravellingPoint.IEdgePointListener;

import ua.pp.shurgent.railward.gametest.RailwardTrainPassageGameTests;

/** Occupies Railward's former redirect target, like a competing train addon. */
@Mixin(value = Carriage.class, remap = false)
public abstract class ForeignCarriagePassageMixin {
	@Redirect(method = "travel", at = @At(value = "INVOKE",
		target = "Lcom/simibubi/create/content/trains/entity/Train;frontSignalListener()Lcom/simibubi/create/content/trains/entity/TravellingPoint$IEdgePointListener;"),
		remap = false)
	private IEdgePointListener test$wrapTrainPassage(Train train) {
		return RailwardTrainPassageGameTests.wrapForeignListener(train, train.frontSignalListener());
	}
}
