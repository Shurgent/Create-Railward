package ua.pp.shurgent.railward.block.train.parts;

import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.items.ItemHandlerHelper;
import ua.pp.shurgent.railward.AllBlockEntities;

public class BulkCargoContainerBlock extends Block implements IBE<BulkCargoContainerBlockEntity> {

	public static final BooleanProperty NORTH = BooleanProperty.create("north");
	public static final BooleanProperty SOUTH = BooleanProperty.create("south");
	public static final BooleanProperty EAST = BooleanProperty.create("east");
	public static final BooleanProperty WEST = BooleanProperty.create("west");
	public static final BooleanProperty BOTTOM = BooleanProperty.create("bottom");
	public static final BooleanProperty TOP = BooleanProperty.create("top");
	public static final BooleanProperty X_LONG = BooleanProperty.create("x_long");
	public static final IntegerProperty CARGO_LEVEL = IntegerProperty.create("cargo_level", 0, 16);

	private static final VoxelShape FLOOR = Block.box(0, 0, 0, 16, 2, 16);
	private static final VoxelShape NORTH_WALL = Block.box(0, 0, 0, 16, 16, 2);
	private static final VoxelShape SOUTH_WALL = Block.box(0, 0, 14, 16, 16, 16);
	private static final VoxelShape EAST_WALL = Block.box(14, 0, 0, 16, 16, 16);
	private static final VoxelShape WEST_WALL = Block.box(0, 0, 0, 2, 16, 16);

