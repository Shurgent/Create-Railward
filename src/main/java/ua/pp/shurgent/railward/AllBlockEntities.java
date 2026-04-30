package ua.pp.shurgent.railward;

import static ua.pp.shurgent.railward.Railward.REGISTRATE;

import com.tterrag.registrate.util.entry.BlockEntityEntry;

import ua.pp.shurgent.railward.block.train.signal.ModernSignalBlockEntity;
import ua.pp.shurgent.railward.block.train.signal.ModernSignalGroundBlockEntity;
import ua.pp.shurgent.railward.client.renderer.ModernSignalGroundRenderer;
import ua.pp.shurgent.railward.client.renderer.ModernSignalRenderer;
import ua.pp.shurgent.railward.block.train.parts.ModernWhistleBlockEntity;
import ua.pp.shurgent.railward.block.train.parts.BulkCargoContainerBlockEntity;
import ua.pp.shurgent.railward.block.train.parts.TrainCouplerBlockEntity;
import ua.pp.shurgent.railward.block.train.parts.WagonFrameBlockEntity;
import ua.pp.shurgent.railward.client.renderer.BulkCargoContainerRenderer;
import ua.pp.shurgent.railward.client.renderer.ModernWhistleRenderer;
import ua.pp.shurgent.railward.client.renderer.TrainCouplerRenderer;
import ua.pp.shurgent.railward.client.renderer.WagonFrameRenderer;

public class AllBlockEntities {

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
			.validBlocks(AllBlocks.BULK_CARGO_CONTAINER)
			.renderer(() -> BulkCargoContainerRenderer::new)
			.register();

	public static void register() {
	}
}
