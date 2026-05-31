package ua.pp.shurgent.railward.block.train.parts;

import java.util.List;

import javax.annotation.Nullable;

import com.simibubi.create.content.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.IMultiBlockEntityContainer;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.utility.Lang;
import com.simibubi.create.foundation.utility.LangBuilder;
import com.simibubi.create.foundation.utility.animation.LerpedFloat;
import com.simibubi.create.foundation.utility.animation.LerpedFloat.Chaser;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;
import ua.pp.shurgent.railward.Railward;

public class BulkCargoContainerBlockEntity extends SmartBlockEntity
	implements IHaveGoggleInformation, IMultiBlockEntityContainer.Inventory {

	public static final int MAX_SIDE = 11;
	public static final int MAX_HEIGHT = 16;
	public static final int STACKS_PER_BLOCK = 27;
	private static final double TOP_INTAKE_MARGIN = .75;

	private CargoItemHandler inventory;
	private LazyOptional<IItemHandler> itemCapability;
	private final RoutedCargoItemHandler routedInventory;
	private LazyOptional<IItemHandler> routedItemCapability;
	private BlockPos controller;
	private BlockPos lastKnownPos;
	private boolean updateConnectivity;
	private boolean suppressInventoryUpdates;
	private int sizeX;
	private int sizeZ;
	private int height;

	private LerpedFloat cargoLevel;
	private ItemStack clientDisplayStack;
	private boolean clientCargoKnown;
	private BlockPos clientCargoLocalPos;
	private BlockPos clientCargoControllerPos;
	private int clientAmount;
	private int clientCapacity;

	public BulkCargoContainerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
		inventory = new CargoItemHandler(STACKS_PER_BLOCK);
		itemCapability = LazyOptional.of(() -> inventory);
		routedInventory = new RoutedCargoItemHandler();
		routedItemCapability = LazyOptional.of(() -> routedInventory);
		sizeX = 1;
		sizeZ = 1;
		height = 1;
		updateConnectivity = true;
		cargoLevel = LerpedFloat.linear()
			.startWithValue(0);
		clientDisplayStack = ItemStack.EMPTY;
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
	}

	@Override
	public void tick() {
		super.tick();
		if (lastKnownPos == null)
			lastKnownPos = getBlockPos();
		else if (!lastKnownPos.equals(worldPosition) && worldPosition != null) {
			removeController(true);
			lastKnownPos = worldPosition;
			return;
		}

		if (updateConnectivity && level != null && !level.isClientSide) {
			updateConnectivity = false;
			BulkCargoContainerConnectivity.formFrom(this);
		}

		if (cargoLevel != null)
			cargoLevel.tickChaser();

		if (level != null && !level.isClientSide && isController())
			collectDroppedItems();
	}

	public void queueConnectivityUpdate() {
		updateConnectivity = true;
	}

	public ItemStack tryInsert(ItemStack stack, boolean simulate) {
		BulkCargoContainerBlockEntity controllerBE = getControllerBE();
		if (controllerBE == null)
			return stack;
		ItemStack remainder = BulkCargoStorage.insert(controllerBE.inventory, stack, simulate);
		if (!simulate && remainder.getCount() != stack.getCount())
			controllerBE.onCargoContentsChanged();
		return remainder;
	}

	public ItemStack extractOneStack(boolean simulate) {
		BulkCargoContainerBlockEntity controllerBE = getControllerBE();
		if (controllerBE == null)
			return ItemStack.EMPTY;
		ItemStack extracted = BulkCargoStorage.extractOneStack(controllerBE.inventory, simulate);
		if (!simulate && !extracted.isEmpty())
			controllerBE.onCargoContentsChanged();
		return extracted;
	}

	public void dropContents() {
		if (level == null || level.isClientSide)
			return;
		BulkCargoContainerBlockEntity controllerBE = getControllerBE();
		if (controllerBE == null || controllerBE != this)
			return;
		for (ItemStack stack : BulkCargoStorage.extractAll(inventory))
			Containers.dropItemStack(level, worldPosition.getX() + .5, worldPosition.getY() + .5,
				worldPosition.getZ() + .5, stack);
		onCargoContentsChanged();
	}

	void setMultiblock(BlockPos controller, int sizeX, int sizeZ, int height, boolean controllerPart) {
		this.controller = controllerPart ? null : controller;
		this.sizeX = sizeX;
		this.sizeZ = sizeZ;
		this.height = height;
		if (!controllerPart)
			resizeInventory(0);
		setChanged();
		sendData();
	}

	void setSingle() {
		controller = null;
		sizeX = 1;
		sizeZ = 1;
		height = 1;
		resizeInventory(STACKS_PER_BLOCK);
		setChanged();
		sendData();
	}

	List<ItemStack> replaceInventoryContents(List<ItemStack> stacks, int slots) {
		suppressInventoryUpdates = true;
		resizeInventory(slots);
		List<ItemStack> leftovers = new java.util.ArrayList<>();
		for (ItemStack stack : stacks) {
			ItemStack remainder = BulkCargoStorage.insertExisting(inventory, stack, false);
			if (!remainder.isEmpty())
				leftovers.add(remainder);
		}
		suppressInventoryUpdates = false;
		onCargoContentsChanged();
		return leftovers;
	}

	List<ItemStack> takeInventoryContents() {
		suppressInventoryUpdates = true;
		List<ItemStack> stacks = BulkCargoStorage.extractAll(inventory);
		suppressInventoryUpdates = false;
		return stacks;
	}

	IItemHandlerModifiable getInventoryForConnectivity() {
		return inventory;
	}

	private void resizeInventory(int slots) {
		if (inventory.getSlots() == slots)
			return;
		List<ItemStack> stacks = BulkCargoStorage.extractAll(inventory);
		inventory = new CargoItemHandler(slots);
		refreshCapability();
		for (ItemStack stack : stacks) {
			ItemStack remainder = BulkCargoStorage.insertExisting(inventory, stack, false);
			if (!remainder.isEmpty() && level != null && !level.isClientSide)
				Containers.dropItemStack(level, worldPosition.getX() + .5, worldPosition.getY() + .5,
					worldPosition.getZ() + .5, remainder);
		}
	}

	private void resizeInventorySilently(int slots) {
		boolean wasSuppressing = suppressInventoryUpdates;
		suppressInventoryUpdates = true;
		resizeInventory(slots);
		suppressInventoryUpdates = wasSuppressing;
	}

	private int getExpectedInventorySlots() {
		return isController() ? sizeX * sizeZ * height * STACKS_PER_BLOCK : 0;
	}

	private void onCargoContentsChanged() {
		if (suppressInventoryUpdates || level == null || isRemoved())
			return;
		if (!level.isClientSide) {
			setChanged();
			BulkCargoContainerConnectivity.updateCargoLevels(this);
			sendData();
		}
		chaseCargoLevel(getFillState());
	}

	public float getFillState() {
		if (isVirtualCargoView())
			return clientCapacity <= 0 ? 0 : Math.min(1, clientAmount / (float) clientCapacity);
		return BulkCargoStorage.getFillState(inventory);
	}

	public ItemStack getDisplayStack() {
		if (isVirtualCargoView())
			return clientDisplayStack;
		return BulkCargoStorage.getDisplayStack(inventory);
	}

	public int getStoredAmount() {
		if (isVirtualCargoView())
			return clientAmount;
		return BulkCargoStorage.getStoredAmount(inventory);
	}

	public int getCapacityItems() {
		if (isVirtualCargoView())
			return clientCapacity;
		return BulkCargoStorage.getCapacityItems(inventory, getDisplayStack());
	}

	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		BulkCargoContainerBlockEntity tooltipSource = isVirtualCargoView() ? this : getControllerBE();
		if (tooltipSource == null)
			return false;

		ItemStack displayStack = tooltipSource.getDisplayStack();
		railwardLang("gui.goggles.bulk_cargo_container").forGoggles(tooltip);

		LangBuilder cargoLine = Lang.builder()
			.add(railwardLang("gui.goggles.bulk_cargo_container.cargo").style(ChatFormatting.GRAY))
			.space();
		if (displayStack.isEmpty())
			cargoLine.add(railwardLang("gui.goggles.bulk_cargo_container.empty").style(ChatFormatting.DARK_GRAY));
		else
			cargoLine.add(Lang.itemName(displayStack).style(ChatFormatting.GRAY));
		cargoLine.forGoggles(tooltip, 1);

		Lang.builder()
			.add(railwardLang("gui.goggles.bulk_cargo_container.amount").style(ChatFormatting.GRAY))
			.space()
			.add(Lang.number(tooltipSource.getStoredAmount())
				.style(ChatFormatting.GOLD))
			.text(ChatFormatting.GRAY, " / ")
			.add(Lang.number(tooltipSource.getCapacityItems())
				.style(ChatFormatting.DARK_GRAY))
			.space()
			.add(railwardLang("gui.goggles.bulk_cargo_container.items").style(ChatFormatting.GRAY))
			.forGoggles(tooltip, 1);

		return true;
	}

	private static LangBuilder railwardLang(String key) {
		return Lang.builder(Railward.MOD_ID)
			.translate(key);
	}

	private boolean isVirtualCargoView() {
		return level != null && level.isClientSide && clientCargoKnown;
	}

	public boolean hasClientCargoView() {
		return isVirtualCargoView();
	}

	public void applyClientCargo(ItemStack displayStack, int amount, int capacity) {
		applyClientCargo(displayStack, amount, capacity, null, null);
	}

	public void applyClientCargo(ItemStack displayStack, int amount, int capacity, BlockPos localPos,
		BlockPos controllerPos) {
		boolean firstKnownState = !clientCargoKnown;
		clientDisplayStack = displayStack.copy();
		if (!clientDisplayStack.isEmpty())
			clientDisplayStack.setCount(1);
		clientCargoKnown = true;
		clientCargoLocalPos = localPos;
		clientCargoControllerPos = controllerPos;
		clientAmount = amount;
		clientCapacity = capacity;
		float fillState = getFillState();
		if (firstKnownState || cargoLevel == null)
			cargoLevel = LerpedFloat.linear()
				.startWithValue(fillState);
		else
			cargoLevel.chase(fillState, .5f, Chaser.EXP);
	}

	public int getClientCargoLayer() {
		BlockPos localPos = clientCargoLocalPos == null ? worldPosition : clientCargoLocalPos;
		BlockPos controllerPos = clientCargoControllerPos == null ? getController() : clientCargoControllerPos;
		if (localPos == null || controllerPos == null)
			return 0;
		return localPos.getY() - controllerPos.getY();
	}

	private void collectDroppedItems() {
		if (inventory.getSlots() == 0)
			return;

		AABB intakeBox = new AABB(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(),
			worldPosition.getX() + sizeX, worldPosition.getY() + height + TOP_INTAKE_MARGIN,
			worldPosition.getZ() + sizeZ);
		List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, intakeBox,
			item -> item.isAlive() && !item.getItem()
				.isEmpty());
		for (ItemEntity itemEntity : items) {
			ItemStack stack = itemEntity.getItem();
			ItemStack remainder = tryInsert(stack, false);
			if (remainder.getCount() == stack.getCount())
				continue;
			if (remainder.isEmpty())
				itemEntity.discard();
			else
				itemEntity.setItem(remainder);
		}
	}

	private void chaseCargoLevel(float fillState) {
		if (cargoLevel == null)
			cargoLevel = LerpedFloat.linear()
				.startWithValue(fillState);
		cargoLevel.chase(fillState, .5f, Chaser.EXP);
	}

	public LerpedFloat getCargoLevel() {
		return cargoLevel;
	}

	public int getSizeX() {
		return sizeX;
	}

	public int getSizeZ() {
		return sizeZ;
	}

	@Override
	public BlockPos getController() {
		return isController() ? worldPosition : controller;
	}

	@Override
	@SuppressWarnings("unchecked")
	public BulkCargoContainerBlockEntity getControllerBE() {
		if (isController())
			return this;
		if (level == null || controller == null)
			return null;
		BlockEntity blockEntity = level.getBlockEntity(controller);
		if (blockEntity instanceof BulkCargoContainerBlockEntity)
			return (BulkCargoContainerBlockEntity) blockEntity;
		return null;
	}

	@Override
	public boolean isController() {
		return controller == null || worldPosition.equals(controller);
	}

	@Override
	public void setController(BlockPos pos) {
		controller = pos.equals(worldPosition) ? null : pos;
		setChanged();
		sendData();
	}

	@Override
	public void removeController(boolean keepContents) {
		controller = null;
		sizeX = 1;
		sizeZ = 1;
		height = 1;
		if (!keepContents)
			resizeInventory(STACKS_PER_BLOCK);
		setChanged();
		sendData();
	}

	@Override
	public BlockPos getLastKnownPos() {
		return lastKnownPos;
	}

	@Override
	public void preventConnectivityUpdate() {
		updateConnectivity = false;
	}

	@Override
	public void notifyMultiUpdated() {
		setChanged();
		sendData();
	}

	@Override
	public net.minecraft.core.Direction.Axis getMainConnectionAxis() {
		return net.minecraft.core.Direction.Axis.Y;
	}

	@Override
	public int getMaxLength(net.minecraft.core.Direction.Axis longAxis, int width) {
		return longAxis == net.minecraft.core.Direction.Axis.Y ? MAX_HEIGHT : MAX_SIDE;
	}

	@Override
	public int getMaxWidth() {
		return MAX_SIDE;
	}

	@Override
	public int getHeight() {
		return height;
	}

	@Override
	public void setHeight(int height) {
		this.height = height;
	}

	@Override
	public int getWidth() {
		return Math.max(sizeX, sizeZ);
	}

	@Override
	public void setWidth(int width) {
		sizeX = width;
		sizeZ = width;
	}

	@Override
	public boolean hasInventory() {
		return isController();
	}

	@Override
	protected AABB createRenderBoundingBox() {
		if (isController())
			return super.createRenderBoundingBox().expandTowards(sizeX - 1, height - 1, sizeZ - 1);
		return super.createRenderBoundingBox();
	}

	@Override
	protected void write(CompoundTag tag, boolean clientPacket) {
		super.write(tag, clientPacket);
		if (lastKnownPos != null)
			tag.put("LastKnownPos", NbtUtils.writeBlockPos(lastKnownPos));
		if (!isController())
			tag.put("Controller", NbtUtils.writeBlockPos(controller));
		tag.putInt("SizeX", sizeX);
		tag.putInt("SizeZ", sizeZ);
		tag.putInt("Height", height);
		if (isController()) {
			tag.put("Inventory", inventory.serializeNBT());
		}
	}

	@Override
	protected void read(CompoundTag tag, boolean clientPacket) {
		super.read(tag, clientPacket);
		controller = null;
		lastKnownPos = null;
		if (tag.contains("LastKnownPos"))
			lastKnownPos = NbtUtils.readBlockPos(tag.getCompound("LastKnownPos"));
		if (tag.contains("Controller"))
			controller = NbtUtils.readBlockPos(tag.getCompound("Controller"));
		sizeX = Math.max(1, tag.getInt("SizeX"));
		sizeZ = Math.max(1, tag.getInt("SizeZ"));
		height = Math.max(1, tag.getInt("Height"));
		if (isController()) {
			if (tag.contains("Inventory"))
				inventory.deserializeNBT(tag.getCompound("Inventory"));
			resizeInventorySilently(getExpectedInventorySlots());
			chaseCargoLevel(getFillState());
			if (clientPacket && hasLevel())
				invalidateRenderBoundingBox();
		} else
			resizeInventorySilently(0);
	}

	@Override
	public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable net.minecraft.core.Direction side) {
		if (cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
			if (isController()) {
				if (!itemCapability.isPresent())
					refreshCapability();
				return itemCapability.cast();
			}
			if (getControllerBE() != null) {
				if (!routedItemCapability.isPresent())
					routedItemCapability = LazyOptional.of(() -> routedInventory);
				return routedItemCapability.cast();
			}
		}
		return super.getCapability(cap, side);
	}

	private void refreshCapability() {
		LazyOptional<IItemHandler> oldCap = itemCapability;
		itemCapability = LazyOptional.of(() -> inventory);
		if (oldCap != null)
			oldCap.invalidate();
	}

	@Override
	public void invalidate() {
		super.invalidate();
		if (itemCapability != null)
			itemCapability.invalidate();
		if (routedItemCapability != null)
			routedItemCapability.invalidate();
	}

	private class CargoItemHandler extends ItemStackHandler {
		public CargoItemHandler(int size) {
			super(size);
		}

		@Override
		public boolean isItemValid(int slot, ItemStack stack) {
			ItemStack display = BulkCargoStorage.getDisplayStack(this);
			return BulkCargoStorage.isAcceptedCargo(stack)
				&& (display.isEmpty() || BulkCargoStorage.canStackAsCargo(display, stack));
		}

		@Override
		public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
			if (!isItemValid(slot, stack))
				return stack;
			return super.insertItem(slot, stack, simulate);
		}

		@Override
		protected void onContentsChanged(int slot) {
			super.onContentsChanged(slot);
			onCargoContentsChanged();
		}
	}

	private class RoutedCargoItemHandler implements IItemHandlerModifiable {

		private IItemHandlerModifiable getTarget() {
			BulkCargoContainerBlockEntity controllerBE = getControllerBE();
			return controllerBE == null ? null : controllerBE.inventory;
		}

		private boolean hasSlot(IItemHandlerModifiable target, int slot) {
			return target != null && slot >= 0 && slot < target.getSlots();
		}

		@Override
		public void setStackInSlot(int slot, ItemStack stack) {
			IItemHandlerModifiable target = getTarget();
			if (hasSlot(target, slot))
				target.setStackInSlot(slot, stack);
		}

		@Override
		public int getSlots() {
			IItemHandlerModifiable target = getTarget();
			return target == null ? 0 : target.getSlots();
		}

		@Override
		public ItemStack getStackInSlot(int slot) {
			IItemHandlerModifiable target = getTarget();
			return hasSlot(target, slot) ? target.getStackInSlot(slot) : ItemStack.EMPTY;
		}

		@Override
		public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
			IItemHandlerModifiable target = getTarget();
			return hasSlot(target, slot) ? target.insertItem(slot, stack, simulate) : stack;
		}

		@Override
		public ItemStack extractItem(int slot, int amount, boolean simulate) {
			IItemHandlerModifiable target = getTarget();
			return hasSlot(target, slot) ? target.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
		}

		@Override
		public int getSlotLimit(int slot) {
			IItemHandlerModifiable target = getTarget();
			return hasSlot(target, slot) ? target.getSlotLimit(slot) : 0;
		}

		@Override
		public boolean isItemValid(int slot, ItemStack stack) {
			IItemHandlerModifiable target = getTarget();
			return hasSlot(target, slot) && target.isItemValid(slot, stack);
		}
	}

}
