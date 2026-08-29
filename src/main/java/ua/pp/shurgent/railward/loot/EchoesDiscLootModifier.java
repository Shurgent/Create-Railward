package ua.pp.shurgent.railward.loot;

import java.util.List;

import javax.annotation.Nonnull;

import com.google.gson.JsonObject;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.GlobalLootModifierSerializer;
import net.minecraftforge.common.loot.LootModifier;
import ua.pp.shurgent.railward.AllItems;

public class EchoesDiscLootModifier extends LootModifier {

	public EchoesDiscLootModifier(LootItemCondition[] conditions) {
		super(conditions);
	}

	@Nonnull
	@Override
	protected List<ItemStack> doApply(List<ItemStack> generatedLoot, LootContext context) {
		generatedLoot.add(new ItemStack(AllItems.MUSIC_DISC_ECHOES.get()));
		return generatedLoot;
	}

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
}
