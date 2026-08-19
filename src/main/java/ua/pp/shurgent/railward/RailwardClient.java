package ua.pp.shurgent.railward;

import com.simibubi.create.CreateClient;

import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ClientRegistry;
import net.minecraftforge.client.gui.ForgeIngameGui;
import net.minecraftforge.client.gui.OverlayRegistry;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import ua.pp.shurgent.railward.client.RailwardContraptionGoggleOverlay;
import ua.pp.shurgent.railward.client.model.TrainWallPanelDualModel;
import ua.pp.shurgent.railward.client.ModernTrainControllerClient;
import ua.pp.shurgent.railward.client.ModernWhistleSecondaryHonkKeyHandler;
import ua.pp.shurgent.railward.client.renderer.LocomotiveBufferLightsPlacementPreview;
import ua.pp.shurgent.railward.client.renderer.DualPanelPlacementPreview;
import ua.pp.shurgent.railward.client.renderer.LocomotiveSpotlightForwardRenderer;
import ua.pp.shurgent.railward.client.renderer.RailingInteractionHighlight;
import ua.pp.shurgent.railward.client.renderer.SurfaceMountedPlacementPreview;
import ua.pp.shurgent.railward.client.renderer.WagonFrameInteractionHighlight;

@Mod.EventBusSubscriber(Dist.CLIENT)
public class RailwardClient {

	public static void onCtorClient(IEventBus modEventBus, IEventBus forgeEventBus) {
		
		AllPartialModels.init();
		
		modEventBus.addListener(RailwardClient::clientInit);
		forgeEventBus.addListener(LocomotiveBufferLightsPlacementPreview::render);
		forgeEventBus.addListener(DualPanelPlacementPreview::render);
		forgeEventBus.addListener(SurfaceMountedPlacementPreview::render);
		forgeEventBus.addListener(LocomotiveSpotlightForwardRenderer::prepare);
		forgeEventBus.addListener(LocomotiveSpotlightForwardRenderer::onClientLogout);
		forgeEventBus.addListener(RailingInteractionHighlight::render);
		forgeEventBus.addListener(WagonFrameInteractionHighlight::render);
		forgeEventBus.addListener(ModernWhistleSecondaryHonkKeyHandler::onClientTick);
		forgeEventBus.addListener(ModernTrainControllerClient::onClientTick);
		
	}

	public static void clientInit(final FMLClientSetupEvent event) {
		for (var panel : AllBlocks.trainWallPanelBlocks())
			CreateClient.MODEL_SWAPPER.getCustomBlockModels()
					.register(panel.get().getRegistryName(), TrainWallPanelDualModel::new);
		event.enqueueWork(() -> {
			for (var panel : AllBlocks.trainWallPanelBlocks())
				ItemBlockRenderTypes.setRenderLayer(panel.get(),
						type -> type == RenderType.solid() || type == RenderType.cutout());
		});
		ClientRegistry.registerKeyBinding(ModernWhistleSecondaryHonkKeyHandler.SECONDARY_HONK);
		ClientRegistry.registerKeyBinding(ModernTrainControllerClient.REVERSER);
		OverlayRegistry.registerOverlayAbove(ForgeIngameGui.HOTBAR_ELEMENT, "Railward Contraption Goggles",
			RailwardContraptionGoggleOverlay.OVERLAY);
	}

}
