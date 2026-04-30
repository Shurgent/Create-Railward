package ua.pp.shurgent.railward.block.train.parts;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.MountedStorage;
import com.simibubi.create.content.contraptions.MountedStorageManager;
import com.simibubi.create.content.trains.entity.CarriageContraption;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;
import ua.pp.shurgent.railward.mixin.create.ContraptionAccessor;
import ua.pp.shurgent.railward.mixin.create.MountedStorageAccessor;
import ua.pp.shurgent.railward.mixin.create.MountedStorageManagerAccessor;

public final class BulkCargoContraptionAssembly {

	private BulkCargoContraptionAssembly() {
	}

	public static void normalize(CarriageContraption contraption) {
		Map<BlockPos, StructureBlockInfo> blocks = contraption.getBlocks();
		Set<BlockPos> remaining = new HashSet<>();
		for (Map.Entry<BlockPos, StructureBlockInfo> entry : blocks.entrySet())
			if (entry.getValue().state.getBlock() instanceof BulkCargoContainerBlock)
				remaining.add(entry.getKey());

		while (!remaining.isEmpty()) {
			BlockPos start = remaining.iterator()
				.next();
			Set<BlockPos> component = collectComponent(blocks, start, remaining);
			Bounds bounds = Bounds.of(component);
			if (!bounds.isValid(component))
				continue;
			normalizeComponent(contraption, blocks, component, bounds);
		}
	}

	private static Set<BlockPos> collectComponent(Map<BlockPos, StructureBlockInfo> blocks, BlockPos start,
		Set<BlockPos> remaining) {
		Set<BlockPos> component = new HashSet<>();
		Queue<BlockPos> frontier = new ArrayDeque<>();
		frontier.add(start);

		while (!frontier.isEmpty()) {
			BlockPos current = frontier.poll();
			if (!remaining.remove(current))
				continue;
			component.add(current);
			for (Direction direction : Direction.values()) {
				BlockPos next = current.relative(direction);
				if (!remaining.contains(next))
					continue;
				StructureBlockInfo info = blocks.get(next);
				if (info != null && info.state.getBlock() instanceof BulkCargoContainerBlock)
					frontier.add(next);
			}
		}
		return component;
	}

	private static void normalizeComponent(Contraption contraption, Map<BlockPos, StructureBlockInfo> blocks,
		Set<BlockPos> component, Bounds bounds) {
		BlockPos controllerPos = new BlockPos(bounds.minX, bounds.minY, bounds.minZ);
		MountedStorageManager manager = ((ContraptionAccessor) contraption).railward$getStorage();
		Map<BlockPos, MountedStorage> storage = ((MountedStorageManagerAccessor) manager).railward$getStorage();
		IItemHandlerModifiable inventory = normalizeStorage(storage, component, controllerPos, bounds.volume());
		float fillBlocks = BulkCargoStorage.getFillState(inventory) * bounds.height();
		boolean xLong = bounds.sizeX() >= bounds.sizeZ();

		for (int x = 0; x < bounds.sizeX(); x++)
			for (int y = 0; y < bounds.height(); y++)
				for (int z = 0; z < bounds.sizeZ(); z++) {
					BlockPos localPos = new BlockPos(bounds.minX + x, bounds.minY + y, bounds.minZ + z);
					StructureBlockInfo info = blocks.get(localPos);
					if (info == null || !(info.state.getBlock() instanceof BulkCargoContainerBlock))
						continue;

					float localFill = Mth.clamp(fillBlocks - y, 0, 1);
					int cargoLevel = localFill <= 0 ? 0 : Mth.clamp(Mth.ceil(localFill * 16), 1, 16);
					BlockState state = info.state.setValue(BulkCargoContainerBlock.WEST, x == 0)
						.setValue(BulkCargoContainerBlock.EAST, x == bounds.sizeX() - 1)
						.setValue(BulkCargoContainerBlock.NORTH, z == 0)
						.setValue(BulkCargoContainerBlock.SOUTH, z == bounds.sizeZ() - 1)
						.setValue(BulkCargoContainerBlock.BOTTOM, y == 0)
						.setValue(BulkCargoContainerBlock.TOP, y == bounds.height() - 1)
						.setValue(BulkCargoContainerBlock.X_LONG, xLong)
						.setValue(BulkCargoContainerBlock.CARGO_LEVEL, cargoLevel);
					CompoundTag nbt = info.nbt == null ? new CompoundTag() : info.nbt.copy();
					nbt.put("Controller", NbtUtils.writeBlockPos(controllerPos));
					nbt.putInt("SizeX", bounds.sizeX());
					nbt.putInt("SizeZ", bounds.sizeZ());
					nbt.putInt("Height", bounds.height());
					nbt.remove("LastKnownPos");
					if (localPos.equals(controllerPos) && inventory instanceof ItemStackHandler handler)
						nbt.put("Inventory", handler.serializeNBT());
					else
						nbt.remove("Inventory");
					blocks.put(localPos, new StructureBlockInfo(localPos, state, nbt));
				}

		manager.createHandlers();
	}

