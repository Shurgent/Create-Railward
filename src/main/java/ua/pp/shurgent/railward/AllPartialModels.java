package ua.pp.shurgent.railward;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.jozufozu.flywheel.core.PartialModel;

import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import ua.pp.shurgent.railward.block.train.parts.door.CabCornerDoorPanelVariant;

public class AllPartialModels {

	public static final PartialModel

	// Bogeys
	BOGEY_18_100_FRAME = block("train/bogey/18_100/bogey_18_100_frame"),
	BOGEY_18_100_WHEELSET = block("train/bogey/18_100/bogey_18_100_wheelset"),
	BOGEY_TRIPLE_AXLE_EQUALIZED_LOCOMOTIVE_FRAME =
		block("train/bogey/triple_axle_equalized_locomotive/bogey_triple_axle_equalized_locomotive_frame"),
	BOGEY_TRIPLE_AXLE_EQUALIZED_LOCOMOTIVE_WHEELSET =
		block("train/bogey/triple_axle_equalized_locomotive/bogey_triple_axle_equalized_locomotive_wheelset"),

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

	// Whistle Sign
	WHISTLE_SIGN = block("train/signal/whistle_sign"),
	SPEED_SIGN = block("train/signal/speed_sign"),

	// Modern Whistle
	MODERN_WHISTLE_BASE = block("train/parts/modern_whistle_base"),
	MODERN_WHISTLE_HORN_1 = block("train/parts/modern_whistle_horn_1"),
	MODERN_WHISTLE_HORN_2 = block("train/parts/modern_whistle_horn_2"),
	MODERN_WHISTLE_WHISTLE = block("train/parts/modern_whistle_whistle"),

	// Pilots
	PILOT_TYPE_1_LEFT = block("train/parts/pilot_type_1_left"),
	PILOT_TYPE_1_RIGHT = block("train/parts/pilot_type_1_right"),
	PILOT_TYPE_2_LEFT = block("train/parts/pilot_type_2_left"),
	PILOT_TYPE_2_RIGHT = block("train/parts/pilot_type_2_right"),

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

	// Radiator Fan
	RADIATOR_FAN_BASE = block("train/parts/radiator_fan_base"),
	RADIATOR_FAN_ROTOR = block("train/parts/radiator_fan_rotor"),

	// Railings
	RAILING_VERTICAL_CORE = block("train/parts/railing/vertical_core"),
	RAILING_HORIZONTAL_X_CORE = block("train/parts/railing/horizontal_x_core"),
	RAILING_HORIZONTAL_Z_CORE = block("train/parts/railing/horizontal_z_core"),
	RAILING_CONNECTOR = block("train/parts/railing/connector"),
	RAILING_CONNECTOR_HALF = block("train/parts/railing/connector_half"),

	// Train Coupler
	TRAIN_COUPLER = block("train/parts/train_coupler"),
	TRAIN_COUPLER_STATIC = block("train/parts/train_coupler_static"),
	TRAIN_COUPLER_DRAWBAR = block("train/parts/train_coupler_drawbar"),
	TRAIN_COUPLER_HEAD = block("train/parts/train_coupler_head"),
	TRAIN_COUPLER_LEVER_DYNAMIC = block("train/parts/train_coupler_lever_dynamic"),

	// Modern Train Controller
	MODERN_TRAIN_CONTROLLER_BRAKE = block("train/parts/modern_train_controller/brake_handle"),
	MODERN_TRAIN_CONTROLLER_REVERSER = block("train/parts/modern_train_controller/reverse_handle"),
	MODERN_TRAIN_CONTROLLER_THROTTLE = block("train/parts/modern_train_controller/controller_handle"),

	// Fluid Loading
	CISTERN_HATCH_LID = block("train/station/cistern_hatch_lid"),
	FLUID_TERMINAL_HOSE_UPPER = block("train/station/fluid_terminal_hose_upper"),
	FLUID_TERMINAL_HOSE_LOWER = block("train/station/fluid_terminal_hose_lower"),

	// Cab Signal
	CAB_SIGNAL_BASE = block("train/parts/cab_signal_base"),
	CAB_SIGNAL_OFF = block("train/parts/cab_signal_off"),
	CAB_SIGNAL_GREEN = block("train/parts/cab_signal_green"),
	CAB_SIGNAL_YELLOW = block("train/parts/cab_signal_yellow"),
	CAB_SIGNAL_YELLOW_RED = block("train/parts/cab_signal_yellow_red"),
	CAB_SIGNAL_RED = block("train/parts/cab_signal_red"),
	CAB_SIGNAL_WHITE = block("train/parts/cab_signal_white")

	;

