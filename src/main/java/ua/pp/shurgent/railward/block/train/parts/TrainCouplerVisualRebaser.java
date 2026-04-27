package ua.pp.shurgent.railward.block.train.parts;

import com.simibubi.create.content.trains.entity.ArrivalSoundQueue;
import com.simibubi.create.content.trains.entity.CarriageContraption;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class TrainCouplerVisualRebaser {

	public static void apply(CarriageContraptionEntity entity, BlockPos offset, Direction newAssemblyDirection) {
		if (!(entity.getContraption() instanceof CarriageContraption contraption))
			return;

		CompoundTag tag = contraption.writeNBT(false);
		rebaseContraptionTag(tag, offset, newAssemblyDirection);

		contraption.modelData.clear();
		contraption.presentBlockEntities.clear();
		contraption.maybeInstancedBlockEntities.clear();
		contraption.specialRenderedBlockEntities.clear();
		contraption.soundQueue = new ArrivalSoundQueue();
		contraption.readNBT(entity.level, tag, false);
		contraption.entity = entity;
		entity.setInitialOrientation(newAssemblyDirection.getClockWise());

		rebuildBounds(contraption);
		contraption.invalidateColliders();
		contraption.deferInvalidate = true;
	}

	private static void rebaseContraptionTag(CompoundTag tag, BlockPos offset, Direction newAssemblyDirection) {
		tag.putString("AssemblyDirection", newAssemblyDirection.name());
		swapBooleans(tag, "FrontControls", "BackControls");
		swapBooleans(tag, "FrontBlazeConductor", "BackBlazeConductor");

		if (tag.contains("Anchor", Tag.TAG_COMPOUND))
			tag.put("Anchor", NbtUtils.writeBlockPos(NbtUtils.readBlockPos(tag.getCompound("Anchor"))
				.offset(offset)));

		rebaseBlocks(tag.getCompound("Blocks"), offset);
		rebasePosList(tag.getList("Actors", Tag.TAG_COMPOUND), "Pos", offset);
		rebasePosList(tag.getList("Interactors", Tag.TAG_COMPOUND), "Pos", offset);
		rebaseBarePosList(tag.getList("Seats", Tag.TAG_COMPOUND), offset);
		rebaseStorageList(tag.getList("Storage", Tag.TAG_COMPOUND), offset);
		rebaseStorageList(tag.getList("FluidStorage", Tag.TAG_COMPOUND), offset);
		rebaseCapturedMultiblocks(tag.getList("CapturedMultiblocks", Tag.TAG_COMPOUND), offset);
		rebaseConductorSeats(tag.getList("ConductorSeats", Tag.TAG_COMPOUND), offset);
		rebaseSoundQueue(tag.getCompound("SoundQueue"), offset, newAssemblyDirection);
		rebaseSuperglue(tag.getList("Superglue", Tag.TAG_COMPOUND), offset);
		tag.remove("BoundsFront");
	}

	private static void rebaseBlocks(CompoundTag blocksTag, BlockPos offset) {
		ListTag blockList = blocksTag.getList("BlockList", Tag.TAG_COMPOUND);
		for (Tag rawEntry : blockList) {
			CompoundTag entry = (CompoundTag) rawEntry;
			BlockPos newPos = rebase(BlockPos.of(entry.getLong("Pos")), offset);
			entry.putLong("Pos", newPos.asLong());

			if (!entry.contains("Data", Tag.TAG_COMPOUND))
				continue;
			CompoundTag data = entry.getCompound("Data");
			if (data.contains("x", Tag.TAG_INT))
				data.putInt("x", newPos.getX());
			if (data.contains("y", Tag.TAG_INT))
				data.putInt("y", newPos.getY());
			if (data.contains("z", Tag.TAG_INT))
				data.putInt("z", newPos.getZ());
			rebaseOptionalPos(data, "Controller", offset);
			rebaseOptionalPos(data, "LastKnownPos", offset);
		}
	}

	private static void rebaseCapturedMultiblocks(ListTag list, BlockPos offset) {
		for (Tag rawEntry : list) {
			CompoundTag entry = (CompoundTag) rawEntry;
			rebaseOptionalPos(entry, "Controller", offset);
			rebaseBarePosList(entry.getList("Parts", Tag.TAG_COMPOUND), offset);
		}
	}

	private static void rebaseConductorSeats(ListTag list, BlockPos offset) {
		for (Tag rawEntry : list) {
			CompoundTag entry = (CompoundTag) rawEntry;
			rebaseOptionalPos(entry, "Pos", offset);
			boolean forward = entry.getBoolean("Forward");
			entry.putBoolean("Forward", entry.getBoolean("Backward"));
			entry.putBoolean("Backward", forward);
		}
	}

	private static void rebaseSoundQueue(CompoundTag soundQueue, BlockPos offset, Direction newAssemblyDirection) {
		ListTag sources = soundQueue.getList("Sources", Tag.TAG_COMPOUND);
		for (Tag rawEntry : sources) {
			CompoundTag entry = (CompoundTag) rawEntry;
			if (!entry.contains("Pos", Tag.TAG_COMPOUND))
				continue;
			BlockPos newPos = rebase(NbtUtils.readBlockPos(entry.getCompound("Pos")), offset);
			entry.put("Pos", NbtUtils.writeBlockPos(newPos));
			entry.putInt("Tick", coordinateAlong(newPos, newAssemblyDirection));
		}
	}

	private static void rebaseSuperglue(ListTag list, BlockPos offset) {
		for (Tag rawEntry : list) {
			CompoundTag entry = (CompoundTag) rawEntry;
			rebaseVec(entry, "From", offset);
			rebaseVec(entry, "To", offset);
		}
	}

	private static void rebaseStorageList(ListTag list, BlockPos offset) {
		rebasePosList(list, "Pos", offset);
	}

	private static void rebasePosList(ListTag list, String key, BlockPos offset) {
		for (Tag rawEntry : list)
			rebaseOptionalPos((CompoundTag) rawEntry, key, offset);
	}

	private static void rebaseBarePosList(ListTag list, BlockPos offset) {
		for (Tag rawEntry : list)
			writePos((CompoundTag) rawEntry, rebase(NbtUtils.readBlockPos((CompoundTag) rawEntry), offset));
	}

	private static void rebaseOptionalPos(CompoundTag tag, String key, BlockPos offset) {
		if (!tag.contains(key, Tag.TAG_COMPOUND))
			return;
		tag.put(key, NbtUtils.writeBlockPos(rebase(NbtUtils.readBlockPos(tag.getCompound(key)), offset)));
	}

	private static void rebaseVec(CompoundTag tag, String key, BlockPos offset) {
		if (!tag.contains(key, Tag.TAG_LIST))
			return;

		ListTag list = tag.getList(key, Tag.TAG_DOUBLE);
		Vec3 vec = new Vec3(list.getDouble(0) - offset.getX(), list.getDouble(1) - offset.getY(),
			list.getDouble(2) - offset.getZ());
		ListTag rebased = new ListTag();
		rebased.add(DoubleTag.valueOf(vec.x));
		rebased.add(DoubleTag.valueOf(vec.y));
		rebased.add(DoubleTag.valueOf(vec.z));
		tag.put(key, rebased);
	}

	private static void writePos(CompoundTag tag, BlockPos pos) {
		tag.putInt("X", pos.getX());
		tag.putInt("Y", pos.getY());
		tag.putInt("Z", pos.getZ());
	}

	private static BlockPos rebase(BlockPos pos, BlockPos offset) {
		return pos.subtract(offset);
	}

	private static int coordinateAlong(BlockPos pos, Direction direction) {
		return direction.getAxis()
			.choose(pos.getX(), pos.getY(), pos.getZ()) * direction.getAxisDirection()
				.getStep();
	}

	private static void swapBooleans(CompoundTag tag, String first, String second) {
		boolean firstValue = tag.getBoolean(first);
		tag.putBoolean(first, tag.getBoolean(second));
		tag.putBoolean(second, firstValue);
	}

	private static void rebuildBounds(CarriageContraption contraption) {
		AABB bounds = null;
		for (BlockPos pos : contraption.getBlocks()
			.keySet()) {
			AABB blockBounds = new AABB(pos);
			bounds = bounds == null ? blockBounds : bounds.minmax(blockBounds);
		}
		if (bounds == null)
			return;

		contraption.bounds = bounds;
		contraption.expandBoundsAroundAxis(Direction.Axis.Y);
	}

}
