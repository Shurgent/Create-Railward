package ua.pp.shurgent.railward.mixin.create;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import com.simibubi.create.content.contraptions.MountedStorage;

import net.minecraftforge.items.ItemStackHandler;

@Mixin(value = MountedStorage.class, remap = false)
public interface MountedStorageAccessor {

	@Accessor("handler")
	ItemStackHandler railward$getHandler();

	@Accessor("handler")
	void railward$setHandler(ItemStackHandler handler);

}
