package ua.pp.shurgent.railward.client.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import com.simibubi.create.content.decoration.copycat.CopycatBlock;
import com.simibubi.create.foundation.model.BakedModelWrapperWithData;
import com.simibubi.create.foundation.model.BakedQuadHelper;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.model.data.EmptyModelData;
import net.minecraftforge.client.model.data.IModelData;
import net.minecraftforge.client.model.data.ModelDataMap.Builder;
import net.minecraftforge.client.model.data.ModelProperty;
import ua.pp.shurgent.railward.AllPartialModels;
import ua.pp.shurgent.railward.block.train.parts.TrainRoofPartitionBehaviour;
import ua.pp.shurgent.railward.block.train.parts.TrainRoofPartitionBehaviour.PartitionShape;

/**
 * Keeps the placed roof as an upper slab while exposing a full-cube source
 * model when Create samples it as a copycat material.
 */
public class TrainRoofModel extends BakedModelWrapperWithData {

	private static final ModelProperty<Boolean> COPYCAT_SOURCE = new ModelProperty<>();
	private static final ModelProperty<Integer> CEILING_NEIGHBOR_MASK = new ModelProperty<>();
	private static final Direction[] HORIZONTAL_DIRECTIONS = {
		Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST
	};
	private final boolean expandCopycatSource;

	public TrainRoofModel(BakedModel originalModel) {
		this(originalModel, true);
	}

	public TrainRoofModel(BakedModel originalModel, boolean expandCopycatSource) {
		super(originalModel);
		this.expandCopycatSource = expandCopycatSource;
	}

	@Override
	protected void gatherModelData(Builder builder, BlockAndTintGetter world, BlockPos pos, BlockState state,
			IModelData blockEntityData) {
		builder.withInitial(COPYCAT_SOURCE,
			expandCopycatSource && world.getBlockState(pos).getBlock() instanceof CopycatBlock);
		int ceilingNeighborMask = 0;
		for (Direction direction : HORIZONTAL_DIRECTIONS)
			if (TrainRoofPartitionBehaviour.isRoof(world.getBlockState(pos.relative(direction))))
				ceilingNeighborMask |= directionBit(direction);
		builder.withInitial(CEILING_NEIGHBOR_MASK, ceilingNeighborMask);
	}

	@Override
	public List<BakedQuad> getQuads(BlockState state, Direction side, Random random, IModelData data) {
		List<BakedQuad> baseQuads = super.getQuads(state, side, random, data);
		boolean copycatSource = Boolean.TRUE.equals(data.getData(COPYCAT_SOURCE));
		if (copycatSource && !baseQuads.isEmpty()) {
			List<BakedQuad> fullCubeQuads = new ArrayList<>(baseQuads.size());
			for (BakedQuad quad : baseQuads)
				fullCubeQuads.add(expandToFullCube(quad));
			baseQuads = fullCubeQuads;
		}

		if (copycatSource || state == null || side != null)
			return baseQuads;

		int ceilingNeighborMask = data.getData(CEILING_NEIGHBOR_MASK) == null
			? 0 : data.getData(CEILING_NEIGHBOR_MASK);
		List<BakedQuad> result = null;
		for (Direction partitionDirection : HORIZONTAL_DIRECTIONS) {
			if (!TrainRoofPartitionBehaviour.hasPartition(state, partitionDirection))
				continue;
			PartitionShape partitionShape = TrainRoofPartitionBehaviour.getPartitionShape(state, partitionDirection);
			boolean interiorExterior = (ceilingNeighborMask & directionBit(partitionDirection)) != 0;
			BakedModel panelModel = getPartitionModel(partitionShape, interiorExterior);
			if (panelModel == null)
				continue;
			List<BakedQuad> panelQuads = panelModel.getQuads(null, null, random, EmptyModelData.INSTANCE);
			if (panelQuads.isEmpty())
				continue;
			if (result == null) {
				result = new ArrayList<>(baseQuads.size() + panelQuads.size() * 2);
				result.addAll(baseQuads);
			}
			for (BakedQuad quad : panelQuads) {
				Direction rotatedFace = rotateFromNorth(quad.getDirection(), partitionDirection);
				if (isEndFace(rotatedFace, partitionDirection)
						&& TrainRoofPartitionBehaviour.hasPartition(state, rotatedFace))
					continue;
				result.add(rotateFromNorth(quad, partitionDirection));
			}
		}
		return result == null ? baseQuads : result;
	}

