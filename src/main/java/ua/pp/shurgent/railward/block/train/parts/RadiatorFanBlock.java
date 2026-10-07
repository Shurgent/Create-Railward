package ua.pp.shurgent.railward.block.train.parts;

import ua.pp.shurgent.railward.compat.RailwardWrenchable;
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
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import ua.pp.shurgent.railward.AllBlockEntities;

public class RadiatorFanBlock extends HorizontalDirectionalBlock
	implements IBE<RadiatorFanBlockEntity>, RailwardWrenchable {

	public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
	public static final EnumProperty<Half> HALF = BlockStateProperties.HALF;

	private static final VoxelShape BOTTOM_SHAPE = Block.box(0, 0, 0, 16, 8, 16);
	private static final VoxelShape TOP_SHAPE = Block.box(0, 8, 0, 16, 16, 16);

	public RadiatorFanBlock(Properties properties) {
		super(properties.noOcclusion());
		registerDefaultState(defaultBlockState()
			.setValue(FACING, Direction.NORTH)
			.setValue(HALF, Half.BOTTOM));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, HALF);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		Direction clickedFace = context.getClickedFace();
		boolean top = clickedFace == Direction.DOWN
			|| clickedFace != Direction.UP
			&& context.getClickLocation().y - context.getClickedPos().getY() > .5;
		return defaultBlockState()
			.setValue(FACING, context.getHorizontalDirection())
			.setValue(HALF, top ? Half.TOP : Half.BOTTOM);
	}

	@Override
	public InteractionResult onWrenched(BlockState state, UseOnContext context) {
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();
		if (!level.isClientSide) {
			level.setBlock(pos, state.setValue(FACING, state.getValue(FACING).getClockWise()), 3);
			playRotateSound(level, pos);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public RenderShape getRenderShape(BlockState state) {
		return RenderShape.ENTITYBLOCK_ANIMATED;
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return state.getValue(HALF) == Half.TOP ? TOP_SHAPE : BOTTOM_SHAPE;
	}

	@Override
	public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
			CollisionContext context) {
		return getShape(state, level, pos, context);
	}

	@Override
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
		IBE.onRemove(state, level, pos, newState);
	}

	@Override
	public Class<RadiatorFanBlockEntity> getBlockEntityClass() {
		return RadiatorFanBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends RadiatorFanBlockEntity> getBlockEntityType() {
		return AllBlockEntities.RADIATOR_FAN.get();
	}
}
