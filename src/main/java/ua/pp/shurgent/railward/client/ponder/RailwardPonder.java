package ua.pp.shurgent.railward.client.ponder;

import ua.pp.shurgent.railward.AllBlocks;
import ua.pp.shurgent.railward.Railward;

//? if create6 {
/*import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.foundation.PonderIndex;
import net.minecraft.resources.ResourceLocation;
*///?} else {
import com.simibubi.create.foundation.ponder.PonderRegistrationHelper;
//?}

/** Registered only from the client entry point. */
public final class RailwardPonder {

	private RailwardPonder() {
	}

	public static void register() {
		//? if create6 {
		/*PonderIndex.addPlugin(new Plugin());
		*///?} else {
		var helper = new PonderRegistrationHelper(Railward.MOD_ID);
		helper.forComponents(AllBlocks.MODERN_SIGNAL, AllBlocks.MODERN_SIGNAL_GROUND)
			.addStoryBoard("signals", SignalScenes::placement);
		helper.forComponents(AllBlocks.MODERN_SIGNAL).addStoryBoard("signals", SignalScenes::mainSignal);
		helper.forComponents(AllBlocks.MODERN_SIGNAL_GROUND).addStoryBoard("signals", SignalScenes::groundSignal);
		helper.forComponents(AllBlocks.CISTERN_HATCH, AllBlocks.FLUID_TERMINAL)
			.addStoryBoard("cistern_transfer", FluidTransferScenes::hatch)
			.addStoryBoard("cistern_transfer", FluidTransferScenes::terminal);
		helper.forComponents(AllBlocks.BULK_CARGO_CONTAINER, AllBlocks.BULK_CARGO_CONTAINER_BLUE,
			AllBlocks.BULK_CARGO_CONTAINER_LIGHT_BLUE)
			.addStoryBoard("bulk_storage", BulkCargoScenes::storage)
			.addStoryBoard("bulk_transport", BulkCargoScenes::transport);
		helper.forComponents(AllBlocks.UNLOADING_HATCH, AllBlocks.UNLOADING_BUNKER)
			.addStoryBoard("bulk_transport", BulkCargoScenes::transport);
		//?}
	}

	//? if create6 {
	/*private static final class Plugin implements PonderPlugin {
		@Override
		public String getModId() {
			return Railward.MOD_ID;
		}

		@Override
		public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
			helper.forComponents(AllBlocks.MODERN_SIGNAL.getId(), AllBlocks.MODERN_SIGNAL_GROUND.getId())
				.addStoryBoard("signals", SignalScenes::placement);
			helper.forComponents(AllBlocks.MODERN_SIGNAL.getId()).addStoryBoard("signals", SignalScenes::mainSignal);
			helper.forComponents(AllBlocks.MODERN_SIGNAL_GROUND.getId()).addStoryBoard("signals", SignalScenes::groundSignal);
			helper.forComponents(AllBlocks.CISTERN_HATCH.getId(), AllBlocks.FLUID_TERMINAL.getId())
				.addStoryBoard("cistern_transfer", FluidTransferScenes::hatch)
				.addStoryBoard("cistern_transfer", FluidTransferScenes::terminal);
			helper.forComponents(AllBlocks.BULK_CARGO_CONTAINER.getId(), AllBlocks.BULK_CARGO_CONTAINER_BLUE.getId(),
				AllBlocks.BULK_CARGO_CONTAINER_LIGHT_BLUE.getId())
				.addStoryBoard("bulk_storage", BulkCargoScenes::storage)
				.addStoryBoard("bulk_transport", BulkCargoScenes::transport);
			helper.forComponents(AllBlocks.UNLOADING_HATCH.getId(), AllBlocks.UNLOADING_BUNKER.getId())
				.addStoryBoard("bulk_transport", BulkCargoScenes::transport);
		}
	}
	*///?}
}
