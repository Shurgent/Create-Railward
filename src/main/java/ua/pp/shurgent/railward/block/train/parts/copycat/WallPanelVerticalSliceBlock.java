package ua.pp.shurgent.railward.block.train.parts.copycat;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class WallPanelVerticalSliceBlock extends WallPanelCopycatBlock {

	public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
	private static final double THICKNESS = 3;

	public WallPanelVerticalSliceBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH));
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockState state = super.getStateForPlacement(context);
		if (state == null)
			return null;

		double localX = context.getClickLocation().x - context.getClickedPos().getX();
		double localZ = context.getClickLocation().z - context.getClickedPos().getZ();
		return state.setValue(FACING, cornerFacing(localX, localZ));
	}

	@Override
	protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder.add(FACING));
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		AABB bounds = getBounds(state);
		return Block.box(bounds.minX * 16, bounds.minY * 16, bounds.minZ * 16,
				bounds.maxX * 16, bounds.maxY * 16, bounds.maxZ * 16);
	}

	public static AABB getBounds(BlockState state) {
		Direction facing = state.getValue(FACING);
		boolean highX = facing == Direction.EAST || facing == Direction.SOUTH;
		boolean highZ = facing == Direction.SOUTH || facing == Direction.WEST;
		double minX = highX ? 16 - THICKNESS : 0;
		double minZ = highZ ? 16 - THICKNESS : 0;
		return new AABB(minX / 16, 0, minZ / 16, (minX + THICKNESS) / 16, 1,
				(minZ + THICKNESS) / 16);
	}

	@Override
	protected AABB getShapeBounds(BlockState state) {
		return getBounds(state);
	}

	@Override
	protected boolean connectsAlong(BlockState state, BlockPos offset) {
		return offset.getX() == 0 && offset.getZ() == 0 && Math.abs(offset.getY()) == 1;
	}

	@Override
	public BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	@SuppressWarnings("deprecation")
	public BlockState mirror(BlockState state, Mirror mirror) {
		return state.rotate(mirror.getRotation(state.getValue(FACING)));
	}
}
