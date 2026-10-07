package ua.pp.shurgent.railward.compat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//? if create6 {
/*import com.simibubi.create.AllMountedStorageTypes;
import com.simibubi.create.api.contraption.storage.SyncedMountedStorage;
import com.simibubi.create.api.contraption.storage.fluid.MountedFluidStorage;
import com.simibubi.create.api.contraption.storage.fluid.MountedFluidStorageType;
import com.simibubi.create.api.contraption.storage.item.MountedItemStorage;
import com.simibubi.create.api.contraption.storage.item.MountedItemStorageType;
import com.simibubi.create.api.contraption.storage.item.simple.SimpleMountedStorage;
*///?} else {
import com.simibubi.create.content.contraptions.MountedFluidStorage;
import com.simibubi.create.content.contraptions.MountedStorage;
//?}
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.MountedStorageManager;
import com.tterrag.registrate.builders.BlockBuilder;
import com.tterrag.registrate.util.nullness.NonNullUnaryOperator;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;
import ua.pp.shurgent.railward.mixin.create.MountedStorageAccessor;
import ua.pp.shurgent.railward.mixin.create.MountedStorageManagerAccessor;

/** Stable access to mounted storage across Create's two storage architectures. */
public final class RailwardMountedStorage {

	private RailwardMountedStorage() {
	}

	public static IItemHandlerModifiable items(Contraption contraption) {
		//? if create6
		/*return contraption.getStorage().getAllItems();
		*///? if !create6
		return contraption.getSharedInventory();
	}

	public static IFluidHandler fluids(Contraption contraption) {
		//? if create6
		/*return contraption.getStorage().getFluids();
		*///? if !create6
		return contraption.getSharedFluidTanks();
	}

	public static IItemHandler items(MountedStorageManager manager) {
		//? if create6
		/*return manager.getAllItems();
		*///? if !create6
		return manager.getItems();
	}

	public static IFluidHandler fluids(MountedStorageManager manager) {
		return manager.getFluids();
	}

	public static <B extends Block, P> NonNullUnaryOperator<BlockBuilder<B, P>> simpleItemStorage() {
		//? if create6
		/*return MountedItemStorageType.mountedItemStorage(AllMountedStorageTypes.SIMPLE);
		*///? if !create6
		return builder -> builder;
	}

	public static <B extends Block, P> NonNullUnaryOperator<BlockBuilder<B, P>> fluidTankStorage() {
		//? if create6
		/*return MountedFluidStorageType.mountedFluidStorage(AllMountedStorageTypes.FLUID_TANK);
		*///? if !create6
		return builder -> builder;
	}

	public static Map<BlockPos, ItemStorage> itemStorages(MountedStorageManager manager) {
		Map<BlockPos, ItemStorage> result = new HashMap<>();
		if (manager == null)
			return result;
		//? if create6 {
		/*MountedStorageManagerAccessor accessor = (MountedStorageManagerAccessor) manager;
		Map<BlockPos, MountedItemStorage> builder = accessor.railward$getItemsBuilder();
		Map<BlockPos, MountedItemStorage> source = builder == null ? manager.getAllItemStorages() : builder;
		source.forEach((pos, storage) -> result.put(pos, new ItemStorage(storage)));
		*///?} else {
		((MountedStorageManagerAccessor) manager).railward$getStorage().forEach((pos, storage) -> result.put(pos, new ItemStorage(storage)));
		//?}
		return result;
	}

	public static Map<BlockPos, FluidStorage> fluidStorages(MountedStorageManager manager) {
		Map<BlockPos, FluidStorage> result = new HashMap<>();
		if (manager == null)
			return result;
		//? if create6 {
		/*MountedStorageManagerAccessor accessor = (MountedStorageManagerAccessor) manager;
		Map<BlockPos, MountedFluidStorage> builder = accessor.railward$getFluidsBuilder();
		Map<BlockPos, MountedFluidStorage> source = builder == null ? manager.getFluids().storages : builder;
		source.forEach((pos, storage) -> result.put(pos, new FluidStorage(storage)));
		*///?} else {
		((MountedStorageManagerAccessor) manager).railward$getFluidStorage().forEach((pos, storage) -> result.put(pos, new FluidStorage(storage)));
		//?}
		return result;
	}

	public static void commitItems(MountedStorageManager manager, Map<BlockPos, ItemStorage> storages) {
		commit(manager, storages, null);
	}

	public static void commitFluids(MountedStorageManager manager, Map<BlockPos, FluidStorage> storages) {
		commit(manager, null, storages);
	}

