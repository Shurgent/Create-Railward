package ua.pp.shurgent.railward;

import static ua.pp.shurgent.railward.Railward.REGISTRATE;

import com.simibubi.create.content.decoration.slidingDoor.SlidingDoorBlockEntity;
import com.tterrag.registrate.util.entry.BlockEntityEntry;

import ua.pp.shurgent.railward.block.train.signal.ModernSignalBlockEntity;
import ua.pp.shurgent.railward.block.train.signal.ModernSignalGroundBlockEntity;
import ua.pp.shurgent.railward.block.train.signal.SpeedSignBlockEntity;
import ua.pp.shurgent.railward.block.train.signal.WhistleSignBlockEntity;
import ua.pp.shurgent.railward.block.train.bogey.FreightBogey18_100BlockEntity;
import ua.pp.shurgent.railward.block.train.bogey.TripleAxleEqualizedLocomotiveBogeyBlockEntity;
import com.simibubi.create.content.trains.bogey.BogeyBlockEntityRenderer;
import ua.pp.shurgent.railward.client.renderer.ModernSignalGroundRenderer;
import ua.pp.shurgent.railward.client.renderer.ModernSignalRenderer;
import ua.pp.shurgent.railward.client.renderer.SpeedSignRenderer;
import ua.pp.shurgent.railward.client.renderer.WhistleSignRenderer;
import ua.pp.shurgent.railward.block.train.parts.ModernWhistleBlockEntity;
import ua.pp.shurgent.railward.block.train.parts.BulkCargoContainerBlockEntity;
import ua.pp.shurgent.railward.block.train.parts.CisternBlockEntity;
import ua.pp.shurgent.railward.block.train.parts.CabSignalBlockEntity;
import ua.pp.shurgent.railward.block.train.parts.LocomotiveBufferLightsBlockEntity;
import ua.pp.shurgent.railward.block.train.parts.LocomotiveSpotlightBlockEntity;
import ua.pp.shurgent.railward.block.train.parts.PilotBlockEntity;
import ua.pp.shurgent.railward.block.train.parts.RadiatorFanBlockEntity;
import ua.pp.shurgent.railward.block.train.parts.RailingBlockEntity;
import ua.pp.shurgent.railward.block.train.parts.TrainCouplerBlockEntity;
import ua.pp.shurgent.railward.block.train.parts.TrainWallPanelBlockEntity;
import ua.pp.shurgent.railward.block.train.parts.door.CabCornerDoorBlockEntity;
import ua.pp.shurgent.railward.block.train.parts.copycat.CopycatSlabBlockEntity;
import ua.pp.shurgent.railward.block.train.parts.copycat.DualPanelCopycatBlockEntity;
import ua.pp.shurgent.railward.block.train.station.CisternHatchBlockEntity;
import ua.pp.shurgent.railward.block.train.station.FluidTerminalBlockEntity;
import ua.pp.shurgent.railward.block.train.station.UnloadingBunkerBlockEntity;
import ua.pp.shurgent.railward.block.train.station.UnloadingHatchBlockEntity;
import ua.pp.shurgent.railward.block.train.parts.WagonFrameBlockEntity;
import ua.pp.shurgent.railward.client.renderer.BulkCargoContainerRenderer;
import ua.pp.shurgent.railward.client.renderer.CisternHatchRenderer;
import ua.pp.shurgent.railward.client.renderer.CisternRenderer;
import ua.pp.shurgent.railward.client.renderer.CabSignalRenderer;
import ua.pp.shurgent.railward.client.renderer.FluidTerminalRenderer;
import ua.pp.shurgent.railward.client.renderer.LocomotiveBufferLightsRenderer;
import ua.pp.shurgent.railward.client.renderer.LocomotiveSpotlightRenderer;
import ua.pp.shurgent.railward.client.renderer.PilotRenderer;
import ua.pp.shurgent.railward.client.renderer.RadiatorFanRenderer;
import ua.pp.shurgent.railward.client.renderer.ModernWhistleRenderer;
import ua.pp.shurgent.railward.client.renderer.RailingRenderer;
import ua.pp.shurgent.railward.client.renderer.RailwardHingedDoorRenderer;
import ua.pp.shurgent.railward.client.renderer.RailwardSlidingDoorRenderer;
import ua.pp.shurgent.railward.client.renderer.CabCornerDoorRenderer;
import ua.pp.shurgent.railward.client.renderer.TrainWallPanelAttachmentRenderer;
import ua.pp.shurgent.railward.client.renderer.TrainCouplerRenderer;
import ua.pp.shurgent.railward.client.renderer.UnloadingBunkerRenderer;
import ua.pp.shurgent.railward.client.renderer.UnloadingHatchRenderer;
import ua.pp.shurgent.railward.client.renderer.WagonFrameRenderer;

