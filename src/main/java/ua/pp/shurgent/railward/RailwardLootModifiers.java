package ua.pp.shurgent.railward;

import net.minecraftforge.common.loot.GlobalLootModifierSerializer;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import ua.pp.shurgent.railward.loot.EchoesDiscLootModifier;

public class RailwardLootModifiers {

	private static final DeferredRegister<GlobalLootModifierSerializer<?>> LOOT_MODIFIER_SERIALIZERS =
		DeferredRegister.create(ForgeRegistries.Keys.LOOT_MODIFIER_SERIALIZERS, Railward.MOD_ID);

	public static final RegistryObject<GlobalLootModifierSerializer<EchoesDiscLootModifier>> ECHOES_DISC =
		LOOT_MODIFIER_SERIALIZERS.register("echoes_disc", EchoesDiscLootModifier.Serializer::new);

	private RailwardLootModifiers() {
	}

	public static void register(IEventBus modEventBus) {
		LOOT_MODIFIER_SERIALIZERS.register(modEventBus);
	}
}
