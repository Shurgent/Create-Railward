package ua.pp.shurgent.railward.block.train.signal;

import com.simibubi.create.AllSoundEvents;
import ua.pp.shurgent.railward.compat.RailwardWrenchable;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import ua.pp.shurgent.railward.AllBlockEntities;

public class WhistleSignBlock extends Block implements IBE<WhistleSignBlockEntity>, RailwardWrenchable {

	public static final IntegerProperty ROTATION = BlockStateProperties.ROTATION_16;

	private static final VoxelShape SHAPE = Block.box(5, 0, 5, 11, 24, 11);

	public WhistleSignBlock(Properties properties) {
		super(properties.strength(1.0f, 6f).noOcclusion().sound(SoundType.METAL));
		registerDefaultState(defaultBlockState().setValue(ROTATION, 0));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(ROTATION);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		int rot = Mth.floor(((context.getRotation()) * 16.0F / 360.0F) + 0.5D) & 15;
		return defaultBlockState().setValue(ROTATION, rot);
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
	public InteractionResult onWrenched(BlockState state, UseOnContext context) {
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();

		if (level.isClientSide)
			return InteractionResult.SUCCESS;

		int next = (state.getValue(ROTATION) + 1) & 15;
		level.setBlockAndUpdate(pos, state.setValue(ROTATION, next));
		float pitch = level.getRandom().nextFloat() + 0.5f;
		AllSoundEvents.WRENCH_ROTATE.playOnServer(level, pos, 1f, pitch);
		return InteractionResult.SUCCESS;
	}

	@Override
	public Class<WhistleSignBlockEntity> getBlockEntityClass() {
		return WhistleSignBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends WhistleSignBlockEntity> getBlockEntityType() {
		return AllBlockEntities.WHISTLE_SIGN.get();
	}

	@Override
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
		IBE.onRemove(state, level, pos, newState);
	}
}
