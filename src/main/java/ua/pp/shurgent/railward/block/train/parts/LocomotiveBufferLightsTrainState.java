package ua.pp.shurgent.railward.block.train.parts;

/**
 * Compatibility facade kept for worlds and integrations built against the old
 * buffer-lights-specific class name. Train direction is now owned by
 * {@link TrainDirectionState} and shared by all locomotive equipment.
 */
@Deprecated
public final class LocomotiveBufferLightsTrainState extends TrainDirectionState {

	private LocomotiveBufferLightsTrainState() {
	}
}
