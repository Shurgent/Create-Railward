package ua.pp.shurgent.railward.block.train.signal;

import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import ua.pp.shurgent.railward.AllBlockEntities;
import ua.pp.shurgent.railward.compat.RailwardWrenchable;

public class RelayCaseBlock extends HorizontalDirectionalBlock implements IBE<RelayCaseBlockEntity>, RailwardWrenchable {

	/** Output reports trains only; occupied also includes the external input. */
	public static final BooleanProperty TRAIN_PRESENT = BooleanProperty.create("train_present");
	public static final BooleanProperty OCCUPIED = BooleanProperty.create("occupied");
	private static final VoxelShape NORTH = Shapes.or(Block.box(3, 0, 4, 5, 2, 12),
		Block.box(11, 0, 4, 13, 2, 12), Block.box(2, 2, 4, 14, 15, 12), Block.box(2, 15, 3, 14, 16, 12));
	private static final VoxelShape SOUTH = Shapes.or(Block.box(3, 0, 4, 5, 2, 12),
		Block.box(11, 0, 4, 13, 2, 12), Block.box(2, 2, 4, 14, 15, 12), Block.box(2, 15, 4, 14, 16, 13));
	private static final VoxelShape EAST = Shapes.or(Block.box(4, 0, 3, 12, 2, 5),
		Block.box(4, 0, 11, 12, 2, 13), Block.box(4, 2, 2, 12, 15, 14), Block.box(4, 15, 2, 13, 16, 14));
	private static final VoxelShape WEST = Shapes.or(Block.box(4, 0, 3, 12, 2, 5),
		Block.box(4, 0, 11, 12, 2, 13), Block.box(4, 2, 2, 12, 15, 14), Block.box(3, 15, 2, 12, 16, 14));

	public RelayCaseBlock(Properties properties) {
		super(properties.strength(3, 6).noOcclusion().sound(SoundType.METAL));
		registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH)
			.setValue(TRAIN_PRESENT, false).setValue(OCCUPIED, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, TRAIN_PRESENT, OCCUPIED);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	@Override
	public BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	public BlockState mirror(BlockState state, Mirror mirror) {
		return rotate(state, mirror.getRotation(state.getValue(FACING)));
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return switch (state.getValue(FACING)) {
			case SOUTH -> SOUTH;
			case EAST -> EAST;
			case WEST -> WEST;
			default -> NORTH;
		};
	}

	@Override
	public InteractionResult onWrenched(BlockState state, UseOnContext context) {
		if (!context.getLevel().isClientSide) {
			context.getLevel().setBlockAndUpdate(context.getClickedPos(), rotate(state, Rotation.CLOCKWISE_90));
			playRotateSound(context.getLevel(), context.getClickedPos());
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public boolean isSignalSource(BlockState state) {
		return true;
	}

	@Override
	public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction side) {
		return state.getValue(TRAIN_PRESENT) ? 15 : 0;
	}

	@Override
	public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean moving) {
		if (!level.isClientSide && level.getBlockEntity(pos) instanceof RelayCaseBlockEntity relay)
			relay.refreshState();
	}

	@Override
	public Class<RelayCaseBlockEntity> getBlockEntityClass() {
		return RelayCaseBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends RelayCaseBlockEntity> getBlockEntityType() {
		return AllBlockEntities.RELAY_CASE.get();
	}

	@Override
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
		IBE.onRemove(state, level, pos, newState);
		if (!state.is(newState.getBlock()) && state.getValue(TRAIN_PRESENT))
			level.updateNeighborsAt(pos, this);
	}
}
