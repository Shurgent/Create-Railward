package ua.pp.shurgent.railward.compat;

//? if create6
/*import com.simibubi.create.foundation.utility.CreateLang;
*///? if !create6
import com.simibubi.create.foundation.utility.Lang;
import com.simibubi.create.foundation.utility.LangBuilder;

import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;

/** Create's namespaced language facade moved out of Catnip in Create 6. */
public final class RailwardLang {

	private RailwardLang() {
	}

	public static LangBuilder builder() {
		//? if create6
		/*return CreateLang.builder();
		*///? if !create6
		return Lang.builder();
	}

	public static LangBuilder builder(String namespace) {
		//? if create6
		/*return CreateLang.builder(namespace);
		*///? if !create6
		return Lang.builder(namespace);
	}

	public static LangBuilder itemName(ItemStack stack) {
		//? if create6
		/*return CreateLang.itemName(stack);
		*///? if !create6
		return Lang.itemName(stack);
	}

	public static LangBuilder number(double value) {
		//? if create6
		/*return CreateLang.number(value);
		*///? if !create6
		return Lang.number(value);
	}

	public static MutableComponent translateDirect(String key, Object... args) {
		//? if create6
		/*return CreateLang.translateDirect(key, args);
		*///? if !create6
		return Lang.translateDirect(key, args);
	}
}
