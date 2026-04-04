package ua.pp.shurgent.railward.block.train.signal;

import java.lang.ref.WeakReference;
import java.util.List;

import com.simibubi.create.content.trains.signal.SignalBlockEntity;
import com.simibubi.create.content.trains.signal.SignalBlockEntity.SignalState;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public class ModernSignalBlockEntity extends SmartBlockEntity {

	private WeakReference<SignalBlockEntity> cachedSignalTE;
	public SignalState signalState;

	// раз в SEARCH_INTERVAL тиков будем искать SignalBlockEntity
	private static final int SEARCH_INTERVAL = 10;
	private int tickCounter = 0;

	public ModernSignalBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
		cachedSignalTE = new WeakReference<>(null);
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
	}

	@Override
        public void tick() {
		super.tick();
		if (!level.isClientSide)
			return;

		signalState = null;
		SignalBlockEntity signalBlockEntity = cachedSignalTE.get();

		if (signalBlockEntity == null || signalBlockEntity.isRemoved()) {

			if (++tickCounter % SEARCH_INTERVAL == 0) {
				for (int i = 1; i <= 4; i++) {
					BlockPos checkPos = worldPosition.below(i);
					BlockEntity blockEntity = level.getBlockEntity(checkPos);
					if (blockEntity instanceof SignalBlockEntity signal) {
						signalState = signal.getState();
						cachedSignalTE = new WeakReference<>(signal);
						break;
					}
				}
			}
			return;
		}

                signalState = signalBlockEntity.getState();
        }

        @Override
        public AABB getRenderBoundingBox() {
                // extend the render bounding box to include the whole 4 block structure
                return new AABB(worldPosition).expandTowards(0, 4, 0);
        }
}
