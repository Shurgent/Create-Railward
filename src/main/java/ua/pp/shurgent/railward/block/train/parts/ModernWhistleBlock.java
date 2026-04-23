package ua.pp.shurgent.railward.block.train.parts;

import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.decoration.steamWhistle.WhistleBlock;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import ua.pp.shurgent.railward.AllBlockEntities;

public class ModernWhistleBlock extends WhistleBlock {

	public static final DirectionProperty ATTACHED_FACE = DirectionProperty.create("attached_face");
	public static final IntegerProperty ROTATION = IntegerProperty.create("rotation", 0, 3);
	public static final EnumProperty<ModernWhistleSoundType> SOUND_TYPE = EnumProperty.create("sound_type",
		ModernWhistleSoundType.class);

	private static final VoxelShape SHAPE = Block.box(3, 3, 3, 13, 13, 13);

	public ModernWhistleBlock(Properties properties) {
		super(BlockBehaviour.Properties.of(Material.METAL)
			.strength(1.0f, 6f)
			.sound(SoundType.METAL)
			.noOcclusion());
		registerDefaultState(syncCompatibilityState(defaultBlockState().setValue(ATTACHED_FACE, Direction.DOWN)
			.setValue(ROTATION, 0)
			.setValue(POWERED, false)
			.setValue(SOUND_TYPE, ModernWhistleSoundType.HORN_1)));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(ATTACHED_FACE, ROTATION, SOUND_TYPE);
		super.createBlockStateDefinition(builder);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		Direction attachedFace = context.getClickedFace().getOpposite();
		BlockState state = syncCompatibilityState(defaultBlockState().setValue(ATTACHED_FACE, attachedFace)
			.setValue(POWERED, context.getLevel().hasNeighborSignal(context.getClickedPos())));
		return state.canSurvive(context.getLevel(), context.getClickedPos()) ? state : null;
	}

	private BlockState syncCompatibilityState(BlockState state) {
		Direction attachedFace = state.getValue(ATTACHED_FACE);
		return state.setValue(WALL, attachedFace.getAxis() != Direction.Axis.Y)
			.setValue(FACING, resolveTrainFacing(state))
			.setValue(SIZE, state.getValue(SOUND_TYPE).getTrainWhistleSize());
	}

	private Direction resolveTrainFacing(BlockState state) {
		Direction attachedFace = state.getValue(ATTACHED_FACE);
		if (attachedFace.getAxis() != Direction.Axis.Y)
			return attachedFace;
		return switch (state.getValue(ROTATION)) {
		case 0 -> Direction.NORTH;
		case 1 -> Direction.EAST;
		case 2 -> Direction.SOUTH;
		default -> Direction.WEST;
		};
	}

	@Override
	public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		Direction attachedFace = state.getValue(ATTACHED_FACE);
		BlockPos supportPos = pos.relative(attachedFace);
		BlockState supportState = level.getBlockState(supportPos);
		return supportState.isFaceSturdy(level, supportPos, attachedFace.getOpposite());
	}

	@Override
	public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
		LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
		if (direction == state.getValue(ATTACHED_FACE) && !state.canSurvive(level, pos))
			return Blocks.AIR.defaultBlockState();
		return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
	}

	@Override
	public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos,
		boolean isMoving) {
		if (level.isClientSide)
			return;

		boolean powered = state.getValue(POWERED);
		boolean hasSignal = level.hasNeighborSignal(pos);
		if (powered != hasSignal)
			level.setBlock(pos, syncCompatibilityState(state.setValue(POWERED, hasSignal)), 2);
	}

	@Override
	public InteractionResult onWrenched(BlockState state, UseOnContext context) {
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();

		if (level.isClientSide)
			return InteractionResult.SUCCESS;

		level.setBlock(pos, syncCompatibilityState(state.cycle(SOUND_TYPE)), 3);
		syncTrainProfile(level, pos);
		playRotateSound(level, pos);
		return InteractionResult.SUCCESS;
	}

	@Override
	public InteractionResult onSneakWrenched(BlockState state, UseOnContext context) {
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();

		if (level.isClientSide)
			return InteractionResult.SUCCESS;

		int nextRotation = (state.getValue(ROTATION) + 1) & 3;
		level.setBlock(pos, syncCompatibilityState(state.setValue(ROTATION, nextRotation)), 3);
		playRotateSound(level, pos);
		return InteractionResult.SUCCESS;
	}

	private void syncTrainProfile(Level level, BlockPos pos) {
		if (level.getBlockEntity(pos) instanceof ModernWhistleBlockEntity modernWhistle)
			modernWhistle.syncTrainWhistleProfile();
	}

	@Override
	public void playRotateSound(Level level, BlockPos pos) {
		float pitch = level.getRandom().nextFloat() + 0.5f;
		AllSoundEvents.WRENCH_ROTATE.playOnServer(level, pos, 1f, pitch);
	}

	@Override
	public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
	}

	@Override
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
		IBE.onRemove(state, level, pos, newState);
	}

	@Override
	public RenderShape getRenderShape(BlockState state) {
		return RenderShape.ENTITYBLOCK_ANIMATED;
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	public boolean isPathfindable(BlockState state, BlockGetter level, BlockPos pos, PathComputationType type) {
		return false;
	}

	@Override
	public BlockEntityType<? extends com.simibubi.create.content.decoration.steamWhistle.WhistleBlockEntity> getBlockEntityType() {
		return AllBlockEntities.MODERN_WHISTLE.get();
	}

}
