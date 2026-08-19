package ua.pp.shurgent.railward.client.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.simibubi.create.content.decoration.copycat.CopycatModel;
import com.simibubi.create.foundation.model.BakedModelHelper;
import com.simibubi.create.foundation.model.BakedQuadHelper;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.model.data.IModelData;
import ua.pp.shurgent.railward.block.train.parts.copycat.HalfWallPanelBlock;
import ua.pp.shurgent.railward.block.train.parts.copycat.CopycatWallPanelBlock;
import ua.pp.shurgent.railward.block.train.parts.copycat.WallPanelCornerBlock;
import ua.pp.shurgent.railward.block.train.parts.copycat.WallPanelHorizontalSliceBlock;
import ua.pp.shurgent.railward.block.train.parts.copycat.WallPanelVerticalSliceBlock;

/**
 * Crops the copied block model onto one fixed 3 px cuboid.
 *
 * <p>Each face is sampled from the matching outer face of the source model and
 * moved onto the target cuboid. This keeps all six faces visible even when the
 * target face lies inside the block space.</p>
 */
public class WallPanelCopycatModel extends CopycatModel {

	public enum Shape {
		VERTICAL_SLICE,
		HORIZONTAL_SLICE,
		CORNER,
		WALL_PANEL,
		HALF_PANEL
	}

	private final Shape shape;

	public WallPanelCopycatModel(BakedModel originalModel, Shape shape) {
		super(originalModel);
		this.shape = shape;
	}

	@Override
	protected List<BakedQuad> getCroppedQuads(BlockState state, Direction side, Random random, BlockState material,
			IModelData wrappedData) {
		BakedModel materialModel = getModelOf(material);
		List<BakedQuad> templateQuads = materialModel.getQuads(material, side, random, wrappedData);
		if (templateQuads.isEmpty())
			return templateQuads;

		AABB target = getTargetBounds(state);
		if (shape == Shape.HALF_PANEL)
			return getHalfPanelQuads(templateQuads, target);
		if (shape == Shape.WALL_PANEL)
			return getFullPanelQuads(templateQuads, target);

		List<BakedQuad> result = new ArrayList<>(templateQuads.size());
		for (BakedQuad quad : templateQuads) {
			Crop crop = cropForFace(target, quad.getDirection());
			result.add(BakedQuadHelper.cloneWithCustomGeometry(quad,
					BakedModelHelper.cropAndMove(quad.getVertices(), quad.getSprite(), crop.source, crop.offset)));
		}
		return result;
	}

	/**
	 * A half panel cannot use one continuous crop of the source model: that would
	 * put the source texture's middle across its cut edge. Copycats+ solves this
	 * by assembling the shape from four edge crops. We use the same layout,
	 * adapted to a 3 px panel: two 4 px-high strips and 1 px + 2 px depth strips.
	 */
	private static List<BakedQuad> getHalfPanelQuads(List<BakedQuad> templateQuads, AABB target) {
		List<Piece> pieces = getHalfPanelPieces(target);
		List<BakedQuad> result = new ArrayList<>(templateQuads.size() * 2);

		for (BakedQuad quad : templateQuads) {
			Direction face = quad.getDirection();
			for (Piece piece : pieces) {
				if (!isOuterFace(piece.destination, target, face))
					continue;

				Vec3 offset = new Vec3(
						piece.destination.minX - piece.source.minX,
						piece.destination.minY - piece.source.minY,
						piece.destination.minZ - piece.source.minZ);
				result.add(BakedQuadHelper.cloneWithCustomGeometry(quad,
						BakedModelHelper.cropAndMove(quad.getVertices(), quad.getSprite(), piece.source, offset)));
			}
		}

		return result;
	}

	private static List<BakedQuad> getFullPanelQuads(List<BakedQuad> templateQuads, AABB target) {
		List<Piece> pieces = getPanelPieces(target, false);
		return renderPieces(templateQuads, target, pieces);
	}

	private static List<Piece> getHalfPanelPieces(AABB target) {
		return getPanelPieces(target, true);
	}

