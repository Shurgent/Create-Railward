package ua.pp.shurgent.railward.block.train.parts;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.MountedStorageManager;
import com.simibubi.create.content.contraptions.StructureTransform;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraftforge.fluids.capability.IFluidHandler;
import ua.pp.shurgent.railward.compat.RailwardMountedStorage;
import ua.pp.shurgent.railward.compat.RailwardMountedStorage.FluidStorage;

public final class CisternContraptionDisassembly {

	private CisternContraptionDisassembly() {
	}

	public static void prepare(Contraption contraption, StructureTransform transform) {
		MountedStorageManager manager = BulkCargoContraptionHelper.getStorageManager(contraption);
		if (manager == null)
			return;

		Map<BlockPos, FluidStorage> fluidStorage = RailwardMountedStorage.fluidStorages(manager);
		if (fluidStorage.isEmpty())
			return;

		Map<BlockPos, Set<BlockPos>> components = new HashMap<>();
		for (Map.Entry<BlockPos, StructureBlockInfo> entry : contraption.getBlocks()
			.entrySet()) {
			StructureBlockInfo info = entry.getValue();
			if (!(info.state.getBlock() instanceof CisternBlock))
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
			StorageNormalization normalized =
				normalizeFluidStorage(fluidStorage, bounds.controllerLocalPos(), entry.getValue());
			changed |= normalized.changed();
			writeDisassemblyNbt(contraption, entry.getValue(), bounds, normalized.handler());
		}

		if (changed)
			RailwardMountedStorage.commitFluids(manager, fluidStorage);
	}

	private static BlockPos getControllerLocalPos(Contraption contraption, StructureTransform transform,
		BlockPos localPos, CompoundTag nbt) {
		if (nbt == null || !nbt.contains("Controller"))
			return localPos;

		BlockPos controllerPos = NbtUtils.readBlockPos(nbt.getCompound("Controller"));
		BlockPos localControllerPos = transform.unapply(controllerPos);
		if (isCisternBlock(contraption, localControllerPos))
			return localControllerPos;
		if (isCisternBlock(contraption, controllerPos))
			return controllerPos;
		return localPos;
	}

	private static boolean isCisternBlock(Contraption contraption, BlockPos localPos) {
		StructureBlockInfo info = contraption.getBlocks()
			.get(localPos);
		return info != null && info.state.getBlock() instanceof CisternBlock;
	}

	private static StorageNormalization normalizeFluidStorage(Map<BlockPos, FluidStorage> storage,
		BlockPos controllerPos, Set<BlockPos> component) {
		FluidStorage controllerStorage = storage.get(controllerPos);
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

		boolean changed = !controllerStoragePos.equals(controllerPos);
		for (BlockPos pos : component)
			if (!pos.equals(controllerStoragePos) && storage.remove(pos) != null)
				changed = true;

		if (!controllerStoragePos.equals(controllerPos)) {
			storage.remove(controllerStoragePos);
			storage.put(controllerPos, controllerStorage);
		}
		return new StorageNormalization(changed, controllerStorage.handler());
	}

	private static void writeDisassemblyNbt(Contraption contraption, Set<BlockPos> component, Bounds bounds,
		IFluidHandler handler) {
		for (BlockPos localPos : component) {
			StructureBlockInfo info = contraption.getBlocks()
				.get(localPos);
			if (info == null || !(info.state.getBlock() instanceof CisternBlock))
				continue;

			CompoundTag nbt = info.nbt == null ? new CompoundTag() : info.nbt.copy();
			nbt.put("Controller", NbtUtils.writeBlockPos(bounds.controllerGlobalPos()));
			nbt.putBoolean("Window", false);
			nbt.putInt("Size", bounds.width());
			nbt.putInt("Height", bounds.length());
			nbt.remove("LastKnownPos");
			if (localPos.equals(bounds.controllerLocalPos())) {
				if (handler != null)
					nbt.put("TankContent", snapshotFluid(handler));
			} else
				nbt.remove("TankContent");

			contraption.getBlocks()
				.put(localPos, new StructureBlockInfo(info.pos, info.state, nbt));
		}
	}

	private static CompoundTag snapshotFluid(IFluidHandler handler) {
		if (handler == null || handler.getTanks() <= 0)
			return new CompoundTag();
		return handler.getFluidInTank(0)
			.copy()
			.writeToNBT(new CompoundTag());
	}

	private record StorageNormalization(boolean changed, IFluidHandler handler) {
	}

	private record Bounds(BlockPos controllerGlobalPos, BlockPos controllerLocalPos, int width, int length) {
		private static Bounds of(Set<BlockPos> component, StructureTransform transform,
			BlockPos fallbackControllerLocalPos) {
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

			int sizeX = maxX - minX + 1;
			int sizeY = maxY - minY + 1;
			int sizeZ = maxZ - minZ + 1;
			int width = sizeY;
			int length = Math.max(sizeX, sizeZ);
			BlockPos controllerGlobalPos = new BlockPos(minX, minY, minZ);
			BlockPos controllerLocalPos = transform.unapply(controllerGlobalPos);
			if (!component.contains(controllerLocalPos))
				controllerLocalPos = fallbackControllerLocalPos;
			return new Bounds(controllerGlobalPos, controllerLocalPos, width, length);
		}
	}
}
