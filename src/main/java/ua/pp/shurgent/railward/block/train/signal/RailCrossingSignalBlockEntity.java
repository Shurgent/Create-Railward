package ua.pp.shurgent.railward.block.train.signal;

import java.util.List;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.registries.RegistryObject;
import ua.pp.shurgent.railward.RailwardSoundEvents;
import ua.pp.shurgent.railward.client.sound.RailCrossingSignalAudio;

public class RailCrossingSignalBlockEntity extends SmartBlockEntity {

	private static final int FLASH_TICKS = 10;
	private static final long INACTIVE = -1;

	private BlockPos cabinet;
	private long activeSince = INACTIVE;
	private CrossingSign sign = CrossingSign.SINGLE_TRACK;
	private CrossingSound sound = CrossingSound.SMALL_BELL;

	public RailCrossingSignalBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
	}

	@Override
	public void tick() {
		super.tick();
		if (level == null)
			return;
		if (level.isClientSide) {
			DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> RailCrossingSignalAudio.update(this));
			return;
		}

		boolean occupied = cabinet != null && level.isLoaded(cabinet)
			&& level.getBlockEntity(cabinet) instanceof RelayCaseBlockEntity relay && relay.isOccupied();
		if (occupied != isActive()) {
			activeSince = occupied ? level.getGameTime() : INACTIVE;
			sendData();
		}
	}

	void bindTo(BlockPos cabinet) {
		this.cabinet = cabinet;
		activeSince = INACTIVE;
		notifyUpdate();
	}

	public void configure(CrossingSign sign, CrossingSound sound) {
		if (this.sign == sign && this.sound == sound)
			return;
		this.sign = sign;
		this.sound = sound;
		notifyUpdate();
	}

	public CrossingSign getSign() {
		return sign;
	}

	public CrossingSound getSound() {
		return sound;
	}

	public boolean isActive() {
		return activeSince != INACTIVE;
	}

	@Override
	public void invalidate() {
		super.invalidate();
		if (level != null && level.isClientSide)
			DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> RailCrossingSignalAudio.stop(this));
	}

	public Lamp getLamp() {
		if (!isActive() || level == null)
			return Lamp.OFF;
		return Lamp.flashing(level.getGameTime() - activeSince);
	}

	@Override
	protected void write(CompoundTag tag, boolean clientPacket) {
		super.write(tag, clientPacket);
		tag.putString("CrossingSign", sign.name());
		tag.putString("CrossingSound", sound.name());
		if (clientPacket) {
			tag.putLong("ActiveSince", activeSince);
		} else if (cabinet != null) {
			tag.putLong("CabinetPos", cabinet.asLong());
		}
	}

	@Override
	protected void read(CompoundTag tag, boolean clientPacket) {
		super.read(tag, clientPacket);
		sign = readOption(CrossingSign.class, tag.getString("CrossingSign"), CrossingSign.SINGLE_TRACK);
		sound = readOption(CrossingSound.class, tag.getString("CrossingSound"), CrossingSound.SMALL_BELL);
		if (clientPacket) {
			activeSince = tag.contains("ActiveSince", Tag.TAG_LONG) ? tag.getLong("ActiveSince") : INACTIVE;
		} else {
			cabinet = tag.contains("CabinetPos", Tag.TAG_LONG) ? BlockPos.of(tag.getLong("CabinetPos")) : null;
			activeSince = INACTIVE;
		}
	}

	private static <T extends Enum<T>> T readOption(Class<T> type, String name, T fallback) {
		try {
			return Enum.valueOf(type, name);
		} catch (IllegalArgumentException ignored) {
			return fallback;
		}
	}

	public enum CrossingSign {
		NONE, SINGLE_TRACK, MULTIPLE_TRACKS
	}

	public enum CrossingSound {
		SMALL_BELL(RailwardSoundEvents.CROSSING_BELL),
		BELL(RailwardSoundEvents.CROSSING_RING),
		SIREN(RailwardSoundEvents.CROSSING_SIREN);

		private final RegistryObject<SoundEvent> event;

		CrossingSound(RegistryObject<SoundEvent> event) {
			this.event = event;
		}

		public SoundEvent getEvent() {
			return event.get();
		}
	}

	public enum Lamp {
		OFF, LEFT, RIGHT;

		public static Lamp flashing(long elapsedTicks) {
			return (Math.max(0, elapsedTicks) / FLASH_TICKS) % 2 == 0 ? LEFT : RIGHT;
		}
	}
}