	public static boolean updateFluid(MountedStorageManager manager, BlockPos pos, FluidStack fluid) {
		if (manager == null)
			return false;
		//? if create6 {
		/*FluidStorage storage = fluidStorages(manager).get(pos);
		if (storage == null)
			return false;
		IFluidHandler handler = storage.handler();
		for (int tank = 0; tank < handler.getTanks(); tank++) {
			FluidStack current = handler.getFluidInTank(tank);
			if (!current.isEmpty())
				handler.drain(current.copy(), FluidAction.EXECUTE);
		}
		if (!fluid.isEmpty())
			handler.fill(fluid.copy(), FluidAction.EXECUTE);
		return true;
		*///?} else {
		manager.updateContainedFluid(pos, fluid.copy());
		return fluidStorages(manager).containsKey(pos);
		//?}
	}

	public static void commit(MountedStorageManager manager, Map<BlockPos, ItemStorage> itemStorages,
		Map<BlockPos, FluidStorage> fluidStorages) {
		if (manager == null)
			return;

		//? if create6 {
		/*MountedStorageManagerAccessor accessor = (MountedStorageManagerAccessor) manager;
		Map<BlockPos, MountedItemStorage> items = new HashMap<>();
		if (itemStorages == null) {
			Map<BlockPos, MountedItemStorage> builder = accessor.railward$getItemsBuilder();
			items.putAll(builder == null ? manager.getAllItemStorages() : builder);
		}
		else
			itemStorages.forEach((pos, storage) -> items.put(pos, storage.delegate));

		Map<BlockPos, MountedFluidStorage> fluids = new HashMap<>();
		if (fluidStorages == null) {
			Map<BlockPos, MountedFluidStorage> builder = accessor.railward$getFluidsBuilder();
			fluids.putAll(builder == null ? manager.getFluids().storages : builder);
		}
		else
			fluidStorages.forEach((pos, storage) -> fluids.put(pos, storage.delegate));

		List<IItemHandlerModifiable> externalHandlers = new ArrayList<>(accessor.railward$getExternalHandlers());
		accessor.railward$reset();
		accessor.railward$getItemsBuilder().putAll(items);
		accessor.railward$getFluidsBuilder().putAll(fluids);
		items.forEach((pos, storage) -> {
			if (storage instanceof SyncedMountedStorage synced)
				accessor.railward$getSyncedItemsBuilder().put(pos, synced);
		});
		fluids.forEach((pos, storage) -> {
			if (storage instanceof SyncedMountedStorage synced)
				accessor.railward$getSyncedFluidsBuilder().put(pos, synced);
		});
		manager.initialize();
		externalHandlers.forEach(manager::attachExternal);
		*///?} else {
		MountedStorageManagerAccessor accessor = (MountedStorageManagerAccessor) manager;
		if (itemStorages != null) {
			Map<BlockPos, MountedStorage> target = accessor.railward$getStorage();
			target.clear();
			itemStorages.forEach((pos, storage) -> target.put(pos, storage.delegate));
		}
		if (fluidStorages != null) {
			Map<BlockPos, MountedFluidStorage> target = accessor.railward$getFluidStorage();
			target.clear();
			fluidStorages.forEach((pos, storage) -> target.put(pos, storage.delegate));
		}
		manager.createHandlers();
		//?}
	}

	public static final class ItemStorage {
		//? if create6
		/*private final MountedItemStorage delegate;
		*///? if !create6
		private final MountedStorage delegate;

		//? if create6
		/*private ItemStorage(MountedItemStorage delegate) {
		*///? if !create6
		private ItemStorage(MountedStorage delegate) {
			this.delegate = delegate;
		}

		public IItemHandlerModifiable handler() {
			//? if create6
			/*return delegate;
			*///? if !create6
			return delegate.getItemHandler();
		}

		public ItemStorage withHandler(ItemStackHandler handler) {
			//? if create6
			/*return new ItemStorage(new SimpleMountedStorage(handler));
			*///? if !create6
			((MountedStorageAccessor) delegate).railward$setHandler(handler);
			//? if !create6
			return this;
		}
	}

	public static final class FluidStorage {
		private final MountedFluidStorage delegate;

		private FluidStorage(MountedFluidStorage delegate) {
			this.delegate = delegate;
		}

		public IFluidHandler handler() {
			//? if create6
			/*return delegate;
			*///? if !create6
			return delegate.getFluidHandler();
		}

		public void rebind(BlockEntity blockEntity) {
			//? if !create6
			delegate.assignBlockEntity(blockEntity);
		}
	}

	public static IFluidHandler fluidHandler(MountedFluidStorage storage) {
		//? if create6
		/*return storage;
		*///? if !create6
		return storage.getFluidHandler();
	}
}
