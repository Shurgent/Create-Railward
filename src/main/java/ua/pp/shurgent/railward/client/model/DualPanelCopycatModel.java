package ua.pp.shurgent.railward.client.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.simibubi.create.content.decoration.copycat.CopycatModel;
import com.simibubi.create.foundation.model.BakedModelHelper;
import com.simibubi.create.foundation.model.BakedModelWrapperWithData;
import com.simibubi.create.foundation.model.BakedQuadHelper;

import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.client.model.data.EmptyModelData;
import net.minecraftforge.client.model.data.IModelData;
import net.minecraftforge.client.model.data.ModelDataMap.Builder;
import ua.pp.shurgent.railward.block.train.parts.copycat.DualPanelCopycatBehaviour;
import ua.pp.shurgent.railward.block.train.parts.copycat.DualPanelCopycatModelData;
import ua.pp.shurgent.railward.block.train.parts.copycat.HalfWallPanelBlock;
import ua.pp.shurgent.railward.block.train.parts.copycat.CopycatWallPanelBlock;

/** Adds the independently textured 13 px perpendicular panel to an existing copycat model. */
public class DualPanelCopycatModel extends BakedModelWrapperWithData {

	public DualPanelCopycatModel(BakedModel originalModel) {
		super(originalModel);
	}

	@Override
	protected void gatherModelData(Builder builder, BlockAndTintGetter world, BlockPos pos, BlockState state,
			IModelData blockEntityData) {
		Direction facing = blockEntityData.getData(DualPanelCopycatModelData.SECONDARY_FACING);
		BlockState material = blockEntityData.getData(DualPanelCopycatModelData.SECONDARY_MATERIAL);
		if (facing == null || material == null)
			return;
		builder.withInitial(DualPanelCopycatModelData.SECONDARY_FACING, facing);
		builder.withInitial(DualPanelCopycatModelData.SECONDARY_MATERIAL, material);
		IModelData wrapped = CopycatModel.getModelOf(material)
				.getModelData(world, pos, material, EmptyModelData.INSTANCE);
		builder.withInitial(DualPanelCopycatModelData.SECONDARY_WRAPPED_DATA, wrapped);
	}

	@Override
	public List<BakedQuad> getQuads(BlockState state, Direction side, Random random, IModelData data) {
		if (state == null)
			return originalModel.getQuads(null, side, random, data);
		Direction facing = data.getData(DualPanelCopycatModelData.SECONDARY_FACING);
		BlockState material = data.getData(DualPanelCopycatModelData.SECONDARY_MATERIAL);
		List<BakedQuad> primary = facing == null
				? originalModel.getQuads(state, side, random, data)
				: getPrimaryQuads(state, side, random, data);
		if (facing == null || material == null)
			return primary;

		RenderType renderType = MinecraftForgeClient.getRenderType();
		if (renderType != null && !ItemBlockRenderTypes.canRenderInLayer(material, renderType))
			return primary;
		IModelData wrapped = data.getData(DualPanelCopycatModelData.SECONDARY_WRAPPED_DATA);
		if (wrapped == null)
			wrapped = EmptyModelData.INSTANCE;

		/*
		 * The copied inner face is physically inside the block, but its source quad is
		 * still assigned to the matching block cull face. WorldModelBuilder checks that
		 * cull face against the neighbouring block while baking a contraption and can
		 * consequently discard the face. Static chunk rendering does not expose this in
		 * the common case, which made the issue appear train-only.
		 *
		 * Move that one face to the unculled (side == null) batch. This mirrors the
		 * workaround used by Create's CopycatModel for faces which have been moved away
		 * from the edge of their block space.
		 */
		Direction innerFace = facing.getOpposite();
		if (side == innerFace)
			return primary;

		BakedModel materialModel = CopycatModel.getModelOf(material);
		List<BakedQuad> templates = materialModel.getQuads(material, side, random, wrapped);
		if (side == null) {
			List<BakedQuad> innerTemplates = materialModel.getQuads(material, innerFace, random, wrapped);
			if (!innerTemplates.isEmpty()) {
				templates = new ArrayList<>(templates);
				templates.addAll(innerTemplates);
			}
		}
		if (templates.isEmpty())
			return primary;

		AABB target = DualPanelCopycatBehaviour.getSecondaryBounds(state, facing);
		List<Piece> pieces = getPieces(state, target, facing);
		List<BakedQuad> result = new ArrayList<>(primary.size() + templates.size() * pieces.size());
		result.addAll(primary);
		for (BakedQuad quad : templates) {
			Direction face = quad.getDirection();
			for (Piece piece : pieces) {
				if (!isOuterFace(piece.destination, target, face))
					continue;
				result.add(BakedQuadHelper.cloneWithCustomGeometry(quad,
						cropMoveAndScale(quad, piece.source, piece.destination)));
			}
		}
		return result;
	}

