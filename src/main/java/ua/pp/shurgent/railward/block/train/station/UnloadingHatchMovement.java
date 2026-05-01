package ua.pp.shurgent.railward.block.train.station;

import java.util.Optional;

import com.simibubi.create.content.contraptions.behaviour.MovementBehaviour;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.trains.entity.CarriageContraption;
import com.simibubi.create.foundation.utility.VecHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

public class UnloadingHatchMovement implements MovementBehaviour {

	private static final String WORKING_POS = "RailwardUnloadingBunker";
	private static final String HATCH_POS = "RailwardUnloadingHatch";

	@Override
	public Vec3 getActiveAreaOffset(MovementContext context) {
		return Vec3.ZERO;
	}

	@Override
	public void visitNewPosition(MovementContext context, BlockPos pos) {
		if (!findBunker(context, pos))
			reset(context);
	}

	@Override
	public void tick(MovementContext context) {
		boolean onCarriage = context.contraption instanceof CarriageContraption;
		if (onCarriage && context.motion.length() > 1 / 4f)
			return;

		if (!context.data.contains(WORKING_POS)) {
			if (context.position != null)
				findBunker(context, new BlockPos(context.position));
			return;
		}

		BlockPos bunkerPos = NbtUtils.readBlockPos(context.data.getCompound(WORKING_POS));
		BlockEntity blockEntity = context.world.getBlockEntity(bunkerPos);
		if (!(blockEntity instanceof UnloadingBunkerBlockEntity bunker)) {
			reset(context);
			return;
		}

		if (!isVertical(context)) {
			reset(context);
			return;
		}

		Vec3 target = VecHelper.getCenterOf(bunkerPos);
		if (!context.stall && !onCarriage && context.position != null
			&& context.position.closerThan(target, target.distanceTo(context.position.add(context.motion))))
			context.stall = true;

		BlockPos hatchPos = context.data.contains(HATCH_POS) ? NbtUtils.readBlockPos(context.data.getCompound(HATCH_POS))
			: new BlockPos(context.position);
		if (!context.world.isClientSide) {
			bunker.startUnloadingFrom(context.contraption, hatchPos);
			bunker.keepAlive = 2;
		}

		if (context.stall && bunker.canReleaseContraption())
			context.stall = false;
	}

	@Override
	public void cancelStall(MovementContext context) {
		reset(context);
	}

	private boolean findBunker(MovementContext context, BlockPos hatchPos) {
		if (context.contraption instanceof CarriageContraption carriageContraption && !carriageContraption.notInPortal())
			return false;
		if (!isVertical(context))
			return false;

		for (int distance = 1; distance <= 3; distance++) {
			BlockPos candidate = hatchPos.below(distance);
			BlockEntity blockEntity = context.world.getBlockEntity(candidate);
			if (!(blockEntity instanceof UnloadingBunkerBlockEntity bunker))
				continue;

			context.data.put(WORKING_POS, NbtUtils.writeBlockPos(candidate));
			context.data.put(HATCH_POS, NbtUtils.writeBlockPos(hatchPos));
			if (!context.world.isClientSide) {
				bunker.startUnloadingFrom(context.contraption, hatchPos);
				bunker.keepAlive = 2;
			}
			return true;
		}
		return false;
	}

	private boolean isVertical(MovementContext context) {
		Optional<Direction> currentDown = getCurrentDown(context);
		return currentDown.isPresent() && currentDown.get() == Direction.DOWN;
	}

	private Optional<Direction> getCurrentDown(MovementContext context) {
		Vec3 down = Vec3.atLowerCornerOf(Direction.DOWN.getNormal());
		down = context.rotation.apply(down);
		Direction nearest = Direction.getNearest(down.x, down.y, down.z);
		if (down.distanceTo(Vec3.atLowerCornerOf(nearest.getNormal())) > 1 / 2f)
			return Optional.empty();
		return Optional.of(nearest);
	}

	private void reset(MovementContext context) {
		context.data.remove(WORKING_POS);
		context.data.remove(HATCH_POS);
		context.stall = false;
	}
}
