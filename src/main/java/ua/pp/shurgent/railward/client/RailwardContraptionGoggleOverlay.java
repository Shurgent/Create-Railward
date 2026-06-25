package ua.pp.shurgent.railward.client;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllItems;
import com.simibubi.create.compat.Mods;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.ContraptionHandler;
import com.simibubi.create.content.contraptions.ContraptionHandlerClient;
import com.simibubi.create.content.equipment.goggles.GogglesItem;
import com.simibubi.create.content.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.gui.RemovedGuiUtils;
import com.simibubi.create.foundation.gui.Theme;
import com.simibubi.create.foundation.gui.element.GuiGameElement;
import com.simibubi.create.foundation.mixin.accessor.MouseHandlerAccessor;
import com.simibubi.create.foundation.utility.Color;
import com.simibubi.create.foundation.utility.Couple;
import com.simibubi.create.infrastructure.config.AllConfigs;
import com.simibubi.create.infrastructure.config.CClient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.gui.ForgeIngameGui;
import net.minecraftforge.client.gui.IIngameOverlay;
import ua.pp.shurgent.railward.block.train.parts.BulkCargoContainerBlockEntity;
import ua.pp.shurgent.railward.block.train.parts.BulkCargoContraptionHelper;
import ua.pp.shurgent.railward.block.train.parts.CisternBlockEntity;
import ua.pp.shurgent.railward.block.train.parts.CisternContraptionHelper;

public final class RailwardContraptionGoggleOverlay {

	public static final IIngameOverlay OVERLAY = RailwardContraptionGoggleOverlay::renderOverlay;

	private static int hoverTicks;
	private static TargetKey lastTarget;

	private RailwardContraptionGoggleOverlay() {
	}

