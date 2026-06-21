package ua.pp.shurgent.railward.block.train.parts.panel;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

public interface TrainPanelAttachment {

	TrainPanelAttachmentType<?> getType();

	void write(CompoundTag tag);

	void read(CompoundTag tag);

	ItemStack getDrop();

	VoxelShape getInteractionShape(BlockState panelState);
}
