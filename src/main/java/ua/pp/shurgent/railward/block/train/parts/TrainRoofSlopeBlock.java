package ua.pp.shurgent.railward.block.train.parts;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.utility.VoxelShaper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.StairsShape;
import net.minecraft.world.level.material.MaterialColor;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class TrainRoofSlopeBlock extends HorizontalDirectionalBlock implements IWrenchable {

	public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
	public static final EnumProperty<StairsShape> SHAPE = BlockStateProperties.STAIRS_SHAPE;
	public static final EnumProperty<Direction.Axis> AXIS = TrainRoofBlock.AXIS;

	private static final VoxelShape STRAIGHT_NORTH = straightNorth();
	private static final VoxelShape OUTER_LEFT_NORTH = outerLeftNorth();
	private static final VoxelShape INNER_LEFT_NORTH = innerLeftNorth();
	private static final VoxelShaper STRAIGHT = VoxelShaper.forDirectional(STRAIGHT_NORTH, Direction.NORTH);
	private static final VoxelShaper OUTER_LEFT = VoxelShaper.forDirectional(OUTER_LEFT_NORTH, Direction.NORTH);
	private static final VoxelShaper INNER_LEFT = VoxelShaper.forDirectional(INNER_LEFT_NORTH, Direction.NORTH);

	public TrainRoofSlopeBlock(BlockBehaviour.Properties properties) {
		super(properties.strength(1.0f, 6f)
			.sound(SoundType.METAL)
			.color(MaterialColor.COLOR_LIGHT_BLUE));
		registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH)
			.setValue(SHAPE, StairsShape.STRAIGHT)
			.setValue(AXIS, Direction.Axis.X));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, SHAPE, AXIS);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockState state = defaultBlockState()
			.setValue(FACING, context.getHorizontalDirection())
			.setValue(AXIS, TrainRoofBlock.getPlacementAxis(context));
		return state.setValue(SHAPE, getRoofShape(state, context.getLevel(), context.getClickedPos()));
	}

	@Override
	public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level,
			BlockPos pos, BlockPos neighborPos) {
		return state.setValue(SHAPE, getRoofShape(state, level, pos));
	}

	@Override
	public InteractionResult onWrenched(BlockState state, UseOnContext context) {
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();

		if (level.isClientSide)
			return InteractionResult.SUCCESS;

		level.setBlock(pos, state.setValue(AXIS, TrainRoofBlock.nextAxis(state.getValue(AXIS))), 3);
		playRotateSound(level, pos);
		return InteractionResult.SUCCESS;
	}

	@Override
	public RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return shapeFor(state);
	}

	@Override
	public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return shapeFor(state);
	}

	@Override
	public VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
		return shapeFor(state);
	}

	@Override
	public boolean useShapeForLightOcclusion(BlockState state) {
		return true;
	}

	private static VoxelShape shapeFor(BlockState state) {
		Direction facing = state.getValue(FACING);
		return switch (state.getValue(SHAPE)) {
		case OUTER_LEFT -> OUTER_LEFT.get(facing);
		case OUTER_RIGHT -> OUTER_LEFT.get(facing.getClockWise());
		case INNER_LEFT -> INNER_LEFT.get(facing);
		case INNER_RIGHT -> INNER_LEFT.get(facing.getClockWise());
		default -> STRAIGHT.get(facing);
		};
	}

	private static StairsShape getRoofShape(BlockState state, BlockGetter level, BlockPos pos) {
		Direction facing = state.getValue(FACING);
		BlockState front = level.getBlockState(pos.relative(facing));

		if (isRoofSlope(front)) {
			Direction frontFacing = front.getValue(FACING);
			if (frontFacing.getAxis() != facing.getAxis() && canTakeShape(state, level, pos, frontFacing.getOpposite()))
				return frontFacing == facing.getCounterClockWise() ? StairsShape.OUTER_LEFT : StairsShape.OUTER_RIGHT;
		}

		BlockState back = level.getBlockState(pos.relative(facing.getOpposite()));
		if (isRoofSlope(back)) {
			Direction backFacing = back.getValue(FACING);
			if (backFacing.getAxis() != facing.getAxis() && canTakeShape(state, level, pos, backFacing))
				return backFacing == facing.getCounterClockWise() ? StairsShape.INNER_LEFT : StairsShape.INNER_RIGHT;
		}

		return StairsShape.STRAIGHT;
	}

	private static boolean canTakeShape(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
		BlockState adjacent = level.getBlockState(pos.relative(direction));
		return !isRoofSlope(adjacent) || adjacent.getValue(FACING) != state.getValue(FACING);
	}

	private static boolean isRoofSlope(BlockState state) {
		return state.getBlock() instanceof TrainRoofSlopeBlock;
	}

	private static VoxelShape straightNorth() {
		VoxelShape shape = Shapes.empty();
		for (int z = 0; z < 4; z++) {
			double z1 = z * 4;
			double z2 = z1 + 4;
			double top = 16 - z * (8d / 3d);
			shape = Shapes.or(shape, Block.box(0, top - 8, z1, 16, top, z2));
		}
		return shape;
	}

	private static VoxelShape outerLeftNorth() {
		VoxelShape shape = Shapes.empty();
		for (int x = 0; x < 4; x++) {
			for (int z = 0; z < 4; z++) {
				double top = 16 - Math.max(x, z) * (8d / 3d);
				shape = Shapes.or(shape, Block.box(x * 4, top - 8, z * 4, x * 4 + 4, top, z * 4 + 4));
			}
		}
		return shape;
	}

	private static VoxelShape innerLeftNorth() {
		VoxelShape shape = Shapes.empty();
		for (int x = 0; x < 4; x++) {
			for (int z = 0; z < 4; z++) {
				double top = 16 - (x * z) * (8d / 9d);
				shape = Shapes.or(shape, Block.box(x * 4, top - 8, z * 4, x * 4 + 4, top, z * 4 + 4));
			}
		}
		return shape;
	}
}
