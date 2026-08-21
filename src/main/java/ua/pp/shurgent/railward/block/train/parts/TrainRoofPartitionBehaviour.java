package ua.pp.shurgent.railward.block.train.parts;

import java.util.EnumSet;
import java.util.function.UnaryOperator;

import javax.annotation.Nullable;

import com.simibubi.create.AllItems;
import com.simibubi.create.foundation.utility.VoxelShaper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.StairsShape;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import ua.pp.shurgent.railward.block.train.parts.copycat.CopycatWallPanelBlock;
import ua.pp.shurgent.railward.block.train.parts.copycat.DualPanelCopycatBlockEntity;
import ua.pp.shurgent.railward.block.train.parts.door.CabCornerDoorBlock;
import ua.pp.shurgent.railward.block.train.parts.door.TrainDoorBlock;

/** Shared state, interaction and collision logic for roof partitions. */
public final class TrainRoofPartitionBehaviour {

	public static final BooleanProperty PARTITION_NORTH = BooleanProperty.create("partition_north");
	public static final BooleanProperty PARTITION_EAST = BooleanProperty.create("partition_east");
	public static final BooleanProperty PARTITION_SOUTH = BooleanProperty.create("partition_south");
	public static final BooleanProperty PARTITION_WEST = BooleanProperty.create("partition_west");

	private static final Direction[] HORIZONTAL_DIRECTIONS = {
		Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST
	};
	private static final double PANEL_HEIGHT = 8;
	private static final double PANEL_THICKNESS = 3;
	private static final double INTERACTION_WIDTH = 5;
	private static final double HIT_EPSILON = 1e-4;
	private static final VoxelShaper WEDGE_COUNTER_CLOCKWISE = VoxelShaper.forDirectional(
		wedgeNorth(true), Direction.NORTH);
	private static final VoxelShaper WEDGE_CLOCKWISE = VoxelShaper.forDirectional(
		wedgeNorth(false), Direction.NORTH);

	public enum PartitionShape {
		NONE,
		FULL,
		WEDGE_COUNTER_CLOCKWISE,
		WEDGE_CLOCKWISE
	}

	private TrainRoofPartitionBehaviour() {
	}

	public static void addProperties(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(PARTITION_NORTH, PARTITION_EAST, PARTITION_SOUTH, PARTITION_WEST);
	}

	public static BlockState withDefaults(BlockState state) {
		return state.setValue(PARTITION_NORTH, false)
			.setValue(PARTITION_EAST, false)
			.setValue(PARTITION_SOUTH, false)
			.setValue(PARTITION_WEST, false);
	}

	public static boolean hasPartition(BlockState state, Direction direction) {
		return direction.getAxis().isHorizontal() && state.getValue(property(direction));
	}

	public static BlockState togglePartition(BlockState state, Direction direction) {
		return state.cycle(property(direction));
	}

	public static boolean isRoof(BlockState state) {
		return state.getBlock() instanceof TrainRoofBlock || state.getBlock() instanceof TrainRoofSlopeBlock;
	}

	public static boolean isAllowed(BlockState state, Direction direction) {
		return getPartitionShape(state, direction) != PartitionShape.NONE;
	}

	public static PartitionShape getPartitionShape(BlockState state, Direction direction) {
		if (!direction.getAxis().isHorizontal())
			return PartitionShape.NONE;
		if (state.getBlock() instanceof TrainRoofBlock)
			return PartitionShape.FULL;
		if (!(state.getBlock() instanceof TrainRoofSlopeBlock))
			return PartitionShape.NONE;

		boolean counterClockwiseHigh = isHighCorner(state, direction, direction.getCounterClockWise());
		boolean clockwiseHigh = isHighCorner(state, direction, direction.getClockWise());
		if (counterClockwiseHigh && clockwiseHigh)
			return PartitionShape.FULL;
		if (counterClockwiseHigh)
			return PartitionShape.WEDGE_COUNTER_CLOCKWISE;
		if (clockwiseHigh)
			return PartitionShape.WEDGE_CLOCKWISE;
		return PartitionShape.NONE;
	}

	public static BlockState clearUnsupported(BlockState state) {
		for (Direction direction : HORIZONTAL_DIRECTIONS)
			if (hasPartition(state, direction) && !isAllowed(state, direction))
				state = state.setValue(property(direction), false);
		return state;
	}

	public static BlockState enablePartitionsFromBelow(BlockState roofState, BlockGetter level, BlockPos roofPos) {
		if (!isRoof(roofState))
			return roofState;
		for (Direction direction : getPartitionDirections(level, roofPos.below()))
			if (isAllowed(roofState, direction))
				roofState = roofState.setValue(property(direction), true);
		return roofState;
	}

