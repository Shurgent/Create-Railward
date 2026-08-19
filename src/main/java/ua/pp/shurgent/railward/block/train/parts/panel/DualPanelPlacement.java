package ua.pp.shurgent.railward.block.train.parts.panel;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import ua.pp.shurgent.railward.block.train.parts.TrainWallPanelBlock;
import ua.pp.shurgent.railward.block.train.parts.TrainWallPanelBlockEntity;
import ua.pp.shurgent.railward.block.train.parts.copycat.CopycatWallPanelBlock;
import ua.pp.shurgent.railward.block.train.parts.copycat.DualPanelCopycatBlockEntity;
import ua.pp.shurgent.railward.block.train.parts.copycat.HalfWallPanelBlock;

/** Shared placement calculation used by both the item action and its client preview. */
public final class DualPanelPlacement {

	private DualPanelPlacement() {
	}

	@Nullable
	public static Placement find(UseOnContext context, Block heldBlock) {
		Player player = context.getPlayer();
		if (player == null || player.isShiftKeyDown())
			return null;
		Level level = context.getLevel();
		BlockPlaceContext placeContext = new BlockPlaceContext(context);
		BlockPos targetPos = placeContext.getClickedPos();
		BlockState primary = level.getBlockState(targetPos);
		BlockState normallyPlaced = heldBlock.getStateForPlacement(placeContext);
		if (normallyPlaced == null)
			return null;

		if (primary.getBlock() instanceof TrainWallPanelBlock
				&& heldBlock instanceof TrainWallPanelBlock
				&& level.getBlockEntity(targetPos) instanceof TrainWallPanelBlockEntity panel
				&& !panel.hasSecondaryPanel()) {
			Direction facing = normallyPlaced.getValue(TrainWallPanelBlock.FACING);
			if (primary.getValue(TrainWallPanelBlock.FACING).getAxis() != facing.getAxis())
				return new Placement(targetPos, primary, normallyPlaced, Kind.TRAIN_PANEL);
			return null;
		}

		boolean matchingCopycat = primary.getBlock() == heldBlock
				&& (heldBlock instanceof CopycatWallPanelBlock || heldBlock instanceof HalfWallPanelBlock);
		if (!matchingCopycat
				|| !(level.getBlockEntity(targetPos) instanceof DualPanelCopycatBlockEntity panel)
				|| panel.railward$getSecondaryFacing() != null)
			return null;
		Direction primaryFacing = primary.getBlock() instanceof HalfWallPanelBlock
				? primary.getValue(HalfWallPanelBlock.FACING)
				: primary.getValue(CopycatWallPanelBlock.FACING);
		Direction facing = normallyPlaced.getBlock() instanceof HalfWallPanelBlock
				? normallyPlaced.getValue(HalfWallPanelBlock.FACING)
				: normallyPlaced.getValue(CopycatWallPanelBlock.FACING);
		if (primaryFacing.getAxis() == facing.getAxis())
			return null;
		BlockState secondary = primary.getBlock() instanceof HalfWallPanelBlock
				? primary.setValue(HalfWallPanelBlock.FACING, facing)
				: primary.setValue(CopycatWallPanelBlock.FACING, facing);
		return new Placement(targetPos, primary, secondary, Kind.COPYCAT);
	}

	@Nullable
	public static InteractionResult tryPlace(UseOnContext context, Block heldBlock) {
		Placement placement = find(context, heldBlock);
		if (placement == null)
			return null;
		Player player = context.getPlayer();
		if (player == null || !player.mayBuild()
				|| !context.getLevel().mayInteract(player, placement.pos()))
			return InteractionResult.FAIL;
		Level level = context.getLevel();
		if (level.isClientSide())
			return InteractionResult.SUCCESS;

		if (placement.kind() == Kind.TRAIN_PANEL) {
			TrainWallPanelBlockEntity panel = (TrainWallPanelBlockEntity) level.getBlockEntity(placement.pos());
			panel.setSecondaryPanel(placement.secondary());
		} else {
			DualPanelCopycatBlockEntity panel = (DualPanelCopycatBlockEntity) level.getBlockEntity(placement.pos());
			Direction facing = placement.secondary().getBlock() instanceof HalfWallPanelBlock
					? placement.secondary().getValue(HalfWallPanelBlock.FACING)
					: placement.secondary().getValue(CopycatWallPanelBlock.FACING);
			panel.railward$setSecondaryFacing(facing);
		}

		ItemStack held = context.getItemInHand();
		level.playSound(null, placement.pos(), placement.secondary().getSoundType().getPlaceSound(),
				SoundSource.BLOCKS, 1, .8f);
		player.awardStat(Stats.ITEM_USED.get(held.getItem()));
		if (!player.isCreative()) {
			held.shrink(1);
			if (held.isEmpty())
				player.setItemInHand(context.getHand(), ItemStack.EMPTY);
		}
		return InteractionResult.SUCCESS;
	}

	public enum Kind {
		COPYCAT,
		TRAIN_PANEL
	}

	public record Placement(BlockPos pos, BlockState primary, BlockState secondary, Kind kind) {
	}
}
