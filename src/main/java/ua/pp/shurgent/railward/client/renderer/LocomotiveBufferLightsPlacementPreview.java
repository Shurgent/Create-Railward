package ua.pp.shurgent.railward.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.AllItems;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.client.event.DrawSelectionEvent;
import ua.pp.shurgent.railward.AllBlocks;
import ua.pp.shurgent.railward.block.train.parts.LocomotiveBufferLightsVariant;
import ua.pp.shurgent.railward.block.train.parts.LocomotiveSpotlightVariant;
import ua.pp.shurgent.railward.block.train.parts.TrainWallPanelBlock;
import ua.pp.shurgent.railward.block.train.parts.TrainWallPanelBlockEntity;
import ua.pp.shurgent.railward.block.train.parts.panel.TrainPanelAttachment;

public class LocomotiveBufferLightsPlacementPreview {

	private LocomotiveBufferLightsPlacementPreview() {
	}

	public static void render(DrawSelectionEvent.HighlightBlock event) {
		Minecraft minecraft = Minecraft.getInstance();
		Player player = minecraft.player;
		Level level = minecraft.level;
		if (player == null || level == null)
			return;

		BlockHitResult target = event.getTarget();
		BlockPos pos = target.getBlockPos();
		BlockState state = level.getBlockState(pos);
		if (!(state.getBlock() instanceof TrainWallPanelBlock))
			return;
		if (!(level.getBlockEntity(pos) instanceof TrainWallPanelBlockEntity panel))
			return;

		if (isHoldingBufferLights(player)) {
			renderBufferLightsGhost(event, state, panel, pos);
			return;
		}

		if (isHoldingSpotlight(player)) {
			renderSpotlightGhost(event, state, panel, pos);
			return;
		}

		if (isHoldingWrench(player))
			renderAttachmentOutline(event, state, panel, pos);
	}

	private static void renderBufferLightsGhost(DrawSelectionEvent.HighlightBlock event, BlockState state,
		TrainWallPanelBlockEntity panel, BlockPos pos) {
		if (!AllBlocks.isAttachableTrainWallPanelState(state) || panel.hasAttachment())
			return;

		PoseStack poseStack = event.getPoseStack();
		Vec3 cameraPos = event.getCamera()
			.getPosition();
		Direction facing = state.getValue(TrainWallPanelBlock.FACING);

		poseStack.pushPose();
		poseStack.translate(pos.getX() - cameraPos.x, pos.getY() - cameraPos.y, pos.getZ() - cameraPos.z);
		LocomotiveBufferLightsRenderHelper.renderPanelAttachmentGhost(state, LocomotiveBufferLightsVariant.TYPE_1,
			facing, poseStack, event.getMultiBufferSource());
		poseStack.popPose();
	}

	private static void renderSpotlightGhost(DrawSelectionEvent.HighlightBlock event, BlockState state,
		TrainWallPanelBlockEntity panel, BlockPos pos) {
		if (!AllBlocks.isAttachableTrainWallPanelState(state) || panel.hasAttachment())
			return;

		PoseStack poseStack = event.getPoseStack();
		Vec3 cameraPos = event.getCamera().getPosition();
		Direction facing = state.getValue(TrainWallPanelBlock.FACING);

		poseStack.pushPose();
		poseStack.translate(pos.getX() - cameraPos.x, pos.getY() - cameraPos.y, pos.getZ() - cameraPos.z);
		LocomotiveSpotlightRenderHelper.renderPanelAttachmentGhost(state, LocomotiveSpotlightVariant.SMALL, facing,
			poseStack, event.getMultiBufferSource());
		poseStack.popPose();
	}

	private static void renderAttachmentOutline(DrawSelectionEvent.HighlightBlock event, BlockState state,
		TrainWallPanelBlockEntity panel, BlockPos pos) {
		TrainPanelAttachment attachment = panel.getAttachment();
		if (attachment == null)
			return;

		VoxelShape shape = attachment.getInteractionShape(state);
		if (shape.isEmpty())
			return;
		if (!TrainWallPanelBlock.isAttachmentHit(state, pos, event.getTarget()
			.getLocation(), attachment))
			return;

		PoseStack poseStack = event.getPoseStack();
		Camera camera = event.getCamera();
		Vec3 cameraPos = camera.getPosition();
		VertexConsumer lines = event.getMultiBufferSource()
			.getBuffer(RenderType.lines());

		poseStack.pushPose();
		LevelRenderer.renderVoxelShape(poseStack, lines, shape, pos.getX() - cameraPos.x, pos.getY() - cameraPos.y,
			pos.getZ() - cameraPos.z, 1f, 0.55f, 0.25f, 1f);
		poseStack.popPose();
	}

	private static boolean isHoldingBufferLights(Player player) {
		return AllBlocks.LOCOMOTIVE_BUFFER_LIGHTS.isIn(player.getMainHandItem())
			|| AllBlocks.LOCOMOTIVE_BUFFER_LIGHTS.isIn(player.getOffhandItem());
	}

	private static boolean isHoldingSpotlight(Player player) {
		return AllBlocks.LOCOMOTIVE_SPOTLIGHT.isIn(player.getMainHandItem())
			|| AllBlocks.LOCOMOTIVE_SPOTLIGHT.isIn(player.getOffhandItem());
	}

	private static boolean isHoldingWrench(Player player) {
		return isWrench(player.getMainHandItem()) || isWrench(player.getOffhandItem());
	}

	private static boolean isWrench(ItemStack stack) {
		return AllItems.WRENCH.isIn(stack);
	}
}
