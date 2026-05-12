package ua.pp.shurgent.railward.compat.wthit;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.ContraptionHandlerClient;

import mcp.mobius.waila.api.IBlockAccessor;
import mcp.mobius.waila.api.IBlockComponentProvider;
import mcp.mobius.waila.api.IEntityAccessor;
import mcp.mobius.waila.api.IEntityComponentProvider;
import mcp.mobius.waila.api.IPluginConfig;
import mcp.mobius.waila.api.ITooltip;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.items.IItemHandler;
import ua.pp.shurgent.railward.block.train.parts.BulkCargoContainerBlockEntity;
import ua.pp.shurgent.railward.block.train.parts.BulkCargoContraptionHelper;
import ua.pp.shurgent.railward.block.train.parts.BulkCargoStorage;

public class BulkCargoContainerWailaProvider implements IBlockComponentProvider, IEntityComponentProvider {

	@Override
	public void appendBody(ITooltip tooltip, IBlockAccessor accessor, IPluginConfig config) {
		BulkCargoContainerBlockEntity container = accessor.getBlockEntity();
		if (container == null)
			return;

		BulkCargoContainerBlockEntity controller = container.getControllerBE();
		if (controller == null)
			controller = container;

		appendCargo(tooltip, controller.getDisplayStack(), controller.getStoredAmount());
	}

	@Override
	public void appendBody(ITooltip tooltip, IEntityAccessor accessor, IPluginConfig config) {
		AbstractContraptionEntity entity = accessor.getEntity();
		Contraption contraption = entity.getContraption();
		if (contraption == null)
			return;

		BlockPos localPos = getLookedAtContraptionPos(entity);
		if (localPos == null)
			return;

		BulkCargoContraptionHelper.ControllerData data =
			BulkCargoContraptionHelper.resolveController(contraption, localPos);
		if (data == null)
			return;

		BulkCargoContainerBlockEntity clientContainer = getClientCargoView(contraption, data.controllerPos());
		if (clientContainer != null) {
			appendCargo(tooltip, clientContainer.getDisplayStack(), clientContainer.getStoredAmount());
			return;
		}

		IItemHandler handler = BulkCargoContraptionHelper.getItemHandler(contraption, data.controllerPos());
		if (handler == null)
			return;

		appendCargo(tooltip, BulkCargoStorage.getDisplayStack(handler), BulkCargoStorage.getStoredAmount(handler));
	}

	private static BlockPos getLookedAtContraptionPos(AbstractContraptionEntity entity) {
		Minecraft minecraft = Minecraft.getInstance();
		LocalPlayer player = minecraft.player;
		if (player == null || minecraft.gameMode == null)
			return null;

		float partialTicks = minecraft.getFrameTime();
		Vec3 start = player.getEyePosition(partialTicks);
		Vec3 end = start.add(player.getViewVector(partialTicks)
			.scale(minecraft.gameMode.getPickRange()));
		BlockHitResult result = ContraptionHandlerClient.rayTraceContraption(start, end, entity);
		return result == null ? null : result.getBlockPos();
	}

	private static BulkCargoContainerBlockEntity getClientCargoView(Contraption contraption, BlockPos controllerPos) {
		BlockEntity controllerBlockEntity = contraption.presentBlockEntities.get(controllerPos);
		if (controllerBlockEntity instanceof BulkCargoContainerBlockEntity container && container.hasClientCargoView())
			return container;

		for (BlockEntity blockEntity : contraption.presentBlockEntities.values())
			if (blockEntity instanceof BulkCargoContainerBlockEntity container && container.hasClientCargoView()
				&& controllerPos.equals(container.getController()))
				return container;

		return null;
	}

	private static void appendCargo(ITooltip tooltip, ItemStack displayStack, int amount) {
		tooltip.addLine(new TranslatableComponent("railward.wthit.bulk_cargo.item", displayStack.isEmpty()
			? new TranslatableComponent("railward.wthit.bulk_cargo.empty")
			: displayStack.getHoverName()));
		tooltip.addLine(new TranslatableComponent("railward.wthit.bulk_cargo.amount",
			new TextComponent(Integer.toString(amount))));
	}

}
