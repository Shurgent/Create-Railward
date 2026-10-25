package ua.pp.shurgent.railward.mixin.create.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.actors.trainControls.ControlsBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import ua.pp.shurgent.railward.block.train.parts.ModernTrainControllerBlock;

@Mixin(value = AbstractContraptionEntity.class, remap = false)
public class AbstractContraptionEntityClientMixin {

	@Inject(method = "handleBlockChange", at = @At("HEAD"), cancellable = true, remap = false)
	private void railward$updateControllerStateWithoutRebuildingContraption(BlockPos localPos, BlockState newState,
		CallbackInfo ci) {
		AbstractContraptionEntity entity = (AbstractContraptionEntity) (Object) this;
		Contraption contraption = entity.getContraption();
		if (contraption == null)
			return;

		StructureBlockInfo oldInfo = contraption.getBlocks().get(localPos);
		if (oldInfo == null || !(oldInfo.state.getBlock() instanceof ModernTrainControllerBlock)
			|| !(newState.getBlock() instanceof ModernTrainControllerBlock))
			return;

		BlockState oldState = oldInfo.state;
		if (oldState.getValue(ControlsBlock.OPEN) != newState.getValue(ControlsBlock.OPEN)
			|| ModernTrainControllerBlock.getPhysicalFacing(oldState)
				!= ModernTrainControllerBlock.getPhysicalFacing(newState))
			return;

		contraption.getBlocks().put(localPos, new StructureBlockInfo(localPos, newState, oldInfo.nbt));
		ci.cancel();
	}
}
