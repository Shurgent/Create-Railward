package ua.pp.shurgent.railward.block.train.parts.door;

import com.simibubi.create.content.contraptions.ContraptionWorld;
import com.simibubi.create.content.decoration.slidingDoor.SlidingDoorBlock;
import com.simibubi.create.content.decoration.slidingDoor.SlidingDoorBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import ua.pp.shurgent.railward.AllBlockEntities;

public class TrainDoorBlock extends SlidingDoorBlock {

	private static final VoxelShape SOUTH_AABB = Block.box(0, 0, 0, 16, 16, 3);
	private static final VoxelShape WEST_AABB = Block.box(13, 0, 0, 16, 16, 16);
	private static final VoxelShape NORTH_AABB = Block.box(0, 0, 13, 16, 16, 16);
	private static final VoxelShape EAST_AABB = Block.box(0, 0, 0, 3, 16, 16);
	private static final VoxelShape HINGED_EAST_NORTH_OPEN = Block.box(-13, 0, 0, 3, 16, 3);
	private static final VoxelShape HINGED_EAST_SOUTH_OPEN = Block.box(-13, 0, 13, 3, 16, 16);
	private static final VoxelShape HINGED_WEST_NORTH_OPEN = Block.box(13, 0, 0, 29, 16, 3);
	private static final VoxelShape HINGED_WEST_SOUTH_OPEN = Block.box(13, 0, 13, 29, 16, 16);
	private static final VoxelShape HINGED_SOUTH_WEST_OPEN = Block.box(13, 0, -13, 16, 16, 3);
	private static final VoxelShape HINGED_SOUTH_EAST_OPEN = Block.box(0, 0, -13, 3, 16, 3);
	private static final VoxelShape HINGED_NORTH_WEST_OPEN = Block.box(0, 0, 13, 3, 16, 29);
	private static final VoxelShape HINGED_NORTH_EAST_OPEN = Block.box(13, 0, 13, 16, 16, 29);
	private static final VoxelShape SLIDING_SOUTH_OPEN = Shapes.or(
			Block.box(-13, 0, 0, 1, 16, 3),
			Block.box(15, 0, 0, 29, 16, 3));
	private static final VoxelShape SLIDING_WEST_OPEN = Shapes.or(
			Block.box(13, 0, -13, 16, 16, 1),
			Block.box(13, 0, 15, 16, 16, 29));
	private static final VoxelShape SLIDING_NORTH_OPEN = Shapes.or(
			Block.box(-13, 0, 13, 1, 16, 16),
			Block.box(15, 0, 13, 29, 16, 16));
	private static final VoxelShape SLIDING_EAST_OPEN = Shapes.or(
			Block.box(0, 0, -13, 3, 16, 1),
			Block.box(0, 0, 15, 3, 16, 29));

	private final TrainDoorAnimation animation;

	public TrainDoorBlock(Properties properties, TrainDoorAnimation animation) {
		super(properties, false);
		this.animation = animation;
	}

	public TrainDoorAnimation getAnimation() {
		return animation;
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		if (animation == TrainDoorAnimation.SLIDING) {
			if (!state.getValue(OPEN) && (state.getValue(VISIBLE) || level instanceof ContraptionWorld))
				return super.getShape(state, level, pos, context);
			return slidingOpenShape(state);
		}
		if (!state.getValue(OPEN) && (state.getValue(VISIBLE) || level instanceof ContraptionWorld))
			return hingedShape(state);
		return hingedShape(state.setValue(OPEN, true));
	}

	private static VoxelShape slidingOpenShape(BlockState state) {
		return switch (state.getValue(FACING)) {
		case EAST -> SLIDING_EAST_OPEN;
		case SOUTH -> SLIDING_SOUTH_OPEN;
		case WEST -> SLIDING_WEST_OPEN;
		default -> SLIDING_NORTH_OPEN;
		};
	}

	private static VoxelShape hingedShape(BlockState state) {
		Direction facing = state.getValue(FACING);
		boolean closed = !state.getValue(OPEN);
		boolean rightHinge = state.getValue(HINGE) == DoorHingeSide.RIGHT;

		return switch (facing) {
		case EAST -> closed ? EAST_AABB : rightHinge ? HINGED_EAST_SOUTH_OPEN : HINGED_EAST_NORTH_OPEN;
		case SOUTH -> closed ? SOUTH_AABB : rightHinge ? HINGED_SOUTH_EAST_OPEN : HINGED_SOUTH_WEST_OPEN;
		case WEST -> closed ? WEST_AABB : rightHinge ? HINGED_WEST_NORTH_OPEN : HINGED_WEST_SOUTH_OPEN;
		default -> closed ? NORTH_AABB : rightHinge ? HINGED_NORTH_EAST_OPEN : HINGED_NORTH_WEST_OPEN;
		};
	}

	@Override
	public BlockEntityType<? extends SlidingDoorBlockEntity> getBlockEntityType() {
		return animation == TrainDoorAnimation.HINGED
				? AllBlockEntities.HINGED_TRAIN_DOOR.get()
				: AllBlockEntities.SLIDING_TRAIN_DOOR.get();
	}
}
