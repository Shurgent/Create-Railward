package ua.pp.shurgent.railward.block.train.parts.panel;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import ua.pp.shurgent.railward.AllBlocks;
import ua.pp.shurgent.railward.block.train.parts.LocomotiveSpotlightBlock;
import ua.pp.shurgent.railward.block.train.parts.LocomotiveSpotlightVariant;
import ua.pp.shurgent.railward.block.train.parts.TrainWallPanelBlock;

public class LocomotiveSpotlightAttachment implements TrainPanelAttachment {

	private static final String VARIANT_KEY = "Variant";

	private LocomotiveSpotlightVariant variant = LocomotiveSpotlightVariant.SMALL;

	public LocomotiveSpotlightAttachment() {
	}

	public LocomotiveSpotlightAttachment(LocomotiveSpotlightVariant variant) {
		this.variant = variant;
	}

	@Override
	public TrainPanelAttachmentType<?> getType() {
		return TrainPanelAttachmentRegistry.LOCOMOTIVE_SPOTLIGHT;
	}

	@Override
	public void write(CompoundTag tag) {
		tag.putString(VARIANT_KEY, variant.getSerializedName());
	}

	@Override
	public void read(CompoundTag tag) {
		variant = LocomotiveSpotlightVariant.byName(tag.getString(VARIANT_KEY));
	}

	@Override
	public ItemStack getDrop() {
		return AllBlocks.LOCOMOTIVE_SPOTLIGHT.asStack();
	}

	@Override
	public VoxelShape getInteractionShape(BlockState panelState) {
		Direction facing = panelState.hasProperty(TrainWallPanelBlock.FACING)
			? panelState.getValue(TrainWallPanelBlock.FACING)
			: Direction.NORTH;
		return LocomotiveSpotlightBlock.getPanelAttachmentShape(variant, facing);
	}

	public LocomotiveSpotlightVariant getVariant() {
		return variant;
	}

	public void cycleVariant() {
		variant = variant.next();
	}
}
