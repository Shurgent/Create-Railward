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
import ua.pp.shurgent.railward.block.train.parts.LocomotiveBufferLightsTrainState;

@Mixin(value = Train.class, remap = false)
public abstract class TrainMixin {

	@Inject(method = "tick", at = @At("TAIL"), remap = false)
	private void railward$syncBufferLightsScheduledDirection(Level level, CallbackInfo ci) {
		LocomotiveBufferLightsTrainState.syncScheduledControls((Train) (Object) this, level);
	}

	@Inject(method = "write", at = @At("RETURN"), remap = false)
	private void railward$writeBufferLightsDirection(DimensionPalette dimensions,
		CallbackInfoReturnable<CompoundTag> cir) {
		LocomotiveBufferLightsTrainState.writeScheduledDirection((Train) (Object) this, cir.getReturnValue());
	}

	@Inject(method = "read", at = @At("RETURN"), remap = false)
	private static void railward$readBufferLightsDirection(CompoundTag tag, Map<UUID, TrackGraph> trackNetworks,
		DimensionPalette dimensions, CallbackInfoReturnable<Train> cir) {
		LocomotiveBufferLightsTrainState.readScheduledDirection(cir.getReturnValue(), tag);
	}
}
