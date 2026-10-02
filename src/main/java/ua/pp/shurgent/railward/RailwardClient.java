package ua.pp.shurgent.railward;

import com.simibubi.create.CreateClient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraftforge.api.distmarker.Dist;
//? if >=1.19.2 {
/*import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
*///?} else {
import net.minecraftforge.client.ClientRegistry;
import net.minecraftforge.client.event.ParticleFactoryRegisterEvent;
import net.minecraftforge.client.gui.ForgeIngameGui;
import net.minecraftforge.client.gui.OverlayRegistry;
//?}
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.registries.ForgeRegistries;
import ua.pp.shurgent.railward.client.RailwardContraptionGoggleOverlay;
import ua.pp.shurgent.railward.client.model.TrainWallPanelDualModel;
import ua.pp.shurgent.railward.client.particle.DieselExhaustParticle;
import ua.pp.shurgent.railward.client.particle.DieselExhaustParticleRenderer;
import ua.pp.shurgent.railward.client.ModernTrainControllerClient;
import ua.pp.shurgent.railward.client.ModernWhistleSecondaryHonkKeyHandler;
import ua.pp.shurgent.railward.client.renderer.LocomotiveBufferLightsPlacementPreview;
import ua.pp.shurgent.railward.client.renderer.DualPanelPlacementPreview;
import ua.pp.shurgent.railward.client.renderer.LocomotiveSpotlightForwardRenderer;
import ua.pp.shurgent.railward.client.renderer.RailingInteractionHighlight;
import ua.pp.shurgent.railward.client.renderer.SurfaceMountedPlacementPreview;
import ua.pp.shurgent.railward.client.renderer.TrainRoofPartitionHighlight;
import ua.pp.shurgent.railward.client.renderer.WagonFrameInteractionHighlight;
import ua.pp.shurgent.railward.particle.RailwardParticleTypes;

@Mod.EventBusSubscriber(Dist.CLIENT)
public class RailwardClient {

	public static void onCtorClient(IEventBus modEventBus, IEventBus forgeEventBus) {
		
		AllPartialModels.init();
		
		modEventBus.addListener(RailwardClient::clientInit);
		modEventBus.addListener(RailwardClient::registerParticleFactories);
		//? if >=1.19.2 {
		/*modEventBus.addListener(RailwardClient::registerKeyMappings);
		modEventBus.addListener(RailwardClient::registerGuiOverlays);
		*///?}
		forgeEventBus.addListener(LocomotiveBufferLightsPlacementPreview::render);
		forgeEventBus.addListener(DualPanelPlacementPreview::render);
		forgeEventBus.addListener(SurfaceMountedPlacementPreview::render);
		forgeEventBus.addListener(LocomotiveSpotlightForwardRenderer::prepare);
		forgeEventBus.addListener(LocomotiveSpotlightForwardRenderer::onClientLogout);
		forgeEventBus.addListener(EventPriority.LOWEST, DieselExhaustParticleRenderer::render);
		forgeEventBus.addListener(DieselExhaustParticleRenderer::onClientLogout);
		forgeEventBus.addListener(RailingInteractionHighlight::render);
		forgeEventBus.addListener(TrainRoofPartitionHighlight::render);
		forgeEventBus.addListener(WagonFrameInteractionHighlight::render);
		forgeEventBus.addListener(ModernWhistleSecondaryHonkKeyHandler::onClientTick);
		forgeEventBus.addListener(ModernTrainControllerClient::onClientTick);
		
	}

	//? if >=1.19.2 {
	/*public static void registerParticleFactories(RegisterParticleProvidersEvent event) {
		event.register(RailwardParticleTypes.DIESEL_EXHAUST.get(), DieselExhaustParticle.Factory::new);
	}

	public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
		event.register(ModernWhistleSecondaryHonkKeyHandler.SECONDARY_HONK);
		event.register(ModernTrainControllerClient.REVERSER);
	}

	public static void registerGuiOverlays(RegisterGuiOverlaysEvent event) {
		event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), "railward_contraption_goggles",
			RailwardContraptionGoggleOverlay.OVERLAY);
	}
	*///?} else {
	public static void registerParticleFactories(ParticleFactoryRegisterEvent event) {
		Minecraft.getInstance().particleEngine.register(RailwardParticleTypes.DIESEL_EXHAUST.get(),
			DieselExhaustParticle.Factory::new);
	}
	//?}

	public static void clientInit(final FMLClientSetupEvent event) {
		for (var panel : AllBlocks.trainWallPanelBlocks())
			CreateClient.MODEL_SWAPPER.getCustomBlockModels()
					.register(ForgeRegistries.BLOCKS.getKey(panel.get()), TrainWallPanelDualModel::new);
		event.enqueueWork(() -> {
			for (var panel : AllBlocks.trainWallPanelBlocks())
				ItemBlockRenderTypes.setRenderLayer(panel.get(),
						type -> type == RenderType.solid() || type == RenderType.cutout());
		});
		//? if <1.19.2 {
		ClientRegistry.registerKeyBinding(ModernWhistleSecondaryHonkKeyHandler.SECONDARY_HONK);
		ClientRegistry.registerKeyBinding(ModernTrainControllerClient.REVERSER);
		OverlayRegistry.registerOverlayAbove(ForgeIngameGui.HOTBAR_ELEMENT, "Railward Contraption Goggles",
			RailwardContraptionGoggleOverlay.OVERLAY);
		//?}
	}

}
