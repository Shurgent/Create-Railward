package ua.pp.shurgent.railward.block.train.parts;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.simibubi.create.Create;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.MountedStorage;
import com.simibubi.create.content.contraptions.MountedStorageManager;
import com.simibubi.create.content.contraptions.minecart.TrainCargoManager;
import com.simibubi.create.content.trains.entity.CarriageContraption;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.network.PacketDistributor;
import ua.pp.shurgent.railward.RailwardPackets;
import ua.pp.shurgent.railward.mixin.create.CarriageContraptionAccessor;
import ua.pp.shurgent.railward.mixin.create.ContraptionAccessor;
import ua.pp.shurgent.railward.mixin.create.MountedStorageManagerAccessor;
import ua.pp.shurgent.railward.mixin.create.TrainCargoManagerAccessor;
import ua.pp.shurgent.railward.network.BulkCargoSyncPacket;

public final class BulkCargoContraptionHelper {

	private BulkCargoContraptionHelper() {
	}

	public static ControllerData resolveController(Contraption contraption, BlockPos localPos) {
		StructureBlockInfo info = contraption.getBlocks()
			.get(localPos);
		if (info == null || !(info.state.getBlock() instanceof BulkCargoContainerBlock))
			return null;

		BlockPos controllerPos = localPos;
		CompoundTag nbt = info.nbt;
		if (nbt != null && nbt.contains("Controller"))
			controllerPos = NbtUtils.readBlockPos(nbt.getCompound("Controller"));

		StructureBlockInfo controllerInfo = contraption.getBlocks()
			.get(controllerPos);
		if (controllerInfo == null || !(controllerInfo.state.getBlock() instanceof BulkCargoContainerBlock))
			return null;

		CompoundTag controllerNbt = controllerInfo.nbt;
		int sizeX = readPositiveInt(controllerNbt, "SizeX", readPositiveInt(nbt, "SizeX", 1));
		int sizeZ = readPositiveInt(controllerNbt, "SizeZ", readPositiveInt(nbt, "SizeZ", 1));
		int height = readPositiveInt(controllerNbt, "Height", readPositiveInt(nbt, "Height", 1));
		return new ControllerData(controllerPos, controllerInfo, sizeX, sizeZ, height);
	}

	public static IItemHandlerModifiable getItemHandler(Contraption contraption, BlockPos controllerPos) {
		MountedStorageManager manager = getStorageManager(contraption);
		if (manager == null)
			return null;
		Map<BlockPos, MountedStorage> storage = ((MountedStorageManagerAccessor) manager).railward$getStorage();
		MountedStorage mountedStorage = storage.get(controllerPos);
		return mountedStorage == null ? null : mountedStorage.getItemHandler();
	}

	static boolean tryInsertItemEntity(AbstractContraptionEntity contraptionEntity, ControllerData data,
		ItemEntity itemEntity) {
		if (!itemEntity.isAlive() || itemEntity.getItem()
			.isEmpty())
			return false;

		IItemHandlerModifiable handler = getItemHandler(contraptionEntity.getContraption(), data.controllerPos());
		if (handler == null)
			return false;

		ItemStack stack = itemEntity.getItem();
		ItemStack remainder = BulkCargoStorage.insert(handler, stack, false);
		if (remainder.getCount() == stack.getCount())
			return false;
		if (remainder.isEmpty())
			itemEntity.discard();
		else
			itemEntity.setItem(remainder);

		onCargoContentsChanged(contraptionEntity, data, handler);
		return true;
	}

	static boolean tryExtractOneStackToPlayer(AbstractContraptionEntity contraptionEntity, ControllerData data,
		net.minecraft.world.entity.player.Player player) {
		IItemHandlerModifiable handler = getItemHandler(contraptionEntity.getContraption(), data.controllerPos());
		if (handler == null)
			return false;

		ItemStack extracted = BulkCargoStorage.extractOneStack(handler, false);
		if (extracted.isEmpty())
			return false;

		ItemHandlerHelper.giveItemToPlayer(player, extracted);
		onCargoContentsChanged(contraptionEntity, data, handler);
		return true;
	}

