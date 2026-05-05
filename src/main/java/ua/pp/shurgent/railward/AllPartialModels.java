package ua.pp.shurgent.railward;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.jozufozu.flywheel.core.PartialModel;

import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

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

	private static final String[] TRAIN_DOOR_SOLID_VARIANTS = {
			"blue",
			"green",
			"light_blue"
	};
	private static final String[] TRAIN_DOOR_WOOD_VARIANTS = {
			"stripped_oak",
			"stripped_spruce",
			"stripped_birch",
			"stripped_jungle",
			"stripped_acacia",
			"stripped_dark_oak",
			"stripped_crimson",
			"stripped_warped"
	};
	private static final String[] TRAIN_DOOR_WINDOW_COLORS = {
			"blue",
			"green",
			"light_blue"
	};

	private static final Map<String, SlidingDoorPartialModels> SLIDING_TRAIN_DOORS = createSlidingTrainDoors();

	private static PartialModel block(String path) {
		return new PartialModel(Railward.asResource("block/" + path));
	}

	public static SlidingDoorPartialModels getSlidingDoorModels(String blockPath) {
		return SLIDING_TRAIN_DOORS.get(blockPath);
	}

	private static Map<String, SlidingDoorPartialModels> createSlidingTrainDoors() {
		Map<String, SlidingDoorPartialModels> models = new HashMap<>();
		for (String variant : TRAIN_DOOR_SOLID_VARIANTS)
			addSlidingDoor(models, "train_sliding_door_" + variant);
		for (String variant : TRAIN_DOOR_WOOD_VARIANTS)
			addSlidingDoor(models, "train_sliding_door_" + variant);
		for (String color : TRAIN_DOOR_WINDOW_COLORS) {
			addSlidingDoor(models, "train_sliding_door_" + color + "_window_small");
			addSlidingDoor(models, "train_sliding_door_" + color + "_window_big");
		}
		return Collections.unmodifiableMap(models);
	}

	private static void addSlidingDoor(Map<String, SlidingDoorPartialModels> models, String id) {
		String base = "train/parts/" + id;
		models.put(base, new SlidingDoorPartialModels(
				block(base + "/block_bottom_left"),
				block(base + "/block_bottom_right"),
				block(base + "/block_top_left"),
				block(base + "/block_top_right")));
	}

	public static class SlidingDoorPartialModels {
		private final PartialModel bottomLeft;
		private final PartialModel bottomRight;
		private final PartialModel topLeft;
		private final PartialModel topRight;

		public SlidingDoorPartialModels(PartialModel bottomLeft, PartialModel bottomRight, PartialModel topLeft,
				PartialModel topRight) {
			this.bottomLeft = bottomLeft;
			this.bottomRight = bottomRight;
			this.topLeft = topLeft;
			this.topRight = topRight;
		}

		public PartialModel get(DoubleBlockHalf half, boolean left) {
			if (half == DoubleBlockHalf.UPPER)
				return left ? topLeft : topRight;
			return left ? bottomLeft : bottomRight;
		}
	}
	
	public static void init() {
	}
}
