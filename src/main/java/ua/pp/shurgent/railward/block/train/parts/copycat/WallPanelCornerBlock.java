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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class WallPanelCornerBlock extends WallPanelCopycatBlock {

	public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
	public static final EnumProperty<Half> HALF = BlockStateProperties.HALF;
	private static final double SIZE = 3;

	public WallPanelCornerBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState()
				.setValue(FACING, Direction.NORTH)
				.setValue(HALF, Half.BOTTOM));
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockState state = super.getStateForPlacement(context);
		if (state == null)
			return null;

		double localX = context.getClickLocation().x - context.getClickedPos().getX();
		double localY = context.getClickLocation().y - context.getClickedPos().getY();
		double localZ = context.getClickLocation().z - context.getClickedPos().getZ();
		return state.setValue(FACING, cornerFacing(localX, localZ))
				.setValue(HALF, localY > .5 ? Half.TOP : Half.BOTTOM);
	}

	@Override
	protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder.add(FACING, HALF));
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
		boolean highY = state.getValue(HALF) == Half.TOP;
		boolean highZ = facing == Direction.SOUTH || facing == Direction.WEST;
		double minX = highX ? 16 - SIZE : 0;
		double minY = highY ? 16 - SIZE : 0;
		double minZ = highZ ? 16 - SIZE : 0;
		return new AABB(minX / 16, minY / 16, minZ / 16, (minX + SIZE) / 16,
				(minY + SIZE) / 16, (minZ + SIZE) / 16);
	}

	@Override
	protected AABB getShapeBounds(BlockState state) {
		return getBounds(state);
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
