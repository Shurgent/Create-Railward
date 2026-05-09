package ua.pp.shurgent.railward.block.train.station;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.annotation.Nullable;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.infrastructure.config.AllConfigs;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;
import ua.pp.shurgent.railward.block.train.parts.BulkCargoContraptionHelper;
import ua.pp.shurgent.railward.client.sound.UnloadingBulkCargoLoopSound;

public class UnloadingBunkerBlockEntity extends SmartBlockEntity {

	static final int ANIMATION = 4;
	private static final int TRANSFER_EFFECT_TICKS = 20;

	private int transferTimer;
	int keepAlive;
	private Entity connectedEntity;
	private IItemHandler connectedInventory;
	private LazyOptional<IItemHandler> itemCapability;
	private Vec3 sourcePosition;
	private ItemStack transferStack = ItemStack.EMPTY;
	private int transferEffectTicks;
	private int transferDistance = 1;
	private int transferVisualItems;
	private int transferVisualSequence;
	private int lastClientTransferVisualSequence;
	private final List<UnloadingFallingItemVisual> fallingItems = new ArrayList<>();
	private UnloadingBulkCargoLoopSound soundInstance;

	public UnloadingBunkerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
		itemCapability = createEmptyHandler();
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
	}

	public void startUnloadingFrom(Contraption contraption, BlockPos hatchPos) {
		IItemHandlerModifiable inventory = contraption.getSharedInventory();
		if (connectedEntity == contraption.entity && connectedInventory == inventory) {
			updateSource(hatchPos);
			return;
		}

		connectTo(inventory, contraption.entity, hatchPos);
	}

	public void startUnloadingFrom(IItemHandler inventory, BlockPos hatchPos) {
		if (connectedEntity == null && connectedInventory == inventory) {
			updateSource(hatchPos);
			return;
		}

		connectTo(inventory, null, hatchPos);
	}

	private void connectTo(IItemHandler inventory, Entity entity, BlockPos hatchPos) {
		LazyOptional<IItemHandler> oldCapability = itemCapability;
		itemCapability = LazyOptional.of(() -> new PullOnlySourceHandler(inventory));
		oldCapability.invalidate();

		connectedEntity = entity;
		connectedInventory = inventory;
		updateSource(hatchPos);
		transferTimer = 6 + ANIMATION * 2;
		setChanged();
		sendData();
		if (level != null)
			level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
	}

	private LazyOptional<IItemHandler> createEmptyHandler() {
		return LazyOptional.of(() -> new PullOnlySourceHandler(new ItemStackHandler(0)));
	}

	private void updateSource(BlockPos hatchPos) {
		sourcePosition = Vec3.atCenterOf(hatchPos);
		transferDistance = Mth.clamp(hatchPos.getY() - worldPosition.getY(), 1, 3);
	}

	private void stopTransferring() {
		if (connectedInventory == null)
			return;

		LazyOptional<IItemHandler> oldCapability = itemCapability;
		itemCapability = createEmptyHandler();
		oldCapability.invalidate();
		connectedEntity = null;
		connectedInventory = null;
		keepAlive = 0;
		setChanged();
		if (level != null)
			level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
	}

	boolean canReleaseContraption() {
		return transferTimer <= ANIMATION;
	}

	private boolean canTransfer() {
		if (connectedEntity != null && !connectedEntity.isAlive())
			stopTransferring();
		return connectedInventory != null && isConnected();
	}

	private boolean isConnected() {
		int timeUnit = getTransferTimeout();
		return transferTimer >= ANIMATION && transferTimer <= timeUnit + ANIMATION;
	}

	@Override
	public void tick() {
		super.tick();
		if (level == null)
			return;
		if (transferEffectTicks > 0)
			transferEffectTicks--;
		if (level.isClientSide) {
			DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> tickClientEffects());
			return;
		}

		if (keepAlive > 0) {
			keepAlive--;
			if (keepAlive == 0 && !level.isClientSide) {
				stopTransferring();
				transferTimer = ANIMATION - 1;
				sendData();
				return;
			}
		}

		transferTimer = Math.min(transferTimer, ANIMATION * 2 + getTransferTimeout());
		boolean timerCanDecrement = transferTimer > ANIMATION || transferTimer > 0 && keepAlive == 0;
		if (timerCanDecrement) {
			transferTimer--;
			if (transferTimer <= 0)
				stopTransferring();
		}
	}

	@Override
	public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
		if (cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY)
			return itemCapability.cast();
		return super.getCapability(cap, side);
	}

	@Override
	public void invalidate() {
		super.invalidate();
		if (itemCapability != null)
			itemCapability.invalidate();
		stopClientAudio();
	}

	@Override
	public void onChunkUnloaded() {
		super.onChunkUnloaded();
		stopClientAudio();
	}

	private void stopClientAudio() {
		if (level == null || !level.isClientSide)
			return;
		DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> stopLoop());
	}

	private void onContentTransferred(ItemStack stack) {
		transferTimer = getTransferTimeout() + ANIMATION;
		showTransfer(stack);
		if (connectedEntity instanceof AbstractContraptionEntity contraptionEntity)
			BulkCargoContraptionHelper.updateAndSyncAllCargo(contraptionEntity);
		if (level != null)
			UnloadingTransferEffects.emit(level, sourcePosition == null ? Vec3.atCenterOf(worldPosition.above())
				: sourcePosition, Vec3.atBottomCenterOf(worldPosition), stack);
		sendData();
	}

	private void showTransfer(ItemStack stack) {
		transferStack = stack.copy();
		transferStack.setCount(1);
		transferVisualItems = UnloadingFallingItemVisual.itemCountFor(stack);
		transferVisualSequence++;
		transferEffectTicks = TRANSFER_EFFECT_TICKS;
	}

	public ItemStack getTransferStack() {
		return transferEffectTicks > 0 ? transferStack : ItemStack.EMPTY;
	}

	public int getTransferDistance() {
		return transferDistance;
	}

	public List<UnloadingFallingItemVisual> getFallingItems() {
		return fallingItems;
	}

	@Override
	public AABB getRenderBoundingBox() {
		return new AABB(worldPosition).expandTowards(0, 4, 0)
			.inflate(.5);
	}

	private void tickClientEffects() {
		if (transferVisualSequence != lastClientTransferVisualSequence) {
			lastClientTransferVisualSequence = transferVisualSequence;
			spawnFallingItems();
		}
		tickClientFallingItems();
		tickClientAudio();
	}

	private void tickClientFallingItems() {
		long gameTime = level.getGameTime();
		fallingItems.removeIf(item -> item.isFinished(gameTime));
	}

	private void spawnFallingItems() {
		if (level == null || transferStack.isEmpty() || transferVisualItems <= 0)
			return;

		long gameTime = level.getGameTime();
		Random random = new Random(worldPosition.asLong() ^ (long) transferVisualSequence * 0x9E3779B9L);
		for (int i = 0; i < transferVisualItems; i++) {
			long startTick = gameTime + (long) i * UnloadingFallingItemVisual.SPAWN_INTERVAL_TICKS;
			fallingItems.add(UnloadingFallingItemVisual.create(transferStack, startTick, transferDistance + .5f, 0,
				random));
		}
		while (fallingItems.size() > UnloadingFallingItemVisual.MAX_ACTIVE_ITEMS)
			fallingItems.remove(0);
	}

	private void tickClientAudio() {
		if (transferEffectTicks > 0) {
			ensureLoop();
			if (soundInstance != null)
				soundInstance.keepAlive();
			return;
		}

		stopLoop();
	}

	private void ensureLoop() {
		if (soundInstance != null && !soundInstance.isStopped())
			return;

		soundInstance = new UnloadingBulkCargoLoopSound(worldPosition);
		Minecraft.getInstance()
			.getSoundManager()
			.play(soundInstance);
	}

	private void stopLoop() {
		if (soundInstance == null)
			return;
		soundInstance.stopLoop();
		soundInstance = null;
	}

	@Override
	protected void write(CompoundTag tag, boolean clientPacket) {
		super.write(tag, clientPacket);
		if (!clientPacket)
			return;

		tag.putInt("Timer", transferTimer);
		if (transferEffectTicks <= 0 || transferStack.isEmpty())
			return;

		if (!transferStack.isEmpty())
			tag.put("TransferStack", transferStack.save(new CompoundTag()));
		tag.putInt("TransferEffectTicks", transferEffectTicks);
		tag.putInt("TransferDistance", transferDistance);
		tag.putInt("TransferVisualItems", transferVisualItems);
		tag.putInt("TransferVisualSequence", transferVisualSequence);
	}

	@Override
	protected void read(CompoundTag tag, boolean clientPacket) {
		super.read(tag, clientPacket);
		if (!clientPacket) {
			transferTimer = 0;
			clearTransferVisualState();
			lastClientTransferVisualSequence = transferVisualSequence;
			return;
		}

		transferTimer = tag.getInt("Timer");
		if (!tag.contains("TransferStack")) {
			clearTransferVisualState();
			lastClientTransferVisualSequence = transferVisualSequence;
			return;
		}

		transferStack = tag.contains("TransferStack") ? ItemStack.of(tag.getCompound("TransferStack"))
			: ItemStack.EMPTY;
		transferEffectTicks = tag.getInt("TransferEffectTicks");
		transferDistance = Mth.clamp(tag.getInt("TransferDistance"), 1, 3);
		transferVisualItems = Mth.clamp(tag.getInt("TransferVisualItems"), 0,
			UnloadingFallingItemVisual.MAX_ITEMS_PER_TRANSFER);
		transferVisualSequence = tag.getInt("TransferVisualSequence");
		if (!clientPacket)
			lastClientTransferVisualSequence = transferVisualSequence;
		else if (level != null && level.isClientSide && transferVisualSequence != lastClientTransferVisualSequence) {
			lastClientTransferVisualSequence = transferVisualSequence;
			spawnFallingItems();
		}
	}

	private void clearTransferVisualState() {
		transferStack = ItemStack.EMPTY;
		transferEffectTicks = 0;
		transferDistance = 1;
		transferVisualItems = 0;
		transferVisualSequence = 0;
	}

	private int getTransferTimeout() {
		return AllConfigs.server().logistics.psiTimeout.get();
	}

	private class PullOnlySourceHandler implements IItemHandlerModifiable {
		private final IItemHandler wrapped;

		private PullOnlySourceHandler(IItemHandler wrapped) {
			this.wrapped = wrapped;
		}

		@Override
		public void setStackInSlot(int slot, ItemStack stack) {
		}

		@Override
		public int getSlots() {
			return canTransfer() ? wrapped.getSlots() : 0;
		}

		@Override
		public ItemStack getStackInSlot(int slot) {
			return canTransfer() && slot >= 0 && slot < wrapped.getSlots() ? wrapped.getStackInSlot(slot)
				: ItemStack.EMPTY;
		}

		@Override
		public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
			return stack;
		}

		@Override
		public ItemStack extractItem(int slot, int amount, boolean simulate) {
			if (!canTransfer() || slot < 0 || slot >= wrapped.getSlots())
				return ItemStack.EMPTY;
			ItemStack extracted = wrapped.extractItem(slot, amount, simulate);
			if (!simulate && !extracted.isEmpty())
				onContentTransferred(extracted);
			return extracted;
		}

		@Override
		public int getSlotLimit(int slot) {
			return canTransfer() && slot >= 0 && slot < wrapped.getSlots() ? wrapped.getSlotLimit(slot) : 0;
		}

		@Override
		public boolean isItemValid(int slot, ItemStack stack) {
			return false;
		}
	}
}
