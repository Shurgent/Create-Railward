package ua.pp.shurgent.railward.block.train.parts.copycat;

import javax.annotation.Nullable;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.contraptions.StructureTransform;
import com.simibubi.create.content.decoration.copycat.CopycatBlockEntity;
import com.simibubi.create.content.decoration.copycat.CopycatModel;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import com.simibubi.create.content.schematics.requirement.ItemRequirement.ItemUseType;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.model.data.IModelData;
import net.minecraftforge.client.model.data.ModelDataMap;

/** Copycat data owned by Railward; Create's copycat block entities remain untouched. */
public class DualPanelCopycatBlockEntity extends CopycatBlockEntity
		implements DualPanelCopycatBlockEntityExtension {

	private static final String SECONDARY_FACING = "SecondaryFacing";
	private static final String SECONDARY_MATERIAL = "SecondaryMaterial";
	private static final String SECONDARY_ITEM = "SecondaryItem";
	private static final String LEGACY_SECONDARY_FACING = "RailwardSecondaryFacing";
	private static final String LEGACY_SECONDARY_MATERIAL = "RailwardSecondaryMaterial";
	private static final String LEGACY_SECONDARY_ITEM = "RailwardSecondaryItem";

	@Nullable
	private Direction secondaryFacing;
	private BlockState secondaryMaterial = AllBlocks.COPYCAT_BASE.getDefaultState();
	private ItemStack secondaryConsumedItem = ItemStack.EMPTY;

	public DualPanelCopycatBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Override
	@Nullable
	public Direction railward$getSecondaryFacing() {
		return secondaryFacing;
	}

	@Override
	public BlockState railward$getSecondaryMaterial() {
		return secondaryMaterial;
	}

	@Override
	public ItemStack railward$getSecondaryConsumedItem() {
		return secondaryConsumedItem;
	}

	@Override
	public void railward$setSecondaryFacing(@Nullable Direction facing) {
		secondaryFacing = facing;
		configurationChanged();
	}

	@Override
	public void railward$setSecondaryMaterial(BlockState material) {
		secondaryMaterial = material;
		configurationChanged();
	}

	@Override
	public void railward$setSecondaryConsumedItem(ItemStack stack) {
		secondaryConsumedItem = stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
		if (!secondaryConsumedItem.isEmpty())
			secondaryConsumedItem.setCount(1);
		configurationChanged();
	}

	@Override
	public void railward$clearSecondaryMaterial() {
		secondaryMaterial = AllBlocks.COPYCAT_BASE.getDefaultState();
		secondaryConsumedItem = ItemStack.EMPTY;
		configurationChanged();
	}

	@Override
	public void railward$clearSecondary() {
		secondaryFacing = null;
		secondaryMaterial = AllBlocks.COPYCAT_BASE.getDefaultState();
		secondaryConsumedItem = ItemStack.EMPTY;
		configurationChanged();
	}

	private void configurationChanged() {
		if (level != null && !level.isClientSide) {
			notifyUpdate();
			return;
		}
		setChanged();
		redraw();
	}

	private void redraw() {
		if (!isVirtual())
			requestModelDataUpdate();
		if (hasLevel()) {
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 16);
			level.getChunkSource().getLightEngine().checkBlock(worldPosition);
		}
	}

	@Override
	public ItemRequirement getRequiredItems(BlockState state) {
		ItemRequirement requirement = super.getRequiredItems(state);
		if (secondaryFacing == null)
			return requirement;
		requirement = requirement.union(new ItemRequirement(ItemUseType.CONSUME, new ItemStack(state.getBlock())));
		if (!secondaryConsumedItem.isEmpty())
			requirement = requirement.union(new ItemRequirement(ItemUseType.CONSUME, secondaryConsumedItem));
		return requirement;
	}

	@Override
	public void transform(StructureTransform transform) {
		super.transform(transform);
		if (secondaryFacing == null)
			return;
		secondaryMaterial = transform.apply(secondaryMaterial);
		Vec3 normal = transform.applyWithoutOffsetUncentered(Vec3.atLowerCornerOf(secondaryFacing.getNormal()));
		secondaryFacing = Direction.getNearest((float) normal.x, (float) normal.y, (float) normal.z);
		notifyUpdate();
	}

	@Override
	protected void read(CompoundTag tag, boolean clientPacket) {
		Direction previousFacing = secondaryFacing;
		BlockState previousMaterial = secondaryMaterial;
		super.read(tag, clientPacket);
		String facingKey = tag.contains(SECONDARY_FACING) ? SECONDARY_FACING : LEGACY_SECONDARY_FACING;
		String materialKey = tag.contains(SECONDARY_MATERIAL) ? SECONDARY_MATERIAL : LEGACY_SECONDARY_MATERIAL;
		String itemKey = tag.contains(SECONDARY_ITEM) ? SECONDARY_ITEM : LEGACY_SECONDARY_ITEM;
		secondaryFacing = tag.contains(facingKey)
				? Direction.from3DDataValue(tag.getInt(facingKey)) : null;
		secondaryMaterial = tag.contains(materialKey)
				? NbtUtils.readBlockState(tag.getCompound(materialKey))
				: AllBlocks.COPYCAT_BASE.getDefaultState();
		secondaryConsumedItem = tag.contains(itemKey)
				? ItemStack.of(tag.getCompound(itemKey)) : ItemStack.EMPTY;
		if (secondaryFacing != null && !secondaryFacing.getAxis().isHorizontal()) {
			secondaryFacing = null;
			secondaryMaterial = AllBlocks.COPYCAT_BASE.getDefaultState();
			secondaryConsumedItem = ItemStack.EMPTY;
		}
		if (clientPacket && (previousFacing != secondaryFacing || !previousMaterial.equals(secondaryMaterial)))
			redraw();
	}

	@Override
	public void writeSafe(CompoundTag tag) {
		super.writeSafe(tag);
		writeSecondary(tag, true);
	}

	@Override
	protected void write(CompoundTag tag, boolean clientPacket) {
		super.write(tag, clientPacket);
		writeSecondary(tag, false);
	}

	private void writeSecondary(CompoundTag tag, boolean safe) {
		if (secondaryFacing == null)
			return;
		tag.putInt(SECONDARY_FACING, secondaryFacing.get3DDataValue());
		tag.put(SECONDARY_MATERIAL, NbtUtils.writeBlockState(secondaryMaterial));
		ItemStack stored = secondaryConsumedItem.copy();
		if (safe)
			stored.setTag(null);
		tag.put(SECONDARY_ITEM, stored.save(new CompoundTag()));
	}

	@Override
	public IModelData getModelData() {
		ModelDataMap.Builder builder = new ModelDataMap.Builder()
				.withInitial(CopycatModel.MATERIAL_PROPERTY, getMaterial());
		if (secondaryFacing != null)
			builder.withInitial(DualPanelCopycatModelData.SECONDARY_FACING, secondaryFacing)
					.withInitial(DualPanelCopycatModelData.SECONDARY_MATERIAL, secondaryMaterial);
		return builder.build();
	}
}