	public static final LocomotiveBufferLightsPartialModels LOCOMOTIVE_BUFFER_LIGHTS_1 =
		locomotiveBufferLights("1");
	public static final LocomotiveBufferLightsPartialModels LOCOMOTIVE_BUFFER_LIGHTS_2 =
		locomotiveBufferLights("2");
	public static final LocomotiveBufferLightsPartialModels LOCOMOTIVE_BUFFER_LIGHTS_3 =
		locomotiveBufferLights("3");
	public static final LocomotiveBufferLightsPartialModels LOCOMOTIVE_BUFFER_LIGHTS_4 =
		locomotiveBufferLights("4");
	public static final LocomotiveBufferLightsPartialModels LOCOMOTIVE_BUFFER_LIGHTS_5 =
		locomotiveBufferLights("5");
	public static final LocomotiveBufferLightsPartialModels LOCOMOTIVE_BUFFER_LIGHTS_6 =
		locomotiveBufferLights("6");
	public static final LocomotiveBufferLightsPartialModels LOCOMOTIVE_BUFFER_LIGHTS_7 =
		locomotiveBufferLights("7");
	public static final LocomotiveBufferLightsPartialModels LOCOMOTIVE_BUFFER_LIGHTS_8 =
		locomotiveBufferLights("8");
	public static final LocomotiveBufferLightsPartialModels LOCOMOTIVE_BUFFER_LIGHTS_9 =
		locomotiveBufferLights("9");
	public static final LocomotiveBufferLightsPartialModels LOCOMOTIVE_BUFFER_LIGHTS_10 =
		locomotiveBufferLights("10");
	public static final LocomotiveBufferLightsPartialModels LOCOMOTIVE_BUFFER_LIGHTS_11 =
		locomotiveBufferLights("11");
	public static final LocomotiveSpotlightPartialModels LOCOMOTIVE_SPOTLIGHT_SMALL = locomotiveSpotlight("1");
	public static final LocomotiveSpotlightPartialModels LOCOMOTIVE_SPOTLIGHT_LARGE = locomotiveSpotlight("2");

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
	private static final Map<String, CabCornerDoorPartialModels> CAB_CORNER_TRAIN_DOORS =
		createCabCornerTrainDoors();

	private static PartialModel block(String path) {
		return new PartialModel(Railward.asResource("block/" + path));
	}

	private static LocomotiveBufferLightsPartialModels locomotiveBufferLights(String suffix) {
		String base = "train/parts/locomotive_buffer_lights_" + suffix;
		return new LocomotiveBufferLightsPartialModels(
			block(base + "_base"),
			block(base + "_white_lamp_on"),
			block(base + "_white_lamp_off"),
			block(base + "_white_lens_off"),
			block(base + "_white_glow"),
			block(base + "_red_lamp_on"),
			block(base + "_red_lamp_off"),
			block(base + "_red_lens_off"),
			block(base + "_red_glow"));
	}

	private static LocomotiveSpotlightPartialModels locomotiveSpotlight(String suffix) {
		String base = "train/parts/locomotive_spotlight_" + suffix;
		return new LocomotiveSpotlightPartialModels(
			block(base + "_base"),
			block(base + "_lamp_on"),
			block(base + "_lamp_off"),
			block(base + "_lens_off"),
			block(base + "_glow"));
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

	public static CabCornerDoorPartialModels getCabCornerDoorModels(String blockPath) {
		return CAB_CORNER_TRAIN_DOORS.get(blockPath);
	}

	private static Map<String, CabCornerDoorPartialModels> createCabCornerTrainDoors() {
		Map<String, CabCornerDoorPartialModels> models = new HashMap<>();
		for (String color : TRAIN_DOOR_WINDOW_COLORS) {
			String id = "train_cab_corner_door_" + color + "_window_big";
			String doorBase = "train/parts/train_hinged_door_" + color + "_window_big/";
			Map<CabCornerDoorPanelVariant, PartialModel> panels =
				new java.util.EnumMap<>(CabCornerDoorPanelVariant.class);
			for (CabCornerDoorPanelVariant variant : CabCornerDoorPanelVariant.values())
				panels.put(variant, block("train/parts/train_wall_panel_" + color
					+ variant.getModelSuffix()));
			models.put("train/parts/" + id, new CabCornerDoorPartialModels(
				block(doorBase + "block_bottom"),
				block(doorBase + "block_bottom_hinge"),
				block(doorBase + "block_top"),
				block(doorBase + "block_top_hinge"),
				Collections.unmodifiableMap(panels)));
		}
		return Collections.unmodifiableMap(models);
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

	public record LocomotiveBufferLightsPartialModels(PartialModel base, PartialModel whiteLampOn,
		PartialModel whiteLampOff, PartialModel whiteLensOff, PartialModel whiteGlow, PartialModel redLampOn,
		PartialModel redLampOff, PartialModel redLensOff, PartialModel redGlow) {
	}

	public record LocomotiveSpotlightPartialModels(PartialModel base, PartialModel lampOn, PartialModel lampOff,
		PartialModel lensOff, PartialModel glow) {
	}

	public record CabCornerDoorPartialModels(PartialModel bottomLeft, PartialModel bottomRight,
		PartialModel topLeft, PartialModel topRight,
		Map<CabCornerDoorPanelVariant, PartialModel> panels) {

		public PartialModel getDoor(DoubleBlockHalf half, boolean rightHinge) {
			if (half == DoubleBlockHalf.UPPER)
				return rightHinge ? topRight : topLeft;
			return rightHinge ? bottomRight : bottomLeft;
		}

		public PartialModel getPanel(CabCornerDoorPanelVariant variant) {
			return panels.get(variant);
		}
	}
	
	public static void init() {
	}
}
