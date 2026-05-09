package ua.pp.shurgent.railward.block.train.parts;

import com.simibubi.create.content.equipment.wrench.IWrenchable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.MaterialColor;

public class TrainRoofBlock extends Block implements IWrenchable {

	public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;

	public TrainRoofBlock(BlockBehaviour.Properties properties) {
		super(properties.strength(1.0f, 6f).sound(SoundType.METAL).color(MaterialColor.COLOR_LIGHT_BLUE));
		registerDefaultState(defaultBlockState().setValue(AXIS, Direction.Axis.X));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(AXIS);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(AXIS, getPlacementAxis(context));
	}

	@Override
	public InteractionResult onWrenched(BlockState state, UseOnContext context) {
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();

		if (level.isClientSide)
			return InteractionResult.SUCCESS;

		level.setBlock(pos, state.setValue(AXIS, nextAxis(state.getValue(AXIS))), 3);
		playRotateSound(level, pos);
		return InteractionResult.SUCCESS;
	}

	static Direction.Axis getPlacementAxis(BlockPlaceContext context) {
		LevelAccessor level = context.getLevel();
		BlockPos pos = context.getClickedPos();

		for (Direction direction : Direction.Plane.HORIZONTAL) {
			BlockState neighbor = level.getBlockState(pos.relative(direction));
			if (hasRoofAxis(neighbor))
				return neighbor.getValue(AXIS);
		}

		for (Direction direction : Direction.values()) {
			BlockState neighbor = level.getBlockState(pos.relative(direction));
			if (hasRoofAxis(neighbor))
				return neighbor.getValue(AXIS);
		}

		return context.getHorizontalDirection().getAxis();
	}

	static Direction.Axis nextAxis(Direction.Axis axis) {
		return axis == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X;
	}

	private static boolean hasRoofAxis(BlockState state) {
		return state.hasProperty(AXIS)
			&& (state.getBlock() instanceof TrainRoofBlock || state.getBlock() instanceof TrainRoofSlopeBlock);
	}
}
