package ua.pp.shurgent.railward.block.train.parts;

import java.util.List;

import com.simibubi.create.content.equipment.wrench.IWrenchable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Shared block implementation for the visual variants of diesel exhausts.
 * Each variant only needs its model and model-space emission points.
 */
public class DieselExhaustBlock extends HorizontalDirectionalBlock implements IWrenchable {

	public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

	private static final VoxelShape SHAPE = Shapes.or(
		Block.box(3, 0, 3, 13, 1, 13),
		Block.box(5, 1, 4, 11, 3, 5),
		Block.box(5, 1, 11, 11, 3, 12),
		Block.box(4, 1, 5, 5, 3, 11),
		Block.box(11, 1, 5, 12, 3, 11));
	private static final List<Vec3> TYPE_1_EMISSION_POINTS = List.of(new Vec3(.5, 3 / 16d, .5));

	private final List<Vec3> emissionPoints;

	public DieselExhaustBlock(Properties properties, List<Vec3> emissionPoints) {
		super(properties.noOcclusion());
		this.emissionPoints = List.copyOf(emissionPoints);
		registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH));
	}

	public static DieselExhaustBlock type1(Properties properties) {
		return new DieselExhaustBlock(properties, TYPE_1_EMISSION_POINTS);
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
		CollisionContext context) {
		return SHAPE;
	}

	public List<Vec3> getEmissionPoints(BlockState state) {
		Direction facing = state.getValue(FACING);
		return emissionPoints.stream().map(point -> rotate(point, facing)).toList();
	}

	private static Vec3 rotate(Vec3 point, Direction facing) {
		return switch (facing) {
		case EAST -> new Vec3(1 - point.z, point.y, point.x);
		case SOUTH -> new Vec3(1 - point.x, point.y, 1 - point.z);
		case WEST -> new Vec3(point.z, point.y, 1 - point.x);
		default -> point;
		};
	}
}
