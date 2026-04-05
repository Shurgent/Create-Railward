package ua.pp.shurgent.railward.block.train.parts;

import java.util.List;
import java.util.function.Predicate;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.placement.IPlacementHelper;
import com.simibubi.create.foundation.placement.PlacementHelpers;
import com.simibubi.create.foundation.placement.PlacementOffset;
import com.simibubi.create.foundation.utility.VoxelShaper;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.MaterialColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import ua.pp.shurgent.railward.AllBlocks;

public class TrainWallPanelBlock extends HorizontalDirectionalBlock implements IWrenchable {

	public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

	private static final int placementHelperId = PlacementHelpers.register(new PlacementHelper());

	private static final VoxelShape SHAPE_NORTH = Shapes.or(Block.box(0, 0, 0, 16, 16, 3));

	// Build a directional shaper so you don’t manually rotate each AABB
	private static final VoxelShaper SHAPE = VoxelShaper.forDirectional(SHAPE_NORTH, Direction.NORTH);

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter w, BlockPos pos, CollisionContext ctx) {
		return SHAPE.get(state.getValue(FACING));
	}

	public TrainWallPanelBlock(BlockBehaviour.Properties properties) {
		this(properties, MaterialColor.COLOR_BLUE);
	}

	public TrainWallPanelBlock(BlockBehaviour.Properties properties, MaterialColor color) {
        super(properties.strength(1.0f, 6f).sound(SoundType.METAL).color(color).noOcclusion());
		registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH));
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext ctx) {
		return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection());
	}

	@Override
	public RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	@Override
	protected void createBlockStateDefinition(Builder<Block, BlockState> pBuilder) {
		super.createBlockStateDefinition(pBuilder.add(FACING));
	}

	@SuppressWarnings("deprecation")
	@Override
	public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand,
			BlockHitResult ray) {

		if (!player.isShiftKeyDown() && player.mayBuild()) {
			ItemStack heldItem = player.getItemInHand(hand);
			IPlacementHelper placementHelper = PlacementHelpers.get(placementHelperId);
			if (placementHelper.matchesItem(heldItem)) {
				placementHelper.getOffset(player, world, state, pos, ray).placeInWorld(world,
						(BlockItem) heldItem.getItem(), player, hand, ray);
				return InteractionResult.SUCCESS;
			}
		}

		return super.use(state, world, pos, player, hand, ray);
	}

	public static boolean isTrainWallPanelItem(ItemStack stack) {

		return AllBlocks.TRAIN_WALL_PANEL_BLUE.isIn(stack)
				|| AllBlocks.TRAIN_WALL_PANEL_BLUE_STRIPE_YELLOW.isIn(stack)
				|| AllBlocks.TRAIN_WALL_PANEL_BLUE_STRIPE_RED.isIn(stack)
				|| AllBlocks.TRAIN_WALL_PANEL_BLUE_WINDOW_SMALL.isIn(stack)
				|| AllBlocks.TRAIN_WALL_PANEL_GREEN.isIn(stack)
				|| AllBlocks.TRAIN_WALL_PANEL_GREEN_STRIPE_YELLOW.isIn(stack)
				|| AllBlocks.TRAIN_WALL_PANEL_GREEN_STRIPE_RED.isIn(stack)
				|| AllBlocks.TRAIN_WALL_PANEL_GREEN_WINDOW_SMALL.isIn(stack)
				|| AllBlocks.TRAIN_WALL_PANEL_LIGHT_BLUE.isIn(stack)
				|| AllBlocks.TRAIN_WALL_PANEL_LIGHT_BLUE_STRIPE_YELLOW.isIn(stack)
				|| AllBlocks.TRAIN_WALL_PANEL_LIGHT_BLUE_STRIPE_RED.isIn(stack)
				|| AllBlocks.TRAIN_WALL_PANEL_LIGHT_BLUE_WINDOW_SMALL.isIn(stack);
	}

	public static boolean isTrainWallPanelState(BlockState state) {

		return AllBlocks.TRAIN_WALL_PANEL_BLUE.has(state)
				|| AllBlocks.TRAIN_WALL_PANEL_BLUE_STRIPE_YELLOW.has(state)
				|| AllBlocks.TRAIN_WALL_PANEL_BLUE_STRIPE_RED.has(state)
				|| AllBlocks.TRAIN_WALL_PANEL_BLUE_WINDOW_SMALL.has(state)
				|| AllBlocks.TRAIN_WALL_PANEL_GREEN.has(state)
				|| AllBlocks.TRAIN_WALL_PANEL_GREEN_STRIPE_YELLOW.has(state)
				|| AllBlocks.TRAIN_WALL_PANEL_GREEN_STRIPE_RED.has(state)
				|| AllBlocks.TRAIN_WALL_PANEL_GREEN_WINDOW_SMALL.has(state)
				|| AllBlocks.TRAIN_WALL_PANEL_LIGHT_BLUE.has(state)
				|| AllBlocks.TRAIN_WALL_PANEL_LIGHT_BLUE_STRIPE_YELLOW.has(state)
				|| AllBlocks.TRAIN_WALL_PANEL_LIGHT_BLUE_STRIPE_RED.has(state)
				|| AllBlocks.TRAIN_WALL_PANEL_LIGHT_BLUE_WINDOW_SMALL.has(state);
	}

	@MethodsReturnNonnullByDefault
	private static class PlacementHelper implements IPlacementHelper {
		@Override
		public Predicate<ItemStack> getItemPredicate() {
			return TrainWallPanelBlock::isTrainWallPanelItem;
		}

		@Override
		public Predicate<BlockState> getStatePredicate() {
			return TrainWallPanelBlock::isTrainWallPanelState;
		}

		@Override
		public PlacementOffset getOffset(Player player, Level world, BlockState state, BlockPos pos,
				BlockHitResult ray) {
			List<Direction> directions = IPlacementHelper.orderedByDistanceExceptAxis(pos, ray.getLocation(),
					state.getValue(FACING).getAxis(),
					dir -> world.getBlockState(pos.relative(dir)).getMaterial().isReplaceable());

			if (directions.isEmpty())
				return PlacementOffset.fail();
			else {
				return PlacementOffset.success(pos.relative(directions.get(0)),
						s -> s.setValue(FACING, state.getValue(FACING)));
			}
		}
	}

}
