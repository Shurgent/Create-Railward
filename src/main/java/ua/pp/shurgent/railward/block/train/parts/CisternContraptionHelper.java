package ua.pp.shurgent.railward.block.train.parts;

import java.util.Map;

import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.MountedFluidStorage;
import com.simibubi.create.content.contraptions.MountedStorageManager;
import com.simibubi.create.content.trains.entity.CarriageContraption;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraftforge.fluids.capability.IFluidHandler;
import ua.pp.shurgent.railward.mixin.create.CarriageContraptionAccessor;
import ua.pp.shurgent.railward.mixin.create.ContraptionAccessor;
import ua.pp.shurgent.railward.mixin.create.MountedStorageManagerAccessor;

public final class CisternContraptionHelper {

	private CisternContraptionHelper() {
	}

	public static ControllerData resolveController(Contraption contraption, BlockPos localPos) {
		StructureBlockInfo info = contraption.getBlocks()
			.get(localPos);
		if (info == null || !(info.state.getBlock() instanceof CisternBlock))
			return null;

		BlockPos controllerPos = localPos;
		CompoundTag nbt = info.nbt;
		if (nbt != null && nbt.contains("Controller"))
			controllerPos = NbtUtils.readBlockPos(nbt.getCompound("Controller"));

		StructureBlockInfo controllerInfo = contraption.getBlocks()
			.get(controllerPos);
		if (controllerInfo == null || !(controllerInfo.state.getBlock() instanceof CisternBlock))
			return null;

		CompoundTag controllerNbt = controllerInfo.nbt;
		int width = readPositiveInt(controllerNbt, "Size", readPositiveInt(nbt, "Size", 1));
		int length = readPositiveInt(controllerNbt, "Height", readPositiveInt(nbt, "Height", 1));
		if (!CisternBlockEntity.isValidWidth(width) || length < 1 || length > CisternBlockEntity.MAX_LENGTH)
			return null;

		return new ControllerData(controllerPos, controllerInfo, width, length);
	}

	public static IFluidHandler getFluidHandler(Contraption contraption, BlockPos controllerPos) {
		MountedStorageManager manager = getStorageManager(contraption);
		if (manager == null)
			return null;

		Map<BlockPos, MountedFluidStorage> storage = ((MountedStorageManagerAccessor) manager).railward$getFluidStorage();
		MountedFluidStorage mountedStorage = storage.get(controllerPos);
		return mountedStorage == null ? null : mountedStorage.getFluidHandler();
	}

	private static MountedStorageManager getStorageManager(Contraption contraption) {
		if (contraption instanceof CarriageContraption carriageContraption) {
			MountedStorageManager storageProxy =
				((CarriageContraptionAccessor) carriageContraption).railward$getStorageProxy();
			if (storageProxy != null)
				return storageProxy;
		}
		return ((ContraptionAccessor) contraption).railward$getStorage();
	}

	private static int readPositiveInt(CompoundTag tag, String key, int fallback) {
		if (tag == null || !tag.contains(key))
			return fallback;
		return Math.max(1, tag.getInt(key));
	}

	public record ControllerData(BlockPos controllerPos, StructureBlockInfo controllerInfo, int width, int length) {
	}

}
