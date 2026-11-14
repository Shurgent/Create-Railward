package ua.pp.shurgent.railward.block.train.signal;

import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.utility.VoxelShaper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import ua.pp.shurgent.railward.AllBlockEntities;
import ua.pp.shurgent.railward.client.gui.RailCrossingSignalConfigurationScreen;
import ua.pp.shurgent.railward.compat.RailwardWrenchable;

public class RailCrossingSignalBlock extends HorizontalDirectionalBlock
	implements IBE<RailCrossingSignalBlockEntity>, RailwardWrenchable {

	private static final VoxelShaper SHAPE = VoxelShaper.forHorizontal(Shapes.or(
		box(7, 0, 7, 9, 8, 9), box(4, 8, 7, 12, 9, 8), box(3.6, 9, 7, 4.6, 11, 8),
		box(11.4, 9, 7, 12.4, 11, 8), box(1, 10, 5.5, 7, 16, 8), box(9, 10, 5.5, 15, 16, 8)),
		Direction.NORTH);

	public RailCrossingSignalBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH));
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
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
		if (!level.isClientSide && level.getBlockEntity(pos) instanceof RailCrossingSignalBlockEntity signal)
			signal.bindTo(RailCrossingSignalItem.selectedCabinet(stack, level));
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
	public InteractionResult onWrenched(BlockState state, UseOnContext context) {
		Level level = context.getLevel();
		if (!level.isClientSide) {
			level.setBlockAndUpdate(context.getClickedPos(), rotate(state, Rotation.CLOCKWISE_90));
			playRotateSound(level, context.getClickedPos());
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE.get(state.getValue(FACING));
	}

	@Override
	public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
		BlockHitResult hit) {
		if (!isConfigurationHit(state, hit) || player.isSpectator() || !player.mayBuild())
			return InteractionResult.PASS;
		if (level.isClientSide && level.getBlockEntity(pos) instanceof RailCrossingSignalBlockEntity signal)
			DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> RailCrossingSignalConfigurationScreen.open(signal));
		return InteractionResult.sidedSuccess(level.isClientSide);
	}

	public static boolean isConfigurationHit(BlockState state, BlockHitResult hit) {
		return hit.getDirection() == state.getValue(FACING) && isConfigurationOffset(state,
			hit.getLocation().subtract(Vec3.atLowerCornerOf(hit.getBlockPos())));
	}

	public static boolean isConfigurationOffset(BlockState state, Vec3 localHit) {
		Direction facing = state.getValue(FACING);
		Vec3 offset = localHit.subtract(.5, .5, .5);
		double sideways = facing.getAxis() == Direction.Axis.Z ? offset.x : offset.z;
		double depth = offset.x * facing.getStepX() + offset.z * facing.getStepZ();
		return Math.abs(sideways) <= 1 / 16d && Math.abs(offset.y) <= 1 / 16d
			&& Math.abs(depth - 1 / 16d) < .001;
	}

	@Override
	public Class<RailCrossingSignalBlockEntity> getBlockEntityClass() {
		return RailCrossingSignalBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends RailCrossingSignalBlockEntity> getBlockEntityType() {
		return AllBlockEntities.RAIL_CROSSING_SIGNAL.get();
	}

	@Override
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
		IBE.onRemove(state, level, pos, newState);
	}
}