	private static IItemHandlerModifiable normalizeStorage(Map<BlockPos, MountedStorage> storage, Set<BlockPos> component,
		BlockPos controllerPos, int blocks) {
		int expectedSlots = blocks * BulkCargoContainerBlockEntity.STACKS_PER_BLOCK;
		MountedStorage controllerStorage = storage.get(controllerPos);
		if (controllerStorage == null)
			for (BlockPos pos : component) {
				controllerStorage = storage.get(pos);
				if (controllerStorage != null) {
					storage.put(controllerPos, controllerStorage);
					break;
				}
			}
		if (controllerStorage == null)
			return null;

		ItemStackHandler controllerHandler = ((MountedStorageAccessor) controllerStorage).railward$getHandler();
		boolean needsMerge = controllerHandler == null || controllerHandler.getSlots() != expectedSlots;
		for (BlockPos pos : component)
			if (!pos.equals(controllerPos) && storage.containsKey(pos)) {
				needsMerge = true;
				break;
			}
		if (!needsMerge)
			return controllerHandler;

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
			BulkCargoStorage.insert(merged, stack, false);
		((MountedStorageAccessor) controllerStorage).railward$setHandler(merged);
		storage.put(controllerPos, controllerStorage);
		return merged;
	}

	private record Bounds(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
		static Bounds of(Set<BlockPos> positions) {
			int minX = Integer.MAX_VALUE;
			int minY = Integer.MAX_VALUE;
			int minZ = Integer.MAX_VALUE;
			int maxX = Integer.MIN_VALUE;
			int maxY = Integer.MIN_VALUE;
			int maxZ = Integer.MIN_VALUE;
			for (BlockPos pos : positions) {
				minX = Math.min(minX, pos.getX());
				minY = Math.min(minY, pos.getY());
				minZ = Math.min(minZ, pos.getZ());
				maxX = Math.max(maxX, pos.getX());
				maxY = Math.max(maxY, pos.getY());
				maxZ = Math.max(maxZ, pos.getZ());
			}
			return new Bounds(minX, minY, minZ, maxX, maxY, maxZ);
		}

		boolean isValid(Set<BlockPos> positions) {
			if (sizeX() < 1 || sizeX() > BulkCargoContainerBlockEntity.MAX_SIDE)
				return false;
			if (sizeZ() < 1 || sizeZ() > BulkCargoContainerBlockEntity.MAX_SIDE)
				return false;
			if (height() < 1 || height() > BulkCargoContainerBlockEntity.MAX_HEIGHT)
				return false;
			if (positions.size() != volume())
				return false;
			for (int x = minX; x <= maxX; x++)
				for (int y = minY; y <= maxY; y++)
					for (int z = minZ; z <= maxZ; z++)
						if (!positions.contains(new BlockPos(x, y, z)))
							return false;
			return true;
		}

		int sizeX() {
			return maxX - minX + 1;
		}

		int sizeZ() {
			return maxZ - minZ + 1;
		}

		int height() {
			return maxY - minY + 1;
		}

		int volume() {
			return sizeX() * sizeZ() * height();
		}
	}

}
