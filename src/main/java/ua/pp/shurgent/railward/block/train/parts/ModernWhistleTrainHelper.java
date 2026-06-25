package ua.pp.shurgent.railward.block.train.parts;

import javax.annotation.Nullable;

import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.CarriageContraption;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import com.simibubi.create.content.trains.entity.Train;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;

public class ModernWhistleTrainHelper {

	private ModernWhistleTrainHelper() {
	}

	@Nullable
	public static ModernWhistleSoundType resolveTrainLeadSoundType(CarriageContraptionEntity carriageEntity) {
		Carriage carriage = carriageEntity.getCarriage();
		if (carriage == null || carriage.train == null)
			return null;

		Train train = carriage.train;
		for (Carriage currentCarriage : train.carriages) {
			CarriageContraptionEntity currentEntity = currentCarriage.anyAvailableEntity();
			if (currentEntity == null)
				continue;

			WhistleCandidate candidate = findFirstWhistleCandidate(currentEntity);
			if (candidate == null)
				continue;

			return resolveSoundType(candidate.blockInfo);
		}

		return null;
	}

	@Nullable
	public static ModernWhistleSoundType resolveSoundType(StructureBlockInfo blockInfo) {
		if (blockInfo == null)
			return null;
		if (!(blockInfo.state.getBlock() instanceof ModernWhistleBlock))
			return null;
		return blockInfo.state.getValue(ModernWhistleBlock.SOUND_TYPE);
	}

	@Nullable
	private static WhistleCandidate findFirstWhistleCandidate(CarriageContraptionEntity carriageEntity) {
		Contraption contraption = carriageEntity.getContraption();
		if (!(contraption instanceof CarriageContraption carriageContraption))
			return null;

		Direction assemblyDirection = carriageContraption.getAssemblyDirection();
		Direction.Axis axis = assemblyDirection.getAxis();
		int directionMultiplier = assemblyDirection.getAxisDirection()
			.getStep();

		WhistleCandidate first = null;
		for (StructureBlockInfo blockInfo : contraption.getBlocks()
			.values()) {
			if (!(blockInfo.state.getBlock() instanceof com.simibubi.create.content.decoration.steamWhistle.WhistleBlock))
				continue;

			int score = getAxisCoordinate(blockInfo.pos, axis) * directionMultiplier;
			if (first == null || score < first.score)
				first = new WhistleCandidate(blockInfo, score);
		}

		return first;
	}

	private static int getAxisCoordinate(BlockPos pos, Direction.Axis axis) {
		return switch (axis) {
		case X -> pos.getX();
		case Y -> pos.getY();
		case Z -> pos.getZ();
		};
	}

	private record WhistleCandidate(StructureBlockInfo blockInfo, int score) {
	}
}
