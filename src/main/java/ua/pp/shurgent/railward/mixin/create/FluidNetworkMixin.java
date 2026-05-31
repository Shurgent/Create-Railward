package ua.pp.shurgent.railward.mixin.create;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import com.simibubi.create.content.fluids.FluidNetwork;
import com.simibubi.create.content.fluids.PipeConnection;
import com.simibubi.create.content.fluids.PipeConnection.Flow;
import com.simibubi.create.foundation.fluid.FluidHelper;
import com.simibubi.create.foundation.utility.BlockFace;
import com.simibubi.create.foundation.utility.Iterate;
import com.simibubi.create.foundation.utility.Pair;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;

@Mixin(value = FluidNetwork.class, remap = false)
public abstract class FluidNetworkMixin {

	@Shadow
	private static int CYCLES_PER_TICK;

	@Shadow
	private Level world;
	@Shadow
	private BlockFace start;
	@Shadow
	private Supplier<LazyOptional<IFluidHandler>> sourceSupplier;
	@Shadow
	private LazyOptional<IFluidHandler> source;
	@Shadow
	private int transferSpeed;
	@Shadow
	private int pauseBeforePropagation;
	@Shadow
	private List<BlockFace> queued;
	@Shadow
	private Set<Pair<BlockFace, PipeConnection>> frontier;
	@Shadow
	private Set<BlockPos> visited;
	@Shadow
	private FluidStack fluid;
	@Shadow
	private List<Pair<BlockFace, LazyOptional<IFluidHandler>>> targets;

	@Shadow
	private void keepPortableFluidInterfaceEngaged() {}

	@Shadow
	@Nullable
	private PipeConnection get(BlockFace location) {
		return null;
	}

	@Shadow
	private boolean isPresent(BlockFace location) {
		return false;
	}

