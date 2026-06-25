package ua.pp.shurgent.railward.block.train.parts;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.MountedStorage;
import com.simibubi.create.content.contraptions.MountedStorageManager;
import com.simibubi.create.content.contraptions.StructureTransform;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import ua.pp.shurgent.railward.mixin.create.MountedStorageAccessor;
import ua.pp.shurgent.railward.mixin.create.MountedStorageManagerAccessor;

public final class BulkCargoContraptionDisassembly {

	private BulkCargoContraptionDisassembly() {
	}

	public static void prepare(Contraption contraption, StructureTransform transform) {
		MountedStorageManager manager = BulkCargoContraptionHelper.getStorageManager(contraption);
		if (manager == null)
			return;

		Map<BlockPos, MountedStorage> storage = ((MountedStorageManagerAccessor) manager).railward$getStorage();
		if (storage.isEmpty())
			return;

		Map<BlockPos, Set<BlockPos>> components = new HashMap<>();
		for (Map.Entry<BlockPos, StructureBlockInfo> entry : contraption.getBlocks()
			.entrySet()) {
			StructureBlockInfo info = entry.getValue();
			if (!(info.state.getBlock() instanceof BulkCargoContainerBlock))
				continue;

			BlockPos controllerPos = getControllerLocalPos(contraption, transform, entry.getKey(), info.nbt);
			components.computeIfAbsent(controllerPos, $ -> new HashSet<>())
				.add(entry.getKey());
		}

		boolean changed = false;
		for (Map.Entry<BlockPos, Set<BlockPos>> entry : components.entrySet()) {
			Bounds bounds = Bounds.of(entry.getValue(), transform, entry.getKey());
			if (bounds == null)
				continue;
			StorageNormalization normalized = normalizeStorage(storage, bounds.controllerLocalPos(), entry.getValue());
			changed |= normalized.changed();
			writeDisassemblyNbt(contraption, entry.getValue(), bounds, normalized.handler());
		}

		if (changed)
			manager.createHandlers();
	}

	private static BlockPos getControllerLocalPos(Contraption contraption, StructureTransform transform,
		BlockPos localPos, CompoundTag nbt) {
		if (nbt == null || !nbt.contains("Controller"))
			return localPos;

		BlockPos controllerPos = NbtUtils.readBlockPos(nbt.getCompound("Controller"));
		BlockPos localControllerPos = transform.unapply(controllerPos);
		if (isBulkCargoBlock(contraption, localControllerPos))
			return localControllerPos;
		if (isBulkCargoBlock(contraption, controllerPos))
			return controllerPos;
		return localPos;
	}

	private static boolean isBulkCargoBlock(Contraption contraption, BlockPos localPos) {
		StructureBlockInfo info = contraption.getBlocks()
			.get(localPos);
		return info != null && info.state.getBlock() instanceof BulkCargoContainerBlock;
	}

	private static StorageNormalization normalizeStorage(Map<BlockPos, MountedStorage> storage, BlockPos controllerPos,
		Set<BlockPos> component) {
		int expectedSlots = component.size() * BulkCargoContainerBlockEntity.STACKS_PER_BLOCK;
		MountedStorage controllerStorage = storage.get(controllerPos);
		BlockPos controllerStoragePos = controllerPos;

		if (controllerStorage == null)
			for (BlockPos pos : component) {
				controllerStorage = storage.get(pos);
				if (controllerStorage != null) {
					controllerStoragePos = pos;
					break;
				}
			}
		if (controllerStorage == null)
			return new StorageNormalization(false, null);

		ItemStackHandler controllerHandler = ((MountedStorageAccessor) controllerStorage).railward$getHandler();
		boolean needsMerge = !controllerStoragePos.equals(controllerPos)
			|| controllerHandler == null
			|| controllerHandler.getSlots() != expectedSlots;
		for (BlockPos pos : component)
			if (!pos.equals(controllerStoragePos) && storage.containsKey(pos)) {
				needsMerge = true;
				break;
			}
		if (!needsMerge)
			return new StorageNormalization(false, controllerHandler);

		List<ItemStack> contents = new ArrayList<>();
		for (BlockPos pos : component) {
			MountedStorage mountedStorage = storage.remove(pos);
			if (mountedStorage == null)
				continue;
			ItemStackHandler handler = ((MountedStorageAccessor) mountedStorage).railward$getHandler();
			if (handler != null)
				contents.addAll(BulkCargoStorage.extractAll(handler));
		}

		BulkCargoStorage.CargoItemHandler merged = new BulkCargoStorage.CargoItemHandler(expectedSlots);
		for (ItemStack stack : contents)
			BulkCargoStorage.insertExisting(merged, stack, false);
		((MountedStorageAccessor) controllerStorage).railward$setHandler(merged);
		storage.put(controllerPos, controllerStorage);
		return new StorageNormalization(true, merged);
	}