	public BulkCargoContainerBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(NORTH, true)
			.setValue(SOUTH, true)
			.setValue(EAST, true)
			.setValue(WEST, true)
			.setValue(BOTTOM, true)
			.setValue(TOP, true)
			.setValue(X_LONG, true)
			.setValue(CARGO_LEVEL, 0));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(NORTH, SOUTH, EAST, WEST, BOTTOM, TOP, X_LONG, CARGO_LEVEL);
	}

	@Override
	public RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return buildShape(state);
	}

	@Override
	public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return buildShape(state);
	}

	@Override
	public float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
		return 1.0F;
	}

	@Override
	public boolean isCollisionShapeFullBlock(BlockState state, BlockGetter level, BlockPos pos) {
		return false;
	}

	@Override
	public int getLightBlock(BlockState state, BlockGetter level, BlockPos pos) {
		return 0;
	}

	@Override
	public VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
		return buildShape(state);
	}

	@Override
	public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
		BlockHitResult hit) {
		if (hand == InteractionHand.OFF_HAND)
			return InteractionResult.PASS;

		ItemStack held = player.getItemInHand(hand);
		if (held.isEmpty()) {
			if (level.isClientSide)
				return InteractionResult.SUCCESS;
			return extractOneStack(level, pos, player) ? InteractionResult.SUCCESS : InteractionResult.PASS;
		}

		if (!isLayerPlacementAttempt(held, hit))
			return InteractionResult.PASS;

		if (level.isClientSide)
			return InteractionResult.SUCCESS;

		return placeNextLayer(level, pos, player, held) ? InteractionResult.SUCCESS : InteractionResult.FAIL;
	}

	@Override
	public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, float fallDistance) {
		super.fallOn(level, state, pos, entity, fallDistance);
		if (entity instanceof ItemEntity itemEntity)
			tryInsertItemEntity(level, pos, itemEntity);
	}

	@Override
	@SuppressWarnings("deprecation")
	public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
		super.entityInside(state, level, pos, entity);
		if (!(entity instanceof ItemEntity itemEntity))
			return;
		if (itemEntity.getY() < pos.getY() + .75)
			return;
		tryInsertItemEntity(level, pos, itemEntity);
	}

	@Override
	@SuppressWarnings("deprecation")
	public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
		super.onPlace(state, level, pos, oldState, isMoving);
		if (!level.isClientSide && state.getBlock() != oldState.getBlock())
			queueConnectivity(level, pos);
	}

	@Override
	@SuppressWarnings("deprecation")
	public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos,
		boolean isMoving) {
		super.neighborChanged(state, level, pos, block, fromPos, isMoving);
		if (!level.isClientSide)
			queueConnectivity(level, pos);
	}

	@Override
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
		if (state.getBlock() != newState.getBlock()) {
			if (!level.isClientSide && !isMoving && level.getBlockEntity(pos) instanceof BulkCargoContainerBlockEntity be)
				be.dropContents();
			if (!level.isClientSide && !isMoving)
				for (BlockPos neighbor : BlockPos.betweenClosed(pos.offset(-1, -1, -1), pos.offset(1, 1, 1)))
					if (!neighbor.equals(pos))
						queueConnectivity(level, neighbor);
		}
		IBE.onRemove(state, level, pos, newState);
	}

	@Override
	public Class<BulkCargoContainerBlockEntity> getBlockEntityClass() {
		return BulkCargoContainerBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends BulkCargoContainerBlockEntity> getBlockEntityType() {
		return AllBlockEntities.BULK_CARGO_CONTAINER.get();
	}

	private static VoxelShape buildShape(BlockState state) {
		VoxelShape shape = Shapes.empty();
		if (state.getValue(BOTTOM))
			shape = Shapes.or(shape, FLOOR);
		if (state.getValue(NORTH))
			shape = Shapes.or(shape, NORTH_WALL);
		if (state.getValue(SOUTH))
			shape = Shapes.or(shape, SOUTH_WALL);
		if (state.getValue(EAST))
			shape = Shapes.or(shape, EAST_WALL);
		if (state.getValue(WEST))
			shape = Shapes.or(shape, WEST_WALL);

		int cargoLevel = state.getValue(CARGO_LEVEL);
		if (cargoLevel > 0)
			shape = Shapes.or(shape, Block.box(0, 0, 0, 16, cargoLevel, 16));
		return shape;
	}

	private boolean isLayerPlacementAttempt(ItemStack stack, BlockHitResult hit) {
		if (hit.getDirection() != Direction.UP)
			return false;
		return stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() == this;
	}

	private boolean placeNextLayer(Level level, BlockPos clickedPos, Player player, ItemStack held) {
		if (!(level.getBlockEntity(clickedPos) instanceof BulkCargoContainerBlockEntity clickedBE))
			return false;
		BulkCargoContainerBlockEntity controllerBE = clickedBE.getControllerBE();
		if (controllerBE == null)
			return false;
		if (controllerBE.getHeight() >= BulkCargoContainerBlockEntity.MAX_HEIGHT)
			return false;

		BlockPos controllerPos = controllerBE.getBlockPos();
		int sizeX = controllerBE.getSizeX();
		int sizeZ = controllerBE.getSizeZ();
		int required = sizeX * sizeZ;
		if (!player.getAbilities().instabuild && held.getCount() < required)
			return false;

		int targetY = controllerPos.getY() + controllerBE.getHeight();
		for (int x = 0; x < sizeX; x++)
			for (int z = 0; z < sizeZ; z++) {
				BlockPos target = new BlockPos(controllerPos.getX() + x, targetY, controllerPos.getZ() + z);
				if (!level.getBlockState(target)
					.getMaterial()
					.isReplaceable())
					return false;
			}

		BlockState placedState = defaultBlockState().setValue(BOTTOM, false);
		for (int x = 0; x < sizeX; x++)
			for (int z = 0; z < sizeZ; z++) {
				BlockPos target = new BlockPos(controllerPos.getX() + x, targetY, controllerPos.getZ() + z);
				level.setBlock(target, placedState, 3);
			}

		if (!player.getAbilities().instabuild)
			held.shrink(required);
		queueConnectivity(level, controllerPos);
		return true;
	}

	private static boolean extractOneStack(Level level, BlockPos pos, Player player) {
		if (!(level.getBlockEntity(pos) instanceof BulkCargoContainerBlockEntity container))
			return false;

		ItemStack extracted = container.extractOneStack(false);
		if (extracted.isEmpty())
			return false;

		ItemHandlerHelper.giveItemToPlayer(player, extracted);
		return true;
	}

	private static void tryInsertItemEntity(Level level, BlockPos pos, ItemEntity itemEntity) {
		if (level.isClientSide || !itemEntity.isAlive())
			return;
		if (!(level.getBlockEntity(pos) instanceof BulkCargoContainerBlockEntity container))
			return;

		ItemStack stack = itemEntity.getItem();
		ItemStack remainder = container.tryInsert(stack, false);
		if (remainder.getCount() == stack.getCount())
			return;
		if (remainder.isEmpty())
			itemEntity.discard();
		else
			itemEntity.setItem(remainder);
	}

	private static void queueConnectivity(Level level, BlockPos pos) {
		if (!(level instanceof ServerLevel))
			return;
		if (level.getBlockEntity(pos) instanceof BulkCargoContainerBlockEntity be)
			be.queueConnectivityUpdate();
	}

}
