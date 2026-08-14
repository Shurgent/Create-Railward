package ua.pp.shurgent.railward.mixin.create;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.simibubi.create.AllPackets;
import com.simibubi.create.content.trains.HonkPacket;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.graph.DiscoveredPath;
import com.simibubi.create.content.trains.schedule.Schedule;
import com.simibubi.create.content.trains.schedule.ScheduleRuntime;
import com.simibubi.create.content.trains.schedule.ScheduleRuntime.State;

import net.minecraft.world.level.Level;
import net.minecraftforge.network.PacketDistributor;
import ua.pp.shurgent.railward.RailwardPackets;
import ua.pp.shurgent.railward.network.ModernWhistleSecondaryHonkPacket;
import ua.pp.shurgent.railward.schedule.ModernWhistleHonkInstruction;

@Mixin(value = ScheduleRuntime.class, remap = false)
public abstract class ScheduleRuntimeMixin {

	@Shadow
	private Train train;
	@Shadow
	private Schedule schedule;
	@Shadow
	public boolean paused;
	@Shadow
	public int currentEntry;
	@Shadow
	public State state;

	@Unique
	private Level railward$tickLevel;
	@Unique
	private int railward$honkEntry = -1;
	@Unique
	private int railward$honkTicksRemaining;
	@Unique
	private int railward$primaryKeepAlive;
	@Unique
	private boolean railward$secondaryHonk;

	@Inject(method = "tick", at = @At("HEAD"), remap = false)
	private void railward$beforeScheduleTick(Level level, CallbackInfo ci) {
		railward$tickLevel = level;
		if (!level.isClientSide && railward$honkEntry >= 0 && !railward$isActiveInstructionValid())
			railward$stopScheduledHonk();
	}

	@Inject(method = "tick", at = @At("RETURN"), remap = false)
	private void railward$afterScheduleTick(Level level, CallbackInfo ci) {
		railward$tickLevel = null;
	}

	@Inject(method = "reset", at = @At("HEAD"), remap = false)
	private void railward$resetScheduledHonk(CallbackInfo ci) {
		railward$stopScheduledHonk();
	}

	@Inject(method = "startCurrentInstruction", at = @At("HEAD"), cancellable = true, remap = false)
	private void railward$runHonkInstruction(CallbackInfoReturnable<DiscoveredPath> cir) {
		if (railward$tickLevel == null || railward$tickLevel.isClientSide || schedule == null
			|| currentEntry < 0 || currentEntry >= schedule.entries.size())
			return;
		if (!(schedule.entries.get(currentEntry).instruction instanceof ModernWhistleHonkInstruction instruction))
			return;

		if (railward$honkEntry != currentEntry) {
			railward$stopScheduledHonk();
			railward$honkEntry = currentEntry;
			railward$secondaryHonk = instruction.isSecondary();
			railward$honkTicksRemaining = instruction.getDurationTicks();
			railward$primaryKeepAlive = 5;
			railward$sendScheduledHonk(true);
		}

		if (!railward$secondaryHonk && --railward$primaryKeepAlive <= 0) {
			railward$sendScheduledHonk(true);
			railward$primaryKeepAlive = 5;
		}

		if (--railward$honkTicksRemaining <= 0) {
			railward$stopScheduledHonk();
			state = State.PRE_TRANSIT;
			currentEntry++;
		}
		cir.setReturnValue(null);
	}

	@Unique
	private boolean railward$isActiveInstructionValid() {
		if (schedule == null || paused || train.derailed || train.navigation.destination != null
			|| state != State.PRE_TRANSIT || currentEntry != railward$honkEntry
			|| currentEntry < 0 || currentEntry >= schedule.entries.size())
			return false;
		if (!(schedule.entries.get(currentEntry).instruction instanceof ModernWhistleHonkInstruction instruction))
			return false;
		return instruction.isSecondary() == railward$secondaryHonk;
	}

	@Unique
	private void railward$stopScheduledHonk() {
		if (railward$honkEntry < 0)
			return;
		railward$sendScheduledHonk(false);
		railward$honkEntry = -1;
		railward$honkTicksRemaining = 0;
		railward$primaryKeepAlive = 0;
	}

	@Unique
	private void railward$sendScheduledHonk(boolean active) {
		if (railward$secondaryHonk) {
			RailwardPackets.getChannel()
				.send(PacketDistributor.ALL.noArg(), new ModernWhistleSecondaryHonkPacket(train.id, active));
			return;
		}
		AllPackets.getChannel()
			.send(PacketDistributor.ALL.noArg(), new HonkPacket(train, active));
	}
}