	private List<BakedQuad> getPrimaryQuads(BlockState state, Direction side, Random random, IModelData data) {
		Direction facing = state.getBlock() instanceof HalfWallPanelBlock
				? state.getValue(HalfWallPanelBlock.FACING)
				: state.getValue(CopycatWallPanelBlock.FACING);
		Direction innerFace = facing.getOpposite();
		if (side == innerFace)
			return List.of();

		List<BakedQuad> quads = originalModel.getQuads(state, side, random, data);
		if (side != null)
			return quads;

		List<BakedQuad> innerQuads = originalModel.getQuads(state, innerFace, random, data);
		if (innerQuads.isEmpty())
			return quads;
		List<BakedQuad> result = new ArrayList<>(quads.size() + innerQuads.size());
		result.addAll(quads);
		result.addAll(innerQuads);
		return result;
	}

	private static List<Piece> getPieces(BlockState state, AABB target, Direction secondaryFacing) {
		Direction.Axis primaryAxis = state.getBlock() instanceof HalfWallPanelBlock
				? state.getValue(HalfWallPanelBlock.FACING).getAxis()
				: state.getValue(CopycatWallPanelBlock.FACING).getAxis();
		Direction.Axis normalAxis = secondaryFacing.getAxis();
		Direction.Axis remainingAxis = remainingAxis(primaryAxis, normalAxis);

		List<Segment> remainingSegments = new ArrayList<>(2);
		if (state.getBlock() instanceof HalfWallPanelBlock && remainingAxis == Direction.Axis.Y) {
			remainingSegments.add(new Segment(target.minY, target.minY + .25, 0, .25));
			remainingSegments.add(new Segment(target.minY + .25, target.maxY, .75, 1));
		} else {
			remainingSegments.add(new Segment(axisMin(target, remainingAxis), axisMax(target, remainingAxis),
					axisMin(target, remainingAxis), axisMax(target, remainingAxis)));
		}

		double normalMin = axisMin(target, normalAxis);
		double normalMax = axisMax(target, normalAxis);
		boolean lowSide = normalMin < .25;
		Segment outer = lowSide
				? new Segment(normalMin, normalMin + 1 / 16d, 0, 1 / 16d)
				: new Segment(normalMax - 1 / 16d, normalMax, 15 / 16d, 1);
		Segment inner = lowSide
				? new Segment(normalMin + 1 / 16d, normalMax, 14 / 16d, 1)
				: new Segment(normalMin, normalMax - 1 / 16d, 0, 2 / 16d);
		Segment length = new Segment(axisMin(target, primaryAxis), axisMax(target, primaryAxis), 0, 1);

		List<Piece> pieces = new ArrayList<>(remainingSegments.size() * 2);
		for (Segment remaining : remainingSegments)
			for (Segment normal : new Segment[] { outer, inner }) {
				double[] sourceMin = { 0, 0, 0 };
				double[] sourceMax = { 1, 1, 1 };
				double[] destinationMin = { target.minX, target.minY, target.minZ };
				double[] destinationMax = { target.maxX, target.maxY, target.maxZ };
				set(sourceMin, sourceMax, primaryAxis, length.sourceMin, length.sourceMax);
				set(destinationMin, destinationMax, primaryAxis, length.destinationMin, length.destinationMax);
				set(sourceMin, sourceMax, normalAxis, normal.sourceMin, normal.sourceMax);
				set(destinationMin, destinationMax, normalAxis, normal.destinationMin, normal.destinationMax);
				set(sourceMin, sourceMax, remainingAxis, remaining.sourceMin, remaining.sourceMax);
				set(destinationMin, destinationMax, remainingAxis,
						remaining.destinationMin, remaining.destinationMax);
				pieces.add(new Piece(toBox(sourceMin, sourceMax), toBox(destinationMin, destinationMax)));
			}
		return pieces;
	}

