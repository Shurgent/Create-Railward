package ua.pp.shurgent.railward.block.train.parts.copycat;

import com.simibubi.create.content.decoration.copycat.CopycatBlockEntity;
import com.simibubi.create.content.decoration.copycat.WaterloggedCopycatBlock;
import com.simibubi.create.content.contraptions.ITransformableBlock;
import com.simibubi.create.content.contraptions.StructureTransform;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import ua.pp.shurgent.railward.AllBlocks;
import ua.pp.shurgent.railward.AllBlockEntities;

/**
 * Common Create copycat integration for the fixed-size wall panel pieces.
 *
 * <p>The block entity remains Create's {@link CopycatBlockEntity}; registering
 * our own type makes the copied material survive world saves and contraption
 * assembly while retaining Create's material transform and item requirement
 * handling.</p>
 */
public abstract class WallPanelCopycatBlock extends WaterloggedCopycatBlock implements ITransformableBlock {

	protected WallPanelCopycatBlock(Properties properties) {
		super(properties);
	}

	@Override
	public BlockEntityType<? extends CopycatBlockEntity> getBlockEntityType() {
		return AllBlockEntities.WALL_PANEL_COPYCAT.get();
	}

	@Override
	public BlockState getConnectiveMaterial(BlockAndTintGetter reader, BlockState otherState, Direction face,
			BlockPos fromPos, BlockPos toPos) {
		if (!otherState.is(this))
			return null;
		BlockState targetState = reader.getBlockState(toPos);
		if (!targetState.is(this))
			return null;
		if (!sameShapeState(otherState, targetState))
			return null;

		BlockPos offset = fromPos.subtract(toPos);
		return connectsAlong(otherState, offset) ? getMaterial(reader, toPos) : null;
	}

	protected boolean connectsAlong(BlockState state, BlockPos offset) {
		return false;
	}

	private static boolean sameShapeState(BlockState first, BlockState second) {
		return first.setValue(WATERLOGGED, false).equals(second.setValue(WATERLOGGED, false));
	}

	@Override
	public boolean isUnblockableConnectivitySide(BlockAndTintGetter reader, BlockState state, Direction face,
			BlockPos fromPos, BlockPos toPos) {
		return true;
	}

	@Override
	public boolean isPathfindable(BlockState state, BlockGetter level, BlockPos pos, PathComputationType type) {
		return false;
	}

	@Override
	public boolean supportsExternalFaceHiding(BlockState state) {
		return true;
	}

	@Override
	public boolean hidesNeighborFace(BlockGetter level, BlockPos pos, BlockState state, BlockState neighborState,
			Direction direction) {
		// None of these 3 px pieces covers a complete block face.
		return false;
	}

	protected static Direction cornerFacing(double localX, double localZ) {
		boolean highX = localX > .5;
		boolean highZ = localZ > .5;
		if (highX)
			return highZ ? Direction.SOUTH : Direction.EAST;
		return highZ ? Direction.WEST : Direction.NORTH;
	}

	protected abstract AABB getShapeBounds(BlockState state);

	@Override
	public BlockState transform(BlockState state, StructureTransform transform) {
		AABB transformed = transformBounds(getShapeBounds(state), transform);
		double sizeX = transformed.maxX - transformed.minX;
		double sizeY = transformed.maxY - transformed.minY;
		double sizeZ = transformed.maxZ - transformed.minZ;
		boolean waterlogged = state.getValue(WATERLOGGED);

		if (sizeY > .5) {
			Direction corner = cornerFacing(transformed.minX, transformed.minZ);
			return AllBlocks.WALL_PANEL_VERTICAL_SLICE.getDefaultState()
					.setValue(WallPanelVerticalSliceBlock.FACING, corner)
					.setValue(WATERLOGGED, waterlogged);
		}

		if (sizeX > .5 || sizeZ > .5) {
			Direction facing;
			if (sizeX > .5)
				facing = transformed.minZ > .5 ? Direction.SOUTH : Direction.NORTH;
			else
				facing = transformed.minX > .5 ? Direction.EAST : Direction.WEST;
			return AllBlocks.WALL_PANEL_HORIZONTAL_SLICE.getDefaultState()
					.setValue(WallPanelHorizontalSliceBlock.FACING, facing)
					.setValue(WallPanelHorizontalSliceBlock.HALF,
							transformed.minY > .5 ? Half.TOP : Half.BOTTOM)
					.setValue(WATERLOGGED, waterlogged);
		}

		return AllBlocks.WALL_PANEL_CORNER.getDefaultState()
				.setValue(WallPanelCornerBlock.FACING, cornerFacing(transformed.minX, transformed.minZ))
				.setValue(WallPanelCornerBlock.HALF, transformed.minY > .5 ? Half.TOP : Half.BOTTOM)
				.setValue(WATERLOGGED, waterlogged);
	}

	private static AABB transformBounds(AABB bounds, StructureTransform transform) {
		double minX = Double.POSITIVE_INFINITY;
		double minY = Double.POSITIVE_INFINITY;
		double minZ = Double.POSITIVE_INFINITY;
		double maxX = Double.NEGATIVE_INFINITY;
		double maxY = Double.NEGATIVE_INFINITY;
		double maxZ = Double.NEGATIVE_INFINITY;

		for (double x : new double[] { bounds.minX, bounds.maxX })
			for (double y : new double[] { bounds.minY, bounds.maxY })
				for (double z : new double[] { bounds.minZ, bounds.maxZ }) {
					Vec3 point = transform.applyWithoutOffset(new Vec3(x, y, z));
					minX = Math.min(minX, point.x);
					minY = Math.min(minY, point.y);
					minZ = Math.min(minZ, point.z);
					maxX = Math.max(maxX, point.x);
					maxY = Math.max(maxY, point.y);
					maxZ = Math.max(maxZ, point.z);
				}

		return new AABB(snap(minX), snap(minY), snap(minZ), snap(maxX), snap(maxY), snap(maxZ));
	}

	private static double snap(double value) {
		return Math.round(value * 16) / 16d;
	}
}