public class AllBlockEntities {

	public static final BlockEntityEntry<FreightBogey18_100BlockEntity> FREIGHT_BOGEY_18_100 = REGISTRATE
			.blockEntity("train/bogey/freight_18_100", FreightBogey18_100BlockEntity::new)
			.validBlocks(AllBlocks.FREIGHT_BOGEY_18_100)
			.renderer(() -> BogeyBlockEntityRenderer::new)
			.register();

	public static final BlockEntityEntry<TripleAxleEqualizedLocomotiveBogeyBlockEntity>
			TRIPLE_AXLE_EQUALIZED_LOCOMOTIVE_BOGEY = REGISTRATE
			.blockEntity("train/bogey/triple_axle_equalized_locomotive",
					TripleAxleEqualizedLocomotiveBogeyBlockEntity::new)
			.validBlocks(AllBlocks.TRIPLE_AXLE_EQUALIZED_LOCOMOTIVE_BOGEY)
			.renderer(() -> BogeyBlockEntityRenderer::new)
			.register();

	public static final BlockEntityEntry<DualPanelCopycatBlockEntity> WALL_PANEL_COPYCAT = REGISTRATE
			.blockEntity("train/parts/wall_panel_copycat", DualPanelCopycatBlockEntity::new)
			.validBlocks(AllBlocks.WALL_PANEL_VERTICAL_SLICE, AllBlocks.WALL_PANEL_HORIZONTAL_SLICE,
					AllBlocks.WALL_PANEL_CORNER, AllBlocks.COPYCAT_WALL_PANEL, AllBlocks.HALF_WALL_PANEL)
			.register();

	public static final BlockEntityEntry<CopycatSlabBlockEntity> COPYCAT_SLAB = REGISTRATE
			.blockEntity("train/parts/copycat_slab", CopycatSlabBlockEntity::new)
			.validBlocks(AllBlocks.COPYCAT_SLAB)
			.register();

	public static final BlockEntityEntry<SlidingDoorBlockEntity> HINGED_TRAIN_DOOR = REGISTRATE
			.blockEntity("train/parts/hinged_train_door", SlidingDoorBlockEntity::new)
			.validBlocks(AllBlocks.hingedTrainDoorBlocks())
			.renderer(() -> RailwardHingedDoorRenderer::new)
			.register();

	public static final BlockEntityEntry<SlidingDoorBlockEntity> SLIDING_TRAIN_DOOR = REGISTRATE
			.blockEntity("train/parts/sliding_train_door", SlidingDoorBlockEntity::new)
			.validBlocks(AllBlocks.slidingTrainDoorBlocks())
			.renderer(() -> RailwardSlidingDoorRenderer::new)
			.register();

	public static final BlockEntityEntry<CabCornerDoorBlockEntity> CAB_CORNER_TRAIN_DOOR = REGISTRATE
			.blockEntity("train/parts/cab_corner_train_door", CabCornerDoorBlockEntity::new)
			.validBlocks(AllBlocks.TRAIN_CAB_CORNER_DOOR_BLUE_WINDOW_BIG,
				AllBlocks.TRAIN_CAB_CORNER_DOOR_GREEN_WINDOW_BIG,
				AllBlocks.TRAIN_CAB_CORNER_DOOR_LIGHT_BLUE_WINDOW_BIG)
			.renderer(() -> CabCornerDoorRenderer::new)
			.register();