	private static BakedModel getPartitionModel(PartitionShape shape, boolean interiorExterior) {
		return switch (shape) {
		case FULL -> interiorExterior
			? AllPartialModels.TRAIN_ROOF_PARTITION_INTERIOR.get()
			: AllPartialModels.TRAIN_ROOF_PARTITION.get();
		case WEDGE_COUNTER_CLOCKWISE -> interiorExterior
			? AllPartialModels.TRAIN_ROOF_PARTITION_WEDGE_COUNTER_CLOCKWISE_INTERIOR.get()
			: AllPartialModels.TRAIN_ROOF_PARTITION_WEDGE_COUNTER_CLOCKWISE.get();
		case WEDGE_CLOCKWISE -> interiorExterior
			? AllPartialModels.TRAIN_ROOF_PARTITION_WEDGE_CLOCKWISE_INTERIOR.get()
			: AllPartialModels.TRAIN_ROOF_PARTITION_WEDGE_CLOCKWISE.get();
		case NONE -> null;
		};
	}

	private static BakedQuad expandToFullCube(BakedQuad quad) {
		Direction face = quad.getDirection();
		if (face == Direction.UP)
			return quad;

		int[] vertices = Arrays.copyOf(quad.getVertices(), quad.getVertices().length);
		for (int vertex = 0; vertex < 4; vertex++) {
			Vec3 position = BakedQuadHelper.getXYZ(vertices, vertex);
			double y = face == Direction.DOWN || position.y < .75 ? 0 : 1;
			BakedQuadHelper.setXYZ(vertices, vertex, new Vec3(position.x, y, position.z));

			if (face.getAxis().isHorizontal())
				BakedQuadHelper.setV(vertices, vertex, quad.getSprite().getV((1 - y) * 16));
		}

		return BakedQuadHelper.cloneWithCustomGeometry(quad, vertices);
	}

	private static BakedQuad rotateFromNorth(BakedQuad quad, Direction target) {
		int turns = quarterTurnsFromNorth(target);
		if (turns == 0)
			return quad;

		int[] vertices = Arrays.copyOf(quad.getVertices(), quad.getVertices().length);
		for (int vertex = 0; vertex < 4; vertex++) {
			Vec3 position = BakedQuadHelper.getXYZ(vertices, vertex);
			for (int turn = 0; turn < turns; turn++)
				position = new Vec3(1 - position.z, position.y, position.x);
			BakedQuadHelper.setXYZ(vertices, vertex, position);
		}

		Direction direction = rotateFromNorth(quad.getDirection(), target);
		int packedNormal = packNormal(direction);
		for (int vertex = 0; vertex < 4; vertex++)
			vertices[vertex * BakedQuadHelper.VERTEX_STRIDE + BakedQuadHelper.NORMAL_OFFSET] = packedNormal;
		return new BakedQuad(vertices, quad.getTintIndex(), direction, quad.getSprite(), quad.isShade());
	}

	private static Direction rotateFromNorth(Direction direction, Direction target) {
		if (!direction.getAxis().isHorizontal())
			return direction;
		int turns = quarterTurnsFromNorth(target);
		for (int turn = 0; turn < turns; turn++)
			direction = direction.getClockWise();
		return direction;
	}

	private static int quarterTurnsFromNorth(Direction direction) {
		return switch (direction) {
		case NORTH -> 0;
		case EAST -> 1;
		case SOUTH -> 2;
		case WEST -> 3;
		default -> throw new IllegalArgumentException("Roof partition direction must be horizontal");
		};
	}

	private static boolean isEndFace(Direction face, Direction partitionDirection) {
		return face.getAxis().isHorizontal() && face != partitionDirection
			&& face != partitionDirection.getOpposite();
	}

	private static int packNormal(Direction direction) {
		return direction.getStepX() & 0xff
			| (direction.getStepY() & 0xff) << 8
			| (direction.getStepZ() & 0xff) << 16;
	}

	private static int directionBit(Direction direction) {
		return 1 << direction.get3DDataValue();
	}
}
