package ua.pp.shurgent.railward.client.model;

import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.util.nullness.NonNullConsumer;

import net.minecraft.world.level.block.Block;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;

/** Keeps client-only baked model references out of common registration code. */
public final class TrainRoofModelRegistration {

	private TrainRoofModelRegistration() {
	}

	public static <T extends Block> NonNullConsumer<? super T> create() {
		return DistExecutor.<NonNullConsumer<? super T>>unsafeRunForDist(
				() -> () -> Client.<T>create(),
				() -> () -> block -> {
				});
	}

	@OnlyIn(Dist.CLIENT)
	private static final class Client {

		private static <T extends Block> NonNullConsumer<? super T> create() {
			return CreateRegistrate.blockModel(() -> TrainRoofModel::new);
		}
	}
}
