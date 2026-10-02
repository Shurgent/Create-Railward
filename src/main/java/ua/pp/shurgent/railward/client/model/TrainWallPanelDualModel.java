package ua.pp.shurgent.railward.client.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.simibubi.create.content.decoration.copycat.CopycatModel;
import com.simibubi.create.foundation.model.BakedModelWrapperWithData;
import com.simibubi.create.foundation.model.BakedQuadHelper;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
//? if >=1.19.2
/*import net.minecraft.util.RandomSource;*/
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
//? if >=1.19.2 {
/*import net.minecraftforge.client.model.data.ModelData;*/
//?} else {
import java.util.Random;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.client.model.data.EmptyModelData;
import net.minecraftforge.client.model.data.IModelData;
import net.minecraftforge.client.model.data.ModelDataMap.Builder;
//?}
import ua.pp.shurgent.railward.AllBlocks;
import ua.pp.shurgent.railward.block.train.parts.TrainWallPanelBlock;
import ua.pp.shurgent.railward.block.train.parts.TrainWallPanelModelData;

/** Renders the stored train-wall-panel variant as a narrowed perpendicular side. */
public class TrainWallPanelDualModel extends BakedModelWrapperWithData {

	public TrainWallPanelDualModel(BakedModel originalModel) {
		super(originalModel);
	}

	@Override
	//? if >=1.19.2 {
	/*protected ModelData.Builder gatherModelData(ModelData.Builder builder, BlockAndTintGetter world, BlockPos pos,
			BlockState state, ModelData blockEntityData) {*/
	//?} else {
	protected void gatherModelData(Builder builder, BlockAndTintGetter world, BlockPos pos, BlockState state,
			IModelData blockEntityData) {
	//?}
		BlockState secondary = blockEntityData.getData(TrainWallPanelModelData.SECONDARY_PANEL);
		if (secondary != null)
			builder.withInitial(TrainWallPanelModelData.SECONDARY_PANEL, secondary);
		//? if >=1.19.2
		/*return builder;*/
		/*if (secondary == null)
			return;
		builder.withInitial(TrainWallPanelModelData.SECONDARY_PANEL, secondary);*/
	}

	@Override
	//? if >=1.19.2 {
	/*public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource random, ModelData data,
			RenderType renderType) {*/
	//?} else {
	public List<BakedQuad> getQuads(BlockState state, Direction side, Random random, IModelData data) {
	//?}
		if (state == null)
			//? if >=1.19.2
			/*return originalModel.getQuads(null, side, random, data, renderType);*/
			//? if <1.19.2
			return originalModel.getQuads(null, side, random, data);

		//? if <1.19.2
		RenderType renderType = MinecraftForgeClient.getRenderType();
		List<BakedQuad> primary;
		if (rendersInLayer(state, renderType))
			//? if >=1.19.2
			/*primary = originalModel.getQuads(state, side, random, data, renderType);*/
			//? if <1.19.2
			primary = originalModel.getQuads(state, side, random, data);
		else
			primary = Collections.emptyList();
		BlockState secondary = data.getData(TrainWallPanelModelData.SECONDARY_PANEL);
		if (secondary == null)
			return primary;

		if (!rendersInLayer(secondary, renderType))
			return primary;
		BakedModel secondaryModel = unwrap(CopycatModel.getModelOf(secondary));
		//? if >=1.19.2 {
		/*List<BakedQuad> secondaryQuads = secondaryModel
				.getQuads(secondary, side, random, ModelData.EMPTY, renderType);*/
		//?} else {
		List<BakedQuad> secondaryQuads = secondaryModel
				.getQuads(secondary, side, random, EmptyModelData.INSTANCE);
		//?}
		if (secondaryQuads.isEmpty())
			return primary;

		Direction.Axis lengthAxis = state.getValue(TrainWallPanelBlock.FACING).getAxis();
		AABB target = TrainWallPanelBlock.getSecondaryBounds(state,
				secondary.getValue(TrainWallPanelBlock.FACING));
		double targetMin = axisMin(target, lengthAxis);
		double targetMax = axisMax(target, lengthAxis);
		List<BakedQuad> result = new ArrayList<>(primary.size() + secondaryQuads.size());
		result.addAll(primary);
		for (BakedQuad quad : secondaryQuads) {
			int[] vertices = quad.getVertices().clone();
			for (int vertex = 0; vertex < 4; vertex++) {
				Vec3 position = BakedQuadHelper.getXYZ(vertices, vertex);
				double narrowed = targetMin + axisValue(position, lengthAxis) * (targetMax - targetMin);
				BakedQuadHelper.setXYZ(vertices, vertex, withAxis(position, lengthAxis, narrowed));
			}
			result.add(BakedQuadHelper.cloneWithCustomGeometry(quad, vertices));
		}
		return result;
	}

	private static boolean rendersInLayer(BlockState state, RenderType renderType) {
		if (renderType == null)
			return true;
		return AllBlocks.isCutoutTrainWallPanelState(state)
				? renderType == RenderType.cutout()
				: renderType == RenderType.solid();
	}

	private static BakedModel unwrap(BakedModel model) {
		while (model instanceof TrainWallPanelDualModel dualModel)
			model = dualModel.originalModel;
		return model;
	}

	private static double axisMin(AABB box, Direction.Axis axis) {
		return axis.choose(box.minX, box.minY, box.minZ);
	}

	private static double axisMax(AABB box, Direction.Axis axis) {
		return axis.choose(box.maxX, box.maxY, box.maxZ);
	}

	private static double axisValue(Vec3 value, Direction.Axis axis) {
		return axis.choose(value.x, value.y, value.z);
	}

	private static Vec3 withAxis(Vec3 value, Direction.Axis axis, double coordinate) {
		return switch (axis) {
			case X -> new Vec3(coordinate, value.y, value.z);
			case Y -> new Vec3(value.x, coordinate, value.z);
			case Z -> new Vec3(value.x, value.y, coordinate);
		};
	}
}