	public static final BlockEntityEntry<TrainWallPanelBlockEntity> TRAIN_WALL_PANEL = REGISTRATE
			.blockEntity("train/parts/train_wall_panel", TrainWallPanelBlockEntity::new)
			.validBlocks(AllBlocks.trainWallPanelBlocks())
			.renderer(() -> TrainWallPanelAttachmentRenderer::new)
			.register();

	public static final BlockEntityEntry<ModernSignalGroundBlockEntity> MODERN_SIGNAL_GROUND = REGISTRATE
			.blockEntity("train/signal/modern_signal_ground", ModernSignalGroundBlockEntity::new)
			.validBlocks(AllBlocks.MODERN_SIGNAL_GROUND)
			.renderer(() -> ModernSignalGroundRenderer::new)
			.register();

	public static final BlockEntityEntry<ModernSignalBlockEntity> MODERN_SIGNAL = REGISTRATE
			.blockEntity("train/signal/modern_signal", ModernSignalBlockEntity::new)
			.validBlocks(AllBlocks.MODERN_SIGNAL)
			.renderer(() -> ModernSignalRenderer::new)
			.register();

	public static final BlockEntityEntry<WhistleSignBlockEntity> WHISTLE_SIGN = REGISTRATE
			.blockEntity("train/signal/whistle_sign", WhistleSignBlockEntity::new)
			.validBlocks(AllBlocks.WHISTLE_SIGN)
			.renderer(() -> WhistleSignRenderer::new)
			.register();

	public static final BlockEntityEntry<SpeedSignBlockEntity> SPEED_SIGN = REGISTRATE
			.blockEntity("train/signal/speed_sign", SpeedSignBlockEntity::new)
			.validBlocks(AllBlocks.SPEED_SIGN)
			.renderer(() -> SpeedSignRenderer::new)
			.register();

	public static final BlockEntityEntry<ModernWhistleBlockEntity> MODERN_WHISTLE = REGISTRATE
			.blockEntity("train/parts/modern_whistle", ModernWhistleBlockEntity::new)
			.validBlocks(AllBlocks.MODERN_WHISTLE)
			.renderer(() -> ModernWhistleRenderer::new)
			.register();

	public static final BlockEntityEntry<LocomotiveBufferLightsBlockEntity> LOCOMOTIVE_BUFFER_LIGHTS = REGISTRATE
			.blockEntity("train/parts/locomotive_buffer_lights", LocomotiveBufferLightsBlockEntity::new)
			.validBlocks(AllBlocks.LOCOMOTIVE_BUFFER_LIGHTS)
			.renderer(() -> LocomotiveBufferLightsRenderer::new)
			.register();

	public static final BlockEntityEntry<LocomotiveSpotlightBlockEntity> LOCOMOTIVE_SPOTLIGHT = REGISTRATE
			.blockEntity("train/parts/locomotive_spotlight", LocomotiveSpotlightBlockEntity::new)
			.validBlocks(AllBlocks.LOCOMOTIVE_SPOTLIGHT)
			.renderer(() -> LocomotiveSpotlightRenderer::new)
			.register();

	public static final BlockEntityEntry<CabSignalBlockEntity> CAB_SIGNAL = REGISTRATE
			.blockEntity("train/parts/cab_signal", CabSignalBlockEntity::new)
			.validBlocks(AllBlocks.CAB_SIGNAL)
			.renderer(() -> CabSignalRenderer::new)
			.register();

	public static final BlockEntityEntry<PilotBlockEntity> PILOT_TYPE_1 = REGISTRATE
			.blockEntity("train/parts/pilot_type_1", PilotBlockEntity::new)
			.validBlocks(AllBlocks.PILOT_TYPE_1, AllBlocks.PILOT_TYPE_2)
			.renderer(() -> PilotRenderer::new)
			.register();

