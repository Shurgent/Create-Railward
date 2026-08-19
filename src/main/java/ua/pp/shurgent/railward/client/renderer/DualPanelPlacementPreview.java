package ua.pp.shurgent.railward.client.renderer;

import java.util.List;
import java.util.Random;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.DrawSelectionEvent;
import net.minecraftforge.client.model.data.EmptyModelData;
import ua.pp.shurgent.railward.block.train.parts.TrainWallPanelBlock;
import ua.pp.shurgent.railward.block.train.parts.copycat.CopycatWallPanelBlock;
import ua.pp.shurgent.railward.block.train.parts.copycat.DualPanelCopycatBehaviour;
import ua.pp.shurgent.railward.block.train.parts.copycat.HalfWallPanelBlock;
import ua.pp.shurgent.railward.block.train.parts.panel.DualPanelBlockItem;
import ua.pp.shurgent.railward.block.train.parts.panel.DualPanelPlacement;

/** Shows the narrowed panel that will be merged into an already occupied block. */
public final class DualPanelPlacementPreview {

	private static final float ALPHA = .55f;

	private DualPanelPlacementPreview() {
	}

	public static void render(DrawSelectionEvent.HighlightBlock event) {
		Minecraft minecraft = Minecraft.getInstance();
		Player player = minecraft.player;
		Level level = minecraft.level;
		if (player == null || level == null || player.isShiftKeyDown())
			return;

		InteractionHand hand = heldPanelHand(player);
		if (hand == null)
			return;
		ItemStack stack = player.getItemInHand(hand);
		DualPanelBlockItem item = (DualPanelBlockItem) stack.getItem();
		BlockHitResult target = event.getTarget();
		DualPanelPlacement.Placement placement = DualPanelPlacement.find(
				new UseOnContext(player, hand, target), item.getBlock());
		if (placement == null)
			return;

		BlockState primary = placement.primary();
		Direction.Axis lengthAxis = primaryAxis(primary);
		AABB primaryBounds = primaryBounds(primary);
		boolean primaryAtLowSide = axisMin(primaryBounds, lengthAxis) < .25;

		BlockPos pos = placement.pos();
		Vec3 camera = event.getCamera().getPosition();
		PoseStack poseStack = event.getPoseStack();
		poseStack.pushPose();
		poseStack.translate(pos.getX() - camera.x, pos.getY() - camera.y, pos.getZ() - camera.z);
		if (primaryAtLowSide)
			translate(poseStack, lengthAxis, 3 / 16f);
		scale(poseStack, lengthAxis, 13 / 16f);
		renderModel(minecraft, poseStack.last(), event, placement.secondary());
		poseStack.popPose();
	}

	private static void renderModel(Minecraft minecraft, PoseStack.Pose pose,
			DrawSelectionEvent.HighlightBlock event, BlockState state) {
		BakedModel model = minecraft.getBlockRenderer().getBlockModel(state);
		VertexConsumer consumer = event.getMultiBufferSource().getBuffer(RenderType.translucent());
		Random random = new Random();
		for (Direction direction : Direction.values()) {
			random.setSeed(42L);
			renderQuads(pose, consumer,
					model.getQuads(state, direction, random, EmptyModelData.INSTANCE));
		}
		random.setSeed(42L);
		renderQuads(pose, consumer, model.getQuads(state, null, random, EmptyModelData.INSTANCE));
	}

	private static void renderQuads(PoseStack.Pose pose, VertexConsumer consumer, List<BakedQuad> quads) {
		for (BakedQuad quad : quads) {
			float color = quad.isTinted() ? Mth.clamp(.85f, 0, 1) : 1;
			consumer.putBulkData(pose, quad, color, color, color, ALPHA,
					LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
		}
	}

	private static InteractionHand heldPanelHand(Player player) {
		if (player.getMainHandItem().getItem() instanceof DualPanelBlockItem)
			return InteractionHand.MAIN_HAND;
		if (player.getOffhandItem().getItem() instanceof DualPanelBlockItem)
			return InteractionHand.OFF_HAND;
		return null;
	}

	private static Direction.Axis primaryAxis(BlockState state) {
		Block block = state.getBlock();
		if (block instanceof TrainWallPanelBlock)
			return state.getValue(TrainWallPanelBlock.FACING).getAxis();
		if (block instanceof HalfWallPanelBlock)
			return state.getValue(HalfWallPanelBlock.FACING).getAxis();
		return state.getValue(CopycatWallPanelBlock.FACING).getAxis();
	}

	private static AABB primaryBounds(BlockState state) {
		if (state.getBlock() instanceof TrainWallPanelBlock)
			return TrainWallPanelBlock.getPanelBounds(state.getValue(TrainWallPanelBlock.FACING));
		return DualPanelCopycatBehaviour.getPrimaryBounds(state);
	}

	private static double axisMin(AABB box, Direction.Axis axis) {
		return axis.choose(box.minX, box.minY, box.minZ);
	}

	private static void translate(PoseStack poseStack, Direction.Axis axis, float amount) {
		switch (axis) {
			case X -> poseStack.translate(amount, 0, 0);
			case Y -> poseStack.translate(0, amount, 0);
			case Z -> poseStack.translate(0, 0, amount);
		}
	}

	private static void scale(PoseStack poseStack, Direction.Axis axis, float amount) {
		switch (axis) {
			case X -> poseStack.scale(amount, 1, 1);
			case Y -> poseStack.scale(1, amount, 1);
			case Z -> poseStack.scale(1, 1, amount);
		}
	}
}
