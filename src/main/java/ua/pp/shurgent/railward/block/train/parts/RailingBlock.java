package ua.pp.shurgent.railward.block.train.parts;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.simibubi.create.AllItems;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.ContraptionWorld;
import ua.pp.shurgent.railward.compat.RailwardWrenchable;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import ua.pp.shurgent.railward.AllBlockEntities;
import ua.pp.shurgent.railward.mixin.create.ContraptionWorldAccessor;

public class RailingBlock extends HorizontalDirectionalBlock implements IBE<RailingBlockEntity>, RailwardWrenchable {

	public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

	private static final int VALID_PARTS_MASK = (1 << RailingPart.values().length) - 1;
	private static final Map<Integer, VoxelShape> CONFIGURED_SHAPES = new ConcurrentHashMap<>();
	private static final VoxelShape ALL_PARTS_SHAPE = makeAllPartsShape();

	private final String colorName;
	private final int renderColor;

	public RailingBlock(Properties properties, String colorName, int renderColor) {
		super(properties.noOcclusion());
		this.colorName = colorName;
		this.renderColor = renderColor;
		registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH));
	}

	public String getColorName() {
		return colorName;
	}

	public int getRenderColor() {
		return renderColor;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		Direction clickedFace = context.getClickedFace();
		Direction side = clickedFace.getAxis().isHorizontal()
			? clickedFace
			: context.getHorizontalDirection().getOpposite();
		return defaultBlockState().setValue(FACING, side);
	}

	@Override
	public RenderShape getRenderShape(BlockState state) {
		return RenderShape.ENTITYBLOCK_ANIMATED;
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		if (isHoldingWrench(context))
			return ALL_PARTS_SHAPE;
		return configuredShape(state, level, pos);
	}

	@Override
	public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return configuredShape(state, level, pos);
	}

	@Override
	public VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
		return Shapes.empty();
	}

	public VoxelShape getHighlightedInteractionShape(BlockPos pos, Vec3 hit) {
		RailingPart part = RailingPart.at(hit.subtract(pos.getX(), pos.getY(), pos.getZ()));
		return part == null ? Shapes.empty() : part.shape();
	}

	@Override
	public InteractionResult onWrenched(BlockState state, UseOnContext context) {
		BlockPos pos = context.getClickedPos();
		Level level = context.getLevel();
		Vec3 localHit = context.getClickLocation().subtract(pos.getX(), pos.getY(), pos.getZ());
		RailingPart part = RailingPart.at(localHit);
		if (part == null)
			return InteractionResult.PASS;

		if (level.isClientSide)
			return InteractionResult.SUCCESS;
		if (!(level.getBlockEntity(pos) instanceof RailingBlockEntity railing))
			return InteractionResult.PASS;

		int updatedParts = railing.toggledMask(part);
		if (updatedParts == 0) {
			Player player = context.getPlayer();
			level.removeBlock(pos, false);
			if (player != null && !player.getAbilities().instabuild) {
				ItemStack returned = new ItemStack(state.getBlock());
				if (!player.getInventory().add(returned))
					player.drop(returned, false);
			}
		} else {
			railing.setParts(updatedParts);
			level.sendBlockUpdated(pos, state, state, 3);
		}

		playRotateSound(level, pos);
		return InteractionResult.SUCCESS;
	}

	@Override
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
		IBE.onRemove(state, level, pos, newState);
	}

	@Override
	public Class<RailingBlockEntity> getBlockEntityClass() {
		return RailingBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends RailingBlockEntity> getBlockEntityType() {
		return AllBlockEntities.RAILING.get();
	}

	private VoxelShape configuredShape(BlockState state, BlockGetter level, BlockPos pos) {
		int mask = getContraptionParts(level, pos);
		if (mask < 0)
			mask = level.getBlockEntity(pos) instanceof RailingBlockEntity railing
				? railing.getParts()
				: RailingPart.maskForSide(state.getValue(FACING));
		return level instanceof ContraptionWorld
			? RailingContraptionCollision.shapeForParts(mask)
			: shapeForMask(mask);
	}

	public static VoxelShape shapeForMask(int mask) {
		int sanitizedMask = mask & VALID_PARTS_MASK;
		return CONFIGURED_SHAPES.computeIfAbsent(sanitizedMask, RailingBlock::makeShape);
	}

	private static VoxelShape makeShape(int mask) {
		VoxelShape result = Shapes.empty();
		for (RailingPart part : RailingPart.values())
			if ((mask & part.bit()) != 0)
				result = Shapes.or(result, part.shape());
		return result.optimize();
	}

	private static int getContraptionParts(BlockGetter level, BlockPos pos) {
		if (!(level instanceof ContraptionWorld contraptionWorld))
			return -1;
		Contraption contraption =
			((ContraptionWorldAccessor) (Object) contraptionWorld).railward$getContraption();
		StructureBlockInfo info = contraption.getBlocks().get(pos);
		return RailingContraptionCollision.partsFrom(info);
	}

	private boolean isHoldingWrench(CollisionContext context) {
		if (!(context instanceof EntityCollisionContext entityContext))
			return false;
		Entity entity = entityContext.getEntity();
		if (!(entity instanceof Player player))
			return false;
		return AllItems.WRENCH.isIn(player.getMainHandItem()) || AllItems.WRENCH.isIn(player.getOffhandItem());
	}

	private static VoxelShape makeAllPartsShape() {
		VoxelShape result = Shapes.empty();
		for (RailingPart part : RailingPart.values())
			result = Shapes.or(result, part.interactionShape());
		return result.optimize();
	}
}
