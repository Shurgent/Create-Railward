package ua.pp.shurgent.railward.compat;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.block.state.BlockState;
//? if >=1.19.2 {
/*import net.minecraft.network.chat.Component;
*///?} else {
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
//?}

public final class RailwardComponents {

	private RailwardComponents() {
	}

	public static MutableComponent literal(String text) {
		//? if >=1.19.2 {
		/*return Component.literal(text);
		*///?} else
		return new TextComponent(text);
	}

	public static MutableComponent translatable(String key, Object... arguments) {
		//? if >=1.19.2 {
		/*return Component.translatable(key, arguments);
		*///?} else
		return new TranslatableComponent(key, arguments);
	}

	public static BlockState readBlockState(CompoundTag tag) {
		//? if >=1.20.1
		/*return NbtUtils.readBlockState(net.minecraft.core.registries.BuiltInRegistries.BLOCK.asLookup(), tag);
		*///? if <1.20.1
		return NbtUtils.readBlockState(tag);
	}
}
