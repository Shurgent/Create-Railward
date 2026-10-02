package ua.pp.shurgent.railward.client.model;

import java.util.ArrayList;
import java.util.List;

import com.simibubi.create.content.decoration.copycat.CopycatModel;
import com.simibubi.create.foundation.model.BakedModelHelper;
import com.simibubi.create.foundation.model.BakedModelWrapperWithData;
import com.simibubi.create.foundation.model.BakedQuadHelper;

import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
//? if >=1.19.2
/*import net.minecraft.util.RandomSource;*/
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
//? if >=1.19.2 {
/*import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelData.Builder;*/
//?} else {
import java.util.Random;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.client.model.data.EmptyModelData;
import net.minecraftforge.client.model.data.IModelData;
import net.minecraftforge.client.model.data.ModelDataMap.Builder;
//?}
import net.minecraftforge.client.model.data.ModelProperty;
import ua.pp.shurgent.railward.block.train.parts.copycat.CopycatSlabBlock;
import ua.pp.shurgent.railward.block.train.parts.copycat.CopycatSlabModelData;

/** Renders the two slab halves from their independently stored materials. */
public class CopycatSlabModel extends BakedModelWrapperWithData {

	//? if >=1.19.2 {
	/*private static final ModelProperty<ModelData> BOTTOM_WRAPPED_DATA = new ModelProperty<>();
	private static final ModelProperty<ModelData> TOP_WRAPPED_DATA = new ModelProperty<>();*/
	//?} else {
	private static final ModelProperty<IModelData> BOTTOM_WRAPPED_DATA = new ModelProperty<>();
	private static final ModelProperty<IModelData> TOP_WRAPPED_DATA = new ModelProperty<>();
	//?}

	public CopycatSlabModel(BakedModel originalModel) {
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
		gatherHalfData(builder, world, pos, blockEntityData,
				CopycatSlabModelData.BOTTOM_MATERIAL, BOTTOM_WRAPPED_DATA);
		gatherHalfData(builder, world, pos, blockEntityData,
				CopycatSlabModelData.TOP_MATERIAL, TOP_WRAPPED_DATA);
		//? if >=1.19.2
		/*return builder;*/
	}

	//? if >=1.19.2 {
	/*private static void gatherHalfData(ModelData.Builder builder, BlockAndTintGetter world, BlockPos pos,
			ModelData blockEntityData, ModelProperty<BlockState> materialProperty,
			ModelProperty<ModelData> wrappedProperty) {*/
	//?} else {
	private static void gatherHalfData(Builder builder, BlockAndTintGetter world, BlockPos pos,
			IModelData blockEntityData, ModelProperty<BlockState> materialProperty,
			ModelProperty<IModelData> wrappedProperty) {
	//?}
		BlockState material = blockEntityData.getData(materialProperty);
		if (material == null)
			material = com.simibubi.create.AllBlocks.COPYCAT_BASE.getDefaultState();
		builder.withInitial(materialProperty, material);
		//? if >=1.19.2
		/*ModelData wrappedData = CopycatModel.getModelOf(material)*/
		//? if <1.19.2
		IModelData wrappedData = CopycatModel.getModelOf(material)
				.getModelData(world, pos, material, EmptyModelData.INSTANCE);
		builder.withInitial(wrappedProperty, wrappedData);
	}

	@Override
	//? if >=1.19.2 {
	/*public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource random, ModelData data,
			RenderType renderType) {*/
	//?} else {
	public List<BakedQuad> getQuads(BlockState state, Direction side, Random random, IModelData data) {
	//?}
		if (state == null || !state.hasProperty(CopycatSlabBlock.TYPE))
			//? if >=1.19.2
			/*return super.getQuads(state, side, random, data, renderType);*/
			//? if <1.19.2
			return super.getQuads(state, side, random, data);

		SlabType type = state.getValue(CopycatSlabBlock.TYPE);
		List<BakedQuad> result = new ArrayList<>();
		if (type == SlabType.BOTTOM || type == SlabType.DOUBLE)
			addHalfQuads(result, state, side, random, data,
					//? if >=1.19.2
					/*renderType,*/
					Half.BOTTOM, type == SlabType.DOUBLE);
		if (type == SlabType.TOP || type == SlabType.DOUBLE)
			addHalfQuads(result, state, side, random, data,
					//? if >=1.19.2
					/*renderType,*/
					Half.TOP, type == SlabType.DOUBLE);
		return result;
	}

