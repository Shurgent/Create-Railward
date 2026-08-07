package ua.pp.shurgent.railward.block.train.parts;

import javax.annotation.Nullable;

import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.behaviour.MovementBehaviour;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;

import net.minecraft.world.item.ItemStack;
import ua.pp.shurgent.railward.AllBlocks;
import ua.pp.shurgent.railward.block.train.parts.panel.LocomotiveSpotlightAttachment;
import ua.pp.shurgent.railward.block.train.parts.panel.TrainPanelAttachment;

public class LocomotiveSpotlightMovementBehaviour implements MovementBehaviour {

	@Override
	@Nullable
	public ItemStack canBeDisabledVia(MovementContext context) {
		if (AllBlocks.LOCOMOTIVE_SPOTLIGHT.has(context.state))
			return AllBlocks.LOCOMOTIVE_SPOTLIGHT.asStack();

		TrainPanelAttachment attachment = TrainWallPanelBlockEntity.readAttachment(context.blockEntityData);
		return attachment instanceof LocomotiveSpotlightAttachment ? AllBlocks.LOCOMOTIVE_SPOTLIGHT.asStack() : null;
	}

	@Override
	public boolean renderAsNormalBlockEntity() {
		return true;
	}

	public static boolean isDisabled(Contraption contraption) {
		return contraption.getDisabledActors()
			.stream()
			.anyMatch(filter -> filter.isEmpty() || AllBlocks.LOCOMOTIVE_SPOTLIGHT.isIn(filter));
	}
}
