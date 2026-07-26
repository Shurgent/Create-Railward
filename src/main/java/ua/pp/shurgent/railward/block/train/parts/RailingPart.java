package ua.pp.shurgent.railward.block.train.parts;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

public enum RailingPart {
	NORTH_WEST_LOWER(0, 0, 0, 2, 8, 2, Direction.Axis.Y),
	NORTH_EAST_LOWER(14, 0, 0, 16, 8, 2, Direction.Axis.Y),
	NORTH_WEST_UPPER(0, 8, 0, 2, 16, 2, Direction.Axis.Y),
	NORTH_EAST_UPPER(14, 8, 0, 16, 16, 2, Direction.Axis.Y),
	SOUTH_WEST_LOWER(0, 0, 14, 2, 8, 16, Direction.Axis.Y),
	SOUTH_EAST_LOWER(14, 0, 14, 16, 8, 16, Direction.Axis.Y),
	SOUTH_WEST_UPPER(0, 8, 14, 2, 16, 16, Direction.Axis.Y),
	SOUTH_EAST_UPPER(14, 8, 14, 16, 16, 16, Direction.Axis.Y),

	NORTH_BOTTOM(0, 0, 0, 16, 2, 2, Direction.Axis.X),
	NORTH_MIDDLE(0, 7, 0, 16, 9, 2, Direction.Axis.X),
	NORTH_TOP(0, 14, 0, 16, 16, 2, Direction.Axis.X),
	SOUTH_BOTTOM(0, 0, 14, 16, 2, 16, Direction.Axis.X),
	SOUTH_MIDDLE(0, 7, 14, 16, 9, 16, Direction.Axis.X),
	SOUTH_TOP(0, 14, 14, 16, 16, 16, Direction.Axis.X),
	EAST_BOTTOM(14, 0, 0, 16, 2, 16, Direction.Axis.Z),
	EAST_MIDDLE(14, 7, 0, 16, 9, 16, Direction.Axis.Z),
	EAST_TOP(14, 14, 0, 16, 16, 16, Direction.Axis.Z),
	WEST_BOTTOM(0, 0, 0, 2, 2, 16, Direction.Axis.Z),
	WEST_MIDDLE(0, 7, 0, 2, 9, 16, Direction.Axis.Z),
	WEST_TOP(0, 14, 0, 2, 16, 16, Direction.Axis.Z);

	private static final double HIT_EPSILON = 1.0e-4;

	private final double minX;
	private final double minY;
	private final double minZ;
	private final double maxX;
	private final double maxY;
	private final double maxZ;
	private final Direction.Axis axis;
	private final VoxelShape shape;
	private final Vec3 center;

	RailingPart(double minX, double minY, double minZ, double maxX, double maxY, double maxZ,
			Direction.Axis axis) {
		this.minX = minX / 16d;
		this.minY = minY / 16d;
		this.minZ = minZ / 16d;
		this.maxX = maxX / 16d;
		this.maxY = maxY / 16d;
		this.maxZ = maxZ / 16d;
		this.axis = axis;
		shape = Block.box(minX, minY, minZ, maxX, maxY, maxZ);
		center = new Vec3((this.minX + this.maxX) * .5, (this.minY + this.maxY) * .5,
			(this.minZ + this.maxZ) * .5);
	}

	public int bit() {
		return 1 << ordinal();
	}

	public VoxelShape shape() {
		return shape;
	}

	public Vec3 center() {
		return center;
	}

	public static int maskForSide(Direction side) {
		return switch (side) {
		case NORTH -> mask(NORTH_WEST_LOWER, NORTH_WEST_UPPER, NORTH_EAST_LOWER, NORTH_EAST_UPPER,
			NORTH_BOTTOM, NORTH_MIDDLE, NORTH_TOP);
		case SOUTH -> mask(SOUTH_WEST_LOWER, SOUTH_WEST_UPPER, SOUTH_EAST_LOWER, SOUTH_EAST_UPPER,
			SOUTH_BOTTOM, SOUTH_MIDDLE, SOUTH_TOP);
		case EAST -> mask(NORTH_EAST_LOWER, NORTH_EAST_UPPER, SOUTH_EAST_LOWER, SOUTH_EAST_UPPER,
			EAST_BOTTOM, EAST_MIDDLE, EAST_TOP);
		case WEST -> mask(NORTH_WEST_LOWER, NORTH_WEST_UPPER, SOUTH_WEST_LOWER, SOUTH_WEST_UPPER,
			WEST_BOTTOM, WEST_MIDDLE, WEST_TOP);
		default -> maskForSide(Direction.NORTH);
		};
	}

	public static RailingPart at(Vec3 localHit) {
		RailingPart result = null;
		double bestScore = Double.MAX_VALUE;
		for (RailingPart part : values()) {
			if (!part.contains(localHit))
				continue;
			double score = part.axisDistance(localHit);
			if (score < bestScore) {
				bestScore = score;
				result = part;
			}
		}
		return result;
	}

	public static RailingPart atCenter(Vec3 center) {
		for (RailingPart part : values())
			if (part.center.distanceToSqr(center) < 1.0e-8)
				return part;
		return null;
	}

	private boolean contains(Vec3 point) {
		return point.x >= minX - HIT_EPSILON && point.x <= maxX + HIT_EPSILON
			&& point.y >= minY - HIT_EPSILON && point.y <= maxY + HIT_EPSILON
			&& point.z >= minZ - HIT_EPSILON && point.z <= maxZ + HIT_EPSILON;
	}

	private double axisDistance(Vec3 point) {
		double dx = Math.abs(point.x - (minX + maxX) * .5);
		double dy = Math.abs(point.y - (minY + maxY) * .5);
		double dz = Math.abs(point.z - (minZ + maxZ) * .5);
		return switch (axis) {
		case X -> dy + dz;
		case Y -> dx + dz;
		case Z -> dx + dy;
		};
	}

	private static int mask(RailingPart... parts) {
		int mask = 0;
		for (RailingPart part : parts)
			mask |= part.bit();
		return mask;
	}
}