	public static void onCargoContentsChanged(AbstractContraptionEntity contraptionEntity, ControllerData data,
		IItemHandlerModifiable handler) {
		updateCargoLevels(contraptionEntity, data, handler, true);
		markStorageChanged(contraptionEntity.getContraption());
		syncCargo(contraptionEntity, data.controllerPos(), handler);
	}

	public static void updateAndSyncAllCargo(AbstractContraptionEntity entity) {
		if (entity.level.isClientSide)
			return;

		Contraption contraption = entity.getContraption();
		if (contraption == null)
			return;

		boolean changed = false;
		Set<BlockPos> seenControllers = new HashSet<>();
		for (Map.Entry<BlockPos, StructureBlockInfo> entry : contraption.getBlocks()
			.entrySet()) {
			if (!(entry.getValue().state.getBlock() instanceof BulkCargoContainerBlock))
				continue;

			ControllerData data = resolveController(contraption, entry.getKey());
			if (data == null || !seenControllers.add(data.controllerPos()))
				continue;

			IItemHandlerModifiable handler = getItemHandler(contraption, data.controllerPos());
			if (handler == null)
				continue;

			updateCargoLevels(entity, data, handler, true);
			syncCargo(entity, data.controllerPos(), handler);
			changed = true;
		}

		if (changed)
			markStorageChanged(contraption);
	}

	public static void updateCargoLevels(AbstractContraptionEntity contraptionEntity, ControllerData data,
		IItemHandler inventory) {
		updateCargoLevels(contraptionEntity, data, inventory, false);
	}

	private static void updateCargoLevels(AbstractContraptionEntity contraptionEntity, ControllerData data,
		IItemHandler inventory, boolean updateInventoryNbt) {
		float fillBlocks = BulkCargoStorage.getFillState(inventory) * data.height();
		boolean xLong = data.sizeX() >= data.sizeZ();
		Contraption contraption = contraptionEntity.getContraption();
		for (int x = 0; x < data.sizeX(); x++)
			for (int y = 0; y < data.height(); y++)
				for (int z = 0; z < data.sizeZ(); z++) {
					BlockPos localPos = data.controllerPos()
						.offset(x, y, z);
					StructureBlockInfo info = contraption.getBlocks()
						.get(localPos);
					if (info == null || !(info.state.getBlock() instanceof BulkCargoContainerBlock))
						continue;

					float localFill = Mth.clamp(fillBlocks - y, 0, 1);
					int cargoLevel = localFill <= 0 ? 0 : Mth.clamp(Mth.ceil(localFill * 16), 1, 16);
					BlockState updated = info.state.setValue(BulkCargoContainerBlock.WEST, x == 0)
						.setValue(BulkCargoContainerBlock.EAST, x == data.sizeX() - 1)
						.setValue(BulkCargoContainerBlock.NORTH, z == 0)
						.setValue(BulkCargoContainerBlock.SOUTH, z == data.sizeZ() - 1)
						.setValue(BulkCargoContainerBlock.BOTTOM, y == 0)
						.setValue(BulkCargoContainerBlock.TOP, y == data.height() - 1)
						.setValue(BulkCargoContainerBlock.X_LONG, xLong)
						.setValue(BulkCargoContainerBlock.CARGO_LEVEL, cargoLevel);
					CompoundTag nbt = updateInventoryNbt && localPos.equals(data.controllerPos())
						? updatedControllerNbt(info.nbt, data, inventory) : info.nbt;
					if (updated != info.state || nbt != info.nbt)
						contraptionEntity.setBlock(localPos, new StructureBlockInfo(localPos, updated, nbt));
				}
	}

	static void updateCargoLevels(AbstractContraptionEntity contraptionEntity, ControllerData data) {
		IItemHandlerModifiable handler = getItemHandler(contraptionEntity.getContraption(), data.controllerPos());
		if (handler != null)
			updateCargoLevels(contraptionEntity, data, handler);
	}