	public static void enablePartitionsAbove(Level level, BlockPos supportPos) {
		if (level.isClientSide)
			return;
		BlockPos roofPos = supportPos.above();
		BlockState roofState = level.getBlockState(roofPos);
		BlockState updated = enablePartitionsFromBelow(roofState, level, roofPos);
		if (!updated.equals(roofState))
			level.setBlock(roofPos, updated, Block.UPDATE_ALL);
	}

	private static EnumSet<Direction> getPartitionDirections(BlockGetter level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		EnumSet<Direction> directions = EnumSet.noneOf(Direction.class);
		if (state.getBlock() instanceof TrainWallPanelBlock) {
			directions.add(state.getValue(TrainWallPanelBlock.FACING));
			if (level.getBlockEntity(pos) instanceof TrainWallPanelBlockEntity panel) {
				BlockState secondary = panel.getSecondaryPanel();
				if (secondary != null)
					directions.add(secondary.getValue(TrainWallPanelBlock.FACING));
			}
			return directions;
		}
		if (state.getBlock() instanceof CopycatWallPanelBlock) {
			directions.add(state.getValue(CopycatWallPanelBlock.FACING));
			if (level.getBlockEntity(pos) instanceof DualPanelCopycatBlockEntity panel) {
				Direction secondary = panel.railward$getSecondaryFacing();
				if (secondary != null)
					directions.add(secondary);
			}
			return directions;
		}
		if (state.getBlock() instanceof TrainDoorBlock) {
			directions.add(state.getValue(TrainDoorBlock.FACING).getOpposite());
			return directions;
		}
		if (state.getBlock() instanceof CabCornerDoorBlock) {
			Direction facing = state.getValue(CabCornerDoorBlock.FACING);
			directions.add(facing.getOpposite());
			directions.add(state.getValue(CabCornerDoorBlock.HINGE) == DoorHingeSide.RIGHT
				? facing.getClockWise()
				: facing.getCounterClockWise());
		}
		return directions;
	}

	@Nullable
	public static Direction resolveTarget(BlockState state, BlockPos pos, Vec3 hit, Direction hitFace) {
		Vec3 localHit = hit.subtract(pos.getX(), pos.getY(), pos.getZ());
		Direction panelTarget = resolvePanelTarget(state, localHit, hitFace);
		if (panelTarget != null)
			return panelTarget;
		if (hitFace != Direction.DOWN)
			return null;

		double localX = localHit.x * 16;
		double localZ = localHit.z * 16;
		Direction nearest = null;
		double nearestDistance = Double.MAX_VALUE;
		for (Direction direction : HORIZONTAL_DIRECTIONS) {
			if (!isAllowed(state, direction))
				continue;
			double distance = switch (direction) {
			case NORTH -> localZ;
			case EAST -> 16 - localX;
			case SOUTH -> 16 - localZ;
			case WEST -> localX;
			default -> Double.MAX_VALUE;
			};
			if (distance <= INTERACTION_WIDTH && distance < nearestDistance) {
				nearest = direction;
				nearestDistance = distance;
			}
		}
		return nearest;
	}

	public static VoxelShape addPartitions(BlockState state, VoxelShape roofShape) {
		VoxelShape result = roofShape;
		for (Direction direction : HORIZONTAL_DIRECTIONS)
			if (hasPartition(state, direction))
				result = Shapes.or(result, partitionShape(state, direction));
		return result;
	}

	public static VoxelShape getSelectionShape(BlockState state, VoxelShape roofShape, CollisionContext context) {
		VoxelShape result = addPartitions(state, roofShape);
		if (!isHoldingWrench(context))
			return result;

		for (Direction direction : HORIZONTAL_DIRECTIONS)
			if (isAllowed(state, direction))
				result = Shapes.or(result, fullPartitionShape(direction));
		return result.optimize();
	}

	public static VoxelShape getHighlightShape(BlockState state, BlockPos pos, Vec3 hit, Direction hitFace) {
		Direction direction = resolveTarget(state, pos, hit, hitFace);
		if (direction == null)
			return Shapes.empty();
		return fullPartitionShape(direction);
	}

	@Nullable
	private static Direction resolvePanelTarget(BlockState state, Vec3 localHit, Direction hitFace) {
		for (boolean matchFaceAxis : new boolean[] {true, false}) {
			for (Direction direction : HORIZONTAL_DIRECTIONS) {
				if (!isAllowed(state, direction))
					continue;
				if (matchFaceAxis && direction.getAxis() != hitFace.getAxis())
					continue;
				if (contains(fullPartitionShape(direction), localHit))
					return direction;
			}
		}
		return null;
	}

	private static boolean contains(VoxelShape shape, Vec3 point) {
		for (AABB bounds : shape.toAabbs())
			if (point.x >= bounds.minX - HIT_EPSILON && point.x <= bounds.maxX + HIT_EPSILON
					&& point.y >= bounds.minY - HIT_EPSILON && point.y <= bounds.maxY + HIT_EPSILON
					&& point.z >= bounds.minZ - HIT_EPSILON && point.z <= bounds.maxZ + HIT_EPSILON)
				return true;
		return false;
	}

