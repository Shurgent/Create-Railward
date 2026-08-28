package ua.pp.shurgent.railward.block.train.parts.copycat;

import java.util.List;
import java.util.function.Predicate;

import com.simibubi.create.content.contraptions.StructureTransform;
import com.simibubi.create.foundation.placement.IPlacementHelper;
import com.simibubi.create.foundation.placement.PlacementHelpers;
import com.simibubi.create.foundation.placement.PlacementOffset;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import ua.pp.shurgent.railward.block.train.parts.panel.DualPanelBlockItem;
import ua.pp.shurgent.railward.block.train.parts.panel.DualPanelPlacement;

public class CopycatWallPanelBlock extends WallPanelCopycatBlock {

	public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
	private static final double THICKNESS = 3;
	private static final int PLACEMENT_HELPER_ID = PlacementHelpers.register(new PlacementHelper());

	public CopycatWallPanelBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH));
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockState state = super.getStateForPlacement(context);
		return state == null ? null : state.setValue(FACING, context.getHorizontalDirection());
	}

	@Override
	protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder.add(FACING));
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

		if (!player.isShiftKeyDown() && player.mayBuild()) {
			ItemStack held = player.getItemInHand(hand);
			if (held.getItem() instanceof DualPanelBlockItem panelItem) {
				InteractionResult merged = DualPanelPlacement.tryPlace(new UseOnContext(player, hand, hit),
						panelItem.getBlock());
				if (merged != null)
					return merged;
			}
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
		if (facing.getAxis() == Axis.Z) {
			double minZ = facing == Direction.SOUTH ? 16 - THICKNESS : 0;
			return new AABB(0, 0, minZ / 16, 1, 1, (minZ + THICKNESS) / 16);
		}
		double minX = facing == Direction.EAST ? 16 - THICKNESS : 0;
		return new AABB(minX / 16, 0, 0, (minX + THICKNESS) / 16, 1, 1);
	}

	@Override
	protected AABB getShapeBounds(BlockState state) {
		return getBounds(state);
	}

	@Override
	protected boolean connectsAlong(BlockState state, BlockPos offset) {
		Axis longAxis = state.getValue(FACING).getAxis() == Axis.X ? Axis.Z : Axis.X;
		return longAxis.choose(Math.abs(offset.getX()), Math.abs(offset.getY()), Math.abs(offset.getZ())) == 1;
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

	@Override
	public BlockState transform(BlockState state, StructureTransform transform) {
		Direction facing = state.getValue(FACING);
		Vec3 normal = transform.applyWithoutOffsetUncentered(Vec3.atLowerCornerOf(facing.getNormal()));
		Direction transformedFacing = Direction.getNearest((float) normal.x, (float) normal.y, (float) normal.z);
		// The full wall panel must never be reclassified as one of the 3x3 slices.
		// Train/contraption transforms rotate it around Y, so its normal remains horizontal.
		return transformedFacing.getAxis().isHorizontal()
				? state.setValue(FACING, transformedFacing)
				: state;
	}

	private static class PlacementHelper implements IPlacementHelper {

		@Override
		public Predicate<ItemStack> getItemPredicate() {
			return stack -> stack.getItem() instanceof BlockItem item
					&& item.getBlock() instanceof CopycatWallPanelBlock;
		}

		@Override
		public Predicate<BlockState> getStatePredicate() {
			return state -> state.getBlock() instanceof CopycatWallPanelBlock;
		}

		@Override
		public PlacementOffset getOffset(Player player, Level level, BlockState state, BlockPos pos,
				BlockHitResult hit) {
			if (DualPanelPlacement.hasPlacement(player, hit))
				return PlacementOffset.fail();
			List<Direction> directions = IPlacementHelper.orderedByDistanceExceptAxis(pos, hit.getLocation(),
					state.getValue(FACING).getAxis(),
					direction -> level.getBlockState(pos.relative(direction)).getMaterial().isReplaceable());
			if (directions.isEmpty())
				return PlacementOffset.fail();
			return PlacementOffset.success(pos.relative(directions.get(0)),
					placed -> placed.setValue(FACING, state.getValue(FACING)));
		}
	}
}
