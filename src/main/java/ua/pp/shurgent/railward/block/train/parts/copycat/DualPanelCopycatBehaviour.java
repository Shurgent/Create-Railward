package ua.pp.shurgent.railward.block.train.parts.copycat;

import javax.annotation.Nullable;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.decoration.copycat.CopycatBlock;
import com.simibubi.create.content.decoration.copycat.CopycatBlockEntity;
import com.simibubi.create.content.redstone.RoseQuartzLampBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class DualPanelCopycatBehaviour {

	private static final double EPSILON = 1 / 1024d;

	private DualPanelCopycatBehaviour() {
	}

	@Nullable
	public static InteractionResult use(CopycatBlock block, BlockState state, Level level, BlockPos pos,
			Player player, InteractionHand hand, BlockHitResult hit) {
		if (player == null || !player.mayBuild())
			return null;
		DualPanelCopycatBlockEntityExtension data = getData(level, pos);
		if (data == null)
			return null;

		ItemStack held = player.getItemInHand(hand);

		if (!isSecondaryHit(state, data, hit.getLocation(), pos))
			return null;
		BlockState material = block.getAcceptedBlockState(level, pos, held, hit.getDirection());
		if (material != null)
			material = block.prepareMaterial(level, pos, state, player, hand, hit, material);
		if (material == null)
			return null;

		if (data.railward$getSecondaryMaterial().is(material.getBlock())) {
			if (level.isClientSide())
				return InteractionResult.SUCCESS;
			if (!cycleSecondaryMaterial(data))
				return InteractionResult.PASS;
			level.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, .75f, .95f);
			return InteractionResult.SUCCESS;
		}
		if (hasSecondaryMaterial(data))
			return InteractionResult.PASS;
		if (level.isClientSide())
			return InteractionResult.SUCCESS;

		data.railward$setSecondaryMaterial(material);
		data.railward$setSecondaryConsumedItem(held);
		level.playSound(null, pos, material.getSoundType().getPlaceSound(), SoundSource.BLOCKS, 1, .75f);
		if (!player.isCreative()) {
			held.shrink(1);
			if (held.isEmpty())
				player.setItemInHand(hand, ItemStack.EMPTY);
		}
		return InteractionResult.SUCCESS;
	}

	@Nullable
	public static InteractionResult onWrenched(BlockState state, UseOnContext context) {
		DualPanelCopycatBlockEntityExtension data = getData(context.getLevel(), context.getClickedPos());
		if (data == null || !isSecondaryHit(state, data, context.getClickLocation(), context.getClickedPos()))
			return null;
		if (!hasSecondaryMaterial(data))
			return InteractionResult.PASS;
		Level level = context.getLevel();
		if (level.isClientSide())
			return InteractionResult.SUCCESS;

		Player player = context.getPlayer();
		ItemStack refund = data.railward$getSecondaryConsumedItem().copy();
		if (player != null && !player.isCreative() && !refund.isEmpty())
			player.getInventory().placeItemBackInInventory(refund);
		level.levelEvent(2001, context.getClickedPos(), Block.getId(data.railward$getSecondaryMaterial()));
		data.railward$clearSecondaryMaterial();
		return InteractionResult.SUCCESS;
	}

	@Nullable
	public static InteractionResult onSneakWrenched(BlockState state, UseOnContext context) {
		DualPanelCopycatBlockEntityExtension data = getData(context.getLevel(), context.getClickedPos());
		if (data == null || !isSecondaryHit(state, data, context.getClickLocation(), context.getClickedPos()))
			return null;
		Level level = context.getLevel();
		if (level.isClientSide())
			return InteractionResult.SUCCESS;

		Player player = context.getPlayer();
		if (player != null && !player.isCreative()) {
			player.getInventory().placeItemBackInInventory(new ItemStack(state.getBlock()));
			ItemStack material = data.railward$getSecondaryConsumedItem().copy();
			if (!material.isEmpty())
				player.getInventory().placeItemBackInInventory(material);
		}
		if (hasSecondaryMaterial(data))
			level.levelEvent(2001, context.getClickedPos(), Block.getId(data.railward$getSecondaryMaterial()));
		data.railward$clearSecondary();
		level.playSound(null, context.getClickedPos(), SoundEvents.ITEM_FRAME_REMOVE_ITEM,
				SoundSource.BLOCKS, .75f, 1);
		return InteractionResult.SUCCESS;
	}

	public static VoxelShape addSecondaryShape(BlockState state, BlockGetter level, BlockPos pos,
			VoxelShape primaryShape) {
		DualPanelCopycatBlockEntityExtension data = getData(level, pos);
		if (data == null || data.railward$getSecondaryFacing() == null)
			return primaryShape;
		return Shapes.or(primaryShape, Shapes.create(getSecondaryBounds(state, data.railward$getSecondaryFacing())));
	}

	public static AABB getPrimaryBounds(BlockState state) {
		if (state.getBlock() instanceof HalfWallPanelBlock)
			return HalfWallPanelBlock.getBounds(state);
		return CopycatWallPanelBlock.getBounds(state);
	}

	public static AABB getSecondaryBounds(BlockState state, Direction secondaryFacing) {
		AABB primary = getPrimaryBounds(state);
		AABB secondary;
		if (state.getBlock() instanceof HalfWallPanelBlock) {
			BlockState synthetic = state.setValue(HalfWallPanelBlock.FACING, secondaryFacing);
			secondary = HalfWallPanelBlock.getBounds(synthetic);
		} else {
			BlockState synthetic = state.setValue(CopycatWallPanelBlock.FACING, secondaryFacing);
			secondary = CopycatWallPanelBlock.getBounds(synthetic);
		}

		Direction.Axis primaryAxis = primaryAxis(state);
		boolean primaryAtLowSide = axisMin(primary, primaryAxis) < .25;
		return setAxisRange(secondary, primaryAxis,
				primaryAtLowSide ? axisMax(primary, primaryAxis) : axisMin(secondary, primaryAxis),
				primaryAtLowSide ? axisMax(secondary, primaryAxis) : axisMin(primary, primaryAxis));
	}

	public static boolean isSupported(BlockState state) {
		return state.getBlock() instanceof CopycatWallPanelBlock || state.getBlock() instanceof HalfWallPanelBlock;
	}

	public static void dropSecondary(Level level, BlockPos pos, BlockState state) {
		DualPanelCopycatBlockEntityExtension data = getData(level, pos);
		if (data == null || data.railward$getSecondaryFacing() == null)
			return;
		Block.popResource(level, pos, new ItemStack(state.getBlock()));
		Block.popResource(level, pos, data.railward$getSecondaryConsumedItem().copy());
		data.railward$clearSecondary();
	}

	public static void clearSecondaryForCreative(BlockGetter level, BlockPos pos) {
		DualPanelCopycatBlockEntityExtension data = getData(level, pos);
		if (data != null)
			data.railward$clearSecondary();
	}

	private static boolean isSecondaryHit(BlockState state, DualPanelCopycatBlockEntityExtension data,
			Vec3 worldHit, BlockPos pos) {
		Direction facing = data.railward$getSecondaryFacing();
		if (facing == null)
			return false;
		Vec3 hit = worldHit.subtract(pos.getX(), pos.getY(), pos.getZ());
		AABB secondary = getSecondaryBounds(state, facing).inflate(EPSILON);
		AABB primary = getPrimaryBounds(state).inflate(EPSILON);
		return secondary.contains(hit) && !primary.contains(hit);
	}

	private static boolean cycleSecondaryMaterial(DualPanelCopycatBlockEntityExtension data) {
		BlockState material = data.railward$getSecondaryMaterial();
		BlockState cycled;
		if (material.hasProperty(TrapDoorBlock.HALF)
				&& material.getOptionalValue(TrapDoorBlock.OPEN).orElse(false))
			cycled = material.cycle(TrapDoorBlock.HALF);
		else if (material.hasProperty(BlockStateProperties.FACING))
			cycled = material.cycle(BlockStateProperties.FACING);
		else if (material.hasProperty(BlockStateProperties.HORIZONTAL_FACING))
			cycled = material.setValue(BlockStateProperties.HORIZONTAL_FACING,
					material.getValue(BlockStateProperties.HORIZONTAL_FACING).getClockWise());
		else if (material.hasProperty(BlockStateProperties.AXIS))
			cycled = material.cycle(BlockStateProperties.AXIS);
		else if (material.hasProperty(BlockStateProperties.HORIZONTAL_AXIS))
			cycled = material.cycle(BlockStateProperties.HORIZONTAL_AXIS);
		else if (material.hasProperty(BlockStateProperties.LIT))
			cycled = material.cycle(BlockStateProperties.LIT);
		else if (material.hasProperty(RoseQuartzLampBlock.POWERING))
			cycled = material.cycle(RoseQuartzLampBlock.POWERING);
		else
			return false;
		data.railward$setSecondaryMaterial(cycled);
		return true;
	}

	private static boolean hasSecondaryMaterial(DualPanelCopycatBlockEntityExtension data) {
		return !AllBlocks.COPYCAT_BASE.has(data.railward$getSecondaryMaterial());
	}

	@Nullable
	private static DualPanelCopycatBlockEntityExtension getData(BlockGetter level, BlockPos pos) {
		BlockEntity blockEntity = level.getBlockEntity(pos);
		return blockEntity instanceof CopycatBlockEntity && blockEntity instanceof DualPanelCopycatBlockEntityExtension data
				? data : null;
	}

	private static Direction.Axis primaryAxis(BlockState state) {
		return state.getBlock() instanceof HalfWallPanelBlock
				? state.getValue(HalfWallPanelBlock.FACING).getAxis()
				: state.getValue(CopycatWallPanelBlock.FACING).getAxis();
	}

	private static double axisMin(AABB box, Direction.Axis axis) {
		return axis.choose(box.minX, box.minY, box.minZ);
	}

	private static double axisMax(AABB box, Direction.Axis axis) {
		return axis.choose(box.maxX, box.maxY, box.maxZ);
	}

	private static AABB setAxisRange(AABB box, Direction.Axis axis, double min, double max) {
		return switch (axis) {
			case X -> new AABB(min, box.minY, box.minZ, max, box.maxY, box.maxZ);
			case Y -> new AABB(box.minX, min, box.minZ, box.maxX, max, box.maxZ);
			case Z -> new AABB(box.minX, box.minY, min, box.maxX, box.maxY, max);
		};
	}
}
