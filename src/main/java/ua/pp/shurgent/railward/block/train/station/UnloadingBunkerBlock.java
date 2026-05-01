package ua.pp.shurgent.railward.block.train.station;

import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.content.equipment.wrench.IWrenchable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import ua.pp.shurgent.railward.AllBlockEntities;

public class UnloadingBunkerBlock extends HorizontalDirectionalBlock implements IBE<UnloadingBunkerBlockEntity>, IWrenchable {

	public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

	private static final VoxelShape BOWL = Shapes.or(Block.box(0, 0, 0, 16, 2, 16),
		Block.box(0, 0, 0, 2, 16, 16), Block.box(14, 0, 0, 16, 16, 16),
		Block.box(0, 0, 0, 16, 16, 2), Block.box(0, 0, 14, 16, 16, 16));
	private static final VoxelShape GRATE = Shapes.or(Block.box(0, 14, 0, 16, 16, 2),
		Block.box(0, 14, 14, 16, 16, 16), Block.box(0, 14, 0, 2, 16, 16),
		Block.box(14, 14, 0, 16, 16, 16), Block.box(7, 14, 0, 9, 16, 16),
		Block.box(0, 14, 7, 16, 16, 9));
	private static final VoxelShape SHAPE = Shapes.or(BOWL, GRATE);

	public UnloadingBunkerBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
	}

	@Override
	public InteractionResult onWrenched(BlockState state, UseOnContext context) {
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();
		if (level.isClientSide)
			return InteractionResult.SUCCESS;

		level.setBlock(pos, state.setValue(FACING, state.getValue(FACING).getClockWise()), 3);
		playRotateSound(level, pos);
		return InteractionResult.SUCCESS;
	}

	@Override
	public RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
		IBE.onRemove(state, level, pos, newState);
	}

	@Override
	public Class<UnloadingBunkerBlockEntity> getBlockEntityClass() {
		return UnloadingBunkerBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends UnloadingBunkerBlockEntity> getBlockEntityType() {
		return AllBlockEntities.UNLOADING_BUNKER.get();
	}
}