	public static void syncCargo(AbstractContraptionEntity entity, BlockPos controllerPos, IItemHandler inventory) {
		ItemStack display = BulkCargoStorage.getDisplayStack(inventory);
		int amount = BulkCargoStorage.getStoredAmount(inventory);
		int capacity = BulkCargoStorage.getCapacityItems(inventory, display);
		RailwardPackets.getChannel()
			.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity),
				new BulkCargoSyncPacket(entity.getId(), controllerPos, display, amount, capacity));
	}

	public static void syncCargo(AbstractContraptionEntity entity, ControllerData data) {
		IItemHandlerModifiable handler = getItemHandler(entity.getContraption(), data.controllerPos());
		if (handler != null)
			syncCargo(entity, data.controllerPos(), handler);
	}

	public static void syncAllCargo(AbstractContraptionEntity entity) {
		if (entity.level.isClientSide)
			return;

		Contraption contraption = entity.getContraption();
		if (contraption == null)
			return;

		Set<BlockPos> seenControllers = new HashSet<>();
		for (Map.Entry<BlockPos, StructureBlockInfo> entry : contraption.getBlocks()
			.entrySet()) {
			if (!(entry.getValue().state.getBlock() instanceof BulkCargoContainerBlock))
				continue;

			ControllerData data = resolveController(contraption, entry.getKey());
			if (data == null || !seenControllers.add(data.controllerPos()))
				continue;

			syncCargo(entity, data);
		}
	}

	public static boolean applyClientCargo(AbstractContraptionEntity entity, BlockPos controllerPos,
		ItemStack displayStack, int amount, int capacity) {
		Contraption contraption = entity.getContraption();
		if (contraption == null)
			return false;

		boolean applied = false;
		for (Map.Entry<BlockPos, BlockEntity> entry : contraption.presentBlockEntities.entrySet()) {
			if (!(entry.getValue() instanceof BulkCargoContainerBlockEntity be))
				continue;
			ControllerData data = resolveController(contraption, entry.getKey());
			if (data == null || !data.controllerPos()
				.equals(controllerPos))
				continue;
			be.applyClientCargo(displayStack, amount, capacity, entry.getKey(), controllerPos);
			applied = true;
		}
		return applied;
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

	private static void markStorageChanged(Contraption contraption) {
		if (contraption == null)
			return;
		MountedStorageManager manager = getStorageManager(contraption);
		if (manager instanceof TrainCargoManager) {
			((TrainCargoManagerAccessor) manager).railward$changeDetected();
			Create.RAILWAYS.markTracksDirty();
		}
	}

	private static CompoundTag updatedControllerNbt(CompoundTag source, ControllerData data, IItemHandler inventory) {
		CompoundTag nbt = source == null ? new CompoundTag() : source.copy();
		nbt.put("Controller", NbtUtils.writeBlockPos(data.controllerPos()));
		nbt.putInt("SizeX", data.sizeX());
		nbt.putInt("SizeZ", data.sizeZ());
		nbt.putInt("Height", data.height());
		nbt.remove("LastKnownPos");
		if (inventory != null)
			nbt.put("Inventory", snapshotInventory(inventory));
		else
			nbt.remove("Inventory");
		return nbt;
	}

	private static CompoundTag snapshotInventory(IItemHandler inventory) {
		ItemStackHandler snapshot = new ItemStackHandler(inventory.getSlots());
		for (int slot = 0; slot < inventory.getSlots(); slot++)
			snapshot.setStackInSlot(slot, inventory.getStackInSlot(slot)
				.copy());
		return snapshot.serializeNBT();
	}

	private static int readPositiveInt(CompoundTag tag, String key, int fallback) {
		if (tag == null || !tag.contains(key))
			return fallback;
		return Math.max(1, tag.getInt(key));
	}

	public record ControllerData(BlockPos controllerPos, StructureBlockInfo controllerInfo, int sizeX, int sizeZ,
		int height) {
	}

}
