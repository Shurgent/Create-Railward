package ua.pp.shurgent.railward.block.train.signal;

import javax.annotation.Nullable;

import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.BlockEvent;
import ua.pp.shurgent.railward.AllBlockEntities;

public class ModernSignalBlock extends Block implements IBE<ModernSignalBlockEntity>, IWrenchable {

	public static final IntegerProperty ROTATION = BlockStateProperties.ROTATION_16;
	private static final ThreadLocal<Boolean> REMOVING_CLUSTER = ThreadLocal.withInitial(() -> false);
	/**
	 * Height index of the signal block. 0 is the base with the BlockEntity, 1-3 are
	 * the additional dummy blocks placed above it.
	 */
	public static final IntegerProperty PART = IntegerProperty.create("part", 0, 3);

	public ModernSignalBlock(Properties props) {
		super(BlockBehaviour.Properties.of(Material.METAL).strength(1.0f, 6f).noOcclusion().sound(SoundType.METAL));
		registerDefaultState(this.defaultBlockState().setValue(ROTATION, 0).setValue(PART, 0));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(ROTATION, PART);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext ctx) {
		Level level = ctx.getLevel();
		BlockPos pos = ctx.getClickedPos();
		// check that there is enough space for the full height
		for (int i = 0; i < 4; i++) {
			BlockPos check = pos.above(i);
			if (!level.getBlockState(check).canBeReplaced(ctx))
				return null;
		}

		int rot = Mth.floor(((ctx.getRotation()) * 16.0F / 360.0F) + 0.5D) & 15;
		return defaultBlockState().setValue(ROTATION, rot).setValue(PART, 0);
	}

	@Override
	public RenderShape getRenderShape(BlockState state) {
		return RenderShape.ENTITYBLOCK_ANIMATED;
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer,
			ItemStack stack) {
		super.setPlacedBy(level, pos, state, placer, stack);
		if (level.isClientSide)
			return;
		for (int i = 1; i < 4; i++) {
			BlockPos above = pos.above(i);
			level.setBlock(above, state.setValue(PART, i), 3);
		}
	}

	private static final VoxelShape[] SHAPES = new VoxelShape[] {
			Block.box(6, 0, 6, 10, 16, 10),
			Block.box(7, 0, 7, 9, 16, 9),
			Block.box(7, 0, 7, 9, 16, 9),
			Block.box(4, 0, 4, 12, 16, 12)
	};

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
		int part = state.getValue(PART);
		return SHAPES[Math.min(part, SHAPES.length - 1)];
	}

	@Override
	public InteractionResult onWrenched(BlockState state, UseOnContext context) {
		Level world = context.getLevel();
		BlockPos pos = context.getClickedPos();

		// на клиенте возвращаем SUCCESS, чтобы проигралась анимация ключа
		if (world.isClientSide)
			return InteractionResult.SUCCESS;

		// следующий шаг поворота
		int current = state.getValue(ROTATION);
		int next = (current + 1) & 15;
		BlockState rotated = state.setValue(ROTATION, next);

		// обновляем блок
		world.setBlockAndUpdate(pos, rotated);

		// проигрываем звук — используем getRandom() вместо Create.RANDOM
		float pitch = world.getRandom().nextFloat() + 0.5f;
		AllSoundEvents.WRENCH_ROTATE.playOnServer(world, pos, 1f, pitch);

		return InteractionResult.SUCCESS;
	}

	@Override
	public InteractionResult onSneakWrenched(BlockState state, UseOnContext context) {
		Level world = context.getLevel();
		BlockPos pos = context.getClickedPos();
		Player player = context.getPlayer();

		if (!(world instanceof ServerLevel serverLevel))
			return InteractionResult.SUCCESS;

        int part = state.getValue(PART);
        BlockPos base = pos.below(part);
        BlockState baseState = world.getBlockState(base);
        
		BlockEvent.BreakEvent event = new BlockEvent.BreakEvent(world, base, baseState, player);
		MinecraftForge.EVENT_BUS.post(event);
		if (event.isCanceled())
			return InteractionResult.SUCCESS;

		if (player != null && !player.isCreative()) {
			Block.getDrops(baseState, serverLevel, base, world.getBlockEntity(base), player, context.getItemInHand())
				.forEach(itemStack -> {
					player.getInventory()
						.placeItemBackInInventory(itemStack);
				});
		}

        for (int i = 0; i < 4; i++) {
            BlockPos target = base.above(i);
            BlockState s = world.getBlockState(target);
            if (s.getBlock() instanceof ModernSignalBlock)
                    world.destroyBlock(target, false);
        }		
		
		//? if >=1.19.2
		/*state.spawnAfterBreak(serverLevel, pos, ItemStack.EMPTY, true);*/
		//? if <1.19.2
		state.spawnAfterBreak(serverLevel, pos, ItemStack.EMPTY);
		playRemoveSound(world, pos);
		return InteractionResult.SUCCESS;
	}
	
	@Override
	public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
		if (!level.isClientSide) {
			int part = state.getValue(PART);
			BlockPos base = pos.below(part);
			for (int i = 0; i < 4; i++) {
				BlockPos target = base.above(i);
				BlockState s = level.getBlockState(target);
				if (s.getBlock() instanceof ModernSignalBlock)
					level.destroyBlock(target, false);
			}
			if (!player.isCreative())
				popResource(level, base, new ItemStack(this));
		}
		super.playerWillDestroy(level, pos, state, player);
	}

	@Override
	public Class<ModernSignalBlockEntity> getBlockEntityClass() {
		return ModernSignalBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends ModernSignalBlockEntity> getBlockEntityType() {
		return AllBlockEntities.MODERN_SIGNAL.get();
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		if (state.getValue(PART) == 0)
			return IBE.super.newBlockEntity(pos, state);
		return null;
	}

	@Override
	@SuppressWarnings("deprecation")
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
		if (state.getBlock() != newState.getBlock() && !level.isClientSide && !REMOVING_CLUSTER.get()) {
			REMOVING_CLUSTER.set(true);
			try {
				int part = state.getValue(PART);
				BlockPos base = pos.below(part);
				for (int i = 0; i < 4; i++) {
					BlockPos target = base.above(i);
					if (target.equals(pos))
						continue;
					BlockState targetState = level.getBlockState(target);
					if (targetState.getBlock() instanceof ModernSignalBlock) {
						level.setBlock(target, Blocks.AIR.defaultBlockState(), 35);
					}
				}
			} finally {
				REMOVING_CLUSTER.set(false);
			}
		}
		super.onRemove(state, level, pos, newState, isMoving);
	}
}
