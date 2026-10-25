package ua.pp.shurgent.railward.mixin.create.client;

//? if create6 {
/*import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.simibubi.create.content.trains.signal.SignalBlockEntity;
import com.simibubi.create.content.trains.signal.SignalVisual;

import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.api.visual.SectionTrackedVisual.SectionCollector;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.visual.AbstractBlockEntityVisual;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;

@Mixin(value = SignalVisual.class, remap = false)
public abstract class SignalVisualMixin extends AbstractBlockEntityVisual<SignalBlockEntity> {

	@Shadow
	@Final
	private TransformedInstance signalOverlay;

	protected SignalVisualMixin(VisualizationContext context, SignalBlockEntity blockEntity, float partialTick) {
		super(context, blockEntity, partialTick);
	}

	@Override
	public void setSectionCollector(SectionCollector sectionCollector) {
		this.lightSections = sectionCollector;
		BlockPos targetPosition = blockEntity.edgePoint.getGlobalPosition();
		LongSet sections = new LongOpenHashSet(2);
		sections.add(SectionPos.asLong(pos));
		sections.add(SectionPos.asLong(targetPosition));
		sectionCollector.sections(sections);
	}

	@Inject(method = "updateLight", at = @At("TAIL"), remap = false)
	private void railward$relightOverlayAtTrack(float partialTick, CallbackInfo ci) {
		// Create 6 relights both the signal indicator and its remote track overlay at the
		// signal block. The old renderer sampled the overlay at the target track instead.
		relight(blockEntity.edgePoint.getGlobalPosition(), signalOverlay);
	}
}
*///?}
