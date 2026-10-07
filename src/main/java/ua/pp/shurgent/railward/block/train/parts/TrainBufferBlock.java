package ua.pp.shurgent.railward.block.train.parts;

import ua.pp.shurgent.railward.compat.RailwardWrenchable;
import com.simibubi.create.foundation.utility.VoxelShaper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.material.MaterialColor;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class TrainBufferBlock extends HorizontalDirectionalBlock implements RailwardWrenchable {
	public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

	private static final VoxelShape SHAPE_NORTH = Shapes.or(
		Block.box(5, 7, 0, 11, 13, 3), // base
		Block.box(6, 8, 3, 10, 12, 11), // rod
		Block.box(3, 6, 11, 13, 14, 12), // cap
		Block.box(0, 0, 0, 16, 16, 1) // rear full size hitbox for easier selection
	);

	// Build a directional shaper so you don’t manually rotate each AABB
	private static final VoxelShaper SHAPE = VoxelShaper.forDirectional(SHAPE_NORTH, Direction.NORTH);

	public TrainBufferBlock(Properties props) {
        super(BlockBehaviour.Properties.of(Material.METAL).strength(1.0f, 6f).sound(SoundType.METAL).color(MaterialColor.COLOR_BROWN).noOcclusion());
		registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH));
	}

	@Override
	public InteractionResult onWrenched(BlockState state, UseOnContext context) {
		return InteractionResult.FAIL;
	}
	
	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
		b.add(FACING);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext ctx) {
		return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection());
	}

	@Override
	public RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter w, BlockPos pos, CollisionContext ctx) {
		return SHAPE.get(state.getValue(FACING));
	}

}
