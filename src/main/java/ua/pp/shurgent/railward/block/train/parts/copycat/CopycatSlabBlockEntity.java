package ua.pp.shurgent.railward.block.train.parts.copycat;

import java.util.ArrayList;
import java.util.List;

import com.simibubi.create.content.contraptions.StructureTransform;
import com.simibubi.create.content.decoration.copycat.CopycatBlockEntity;
import com.simibubi.create.content.redstone.RoseQuartzLampBlock;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import com.simibubi.create.content.schematics.requirement.ItemRequirement.ItemUseType;
import com.simibubi.create.content.schematics.requirement.ItemRequirement.StackRequirement;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.phys.Vec3;
//? if >=1.19.2
/*import net.minecraftforge.client.model.data.ModelData;*/
//? if <1.19.2 {
import net.minecraftforge.client.model.data.IModelData;
import net.minecraftforge.client.model.data.ModelDataMap;
//?}

/**
 * Stores a material and its consumed item independently for each slab half.
 * The inherited CopycatBlockEntity data is kept as a compatibility material
 * for Create's common copycat helpers; rendering and drops use these two slots.
 */
public class CopycatSlabBlockEntity extends CopycatBlockEntity {

	private BlockState bottomMaterial = copycatBase();
	private BlockState topMaterial = copycatBase();
	private ItemStack bottomItem = ItemStack.EMPTY;
	private ItemStack topItem = ItemStack.EMPTY;

	public CopycatSlabBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	public BlockState getMaterial(Half half) {
		return half == Half.TOP ? topMaterial : bottomMaterial;
	}

	@Override
	public BlockState getMaterial() {
		return hasCustomMaterial(Half.BOTTOM) ? bottomMaterial
				: hasCustomMaterial(Half.TOP) ? topMaterial : copycatBase();
	}

	public ItemStack getConsumedItem(Half half) {
		return half == Half.TOP ? topItem : bottomItem;
	}

	public boolean hasCustomMaterial(Half half) {
		return !getMaterial(half).is(com.simibubi.create.AllBlocks.COPYCAT_BASE.get());
	}

	@Override
	public boolean hasCustomMaterial() {
		return hasCustomMaterial(Half.BOTTOM) || hasCustomMaterial(Half.TOP);
	}

	public void setMaterial(Half half, BlockState material) {
		if (half == Half.TOP)
			topMaterial = material;
		else
			bottomMaterial = material;
		notifyUpdate();
	}

	public void setConsumedItem(Half half, ItemStack stack) {
		ItemStack stored = stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
		if (!stored.isEmpty())
			stored.setCount(1);
		if (half == Half.TOP)
			topItem = stored;
		else
			bottomItem = stored;
		notifyUpdate();
	}

	public void clear(Half half) {
		if (half == Half.TOP) {
			topMaterial = copycatBase();
			topItem = ItemStack.EMPTY;
		} else {
			bottomMaterial = copycatBase();
			bottomItem = ItemStack.EMPTY;
		}
		notifyUpdate();
	}

	public boolean cycleMaterial(Half half) {
		BlockState material = getMaterial(half);
		BlockState cycled;
		if (material.hasProperty(TrapDoorBlock.HALF)
				&& material.getOptionalValue(TrapDoorBlock.OPEN).orElse(false))
			cycled = material.cycle(TrapDoorBlock.HALF);
		else if (material.hasProperty(BlockStateProperties.FACING))
			cycled = material.cycle(BlockStateProperties.FACING);
		else if (material.hasProperty(BlockStateProperties.HORIZONTAL_FACING))
			cycled = material.setValue(BlockStateProperties.HORIZONTAL_FACING,
					material.getValue(BlockStateProperties.HORIZONTAL_FACING).getClockWise());
		else if (material.hasProperty(BlockStateProperties.AXIS))
			cycled = material.cycle(BlockStateProperties.AXIS);
		else if (material.hasProperty(BlockStateProperties.HORIZONTAL_AXIS))
			cycled = material.cycle(BlockStateProperties.HORIZONTAL_AXIS);
		else if (material.hasProperty(BlockStateProperties.LIT))
			cycled = material.cycle(BlockStateProperties.LIT);
		else if (material.hasProperty(RoseQuartzLampBlock.POWERING))
			cycled = material.cycle(RoseQuartzLampBlock.POWERING);
		else
			return false;

		setMaterial(half, cycled);
		return true;
	}

	public boolean otherHalfPaysFor(Half half, ItemStack candidate) {
		ItemStack other = getConsumedItem(opposite(half));
		return !other.isEmpty() && other.getItem() == candidate.getItem();
	}

