package ua.pp.shurgent.railward.block.train.parts;

import java.util.List;

import javax.annotation.Nullable;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import ua.pp.shurgent.railward.block.train.parts.panel.TrainPanelAttachment;
import ua.pp.shurgent.railward.block.train.parts.panel.TrainPanelAttachmentRegistry;

public class TrainWallPanelBlockEntity extends SmartBlockEntity {

	private static final String ATTACHMENT_KEY = "PanelAttachment";

	@Nullable
	private TrainPanelAttachment attachment;

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
		if (level != null && !level.isClientSide)
			notifyUpdate();
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
	}

	@Override
	protected void read(CompoundTag tag, boolean clientPacket) {
		super.read(tag, clientPacket);
		attachment = tag.contains(ATTACHMENT_KEY) ? TrainPanelAttachmentRegistry.read(tag.getCompound(ATTACHMENT_KEY))
			: null;
	}
}
