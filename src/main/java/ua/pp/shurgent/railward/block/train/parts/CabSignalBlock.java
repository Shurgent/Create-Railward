package ua.pp.shurgent.railward.block.train.parts;

import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import ua.pp.shurgent.railward.AllBlockEntities;
import ua.pp.shurgent.railward.block.surface.SurfaceMountedBlock;

public class CabSignalBlock extends SurfaceMountedBlock<CabSignalBlockEntity> {

	public static final double MODEL_WIDTH = 2 / 16d;
	public static final double MODEL_HEIGHT = 7 / 16d;
	public static final double MODEL_DEPTH = 2 / 16d;

	public CabSignalBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected double getModelWidth() {
		return MODEL_WIDTH;
	}

	@Override
	protected double getModelHeight() {
		return MODEL_HEIGHT;
	}

	@Override
	protected double getModelDepth() {
		return MODEL_DEPTH;
	}

	@Override
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
		IBE.onRemove(state, level, pos, newState);
	}

	@Override
	public Class<CabSignalBlockEntity> getBlockEntityClass() {
		return CabSignalBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends CabSignalBlockEntity> getBlockEntityType() {
		return AllBlockEntities.CAB_SIGNAL.get();
	}
}
