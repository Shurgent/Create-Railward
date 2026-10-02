package ua.pp.shurgent.railward;

import static ua.pp.shurgent.railward.Railward.REGISTRATE;

import com.tterrag.registrate.util.entry.ItemEntry;

import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.RecordItem;

public class AllItems {

	public static final ItemEntry<RecordItem> MUSIC_DISC_ECHOES = REGISTRATE
		.item("music_disc_echoes",
			// 183.072 seconds, rounded up to whole game ticks for the 1.19 API.
			//? if >=1.19.2
			/*properties -> new RecordItem(15, RailwardSoundEvents.MUSIC_DISC_ECHOES, properties, 3662))*/
			//? if <1.19.2
			properties -> new RecordItem(15, RailwardSoundEvents.MUSIC_DISC_ECHOES, properties))
		.properties(properties -> properties
			.stacksTo(1)
			//? if <1.20.1
			.tab(Railward.RAILWARD_TAB)
			.rarity(Rarity.RARE))
		.register();

	private AllItems() {
	}

	public static void register() {
	}
}