	private static List<Piece> getPanelPieces(AABB target, boolean halfHeight) {
		double middleY = target.minY + .25;
		Segment[] vertical = halfHeight ? new Segment[] {
				new Segment(target.minY, middleY, 0, .25),
				new Segment(middleY, target.maxY, .75, 1)
		} : new Segment[] { new Segment(target.minY, target.maxY, 0, 1) };

		boolean normalIsX = target.maxX - target.minX < .5;
		double normalMin = normalIsX ? target.minX : target.minZ;
		double normalMax = normalIsX ? target.maxX : target.maxZ;
		boolean positiveSide = normalMin > .5;
		Segment outer = positiveSide
				? new Segment(normalMax - 1 / 16d, normalMax, 15 / 16d, 1)
				: new Segment(normalMin, normalMin + 1 / 16d, 0, 1 / 16d);
		Segment inner = positiveSide
				? new Segment(normalMin, normalMax - 1 / 16d, 0, 2 / 16d)
				: new Segment(normalMin + 1 / 16d, normalMax, 14 / 16d, 1);

		List<Piece> pieces = new ArrayList<>(4);
		for (Segment y : vertical)
			for (Segment normal : new Segment[] { outer, inner }) {
				AABB source;
				AABB destination;
				if (normalIsX) {
					source = new AABB(normal.sourceMin, y.sourceMin, 0,
							normal.sourceMax, y.sourceMax, 1);
					destination = new AABB(normal.destinationMin, y.destinationMin, 0,
							normal.destinationMax, y.destinationMax, 1);
				} else {
					source = new AABB(0, y.sourceMin, normal.sourceMin,
							1, y.sourceMax, normal.sourceMax);
					destination = new AABB(0, y.destinationMin, normal.destinationMin,
							1, y.destinationMax, normal.destinationMax);
				}
				pieces.add(new Piece(source, destination));
			}
		return pieces;
	}

	private static List<BakedQuad> renderPieces(List<BakedQuad> templateQuads, AABB target, List<Piece> pieces) {
		List<BakedQuad> result = new ArrayList<>(templateQuads.size() * 2);
		for (BakedQuad quad : templateQuads) {
			Direction face = quad.getDirection();
			for (Piece piece : pieces) {
				if (!isOuterFace(piece.destination, target, face))
					continue;
				Vec3 offset = new Vec3(piece.destination.minX - piece.source.minX,
						piece.destination.minY - piece.source.minY,
						piece.destination.minZ - piece.source.minZ);
				result.add(BakedQuadHelper.cloneWithCustomGeometry(quad,
						BakedModelHelper.cropAndMove(quad.getVertices(), quad.getSprite(), piece.source, offset)));
			}
		}
		return result;
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

	private AABB getTargetBounds(BlockState state) {
		return switch (shape) {
			case VERTICAL_SLICE -> WallPanelVerticalSliceBlock.getBounds(state);
			case HORIZONTAL_SLICE -> WallPanelHorizontalSliceBlock.getBounds(state);
			case CORNER -> WallPanelCornerBlock.getBounds(state);
			case WALL_PANEL -> CopycatWallPanelBlock.getBounds(state);
			case HALF_PANEL -> HalfWallPanelBlock.getBounds(state);
		};
	}

	private static Crop cropForFace(AABB target, Direction face) {
		double width = target.maxX - target.minX;
		double height = target.maxY - target.minY;
		double depth = target.maxZ - target.minZ;

		return switch (face) {
			case WEST -> new Crop(
					new AABB(0, target.minY, target.minZ, width, target.maxY, target.maxZ),
					new Vec3(target.minX, 0, 0));
			case EAST -> new Crop(
					new AABB(1 - width, target.minY, target.minZ, 1, target.maxY, target.maxZ),
					new Vec3(target.maxX - 1, 0, 0));
			case DOWN -> new Crop(
					new AABB(target.minX, 0, target.minZ, target.maxX, height, target.maxZ),
					new Vec3(0, target.minY, 0));
			case UP -> new Crop(
					new AABB(target.minX, 1 - height, target.minZ, target.maxX, 1, target.maxZ),
					new Vec3(0, target.maxY - 1, 0));
			case NORTH -> new Crop(
					new AABB(target.minX, target.minY, 0, target.maxX, target.maxY, depth),
					new Vec3(0, 0, target.minZ));
			case SOUTH -> new Crop(
					new AABB(target.minX, target.minY, 1 - depth, target.maxX, target.maxY, 1),
					new Vec3(0, 0, target.maxZ - 1));
		};
	}

	private record Crop(AABB source, Vec3 offset) {
	}

	private record Segment(double destinationMin, double destinationMax, double sourceMin, double sourceMax) {
	}

	private record Piece(AABB source, AABB destination) {
	}
}
