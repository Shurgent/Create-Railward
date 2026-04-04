package ua.pp.shurgent.railward;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.simibubi.create.foundation.data.CreateRegistrate;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

// The value here should match an entry in the META-INF/mods.toml file
@Mod("railward")
public class Railward {
	public static final String MOD_ID = "railward";
	public static final CreateRegistrate REGISTRATE = CreateRegistrate.create(MOD_ID);

	public static final CreativeModeTab RAILWARD_TAB = new CreativeModeTab("railward") {
		@Override
		public ItemStack makeIcon() {
			return new ItemStack(AllBlocks.MODERN_SIGNAL_GROUND.get());
		}
	};

	// Directly reference a slf4j logger
	private static final Logger LOGGER = LogUtils.getLogger();

	public Railward() {
		
		IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
		IEventBus forgeEventBus = MinecraftForge.EVENT_BUS;
		
		MinecraftForge.EVENT_BUS.register(this);
		REGISTRATE.registerEventListeners(modEventBus);
		
		AllBlocks.register();
		AllBlockEntities.register();
		
		// Register the setup method for modloading
		modEventBus.addListener(this::setup);
		
		DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> RailwardClient.onCtorClient(modEventBus, forgeEventBus));
	}

	private void setup(final FMLCommonSetupEvent event) {
		// some preinit code
		LOGGER.info("HELLO FROM PREINIT");
		LOGGER.info("DIRT BLOCK >> {}", Blocks.DIRT.getRegistryName());
	}

	// You can use SubscribeEvent and let the Event Bus discover methods to call
	@SubscribeEvent
	public void onServerStarting(ServerStartingEvent event) {
		// Do something when the server starts
		LOGGER.info("HELLO from server starting");
	}

	// You can use EventBusSubscriber to automatically subscribe events on the
	// contained class (this is subscribing to the MOD
	// Event bus for receiving Registry Events)
	@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
	public static class RegistryEvents {
		@SubscribeEvent
		public static void onBlocksRegistry(final RegistryEvent.Register<Block> blockRegistryEvent) {
			// Register a new block here
			LOGGER.info("HELLO from Register Block");
		}
	}
	@SuppressWarnings("removal")
	public static ResourceLocation asResource(String resourcePath) {
		return new ResourceLocation(MOD_ID, resourcePath);
	}
}
