package ua.pp.shurgent.railward.block.train.parts.copycat;

import javax.annotation.Nullable;

import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public interface DualPanelCopycatBlockEntityExtension {

	@Nullable
	Direction railward$getSecondaryFacing();

	BlockState railward$getSecondaryMaterial();

	ItemStack railward$getSecondaryConsumedItem();

	void railward$setSecondaryFacing(@Nullable Direction facing);

	void railward$setSecondaryMaterial(BlockState material);

	void railward$setSecondaryConsumedItem(ItemStack stack);

	void railward$clearSecondaryMaterial();

	void railward$clearSecondary();
}
