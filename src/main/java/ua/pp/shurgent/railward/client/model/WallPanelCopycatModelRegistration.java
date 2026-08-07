package ua.pp.shurgent.railward.client.model;

import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.util.nullness.NonNullConsumer;

import net.minecraft.world.level.block.Block;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;

/**
 * Keeps all BakedModel references out of common block registration bytecode.
 */
public final class WallPanelCopycatModelRegistration {

	private WallPanelCopycatModelRegistration() {
	}

	public static <T extends Block> NonNullConsumer<? super T> verticalSlice() {
		return create(ModelShape.VERTICAL_SLICE);
	}

	public static <T extends Block> NonNullConsumer<? super T> horizontalSlice() {
		return create(ModelShape.HORIZONTAL_SLICE);
	}

	public static <T extends Block> NonNullConsumer<? super T> corner() {
		return create(ModelShape.CORNER);
	}

	public static <T extends Block> NonNullConsumer<? super T> halfPanel() {
		return create(ModelShape.HALF_PANEL);
	}

	public static <T extends Block> NonNullConsumer<? super T> slab() {
		return DistExecutor.<NonNullConsumer<? super T>>unsafeRunForDist(
				() -> () -> Client.<T>createSlab(),
				() -> () -> block -> {
				});
	}

	private static <T extends Block> NonNullConsumer<? super T> create(ModelShape shape) {
		return DistExecutor.<NonNullConsumer<? super T>>unsafeRunForDist(
				() -> () -> Client.<T>create(shape),
				() -> () -> block -> {
				});
	}

	private enum ModelShape {
		VERTICAL_SLICE,
		HORIZONTAL_SLICE,
		CORNER,
		HALF_PANEL
	}

	@OnlyIn(Dist.CLIENT)
	private static final class Client {

		private static <T extends Block> NonNullConsumer<? super T> create(ModelShape shape) {
			WallPanelCopycatModel.Shape clientShape = switch (shape) {
				case VERTICAL_SLICE -> WallPanelCopycatModel.Shape.VERTICAL_SLICE;
				case HORIZONTAL_SLICE -> WallPanelCopycatModel.Shape.HORIZONTAL_SLICE;
				case CORNER -> WallPanelCopycatModel.Shape.CORNER;
				case HALF_PANEL -> WallPanelCopycatModel.Shape.HALF_PANEL;
			};
			return CreateRegistrate.blockModel(
					() -> model -> new WallPanelCopycatModel(model, clientShape));
		}

		private static <T extends Block> NonNullConsumer<? super T> createSlab() {
			return CreateRegistrate.blockModel(() -> CopycatSlabModel::new);
		}
	}
}
