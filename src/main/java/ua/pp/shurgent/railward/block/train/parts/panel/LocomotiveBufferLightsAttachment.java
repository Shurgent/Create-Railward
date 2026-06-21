package ua.pp.shurgent.railward.block.train.parts.panel;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import ua.pp.shurgent.railward.AllBlocks;
import ua.pp.shurgent.railward.block.train.parts.LocomotiveBufferLightsBlock;
import ua.pp.shurgent.railward.block.train.parts.LocomotiveBufferLightsVariant;
import ua.pp.shurgent.railward.block.train.parts.TrainWallPanelBlock;

public class LocomotiveBufferLightsAttachment implements TrainPanelAttachment {

	private static final String VARIANT_KEY = "Variant";

	private LocomotiveBufferLightsVariant variant = LocomotiveBufferLightsVariant.TYPE_1;

	public LocomotiveBufferLightsAttachment() {
	}

	public LocomotiveBufferLightsAttachment(LocomotiveBufferLightsVariant variant) {
		this.variant = variant;
	}

	@Override
	public TrainPanelAttachmentType<?> getType() {
		return TrainPanelAttachmentRegistry.LOCOMOTIVE_BUFFER_LIGHTS;
	}

	@Override
	public void write(CompoundTag tag) {
		tag.putString(VARIANT_KEY, variant.getSerializedName());
	}

	@Override
	public void read(CompoundTag tag) {
		variant = LocomotiveBufferLightsVariant.byName(tag.getString(VARIANT_KEY));
	}

	@Override
	public ItemStack getDrop() {
		return AllBlocks.LOCOMOTIVE_BUFFER_LIGHTS.asStack();
	}

	@Override
	public VoxelShape getInteractionShape(BlockState panelState) {
		Direction facing = panelState.hasProperty(TrainWallPanelBlock.FACING)
			? panelState.getValue(TrainWallPanelBlock.FACING)
			: Direction.NORTH;
		return LocomotiveBufferLightsBlock.getPanelAttachmentShape(facing);
	}

	public LocomotiveBufferLightsVariant getVariant() {
		return variant;
	}

	public void cycleVariant() {
		variant = variant.next();
	}
}
