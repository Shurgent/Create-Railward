package ua.pp.shurgent.railward.block.train.parts;

import com.simibubi.create.AllShapes;
import com.simibubi.create.content.contraptions.actors.trainControls.ControlsBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class ModernTrainControllerBlock extends ControlsBlock {

	public static final BooleanProperty REVERSED = BooleanProperty.create("reversed");
	public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
	public static final BooleanProperty BRAKING = BooleanProperty.create("braking");
	public static final BooleanProperty THROTTLE = BooleanProperty.create("throttle");

	public ModernTrainControllerBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState()
			.setValue(REVERSED, false)
			.setValue(ACTIVE, false)
			.setValue(BRAKING, false)
			.setValue(THROTTLE, false));
	}

	@Override
	protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(REVERSED, ACTIVE, BRAKING, THROTTLE);
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return AllShapes.CONTROLS.get(getPhysicalFacing(state));
	}

	/**
	 * FACING is the direction selected by the reverser so Create can keep using its
	 * native manual-control calculations. REVERSED preserves the physical model and
	 * hitbox orientation while FACING is flipped.
	 */
	public static Direction getPhysicalFacing(BlockState state) {
		Direction selected = state.getValue(FACING);
		return state.getValue(REVERSED) ? selected.getOpposite() : selected;
	}
}
