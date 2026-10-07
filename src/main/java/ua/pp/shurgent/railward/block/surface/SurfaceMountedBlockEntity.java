package ua.pp.shurgent.railward.block.surface;

import java.util.List;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.content.contraptions.StructureTransform;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import ua.pp.shurgent.railward.compat.RailwardTransformableBlockEntity;

public abstract class SurfaceMountedBlockEntity extends SmartBlockEntity implements RailwardTransformableBlockEntity {

	private static final String ANCHOR_X = "SurfaceAnchorX";
	private static final String ANCHOR_Y = "SurfaceAnchorY";
	private static final String ANCHOR_Z = "SurfaceAnchorZ";

	private Vec3 anchor;

	protected SurfaceMountedBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
		anchor = defaultAnchor(state.getValue(SurfaceMountedBlock.FACING));
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
	}

	public Vec3 getAnchor() {
		return anchor;
	}

	@Override
	public AABB getRenderBoundingBox() {
		return new AABB(worldPosition).inflate(.5);
	}

	public void setAnchor(Vec3 anchor) {
		Direction facing = getBlockState().getValue(SurfaceMountedBlock.FACING);
		this.anchor = constrainToFace(anchor, facing);
		setChanged();
		if (level != null)
			notifyUpdate();
	}

	@Override
	protected void write(CompoundTag tag, boolean clientPacket) {
		super.write(tag, clientPacket);
		tag.putDouble(ANCHOR_X, anchor.x);
		tag.putDouble(ANCHOR_Y, anchor.y);
		tag.putDouble(ANCHOR_Z, anchor.z);
	}

	@Override
	public void writeSafe(CompoundTag tag) {
		super.writeSafe(tag);
		tag.putDouble(ANCHOR_X, anchor.x);
		tag.putDouble(ANCHOR_Y, anchor.y);
		tag.putDouble(ANCHOR_Z, anchor.z);
	}

	@Override
	public void transform(StructureTransform transform) {
		Direction facing = getBlockState().getValue(SurfaceMountedBlock.FACING);
		anchor = constrainToFace(transform.applyWithoutOffset(anchor), facing);
		setChanged();
	}

	@Override
	protected void read(CompoundTag tag, boolean clientPacket) {
		super.read(tag, clientPacket);
		Direction facing = getBlockState().getValue(SurfaceMountedBlock.FACING);
		if (tag.contains(ANCHOR_X) && tag.contains(ANCHOR_Y) && tag.contains(ANCHOR_Z))
			anchor = clampToBlock(new Vec3(tag.getDouble(ANCHOR_X), tag.getDouble(ANCHOR_Y),
				tag.getDouble(ANCHOR_Z)));
		else
			anchor = defaultAnchor(facing);
	}

	public static Vec3 fromWorldHit(Vec3 hit, BlockPos placedPos, Direction facing) {
		return constrainToFace(hit.subtract(placedPos.getX(), placedPos.getY(), placedPos.getZ()), facing);
	}

	public static Vec3 defaultAnchor(Direction facing) {
		return constrainToFace(new Vec3(.5, .5, .5), facing);
	}

	private static Vec3 constrainToFace(Vec3 anchor, Direction facing) {
		Vec3 clamped = clampToBlock(anchor);
		double x = clamped.x;
		double y = clamped.y;
		double z = clamped.z;
		return switch (facing) {
		case SOUTH -> new Vec3(x, y, 0);
		case NORTH -> new Vec3(x, y, 1);
		case EAST -> new Vec3(0, y, z);
		case WEST -> new Vec3(1, y, z);
		case UP -> new Vec3(x, 0, z);
		case DOWN -> new Vec3(x, 1, z);
		};
	}

	private static Vec3 clampToBlock(Vec3 anchor) {
		return new Vec3(Mth.clamp(anchor.x, 0, 1), Mth.clamp(anchor.y, 0, 1),
			Mth.clamp(anchor.z, 0, 1));
	}
}
