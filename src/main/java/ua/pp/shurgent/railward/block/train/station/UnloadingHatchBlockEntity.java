package ua.pp.shurgent.railward.block.train.station;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import ua.pp.shurgent.railward.block.train.parts.BulkCargoContainerBlock;
import ua.pp.shurgent.railward.block.train.parts.BulkCargoContraptionHelper;
import ua.pp.shurgent.railward.block.train.parts.BulkCargoStorage;
import ua.pp.shurgent.railward.client.sound.UnloadingBulkCargoLoopSound;

public class UnloadingHatchBlockEntity extends SmartBlockEntity {

	private static final int MAX_STACKS_PER_TICK = 4;
	private static final int TRANSFER_EFFECT_TICKS = 20;

	private ItemStack transferStack = ItemStack.EMPTY;
	private int transferEffectTicks;
	private int transferDistance = 1;
	private int transferVisualItems;
	private int transferVisualSequence;
	private int lastClientTransferVisualSequence;
	private final List<UnloadingFallingItemVisual> fallingItems = new ArrayList<>();
	private UnloadingBulkCargoLoopSound soundInstance;

	public UnloadingHatchBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
	}

	@Override
	public void tick() {
		super.tick();
		if (transferEffectTicks > 0)
			transferEffectTicks--;
		if (level == null)
			return;
		if (level.isClientSide) {
			DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> tickClientEffects());
			return;
		}

		IItemHandler source = getSourceInventory();
		if (source == null)
			return;

		ContainerTarget target = findContainerTarget();
		if (target == null)
			return;

		boolean movedAny = false;
		for (int i = 0; i < MAX_STACKS_PER_TICK; i++) {
			ItemStack moved = transferOneStack(source, target);
			if (moved.isEmpty())
				break;

			movedAny = true;
			showTransfer(moved, target.distance());
			UnloadingTransferEffects.emit(level, Vec3.atCenterOf(worldPosition),
				Vec3.atBottomCenterOf(worldPosition.below(target.distance())), moved);
		}

		if (!movedAny)
			return;

		BulkCargoContraptionHelper.updateCargoLevels(target.entity(), target.data(), target.handler());
		BulkCargoContraptionHelper.syncCargo(target.entity(), target.data().controllerPos(), target.handler());
		sendData();
	}

	private IItemHandler getSourceInventory() {
		BlockEntity blockEntity = level.getBlockEntity(worldPosition.above());
		if (blockEntity == null)
			return null;
		Capability<IItemHandler> itemCapability = CapabilityItemHandler.ITEM_HANDLER_CAPABILITY;
		return blockEntity.getCapability(itemCapability, Direction.DOWN)
			.resolve()
			.orElse(null);
	}

	private ContainerTarget findContainerTarget() {
		AABB searchBox = new AABB(worldPosition).expandTowards(0, -4, 0)
			.inflate(3);
		List<AbstractContraptionEntity> entities =
			level.getEntitiesOfClass(AbstractContraptionEntity.class, searchBox, entity -> entity.isAlive());

		for (AbstractContraptionEntity entity : entities) {
			Contraption contraption = entity.getContraption();
			if (contraption == null)
				continue;

			Set<BlockPos> seenControllers = new HashSet<>();
			for (Map.Entry<BlockPos, StructureBlockInfo> entry : contraption.getBlocks()
				.entrySet()) {
				if (!(entry.getValue().state.getBlock() instanceof BulkCargoContainerBlock))
					continue;

				BulkCargoContraptionHelper.ControllerData data =
					BulkCargoContraptionHelper.resolveController(contraption, entry.getKey());
				if (data == null || !seenControllers.add(data.controllerPos()))
					continue;

				int distance = findDistanceToContainer(entity, data);
				if (distance == 0)
					continue;

				IItemHandlerModifiable handler = BulkCargoContraptionHelper.getItemHandler(contraption,
					data.controllerPos());
				if (handler != null)
					return new ContainerTarget(entity, data, handler, distance);
			}
		}
		return null;
	}

	private int findDistanceToContainer(AbstractContraptionEntity entity,
		BulkCargoContraptionHelper.ControllerData data) {
		for (int distance = 1; distance <= 3; distance++) {
			Vec3 localPos = entity.toLocalVector(Vec3.atCenterOf(worldPosition.below(distance)), 0);
			if (isInsideContainer(data, localPos))
				return distance;
		}
		return 0;
	}

	private boolean isInsideContainer(BulkCargoContraptionHelper.ControllerData data, Vec3 localPos) {
		BlockPos origin = data.controllerPos();
		double minX = origin.getX();
		double minY = origin.getY();
		double minZ = origin.getZ();
		double maxX = minX + data.sizeX();
		double maxY = minY + data.height();
		double maxZ = minZ + data.sizeZ();
		return localPos.x >= minX && localPos.x < maxX && localPos.y >= minY && localPos.y < maxY
			&& localPos.z >= minZ && localPos.z < maxZ;
	}

	private ItemStack transferOneStack(IItemHandler source, ContainerTarget target) {
		for (int slot = 0; slot < source.getSlots(); slot++) {
			ItemStack simulatedExtract = source.extractItem(slot, 64, true);
			if (simulatedExtract.isEmpty())
				continue;

			ItemStack simulatedRemainder = BulkCargoStorage.insert(target.handler(), simulatedExtract, true);
			int accepted = simulatedExtract.getCount() - simulatedRemainder.getCount();
			if (accepted <= 0)
				continue;

			ItemStack extracted = source.extractItem(slot, accepted, false);
			if (extracted.isEmpty())
				continue;

			ItemStack remainder = BulkCargoStorage.insert(target.handler(), extracted, false);
			int moved = extracted.getCount() - remainder.getCount();
			if (!remainder.isEmpty())
				source.insertItem(slot, remainder, false);
			if (moved <= 0)
				continue;

			ItemStack movedStack = extracted.copy();
			movedStack.setCount(moved);
			return movedStack;
		}
		return ItemStack.EMPTY;
	}

	private void showTransfer(ItemStack stack, int distance) {
		transferStack = stack.copy();
		transferStack.setCount(1);
		transferDistance = Mth.clamp(distance, 1, 3);
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
		return new AABB(worldPosition).expandTowards(0, -4, 0)
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
			fallingItems.add(UnloadingFallingItemVisual.create(transferStack, startTick, transferDistance, random));
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

	@Override
	public void onChunkUnloaded() {
		super.onChunkUnloaded();
		stopClientAudio();
	}

	@Override
	public void invalidate() {
		super.invalidate();
		stopClientAudio();
	}

	private void stopClientAudio() {
		if (level == null || !level.isClientSide)
			return;
		DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> stopLoop());
	}

	private record ContainerTarget(AbstractContraptionEntity entity, BulkCargoContraptionHelper.ControllerData data,
		IItemHandlerModifiable handler, int distance) {
	}
}
