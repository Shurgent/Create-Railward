package ua.pp.shurgent.railward.loot;

import javax.annotation.Nonnull;

//? if >=1.19.2 {
/*import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraftforge.common.loot.IGlobalLootModifier;
*///?} else {
import java.util.List;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.loot.GlobalLootModifierSerializer;
//?}

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.LootModifier;
import ua.pp.shurgent.railward.AllItems;

public class EchoesDiscLootModifier extends LootModifier {
	//? if >=1.19.2
	/*public static final Codec<EchoesDiscLootModifier> CODEC = RecordCodecBuilder.create(instance ->
		codecStart(instance).apply(instance, EchoesDiscLootModifier::new));*/

	public EchoesDiscLootModifier(LootItemCondition[] conditions) {
		super(conditions);
	}

	@Nonnull
	@Override
	//? if >=1.19.2 {
	/*protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
	*///?} else
	protected List<ItemStack> doApply(List<ItemStack> generatedLoot, LootContext context) {
		generatedLoot.add(new ItemStack(AllItems.MUSIC_DISC_ECHOES.get()));
		return generatedLoot;
	}

	//? if >=1.19.2 {
	/*@Override
	public Codec<? extends IGlobalLootModifier> codec() {
		return CODEC;
	}
	*///?} else {
	public static class Serializer extends GlobalLootModifierSerializer<EchoesDiscLootModifier> {

		@Override
		public EchoesDiscLootModifier read(ResourceLocation location, JsonObject object,
				LootItemCondition[] conditions) {
			return new EchoesDiscLootModifier(conditions);
		}

		@Override
		public JsonObject write(EchoesDiscLootModifier instance) {
			return makeConditions(instance.conditions);
		}
	}
	//?}
}
