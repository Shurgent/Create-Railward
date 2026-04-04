package ua.pp.shurgent.railward;

import com.jozufozu.flywheel.core.PartialModel;

public class AllPartialModels {

	public static final PartialModel

	// Modern Ground Signal
	SIGNAL_GROUND_BASE = block("train/signal/modern_signal_ground"),
	SIGNAL_GROUND_BLUE = block("train/signal/lamp_blue_off"),
	SIGNAL_GROUND_BLUE_LAMP = block("train/signal/lamp_blue_lamp"),
	SIGNAL_GROUND_BLUE_GLOW = block("train/signal/lamp_blue_glow"),
	SIGNAL_GROUND_WHITE = block("train/signal/lamp_white_off"),
	SIGNAL_GROUND_WHITE_LAMP = block("train/signal/lamp_white_lamp"),
	SIGNAL_GROUND_WHITE_GLOW = block("train/signal/lamp_white_glow"),

	// Modern Signal
	SIGNAL_BASE = block("train/signal/modern_signal"),
	SIGNAL_RED = block("train/signal/lamp_red_off"),
	SIGNAL_RED_LAMP = block("train/signal/lamp_red_lamp"),
	SIGNAL_RED_GLOW = block("train/signal/lamp_red_glow"),
	SIGNAL_YELLOW = block("train/signal/lamp_yellow_off"),
	SIGNAL_YELLOW_LAMP = block("train/signal/lamp_yellow_lamp"),
	SIGNAL_YELLOW_GLOW = block("train/signal/lamp_yellow_glow"),
	SIGNAL_GREEN = block("train/signal/lamp_green_off"),
	SIGNAL_GREEN_LAMP = block("train/signal/lamp_green_lamp"),
	SIGNAL_GREEN_GLOW = block("train/signal/lamp_green_glow")

	;

	private static PartialModel block(String path) {
		return new PartialModel(Railward.asResource("block/" + path));
	}
	
	public static void init() {
	}
}