	private static void writeDisassemblyNbt(Contraption contraption, Set<BlockPos> component, Bounds bounds,
		ItemStackHandler handler) {
		for (BlockPos localPos : component) {
			StructureBlockInfo info = contraption.getBlocks()
				.get(localPos);
			if (info == null || !(info.state.getBlock() instanceof BulkCargoContainerBlock))
				continue;

			CompoundTag nbt = info.nbt == null ? new CompoundTag() : info.nbt.copy();
			nbt.put("Controller", NbtUtils.writeBlockPos(bounds.controllerGlobalPos()));
			nbt.putInt("SizeX", bounds.sizeX());
			nbt.putInt("SizeZ", bounds.sizeZ());
			nbt.putInt("Height", bounds.height());
			nbt.remove("LastKnownPos");
			if (localPos.equals(bounds.controllerLocalPos())) {
				if (handler != null)
					nbt.put("Inventory", snapshotInventory(handler));
			} else
				nbt.remove("Inventory");

			contraption.getBlocks()
				.put(localPos, new StructureBlockInfo(info.pos, info.state, nbt));
		}
	}

	private static CompoundTag snapshotInventory(IItemHandler inventory) {
		CompoundTag nbt = new CompoundTag();
		if (inventory == null)
			return nbt;

		ItemStackHandler snapshot = new ItemStackHandler(inventory.getSlots());
		for (int slot = 0; slot < inventory.getSlots(); slot++)
			snapshot.setStackInSlot(slot, inventory.getStackInSlot(slot)
				.copy());
		return snapshot.serializeNBT();
	}

	private record StorageNormalization(boolean changed, ItemStackHandler handler) {
	}

	private record Bounds(BlockPos controllerGlobalPos, BlockPos controllerLocalPos, int sizeX, int sizeZ, int height) {
		private static Bounds of(Set<BlockPos> component, StructureTransform transform, BlockPos fallbackControllerLocalPos) {
			if (component.isEmpty())
				return null;

			int minX = Integer.MAX_VALUE;
			int minY = Integer.MAX_VALUE;
			int minZ = Integer.MAX_VALUE;
			int maxX = Integer.MIN_VALUE;
			int maxY = Integer.MIN_VALUE;
			int maxZ = Integer.MIN_VALUE;
			for (BlockPos localPos : component) {
				BlockPos globalPos = transform.apply(localPos);
				minX = Math.min(minX, globalPos.getX());
				minY = Math.min(minY, globalPos.getY());
				minZ = Math.min(minZ, globalPos.getZ());
				maxX = Math.max(maxX, globalPos.getX());
				maxY = Math.max(maxY, globalPos.getY());
				maxZ = Math.max(maxZ, globalPos.getZ());
			}

			BlockPos controllerGlobalPos = new BlockPos(minX, minY, minZ);
			BlockPos controllerLocalPos = transform.unapply(controllerGlobalPos);
			if (!component.contains(controllerLocalPos))
				controllerLocalPos = fallbackControllerLocalPos;
			return new Bounds(controllerGlobalPos, controllerLocalPos, maxX - minX + 1,
				maxZ - minZ + 1, maxY - minY + 1);
		}
	}
}
