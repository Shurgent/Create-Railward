package ua.pp.shurgent.railward.mixin.create;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

//? if create6 {
/*import com.simibubi.create.api.contraption.storage.item.MountedItemStorage;
*///?} else
import com.simibubi.create.content.contraptions.MountedStorage;

import net.minecraftforge.items.ItemStackHandler;

//? if create6
/*@Mixin(value = MountedItemStorage.class, remap = false)
*///? if !create6
@Mixin(value = MountedStorage.class, remap = false)
public interface MountedStorageAccessor {

	//? if !create6 {
	@Accessor("handler")
	ItemStackHandler railward$getHandler();

	@Accessor("handler")
	void railward$setHandler(ItemStackHandler handler);
	//?}

}
