package ua.pp.shurgent.railward.block.train.station;

import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.actors.psi.PortableStorageInterfaceBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
//? if >=1.20.1
/*import net.minecraftforge.common.capabilities.ForgeCapabilities;*/
//? if <1.20.1
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;

public class FluidTerminalBlockEntity extends PortableStorageInterfaceBlockEntity {

	public static final int MAX_TRANSFER_DISTANCE = 2;

	private LazyOptional<IFluidHandler> capability;

	public FluidTerminalBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
		capability = createEmptyHandler();
	}

	@Override
	public void startTransferringTo(Contraption contraption, float distance) {
		if (connectedEntity == contraption.entity)
			return;

		LazyOptional<IFluidHandler> oldCapability = capability;
		capability = LazyOptional.of(() -> new TerminalFluidHandler(contraption.getSharedFluidTanks()));
		oldCapability.invalidate();
		super.startTransferringTo(contraption, Mth.clamp(distance, 1, MAX_TRANSFER_DISTANCE));
	}

	public boolean canReleaseContraption() {
		return transferTimer <= ANIMATION;
	}

	public boolean isConnectedToHatch() {
		if (level != null && level.isClientSide)
			return transferTimer >= ANIMATION && transferTimer <= getTransferTimeout() + ANIMATION;
		return canTransfer();
	}

	public float getHoseExtensionDistance(float partialTicks) {
		return Mth.clamp(connectionAnimation.getValue(partialTicks) * distance, 0, MAX_TRANSFER_DISTANCE);
	}

	@Override
	protected void invalidateCapability() {
		capability.invalidate();
	}

	@Override
	protected void stopTransferring() {
		LazyOptional<IFluidHandler> oldCapability = capability;
		capability = createEmptyHandler();
		oldCapability.invalidate();
		super.stopTransferring();
	}

	private LazyOptional<IFluidHandler> createEmptyHandler() {
		return LazyOptional.of(() -> new TerminalFluidHandler(new FluidTank(0)));
	}

	@Override
	public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
		if (cap == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY && side != Direction.DOWN)
			return capability.cast();
		return super.getCapability(cap, side);
	}

	private class TerminalFluidHandler implements IFluidHandler {

		private final IFluidHandler wrapped;

		private TerminalFluidHandler(IFluidHandler wrapped) {
			this.wrapped = wrapped;
		}

		@Override
		public int getTanks() {
			return wrapped.getTanks();
		}

		@Override
		public FluidStack getFluidInTank(int tank) {
			return wrapped.getFluidInTank(tank);
		}

		@Override
		public int getTankCapacity(int tank) {
			return wrapped.getTankCapacity(tank);
		}

		@Override
		public boolean isFluidValid(int tank, FluidStack stack) {
			return wrapped.isFluidValid(tank, stack);
		}

		@Override
		public int fill(FluidStack resource, FluidAction action) {
			if (!canTransfer())
				return 0;
			int filled = wrapped.fill(resource, action);
			if (filled > 0 && action.execute())
				onContentTransferred();
			return filled;
		}

		@Override
		public FluidStack drain(FluidStack resource, FluidAction action) {
			if (!canTransfer())
				return FluidStack.EMPTY;
			FluidStack drained = wrapped.drain(resource, action);
			if (!drained.isEmpty() && action.execute())
				onContentTransferred();
			return drained;
		}

		@Override
		public FluidStack drain(int maxDrain, FluidAction action) {
			if (!canTransfer())
				return FluidStack.EMPTY;
			FluidStack drained = wrapped.drain(maxDrain, action);
			if (!drained.isEmpty() && action.execute())
				onContentTransferred();
			return drained;
		}
	}
}
