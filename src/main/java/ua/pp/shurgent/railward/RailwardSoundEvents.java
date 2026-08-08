package ua.pp.shurgent.railward;

import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class RailwardSoundEvents {

	private static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS,
		Railward.MOD_ID);

	public static final RegistryObject<SoundEvent> MODERN_WHISTLE_HORN_1_START = register(
		"modern_whistle_horn_1_start");
	public static final RegistryObject<SoundEvent> MODERN_WHISTLE_HORN_1_LOOP = register(
		"modern_whistle_horn_1_loop");
	public static final RegistryObject<SoundEvent> MODERN_WHISTLE_HORN_1_END = register(
		"modern_whistle_horn_1_end");

	public static final RegistryObject<SoundEvent> MODERN_WHISTLE_HORN_2_START = register(
		"modern_whistle_horn_2_start");
	public static final RegistryObject<SoundEvent> MODERN_WHISTLE_HORN_2_LOOP = register(
		"modern_whistle_horn_2_loop");
	public static final RegistryObject<SoundEvent> MODERN_WHISTLE_HORN_2_END = register(
		"modern_whistle_horn_2_end");

	public static final RegistryObject<SoundEvent> MODERN_WHISTLE_WHISTLE_START = register(
		"modern_whistle_whistle_start");
	public static final RegistryObject<SoundEvent> MODERN_WHISTLE_WHISTLE_LOOP = register(
		"modern_whistle_whistle_loop");
	public static final RegistryObject<SoundEvent> MODERN_WHISTLE_WHISTLE_END = register(
		"modern_whistle_whistle_end");

	public static final RegistryObject<SoundEvent> TRAIN_COUPLER_MANUAL = register("train_coupler_manual");
	public static final RegistryObject<SoundEvent> TRAIN_COUPLER_AUTO = register("train_coupler_auto");
	public static final RegistryObject<SoundEvent> UNLOADING_BULK_CARGO = register("unloading_bulk_cargo");
	public static final RegistryObject<SoundEvent> CAB_SIGNAL_RED = register("cab_signal_red");

	private static RegistryObject<SoundEvent> register(String name) {
		return SOUND_EVENTS.register(name, () -> new SoundEvent(Railward.asResource(name)));
	}

	public static void register(IEventBus modEventBus) {
		SOUND_EVENTS.register(modEventBus);
	}

}