	//? if >=1.19.2 {
	/*private static void addHalfQuads(List<BakedQuad> result, BlockState state, Direction side, RandomSource random,
			ModelData data, RenderType renderType, Half half, boolean doubleSlab) {*/
	//?} else {
	private static void addHalfQuads(List<BakedQuad> result, BlockState state, Direction side, Random random,
			IModelData data, Half half, boolean doubleSlab) {
	//?}
		BlockState material = getMaterial(data, half);
		//? if <1.19.2
		RenderType renderType = MinecraftForgeClient.getRenderType();
		// Forge 1.19 passes the active layer into the material model directly.
		//? if <1.19.2 {
		if (renderType != null && !ItemBlockRenderTypes.canRenderInLayer(material, renderType))
			return;
		//?}

		//? if >=1.19.2
		/*ModelData wrappedData = data.getData(half == Half.TOP ? TOP_WRAPPED_DATA : BOTTOM_WRAPPED_DATA);*/
		//? if <1.19.2
		IModelData wrappedData = data.getData(half == Half.TOP ? TOP_WRAPPED_DATA : BOTTOM_WRAPPED_DATA);
		if (wrappedData == null)
			wrappedData = EmptyModelData.INSTANCE;
		//? if >=1.19.2
		/*List<BakedQuad> templates = CopycatModel.getModelOf(material)
				.getQuads(material, side, random, wrappedData, renderType);*/
		//? if <1.19.2
		List<BakedQuad> templates = CopycatModel.getModelOf(material).getQuads(material, side, random, wrappedData);
		if (templates.isEmpty())
			return;

		AABB target = half == Half.BOTTOM ? new AABB(0, 0, 0, 1, .5, 1) : new AABB(0, .5, 0, 1, 1, 1);
		List<Piece> pieces = getPieces(half);
		for (BakedQuad quad : templates) {
			Direction face = quad.getDirection();
			if (doubleSlab && (half == Half.BOTTOM && face == Direction.UP
					|| half == Half.TOP && face == Direction.DOWN))
				continue;
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
	}

	private static List<Piece> getPieces(Half half) {
		double destinationMin = half == Half.BOTTOM ? 0 : .5;
		return List.of(
				new Piece(new AABB(0, 0, 0, 1, .25, 1),
						new AABB(0, destinationMin, 0, 1, destinationMin + .25, 1)),
				new Piece(new AABB(0, .75, 0, 1, 1, 1),
						new AABB(0, destinationMin + .25, 0, 1, destinationMin + .5, 1)));
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

	@Override
	//? if >=1.19.2
	/*public TextureAtlasSprite getParticleIcon(ModelData data) {*/
	//? if <1.19.2
	public TextureAtlasSprite getParticleIcon(IModelData data) {
		BlockState material = getMaterial(data, Half.BOTTOM);
		//? if >=1.19.2
		/*ModelData wrappedData = data.getData(BOTTOM_WRAPPED_DATA);*/
		//? if <1.19.2
		IModelData wrappedData = data.getData(BOTTOM_WRAPPED_DATA);
		if (com.simibubi.create.AllBlocks.COPYCAT_BASE.has(material)) {
			material = getMaterial(data, Half.TOP);
			wrappedData = data.getData(TOP_WRAPPED_DATA);
		}
		return CopycatModel.getModelOf(material)
				.getParticleIcon(wrappedData == null ? EmptyModelData.INSTANCE : wrappedData);
	}

	//? if >=1.19.2
	/*private static BlockState getMaterial(ModelData data, Half half) {*/
	//? if <1.19.2
	private static BlockState getMaterial(IModelData data, Half half) {
		ModelProperty<BlockState> property = half == Half.TOP
				? CopycatSlabModelData.TOP_MATERIAL : CopycatSlabModelData.BOTTOM_MATERIAL;
		BlockState material = data.getData(property);
		return material == null ? com.simibubi.create.AllBlocks.COPYCAT_BASE.getDefaultState() : material;
	}

	private record Piece(AABB source, AABB destination) {
	}
}
