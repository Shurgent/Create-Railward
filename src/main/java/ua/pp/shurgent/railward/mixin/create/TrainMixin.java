package ua.pp.shurgent.railward.mixin.create;

import java.util.Map;
import java.util.UUID;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.graph.DimensionPalette;
import com.simibubi.create.content.trains.graph.TrackGraph;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import ua.pp.shurgent.railward.block.train.parts.CabSignalTrainState;
import ua.pp.shurgent.railward.block.train.parts.TrainDirectionState;

@Mixin(value = Train.class, remap = false)
public abstract class TrainMixin {

	@Inject(method = "tick", at = @At("TAIL"), remap = false)
	private void railward$syncBufferLightsScheduledDirection(Level level, CallbackInfo ci) {
		TrainDirectionState.syncScheduledControls((Train) (Object) this, level);
	}

	@Inject(method = "write", at = @At("RETURN"), remap = false)
	private void railward$writeBufferLightsDirection(DimensionPalette dimensions,
		CallbackInfoReturnable<CompoundTag> cir) {
		Train train = (Train) (Object) this;
		TrainDirectionState.writeScheduledDirection(train, cir.getReturnValue());
		CabSignalTrainState.write(train, cir.getReturnValue());
	}

	@Inject(method = "read", at = @At("RETURN"), remap = false)
	private static void railward$readBufferLightsDirection(CompoundTag tag, Map<UUID, TrackGraph> trackNetworks,
		DimensionPalette dimensions, CallbackInfoReturnable<Train> cir) {
		Train train = cir.getReturnValue();
		TrainDirectionState.readScheduledDirection(train, tag);
		CabSignalTrainState.read(train, tag);
	}
}
