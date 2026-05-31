package ua.pp.shurgent.railward.block.train.station;

import java.util.List;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.utility.animation.LerpedFloat;
import com.simibubi.create.foundation.utility.animation.LerpedFloat.Chaser;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class CisternHatchBlockEntity extends SmartBlockEntity {

	private final LerpedFloat lidAnimation;

	public CisternHatchBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
		lidAnimation = LerpedFloat.linear()
			.startWithValue(0);
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
	}

	@Override
	public void tick() {
		super.tick();
		if (level == null || !level.isClientSide)
			return;

		lidAnimation.chase(shouldRenderOpen() ? 1 : 0, .25f, Chaser.LINEAR);
		lidAnimation.tickChaser();
	}

	public float getLidProgress(float partialTicks) {
		return lidAnimation.getValue(partialTicks);
	}

	private boolean shouldRenderOpen() {
		for (int distance = 1; distance <= FluidTerminalBlockEntity.MAX_TRANSFER_DISTANCE; distance++) {
			BlockEntity blockEntity = level.getBlockEntity(worldPosition.above(distance));
			if (blockEntity instanceof FluidTerminalBlockEntity terminal && terminal.isConnectedToHatch())
				return true;
		}
		return false;
	}
}
