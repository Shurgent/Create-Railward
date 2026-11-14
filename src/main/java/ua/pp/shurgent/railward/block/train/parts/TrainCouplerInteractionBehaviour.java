package ua.pp.shurgent.railward.block.train.parts;

import com.simibubi.create.AllItems;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.behaviour.MovingInteractionBehaviour;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import com.simibubi.create.foundation.utility.VecHelper;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import ua.pp.shurgent.railward.RailwardSoundEvents;
import ua.pp.shurgent.railward.compat.RailwardComponents;
import ua.pp.shurgent.railward.compat.TrainCouplerCompatibility;

public class TrainCouplerInteractionBehaviour extends MovingInteractionBehaviour {
	private final MovingInteractionBehaviour fallback;

	public TrainCouplerInteractionBehaviour() {
		this(null);
	}

	public TrainCouplerInteractionBehaviour(MovingInteractionBehaviour fallback) {
		this.fallback = fallback;
	}

	@Override
	public boolean handlePlayerInteraction(Player player, InteractionHand activeHand, BlockPos localPos,
		AbstractContraptionEntity contraptionEntity) {
		ItemStack itemInHand = player.getItemInHand(activeHand);
		var info = contraptionEntity.getContraption().getBlocks().get(localPos);
		if (activeHand == InteractionHand.OFF_HAND || !AllItems.WRENCH.isIn(itemInHand)
			|| !(contraptionEntity instanceof CarriageContraptionEntity carriageEntity)
			|| info == null || !TrainCouplerCompatibility.isCoupler(info.state))
			return fallback != null && fallback.handlePlayerInteraction(player, activeHand, localPos, contraptionEntity);

		if (player.level.isClientSide)
			return true;

		TrainCouplerHandler.Result result = TrainCouplerHandler.tryDecouple(player.level, carriageEntity, localPos);
		if (result.success()) {
			Vec3 soundPos = carriageEntity.toGlobalVector(VecHelper.getCenterOf(localPos), 1);
			player.level.playSound(null, soundPos.x, soundPos.y, soundPos.z,
				RailwardSoundEvents.TRAIN_COUPLER_MANUAL.get(), SoundSource.BLOCKS, 1, 1);
			player.displayClientMessage(RailwardComponents.translatable(result.message()).withStyle(ChatFormatting.GREEN), true);
		} else {
			AllSoundEvents.DENY.playOnServer(player.level, player.blockPosition(), 1, 1);
			player.displayClientMessage(RailwardComponents.translatable(result.message()).withStyle(ChatFormatting.RED), true);
		}
		return true;
	}

	@Override
	public void handleEntityCollision(Entity entity, BlockPos localPos, AbstractContraptionEntity contraptionEntity) {
		if (fallback != null)
			fallback.handleEntityCollision(entity, localPos, contraptionEntity);
	}

}
