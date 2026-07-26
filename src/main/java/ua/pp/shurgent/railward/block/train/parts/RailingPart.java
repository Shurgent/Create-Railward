package ua.pp.shurgent.railward.block.train.parts;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

public enum RailingPart {
	NORTH_WEST_LOWER(0, 0, 0, 1, 8, 1, Direction.Axis.Y),
	NORTH_EAST_LOWER(15, 0, 0, 16, 8, 1, Direction.Axis.Y),
	NORTH_WEST_UPPER(0, 8, 0, 1, 16, 1, Direction.Axis.Y),
	NORTH_EAST_UPPER(15, 8, 0, 16, 16, 1, Direction.Axis.Y),
	SOUTH_WEST_LOWER(0, 0, 15, 1, 8, 16, Direction.Axis.Y),
	SOUTH_EAST_LOWER(15, 0, 15, 16, 8, 16, Direction.Axis.Y),
	SOUTH_WEST_UPPER(0, 8, 15, 1, 16, 16, Direction.Axis.Y),
	SOUTH_EAST_UPPER(15, 8, 15, 16, 16, 16, Direction.Axis.Y),

	NORTH_BOTTOM(0, 0, 0, 16, 1, 1, Direction.Axis.X),
	NORTH_MIDDLE(0, 7.5, 0, 16, 8.5, 1, Direction.Axis.X),
	NORTH_TOP(0, 15, 0, 16, 16, 1, Direction.Axis.X),
	SOUTH_BOTTOM(0, 0, 15, 16, 1, 16, Direction.Axis.X),
	SOUTH_MIDDLE(0, 7.5, 15, 16, 8.5, 16, Direction.Axis.X),
	SOUTH_TOP(0, 15, 15, 16, 16, 16, Direction.Axis.X),
	EAST_BOTTOM(15, 0, 0, 16, 1, 16, Direction.Axis.Z),
	EAST_MIDDLE(15, 7.5, 0, 16, 8.5, 16, Direction.Axis.Z),
	EAST_TOP(15, 15, 0, 16, 16, 16, Direction.Axis.Z),
	WEST_BOTTOM(0, 0, 0, 1, 1, 16, Direction.Axis.Z),
	WEST_MIDDLE(0, 7.5, 0, 1, 8.5, 16, Direction.Axis.Z),
	WEST_TOP(0, 15, 0, 1, 16, 16, Direction.Axis.Z);

	private static final double HIT_EPSILON = 1.0e-4;

	private final double minX;
	private final double minY;
	private final double minZ;
	private final double maxX;
	private final double maxY;
	private final double maxZ;
	private final Direction.Axis axis;
	private final VoxelShape shape;
	private final VoxelShape interactionShape;
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
		double hitMinX = axis == Direction.Axis.X ? minX : interactionMin(minX, maxX);
		double hitMinY = axis == Direction.Axis.Y ? minY : interactionMin(minY, maxY);
		double hitMinZ = axis == Direction.Axis.Z ? minZ : interactionMin(minZ, maxZ);
		double hitMaxX = axis == Direction.Axis.X ? maxX : interactionMax(minX, maxX);
		double hitMaxY = axis == Direction.Axis.Y ? maxY : interactionMax(minY, maxY);
		double hitMaxZ = axis == Direction.Axis.Z ? maxZ : interactionMax(minZ, maxZ);
		interactionShape = Block.box(hitMinX, hitMinY, hitMinZ, hitMaxX, hitMaxY, hitMaxZ);
		center = new Vec3((this.minX + this.maxX) * .5, (this.minY + this.maxY) * .5,
			(this.minZ + this.maxZ) * .5);
	}

	public int bit() {
		return 1 << ordinal();
	}

	public VoxelShape shape() {
		return shape;
	}

	public VoxelShape interactionShape() {
		return interactionShape;
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
		return interactionShape.bounds()
			.inflate(HIT_EPSILON)
			.contains(point);
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

	private static double interactionMin(double min, double max) {
		if (min <= 0)
			return 0;
		if (max >= 16)
			return 14;
		return min - .5;
	}

	private static double interactionMax(double min, double max) {
		if (max >= 16)
			return 16;
		if (min <= 0)
			return 2;
		return max + .5;
	}
}
