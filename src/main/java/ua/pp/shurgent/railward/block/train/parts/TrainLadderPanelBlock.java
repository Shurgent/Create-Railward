package ua.pp.shurgent.railward.block.train.parts;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MaterialColor;

/**
 * A train wall panel whose modeled outer face can also be climbed.
 */
public class TrainLadderPanelBlock extends TrainWallPanelBlock {

	public TrainLadderPanelBlock(BlockBehaviour.Properties properties, MaterialColor color) {
		super(properties, color);
	}

	@Override
	public boolean isLadder(BlockState state, LevelReader level, BlockPos pos, LivingEntity entity) {
		return true;
	}
}
