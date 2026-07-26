package ua.pp.shurgent.railward.block.train.parts;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class RailingContraptionCollision {

	private static final int VALID_PARTS_MASK = (1 << RailingPart.values().length) - 1;
	private static final int NORTH_RAILS = mask(RailingPart.NORTH_BOTTOM, RailingPart.NORTH_MIDDLE,
		RailingPart.NORTH_TOP);
	private static final int SOUTH_RAILS = mask(RailingPart.SOUTH_BOTTOM, RailingPart.SOUTH_MIDDLE,
		RailingPart.SOUTH_TOP);
	private static final int EAST_RAILS = mask(RailingPart.EAST_BOTTOM, RailingPart.EAST_MIDDLE,
		RailingPart.EAST_TOP);
	private static final int WEST_RAILS = mask(RailingPart.WEST_BOTTOM, RailingPart.WEST_MIDDLE,
		RailingPart.WEST_TOP);

	private static final VoxelShape NORTH_BARRIER = Block.box(0, 0, 0, 16, 24, 1);
	private static final VoxelShape SOUTH_BARRIER = Block.box(0, 0, 15, 16, 24, 16);
	private static final VoxelShape EAST_BARRIER = Block.box(15, 0, 0, 16, 24, 16);
	private static final VoxelShape WEST_BARRIER = Block.box(0, 0, 0, 1, 24, 16);
	private static final Map<Integer, VoxelShape> CONTRAPTION_SHAPES = new ConcurrentHashMap<>();

	private RailingContraptionCollision() {
	}

	public static int partsFrom(StructureBlockInfo info) {
		if (info == null || !(info.state.getBlock() instanceof RailingBlock))
			return -1;
		if (info.nbt != null && info.nbt.contains(RailingBlockEntity.PARTS_KEY, Tag.TAG_INT))
			return info.nbt.getInt(RailingBlockEntity.PARTS_KEY);
		return RailingPart.maskForSide(info.state.getValue(RailingBlock.FACING));
	}

	public static VoxelShape shapeFrom(StructureBlockInfo info) {
		int parts = partsFrom(info);
		return parts < 0 ? null : shapeForParts(parts);
	}

	public static VoxelShape shapeForParts(int parts) {
		int sanitizedParts = parts & VALID_PARTS_MASK;
		return CONTRAPTION_SHAPES.computeIfAbsent(sanitizedParts,
			RailingContraptionCollision::makeContraptionShape);
	}

	private static VoxelShape makeContraptionShape(int parts) {
		VoxelShape result = RailingBlock.shapeForMask(parts);
		if ((parts & NORTH_RAILS) != 0)
			result = Shapes.or(result, NORTH_BARRIER);
		if ((parts & SOUTH_RAILS) != 0)
			result = Shapes.or(result, SOUTH_BARRIER);
		if ((parts & EAST_RAILS) != 0)
			result = Shapes.or(result, EAST_BARRIER);
		if ((parts & WEST_RAILS) != 0)
			result = Shapes.or(result, WEST_BARRIER);
		return result.optimize();
	}

	private static int mask(RailingPart... parts) {
		int mask = 0;
		for (RailingPart part : parts)
			mask |= part.bit();
		return mask;
	}
}
