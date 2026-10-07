package ua.pp.shurgent.railward.block.train.parts;

import ua.pp.shurgent.railward.compat.RailwardWrenchable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.MaterialColor;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class TrainRoofBlock extends Block implements RailwardWrenchable {

	public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
	private static final VoxelShape SHAPE = Block.box(0, 8, 0, 16, 16, 16);

	public TrainRoofBlock(BlockBehaviour.Properties properties) {
		super(properties.strength(1.0f, 6f)
			.sound(SoundType.METAL)
			.color(MaterialColor.COLOR_LIGHT_BLUE));
		registerDefaultState(TrainRoofPartitionBehaviour.withDefaults(
			defaultBlockState().setValue(AXIS, Direction.Axis.X)));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(AXIS);
		TrainRoofPartitionBehaviour.addProperties(builder);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockState state = defaultBlockState().setValue(AXIS, getPlacementAxis(context));
		return TrainRoofPartitionBehaviour.enablePartitionsFromBelow(state, context.getLevel(),
			context.getClickedPos());
	}

	@Override
	public InteractionResult onWrenched(BlockState state, UseOnContext context) {
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();
		Direction partition = TrainRoofPartitionBehaviour.resolveTarget(state, pos, context.getClickLocation(),
			context.getClickedFace());

		if (level.isClientSide)
			return InteractionResult.SUCCESS;

		BlockState updated = partition == null
			? state.setValue(AXIS, nextAxis(state.getValue(AXIS)))
			: TrainRoofPartitionBehaviour.togglePartition(state, partition);
		level.setBlock(pos, updated, 3);
		playRotateSound(level, pos);
		return InteractionResult.SUCCESS;
	}

	@Override
	public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level,
			BlockPos pos, BlockPos neighborPos) {
		return direction == Direction.DOWN
			? TrainRoofPartitionBehaviour.enablePartitionsFromBelow(state, level, pos)
			: state;
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return TrainRoofPartitionBehaviour.getSelectionShape(state, SHAPE, context);
	}

	@Override
	public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return TrainRoofPartitionBehaviour.addPartitions(state, SHAPE);
	}

	@Override
	public VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
		return TrainRoofPartitionBehaviour.addPartitions(state, SHAPE);
	}

	@Override
	public boolean useShapeForLightOcclusion(BlockState state) {
		return true;
	}

	static Direction.Axis getPlacementAxis(BlockPlaceContext context) {
		LevelAccessor level = context.getLevel();
		BlockPos pos = context.getClickedPos();

		for (Direction direction : Direction.Plane.HORIZONTAL) {
			BlockState neighbor = level.getBlockState(pos.relative(direction));
			if (hasRoofAxis(neighbor))
				return neighbor.getValue(AXIS);
		}

		for (Direction direction : Direction.values()) {
			BlockState neighbor = level.getBlockState(pos.relative(direction));
			if (hasRoofAxis(neighbor))
				return neighbor.getValue(AXIS);
		}

		return context.getHorizontalDirection().getAxis();
	}

	static Direction.Axis nextAxis(Direction.Axis axis) {
		return axis == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X;
	}

	static boolean isQuarterTurn(Rotation rotation) {
		return rotation == Rotation.CLOCKWISE_90 || rotation == Rotation.COUNTERCLOCKWISE_90;
	}

	@Override
	public BlockState rotate(BlockState state, Rotation rotation) {
		BlockState rotated = TrainRoofPartitionBehaviour.rotatePartitions(state, rotation);
		return isQuarterTurn(rotation) ? rotated.setValue(AXIS, nextAxis(rotated.getValue(AXIS))) : rotated;
	}

	@Override
	public BlockState mirror(BlockState state, Mirror mirror) {
		return TrainRoofPartitionBehaviour.mirrorPartitions(state, state, mirror);
	}

	public VoxelShape getHighlightedPartitionShape(BlockState state, BlockPos pos, Vec3 hit, Direction hitFace) {
		return TrainRoofPartitionBehaviour.getHighlightShape(state, pos, hit, hitFace);
	}

	private static boolean hasRoofAxis(BlockState state) {
		return state.hasProperty(AXIS)
			&& (state.getBlock() instanceof TrainRoofBlock || state.getBlock() instanceof TrainRoofSlopeBlock);
	}
}
