package ua.pp.shurgent.railward.mixin.create;

import java.util.Map;
import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

//? if create6 {
/*import com.simibubi.create.api.contraption.storage.SyncedMountedStorage;
import com.simibubi.create.api.contraption.storage.fluid.MountedFluidStorage;
import com.simibubi.create.api.contraption.storage.item.MountedItemStorage;
*///?} else {
import com.simibubi.create.content.contraptions.MountedFluidStorage;
import com.simibubi.create.content.contraptions.MountedStorage;
//?}
import com.simibubi.create.content.contraptions.MountedStorageManager;

import net.minecraft.core.BlockPos;
import net.minecraftforge.items.IItemHandlerModifiable;

@Mixin(value = MountedStorageManager.class, remap = false)
public interface MountedStorageManagerAccessor {

	//? if create6 {
	/*@Invoker("reset")
	void railward$reset();

	@Accessor("itemsBuilder")
	Map<BlockPos, MountedItemStorage> railward$getItemsBuilder();

	@Accessor("fluidsBuilder")
	Map<BlockPos, MountedFluidStorage> railward$getFluidsBuilder();

	@Accessor("syncedItemsBuilder")
	Map<BlockPos, SyncedMountedStorage> railward$getSyncedItemsBuilder();

	@Accessor("syncedFluidsBuilder")
	Map<BlockPos, SyncedMountedStorage> railward$getSyncedFluidsBuilder();

	@Accessor("externalHandlers")
	List<IItemHandlerModifiable> railward$getExternalHandlers();
	*///?} else {
	@Accessor("storage")
	Map<BlockPos, MountedStorage> railward$getStorage();

	@Accessor("fluidStorage")
	Map<BlockPos, MountedFluidStorage> railward$getFluidStorage();
	//?}

}
