package ua.pp.shurgent.railward.block.train.parts.copycat;

import com.simibubi.create.content.contraptions.StructureTransform;
import com.simibubi.create.content.decoration.copycat.CopycatBlockEntity;
import com.simibubi.create.content.decoration.copycat.WaterloggedCopycatBlock;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import com.simibubi.create.content.schematics.requirement.ItemRequirement.ItemUseType;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import ua.pp.shurgent.railward.AllBlockEntities;
import ua.pp.shurgent.railward.compat.RailwardSpecialBlockItemRequirement;
import ua.pp.shurgent.railward.compat.RailwardTransformableBlock;

public class CopycatSlabBlock extends WaterloggedCopycatBlock
		implements RailwardTransformableBlock, RailwardSpecialBlockItemRequirement {

	public static final EnumProperty<SlabType> TYPE = BlockStateProperties.SLAB_TYPE;
	private static final VoxelShape BOTTOM_SHAPE = Block.box(0, 0, 0, 16, 8, 16);
	private static final VoxelShape TOP_SHAPE = Block.box(0, 8, 0, 16, 16, 16);

	public CopycatSlabBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(TYPE, SlabType.BOTTOM));
	}

	@Override
	public BlockEntityType<? extends CopycatBlockEntity> getBlockEntityType() {
		return AllBlockEntities.COPYCAT_SLAB.get();
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockState existing = context.getLevel().getBlockState(context.getClickedPos());
		if (existing.is(this))
			return existing.setValue(TYPE, SlabType.DOUBLE).setValue(WATERLOGGED, false);

		BlockState state = super.getStateForPlacement(context);
		if (state == null)
			return null;
		Direction face = context.getClickedFace();
		double localY = context.getClickLocation().y - context.getClickedPos().getY();
		boolean top = face == Direction.DOWN || face != Direction.UP && localY > .5;
		return state.setValue(TYPE, top ? SlabType.TOP : SlabType.BOTTOM);
	}

	@Override
	public boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
		if (state.getValue(TYPE) == SlabType.DOUBLE || !context.getItemInHand().is(asItem()))
			return false;
		boolean upperClick = context.getClickLocation().y - context.getClickedPos().getY() > .5;
		Direction face = context.getClickedFace();
		return state.getValue(TYPE) == SlabType.BOTTOM
				? face == Direction.UP || face.getAxis().isHorizontal() && upperClick
				: face == Direction.DOWN || face.getAxis().isHorizontal() && !upperClick;
	}

	@Override
	public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
			BlockHitResult hit) {
		if (player == null || !player.mayBuild())
			return InteractionResult.PASS;

		Half half = getTargetHalf(state, hit.getLocation(), pos);
		ItemStack held = player.getItemInHand(hand);
		BlockState material = getAcceptedBlockState(level, pos, held, hit.getDirection());
		if (material != null)
			material = prepareMaterial(level, pos, state, player, hand, hit, material);
		if (material == null)
			return InteractionResult.PASS;

		BlockEntity blockEntity = level.getBlockEntity(pos);
		if (!(blockEntity instanceof CopycatSlabBlockEntity slab))
			return InteractionResult.PASS;
		if (slab.getMaterial(half).is(material.getBlock())) {
			if (!level.isClientSide() && !slab.cycleMaterial(half))
				return InteractionResult.PASS;
			level.playSound(player, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, .75f, .95f);
			return InteractionResult.sidedSuccess(level.isClientSide());
		}
		if (slab.hasCustomMaterial(half))
			return InteractionResult.PASS;
		if (level.isClientSide())
			return InteractionResult.SUCCESS;

		boolean freeToApply = slab.otherHalfPaysFor(half, held);
		slab.setMaterial(half, material);
		if (!freeToApply)
			slab.setConsumedItem(half, held);
		level.playSound(null, pos, material.getSoundType().getPlaceSound(), SoundSource.BLOCKS, 1, .75f);
		if (!player.isCreative() && !freeToApply) {
			held.shrink(1);
			if (held.isEmpty())
				player.setItemInHand(hand, ItemStack.EMPTY);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public InteractionResult onWrenched(BlockState state, UseOnContext context) {
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();
		BlockEntity blockEntity = level.getBlockEntity(pos);
		if (!(blockEntity instanceof CopycatSlabBlockEntity slab))
			return InteractionResult.PASS;
		Half half = getTargetHalf(state, context.getClickLocation(), pos);
		if (!slab.hasCustomMaterial(half))
			return InteractionResult.PASS;
		if (level.isClientSide())
			return InteractionResult.SUCCESS;

		slab.transferPaymentToOtherHalf(half);
		ItemStack refund = slab.getConsumedItem(half).copy();
		Player player = context.getPlayer();
		if (player != null && !player.isCreative() && !refund.isEmpty())
			player.getInventory().placeItemBackInInventory(refund);
		level.levelEvent(2001, pos, Block.getId(slab.getMaterial(half)));
		slab.clear(half);
		return InteractionResult.SUCCESS;
	}

	@Override
	public InteractionResult onSneakWrenched(BlockState state, UseOnContext context) {
		if (state.getValue(TYPE) != SlabType.DOUBLE)
			return super.onSneakWrenched(state, context);

		Half removed = getTargetHalf(state, context.getClickLocation(), context.getClickedPos());
		onWrenched(state, context);
		Level level = context.getLevel();
		if (!level.isClientSide()) {
			Player player = context.getPlayer();
			if (player != null && !player.isCreative())
				player.getInventory().placeItemBackInInventory(new ItemStack(asItem()));
			level.setBlockAndUpdate(context.getClickedPos(),
					state.setValue(TYPE, removed == Half.TOP ? SlabType.BOTTOM : SlabType.TOP));
			level.playSound(null, context.getClickedPos(), SoundEvents.ITEM_FRAME_REMOVE_ITEM,
					SoundSource.BLOCKS, .75f, 1);
		}
		return InteractionResult.sidedSuccess(level.isClientSide());
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
		if (placer == null)
			return;
		ItemStack materialItem = placer.getItemInHand(InteractionHand.OFF_HAND);
		BlockState material = getAcceptedBlockState(level, pos, materialItem, Direction.UP);
		BlockEntity blockEntity = level.getBlockEntity(pos);
		if (material == null || !(blockEntity instanceof CopycatSlabBlockEntity slab))
			return;

		for (Half half : new Half[] { Half.BOTTOM, Half.TOP }) {
			if (!partExists(state, half) || slab.hasCustomMaterial(half))
				continue;
			boolean freeToApply = slab.otherHalfPaysFor(half, materialItem);
			slab.setMaterial(half, material);
			if (!freeToApply)
				slab.setConsumedItem(half, materialItem);
			if ((placer instanceof Player player && player.isCreative()) || freeToApply)
				continue;
			materialItem.shrink(1);
			if (materialItem.isEmpty())
				break;
		}
	}

	@Override
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
		if (!state.is(newState.getBlock()) && !isMoving
				&& level.getBlockEntity(pos) instanceof CopycatSlabBlockEntity slab)
			slab.getPaidItems().forEach(stack -> popResource(level, pos, stack));
		super.onRemove(state, level, pos, newState, isMoving);
	}

	@Override
	public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
		if (player.isCreative() && level.getBlockEntity(pos) instanceof CopycatSlabBlockEntity slab)
			slab.clearPaidItems();
		super.playerWillDestroy(level, pos, state, player);
	}

	@Override
	public ItemRequirement getRequiredItems(BlockState state, BlockEntity blockEntity) {
		return new ItemRequirement(ItemUseType.CONSUME,
				new ItemStack(asItem(), state.getValue(TYPE) == SlabType.DOUBLE ? 2 : 1));
	}

	@Override
	protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder.add(TYPE));
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return switch (state.getValue(TYPE)) {
			case BOTTOM -> BOTTOM_SHAPE;
			case TOP -> TOP_SHAPE;
			case DOUBLE -> Shapes.block();
		};
	}

	@Override
	public boolean isPathfindable(BlockState state, BlockGetter level, BlockPos pos, PathComputationType type) {
		return false;
	}

	//? if <1.19.2 {
	@Override
	public BlockState getConnectiveMaterial(BlockAndTintGetter reader, BlockState otherState, Direction face,
			BlockPos fromPos, BlockPos toPos) {
		return null;
	}
	//?} else {
	/*@Override
	public boolean canConnectTexturesToward(BlockAndTintGetter reader, BlockPos fromPos, BlockPos toPos,
			BlockState state) {
		return false;
	}*/
	//?}

	//? if <1.19.2 {
	@Override
	public boolean isUnblockableConnectivitySide(BlockAndTintGetter reader, BlockState state, Direction face,
			BlockPos fromPos, BlockPos toPos) {
		return true;
	}
	//?}

	@Override
	public boolean supportsExternalFaceHiding(BlockState state) {
		return true;
	}

	@Override
	public boolean hidesNeighborFace(BlockGetter level, BlockPos pos, BlockState state, BlockState neighborState,
			Direction direction) {
		return false;
	}

	@Override
	public BlockState rotate(BlockState state, Rotation rotation) {
		return state;
	}

	@Override
	public BlockState mirror(BlockState state, Mirror mirror) {
		return state;
	}

	@Override
	public BlockState transform(BlockState state, StructureTransform transform) {
		Vec3 transformedUp = transform.applyWithoutOffsetUncentered(new Vec3(0, 1, 0));
		if (transformedUp.y < 0 && state.getValue(TYPE) != SlabType.DOUBLE)
			return state.setValue(TYPE,
					state.getValue(TYPE) == SlabType.TOP ? SlabType.BOTTOM : SlabType.TOP);
		return state;
	}

	public static Half getTargetHalf(BlockState state, Vec3 hit, BlockPos pos) {
		if (state.getValue(TYPE) == SlabType.TOP)
			return Half.TOP;
		if (state.getValue(TYPE) == SlabType.BOTTOM)
			return Half.BOTTOM;
		return hit.y - pos.getY() > .5 ? Half.TOP : Half.BOTTOM;
	}

	public static boolean partExists(BlockState state, Half half) {
		SlabType type = state.getValue(TYPE);
		return type == SlabType.DOUBLE || type == SlabType.TOP && half == Half.TOP
				|| type == SlabType.BOTTOM && half == Half.BOTTOM;
	}
}
