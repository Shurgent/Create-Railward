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
	SIGNAL_GREEN_GLOW = block("train/signal/lamp_green_glow"),

	// Modern Whistle
	MODERN_WHISTLE_BASE = block("train/parts/modern_whistle_base"),
	MODERN_WHISTLE_HORN_1 = block("train/parts/modern_whistle_horn_1"),
	MODERN_WHISTLE_HORN_2 = block("train/parts/modern_whistle_horn_2"),
	MODERN_WHISTLE_WHISTLE = block("train/parts/modern_whistle_whistle"),

	// Wagon Frame
	WAGON_FRAME_BASE = block("train/parts/wagon_frame_base"),
	WAGON_FRAME_MEMBER_THIN = block("train/parts/wagon_frame_member_thin"),
	WAGON_FRAME_MEMBER_THICK = block("train/parts/wagon_frame_member_thick"),
	WAGON_FRAME_MEMBER_TAPER_THIN_TO_THICK = block("train/parts/wagon_frame_member_taper_thin_to_thick"),
	WAGON_FRAME_MEMBER_TAPER_THICK_TO_THIN = block("train/parts/wagon_frame_member_taper_thick_to_thin"),
	WAGON_FRAME_RECEIVER = block("train/parts/wagon_frame_receiver"),
	WAGON_FRAME_BRAKE_CYLINDER = block("train/parts/wagon_frame_brake_cylinder"),
	WAGON_FRAME_DISTRIBUTOR = block("train/parts/wagon_frame_distributor"),
	WAGON_FRAME_BRAKE_PIPE = block("train/parts/wagon_frame_brake_pipe"),
	WAGON_FRAME_FUEL_TANK = block("train/parts/wagon_frame_fuel_tank"),
	WAGON_FRAME_HAND_BRAKE_VALVE = block("train/parts/wagon_frame_hand_brake_valve"),

	// Train Coupler
	TRAIN_COUPLER = block("train/parts/train_coupler")

	;

	private static PartialModel block(String path) {
		return new PartialModel(Railward.asResource("block/" + path));
	}
	
	public static void init() {
	}
}
