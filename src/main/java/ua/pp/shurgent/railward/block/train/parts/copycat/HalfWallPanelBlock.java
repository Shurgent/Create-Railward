package ua.pp.shurgent.railward.block.train.parts.copycat;

import java.util.function.Predicate;

import com.simibubi.create.foundation.placement.IPlacementHelper;
import com.simibubi.create.foundation.placement.PlacementHelpers;
import com.simibubi.create.foundation.placement.PlacementOffset;
import com.simibubi.create.foundation.placement.PoleHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
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
import ua.pp.shurgent.railward.block.train.parts.panel.DualPanelBlockItem;
import ua.pp.shurgent.railward.block.train.parts.panel.DualPanelPlacement;

public class HalfWallPanelBlock extends WallPanelCopycatBlock {

	public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
	public static final EnumProperty<Half> HALF = BlockStateProperties.HALF;
	private static final double HEIGHT = 8;
	private static final double THICKNESS = 3;
	private static final int PLACEMENT_HELPER_ID = PlacementHelpers.register(new PlacementHelper());

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
		if (result != null)
			return result;
		if (!player.isShiftKeyDown() && player.mayBuild()
				&& player.getItemInHand(hand).getItem() instanceof DualPanelBlockItem panelItem) {
			InteractionResult merged = DualPanelPlacement.tryPlace(new UseOnContext(player, hand, hit),
					panelItem.getBlock());
			if (merged != null)
				return merged;
		}
		if (!player.isShiftKeyDown() && player.mayBuild()) {
			ItemStack held = player.getItemInHand(hand);
			IPlacementHelper helper = PlacementHelpers.get(PLACEMENT_HELPER_ID);
			if (helper.matchesItem(held)) {
				PlacementOffset offset = helper.getOffset(player, level, state, pos, hit);
				if (offset.isSuccessful())
					return offset.placeInWorld(level, (BlockItem) held.getItem(), player, hand, hit);
			}
		}
		return super.use(state, level, pos, player, hand, hit);
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

	private static class PlacementHelper extends PoleHelper<Direction> {

		private PlacementHelper() {
			super(candidate -> candidate.getBlock() instanceof HalfWallPanelBlock,
					candidate -> candidate.getValue(FACING).getClockWise().getAxis(), FACING);
		}

		@Override
		public Predicate<ItemStack> getItemPredicate() {
			return stack -> stack.getItem() instanceof BlockItem item
					&& item.getBlock() instanceof HalfWallPanelBlock;
		}

		@Override
		public int attachedPoles(Level level, BlockPos pos, Direction direction) {
			BlockState origin = level.getBlockState(pos);
			BlockPos checkPos = pos.relative(direction);
			int count = 0;
			while (matchesRow(origin, level.getBlockState(checkPos))) {
				count++;
				checkPos = checkPos.relative(direction);
			}
			return count;
		}

		@Override
		public PlacementOffset getOffset(Player player, Level level, BlockState state, BlockPos pos,
				BlockHitResult hit) {
			if (DualPanelPlacement.hasPlacement(player, hit))
				return PlacementOffset.fail();
			PlacementOffset offset = super.getOffset(player, level, state, pos, hit);
			if (offset.isSuccessful())
				offset.withTransform(offset.getTransform()
						.andThen(placed -> placed.setValue(HALF, state.getValue(HALF))));
			return offset;
		}

		private static boolean matchesRow(BlockState origin, BlockState candidate) {
			return candidate.getBlock() instanceof HalfWallPanelBlock
					&& candidate.getValue(FACING) == origin.getValue(FACING)
					&& candidate.getValue(HALF) == origin.getValue(HALF);
		}
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
