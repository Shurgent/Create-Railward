package ua.pp.shurgent.railward;

import static ua.pp.shurgent.railward.Railward.REGISTRATE;

import com.tterrag.registrate.util.entry.BlockEntityEntry;

import ua.pp.shurgent.railward.block.train.signal.ModernSignalBlockEntity;
import ua.pp.shurgent.railward.block.train.signal.ModernSignalGroundBlockEntity;
import ua.pp.shurgent.railward.client.renderer.ModernSignalGroundRenderer;
import ua.pp.shurgent.railward.client.renderer.ModernSignalRenderer;
import ua.pp.shurgent.railward.block.train.parts.ModernWhistleBlockEntity;
import ua.pp.shurgent.railward.client.renderer.ModernWhistleRenderer;

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

	public static void register() {
	}
}
