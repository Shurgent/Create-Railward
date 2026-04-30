package ua.pp.shurgent.railward.block.train.parts;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import ua.pp.shurgent.railward.Railward;

public final class BulkCargoRules {

	public static final TagKey<Item> BULK_CARGO_BLACKLIST =
		TagKey.create(Registry.ITEM_REGISTRY, Railward.asResource("bulk_cargo_blacklist"));

	private BulkCargoRules() {
	}

	public static boolean isAcceptedCargo(ItemStack stack) {
		if (stack.isEmpty() || stack.is(BULK_CARGO_BLACKLIST))
			return false;
		if (!(stack.getItem() instanceof BlockItem))
			return true;
		return isSimpleBlockCargo(stack);
	}

	public static boolean isSimpleBlockCargo(ItemStack stack) {
		if (!(stack.getItem() instanceof BlockItem blockItem))
			return false;

		Block block = blockItem.getBlock();
		BlockState defaultState = block.defaultBlockState();
		if (defaultState.getRenderShape() != RenderShape.MODEL)
			return false;

		if (block instanceof SlabBlock)
			return true;

		VoxelShape shape = defaultState.getShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
		return Block.isShapeFullBlock(shape);
	}

}
