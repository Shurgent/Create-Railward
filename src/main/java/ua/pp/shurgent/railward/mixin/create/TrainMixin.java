package ua.pp.shurgent.railward.mixin.create;

import java.util.Map;
import java.util.UUID;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.entity.TravellingPoint.IEdgePointListener;
import com.simibubi.create.content.trains.graph.DimensionPalette;
import com.simibubi.create.content.trains.graph.TrackGraph;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import ua.pp.shurgent.railward.block.train.parts.CabSignalTrainState;
import ua.pp.shurgent.railward.block.train.parts.CabSignalTrainState.PassSnapshot;
import ua.pp.shurgent.railward.block.train.parts.DieselExhaustEmitter;
import ua.pp.shurgent.railward.block.train.parts.ModernTrainControllerState;
import ua.pp.shurgent.railward.block.train.parts.TrainDirectionState;
import ua.pp.shurgent.railward.block.train.signal.RelayCaseEdgePoint;
import ua.pp.shurgent.railward.block.train.signal.RelayCaseRailwayManager;
import ua.pp.shurgent.railward.block.train.signal.SpeedSignTrainHandler;
import ua.pp.shurgent.railward.block.train.signal.WhistleSignTrainHandler;

@Mixin(value = Train.class, remap = false)
public abstract class TrainMixin {

	/** Wrap the returned listener so other mods can still intercept the call in Carriage.travel. */
	@Inject(method = "frontSignalListener", at = @At("RETURN"), cancellable = true, remap = false)
	private void railward$wrapFrontSignalListener(CallbackInfoReturnable<IEdgePointListener> cir) {
		Train train = (Train) (Object) this;
		IEdgePointListener original = cir.getReturnValue();
		cir.setReturnValue((distance, couple) -> {
			PassSnapshot cabSignalSnapshot = CabSignalTrainState.snapshotFrontEdgePoint(train, couple);
			WhistleSignTrainHandler.onFrontEdgePoint(train, couple);
			SpeedSignTrainHandler.onFrontEdgePoint(train, couple);
			if (couple.getFirst() instanceof RelayCaseEdgePoint relay)
				relay.onTrainEntered();
			boolean stopped = original.test(distance, couple);
			CabSignalTrainState.finishFrontEdgePoint(train, cabSignalSnapshot, stopped);
			return stopped;
		});
	}

	@Inject(method = "tick", at = @At("TAIL"), remap = false)
	private void railward$syncBufferLightsScheduledDirection(Level level, CallbackInfo ci) {
		Train train = (Train) (Object) this;
		TrainDirectionState.syncScheduledControls(train, level);
		ModernTrainControllerState.syncScheduledControls(train);
		DieselExhaustEmitter.tick(train, level);
	}

	@Inject(method = "write", at = @At("RETURN"), remap = false)
	private void railward$writeBufferLightsDirection(DimensionPalette dimensions,
		CallbackInfoReturnable<CompoundTag> cir) {
		Train train = (Train) (Object) this;
		TrainDirectionState.writeScheduledDirection(train, cir.getReturnValue());
		CabSignalTrainState.write(train, cir.getReturnValue());
		RelayCaseRailwayManager.write(train, cir.getReturnValue());
	}

	@Inject(method = "read", at = @At("RETURN"), remap = false)
	private static void railward$readBufferLightsDirection(CompoundTag tag, Map<UUID, TrackGraph> trackNetworks,
		DimensionPalette dimensions, CallbackInfoReturnable<Train> cir) {
		Train train = cir.getReturnValue();
		TrainDirectionState.readScheduledDirection(train, tag);
		CabSignalTrainState.read(train, tag);
		RelayCaseRailwayManager.read(train, tag);
	}
}