	private static int[] cropMoveAndScale(BakedQuad quad, AABB source, AABB destination) {
		Vec3 offset = new Vec3(destination.minX - source.minX, destination.minY - source.minY,
				destination.minZ - source.minZ);
		int[] vertices = BakedModelHelper.cropAndMove(quad.getVertices(), quad.getSprite(), source, offset);
		AABB movedSource = source.move(offset);
		for (int vertex = 0; vertex < 4; vertex++) {
			Vec3 position = BakedQuadHelper.getXYZ(vertices, vertex);
			BakedQuadHelper.setXYZ(vertices, vertex, new Vec3(
					scale(position.x, movedSource.minX, movedSource.maxX, destination.minX, destination.maxX),
					scale(position.y, movedSource.minY, movedSource.maxY, destination.minY, destination.maxY),
					scale(position.z, movedSource.minZ, movedSource.maxZ, destination.minZ, destination.maxZ)));
		}
		return vertices;
	}

	private static double scale(double value, double sourceMin, double sourceMax, double targetMin,
			double targetMax) {
		if (Math.abs(sourceMax - sourceMin) < .00001)
			return targetMin;
		return targetMin + (value - sourceMin) / (sourceMax - sourceMin) * (targetMax - targetMin);
	}

	private static boolean isOuterFace(AABB piece, AABB target, Direction face) {
		double pieceFace = switch (face) {
			case WEST -> piece.minX;
			case EAST -> piece.maxX;
			case DOWN -> piece.minY;
			case UP -> piece.maxY;
			case NORTH -> piece.minZ;
			case SOUTH -> piece.maxZ;
		};
		double targetFace = switch (face) {
			case WEST -> target.minX;
			case EAST -> target.maxX;
			case DOWN -> target.minY;
			case UP -> target.maxY;
			case NORTH -> target.minZ;
			case SOUTH -> target.maxZ;
		};
		return Math.abs(pieceFace - targetFace) < .001;
	}

	private static Direction.Axis remainingAxis(Direction.Axis first, Direction.Axis second) {
		for (Direction.Axis axis : Direction.Axis.values())
			if (axis != first && axis != second)
				return axis;
		throw new IllegalArgumentException("Parallel panel axes");
	}

	private static void set(double[] min, double[] max, Direction.Axis axis, double newMin, double newMax) {
		int index = axis.ordinal();
		min[index] = newMin;
		max[index] = newMax;
	}

	private static AABB toBox(double[] min, double[] max) {
		return new AABB(min[0], min[1], min[2], max[0], max[1], max[2]);
	}

	private static double axisMin(AABB box, Direction.Axis axis) {
		return axis.choose(box.minX, box.minY, box.minZ);
	}

	private static double axisMax(AABB box, Direction.Axis axis) {
		return axis.choose(box.maxX, box.maxY, box.maxZ);
	}

	private record Segment(double destinationMin, double destinationMax, double sourceMin, double sourceMax) {
	}

	private record Piece(AABB source, AABB destination) {
	}
}
