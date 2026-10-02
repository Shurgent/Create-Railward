package ua.pp.shurgent.railward.block.train.parts;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.MountedFluidStorage;
import com.simibubi.create.content.contraptions.MountedStorageManager;
import com.simibubi.create.content.trains.entity.CarriageContraption;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.network.PacketDistributor;
import ua.pp.shurgent.railward.RailwardPackets;
import ua.pp.shurgent.railward.mixin.create.CarriageContraptionAccessor;
import ua.pp.shurgent.railward.mixin.create.ContraptionAccessor;
import ua.pp.shurgent.railward.mixin.create.MountedStorageManagerAccessor;
import ua.pp.shurgent.railward.network.CisternFluidSyncPacket;

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
		MountedStorageManager primary = getStorageManager(contraption);
		IFluidHandler primaryHandler = getFluidHandler(primary, controllerPos);
		MountedStorageManager base = getBaseStorageManager(contraption);
		if (base != primary) {
			IFluidHandler baseHandler = getFluidHandler(base, controllerPos);
			if (isBetterFluidView(baseHandler, primaryHandler))
				return baseHandler;
		}
		return primaryHandler;
	}

	public static void syncFluid(AbstractContraptionEntity entity, ControllerData data) {
		syncFluid(entity, data.controllerPos());
	}

	public static void syncFluid(AbstractContraptionEntity entity, BlockPos controllerPos) {
		if (entity.level.isClientSide)
			return;

		Contraption contraption = entity.getContraption();
		if (contraption == null)
			return;

		IFluidHandler handler = getFluidHandler(contraption, controllerPos);
		if (handler == null || handler.getTanks() <= 0)
			return;

		RailwardPackets.getChannel()
			.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity),
				new CisternFluidSyncPacket(entity.getId(), controllerPos, handler.getFluidInTank(0)
					.copy()));
	}

	public static void syncAllFluids(AbstractContraptionEntity entity) {
		if (entity.level.isClientSide || entity.getContraption() == null)
			return;

		Contraption contraption = entity.getContraption();
		Set<BlockPos> seenControllers = new HashSet<>();
		for (Map.Entry<BlockPos, StructureBlockInfo> entry : contraption.getBlocks().entrySet()) {
			if (!(entry.getValue().state.getBlock() instanceof CisternBlock))
				continue;
			ControllerData data = resolveController(contraption, entry.getKey());
			if (data != null && seenControllers.add(data.controllerPos()))
				syncFluid(entity, data);
		}
	}

	public static boolean applyClientFluid(AbstractContraptionEntity entity, BlockPos controllerPos, FluidStack fluid) {
		Contraption contraption = entity.getContraption();
		if (contraption == null)
			return false;

		boolean applied = false;
		MountedStorageManager base = getBaseStorageManager(contraption);
		applied |= updateContainedFluid(base, controllerPos, fluid);
		MountedStorageManager proxy = getStorageProxyManager(contraption);
		if (proxy != null && proxy != base)
			applied |= updateContainedFluid(proxy, controllerPos, fluid);
		applied |= updateClientTank(contraption, controllerPos, fluid);
		return applied;
	}

	private static IFluidHandler getFluidHandler(MountedStorageManager manager, BlockPos controllerPos) {
		if (manager == null)
			return null;
		Map<BlockPos, MountedFluidStorage> storage = ((MountedStorageManagerAccessor) manager).railward$getFluidStorage();
		MountedFluidStorage mountedStorage = storage.get(controllerPos);
		return mountedStorage == null ? null : mountedStorage.getFluidHandler();
	}

	private static boolean isBetterFluidView(IFluidHandler candidate, IFluidHandler current) {
		if (candidate == null || candidate.getTanks() <= 0)
			return false;
		if (current == null || current.getTanks() <= 0)
			return true;
		return current.getFluidInTank(0)
			.isEmpty()
			&& !candidate.getFluidInTank(0)
				.isEmpty();
	}

	private static boolean updateContainedFluid(MountedStorageManager manager, BlockPos controllerPos, FluidStack fluid) {
		if (manager == null)
			return false;
		manager.updateContainedFluid(controllerPos, fluid.copy());
		return getFluidHandler(manager, controllerPos) != null;
	}

	private static boolean updateClientTank(Contraption contraption, BlockPos controllerPos, FluidStack fluid) {
		BlockEntity blockEntity = contraption.presentBlockEntities.get(controllerPos);
		if (!(blockEntity instanceof CisternBlockEntity cistern) || !(cistern.getTankInventory() instanceof FluidTank tank))
			return false;

		tank.setFluid(fluid.copy());
		return true;
	}

	private static MountedStorageManager getStorageManager(Contraption contraption) {
		MountedStorageManager base = getBaseStorageManager(contraption);
		if (contraption.entity != null && (contraption.entity).level.isClientSide)
			return base;

		MountedStorageManager storageProxy = getStorageProxyManager(contraption);
		return storageProxy == null ? base : storageProxy;
	}

	private static MountedStorageManager getStorageProxyManager(Contraption contraption) {
		if (!(contraption instanceof CarriageContraption carriageContraption))
			return null;
		return ((CarriageContraptionAccessor) carriageContraption).railward$getStorageProxy();
	}

	private static MountedStorageManager getBaseStorageManager(Contraption contraption) {
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
