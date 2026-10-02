package ua.pp.shurgent.railward;

//? if >=1.19.2 {
/*import com.mojang.serialization.Codec;
import net.minecraftforge.common.loot.IGlobalLootModifier;
*///?} else
import net.minecraftforge.common.loot.GlobalLootModifierSerializer;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import ua.pp.shurgent.railward.loot.EchoesDiscLootModifier;

public class RailwardLootModifiers {

	//? if >=1.19.2 {
	/*private static final DeferredRegister<Codec<? extends IGlobalLootModifier>> LOOT_MODIFIER_SERIALIZERS =
	*///?} else
	private static final DeferredRegister<GlobalLootModifierSerializer<?>> LOOT_MODIFIER_SERIALIZERS =
		//? if >=1.19.2
		/*DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, Railward.MOD_ID);*/
		//? if <1.19.2
		DeferredRegister.create(ForgeRegistries.Keys.LOOT_MODIFIER_SERIALIZERS, Railward.MOD_ID);

	//? if >=1.19.2 {
	/*public static final RegistryObject<Codec<EchoesDiscLootModifier>> ECHOES_DISC =
		LOOT_MODIFIER_SERIALIZERS.register("echoes_disc", () -> EchoesDiscLootModifier.CODEC);
	*///?} else {
	public static final RegistryObject<GlobalLootModifierSerializer<EchoesDiscLootModifier>> ECHOES_DISC =
		LOOT_MODIFIER_SERIALIZERS.register("echoes_disc", EchoesDiscLootModifier.Serializer::new);
	//?}

	private RailwardLootModifiers() {
	}

	public static void register(IEventBus modEventBus) {
		LOOT_MODIFIER_SERIALIZERS.register(modEventBus);
	}
}