	public void transferPaymentToOtherHalf(Half removedHalf) {
		ItemStack removed = getConsumedItem(removedHalf);
		Half otherHalf = opposite(removedHalf);
		if (!removed.isEmpty() && hasCustomMaterial(otherHalf) && getConsumedItem(otherHalf).isEmpty()
				&& getMaterial(otherHalf).is(getMaterial(removedHalf).getBlock())) {
			setConsumedItem(otherHalf, removed);
			setConsumedItem(removedHalf, ItemStack.EMPTY);
		}
	}

	public List<ItemStack> getPaidItems() {
		List<ItemStack> result = new ArrayList<>(2);
		if (!bottomItem.isEmpty())
			result.add(bottomItem.copy());
		if (!topItem.isEmpty())
			result.add(topItem.copy());
		return result;
	}

	public void clearPaidItems() {
		bottomItem = ItemStack.EMPTY;
		topItem = ItemStack.EMPTY;
	}

	@Override
	public ItemRequirement getRequiredItems(BlockState state) {
		List<StackRequirement> requirements = getPaidItems().stream()
				.map(stack -> new StackRequirement(stack, ItemUseType.CONSUME))
				.toList();
		return requirements.isEmpty() ? ItemRequirement.NONE : new ItemRequirement(requirements);
	}

	@Override
	//? if create6
	/*public void transform(BlockEntity blockEntity, StructureTransform transform) {
	*///? if !create6
	public void transform(StructureTransform transform) {
		bottomMaterial = transform.apply(bottomMaterial);
		topMaterial = transform.apply(topMaterial);
		Vec3 transformedUp = transform.applyWithoutOffsetUncentered(new Vec3(0, 1, 0));
		if (transformedUp.y < 0) {
			BlockState material = bottomMaterial;
			bottomMaterial = topMaterial;
			topMaterial = material;
			ItemStack item = bottomItem;
			bottomItem = topItem;
			topItem = item;
		}
		notifyUpdate();
	}

	@Override
	protected void read(CompoundTag tag, boolean clientPacket) {
		BlockState oldBottom = bottomMaterial;
		BlockState oldTop = topMaterial;
		super.read(tag, clientPacket);
		bottomMaterial = readMaterial(tag, "BottomMaterial");
		topMaterial = readMaterial(tag, "TopMaterial");
		bottomItem = readItem(tag, "BottomItem");
		topItem = readItem(tag, "TopItem");
		if (clientPacket && (!bottomMaterial.equals(oldBottom) || !topMaterial.equals(oldTop)))
			redraw();
	}

	@Override
	public void writeSafe(CompoundTag tag) {
		super.writeSafe(tag);
		writeCustom(tag, true);
	}

	@Override
	protected void write(CompoundTag tag, boolean clientPacket) {
		super.write(tag, clientPacket);
		writeCustom(tag, false);
	}

	@Override
	//? if >=1.19.2 {
	/*public ModelData getModelData() {
		return ModelData.builder()
				.with(CopycatSlabModelData.BOTTOM_MATERIAL, bottomMaterial)
				.with(CopycatSlabModelData.TOP_MATERIAL, topMaterial)
				.build();
	}
	*///?} else {
	public IModelData getModelData() {
		return new ModelDataMap.Builder()
				.withInitial(CopycatSlabModelData.BOTTOM_MATERIAL, bottomMaterial)
				.withInitial(CopycatSlabModelData.TOP_MATERIAL, topMaterial)
				.build();
	}
	//?}

	private void writeCustom(CompoundTag tag, boolean safe) {
		tag.put("BottomMaterial", NbtUtils.writeBlockState(bottomMaterial));
		tag.put("TopMaterial", NbtUtils.writeBlockState(topMaterial));
		tag.put("BottomItem", writeItem(bottomItem, safe));
		tag.put("TopItem", writeItem(topItem, safe));
	}

	private static CompoundTag writeItem(ItemStack stack, boolean safe) {
		ItemStack copy = stack.copy();
		if (safe)
			copy.setTag(null);
		return copy.save(new CompoundTag());
	}

	private static ItemStack readItem(CompoundTag tag, String key) {
		return tag.contains(key) ? ItemStack.of(tag.getCompound(key)) : ItemStack.EMPTY;
	}

	private static BlockState readMaterial(CompoundTag tag, String key) {
		return tag.contains(key) ? ua.pp.shurgent.railward.compat.RailwardComponents.readBlockState(tag.getCompound(key)) : copycatBase();
	}

	private static BlockState copycatBase() {
		return com.simibubi.create.AllBlocks.COPYCAT_BASE.getDefaultState();
	}

	private void redraw() {
		if (!isVirtual())
			requestModelDataUpdate();
		if (hasLevel()) {
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 16);
			level.getChunkSource().getLightEngine().checkBlock(worldPosition);
		}
	}

	public static Half opposite(Half half) {
		return half == Half.TOP ? Half.BOTTOM : Half.TOP;
	}
}
