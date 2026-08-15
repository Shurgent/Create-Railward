package ua.pp.shurgent.railward.block.train.parts;

import com.simibubi.create.content.contraptions.AssemblyException;
import com.simibubi.create.content.trains.entity.CarriageContraption;
import com.simibubi.create.foundation.utility.Couple;
import com.simibubi.create.foundation.utility.Iterate;
import com.simibubi.create.foundation.utility.Lang;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import ua.pp.shurgent.railward.mixin.create.CarriageContraptionAccessor;

public final class ModernTrainControllerAssembly {

	private ModernTrainControllerAssembly() {
	}

	public static void apply(CarriageContraption contraption) throws AssemblyException {
		boolean found = false;
		Direction assemblyDirection = contraption.getAssemblyDirection();

		for (StructureBlockInfo info : contraption.getBlocks().values()) {
			if (!(info.state.getBlock() instanceof ModernTrainControllerBlock))
				continue;
			Direction physicalFacing = ModernTrainControllerBlock.getPhysicalFacing(info.state);
			if (physicalFacing.getAxis() != assemblyDirection.getAxis())
				throw new AssemblyException(Lang.translateDirect("train_assembly.sideways_controls"));
			found = true;
		}

		if (!found)
			return;

		CarriageContraptionAccessor accessor = (CarriageContraptionAccessor) contraption;
		accessor.railward$setForwardControls(true);
		accessor.railward$setBackwardControls(true);

		for (BlockPos seatPos : contraption.getSeats()) {
			if (!hasControllerFacingSeat(contraption, seatPos))
				continue;
			Couple<Boolean> sides = contraption.conductorSeats.computeIfAbsent(seatPos,
				ignored -> Couple.create(false, false));
			sides.setFirst(true);
			sides.setSecond(true);
		}
	}

	public static boolean hasControllerFacingSeat(CarriageContraption contraption, BlockPos seatPos) {
		for (Direction direction : Iterate.directionsInAxis(contraption.getAssemblyDirection().getAxis())) {
			BlockPos controlsPos = seatPos.relative(direction);
			StructureBlockInfo info = contraption.getBlocks().get(controlsPos);
			if (info == null || !(info.state.getBlock() instanceof ModernTrainControllerBlock))
				continue;
			BlockState state = info.state;
			if (ModernTrainControllerBlock.getPhysicalFacing(state) == direction.getOpposite())
				return true;
		}
		return false;
	}
}
