package ua.pp.shurgent.railward.block.train.signal;

import com.simibubi.create.AllSoundEvents;
import ua.pp.shurgent.railward.compat.RailwardWrenchable;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import ua.pp.shurgent.railward.AllBlockEntities;

public class ModernSignalGroundBlock extends Block implements IBE<ModernSignalGroundBlockEntity>, RailwardWrenchable {

	public static final IntegerProperty ROTATION = BlockStateProperties.ROTATION_16;

	public ModernSignalGroundBlock(Properties props) {
		super(BlockBehaviour.Properties.of(Material.METAL).strength(1.0f, 6f).noOcclusion().sound(SoundType.METAL));
		registerDefaultState(this.defaultBlockState().setValue(ROTATION, 0));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(ROTATION);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext ctx) {
		int rot = Mth.floor(((ctx.getRotation()) * 16.0F / 360.0F) + 0.5D) & 15;
		return defaultBlockState().setValue(ROTATION, rot);
	}

	@Override
	public RenderShape getRenderShape(BlockState state) {
	  return RenderShape.ENTITYBLOCK_ANIMATED;
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
		return Block.box(4, 0, 4, 12, 14, 12);
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
	public Class<ModernSignalGroundBlockEntity> getBlockEntityClass() {
		return ModernSignalGroundBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends ModernSignalGroundBlockEntity> getBlockEntityType() {
		return AllBlockEntities.MODERN_SIGNAL_GROUND.get();
	}
}
