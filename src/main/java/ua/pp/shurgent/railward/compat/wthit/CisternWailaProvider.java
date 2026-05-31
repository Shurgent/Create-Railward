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
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidTank;
import net.minecraftforge.fluids.capability.IFluidHandler;
import ua.pp.shurgent.railward.block.train.parts.CisternBlockEntity;
import ua.pp.shurgent.railward.block.train.parts.CisternContraptionHelper;

public class CisternWailaProvider implements IBlockComponentProvider, IEntityComponentProvider {

	@Override
	public void appendBody(ITooltip tooltip, IBlockAccessor accessor, IPluginConfig config) {
		CisternBlockEntity cistern = accessor.getBlockEntity();
		if (cistern == null)
			return;

		CisternBlockEntity controller = cistern.getControllerBE();
		if (controller == null)
			controller = cistern;

		IFluidTank tank = controller.getTankInventory();
		appendFluid(tooltip, tank.getFluid(), tank.getCapacity());
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

		CisternContraptionHelper.ControllerData data = CisternContraptionHelper.resolveController(contraption, localPos);
		if (data == null)
			return;

		IFluidHandler handler = CisternContraptionHelper.getFluidHandler(contraption, data.controllerPos());
		if (handler == null || handler.getTanks() <= 0)
			return;

		appendFluid(tooltip, handler.getFluidInTank(0), handler.getTankCapacity(0));
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

	private static void appendFluid(ITooltip tooltip, FluidStack fluid, int capacity) {
		Component fluidName = fluid.isEmpty()
			? new TranslatableComponent("railward.wthit.cistern.empty")
			: fluid.getDisplayName();
		tooltip.addLine(new TranslatableComponent("railward.wthit.cistern.fluid", fluidName));
		tooltip.addLine(new TranslatableComponent("railward.wthit.cistern.amount",
			new TextComponent(Integer.toString(fluid.getAmount())), new TextComponent(Integer.toString(capacity))));
	}

}
