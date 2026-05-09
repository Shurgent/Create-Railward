package ua.pp.shurgent.railward.compat.wthit;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.ContraptionHandlerClient;
import com.simibubi.create.foundation.utility.Couple;

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

		IItemHandler handler = BulkCargoContraptionHelper.getItemHandler(contraption, data.controllerPos());
		if (handler == null)
			return;

		appendCargo(tooltip, BulkCargoStorage.getDisplayStack(handler), BulkCargoStorage.getStoredAmount(handler));
	}

	private static BlockPos getLookedAtContraptionPos(AbstractContraptionEntity entity) {
		LocalPlayer player = Minecraft.getInstance().player;
		if (player == null)
			return null;

		Couple<Vec3> rayInputs = ContraptionHandlerClient.getRayInputs(player);
		BlockHitResult result = ContraptionHandlerClient.rayTraceContraption(rayInputs.getFirst(), rayInputs.getSecond(),
			entity);
		return result == null ? null : result.getBlockPos();
	}

	private static void appendCargo(ITooltip tooltip, ItemStack displayStack, int amount) {
		tooltip.addLine(new TranslatableComponent("railward.wthit.bulk_cargo.item", displayStack.isEmpty()
			? new TranslatableComponent("railward.wthit.bulk_cargo.empty")
			: displayStack.getHoverName()));
		tooltip.addLine(new TranslatableComponent("railward.wthit.bulk_cargo.amount",
			new TextComponent(Integer.toString(amount))));
	}

}
