package ua.pp.shurgent.railward.block.train.parts.door;

import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.decoration.slidingDoor.SlidingDoorBlock;
import com.simibubi.create.content.decoration.slidingDoor.SlidingDoorBlockEntity;
import com.simibubi.create.foundation.utility.VoxelShaper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import ua.pp.shurgent.railward.AllBlockEntities;

public class CabCornerDoorBlock extends SlidingDoorBlock {

	private static final VoxelShape RIGHT_PANEL_NORTH = Block.box(13, 0, 0, 16, 16, 16);
	private static final VoxelShape LEFT_PANEL_NORTH = Block.box(0, 0, 0, 3, 16, 16);
	private static final VoxelShaper RIGHT_PANEL = VoxelShaper.forHorizontal(RIGHT_PANEL_NORTH, Direction.NORTH);
	private static final VoxelShaper LEFT_PANEL = VoxelShaper.forHorizontal(LEFT_PANEL_NORTH, Direction.NORTH);
	private static final VoxelShaper RIGHT_OPEN = VoxelShaper.forHorizontal(
		Block.box(10, 0, 0, 16, 16, 16), Direction.NORTH);
	private static final VoxelShaper LEFT_OPEN = VoxelShaper.forHorizontal(
		Block.box(0, 0, 0, 6, 16, 16), Direction.NORTH);

	private static final VoxelShaper RIGHT_CLOSED = VoxelShaper.forHorizontal(Shapes.or(
		RIGHT_PANEL_NORTH,
		Block.box(0, 0, 13, 13, 16, 16)), Direction.NORTH);
	private static final VoxelShaper LEFT_CLOSED = VoxelShaper.forHorizontal(Shapes.or(
		LEFT_PANEL_NORTH,
		Block.box(3, 0, 13, 16, 16, 16)), Direction.NORTH);

	public CabCornerDoorBlock(Properties properties) {
		super(properties, false);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockState state = super.getStateForPlacement(context);
		if (state == null)
			return null;

		DoorHingeSide hinge = state.getValue(HINGE) == DoorHingeSide.RIGHT
			? DoorHingeSide.LEFT
			: DoorHingeSide.RIGHT;
		return state.setValue(FACING, state.getValue(FACING).getOpposite())
			.setValue(HINGE, hinge);
	}

	@Override
	public RenderShape getRenderShape(BlockState state) {
		return RenderShape.ENTITYBLOCK_ANIMATED;
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		if (!state.getValue(OPEN))
			return getClosedShape(state);
		return getOpenShape(state);
	}

	@Override
	public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
			CollisionContext context) {
		if (!state.getValue(OPEN))
			return getClosedShape(state);
		return getPanelShape(state);
	}

	private static VoxelShape getClosedShape(BlockState state) {
		return (isPanelOnRight(state) ? RIGHT_CLOSED : LEFT_CLOSED).get(state.getValue(FACING));
	}

	private static VoxelShape getOpenShape(BlockState state) {
		return (isPanelOnRight(state) ? RIGHT_OPEN : LEFT_OPEN).get(state.getValue(FACING));
	}

	public static VoxelShape getPanelShape(BlockState state) {
		return (isPanelOnRight(state) ? RIGHT_PANEL : LEFT_PANEL).get(state.getValue(FACING));
	}

	private static boolean isPanelOnRight(BlockState state) {
		return state.getValue(HINGE) == DoorHingeSide.RIGHT;
	}

	@Override
	public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
			BlockHitResult hit) {
		ItemStack heldItem = player.getItemInHand(hand);
		if (!player.isShiftKeyDown() && AllItems.WRENCH.isIn(heldItem) && isPanelHit(state, pos, hit.getLocation()))
			return cyclePanel(state, level, pos);
		return super.use(state, level, pos, player, hand, hit);
	}

	@Override
	public InteractionResult onWrenched(BlockState state, UseOnContext context) {
		if (!isPanelHit(state, context.getClickedPos(), context.getClickLocation()))
			return InteractionResult.FAIL;
		return cyclePanel(state, context.getLevel(), context.getClickedPos());
	}

	private InteractionResult cyclePanel(BlockState state, Level level, BlockPos pos) {
		if (level.isClientSide)
			return InteractionResult.SUCCESS;

		DoubleBlockHalf half = state.getValue(HALF);
		BlockPos lowerPos = half == DoubleBlockHalf.UPPER ? pos.below() : pos;
		if (!(level.getBlockEntity(lowerPos) instanceof CabCornerDoorBlockEntity door))
			return InteractionResult.FAIL;

		door.cyclePanel(half);
		float pitch = level.getRandom().nextFloat() + .5f;
		AllSoundEvents.WRENCH_ROTATE.playOnServer(level, pos, 1, pitch);
		return InteractionResult.SUCCESS;
	}

	@Override
	public InteractionResult onSneakWrenched(BlockState state, UseOnContext context) {
		if (state.getValue(HALF) == DoubleBlockHalf.LOWER)
			return super.onSneakWrenched(state, context);

		Player player = context.getPlayer();
		if (player == null)
			return InteractionResult.PASS;

		BlockPos lowerPos = context.getClickedPos().below();
		BlockState lowerState = context.getLevel().getBlockState(lowerPos);
		if (!lowerState.is(this))
			return InteractionResult.PASS;

		UseOnContext lowerContext = new UseOnContext(player, context.getHand(),
			new net.minecraft.world.phys.BlockHitResult(Vec3.atCenterOf(lowerPos), context.getClickedFace(),
				lowerPos, false));
		return super.onSneakWrenched(lowerState, lowerContext);
	}

	private static boolean isPanelHit(BlockState state, BlockPos pos, Vec3 location) {
		Vec3 hit = location.subtract(Vec3.atLowerCornerOf(pos));
		return getPanelShape(state).toAabbs()
			.stream()
			.anyMatch(box -> box.inflate(1.0E-5D).contains(hit));
	}

	@Override
	public BlockEntityType<? extends SlidingDoorBlockEntity> getBlockEntityType() {
		return AllBlockEntities.CAB_CORNER_TRAIN_DOOR.get();
	}
}
