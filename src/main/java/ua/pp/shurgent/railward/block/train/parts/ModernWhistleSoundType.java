package ua.pp.shurgent.railward.block.train.parts;

import com.simibubi.create.content.decoration.steamWhistle.WhistleBlock.WhistleSize;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.StringRepresentable;
import ua.pp.shurgent.railward.RailwardSoundEvents;

public enum ModernWhistleSoundType implements StringRepresentable {

	HORN_1("horn_1", 4, 20),
	HORN_2("horn_2", 8, 30),
	WHISTLE("whistle", 4, 28);

	private final String serializedName;
	private final int loopStartDelayTicks;
	private final int arrivalLoopTicks;

	ModernWhistleSoundType(String serializedName, int loopStartDelayTicks, int arrivalLoopTicks) {
		this.serializedName = serializedName;
		this.loopStartDelayTicks = loopStartDelayTicks;
		this.arrivalLoopTicks = arrivalLoopTicks;
	}

	public SoundEvent getStartEvent() {
		return switch (this) {
		case HORN_1 -> RailwardSoundEvents.MODERN_WHISTLE_HORN_1_START.get();
		case HORN_2 -> RailwardSoundEvents.MODERN_WHISTLE_HORN_2_START.get();
		case WHISTLE -> RailwardSoundEvents.MODERN_WHISTLE_WHISTLE_START.get();
		};
	}

	public SoundEvent getLoopEvent() {
		return switch (this) {
		case HORN_1 -> RailwardSoundEvents.MODERN_WHISTLE_HORN_1_LOOP.get();
		case HORN_2 -> RailwardSoundEvents.MODERN_WHISTLE_HORN_2_LOOP.get();
		case WHISTLE -> RailwardSoundEvents.MODERN_WHISTLE_WHISTLE_LOOP.get();
		};
	}

	public SoundEvent getEndEvent() {
		return switch (this) {
		case HORN_1 -> RailwardSoundEvents.MODERN_WHISTLE_HORN_1_END.get();
		case HORN_2 -> RailwardSoundEvents.MODERN_WHISTLE_HORN_2_END.get();
		case WHISTLE -> RailwardSoundEvents.MODERN_WHISTLE_WHISTLE_END.get();
		};
	}

	public WhistleSize getTrainWhistleSize() {
		return WhistleSize.MEDIUM;
	}

	public int getTrainPitch() {
		return 0;
	}

	public int getLoopStartDelayTicks() {
		return loopStartDelayTicks;
	}

	public int getArrivalLoopTicks() {
		return arrivalLoopTicks;
	}

	@Override
	public String getSerializedName() {
		return serializedName;
	}

}
