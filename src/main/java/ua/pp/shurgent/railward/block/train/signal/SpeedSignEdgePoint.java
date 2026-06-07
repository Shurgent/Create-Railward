package ua.pp.shurgent.railward.block.train.signal;

import java.util.HashMap;
import java.util.Map;
import java.util.OptionalInt;

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

public class SpeedSignEdgePoint extends TrackEdgePoint {

	private Couple<Map<BlockPos, Integer>> speedLimits;

	public SpeedSignEdgePoint() {
		speedLimits = Couple.create(HashMap::new);
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
		speedLimits.forEach(side -> side.keySet()
			.forEach(pos -> invalidateAt(level, pos)));
	}

	@Override
	public void blockEntityAdded(BlockEntity blockEntity, boolean front) {
		int speedLimitKmh = blockEntity instanceof SpeedSignBlockEntity speedSign
			? speedSign.getSpeedLimitKmh()
			: SpeedSignBlockEntity.getDefaultSpeedKmh();
		updateSpeedLimit(blockEntity.getBlockPos(), front, speedLimitKmh);
	}

	@Override
	public void blockEntityRemoved(BlockPos blockEntityPos, boolean front) {
		speedLimits.forEach(side -> side.remove(blockEntityPos));
		if (speedLimits.both(Map::isEmpty))
			removeFromAllGraphs();
	}

	public void updateSpeedLimit(BlockPos blockEntityPos, boolean front, int speedLimitKmh) {
		speedLimits.get(front)
			.put(blockEntityPos, SpeedSignBlockEntity.clampSpeedLimitKmh(speedLimitKmh));
	}

	public OptionalInt getSpeedLimitKmh(TrackNode side) {
		Map<BlockPos, Integer> limits = speedLimits.get(isPrimary(side));
		if (limits.isEmpty())
			return OptionalInt.empty();

		int limit = SpeedSignBlockEntity.getConfiguredMaxSpeedKmh();
		for (int value : limits.values())
			limit = Math.min(limit, SpeedSignBlockEntity.clampSpeedLimitKmh(value));
		return OptionalInt.of(limit);
	}

	public boolean hasSignsOnBothSides() {
		return speedLimits.both(side -> !side.isEmpty());
	}

	@Override
	public void read(CompoundTag nbt, boolean migration, DimensionPalette dimensions) {
		super.read(nbt, migration, dimensions);
		if (migration)
			return;

		speedLimits = Couple.create(HashMap::new);
		for (int i = 1; i <= 2; i++) {
			boolean first = i == 1;
			ListTag list = nbt.getList("Tiles" + i, Tag.TAG_COMPOUND);
			for (int j = 0; j < list.size(); j++) {
				CompoundTag entry = list.getCompound(j);
				int speedLimitKmh = entry.contains("SpeedLimit")
					? entry.getInt("SpeedLimit")
					: SpeedSignBlockEntity.getDefaultSpeedKmh();
				speedLimits.get(first)
					.put(NbtUtils.readBlockPos(entry.getCompound("Pos")),
						SpeedSignBlockEntity.clampSpeedLimitKmh(speedLimitKmh));
			}
		}
	}

	@Override
	public void read(FriendlyByteBuf buffer, DimensionPalette dimensions) {
		super.read(buffer, dimensions);
		speedLimits = Couple.create(HashMap::new);
		readSpeedLimits(buffer, true);
		readSpeedLimits(buffer, false);
	}

	@Override
	public void write(CompoundTag nbt, DimensionPalette dimensions) {
		super.write(nbt, dimensions);
		for (int i = 1; i <= 2; i++) {
			boolean first = i == 1;
			Map<BlockPos, Integer> limits = speedLimits.get(first);
			if (limits.isEmpty())
				continue;

			ListTag list = new ListTag();
			limits.forEach((pos, speedLimitKmh) -> {
				CompoundTag entry = new CompoundTag();
				entry.put("Pos", NbtUtils.writeBlockPos(pos));
				entry.putInt("SpeedLimit", SpeedSignBlockEntity.clampSpeedLimitKmh(speedLimitKmh));
				list.add(entry);
			});
			nbt.put("Tiles" + i, list);
		}
	}

	@Override
	public void write(FriendlyByteBuf buffer, DimensionPalette dimensions) {
		super.write(buffer, dimensions);
		writeSpeedLimits(buffer, true);
		writeSpeedLimits(buffer, false);
	}

	private void readSpeedLimits(FriendlyByteBuf buffer, boolean front) {
		Map<BlockPos, Integer> limits = speedLimits.get(front);
		int count = buffer.readVarInt();
		for (int i = 0; i < count; i++)
			limits.put(buffer.readBlockPos(), SpeedSignBlockEntity.clampSpeedLimitKmh(buffer.readVarInt()));
	}

	private void writeSpeedLimits(FriendlyByteBuf buffer, boolean front) {
		Map<BlockPos, Integer> limits = speedLimits.get(front);
		buffer.writeVarInt(limits.size());
		limits.forEach((pos, speedLimitKmh) -> {
			buffer.writeBlockPos(pos);
			buffer.writeVarInt(SpeedSignBlockEntity.clampSpeedLimitKmh(speedLimitKmh));
		});
	}
}