	/**
	 * @author Shurgent
	 * @reason Create 0.5.1 drains once per matching source tank, but only keeps the
	 *         last drained stack in transfer. Multi-tank contraptions therefore lose
	 *         the earlier drained portions. Keep the original network logic, but
	 *         accumulate source extraction up to the selected flow speed.
	 */
	@Overwrite
	public void tick() {
		if (pauseBeforePropagation > 0) {
			pauseBeforePropagation--;
			return;
		}

		for (int cycle = 0; cycle < CYCLES_PER_TICK; cycle++) {
			boolean shouldContinue = false;
			for (Iterator<BlockFace> iterator = queued.iterator(); iterator.hasNext();) {
				BlockFace blockFace = iterator.next();
				if (!isPresent(blockFace))
					continue;
				PipeConnection pipeConnection = get(blockFace);
				if (pipeConnection != null) {
					if (blockFace.equals(start))
						transferSpeed = (int) Math.max(1, pipeConnection.getPressure()
							.get(true) / 2f);
					frontier.add(Pair.of(blockFace, pipeConnection));
				}
				iterator.remove();
			}

			for (Iterator<Pair<BlockFace, PipeConnection>> iterator = frontier.iterator(); iterator.hasNext();) {
				Pair<BlockFace, PipeConnection> pair = iterator.next();
				BlockFace blockFace = pair.getFirst();
				PipeConnection pipeConnection = pair.getSecond();

				if (!pipeConnection.hasFlow())
					continue;

				Flow flow = ((PipeConnectionAccessor) pipeConnection).railward$getFlow()
					.get();
				if (!fluid.isEmpty() && !flow.fluid.isFluidEqual(fluid)) {
					iterator.remove();
					continue;
				}
				if (!flow.inbound) {
					if (pipeConnection.comparePressure() >= 0)
						iterator.remove();
					continue;
				}
				if (!flow.complete)
					continue;

				if (fluid.isEmpty())
					fluid = flow.fluid;

				boolean canRemove = true;
				for (Direction side : Iterate.directions) {
					if (side == blockFace.getFace())
						continue;
					BlockFace adjacentLocation = new BlockFace(blockFace.getPos(), side);
					PipeConnection adjacent = get(adjacentLocation);
					if (adjacent == null)
						continue;
					if (!adjacent.hasFlow()) {
						if (adjacent.hasPressure() && adjacent.getPressure()
							.getSecond() > 0)
							canRemove = false;
						continue;
					}
					Flow outFlow = ((PipeConnectionAccessor) adjacent).railward$getFlow()
						.get();
					if (outFlow.inbound) {
						if (adjacent.comparePressure() > 0)
							canRemove = false;
						continue;
					}
					if (!outFlow.complete) {
						canRemove = false;
						continue;
					}

					PipeConnectionAccessor adjacentAccess = (PipeConnectionAccessor) adjacent;
					if (!adjacentAccess.railward$getSource()
						.isPresent() && !adjacent.determineSource(world, blockFace.getPos())) {
						canRemove = false;
						continue;
					}

					if (adjacentAccess.railward$getSource()
						.isPresent() && adjacentAccess.railward$getSource()
							.get()
						.isEndpoint()) {
						targets.add(Pair.of(adjacentLocation, adjacentAccess.railward$getSource()
							.get()
							.provideHandler()));
						continue;
					}

					if (visited.add(adjacentLocation.getConnectedPos())) {
						queued.add(adjacentLocation.getOpposite());
						shouldContinue = true;
					}
				}
				if (canRemove)
					iterator.remove();
			}
			if (!shouldContinue)
				break;
		}

		if (!source.isPresent())
			source = sourceSupplier.get();
		if (!source.isPresent())
			return;

		keepPortableFluidInterfaceEngaged();

		if (targets.isEmpty())
			return;
		for (Pair<BlockFace, LazyOptional<IFluidHandler>> pair : targets) {
			if (pair.getSecond()
				.isPresent() && world.getGameTime() % 40 != 0)
				continue;
			PipeConnection pipeConnection = get(pair.getFirst());
			if (pipeConnection == null)
				continue;
			((PipeConnectionAccessor) pipeConnection).railward$getSource()
				.ifPresent(fs -> {
					if (fs.isEndpoint())
						pair.setSecond(fs.provideHandler());
				});
		}

		int flowSpeed = transferSpeed;
		Map<IFluidHandler, Integer> accumulatedFill = new IdentityHashMap<>();

		for (boolean simulate : Iterate.trueAndFalse) {
			FluidAction action = simulate ? FluidAction.SIMULATE : FluidAction.EXECUTE;

			IFluidHandler handler = source.orElse(null);
			if (handler == null)
				return;

			FluidStack transfer = railward$extractTransfer(handler, flowSpeed, action);

			if (transfer.isEmpty())
				return;
			if (simulate)
				flowSpeed = transfer.getAmount();

			List<Pair<BlockFace, LazyOptional<IFluidHandler>>> availableOutputs = new ArrayList<>(targets);

			while (!availableOutputs.isEmpty() && transfer.getAmount() > 0) {
				int dividedTransfer = transfer.getAmount() / availableOutputs.size();
				int remainder = transfer.getAmount() % availableOutputs.size();

				for (Iterator<Pair<BlockFace, LazyOptional<IFluidHandler>>> iterator =
					availableOutputs.iterator(); iterator.hasNext();) {
					Pair<BlockFace, LazyOptional<IFluidHandler>> pair = iterator.next();
					int toTransfer = dividedTransfer;
					if (remainder > 0) {
						toTransfer++;
						remainder--;
					}

					if (transfer.isEmpty())
						break;
					IFluidHandler targetHandler = pair.getSecond()
						.orElse(null);
					if (targetHandler == null) {
						iterator.remove();
						continue;
					}

					int simulatedTransfer = toTransfer;
					if (simulate)
						simulatedTransfer += accumulatedFill.getOrDefault(targetHandler, 0);

					FluidStack divided = transfer.copy();
					divided.setAmount(simulatedTransfer);
					int fill = targetHandler.fill(divided, action);

					if (simulate) {
						accumulatedFill.put(targetHandler, Integer.valueOf(fill));
						fill -= simulatedTransfer - toTransfer;
					}

					transfer.setAmount(transfer.getAmount() - fill);
					if (fill < simulatedTransfer)
						iterator.remove();
				}
			}

			flowSpeed -= transfer.getAmount();
		}
	}

	private FluidStack railward$extractTransfer(IFluidHandler handler, int flowSpeed, FluidAction action) {
		FluidStack transfer = FluidStack.EMPTY;
		int remaining = flowSpeed;

		for (int i = 0; i < handler.getTanks() && remaining > 0; i++) {
			FluidStack contained = handler.getFluidInTank(i);
			if (contained.isEmpty())
				continue;
			if (!contained.isFluidEqual(fluid))
				continue;
			FluidStack toExtract = FluidHelper.copyStackWithAmount(contained, remaining);
			FluidStack drained = handler.drain(toExtract, action);
			if (drained.isEmpty())
				continue;
			transfer = railward$mergeTransfer(transfer, drained);
			remaining = flowSpeed - transfer.getAmount();
		}

		if (!transfer.isEmpty())
			return transfer;

		FluidStack genericExtract = handler.drain(flowSpeed, action);
		if (!genericExtract.isEmpty() && genericExtract.isFluidEqual(fluid))
			return genericExtract;
		return FluidStack.EMPTY;
	}

	private FluidStack railward$mergeTransfer(FluidStack transfer, FluidStack drained) {
		if (transfer.isEmpty())
			return drained.copy();
		if (drained.isFluidEqual(transfer))
			transfer.grow(drained.getAmount());
		return transfer;
	}

}
