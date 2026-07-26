package ua.pp.shurgent.railward.block.train.parts;

import java.util.List;

import com.simibubi.create.content.contraptions.ITransformableBlockEntity;
import com.simibubi.create.content.contraptions.StructureTransform;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class RailingBlockEntity extends SmartBlockEntity implements ITransformableBlockEntity {

	public static final String PARTS_KEY = "Parts";

	private int parts;

	public RailingBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
		Direction initialSide = state.hasProperty(RailingBlock.FACING)
			? state.getValue(RailingBlock.FACING)
			: Direction.NORTH;
		parts = RailingPart.maskForSide(initialSide);
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
	}

	@Override
	protected AABB createRenderBoundingBox() {
		return new AABB(worldPosition);
	}

	public int getParts() {
		return parts;
	}

	public boolean isEnabled(RailingPart part) {
		return (parts & part.bit()) != 0;
	}

	public int toggledMask(RailingPart part) {
		return parts ^ part.bit();
	}

	public void setParts(int parts) {
		this.parts = parts;
		setChanged();
		if (level != null && !level.isClientSide)
			notifyUpdate();
	}

	@Override
	public void transform(StructureTransform transform) {
		int transformedParts = 0;
		for (RailingPart part : RailingPart.values()) {
			if (!isEnabled(part))
				continue;
			Vec3 transformedCenter = transform.applyWithoutOffset(part.center());
			RailingPart transformedPart = RailingPart.atCenter(transformedCenter);
			transformedParts |= (transformedPart == null ? part : transformedPart).bit();
		}
		setParts(transformedParts);
	}

	@Override
	protected void write(CompoundTag tag, boolean clientPacket) {
		super.write(tag, clientPacket);
		tag.putInt(PARTS_KEY, parts);
	}

	@Override
	public void writeSafe(CompoundTag tag) {
		super.writeSafe(tag);
		tag.putInt(PARTS_KEY, parts);
	}

	@Override
	protected void read(CompoundTag tag, boolean clientPacket) {
		super.read(tag, clientPacket);
		if (tag.contains(PARTS_KEY))
			parts = tag.getInt(PARTS_KEY);
	}
}
