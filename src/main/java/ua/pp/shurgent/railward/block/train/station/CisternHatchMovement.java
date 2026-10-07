package ua.pp.shurgent.railward.block.train.station;

import java.util.Optional;

//? if !create6
import javax.annotation.Nullable;

//? if !create6
import com.jozufozu.flywheel.api.MaterialManager;
import com.jozufozu.flywheel.core.virtual.VirtualRenderWorld;
import com.simibubi.create.content.contraptions.behaviour.MovementBehaviour;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
//? if !create6
import com.simibubi.create.content.contraptions.render.ActorInstance;
import com.simibubi.create.content.contraptions.render.ContraptionMatrices;
//? if !create6
import com.simibubi.create.content.contraptions.render.ContraptionRenderDispatcher;
import com.simibubi.create.content.trains.entity.CarriageContraption;
import com.simibubi.create.foundation.utility.VecHelper;
import com.simibubi.create.foundation.utility.animation.LerpedFloat;
import com.simibubi.create.foundation.utility.animation.LerpedFloat.Chaser;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
//? if !create6
import ua.pp.shurgent.railward.client.renderer.CisternHatchActorInstance;
import ua.pp.shurgent.railward.client.renderer.CisternHatchRenderer;

public class CisternHatchMovement implements MovementBehaviour {

	private static final String TERMINAL_POS = "RailwardFluidTerminal";

	@Override
	public Vec3 getActiveAreaOffset(MovementContext context) {
		return Vec3.ZERO;
	}

	@Override
	public ItemStack canBeDisabledVia(MovementContext context) {
		return new ItemStack(context.state.getBlock());
	}

	//? if !create6 {
	@Override
	public boolean hasSpecialInstancedRendering() {
		return true;
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	@Nullable
	public ActorInstance createInstance(MaterialManager materialManager, VirtualRenderWorld simulationWorld,
		MovementContext context) {
		return new CisternHatchActorInstance(materialManager, simulationWorld, context);
	}
	//?}

	@Override
	@OnlyIn(Dist.CLIENT)
	public void renderInContraption(MovementContext context, VirtualRenderWorld renderWorld,
		ContraptionMatrices matrices, MultiBufferSource buffer) {
		//? if !create6 {
		if (ContraptionRenderDispatcher.canInstance())
			return;
		//?}
		CisternHatchRenderer.renderInContraption(context, renderWorld, matrices, buffer);
	}

	@Override
	public void visitNewPosition(MovementContext context, BlockPos pos) {
		if (context.disabled) {
			reset(context);
			return;
		}
		if (!findTerminal(context, pos))
			reset(context);
	}

	@Override
	public void tick(MovementContext context) {
		if (context.world.isClientSide)
			getAnimation(context).tickChaser();

		if (context.disabled) {
			reset(context);
			return;
		}

		boolean onCarriage = context.contraption instanceof CarriageContraption;
		if (onCarriage && context.motion.length() > 1 / 4f)
			return;

		if (!context.data.contains(TERMINAL_POS)) {
			if (context.position != null)
				findTerminal(context, new BlockPos(context.position));
			return;
		}

		BlockPos terminalPos = NbtUtils.readBlockPos(context.data.getCompound(TERMINAL_POS));
		BlockEntity blockEntity = context.world.getBlockEntity(terminalPos);
		if (!(blockEntity instanceof FluidTerminalBlockEntity terminal)) {
			reset(context);
			return;
		}

		if (!isUpright(context) || terminal.isPowered()) {
			reset(context);
			return;
		}

		BlockPos hatchPos = context.position == null ? null : new BlockPos(context.position);
		if (hatchPos == null || !isTerminalAboveHatch(hatchPos, terminalPos)) {
			reset(context);
			if (hatchPos != null)
				findTerminal(context, hatchPos);
			return;
		}

		Vec3 target = VecHelper.getCenterOf(terminalPos);
		if (!context.stall && !onCarriage && context.position != null
			&& context.position.closerThan(target, target.distanceTo(context.position.add(context.motion))))
			context.stall = true;

		if (!context.world.isClientSide) {
			terminal.startTransferringTo(context.contraption, terminalPos.getY() - hatchPos.getY());
			terminal.keepAlive = 2;
		} else {
			getAnimation(context).chase(terminal.isConnectedToHatch() ? 1 : 0, .25f, Chaser.LINEAR);
		}

		if (context.stall && terminal.canReleaseContraption())
			context.stall = false;
	}

	@Override
	public void cancelStall(MovementContext context) {
		reset(context);
	}

	private boolean findTerminal(MovementContext context, BlockPos hatchPos) {
		if (context.contraption instanceof CarriageContraption carriageContraption && !carriageContraption.notInPortal())
			return false;
		if (!isUpright(context))
			return false;

		for (int distance = 1; distance <= FluidTerminalBlockEntity.MAX_TRANSFER_DISTANCE; distance++) {
			BlockPos candidate = hatchPos.above(distance);
			BlockEntity blockEntity = context.world.getBlockEntity(candidate);
			if (!(blockEntity instanceof FluidTerminalBlockEntity terminal) || terminal.isPowered())
				continue;

			context.data.put(TERMINAL_POS, NbtUtils.writeBlockPos(candidate));
			if (!context.world.isClientSide) {
				terminal.startTransferringTo(context.contraption, distance);
				terminal.keepAlive = 2;
			} else {
				getAnimation(context).chase(terminal.isConnectedToHatch() ? 1 : 0, .25f, Chaser.LINEAR);
			}
			return true;
		}
		return false;
	}

	private boolean isUpright(MovementContext context) {
		Optional<Direction> currentUp = getCurrentUp(context);
		return currentUp.isPresent() && currentUp.get() == Direction.UP;
	}

	private Optional<Direction> getCurrentUp(MovementContext context) {
		Vec3 up = Vec3.atLowerCornerOf(Direction.UP.getNormal());
		up = context.rotation.apply(up);
		Direction nearest = Direction.getNearest(up.x, up.y, up.z);
		if (up.distanceTo(Vec3.atLowerCornerOf(nearest.getNormal())) > 1 / 2f)
			return Optional.empty();
		return Optional.of(nearest);
	}

	private boolean isTerminalAboveHatch(BlockPos hatchPos, BlockPos terminalPos) {
		int distance = terminalPos.getY() - hatchPos.getY();
		return hatchPos.getX() == terminalPos.getX() && hatchPos.getZ() == terminalPos.getZ()
			&& distance >= 1 && distance <= FluidTerminalBlockEntity.MAX_TRANSFER_DISTANCE;
	}

	private void reset(MovementContext context) {
		context.data.remove(TERMINAL_POS);
		context.stall = false;
		getAnimation(context).chase(0, .25f, Chaser.LINEAR);
	}

	public static LerpedFloat getAnimation(MovementContext context) {
		if (context.temporaryData instanceof LerpedFloat animation)
			return animation;
		LerpedFloat animation = LerpedFloat.linear()
			.startWithValue(0);
		context.temporaryData = animation;
		return animation;
	}
}
