package ua.pp.shurgent.railward.block.train.parts;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;

public class CisternBlockEntity extends FluidTankBlockEntity {

	public static final int WIDTH_SMALL = 3;
	public static final int WIDTH_LARGE = 5;
	public static final int MAX_LENGTH = 11;

	public CisternBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
		window = false;
		updateConnectivity = true;
	}

	public void queueConnectivityUpdate() {
		updateConnectivity = true;
	}

	@Override
	protected void updateConnectivity() {
		updateConnectivity = false;
		if (level == null || level.isClientSide)
			return;
		CisternConnectivity.formFrom(this);
	}

	@Override
	protected void onFluidStackChanged(FluidStack newFluidStack) {
		if (!hasLevel() || level.isClientSide)
			return;
		setChanged();
		sendData();
	}

	@SuppressWarnings("unchecked")
	@Override
	public CisternBlockEntity getControllerBE() {
		if (isController())
			return this;
		if (level == null || controller == null)
			return null;
		BlockEntity blockEntity = level.getBlockEntity(controller);
		return blockEntity instanceof CisternBlockEntity cistern ? cistern : null;
	}

	@Override
	public Direction.Axis getMainConnectionAxis() {
		return getCisternAxis();
	}

	public Direction.Axis getCisternAxis() {
		BlockState state = getBlockState();
		if (state.hasProperty(CisternBlock.AXIS))
			return state.getValue(CisternBlock.AXIS);
		return Direction.Axis.X;
	}

	public int getCisternWidth() {
		return width;
	}

	public int getCisternLength() {
		return height;
	}

	@Override
	public int getMaxLength(Direction.Axis longAxis, int width) {
		return longAxis == Direction.Axis.Y || !isValidWidth(width) ? 0 : MAX_LENGTH;
	}

	@Override
	public int getMaxWidth() {
		return WIDTH_LARGE;
	}

	@Override
	public void notifyMultiUpdated() {
		onFluidStackChanged(tankInventory.getFluid());
		if (level != null && level.isClientSide)
			invalidateRenderBoundingBox();
		setChanged();
		sendData();
	}

	@Override
	public void setWindows(boolean window) {
		this.window = false;
	}

	@Override
	public void toggleWindows() {
	}

	@Override
	protected AABB createRenderBoundingBox() {
		if (!isController())
			return super.createRenderBoundingBox();
		return getCisternAxis() == Direction.Axis.X
			? super.createRenderBoundingBox().expandTowards(height - 1, width - 1, width - 1)
			: super.createRenderBoundingBox().expandTowards(width - 1, width - 1, height - 1);
	}

	@Override
	protected void read(CompoundTag compound, boolean clientPacket) {
		super.read(compound, clientPacket);
		window = false;
	}

	@Override
	@Nonnull
	public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
		if (cap == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY && side != null
			&& !isPipeConnectionAllowed(side))
			return LazyOptional.empty();
		return super.getCapability(cap, side);
	}

	void becomeController(int width, int length, FluidStack fluid) {
		setController(worldPosition);
		setWidth(width);
		setHeight(length);
		applyFluidTankSize(width * width * length);
		replaceFluid(fluid);
		preventConnectivityUpdate();
		notifyMultiUpdated();
	}

	void becomePart(BlockPos controllerPos, int width, int length) {
		setController(controllerPos);
		setWidth(width);
		setHeight(length);
		replaceFluid(FluidStack.EMPTY);
		applyFluidTankSize(1);
		preventConnectivityUpdate();
		notifyMultiUpdated();
	}

	void becomeSingle(FluidStack fluid) {
		setController(worldPosition);
		setWidth(1);
		setHeight(1);
		applyFluidTankSize(1);
		replaceFluid(fluid);
		preventConnectivityUpdate();
		notifyMultiUpdated();
	}

	FluidStack getFluidForConnectivity() {
		return tankInventory.getFluid()
			.copy();
	}

	FluidStack drainFluidForConnectivity() {
		return tankInventory.drain(tankInventory.getCapacity(), IFluidHandler.FluidAction.EXECUTE);
	}

	private void replaceFluid(FluidStack fluid) {
		tankInventory.drain(tankInventory.getCapacity(), IFluidHandler.FluidAction.EXECUTE);
		if (!fluid.isEmpty())
			tankInventory.fill(fluid, IFluidHandler.FluidAction.EXECUTE);
	}

	private boolean isPipeConnectionAllowed(Direction side) {
		CisternBlockEntity controllerBE = getControllerBE();
		if (controllerBE == null)
			return false;

		int width = controllerBE.getCisternWidth();
		int length = controllerBE.getCisternLength();
		if (!isValidWidth(width) || length < 1)
			return false;

		BlockPos origin = controllerBE.getBlockPos();
		Direction.Axis axis = controllerBE.getCisternAxis();
		int longIndex = axis == Direction.Axis.X ? worldPosition.getX() - origin.getX()
			: worldPosition.getZ() - origin.getZ();
		int horizontalCross = axis == Direction.Axis.X ? worldPosition.getZ() - origin.getZ()
			: worldPosition.getX() - origin.getX();
		int verticalCross = worldPosition.getY() - origin.getY();

		if (longIndex < 0 || longIndex >= length || horizontalCross < 0 || horizontalCross >= width
			|| verticalCross < 0 || verticalCross >= width)
			return false;

		if (side.getAxis() == axis)
			return isAllowedEndPort(side, axis, width, length, longIndex, horizontalCross, verticalCross);

		return isAllowedSidePort(side, axis, width, length, longIndex, horizontalCross, verticalCross);
	}

	private boolean isAllowedEndPort(Direction side, Direction.Axis axis, int width, int length, int longIndex,
		int horizontalCross, int verticalCross) {
		boolean negativeEnd = side.getAxisDirection() == Direction.AxisDirection.NEGATIVE && longIndex == 0;
		boolean positiveEnd = side.getAxisDirection() == Direction.AxisDirection.POSITIVE && longIndex == length - 1;
		if (!negativeEnd && !positiveEnd)
			return false;
		if (width == 1)
			return horizontalCross == 0 && verticalCross == 0;
		if (width == WIDTH_SMALL)
			return horizontalCross == 1 && verticalCross == 1;
		return horizontalCross >= 1 && horizontalCross <= 3 && verticalCross >= 1 && verticalCross <= 3;
	}

	private boolean isAllowedSidePort(Direction side, Direction.Axis axis, int width, int length, int longIndex,
		int horizontalCross, int verticalCross) {
		if (width == 1)
			return false;
		if (longIndex <= 0 || longIndex >= length - 1)
			return false;

		int center = width / 2;
		if (side == Direction.UP)
			return verticalCross == width - 1 && horizontalCross == center;
		if (side == Direction.DOWN)
			return verticalCross == 0 && horizontalCross == center;

		if (axis == Direction.Axis.X) {
			if (side == Direction.NORTH)
				return horizontalCross == 0 && verticalCross == center;
			if (side == Direction.SOUTH)
				return horizontalCross == width - 1 && verticalCross == center;
		} else {
			if (side == Direction.WEST)
				return horizontalCross == 0 && verticalCross == center;
			if (side == Direction.EAST)
				return horizontalCross == width - 1 && verticalCross == center;
		}
		return false;
	}

	public static boolean isValidWidth(int width) {
		return width == 1 || width == WIDTH_SMALL || width == WIDTH_LARGE;
	}

}
