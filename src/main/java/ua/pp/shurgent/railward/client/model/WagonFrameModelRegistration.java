package ua.pp.shurgent.railward.client.model;

import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.util.nullness.NonNullConsumer;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import ua.pp.shurgent.railward.block.train.parts.WagonFrameBlock;

public final class WagonFrameModelRegistration {

	private WagonFrameModelRegistration() {
	}

	public static NonNullConsumer<? super WagonFrameBlock> create() {
		return DistExecutor.<NonNullConsumer<? super WagonFrameBlock>>unsafeRunForDist(
			() -> Client::create, () -> () -> block -> {});
	}

	@OnlyIn(Dist.CLIENT)
	private static final class Client {

		private static NonNullConsumer<? super WagonFrameBlock> create() {
			return CreateRegistrate.blockModel(() -> WagonFrameModel::new);
		}
	}
}
