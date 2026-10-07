package ua.pp.shurgent.railward.block.surface;

import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.content.contraptions.BlockMovementChecks;
import com.simibubi.create.content.contraptions.BlockMovementChecks.CheckResult;
import ua.pp.shurgent.railward.compat.RailwardWrenchable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public abstract class SurfaceMountedBlock<T extends SurfaceMountedBlockEntity> extends Block
	implements IBE<T>, RailwardWrenchable {

	public static final DirectionProperty FACING = DirectionalBlock.FACING;
	private static boolean movementChecksRegistered;

	protected SurfaceMountedBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(FACING, Direction.SOUTH));
	}

	public static void registerMovementChecks() {
		if (movementChecksRegistered)
			return;
		movementChecksRegistered = true;
		BlockMovementChecks.registerBrittleCheck(state -> state.getBlock() instanceof SurfaceMountedBlock
			? CheckResult.SUCCESS : CheckResult.PASS);
		BlockMovementChecks.registerAttachedCheck((state, level, pos, direction) -> {
			if (!(state.getBlock() instanceof SurfaceMountedBlock))
				return CheckResult.PASS;
			return CheckResult.of(direction == state.getValue(FACING).getOpposite());
		});
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
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
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockState state = defaultBlockState().setValue(FACING, context.getClickedFace());
		return state.canSurvive(context.getLevel(), context.getClickedPos()) ? state : null;
	}

	@Override
	public InteractionResult onWrenched(BlockState state, UseOnContext context) {
		return InteractionResult.FAIL;
	}

	@Override
	public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		BlockPos supportPos = pos.relative(state.getValue(FACING).getOpposite());
		return !level.getBlockState(supportPos).getMaterial().isReplaceable();
	}

	@Override
	public RenderShape getRenderShape(BlockState state) {
		return RenderShape.ENTITYBLOCK_ANIMATED;
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		Vec3 anchor = SurfaceMountedBlockEntity.defaultAnchor(state.getValue(FACING));
		if (level.getBlockEntity(pos) instanceof SurfaceMountedBlockEntity mounted)
			anchor = mounted.getAnchor();
		return createShape(state.getValue(FACING), anchor, getModelWidth(), getModelHeight(), getModelDepth());
	}

	@Override
	public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return Shapes.empty();
	}

	protected abstract double getModelWidth();

	protected abstract double getModelHeight();

	protected abstract double getModelDepth();

	public static VoxelShape createShape(Direction facing, Vec3 anchor, double width, double height, double depth) {
		double minX = Double.POSITIVE_INFINITY;
		double minY = Double.POSITIVE_INFINITY;
		double minZ = Double.POSITIVE_INFINITY;
		double maxX = Double.NEGATIVE_INFINITY;
		double maxY = Double.NEGATIVE_INFINITY;
		double maxZ = Double.NEGATIVE_INFINITY;

		for (double x : new double[] { -.5 * width, .5 * width })
			for (double y : new double[] { -.5 * height, .5 * height })
				for (double z : new double[] { 0, depth }) {
					Vec3 transformed = transformOffset(facing, x, y, z).add(anchor);
					minX = Math.min(minX, transformed.x);
					minY = Math.min(minY, transformed.y);
					minZ = Math.min(minZ, transformed.z);
					maxX = Math.max(maxX, transformed.x);
					maxY = Math.max(maxY, transformed.y);
					maxZ = Math.max(maxZ, transformed.z);
				}
		return Shapes.box(minX, minY, minZ, maxX, maxY, maxZ);
	}

	private static Vec3 transformOffset(Direction facing, double x, double y, double z) {
		return switch (facing) {
		case SOUTH -> new Vec3(x, y, z);
		case NORTH -> new Vec3(-x, y, -z);
		case EAST -> new Vec3(z, y, -x);
		case WEST -> new Vec3(-z, y, x);
		case UP -> new Vec3(x, z, -y);
		case DOWN -> new Vec3(x, -z, y);
		};
	}
}
