package ua.pp.shurgent.railward.block.train.parts;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;

public final class BulkCargoStorage {

	private BulkCargoStorage() {
	}

	public static ItemStack getDisplayStack(IItemHandler handler) {
		if (handler == null)
			return ItemStack.EMPTY;
		for (int slot = 0; slot < handler.getSlots(); slot++) {
			ItemStack stack = handler.getStackInSlot(slot);
			if (stack.isEmpty())
				continue;
			ItemStack display = stack.copy();
			display.setCount(1);
			return display;
		}
		return ItemStack.EMPTY;
	}

	public static int getStoredAmount(IItemHandler handler) {
		if (handler == null)
			return 0;
		int amount = 0;
		for (int slot = 0; slot < handler.getSlots(); slot++)
			amount += handler.getStackInSlot(slot)
				.getCount();
		return amount;
	}

	public static int getCapacityItems(IItemHandler handler, ItemStack displayStack) {
		if (handler == null)
			return 0;
		int stackLimit = displayStack.isEmpty() ? 64 : displayStack.getMaxStackSize();
		int capacity = 0;
		for (int slot = 0; slot < handler.getSlots(); slot++)
			capacity += Math.min(handler.getSlotLimit(slot), stackLimit);
		return capacity;
	}

	public static float getFillState(IItemHandler handler) {
		ItemStack displayStack = getDisplayStack(handler);
		int capacity = getCapacityItems(handler, displayStack);
		if (capacity <= 0)
			return 0;
		return Math.min(1, getStoredAmount(handler) / (float) capacity);
	}

	public static ItemStack insert(IItemHandlerModifiable handler, ItemStack stack, boolean simulate) {
		if (stack.isEmpty())
			return ItemStack.EMPTY;
		if (handler == null)
			return stack;

		ItemStack existing = getDisplayStack(handler);
		if (!existing.isEmpty() && !canStackAsCargo(existing, stack))
			return stack;

		ItemStack remainder = stack.copy();
		for (int slot = 0; slot < handler.getSlots(); slot++) {
			if (remainder.isEmpty())
				break;
			remainder = handler.insertItem(slot, remainder, simulate);
		}
		return remainder;
	}

	public static ItemStack extractOneStack(IItemHandlerModifiable handler, boolean simulate) {
		ItemStack display = getDisplayStack(handler);
		if (handler == null || display.isEmpty())
			return ItemStack.EMPTY;

		ItemStack extracted = display.copy();
		extracted.setCount(0);
		int target = display.getMaxStackSize();
		for (int slot = 0; slot < handler.getSlots(); slot++) {
			if (extracted.getCount() >= target)
				break;
			ItemStack inSlot = handler.getStackInSlot(slot);
			if (inSlot.isEmpty() || !canStackAsCargo(display, inSlot))
				continue;
			ItemStack pulled = handler.extractItem(slot, target - extracted.getCount(), simulate);
			if (pulled.isEmpty())
				continue;
			if (extracted.isEmpty())
				extracted = pulled.copy();
			else
				extracted.grow(pulled.getCount());
		}
		return extracted;
	}

	public static List<ItemStack> extractAll(IItemHandlerModifiable handler) {
		List<ItemStack> stacks = new ArrayList<>();
		if (handler == null)
			return stacks;
		for (int slot = 0; slot < handler.getSlots(); slot++) {
			ItemStack stack = handler.getStackInSlot(slot);
			if (!stack.isEmpty())
				stacks.add(stack.copy());
			handler.setStackInSlot(slot, ItemStack.EMPTY);
		}
		return stacks;
	}

	public static boolean canStackAsCargo(ItemStack first, ItemStack second) {
		return ItemHandlerHelper.canItemStacksStack(first, second);
	}

	public static class CargoItemHandler extends ItemStackHandler {
		public CargoItemHandler(int size) {
			super(size);
		}

		@Override
		public boolean isItemValid(int slot, ItemStack stack) {
			ItemStack display = BulkCargoStorage.getDisplayStack(this);
			return display.isEmpty() || BulkCargoStorage.canStackAsCargo(display, stack);
		}

		@Override
		public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
			if (!isItemValid(slot, stack))
				return stack;
			return super.insertItem(slot, stack, simulate);
		}
	}

}
