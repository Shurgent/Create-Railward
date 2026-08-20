package ua.pp.shurgent.railward.block.train.parts;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.MaterialColor;

/** A train wall panel whose windshield frame can be changed with a wrench. */
public class WindshieldBlock extends TrainWallPanelBlock {

	public static final IntegerProperty FRAME = IntegerProperty.create("frame", 0, 3);

	public WindshieldBlock(BlockBehaviour.Properties properties, MaterialColor color) {
		super(properties, color);
		registerDefaultState(defaultBlockState().setValue(FRAME, 0));
	}

	@Override
	protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder.add(FRAME));
	}

	@Override
	public InteractionResult onWrenched(BlockState state, UseOnContext context) {
		InteractionResult panelResult = super.onWrenched(state, context);
		if (panelResult.consumesAction())
			return panelResult;

		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();
		if (level.getBlockEntity(pos) instanceof TrainWallPanelBlockEntity panel
				&& isSecondaryPanelHit(state, pos, context.getClickLocation(), panel))
			return InteractionResult.FAIL;

		if (level.isClientSide)
			return InteractionResult.SUCCESS;

		level.setBlock(pos, cycleFrame(state), 3);
		playRotateSound(level, pos);
		return InteractionResult.SUCCESS;
	}

	public static BlockState cycleFrame(BlockState state) {
		return state.setValue(FRAME, (state.getValue(FRAME) + 1) % 4);
	}
}
