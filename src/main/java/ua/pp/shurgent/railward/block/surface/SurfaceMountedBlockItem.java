package ua.pp.shurgent.railward.block.surface;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class SurfaceMountedBlockItem extends BlockItem {

	public SurfaceMountedBlockItem(Block block, Properties properties) {
		super(block, properties);
	}

	@Override
	protected boolean placeBlock(BlockPlaceContext context, BlockState state) {
		if (!super.placeBlock(context, state))
			return false;
		if (context.getLevel().getBlockEntity(context.getClickedPos()) instanceof SurfaceMountedBlockEntity mounted)
			mounted.setAnchor(SurfaceMountedBlockEntity.fromWorldHit(context.getClickLocation(),
				context.getClickedPos(), state.getValue(SurfaceMountedBlock.FACING)));
		return true;
	}
}
