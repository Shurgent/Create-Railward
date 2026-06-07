package ua.pp.shurgent.railward;

import static ua.pp.shurgent.railward.Railward.REGISTRATE;

import com.simibubi.create.content.decoration.slidingDoor.SlidingDoorBlockEntity;
import com.tterrag.registrate.util.entry.BlockEntityEntry;

import ua.pp.shurgent.railward.block.train.signal.ModernSignalBlockEntity;
import ua.pp.shurgent.railward.block.train.signal.ModernSignalGroundBlockEntity;
import ua.pp.shurgent.railward.block.train.signal.SpeedSignBlockEntity;
import ua.pp.shurgent.railward.block.train.signal.WhistleSignBlockEntity;
import ua.pp.shurgent.railward.client.renderer.ModernSignalGroundRenderer;
import ua.pp.shurgent.railward.client.renderer.ModernSignalRenderer;
import ua.pp.shurgent.railward.client.renderer.SpeedSignRenderer;
import ua.pp.shurgent.railward.client.renderer.WhistleSignRenderer;
import ua.pp.shurgent.railward.block.train.parts.ModernWhistleBlockEntity;
import ua.pp.shurgent.railward.block.train.parts.BulkCargoContainerBlockEntity;
import ua.pp.shurgent.railward.block.train.parts.CisternBlockEntity;
import ua.pp.shurgent.railward.block.train.parts.TrainCouplerBlockEntity;
import ua.pp.shurgent.railward.block.train.station.CisternHatchBlockEntity;
import ua.pp.shurgent.railward.block.train.station.FluidTerminalBlockEntity;
import ua.pp.shurgent.railward.block.train.station.UnloadingBunkerBlockEntity;
import ua.pp.shurgent.railward.block.train.station.UnloadingHatchBlockEntity;
import ua.pp.shurgent.railward.block.train.parts.WagonFrameBlockEntity;
import ua.pp.shurgent.railward.client.renderer.BulkCargoContainerRenderer;
import ua.pp.shurgent.railward.client.renderer.CisternHatchRenderer;
import ua.pp.shurgent.railward.client.renderer.CisternRenderer;
import ua.pp.shurgent.railward.client.renderer.FluidTerminalRenderer;
import ua.pp.shurgent.railward.client.renderer.ModernWhistleRenderer;
import ua.pp.shurgent.railward.client.renderer.RailwardHingedDoorRenderer;
import ua.pp.shurgent.railward.client.renderer.RailwardSlidingDoorRenderer;
import ua.pp.shurgent.railward.client.renderer.TrainCouplerRenderer;
import ua.pp.shurgent.railward.client.renderer.UnloadingBunkerRenderer;
import ua.pp.shurgent.railward.client.renderer.UnloadingHatchRenderer;
import ua.pp.shurgent.railward.client.renderer.WagonFrameRenderer;

public class AllBlockEntities {

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

	public static final BlockEntityEntry<WagonFrameBlockEntity> WAGON_FRAME = REGISTRATE
			.blockEntity("train/parts/wagon_frame", WagonFrameBlockEntity::new)
			.validBlocks(AllBlocks.WAGON_FRAME)
			.renderer(() -> WagonFrameRenderer::new)
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
