package ua.pp.shurgent.railward.block.train.parts;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import ua.pp.shurgent.railward.compat.RailwardWrenchable;
import com.simibubi.create.content.fluids.transfer.GenericItemEmptying;
import com.simibubi.create.content.fluids.transfer.GenericItemFilling;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.blockEntity.ComparatorUtil;
import com.simibubi.create.foundation.fluid.FluidHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
//? if >=1.20.1
/*import net.minecraftforge.common.capabilities.ForgeCapabilities;*/
//? if <1.20.1
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import ua.pp.shurgent.railward.AllBlockEntities;

public class CisternBlock extends Block implements IBE<CisternBlockEntity>, RailwardWrenchable {

	public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
	private static final int COLLISION_STEPS = 4;
	private static final float COLLISION_STEP_SIZE = 1f / COLLISION_STEPS;
	private static final int DEFAULT_SIDES = 8;
	private static final int LARGE_SIDES = 16;
	private static final Map<CollisionShapeKey, VoxelShape> COLLISION_SHAPES = new ConcurrentHashMap<>();

	public CisternBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(AXIS, Direction.Axis.X));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(AXIS);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		if (context.getPlayer() == null || !context.getPlayer()
			.isShiftKeyDown()) {
			BlockState placedAgainst = context.getLevel()
				.getBlockState(context.getClickedPos()
					.relative(context.getClickedFace()
						.getOpposite()));
			if (placedAgainst.hasProperty(AXIS))
				return defaultBlockState().setValue(AXIS, placedAgainst.getValue(AXIS));
		}
		return defaultBlockState().setValue(AXIS, context.getHorizontalDirection()
			.getAxis());
	}

	@Override
	public RenderShape getRenderShape(BlockState state) {
		return RenderShape.INVISIBLE;
	}

	@Override
	public BlockState rotate(BlockState state, Rotation rotation) {
		if (rotation == Rotation.CLOCKWISE_90 || rotation == Rotation.COUNTERCLOCKWISE_90)
			return state.setValue(AXIS, state.getValue(AXIS) == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X);
		return state;
	}

	@Override
	public BlockState mirror(BlockState state, Mirror mirror) {
		return state;
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return Shapes.block();
	}

	@Override
	public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return getCisternCollisionShape(state, level, pos);
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

	private VoxelShape getCisternCollisionShape(BlockState state, BlockGetter level, BlockPos pos) {
		BlockEntity blockEntity = level.getBlockEntity(pos);
		if (!(blockEntity instanceof CisternBlockEntity cistern))
			return Shapes.block();

		CisternBlockEntity controller = cistern.getControllerBE();
		if (controller == null)
			return Shapes.block();

		int width = controller.getCisternWidth();
		int length = controller.getCisternLength();
		if (!CisternBlockEntity.isValidWidth(width) || length < 1)
			return Shapes.block();
		if (width == 1)
			return Shapes.block();

		Direction.Axis axis = state.hasProperty(AXIS) ? state.getValue(AXIS) : controller.getCisternAxis();
		BlockPos origin = controller.getBlockPos();
		int longIndex = axis == Direction.Axis.X ? pos.getX() - origin.getX() : pos.getZ() - origin.getZ();
		int crossIndex = axis == Direction.Axis.X ? pos.getZ() - origin.getZ() : pos.getX() - origin.getX();
		int verticalIndex = pos.getY() - origin.getY();
		if (longIndex < 0 || longIndex >= length || crossIndex < 0 || crossIndex >= width || verticalIndex < 0
			|| verticalIndex >= width)
			return Shapes.block();

		CollisionShapeKey key = new CollisionShapeKey(axis, width, length, longIndex, crossIndex, verticalIndex);
		return COLLISION_SHAPES.computeIfAbsent(key, CisternBlock::buildCollisionShape);
	}

	private static VoxelShape buildCollisionShape(CollisionShapeKey key) {
		VoxelShape shape = Shapes.empty();
		for (int longStep = 0; longStep < COLLISION_STEPS; longStep++)
			for (int crossStep = 0; crossStep < COLLISION_STEPS; crossStep++)
				for (int yStep = 0; yStep < COLLISION_STEPS; yStep++)
					if (isCollisionCellInside(key, longStep, crossStep, yStep))
						shape = Shapes.or(shape, collisionCell(key.axis, longStep, crossStep, yStep));
		return shape.isEmpty() ? Shapes.empty() : shape;
	}

	private static boolean isCollisionCellInside(CollisionShapeKey key, int longStep, int crossStep, int yStep) {
		float longPos = key.longIndex + (longStep + .5f) * COLLISION_STEP_SIZE;
		float cross = key.crossIndex + (crossStep + .5f) * COLLISION_STEP_SIZE;
		float y = key.verticalIndex + (yStep + .5f) * COLLISION_STEP_SIZE;
		float center = key.width / 2f;
		float offsetCross = cross - center;
		float offsetY = y - center;
		float apothem = collisionApothemAt(key.width, key.length, longPos);
		int sides = key.width == CisternBlockEntity.WIDTH_LARGE ? LARGE_SIDES : DEFAULT_SIDES;

		for (int side = 0; side < sides; side++) {
			float angle = side / (float) sides * (float) (Math.PI * 2);
			float projection = offsetCross * (float) Math.cos(angle) + offsetY * (float) Math.sin(angle);
			if (projection > apothem + 1.0E-4f)
				return false;
		}
		return true;
	}

	private static float collisionApothemAt(int width, int length, float longPos) {
		float bodyApothem = width / 2f;
		float capApothem = width == CisternBlockEntity.WIDTH_LARGE ? 1.5f : 1;
		float bevel = Math.min(width == CisternBlockEntity.WIDTH_SMALL ? .5f : .55f, length / 2f);
		if (length == 1) {
			if (longPos <= .5f)
				return lerp(capApothem, bodyApothem, longPos / .5f);
			return lerp(bodyApothem, capApothem, (longPos - .5f) / .5f);
		}
		if (longPos < bevel)
			return lerp(capApothem, bodyApothem, longPos / bevel);
		if (longPos > length - bevel)
			return lerp(bodyApothem, capApothem, (longPos - (length - bevel)) / bevel);
		return bodyApothem;
	}

	private static float lerp(float from, float to, float t) {
		return from + (to - from) * Math.max(0, Math.min(1, t));
	}

	private static VoxelShape collisionCell(Direction.Axis axis, int longStep, int crossStep, int yStep) {
		double longMin = longStep * 16d / COLLISION_STEPS;
		double longMax = (longStep + 1) * 16d / COLLISION_STEPS;
		double crossMin = crossStep * 16d / COLLISION_STEPS;
		double crossMax = (crossStep + 1) * 16d / COLLISION_STEPS;
		double yMin = yStep * 16d / COLLISION_STEPS;
		double yMax = (yStep + 1) * 16d / COLLISION_STEPS;
		return axis == Direction.Axis.X ? Block.box(longMin, yMin, crossMin, longMax, yMax, crossMax)
			: Block.box(crossMin, yMin, longMin, crossMax, yMax, longMax);
	}

	@Override
	public InteractionResult onWrenched(BlockState state, UseOnContext context) {
		return InteractionResult.FAIL;
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(level, pos, state, placer, stack);
		queueConnectivity(level, pos);
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
		queueConnectivity(level, pos);
	}

	@Override
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
		if (state.hasBlockEntity() && (state.getBlock() != newState.getBlock() || !newState.hasBlockEntity())) {
			BlockEntity blockEntity = level.getBlockEntity(pos);
			if (!level.isClientSide && !isMoving && blockEntity instanceof CisternBlockEntity cistern)
				CisternConnectivity.split(cistern, pos);
		}
		IBE.onRemove(state, level, pos, newState);
	}

	@Override
	public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
		BlockHitResult hit) {
		ItemStack heldItem = player.getItemInHand(hand);
		if (heldItem.isEmpty())
			return InteractionResult.PASS;

		InteractionResult placementResult = tryPlaceNextLayer(state, level, pos, player, hand, heldItem, hit);
		if (placementResult != InteractionResult.PASS)
			return placementResult;

		CisternBlockEntity cistern = getBlockEntity(level, pos);
		if (cistern == null)
			return InteractionResult.FAIL;

		LazyOptional<IFluidHandler> tankCapability =
			cistern.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY);
		if (!tankCapability.isPresent())
			return InteractionResult.PASS;

		FluidStack previousFluid = tankCapability.map(handler -> handler.getFluidInTank(0)
			.copy())
			.orElse(FluidStack.EMPTY);

		boolean exchanged = FluidHelper.tryEmptyItemIntoBE(level, player, hand, heldItem, cistern)
			|| FluidHelper.tryFillItemFromBE(level, player, hand, heldItem, cistern);

		if (!exchanged) {
			if (GenericItemEmptying.canItemBeEmptied(level, heldItem)
				|| GenericItemFilling.canItemBeFilled(level, heldItem))
				return InteractionResult.SUCCESS;
			return InteractionResult.PASS;
		}

		if (!level.isClientSide) {
			CisternBlockEntity controller = cistern.getControllerBE();
			if (controller != null) {
				FluidStack currentFluid = tankCapability.map(handler -> handler.getFluidInTank(0))
					.orElse(FluidStack.EMPTY);
				if (!currentFluid.isFluidStackIdentical(previousFluid)) {
					controller.sendDataImmediately();
					controller.setChanged();
				}
			}
		}

		return InteractionResult.SUCCESS;
	}

	private InteractionResult tryPlaceNextLayer(BlockState state, Level level, BlockPos clickedPos, Player player,
		InteractionHand hand, ItemStack heldItem, BlockHitResult hit) {
		if (!(heldItem.getItem() instanceof BlockItem blockItem) || blockItem.getBlock() != this)
			return InteractionResult.PASS;
		if (!player.mayBuild())
			return InteractionResult.PASS;

		Direction side = hit.getDirection();
		Direction.Axis axis = state.getValue(AXIS);
		if (side.getAxis() != axis)
			return InteractionResult.PASS;

		CisternBlockEntity clickedBE = getBlockEntity(level, clickedPos);
		if (clickedBE == null)
			return InteractionResult.PASS;
		CisternBlockEntity controller = clickedBE.getControllerBE();
		if (controller == null)
			return InteractionResult.PASS;

		int width = controller.getCisternWidth();
		int length = controller.getCisternLength();
		if (!CisternBlockEntity.isValidWidth(width) || length >= CisternBlockEntity.MAX_LENGTH)
			return InteractionResult.PASS;

		BlockPos origin = controller.getBlockPos();
		int clickedLongIndex = axis == Direction.Axis.X ? clickedPos.getX() - origin.getX()
			: clickedPos.getZ() - origin.getZ();
		boolean negativeEnd = side.getAxisDirection() == Direction.AxisDirection.NEGATIVE && clickedLongIndex == 0;
		boolean positiveEnd =
			side.getAxisDirection() == Direction.AxisDirection.POSITIVE && clickedLongIndex == length - 1;
		if (!negativeEnd && !positiveEnd)
			return InteractionResult.PASS;

		int required = width * width;
		if (!player.getAbilities().instabuild && heldItem.getCount() < required)
			return InteractionResult.FAIL;

		BlockPos layerOrigin = getLayerOrigin(origin, axis, length, negativeEnd);
		for (int y = 0; y < width; y++)
			for (int cross = 0; cross < width; cross++) {
				BlockPos target = getLayerPos(layerOrigin, axis, y, cross);
				if (!level.getBlockState(target).getMaterial().isReplaceable())
					return InteractionResult.FAIL;
			}

		if (level.isClientSide)
			return InteractionResult.SUCCESS;

		BlockState placedState = defaultBlockState().setValue(AXIS, axis);
		for (int y = 0; y < width; y++)
			for (int cross = 0; cross < width; cross++)
				level.setBlock(getLayerPos(layerOrigin, axis, y, cross), placedState, 3);

		if (!player.getAbilities().instabuild)
			heldItem.shrink(required);

		CisternConnectivity.formFrom(controller);
		return InteractionResult.SUCCESS;
	}

	private BlockPos getLayerOrigin(BlockPos origin, Direction.Axis axis, int length, boolean negativeEnd) {
		if (axis == Direction.Axis.X)
			return negativeEnd ? origin.west() : origin.offset(length, 0, 0);
		return negativeEnd ? origin.north() : origin.offset(0, 0, length);
	}

	private BlockPos getLayerPos(BlockPos layerOrigin, Direction.Axis axis, int y, int cross) {
		return axis == Direction.Axis.X ? layerOrigin.offset(0, y, cross) : layerOrigin.offset(cross, y, 0);
	}

	@Override
	public boolean hasAnalogOutputSignal(BlockState state) {
		return true;
	}

	@Override
	public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
		CisternBlockEntity cistern = getBlockEntity(level, pos);
		if (cistern == null)
			return 0;
		CisternBlockEntity controller = cistern.getControllerBE();
		return controller == null ? 0 : ComparatorUtil.fractionToRedstoneLevel(controller.getFillState());
	}

	@Override
	public Class<CisternBlockEntity> getBlockEntityClass() {
		return CisternBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends CisternBlockEntity> getBlockEntityType() {
		return AllBlockEntities.CISTERN.get();
	}

	private static void queueConnectivity(Level level, BlockPos pos) {
		if (level.isClientSide)
			return;
		if (level.getBlockEntity(pos) instanceof CisternBlockEntity be)
			be.queueConnectivityUpdate();
	}

	private record CollisionShapeKey(Direction.Axis axis, int width, int length, int longIndex, int crossIndex,
		int verticalIndex) {
	}

}
