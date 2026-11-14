package ua.pp.shurgent.railward.mixin.create;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.simibubi.create.content.trains.graph.TrackGraph;

import ua.pp.shurgent.railward.compat.RelayGraphVersion;

@Mixin(value = TrackGraph.class, remap = false)
public class RelayTrackGraphMixin implements RelayGraphVersion {
	@Unique
	private long railward$relayRevision;

	@Override
	public long railward$getRelayRevision() {
		return railward$relayRevision;
	}

	@Inject(method = { "invalidateBounds", "addPoint", "disconnectNodes", "transferAll" }, at = @At("RETURN"), remap = false)
	private void railward$relayTopologyChanged(CallbackInfo ci) {
		railward$relayRevision++;
	}

	@Inject(method = { "removePoint", "putConnection" }, at = @At("RETURN"), remap = false)
	private void railward$relayConnectionChanged(CallbackInfoReturnable<?> cir) {
		railward$relayRevision++;
	}
}