	public static final BlockEntityEntry<WagonFrameBlockEntity> WAGON_FRAME = REGISTRATE
			.blockEntity("train/parts/wagon_frame", WagonFrameBlockEntity::new)
			.validBlocks(AllBlocks.WAGON_FRAME)
			.renderer(() -> WagonFrameRenderer::new)
			.register();

	public static final BlockEntityEntry<RadiatorFanBlockEntity> RADIATOR_FAN = REGISTRATE
			.blockEntity("train/parts/radiator_fan", RadiatorFanBlockEntity::new)
			.validBlocks(AllBlocks.RADIATOR_FAN)
			.renderer(() -> RadiatorFanRenderer::new)
			.register();

	public static final BlockEntityEntry<RailingBlockEntity> RAILING = REGISTRATE
			.blockEntity("train/parts/railing", RailingBlockEntity::new)
			.validBlocks(AllBlocks.RAILING_RED, AllBlocks.RAILING_YELLOW, AllBlocks.RAILING_ORANGE,
				AllBlocks.RAILING_WHITE, AllBlocks.RAILING_GRAY)
			.renderer(() -> RailingRenderer::new)
			.register();

	public static final BlockEntityEntry<TrainCouplerBlockEntity> TRAIN_COUPLER = REGISTRATE
			.blockEntity("train/parts/train_coupler", TrainCouplerBlockEntity::new)
			.validBlocks(AllBlocks.TRAIN_COUPLER)
			.renderer(() -> TrainCouplerRenderer::new)
			.register();

	public static final BlockEntityEntry<BulkCargoContainerBlockEntity> BULK_CARGO_CONTAINER = REGISTRATE
			.blockEntity("train/parts/bulk_cargo_container", BulkCargoContainerBlockEntity::new)
			.validBlocks(AllBlocks.BULK_CARGO_CONTAINER, AllBlocks.BULK_CARGO_CONTAINER_BLUE,
				AllBlocks.BULK_CARGO_CONTAINER_LIGHT_BLUE)
			.renderer(() -> BulkCargoContainerRenderer::new)
			.register();

	public static final BlockEntityEntry<CisternBlockEntity> CISTERN = REGISTRATE
			.blockEntity("train/parts/cistern", CisternBlockEntity::new)
			.validBlocks(AllBlocks.CISTERN, AllBlocks.CISTERN_BLUE, AllBlocks.CISTERN_LIGHT_BLUE,
				AllBlocks.CISTERN_BLACK, AllBlocks.CISTERN_LIGHT_GRAY, AllBlocks.CISTERN_YELLOW,
				AllBlocks.CISTERN_ORANGE)
			.renderer(() -> CisternRenderer::new)
			.register();

	public static final BlockEntityEntry<CisternHatchBlockEntity> CISTERN_HATCH = REGISTRATE
			.blockEntity("train/station/cistern_hatch", CisternHatchBlockEntity::new)
			.validBlocks(AllBlocks.CISTERN_HATCH)
			.renderer(() -> CisternHatchRenderer::new)
			.register();

	public static final BlockEntityEntry<FluidTerminalBlockEntity> FLUID_TERMINAL = REGISTRATE
			.blockEntity("train/station/fluid_terminal", FluidTerminalBlockEntity::new)
			.validBlocks(AllBlocks.FLUID_TERMINAL)
			.renderer(() -> FluidTerminalRenderer::new)
			.register();

	public static final BlockEntityEntry<UnloadingHatchBlockEntity> UNLOADING_HATCH = REGISTRATE
			.blockEntity("train/station/unloading_hatch", UnloadingHatchBlockEntity::new)
			.validBlocks(AllBlocks.UNLOADING_HATCH)
			.renderer(() -> UnloadingHatchRenderer::new)
			.register();

	public static final BlockEntityEntry<UnloadingBunkerBlockEntity> UNLOADING_BUNKER = REGISTRATE
			.blockEntity("train/station/unloading_bunker", UnloadingBunkerBlockEntity::new)
			.validBlocks(AllBlocks.UNLOADING_BUNKER)
			.renderer(() -> UnloadingBunkerRenderer::new)
			.register();

	public static void register() {
	}
}