	public static BlockState rotatePartitions(BlockState state, Rotation rotation) {
		return remapPartitions(state, state, rotation::rotate);
	}

	public static BlockState rotatePartitions(BlockState source, BlockState target, Rotation rotation) {
		return remapPartitions(source, target, rotation::rotate);
	}

	public static BlockState mirrorPartitions(BlockState source, BlockState target, Mirror mirror) {
		return remapPartitions(source, target, mirror::mirror);
	}

	public static StairsShape mirrorShape(StairsShape shape) {
		return switch (shape) {
		case INNER_LEFT -> StairsShape.INNER_RIGHT;
		case INNER_RIGHT -> StairsShape.INNER_LEFT;
		case OUTER_LEFT -> StairsShape.OUTER_RIGHT;
		case OUTER_RIGHT -> StairsShape.OUTER_LEFT;
		default -> shape;
		};
	}

	private static BlockState remapPartitions(BlockState source, BlockState target,
			UnaryOperator<Direction> directionTransform) {
		target = withDefaults(target);
		for (Direction direction : HORIZONTAL_DIRECTIONS)
			if (hasPartition(source, direction))
				target = target.setValue(property(directionTransform.apply(direction)), true);
		return target;
	}

	private static BooleanProperty property(Direction direction) {
		return switch (direction) {
		case NORTH -> PARTITION_NORTH;
		case EAST -> PARTITION_EAST;
		case SOUTH -> PARTITION_SOUTH;
		case WEST -> PARTITION_WEST;
		default -> throw new IllegalArgumentException("Partition direction must be horizontal");
		};
	}

	private static VoxelShape partitionShape(BlockState state, Direction direction) {
		return switch (getPartitionShape(state, direction)) {
		case FULL -> fullPartitionShape(direction);
		case WEDGE_COUNTER_CLOCKWISE -> WEDGE_COUNTER_CLOCKWISE.get(direction);
		case WEDGE_CLOCKWISE -> WEDGE_CLOCKWISE.get(direction);
		case NONE -> Shapes.empty();
		};
	}

	private static VoxelShape fullPartitionShape(Direction direction) {
		return switch (direction) {
		case NORTH -> Block.box(0, 0, 0, 16, PANEL_HEIGHT, PANEL_THICKNESS);
		case EAST -> Block.box(16 - PANEL_THICKNESS, 0, 0, 16, PANEL_HEIGHT, 16);
		case SOUTH -> Block.box(0, 0, 16 - PANEL_THICKNESS, 16, PANEL_HEIGHT, 16);
		case WEST -> Block.box(0, 0, 0, PANEL_THICKNESS, PANEL_HEIGHT, 16);
		default -> Shapes.empty();
		};
	}

	private static VoxelShape wedgeNorth(boolean counterClockwiseHigh) {
		VoxelShape shape = Shapes.empty();
		for (int segment = 0; segment < 4; segment++) {
			int distanceFromHighEnd = counterClockwiseHigh ? segment : 3 - segment;
			double height = PANEL_HEIGHT - distanceFromHighEnd * PANEL_HEIGHT / 3;
			if (height <= 0)
				continue;
			double minX = segment * 4;
			shape = Shapes.or(shape, Block.box(minX, 0, 0, minX + 4, height, PANEL_THICKNESS));
		}
		return shape;
	}

	private static boolean isHoldingWrench(CollisionContext context) {
		if (!(context instanceof EntityCollisionContext entityContext))
			return false;
		Entity entity = entityContext.getEntity();
		if (!(entity instanceof Player player))
			return false;
		return AllItems.WRENCH.isIn(player.getMainHandItem()) || AllItems.WRENCH.isIn(player.getOffhandItem());
	}

	private static boolean isHighCorner(BlockState state, Direction first, Direction second) {
		Direction facing = state.getValue(TrainRoofSlopeBlock.FACING);
		return switch (state.getValue(TrainRoofSlopeBlock.SHAPE)) {
		case STRAIGHT -> first == facing || second == facing;
		case INNER_LEFT -> !sameCorner(first, second, facing.getOpposite(), facing.getClockWise());
		case INNER_RIGHT -> !sameCorner(first, second, facing.getOpposite(), facing.getCounterClockWise());
		case OUTER_LEFT -> sameCorner(first, second, facing, facing.getCounterClockWise());
		case OUTER_RIGHT -> sameCorner(first, second, facing, facing.getClockWise());
		};
	}

	private static boolean sameCorner(Direction first, Direction second, Direction expectedFirst,
			Direction expectedSecond) {
		return first == expectedFirst && second == expectedSecond
			|| first == expectedSecond && second == expectedFirst;
	}

}
