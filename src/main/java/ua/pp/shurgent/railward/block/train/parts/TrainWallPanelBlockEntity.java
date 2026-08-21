package ua.pp.shurgent.railward.block.train.parts;

import java.util.List;

import javax.annotation.Nullable;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.content.contraptions.ITransformableBlockEntity;
import com.simibubi.create.content.contraptions.StructureTransform;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import com.simibubi.create.content.schematics.requirement.ItemRequirement.ItemUseType;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.IModelData;
import net.minecraftforge.client.model.data.ModelDataMap;
import ua.pp.shurgent.railward.block.train.parts.panel.TrainPanelAttachment;
import ua.pp.shurgent.railward.block.train.parts.panel.TrainPanelAttachmentRegistry;

public class TrainWallPanelBlockEntity extends SmartBlockEntity implements ITransformableBlockEntity {

	private static final String ATTACHMENT_KEY = "PanelAttachment";
	private static final String SECONDARY_PANEL_KEY = "SecondaryPanel";

	@Nullable
	private TrainPanelAttachment attachment;
	@Nullable
	private BlockState secondaryPanel;

	public TrainWallPanelBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
	}

	public boolean hasAttachment() {
		return attachment != null;
	}

	@Nullable
	public TrainPanelAttachment getAttachment() {
		return attachment;
	}

	public void setAttachment(@Nullable TrainPanelAttachment attachment) {
		this.attachment = attachment;
		configurationChanged();
	}

	public void attachmentChanged() {
		configurationChanged();
	}

	public boolean hasSecondaryPanel() {
		return secondaryPanel != null;
	}

	@Nullable
	public BlockState getSecondaryPanel() {
		return secondaryPanel;
	}

	public void setSecondaryPanel(@Nullable BlockState state) {
		secondaryPanel = state;
		configurationChanged();
		if (state != null && level != null)
			TrainRoofPartitionBehaviour.enablePartitionsAbove(level, worldPosition);
	}

	public ItemStack removeSecondaryPanel() {
		if (secondaryPanel == null)
			return ItemStack.EMPTY;
		ItemStack drop = new ItemStack(secondaryPanel.getBlock());
		secondaryPanel = null;
		configurationChanged();
		return drop;
	}

	public void dropSecondaryPanel(Level level, BlockPos pos) {
		ItemStack drop = removeSecondaryPanel();
		if (!drop.isEmpty())
			Containers.dropItemStack(level, pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5, drop);
	}

	public ItemStack removeAttachment() {
		if (attachment == null)
			return ItemStack.EMPTY;
		ItemStack drop = attachment.getDrop();
		attachment = null;
		configurationChanged();
		return drop;
	}

	public void dropAttachment(Level level, BlockPos pos) {
		ItemStack drop = removeAttachment();
		if (!drop.isEmpty())
			Containers.dropItemStack(level, pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5, drop);
	}

	private void configurationChanged() {
		setChanged();
		if (level == null)
			return;
		if (!level.isClientSide) {
			notifyUpdate();
			return;
		}
		requestModelDataUpdate();
		level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 16);
	}

	@Override
	protected void write(CompoundTag tag, boolean clientPacket) {
		super.write(tag, clientPacket);
		writeAttachment(tag);
	}

	@Override
	public void writeSafe(CompoundTag tag) {
		super.writeSafe(tag);
		writeAttachment(tag);
	}

	private void writeAttachment(CompoundTag tag) {
		if (attachment != null)
			tag.put(ATTACHMENT_KEY, TrainPanelAttachmentRegistry.write(attachment));
		if (secondaryPanel != null)
			tag.put(SECONDARY_PANEL_KEY, NbtUtils.writeBlockState(secondaryPanel));
	}

	@Nullable
	public static TrainPanelAttachment readAttachment(@Nullable CompoundTag tag) {
		if (tag == null || !tag.contains(ATTACHMENT_KEY, Tag.TAG_COMPOUND))
			return null;
		return TrainPanelAttachmentRegistry.read(tag.getCompound(ATTACHMENT_KEY));
	}

	@Override
	protected void read(CompoundTag tag, boolean clientPacket) {
		BlockState previousSecondary = secondaryPanel;
		super.read(tag, clientPacket);
		attachment = readAttachment(tag);
		secondaryPanel = tag.contains(SECONDARY_PANEL_KEY, Tag.TAG_COMPOUND)
				? NbtUtils.readBlockState(tag.getCompound(SECONDARY_PANEL_KEY)) : null;
		if (secondaryPanel != null && !TrainWallPanelBlock.isTrainWallPanelState(secondaryPanel))
			secondaryPanel = null;
		if (clientPacket && previousSecondary != secondaryPanel)
			configurationChanged();
	}

	@Override
	public ItemRequirement getRequiredItems(BlockState state) {
		return secondaryPanel == null ? ItemRequirement.NONE
				: new ItemRequirement(ItemUseType.CONSUME, new ItemStack(secondaryPanel.getBlock()));
	}

	@Override
	public void transform(StructureTransform transform) {
		if (secondaryPanel != null)
			secondaryPanel = transform.apply(secondaryPanel);
		notifyUpdate();
	}

	@Override
	public IModelData getModelData() {
		ModelDataMap.Builder builder = new ModelDataMap.Builder();
		if (secondaryPanel != null)
			builder.withInitial(TrainWallPanelModelData.SECONDARY_PANEL, secondaryPanel);
		return builder.build();
	}
}
