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
import net.minecraftforge.client.model.data.IModelData;
import net.minecraftforge.client.model.data.ModelDataMap.Builder;
import net.minecraftforge.client.model.data.ModelProperty;

/**
 * Keeps the placed roof as an upper slab while exposing a full-cube source
 * model when Create samples it as a copycat material.
 */
public class TrainRoofModel extends BakedModelWrapperWithData {

	private static final ModelProperty<Boolean> COPYCAT_SOURCE = new ModelProperty<>();

	public TrainRoofModel(BakedModel originalModel) {
		super(originalModel);
	}

	@Override
	protected void gatherModelData(Builder builder, BlockAndTintGetter world, BlockPos pos, BlockState state,
			IModelData blockEntityData) {
		builder.withInitial(COPYCAT_SOURCE, world.getBlockState(pos).getBlock() instanceof CopycatBlock);
	}

	@Override
	public List<BakedQuad> getQuads(BlockState state, Direction side, Random random, IModelData data) {
		List<BakedQuad> slabQuads = super.getQuads(state, side, random, data);
		if (!Boolean.TRUE.equals(data.getData(COPYCAT_SOURCE)) || slabQuads.isEmpty())
			return slabQuads;

		List<BakedQuad> fullCubeQuads = new ArrayList<>(slabQuads.size());
		for (BakedQuad quad : slabQuads)
			fullCubeQuads.add(expandToFullCube(quad));
		return fullCubeQuads;
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
}
