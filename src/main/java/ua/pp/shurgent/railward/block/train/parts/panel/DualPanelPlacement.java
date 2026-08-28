package ua.pp.shurgent.railward.block.train.parts.panel;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import ua.pp.shurgent.railward.block.train.parts.TrainWallPanelBlock;
import ua.pp.shurgent.railward.block.train.parts.TrainWallPanelBlockEntity;
import ua.pp.shurgent.railward.block.train.parts.copycat.CopycatWallPanelBlock;
import ua.pp.shurgent.railward.block.train.parts.copycat.DualPanelCopycatBlockEntity;
import ua.pp.shurgent.railward.block.train.parts.copycat.HalfWallPanelBlock;

/** Shared placement calculation used by both the item action and its client preview. */
public final class DualPanelPlacement {
	private static final double END_ZONE = 3 / 16d;
	private static final double EPSILON = 1 / 1024d;

	private DualPanelPlacement() {
	}

	public static boolean hasPlacement(Player player, BlockHitResult hit) {
		for (InteractionHand hand : InteractionHand.values()) {
			ItemStack held = player.getItemInHand(hand);
			if (!(held.getItem() instanceof DualPanelBlockItem item))
				continue;
			if (find(new UseOnContext(player, hand, hit), item.getBlock()) != null)
				return true;
		}
		return false;
	}

	@Nullable
	public static Placement find(UseOnContext context, Block heldBlock) {
		Player player = context.getPlayer();
		if (player == null || player.isShiftKeyDown())
			return null;
		Level level = context.getLevel();
		Placement direct = findDirectEndPlacement(context, heldBlock);
		if (direct != null)
			return direct;

		BlockPlaceContext placeContext = new BlockPlaceContext(context);
		BlockPos targetPos = placeContext.getClickedPos();
		// A merge is only an ordinary placement into the block next to the clicked support.
		// The support may itself be a panel (for example, directly above or below the target).
		if (targetPos.equals(context.getClickedPos()))
			return null;
		BlockState primary = level.getBlockState(targetPos);
		BlockState normallyPlaced = heldBlock.getStateForPlacement(placeContext);
		if (normallyPlaced == null)
			return null;
		return createPlacement(level, targetPos, primary, heldBlock, normallyPlaced);
	}

	@Nullable
	private static Placement findDirectEndPlacement(UseOnContext context, Block heldBlock) {
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();
		BlockState primary = level.getBlockState(pos);
		Direction primaryFacing;
		AABB bounds;
		if (primary.getBlock() instanceof TrainWallPanelBlock) {
			primaryFacing = primary.getValue(TrainWallPanelBlock.FACING);
			bounds = TrainWallPanelBlock.getPanelBounds(primaryFacing);
		} else if (primary.getBlock() instanceof CopycatWallPanelBlock) {
			primaryFacing = primary.getValue(CopycatWallPanelBlock.FACING);
			bounds = CopycatWallPanelBlock.getBounds(primary);
		} else if (primary.getBlock() instanceof HalfWallPanelBlock) {
			primaryFacing = primary.getValue(HalfWallPanelBlock.FACING);
			bounds = HalfWallPanelBlock.getBounds(primary);
		} else {
			return null;
		}

		Vec3 localHit = context.getClickLocation().subtract(pos.getX(), pos.getY(), pos.getZ());
		if (!bounds.inflate(EPSILON).contains(localHit))
			return null;
		Direction.Axis endAxis = primaryFacing.getAxis() == Direction.Axis.X
				? Direction.Axis.Z : Direction.Axis.X;
		double coordinate = endAxis.choose(localHit.x, localHit.y, localHit.z);
		double min = endAxis.choose(bounds.minX, bounds.minY, bounds.minZ);
		double max = endAxis.choose(bounds.maxX, bounds.maxY, bounds.maxZ);
		Direction facing;
		if (coordinate <= min + END_ZONE + EPSILON)
			facing = endAxis == Direction.Axis.X ? Direction.WEST : Direction.NORTH;
		else if (coordinate >= max - END_ZONE - EPSILON)
			facing = endAxis == Direction.Axis.X ? Direction.EAST : Direction.SOUTH;
		else
			return null;

		BlockState secondary = heldBlock.defaultBlockState();
		if (heldBlock instanceof TrainWallPanelBlock)
			secondary = secondary.setValue(TrainWallPanelBlock.FACING, facing);
		else if (heldBlock instanceof HalfWallPanelBlock)
			secondary = secondary.setValue(HalfWallPanelBlock.FACING, facing);
		else if (heldBlock instanceof CopycatWallPanelBlock)
			secondary = secondary.setValue(CopycatWallPanelBlock.FACING, facing);
		else
			return null;
		return createPlacement(level, pos, primary, heldBlock, secondary);
	}

	@Nullable
	private static Placement createPlacement(Level level, BlockPos targetPos, BlockState primary, Block heldBlock,
			BlockState normallyPlaced) {
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
