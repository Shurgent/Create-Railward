package ua.pp.shurgent.railward.compat;

import net.minecraft.world.item.Item;

public final class RailwardItemProperties {

	private RailwardItemProperties() {
	}

	public static Item.Properties withoutTab(Item.Properties properties) {
		return properties;
	}
}
