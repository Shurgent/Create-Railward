package ua.pp.shurgent.railward.block.train.parts;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.behaviour.MovingInteractionBehaviour;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;

public class BulkCargoContainerInteractionBehaviour extends MovingInteractionBehaviour {

	@Override
	public boolean handlePlayerInteraction(Player player, InteractionHand activeHand, BlockPos localPos,
		AbstractContraptionEntity contraptionEntity) {
		if (activeHand == InteractionHand.OFF_HAND || !player.getItemInHand(activeHand)
			.isEmpty())
			return false;
		if (player.level.isClientSide)
			return true;

		BulkCargoContraptionHelper.ControllerData data =
			BulkCargoContraptionHelper.resolveController(contraptionEntity.getContraption(), localPos);
		if (data == null)
			return false;
		return BulkCargoContraptionHelper.tryExtractOneStackToPlayer(contraptionEntity, data, player);
	}

	@Override
	public void handleEntityCollision(Entity entity, BlockPos localPos, AbstractContraptionEntity contraptionEntity) {
		if (entity.level.isClientSide || !(entity instanceof ItemEntity itemEntity) || !itemEntity.isAlive())
			return;

		BulkCargoContraptionHelper.ControllerData data =
			BulkCargoContraptionHelper.resolveController(contraptionEntity.getContraption(), localPos);
		if (data == null)
			return;
		BulkCargoContraptionHelper.tryInsertItemEntity(contraptionEntity, data, itemEntity);
	}

}
