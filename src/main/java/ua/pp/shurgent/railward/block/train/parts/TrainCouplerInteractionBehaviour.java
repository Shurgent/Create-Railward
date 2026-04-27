package ua.pp.shurgent.railward.block.train.parts;

import com.simibubi.create.AllItems;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.behaviour.MovingInteractionBehaviour;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class TrainCouplerInteractionBehaviour extends MovingInteractionBehaviour {

	@Override
	public boolean handlePlayerInteraction(Player player, InteractionHand activeHand, BlockPos localPos,
		AbstractContraptionEntity contraptionEntity) {
		if (activeHand == InteractionHand.OFF_HAND)
			return false;

		ItemStack itemInHand = player.getItemInHand(activeHand);
		if (!AllItems.WRENCH.isIn(itemInHand))
			return false;

		if (!(contraptionEntity instanceof CarriageContraptionEntity carriageEntity))
			return false;

		if (player.level.isClientSide)
			return true;

		TrainCouplerHandler.Result result = TrainCouplerHandler.tryDecouple(player.level, carriageEntity, localPos);
		if (result.success()) {
			AllSoundEvents.CONFIRM.playOnServer(player.level, player.blockPosition(), 1, 1);
			player.displayClientMessage(new TranslatableComponent(result.message()).withStyle(ChatFormatting.GREEN), true);
		} else {
			AllSoundEvents.DENY.playOnServer(player.level, player.blockPosition(), 1, 1);
			player.displayClientMessage(new TranslatableComponent(result.message()).withStyle(ChatFormatting.RED), true);
		}
		return true;
	}

}
