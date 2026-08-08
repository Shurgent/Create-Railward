package ua.pp.shurgent.railward.block.train.parts;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import ua.pp.shurgent.railward.block.surface.SurfaceMountedBlockEntity;

public class CabSignalBlockEntity extends SurfaceMountedBlockEntity {

	public CabSignalBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}
}
