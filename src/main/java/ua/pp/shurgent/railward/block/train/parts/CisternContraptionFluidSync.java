package ua.pp.shurgent.railward.block.train.parts;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;

public final class CisternContraptionFluidSync {

	private static final String LAST_TICK_KEY = "RailwardCisternFluidTick";

	private CisternContraptionFluidSync() {
	}

	public static void tick(AbstractContraptionEntity contraptionEntity) {
		if (contraptionEntity.level.isClientSide)
			return;

		long gameTime = contraptionEntity.level.getGameTime();
		if (contraptionEntity.getPersistentData()
			.getLong(LAST_TICK_KEY) == gameTime)
			return;
		contraptionEntity.getPersistentData()
			.putLong(LAST_TICK_KEY, gameTime);

		if (contraptionEntity.tickCount % 20 != 0)
			return;

		Contraption contraption = contraptionEntity.getContraption();
		if (contraption == null)
			return;

		Set<BlockPos> seenControllers = new HashSet<>();
		for (Map.Entry<BlockPos, StructureBlockInfo> entry : contraption.getBlocks()
			.entrySet()) {
			if (!(entry.getValue().state.getBlock() instanceof CisternBlock))
				continue;

			CisternContraptionHelper.ControllerData data =
				CisternContraptionHelper.resolveController(contraption, entry.getKey());
			if (data == null || !seenControllers.add(data.controllerPos()))
				continue;

			CisternContraptionHelper.syncFluid(contraptionEntity, data);
		}
	}

}
