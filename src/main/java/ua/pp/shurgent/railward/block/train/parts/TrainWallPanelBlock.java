package ua.pp.shurgent.railward.block.train.parts;

import java.util.List;
import java.util.function.Predicate;

import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.placement.IPlacementHelper;
import com.simibubi.create.foundation.placement.PlacementHelpers;
import com.simibubi.create.foundation.placement.PlacementOffset;
import com.simibubi.create.foundation.utility.VoxelShaper;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
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
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.MaterialColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import ua.pp.shurgent.railward.AllBlockEntities;
import ua.pp.shurgent.railward.AllBlocks;
import ua.pp.shurgent.railward.block.train.parts.panel.LocomotiveBufferLightsAttachment;
import ua.pp.shurgent.railward.block.train.parts.panel.LocomotiveSpotlightAttachment;
import ua.pp.shurgent.railward.block.train.parts.panel.TrainPanelAttachment;
import ua.pp.shurgent.railward.block.train.parts.panel.DualPanelBlockItem;
import ua.pp.shurgent.railward.block.train.parts.panel.DualPanelPlacement;

public class TrainWallPanelBlock extends HorizontalDirectionalBlock
	implements IBE<TrainWallPanelBlockEntity>, IWrenchable {

	public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

	private static final int placementHelperId = PlacementHelpers.register(new PlacementHelper());

	private static final VoxelShape SHAPE_NORTH = Shapes.or(Block.box(0, 0, 0, 16, 16, 3));

	// Build a directional shaper so you don’t manually rotate each AABB
	private static final VoxelShaper SHAPE = VoxelShaper.forDirectional(SHAPE_NORTH, Direction.NORTH);

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter w, BlockPos pos, CollisionContext ctx) {
		VoxelShape shape = SHAPE.get(state.getValue(FACING));
		if (w.getBlockEntity(pos) instanceof TrainWallPanelBlockEntity panel) {
			BlockState secondary = panel.getSecondaryPanel();
			if (secondary != null)
				shape = Shapes.or(shape, Shapes.create(getSecondaryBounds(state, secondary.getValue(FACING))));
			TrainPanelAttachment attachment = panel.getAttachment();
			if (attachment != null)
				shape = Shapes.or(shape, attachment.getInteractionShape(state));
		}
		return shape;
	}

	@Override
	public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
		CollisionContext context) {
		VoxelShape shape = SHAPE.get(state.getValue(FACING));
		if (level.getBlockEntity(pos) instanceof TrainWallPanelBlockEntity panel
				&& panel.getSecondaryPanel() != null)
			shape = Shapes.or(shape,
					Shapes.create(getSecondaryBounds(state, panel.getSecondaryPanel().getValue(FACING))));
		return shape;
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
			if (heldItem.getItem() instanceof DualPanelBlockItem panelItem
					&& DualPanelPlacement.find(new UseOnContext(player, hand, ray), panelItem.getBlock()) != null)
				return InteractionResult.PASS;
			if (AllBlocks.LOCOMOTIVE_BUFFER_LIGHTS.isIn(heldItem))
				return tryAttachLocomotiveBufferLights(state, world, pos, player, hand);
			if (AllBlocks.LOCOMOTIVE_SPOTLIGHT.isIn(heldItem))
				return tryAttachLocomotiveSpotlight(state, world, pos, player, hand);

			IPlacementHelper placementHelper = PlacementHelpers.get(placementHelperId);
			if (placementHelper.matchesItem(heldItem)) {
				placementHelper.getOffset(player, world, state, pos, ray).placeInWorld(world,
						(BlockItem) heldItem.getItem(), player, hand, ray);
				return InteractionResult.SUCCESS;
			}
		}

		return super.use(state, world, pos, player, hand, ray);
	}

	private InteractionResult tryAttachLocomotiveBufferLights(BlockState state, Level level, BlockPos pos,
		Player player, InteractionHand hand) {
		if (!AllBlocks.isAttachableTrainWallPanelState(state))
			return InteractionResult.PASS;
		if (!(level.getBlockEntity(pos) instanceof TrainWallPanelBlockEntity panel))
			return InteractionResult.PASS;
		if (panel.hasAttachment())
			return InteractionResult.PASS;

		if (level.isClientSide)
			return InteractionResult.SUCCESS;

		ItemStack heldItem = player.getItemInHand(hand);
		panel.setAttachment(new LocomotiveBufferLightsAttachment(getLocomotiveBufferLightsVariant(heldItem)));
		if (!player.isCreative())
			heldItem.shrink(1);
		return InteractionResult.SUCCESS;
	}

	private InteractionResult tryAttachLocomotiveSpotlight(BlockState state, Level level, BlockPos pos,
		Player player, InteractionHand hand) {
		if (!AllBlocks.isAttachableTrainWallPanelState(state))
			return InteractionResult.PASS;
		if (!(level.getBlockEntity(pos) instanceof TrainWallPanelBlockEntity panel))
			return InteractionResult.PASS;
		if (panel.hasAttachment())
			return InteractionResult.PASS;

		if (level.isClientSide)
			return InteractionResult.SUCCESS;

		ItemStack heldItem = player.getItemInHand(hand);
		panel.setAttachment(new LocomotiveSpotlightAttachment(getLocomotiveSpotlightVariant(heldItem)));
		if (!player.isCreative())
			heldItem.shrink(1);
		return InteractionResult.SUCCESS;
	}

	private static LocomotiveBufferLightsVariant getLocomotiveBufferLightsVariant(ItemStack stack) {
		CompoundTag blockStateTag = stack.getTagElement("BlockStateTag");
		if (blockStateTag == null)
			return LocomotiveBufferLightsVariant.TYPE_1;
		return LocomotiveBufferLightsVariant.byName(blockStateTag.getString(LocomotiveBufferLightsBlock.VARIANT
			.getName()));
	}

	private static LocomotiveSpotlightVariant getLocomotiveSpotlightVariant(ItemStack stack) {
		CompoundTag blockStateTag = stack.getTagElement("BlockStateTag");
		if (blockStateTag == null)
			return LocomotiveSpotlightVariant.SMALL;
		return LocomotiveSpotlightVariant.byName(blockStateTag.getString(LocomotiveSpotlightBlock.VARIANT.getName()));
	}

	@Override
	public InteractionResult onWrenched(BlockState state, UseOnContext context) {
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();

		if (!(level.getBlockEntity(pos) instanceof TrainWallPanelBlockEntity panel))
			return InteractionResult.FAIL;
		TrainPanelAttachment attachment = panel.getAttachment();
		if (attachment == null || !isAttachmentHit(state, pos, context.getClickLocation(), attachment))
			return InteractionResult.FAIL;

		if (level.isClientSide)
			return InteractionResult.SUCCESS;

		if (attachment instanceof LocomotiveBufferLightsAttachment bufferLights) {
			bufferLights.cycleVariant();
			panel.attachmentChanged();
			playRotateSound(level, pos);
			return InteractionResult.SUCCESS;
		}
		if (attachment instanceof LocomotiveSpotlightAttachment spotlight) {
			spotlight.cycleVariant();
			panel.attachmentChanged();
			playRotateSound(level, pos);
			return InteractionResult.SUCCESS;
		}

		return InteractionResult.FAIL;
	}

	@Override
	public InteractionResult onSneakWrenched(BlockState state, UseOnContext context) {
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();
		if (level.getBlockEntity(pos) instanceof TrainWallPanelBlockEntity panel
				&& isSecondaryPanelHit(state, pos, context.getClickLocation(), panel)) {
			if (level.isClientSide)
				return InteractionResult.SUCCESS;
			ItemStack drop = panel.removeSecondaryPanel();
			Player player = context.getPlayer();
			if (!drop.isEmpty() && player != null && !player.isCreative())
				player.getInventory().placeItemBackInInventory(drop);
			playRemoveSound(level, pos);
			return InteractionResult.SUCCESS;
		}

		if (!(level.getBlockEntity(pos) instanceof TrainWallPanelBlockEntity panel) || !panel.hasAttachment())
			return IWrenchable.super.onSneakWrenched(state, context);

		if (level.isClientSide)
			return InteractionResult.SUCCESS;

		ItemStack drop = panel.removeAttachment();
		Player player = context.getPlayer();
		if (!drop.isEmpty() && player != null && !player.isCreative())
			player.getInventory()
				.placeItemBackInInventory(drop);
		playRemoveSound(level, pos);
		return InteractionResult.SUCCESS;
	}

	public static boolean isAttachmentHit(BlockState state, BlockPos pos, Vec3 hit, TrainPanelAttachment attachment) {
		VoxelShape shape = attachment.getInteractionShape(state);
		if (shape.isEmpty())
			return false;
		return shape.toAabbs()
			.stream()
			.anyMatch(box -> box.inflate(1.0E-5D)
				.move(pos)
				.contains(hit));
	}

	public static boolean isSecondaryPanelHit(BlockState state, BlockPos pos, Vec3 hit,
			TrainWallPanelBlockEntity panel) {
		BlockState secondary = panel.getSecondaryPanel();
		if (secondary == null)
			return false;
		Vec3 local = hit.subtract(pos.getX(), pos.getY(), pos.getZ());
		AABB secondaryBounds = getSecondaryBounds(state, secondary.getValue(FACING)).inflate(1.0E-5D);
		AABB primaryBounds = getPanelBounds(state.getValue(FACING)).inflate(1.0E-5D);
		return secondaryBounds.contains(local) && !primaryBounds.contains(local);
	}

	public static AABB getPanelBounds(Direction facing) {
		return SHAPE.get(facing).bounds();
	}

	public static AABB getSecondaryBounds(BlockState primaryState, Direction secondaryFacing) {
		AABB primary = getPanelBounds(primaryState.getValue(FACING));
		AABB secondary = getPanelBounds(secondaryFacing);
		Direction.Axis axis = primaryState.getValue(FACING).getAxis();
		boolean primaryAtLowSide = axis.choose(primary.minX, primary.minY, primary.minZ) < .25;
		double joint = primaryAtLowSide
				? axis.choose(primary.maxX, primary.maxY, primary.maxZ)
				: axis.choose(primary.minX, primary.minY, primary.minZ);
		return switch (axis) {
			case X -> new AABB(primaryAtLowSide ? joint : secondary.minX, secondary.minY, secondary.minZ,
					primaryAtLowSide ? secondary.maxX : joint, secondary.maxY, secondary.maxZ);
			case Y -> secondary;
			case Z -> new AABB(secondary.minX, secondary.minY, primaryAtLowSide ? joint : secondary.minZ,
					secondary.maxX, secondary.maxY, primaryAtLowSide ? secondary.maxZ : joint);
		};
	}

	@Override
	public void playRotateSound(Level level, BlockPos pos) {
		float pitch = level.getRandom().nextFloat() + 0.5f;
		AllSoundEvents.WRENCH_ROTATE.playOnServer(level, pos, 1f, pitch);
	}

	public static boolean isTrainWallPanelItem(ItemStack stack) {

		return AllBlocks.TRAIN_WALL_PANEL_BLUE.isIn(stack)
				|| AllBlocks.TRAIN_WALL_PANEL_BLUE_STRIPE_YELLOW.isIn(stack)
				|| AllBlocks.TRAIN_WALL_PANEL_BLUE_STRIPE_RED.isIn(stack)
				|| AllBlocks.TRAIN_WALL_PANEL_BLUE_WINDOW_SMALL.isIn(stack)
				|| AllBlocks.TRAIN_WALL_PANEL_BLUE_WINDOW_BIG.isIn(stack)
				|| AllBlocks.TRAIN_WALL_PANEL_GREEN.isIn(stack)
				|| AllBlocks.TRAIN_WALL_PANEL_GREEN_STRIPE_YELLOW.isIn(stack)
				|| AllBlocks.TRAIN_WALL_PANEL_GREEN_STRIPE_RED.isIn(stack)
				|| AllBlocks.TRAIN_WALL_PANEL_GREEN_WINDOW_SMALL.isIn(stack)
				|| AllBlocks.TRAIN_WALL_PANEL_GREEN_WINDOW_BIG.isIn(stack)
				|| AllBlocks.TRAIN_WALL_PANEL_LIGHT_BLUE.isIn(stack)
				|| AllBlocks.TRAIN_WALL_PANEL_LIGHT_BLUE_STRIPE_YELLOW.isIn(stack)
				|| AllBlocks.TRAIN_WALL_PANEL_LIGHT_BLUE_STRIPE_RED.isIn(stack)
				|| AllBlocks.TRAIN_WALL_PANEL_LIGHT_BLUE_WINDOW_SMALL.isIn(stack)
				|| AllBlocks.TRAIN_WALL_PANEL_LIGHT_BLUE_WINDOW_BIG.isIn(stack)
				|| AllBlocks.isTrainWallPanelVariantItem(stack);
	}

	public static boolean isTrainWallPanelState(BlockState state) {

		return AllBlocks.TRAIN_WALL_PANEL_BLUE.has(state)
				|| AllBlocks.TRAIN_WALL_PANEL_BLUE_STRIPE_YELLOW.has(state)
				|| AllBlocks.TRAIN_WALL_PANEL_BLUE_STRIPE_RED.has(state)
				|| AllBlocks.TRAIN_WALL_PANEL_BLUE_WINDOW_SMALL.has(state)
				|| AllBlocks.TRAIN_WALL_PANEL_BLUE_WINDOW_BIG.has(state)
				|| AllBlocks.TRAIN_WALL_PANEL_GREEN.has(state)
				|| AllBlocks.TRAIN_WALL_PANEL_GREEN_STRIPE_YELLOW.has(state)
				|| AllBlocks.TRAIN_WALL_PANEL_GREEN_STRIPE_RED.has(state)
				|| AllBlocks.TRAIN_WALL_PANEL_GREEN_WINDOW_SMALL.has(state)
				|| AllBlocks.TRAIN_WALL_PANEL_GREEN_WINDOW_BIG.has(state)
				|| AllBlocks.TRAIN_WALL_PANEL_LIGHT_BLUE.has(state)
				|| AllBlocks.TRAIN_WALL_PANEL_LIGHT_BLUE_STRIPE_YELLOW.has(state)
				|| AllBlocks.TRAIN_WALL_PANEL_LIGHT_BLUE_STRIPE_RED.has(state)
				|| AllBlocks.TRAIN_WALL_PANEL_LIGHT_BLUE_WINDOW_SMALL.has(state)
				|| AllBlocks.TRAIN_WALL_PANEL_LIGHT_BLUE_WINDOW_BIG.has(state)
				|| AllBlocks.isTrainWallPanelVariantState(state);
	}

	@Override
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
		if (!level.isClientSide && !isMoving && !state.is(newState.getBlock())
			&& level.getBlockEntity(pos) instanceof TrainWallPanelBlockEntity panel) {
			panel.dropAttachment(level, pos);
			panel.dropSecondaryPanel(level, pos);
		}
		IBE.onRemove(state, level, pos, newState);
	}

	@Override
	public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
		if (player.isCreative() && level.getBlockEntity(pos) instanceof TrainWallPanelBlockEntity panel)
			panel.setSecondaryPanel(null);
		super.playerWillDestroy(level, pos, state, player);
	}

	@Override
	public Class<TrainWallPanelBlockEntity> getBlockEntityClass() {
		return TrainWallPanelBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends TrainWallPanelBlockEntity> getBlockEntityType() {
		return AllBlockEntities.TRAIN_WALL_PANEL.get();
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

		@Override
		public PlacementOffset getOffset(Player player, Level world, BlockState state, BlockPos pos,
				BlockHitResult ray, ItemStack heldItem) {
			if (heldItem.getItem() instanceof DualPanelBlockItem panelItem
					&& DualPanelPlacement.find(new UseOnContext(player, InteractionHand.MAIN_HAND, ray),
							panelItem.getBlock()) != null)
				return PlacementOffset.fail();
			return IPlacementHelper.super.getOffset(player, world, state, pos, ray, heldItem);
		}
	}

}
