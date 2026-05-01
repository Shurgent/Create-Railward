package ua.pp.shurgent.railward.block.train.station;

import java.util.Optional;

import com.simibubi.create.content.contraptions.behaviour.MovementBehaviour;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.trains.entity.CarriageContraption;
import com.simibubi.create.foundation.utility.VecHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.item.ItemStack;
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
	public ItemStack canBeDisabledVia(MovementContext context) {
		return new ItemStack(context.state.getBlock());
	}

	@Override
	public void visitNewPosition(MovementContext context, BlockPos pos) {
		if (context.disabled) {
			reset(context);
			return;
		}
		if (!findBunker(context, pos))
			reset(context);
	}

	@Override
	public void tick(MovementContext context) {
		if (context.disabled) {
			reset(context);
			return;
		}

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

		BlockPos hatchPos = context.position == null ? null : new BlockPos(context.position);
		if (hatchPos == null || !isBunkerBelowHatch(hatchPos, bunkerPos)) {
			reset(context);
			if (hatchPos != null)
				findBunker(context, hatchPos);
			return;
		}

		Vec3 target = VecHelper.getCenterOf(bunkerPos);
		if (!context.stall && !onCarriage && context.position != null
			&& context.position.closerThan(target, target.distanceTo(context.position.add(context.motion))))
			context.stall = true;

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

	private boolean isBunkerBelowHatch(BlockPos hatchPos, BlockPos bunkerPos) {
		int distance = hatchPos.getY() - bunkerPos.getY();
		return hatchPos.getX() == bunkerPos.getX() && hatchPos.getZ() == bunkerPos.getZ()
			&& distance >= 1 && distance <= 3;
	}

	private void reset(MovementContext context) {
		context.data.remove(WORKING_POS);
		context.data.remove(HATCH_POS);
		context.stall = false;
	}
}
