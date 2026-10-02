package ua.pp.shurgent.railward.compat;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

public final class RailwardEntity {

	private RailwardEntity() {
	}

	public static Level level(Entity value) {
		//? if >=1.20.1
		/*return value.level();
		*///? if <1.20.1
		return value.level;
	}
}
