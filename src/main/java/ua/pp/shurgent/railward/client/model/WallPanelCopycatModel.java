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
		CORNER
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
		List<BakedQuad> result = new ArrayList<>(templateQuads.size());
		for (BakedQuad quad : templateQuads) {
			Crop crop = cropForFace(target, quad.getDirection());
			result.add(BakedQuadHelper.cloneWithCustomGeometry(quad,
					BakedModelHelper.cropAndMove(quad.getVertices(), quad.getSprite(), crop.source, crop.offset)));
		}
		return result;
	}

	private AABB getTargetBounds(BlockState state) {
		return switch (shape) {
			case VERTICAL_SLICE -> WallPanelVerticalSliceBlock.getBounds(state);
			case HORIZONTAL_SLICE -> WallPanelHorizontalSliceBlock.getBounds(state);
			case CORNER -> WallPanelCornerBlock.getBounds(state);
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
}
