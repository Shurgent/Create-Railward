package ua.pp.shurgent.railward.block.train.signal;

import java.util.HashSet;
import java.util.Set;

import com.simibubi.create.content.trains.graph.DimensionPalette;
import com.simibubi.create.content.trains.graph.EdgePointType;
import com.simibubi.create.content.trains.graph.TrackNode;
import com.simibubi.create.content.trains.signal.TrackEdgePoint;
import com.simibubi.create.foundation.utility.Couple;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;

public class WhistleSignEdgePoint extends TrackEdgePoint {

	private Couple<Set<BlockPos>> blockEntities;

	public WhistleSignEdgePoint() {
		blockEntities = Couple.create(HashSet::new);
	}

	@Override
	public boolean canMerge() {
		return true;
	}

	@Override
	public boolean canCoexistWith(EdgePointType<?> otherType, boolean front) {
		return otherType == getType();
	}

	@Override
	public void invalidate(LevelAccessor level) {
		blockEntities.forEach(side -> side.forEach(pos -> invalidateAt(level, pos)));
	}

	@Override
	public void blockEntityAdded(BlockEntity blockEntity, boolean front) {
		blockEntities.get(front)
			.add(blockEntity.getBlockPos());
	}

	@Override
	public void blockEntityRemoved(BlockPos blockEntityPos, boolean front) {
		blockEntities.forEach(side -> side.remove(blockEntityPos));
		if (blockEntities.both(Set::isEmpty))
			removeFromAllGraphs();
	}

	public boolean canTriggerFrom(TrackNode side) {
		return !blockEntities.get(isPrimary(side))
			.isEmpty();
	}

	public boolean hasSignsOnBothSides() {
		return blockEntities.both(side -> !side.isEmpty());
	}

	@Override
	public void read(CompoundTag nbt, boolean migration, DimensionPalette dimensions) {
		super.read(nbt, migration, dimensions);
		if (migration)
			return;

		blockEntities = Couple.create(HashSet::new);
		for (int i = 1; i <= 2; i++) {
			boolean first = i == 1;
			ListTag list = nbt.getList("Tiles" + i, Tag.TAG_COMPOUND);
			for (int j = 0; j < list.size(); j++)
				blockEntities.get(first)
					.add(NbtUtils.readBlockPos(list.getCompound(j)));
		}
	}

	@Override
	public void read(FriendlyByteBuf buffer, DimensionPalette dimensions) {
		super.read(buffer, dimensions);
		blockEntities = Couple.create(HashSet::new);
		readBlockEntities(buffer, true);
		readBlockEntities(buffer, false);
	}

	@Override
	public void write(CompoundTag nbt, DimensionPalette dimensions) {
		super.write(nbt, dimensions);
		for (int i = 1; i <= 2; i++) {
			boolean first = i == 1;
			Set<BlockPos> positions = blockEntities.get(first);
			if (positions.isEmpty())
				continue;

			ListTag list = new ListTag();
			positions.forEach(pos -> list.add(NbtUtils.writeBlockPos(pos)));
			nbt.put("Tiles" + i, list);
		}
	}

	@Override
	public void write(FriendlyByteBuf buffer, DimensionPalette dimensions) {
		super.write(buffer, dimensions);
		writeBlockEntities(buffer, true);
		writeBlockEntities(buffer, false);
	}

	private void readBlockEntities(FriendlyByteBuf buffer, boolean front) {
		Set<BlockPos> positions = blockEntities.get(front);
		int count = buffer.readVarInt();
		for (int i = 0; i < count; i++)
			positions.add(buffer.readBlockPos());
	}

	private void writeBlockEntities(FriendlyByteBuf buffer, boolean front) {
		Set<BlockPos> positions = blockEntities.get(front);
		buffer.writeVarInt(positions.size());
		positions.forEach(buffer::writeBlockPos);
	}
}
