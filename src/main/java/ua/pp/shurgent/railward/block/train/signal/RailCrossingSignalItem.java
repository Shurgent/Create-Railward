package ua.pp.shurgent.railward.block.train.signal;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import ua.pp.shurgent.railward.AllBlocks;
import ua.pp.shurgent.railward.compat.RailwardComponents;

public class RailCrossingSignalItem extends BlockItem {

	private static final String CABINET_POS = "CabinetPos";
	private static final String CABINET_DIMENSION = "CabinetDimension";

	public RailCrossingSignalItem(RailCrossingSignalBlock block, Properties properties) {
		super(block, properties);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Level level = context.getLevel();
		Player player = context.getPlayer();
		ItemStack stack = context.getItemInHand();
		if (cancelSelection(level, player, stack))
			return InteractionResult.sidedSuccess(level.isClientSide);

		BlockPos pos = context.getClickedPos();
		if (!AllBlocks.RELAY_CASE.has(level.getBlockState(pos)))
			return super.useOn(context);
		if (player == null || !player.mayBuild())
			return InteractionResult.FAIL;
		if (!level.isClientSide) {
			CompoundTag tag = stack.getOrCreateTag();
			tag.putLong(CABINET_POS, pos.asLong());
			tag.putString(CABINET_DIMENSION, level.dimension().location().toString());
			player.displayClientMessage(cabinetLabel(pos), true);
		}
		return InteractionResult.sidedSuccess(level.isClientSide);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		return cancelSelection(level, player, stack) ? InteractionResultHolder.sidedSuccess(stack, level.isClientSide)
			: super.use(level, player, hand);
	}

	@Override
	public InteractionResult place(BlockPlaceContext context) {
		Level level = context.getLevel();
		ItemStack stack = context.getItemInHand();
		BlockPos cabinet = selectedCabinet(stack, level);
		if (cabinet == null || !level.isClientSide && (!level.isLoaded(cabinet)
			|| !(level.getBlockEntity(cabinet) instanceof RelayCaseBlockEntity))) {
			Player player = context.getPlayer();
			if (!level.isClientSide && player != null)
				player.displayClientMessage(RailwardComponents.translatable("railward.rail_crossing_signal.select_cabinet"), true);
			return InteractionResult.FAIL;
		}
		InteractionResult result = super.place(context);
		if (result.consumesAction() && !level.isClientSide)
			clearSelection(stack);
		return result;
	}

	@Override
	public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
		BlockPos cabinet = level == null ? null : selectedCabinet(stack, level);
		tooltip.add(cabinet == null
			? RailwardComponents.translatable("railward.rail_crossing_signal.select_cabinet")
			: cabinetLabel(cabinet));
		if (hasSelection(stack))
			tooltip.add(RailwardComponents.translatable("railward.rail_crossing_signal.clear_selection_hint"));
	}

	private static boolean cancelSelection(Level level, Player player, ItemStack stack) {
		if (player == null || !player.isShiftKeyDown() || !hasSelection(stack))
			return false;
		if (!level.isClientSide) {
			clearSelection(stack);
			player.displayClientMessage(RailwardComponents.translatable("railward.rail_crossing_signal.selection_cleared"), true);
		}
		return true;
	}

	private static boolean hasSelection(ItemStack stack) {
		CompoundTag tag = stack.getTag();
		return tag != null && tag.contains(CABINET_POS, Tag.TAG_LONG);
	}

	private static void clearSelection(ItemStack stack) {
		stack.removeTagKey(CABINET_POS);
		stack.removeTagKey(CABINET_DIMENSION);
	}

	public static BlockPos selectedCabinet(ItemStack stack, Level level) {
		CompoundTag tag = stack.getTag();
		if (tag != null && tag.contains(CABINET_POS, Tag.TAG_LONG)
			&& level.dimension().location().toString().equals(tag.getString(CABINET_DIMENSION)))
			return BlockPos.of(tag.getLong(CABINET_POS));
		return null;
	}

	private static Component cabinetLabel(BlockPos cabinet) {
		return RailwardComponents.translatable("railward.rail_crossing_signal.linked_cabinet",
			cabinet.getX(), cabinet.getY(), cabinet.getZ());
	}
}
