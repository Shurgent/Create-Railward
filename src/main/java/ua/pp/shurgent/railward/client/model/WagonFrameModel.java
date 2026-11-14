package ua.pp.shurgent.railward.client.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.jozufozu.flywheel.core.PartialModel;
import com.simibubi.create.foundation.model.BakedQuadHelper;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.model.BakedModelWrapper;
//? if >=1.19.2 {
/*import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.RandomSource;
import net.minecraftforge.client.model.data.ModelData;
*///?} else {
import java.util.Random;
import net.minecraftforge.client.model.data.EmptyModelData;
import net.minecraftforge.client.model.data.IModelData;
//?}
import ua.pp.shurgent.railward.AllPartialModels;
import ua.pp.shurgent.railward.block.train.parts.WagonFrameBlock;
import ua.pp.shurgent.railward.block.train.parts.WagonFrameBlockEntity;
import ua.pp.shurgent.railward.block.train.parts.WagonFrameBlockEntity.FrameConfiguration;
import ua.pp.shurgent.railward.block.train.parts.WagonFrameHandBrakeValveSide;
import ua.pp.shurgent.railward.block.train.parts.WagonFrameSide;

public class WagonFrameModel extends BakedModelWrapper<BakedModel> {

	private record OrientedPart(PartialModel model, Direction facing) {}

	// Each baked model owns its cache so resource reloads discard quads from the old atlas.
	private final Map<OrientedPart, List<BakedQuad>> parts = new ConcurrentHashMap<>();

	public WagonFrameModel(BakedModel originalModel) {
		super(originalModel);
	}

	@Override
	//? if >=1.19.2 {
	/*public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource random, ModelData data,
			RenderType renderType) {
		List<BakedQuad> base = super.getQuads(state, side, random, data, renderType);
		if (renderType != null && renderType != RenderType.solid())
			return base;
	*///?} else {
	public List<BakedQuad> getQuads(BlockState state, Direction side, Random random, IModelData data) {
		List<BakedQuad> base = super.getQuads(state, side, random, data);
	//?}
		if (state == null || side != null)
			return base;

		FrameConfiguration configuration = data.getData(WagonFrameBlockEntity.FRAME_CONFIGURATION);
		if (configuration == null)
			return base;

		List<BakedQuad> quads = new ArrayList<>(base);
		Direction facing = state.getValue(WagonFrameBlock.FACING);
		for (WagonFrameSide memberSide : WagonFrameSide.values()) {
			PartialModel member = switch (configuration.member(memberSide)) {
				case NONE -> null;
				case THIN -> AllPartialModels.WAGON_FRAME_MEMBER_THIN;
				case THICK -> AllPartialModels.WAGON_FRAME_MEMBER_THICK;
				case TAPER_THIN_TO_THICK -> AllPartialModels.WAGON_FRAME_MEMBER_TAPER_THIN_TO_THICK;
				case TAPER_THICK_TO_THIN -> AllPartialModels.WAGON_FRAME_MEMBER_TAPER_THICK_TO_THIN;
			};
			Direction memberFacing = switch (memberSide) {
				case FRONT -> facing;
				case RIGHT -> facing.getClockWise();
				case BACK -> facing.getOpposite();
				case LEFT -> facing.getCounterClockWise();
			};
			append(quads, member, memberFacing);
		}
		PartialModel equipment = switch (configuration.equipment()) {
			case NONE -> null;
			case RECEIVER -> AllPartialModels.WAGON_FRAME_RECEIVER;
			case BRAKE_CYLINDER -> AllPartialModels.WAGON_FRAME_BRAKE_CYLINDER;
			case DISTRIBUTOR -> AllPartialModels.WAGON_FRAME_DISTRIBUTOR;
			case BRAKE_PIPE -> AllPartialModels.WAGON_FRAME_BRAKE_PIPE;
			case FUEL_TANK -> AllPartialModels.WAGON_FRAME_FUEL_TANK;
		};
		append(quads, equipment, facing);
		if (configuration.handBrakeValve() != WagonFrameHandBrakeValveSide.NONE)
			append(quads, AllPartialModels.WAGON_FRAME_HAND_BRAKE_VALVE,
				configuration.handBrakeValve() == WagonFrameHandBrakeValveSide.RIGHT ? facing.getOpposite() : facing);
		return quads;
	}

	private void append(List<BakedQuad> quads, PartialModel model, Direction facing) {
		if (model != null)
			quads.addAll(parts.computeIfAbsent(new OrientedPart(model, facing), WagonFrameModel::bakePart));
	}

	private static List<BakedQuad> bakePart(OrientedPart part) {
		//? if >=1.19.2 {
		/*List<BakedQuad> quads = part.model().get().getQuads(null, null, RandomSource.create(42),
			ModelData.EMPTY, null);
		*///?} else {
		List<BakedQuad> quads = part.model().get().getQuads(null, null, new Random(42), EmptyModelData.INSTANCE);
		//?}
		int turns = Math.floorMod(part.facing().get2DDataValue() - Direction.NORTH.get2DDataValue(), 4);
		if (turns == 0)
			return List.copyOf(quads);
		return quads.stream().map(quad -> rotate(quad, turns)).toList();
	}

	private static BakedQuad rotate(BakedQuad quad, int turns) {
		int[] vertices = quad.getVertices().clone();
		for (int vertex = 0; vertex < 4; vertex++) {
			Vec3 position = BakedQuadHelper.getXYZ(vertices, vertex);
			int normalIndex = vertex * BakedQuadHelper.VERTEX_STRIDE + BakedQuadHelper.NORMAL_OFFSET;
			int normal = vertices[normalIndex];
			for (int turn = 0; turn < turns; turn++) {
				position = new Vec3(1 - position.z, position.y, position.x);
				int x = (byte) normal;
				int z = (byte) (normal >> 16);
				normal = (normal & 0xff00ff00) | (-z & 0xff) | ((x & 0xff) << 16);
			}
			BakedQuadHelper.setXYZ(vertices, vertex, position);
			vertices[normalIndex] = normal;
		}
		Direction face = quad.getDirection();
		if (face.getAxis().isHorizontal())
			for (int turn = 0; turn < turns; turn++)
				face = face.getClockWise();
		return new BakedQuad(vertices, quad.getTintIndex(), face, quad.getSprite(), quad.isShade());
	}
}
