package ua.pp.shurgent.railward;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.simibubi.create.foundation.data.CreateRegistrate;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.registries.ForgeRegistries;
import ua.pp.shurgent.railward.block.surface.SurfaceMountedBlock;
import ua.pp.shurgent.railward.particle.RailwardParticleTypes;
import ua.pp.shurgent.railward.schedule.RailwardScheduleInstructions;

// The value here should match an entry in the META-INF/mods.toml file
@Mod("railward")
public class Railward {
	public static final String MOD_ID = "railward";
	public static final CreateRegistrate REGISTRATE = CreateRegistrate.create(MOD_ID);

	//? if <1.20.1 {
	public static final CreativeModeTab RAILWARD_TAB = new CreativeModeTab("railward") {
		@Override
		public ItemStack makeIcon() {
			return new ItemStack(AllBlocks.MODERN_SIGNAL_GROUND.get());
		}
	};
	//?}

	//? if >=1.20.1
	/*static {
		REGISTRATE.defaultCreativeTab("railward",
			builder -> builder.icon(() -> new ItemStack(AllBlocks.MODERN_SIGNAL_GROUND.get())))
			.register();
	}*/

	// Directly reference a slf4j logger
	private static final Logger LOGGER = LogUtils.getLogger();

	public Railward() {
		
		IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
		IEventBus forgeEventBus = MinecraftForge.EVENT_BUS;
		
		ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, RailwardConfig.COMMON_SPEC);
		ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, RailwardConfig.CLIENT_SPEC);
		MinecraftForge.EVENT_BUS.register(this);
		REGISTRATE.registerEventListeners(modEventBus);
		RailwardSoundEvents.register(modEventBus);
		RailwardLootModifiers.register(modEventBus);
		RailwardParticleTypes.register(modEventBus);
		RailwardPackets.registerPackets();
		RailwardScheduleInstructions.register();
		
		AllEdgePointTypes.register();
		AllItems.register();
		AllBlocks.register();
		RailwardBogeyStyles.register();
		AllBlockEntities.register();
		
		// Register the setup method for modloading
		modEventBus.addListener(this::setup);
		
		DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> RailwardClient.onCtorClient(modEventBus, forgeEventBus));
	}

	private void setup(final FMLCommonSetupEvent event) {
		event.enqueueWork((Runnable) SurfaceMountedBlock::registerMovementChecks);
		// some preinit code
		LOGGER.info("HELLO FROM PREINIT");
		LOGGER.info("DIRT BLOCK >> {}", ForgeRegistries.BLOCKS.getKey(Blocks.DIRT));
	}

	// You can use SubscribeEvent and let the Event Bus discover methods to call
	@SubscribeEvent
	public void onServerStarting(ServerStartingEvent event) {
		// Do something when the server starts
		LOGGER.info("HELLO from server starting");
	}

	@SuppressWarnings("removal")
	public static ResourceLocation asResource(String resourcePath) {
		return new ResourceLocation(MOD_ID, resourcePath);
	}
}
