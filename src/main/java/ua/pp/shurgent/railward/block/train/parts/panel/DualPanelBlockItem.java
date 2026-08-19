package ua.pp.shurgent.railward.block.train.parts.panel;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;

public class DualPanelBlockItem extends BlockItem {

	public DualPanelBlockItem(Block block, Properties properties) {
		super(block, properties);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		InteractionResult merged = DualPanelPlacement.tryPlace(context, getBlock());
		return merged != null ? merged : super.useOn(context);
	}
}
