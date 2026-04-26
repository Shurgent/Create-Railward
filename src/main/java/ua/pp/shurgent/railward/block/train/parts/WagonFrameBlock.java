package ua.pp.shurgent.railward.block.train.parts;

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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import ua.pp.shurgent.railward.AllBlockEntities;

public class WagonFrameBlock extends HorizontalDirectionalBlock implements IBE<WagonFrameBlockEntity>, IWrenchable {

	public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

	private static final VoxelShape COLLISION_SHAPE = Block.box(0, 8, 0, 16, 16, 16);
	private static final VoxelShape CENTER_ROTATE_SHAPE = Block.box(5, 5, 5, 11, 11, 11);
	private static final VoxelShape CENTER_ROTATE_HIGHLIGHT_SHAPE = Shapes.or(CENTER_ROTATE_SHAPE,
		Block.box(5, 8, 5, 11, 16, 11));
	private static final VoxelShape MEMBER_FRONT_SHAPE = Block.box(0, 8, 0, 16, 16, 4);
	private static final VoxelShape EQUIPMENT_SHAPE = Block.box(4, 0, 0, 12, 8, 16);
	private static final VoxelShape HAND_BRAKE_LEFT_SHAPE = Block.box(0, 1, 5, 2, 7, 11);
	private static final VoxelShape HAND_BRAKE_RIGHT_SHAPE = Block.box(14, 1, 5, 16, 7, 11);
	private static final VoxelShaper MEMBER_FRONT_SHAPER = VoxelShaper.forDirectional(MEMBER_FRONT_SHAPE,
		Direction.NORTH);
	private static final VoxelShaper EQUIPMENT_SHAPER = VoxelShaper.forDirectional(EQUIPMENT_SHAPE, Direction.NORTH);
	private static final VoxelShaper HAND_BRAKE_LEFT_SHAPER = VoxelShaper.forDirectional(HAND_BRAKE_LEFT_SHAPE,
		Direction.NORTH);
	private static final VoxelShaper HAND_BRAKE_RIGHT_SHAPER = VoxelShaper.forDirectional(HAND_BRAKE_RIGHT_SHAPE,
		Direction.NORTH);

	public WagonFrameBlock(Properties properties) {
		super(BlockBehaviour.Properties.of(Material.METAL)
			.strength(1.0f, 6f)
			.sound(SoundType.METAL)
			.noOcclusion());
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
	public RenderShape getRenderShape(BlockState state) {
		return RenderShape.ENTITYBLOCK_ANIMATED;
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		Direction facing = state.getValue(FACING);
		return Shapes.or(COLLISION_SHAPE, CENTER_ROTATE_SHAPE, EQUIPMENT_SHAPER.get(facing),
			HAND_BRAKE_LEFT_SHAPER.get(facing), HAND_BRAKE_RIGHT_SHAPER.get(facing));
	}

	@Override
	public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return COLLISION_SHAPE;
	}

	public VoxelShape getHighlightedInteractionShape(BlockState state, BlockPos pos, Vec3 hit) {
		Direction facing = state.getValue(FACING);
		return switch (resolveInteractionZone(state, pos, hit)) {
		case ROTATION -> CENTER_ROTATE_HIGHLIGHT_SHAPE;
		case MEMBER_FRONT -> MEMBER_FRONT_SHAPER.get(facing);
		case MEMBER_RIGHT -> MEMBER_FRONT_SHAPER.get(facing.getClockWise());
		case MEMBER_BACK -> MEMBER_FRONT_SHAPER.get(facing.getOpposite());
		case MEMBER_LEFT -> MEMBER_FRONT_SHAPER.get(facing.getCounterClockWise());
		case EQUIPMENT -> EQUIPMENT_SHAPER.get(facing);
		case HAND_BRAKE_LEFT -> HAND_BRAKE_LEFT_SHAPER.get(facing);
		case HAND_BRAKE_RIGHT -> HAND_BRAKE_RIGHT_SHAPER.get(facing);
		default -> Shapes.empty();
		};
	}