	public static void renderOverlay(ForgeIngameGui gui, PoseStack poseStack, float partialTicks, int width,
		int height) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.options.hideGui || minecraft.gameMode == null
			|| minecraft.gameMode.getPlayerMode() == GameType.SPECTATOR)
			return;
		if (minecraft.player == null || minecraft.level == null || !GogglesItem.isWearingGoggles(minecraft.player)) {
			clearHover();
			return;
		}

		TooltipTarget target = findTarget(minecraft, partialTicks);
		if (target == null) {
			clearHover();
			return;
		}

		List<Component> tooltip = new ArrayList<>();
		boolean isShifting = minecraft.player.isShiftKeyDown();
		if (!target.goggleInformation()
			.addToGoggleTooltip(tooltip, isShifting) || tooltip.isEmpty()) {
			clearHover();
			return;
		}

		TargetKey key = new TargetKey(target.entityId(), target.localPos());
		hoverTicks = key.equals(lastTarget) ? hoverTicks + 1 : 1;
		lastTarget = key;
		renderTooltip(poseStack, tooltip, target.goggleInformation()
			.getIcon(isShifting), partialTicks, width, height);
	}

	private static TooltipTarget findTarget(Minecraft minecraft, float partialTicks) {
		ClientLevel level = minecraft.level;
		LocalPlayer player = minecraft.player;
		if (level == null || player == null)
			return null;

		Couple<Vec3> rayInputs = ContraptionHandlerClient.getRayInputs(player);
		Vec3 origin = rayInputs.getFirst();
		Vec3 target = rayInputs.getSecond();
		AABB rayBounds = new AABB(origin, target).inflate(16);

		TooltipTarget closest = null;
		double closestDistance = Double.POSITIVE_INFINITY;
		Collection<WeakReference<AbstractContraptionEntity>> contraptions = ContraptionHandler.loadedContraptions
			.get(level)
			.values();
		for (WeakReference<AbstractContraptionEntity> reference : contraptions) {
			AbstractContraptionEntity entity = reference.get();
			if (entity == null || !entity.isAlive() || !entity.getBoundingBox()
				.intersects(rayBounds))
				continue;

			BlockHitResult hit = ContraptionHandlerClient.rayTraceContraption(origin, target, entity);
			if (hit == null)
				continue;

			double distance = entity.toGlobalVector(hit.getLocation(), partialTicks)
				.distanceToSqr(origin);
			if (distance >= closestDistance)
				continue;

			TooltipTarget candidate = resolveTooltipTarget(entity, hit.getBlockPos());
			if (candidate == null)
				continue;

			closest = candidate;
			closestDistance = distance;
		}

		return closest;
	}

	private static TooltipTarget resolveTooltipTarget(AbstractContraptionEntity entity, BlockPos localPos) {
		Contraption contraption = entity.getContraption();
		if (contraption == null)
			return null;

		BlockEntity blockEntity = contraption.presentBlockEntities.get(localPos);
		if (blockEntity instanceof BulkCargoContainerBlockEntity bulk)
			return resolveBulkTarget(entity, contraption, localPos, bulk);
		if (blockEntity instanceof CisternBlockEntity)
			return resolveCisternTarget(entity, contraption, localPos);
		if (blockEntity instanceof IHaveGoggleInformation goggleInformation)
			return new TooltipTarget(entity.getId(), localPos, goggleInformation);
		return null;
	}

	private static TooltipTarget resolveBulkTarget(AbstractContraptionEntity entity, Contraption contraption,
		BlockPos localPos, BulkCargoContainerBlockEntity fallback) {
		BulkCargoContraptionHelper.ControllerData data =
			BulkCargoContraptionHelper.resolveController(contraption, localPos);
		if (data == null)
			return new TooltipTarget(entity.getId(), localPos, fallback);

		BulkCargoContainerBlockEntity source = getBulkClientView(contraption, data.controllerPos());
		if (source == null && contraption.presentBlockEntities
			.get(data.controllerPos()) instanceof BulkCargoContainerBlockEntity controller)
			source = controller;
		if (source == null)
			source = fallback;
		return new TooltipTarget(entity.getId(), data.controllerPos(), source);
	}

	private static BulkCargoContainerBlockEntity getBulkClientView(Contraption contraption, BlockPos controllerPos) {
		for (BlockEntity blockEntity : contraption.presentBlockEntities.values())
			if (blockEntity instanceof BulkCargoContainerBlockEntity container
				&& container.isClientCargoViewFor(controllerPos))
				return container;
		return null;
	}

	private static TooltipTarget resolveCisternTarget(AbstractContraptionEntity entity, Contraption contraption,
		BlockPos localPos) {
		CisternContraptionHelper.ControllerData data = CisternContraptionHelper.resolveController(contraption, localPos);
		BlockPos sourcePos = data == null ? localPos : data.controllerPos();
		BlockEntity source = contraption.presentBlockEntities.get(sourcePos);
		if (source instanceof IHaveGoggleInformation goggleInformation)
			return new TooltipTarget(entity.getId(), sourcePos, goggleInformation);
		BlockEntity fallback = contraption.presentBlockEntities.get(localPos);
		return fallback instanceof IHaveGoggleInformation goggleInformation
			? new TooltipTarget(entity.getId(), localPos, goggleInformation) : null;
	}

	private static void renderTooltip(PoseStack poseStack, List<Component> tooltip, ItemStack item, float partialTicks,
		int width, int height) {
		Minecraft minecraft = Minecraft.getInstance();
		poseStack.pushPose();

		int tooltipTextWidth = 0;
		for (FormattedText textLine : tooltip)
			tooltipTextWidth = Math.max(tooltipTextWidth, minecraft.font.width(textLine));

		int tooltipHeight = 8;
		if (tooltip.size() > 1) {
			tooltipHeight += 2;
			tooltipHeight += (tooltip.size() - 1) * 10;
		}

		CClient config = AllConfigs.client();
		int posX = width / 2 + config.overlayOffsetX.get();
		int posY = height / 2 + config.overlayOffsetY.get();

		posX = Math.min(posX, width - tooltipTextWidth - 20);
		posY = Math.min(posY, height - tooltipHeight - 20);

		float fade = Mth.clamp((hoverTicks + partialTicks) / 24f, 0, 1);
		Boolean useCustom = config.overlayCustomColor.get();
		Color colorBackground = useCustom ? new Color(config.overlayBackgroundColor.get())
			: Theme.c(Theme.Key.VANILLA_TOOLTIP_BACKGROUND)
				.scaleAlpha(.75f);
		Color colorBorderTop = useCustom ? new Color(config.overlayBorderColorTop.get())
			: Theme.c(Theme.Key.VANILLA_TOOLTIP_BORDER, true)
				.copy();
		Color colorBorderBot = useCustom ? new Color(config.overlayBorderColorBot.get())
			: Theme.c(Theme.Key.VANILLA_TOOLTIP_BORDER, false)
				.copy();

		if (fade < 1) {
			poseStack.translate(Math.pow(1 - fade, 3) * Math.signum(config.overlayOffsetX.get() + .5f) * 8, 0, 0);
			colorBackground.scaleAlpha(fade);
			colorBorderTop.scaleAlpha(fade);
			colorBorderBot.scaleAlpha(fade);
		}

		GuiGameElement.of(item.isEmpty() ? AllItems.GOGGLES.asStack() : item)
			.at(posX + 10, posY - 16, 450)
			.render(poseStack);

		if (!Mods.MODERNUI.isLoaded()) {
			RemovedGuiUtils.drawHoveringText(poseStack, tooltip, posX, posY, width, height, -1,
				colorBackground.getRGB(), colorBorderTop.getRGB(), colorBorderBot.getRGB(), minecraft.font);
			poseStack.popPose();
			return;
		}

		MouseHandler mouseHandler = minecraft.mouseHandler;
		Window window = minecraft.getWindow();
		double guiScale = window.getGuiScale();
		double cursorX = mouseHandler.xpos();
		double cursorY = mouseHandler.ypos();
		((MouseHandlerAccessor) mouseHandler).create$setXPos(Math.round(cursorX / guiScale) * guiScale);
		((MouseHandlerAccessor) mouseHandler).create$setYPos(Math.round(cursorY / guiScale) * guiScale);

		RemovedGuiUtils.drawHoveringText(poseStack, tooltip, posX, posY, width, height, -1,
			colorBackground.getRGB(), colorBorderTop.getRGB(), colorBorderBot.getRGB(), minecraft.font);

		((MouseHandlerAccessor) mouseHandler).create$setXPos(cursorX);
		((MouseHandlerAccessor) mouseHandler).create$setYPos(cursorY);

		poseStack.popPose();
	}

	private static void clearHover() {
		hoverTicks = 0;
		lastTarget = null;
	}

	private record TooltipTarget(int entityId, BlockPos localPos, IHaveGoggleInformation goggleInformation) {
	}

	private record TargetKey(int entityId, BlockPos localPos) {
	}

}
