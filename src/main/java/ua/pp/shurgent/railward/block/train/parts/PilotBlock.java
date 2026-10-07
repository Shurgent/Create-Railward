package ua.pp.shurgent.railward.block.train.parts;

import ua.pp.shurgent.railward.compat.RailwardWrenchable;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.utility.VoxelShaper;

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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import ua.pp.shurgent.railward.AllBlockEntities;

public class PilotBlock extends HorizontalDirectionalBlock implements IBE<PilotBlockEntity>, RailwardWrenchable {

	public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
	public static final EnumProperty<PilotSide> SIDE = EnumProperty.create("side", PilotSide.class);

	private static final VoxelShape SHAPE_NORTH = Shapes.or(
		Block.box(0, 14, 0, 16, 16, 12),
		Block.box(0, 7, 6, 16, 16, 14)
	);
	private static final VoxelShaper SHAPE = VoxelShaper.forDirectional(SHAPE_NORTH, Direction.NORTH);
	private static final VoxelShape TYPE_2_SHAPE_NORTH = Shapes.or(
		Block.box(0, 14, 0, 16, 16, 16),
		Block.box(0, 6, 9, 16, 14, 16),
		Block.box(0, 9, 0, 4, 14, 10)
	);
	private static final VoxelShaper TYPE_2_SHAPE =
		VoxelShaper.forDirectional(TYPE_2_SHAPE_NORTH, Direction.NORTH);

	private final PilotType pilotType;

	public PilotBlock(Properties properties, PilotType pilotType) {
		super(properties.noOcclusion());
		this.pilotType = pilotType;
		registerDefaultState(defaultBlockState()
			.setValue(FACING, Direction.NORTH)
			.setValue(SIDE, PilotSide.LEFT));
	}

	public PilotType getPilotType() {
		return pilotType;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, SIDE);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		Direction facing = context.getHorizontalDirection();
		BlockPos pos = context.getClickedPos();
		Vec3 hit = context.getClickLocation();
		Direction right = facing.getClockWise();
		double lateralOffset = (hit.x - pos.getX() - .5) * right.getStepX()
			+ (hit.z - pos.getZ() - .5) * right.getStepZ();
		PilotSide side = lateralOffset > 0 ? PilotSide.RIGHT : PilotSide.LEFT;
		return defaultBlockState().setValue(FACING, facing).setValue(SIDE, side);
	}

	@Override
	public InteractionResult onWrenched(BlockState state, UseOnContext context) {
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();
		if (level.isClientSide)
			return InteractionResult.SUCCESS;

		level.setBlock(pos, state.setValue(SIDE, state.getValue(SIDE).opposite()), 3);
		playRotateSound(level, pos);
		return InteractionResult.SUCCESS;
	}

	@Override
	public RenderShape getRenderShape(BlockState state) {
		return RenderShape.ENTITYBLOCK_ANIMATED;
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return getShape(state);
	}

	@Override
	public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
			CollisionContext context) {
		return getShape(state);
	}

	@Override
	public VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
		return Shapes.empty();
	}

	@Override
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
		IBE.onRemove(state, level, pos, newState);
	}

	@Override
	public Class<PilotBlockEntity> getBlockEntityClass() {
		return PilotBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends PilotBlockEntity> getBlockEntityType() {
		return AllBlockEntities.PILOT_TYPE_1.get();
	}

	private VoxelShape getShape(BlockState state) {
		VoxelShaper shaper = pilotType == PilotType.TYPE_2 ? TYPE_2_SHAPE : SHAPE;
		return shaper.get(state.getValue(FACING));
	}
}
