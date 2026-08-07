package ua.pp.shurgent.railward.block.train.parts;

import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.utility.VoxelShaper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.MaterialColor;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import ua.pp.shurgent.railward.AllBlockEntities;

public class LocomotiveSpotlightBlock extends HorizontalDirectionalBlock
	implements IBE<LocomotiveSpotlightBlockEntity>, IWrenchable {

	public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
	public static final EnumProperty<LocomotiveSpotlightVariant> VARIANT =
		EnumProperty.create("variant", LocomotiveSpotlightVariant.class);

	private static final VoxelShaper SMALL_SHAPE = VoxelShaper.forDirectional(
		Block.box(5, 7, 11, 11, 13, 16), Direction.NORTH);
	private static final VoxelShaper LARGE_SHAPE = VoxelShaper.forDirectional(
		Block.box(3, 3, 11, 13, 13, 16), Direction.NORTH);
	private static final VoxelShaper SMALL_PANEL_SHAPE = VoxelShaper.forDirectional(
		Block.box(5, 7, 8, 11, 13, 13), Direction.NORTH);
	private static final VoxelShaper LARGE_PANEL_SHAPE = VoxelShaper.forDirectional(
		Block.box(3, 3, 8, 13, 13, 13), Direction.NORTH);

	public LocomotiveSpotlightBlock(BlockBehaviour.Properties properties) {
		super(properties.strength(1.0f, 6f)
			.sound(SoundType.METAL)
			.color(MaterialColor.COLOR_BROWN)
			.noOcclusion());
		registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH)
			.setValue(VARIANT, LocomotiveSpotlightVariant.SMALL));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, VARIANT);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		Direction clickedFace = context.getClickedFace();
		Direction facing = clickedFace.getAxis().isHorizontal() ? clickedFace : context.getHorizontalDirection();
		BlockState state = defaultBlockState().setValue(FACING, facing);
		return state.canSurvive(context.getLevel(), context.getClickedPos()) ? state : null;
	}

	@Override
	public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		BlockState supportState = level.getBlockState(pos.relative(state.getValue(FACING).getOpposite()));
		return !supportState.getMaterial().isReplaceable();
	}

	@Override
	public InteractionResult onWrenched(BlockState state, UseOnContext context) {
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();
		if (level.isClientSide)
			return InteractionResult.SUCCESS;

		level.setBlock(pos, state.setValue(VARIANT, state.getValue(VARIANT).next()), 3);
		playRotateSound(level, pos);
		return InteractionResult.SUCCESS;
	}

	@Override
	public void playRotateSound(Level level, BlockPos pos) {
		float pitch = level.getRandom().nextFloat() + 0.5f;
		AllSoundEvents.WRENCH_ROTATE.playOnServer(level, pos, 1f, pitch);
	}

	@Override
	public RenderShape getRenderShape(BlockState state) {
		return RenderShape.ENTITYBLOCK_ANIMATED;
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return getMountedShape(state.getValue(VARIANT), state.getValue(FACING));
	}

	@Override
	public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
		CollisionContext context) {
		return Shapes.empty();
	}

	@Override
	public boolean isPathfindable(BlockState state, BlockGetter level, BlockPos pos, PathComputationType type) {
		return false;
	}

	public static VoxelShape getMountedShape(LocomotiveSpotlightVariant variant, Direction facing) {
		return (variant == LocomotiveSpotlightVariant.LARGE ? LARGE_SHAPE : SMALL_SHAPE).get(facing);
	}

	public static VoxelShape getPanelAttachmentShape(LocomotiveSpotlightVariant variant, Direction facing) {
		VoxelShaper shape = variant == LocomotiveSpotlightVariant.LARGE ? LARGE_PANEL_SHAPE : SMALL_PANEL_SHAPE;
		return shape.get(facing.getOpposite());
	}

	@Override
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
		IBE.onRemove(state, level, pos, newState);
	}

	@Override
	public Class<LocomotiveSpotlightBlockEntity> getBlockEntityClass() {
		return LocomotiveSpotlightBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends LocomotiveSpotlightBlockEntity> getBlockEntityType() {
		return AllBlockEntities.LOCOMOTIVE_SPOTLIGHT.get();
	}
}