	@Override
	public InteractionResult onWrenched(BlockState state, UseOnContext context) {
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();

		if (level.isClientSide)
			return InteractionResult.SUCCESS;

		InteractionZone zone = resolveInteractionZone(state, pos, context.getClickLocation());
		if (zone == InteractionZone.NONE)
			return InteractionResult.SUCCESS;

		if (zone == InteractionZone.ROTATION) {
			level.setBlock(pos, state.setValue(FACING, state.getValue(FACING).getClockWise()), 3);
			playRotateSound(level, pos);
			return InteractionResult.SUCCESS;
		}

		if (!(level.getBlockEntity(pos) instanceof WagonFrameBlockEntity wagonFrame))
			return InteractionResult.SUCCESS;

		switch (zone) {
		case MEMBER_FRONT -> wagonFrame.cycleMember(WagonFrameSide.FRONT);
		case MEMBER_RIGHT -> wagonFrame.cycleMember(WagonFrameSide.RIGHT);
		case MEMBER_BACK -> wagonFrame.cycleMember(WagonFrameSide.BACK);
		case MEMBER_LEFT -> wagonFrame.cycleMember(WagonFrameSide.LEFT);
		case EQUIPMENT -> wagonFrame.cycleEquipment();
		case HAND_BRAKE_LEFT -> wagonFrame.toggleHandBrakeValve(WagonFrameHandBrakeValveSide.LEFT);
		case HAND_BRAKE_RIGHT -> wagonFrame.toggleHandBrakeValve(WagonFrameHandBrakeValveSide.RIGHT);
		default -> {
			return InteractionResult.SUCCESS;
		}
		}

		playRotateSound(level, pos);
		return InteractionResult.SUCCESS;
	}

	private InteractionZone resolveInteractionZone(BlockState state, BlockPos pos, Vec3 hit) {
		Vec3 local = toFacingLocal(hit.subtract(pos.getX(), pos.getY(), pos.getZ()), state.getValue(FACING));
		double x = local.x * 16d;
		double y = local.y * 16d;
		double z = local.z * 16d;

		if (inside(x, y, z, 5, 5, 5, 11, 11, 11) || inside(x, y, z, 5, 8, 5, 11, 16, 11))
			return InteractionZone.ROTATION;
		if (inside(x, y, z, 0, 1, 5, 2, 7, 11))
			return InteractionZone.HAND_BRAKE_LEFT;
		if (inside(x, y, z, 14, 1, 5, 16, 7, 11))
			return InteractionZone.HAND_BRAKE_RIGHT;
		if (inside(x, y, z, 4, 0, 0, 12, 8, 16))
			return InteractionZone.EQUIPMENT;
		if (y >= 8 && y <= 16)
			return resolveMemberZone(x, z);
		return InteractionZone.NONE;
	}

	private Vec3 toFacingLocal(Vec3 hit, Direction facing) {
		return switch (facing) {
		case SOUTH -> new Vec3(1 - hit.x, hit.y, 1 - hit.z);
		case EAST -> new Vec3(hit.z, hit.y, 1 - hit.x);
		case WEST -> new Vec3(1 - hit.z, hit.y, hit.x);
		default -> hit;
		};
	}

	private boolean inside(double x, double y, double z, double minX, double minY, double minZ, double maxX,
		double maxY, double maxZ) {
		return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
	}

	private InteractionZone resolveMemberZone(double x, double z) {
		double front = z;
		double right = 16 - x;
		double back = 16 - z;
		double left = x;
		double nearest = Math.min(Math.min(front, right), Math.min(back, left));
		if (nearest > 4)
			return InteractionZone.NONE;
		if (nearest == front)
			return InteractionZone.MEMBER_FRONT;
		if (nearest == right)
			return InteractionZone.MEMBER_RIGHT;
		if (nearest == back)
			return InteractionZone.MEMBER_BACK;
		return InteractionZone.MEMBER_LEFT;
	}

	@Override
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
		IBE.onRemove(state, level, pos, newState);
	}

	@Override
	public Class<WagonFrameBlockEntity> getBlockEntityClass() {
		return WagonFrameBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends WagonFrameBlockEntity> getBlockEntityType() {
		return AllBlockEntities.WAGON_FRAME.get();
	}

	private enum InteractionZone {
		NONE,
		ROTATION,
		MEMBER_FRONT,
		MEMBER_RIGHT,
		MEMBER_BACK,
		MEMBER_LEFT,
		EQUIPMENT,
		HAND_BRAKE_LEFT,
		HAND_BRAKE_RIGHT
	}
}
