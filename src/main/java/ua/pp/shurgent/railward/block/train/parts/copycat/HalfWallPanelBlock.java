package ua.pp.shurgent.railward.block.train.parts.copycat;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class HalfWallPanelBlock extends WallPanelCopycatBlock {

	public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
	public static final EnumProperty<Half> HALF = BlockStateProperties.HALF;
	private static final double HEIGHT = 8;
	private static final double THICKNESS = 3;

	public HalfWallPanelBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState()
				.setValue(FACING, Direction.SOUTH)
				.setValue(HALF, Half.BOTTOM));
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockState state = super.getStateForPlacement(context);
		if (state == null)
			return null;

		Direction facing = context.getHorizontalDirection();
		double horizontalClick = facing.getAxis() == Axis.X
				? context.getClickLocation().x - context.getClickedPos().getX()
				: context.getClickLocation().z - context.getClickedPos().getZ();
		if ((facing.getAxisDirection() == Direction.AxisDirection.POSITIVE) != (horizontalClick > .5))
			facing = facing.getOpposite();

		Direction clickedFace = context.getClickedFace();
		boolean top = clickedFace == Direction.DOWN
				|| clickedFace != Direction.UP
				&& context.getClickLocation().y - context.getClickedPos().getY() > .5;
		return state.setValue(FACING, facing).setValue(HALF, top ? Half.TOP : Half.BOTTOM);
	}

	@Override
	protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder.add(FACING, HALF));
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		AABB bounds = getBounds(state);
		VoxelShape primary = Block.box(bounds.minX * 16, bounds.minY * 16, bounds.minZ * 16,
				bounds.maxX * 16, bounds.maxY * 16, bounds.maxZ * 16);
		return DualPanelCopycatBehaviour.addSecondaryShape(state, level, pos, primary);
	}

	@Override
	public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
			BlockHitResult hit) {
		InteractionResult result = DualPanelCopycatBehaviour.use(this, state, level, pos, player, hand, hit);
		return result != null ? result : super.use(state, level, pos, player, hand, hit);
	}

	@Override
	public InteractionResult onWrenched(BlockState state, UseOnContext context) {
		InteractionResult result = DualPanelCopycatBehaviour.onWrenched(state, context);
		return result != null ? result : super.onWrenched(state, context);
	}

	@Override
	public InteractionResult onSneakWrenched(BlockState state, UseOnContext context) {
		InteractionResult result = DualPanelCopycatBehaviour.onSneakWrenched(state, context);
		return result != null ? result : super.onSneakWrenched(state, context);
	}

	public static AABB getBounds(BlockState state) {
		Direction facing = state.getValue(FACING);
		double minY = state.getValue(HALF) == Half.TOP ? 16 - HEIGHT : 0;

		if (facing.getAxis() == Axis.Z) {
			double minZ = facing == Direction.SOUTH ? 16 - THICKNESS : 0;
			return new AABB(0, minY / 16, minZ / 16, 1, (minY + HEIGHT) / 16,
					(minZ + THICKNESS) / 16);
		}

		double minX = facing == Direction.EAST ? 16 - THICKNESS : 0;
		return new AABB(minX / 16, minY / 16, 0, (minX + THICKNESS) / 16,
				(minY + HEIGHT) / 16, 1);
	}

	@Override
	protected AABB getShapeBounds(BlockState state) {
		return getBounds(state);
	}

	@Override
	protected boolean connectsAlong(BlockState state, BlockPos offset) {
		Axis longAxis = state.getValue(FACING).getAxis() == Axis.X ? Axis.Z : Axis.X;
		return longAxis.choose(Math.abs(offset.getX()), Math.abs(offset.getY()), Math.abs(offset.getZ())) == 1
				&& (longAxis == Axis.X ? offset.getY() == 0 && offset.getZ() == 0
						: offset.getX() == 0 && offset.getY() == 0);
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
