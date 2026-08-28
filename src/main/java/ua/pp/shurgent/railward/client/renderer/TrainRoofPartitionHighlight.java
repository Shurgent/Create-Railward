package ua.pp.shurgent.railward.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.AllItems;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.client.event.DrawSelectionEvent;
import ua.pp.shurgent.railward.block.train.parts.TrainRoofBlock;
import ua.pp.shurgent.railward.block.train.parts.TrainRoofSlopeBlock;

public final class TrainRoofPartitionHighlight {

	private TrainRoofPartitionHighlight() {
	}

	public static void render(DrawSelectionEvent.HighlightBlock event) {
		Minecraft minecraft = Minecraft.getInstance();
		Player player = minecraft.player;
		Level level = minecraft.level;
		if (player == null || level == null || player.isShiftKeyDown() || !isHoldingWrench(player))
			return;

		BlockHitResult target = event.getTarget();
		BlockPos pos = target.getBlockPos();
		BlockState state = level.getBlockState(pos);
		VoxelShape shape = Shapes.empty();
		if (state.getBlock() instanceof TrainRoofBlock roof)
			shape = roof.getHighlightedPartitionShape(state, pos, target.getLocation(), target.getDirection());
		else if (state.getBlock() instanceof TrainRoofSlopeBlock slope)
			shape = slope.getHighlightedPartitionShape(state, pos, target.getLocation(), target.getDirection());
		if (shape.isEmpty())
			return;

		PoseStack poseStack = event.getPoseStack();
		Camera camera = event.getCamera();
		Vec3 cameraPos = camera.getPosition();
		VertexConsumer lines = event.getMultiBufferSource().getBuffer(RenderType.lines());

		poseStack.pushPose();
		VoxelShapeOutlineRenderer.render(poseStack, lines, shape, pos.getX() - cameraPos.x,
			pos.getY() - cameraPos.y, pos.getZ() - cameraPos.z, .15f, .85f, 1, 1);
		poseStack.popPose();
	}

	private static boolean isHoldingWrench(Player player) {
		return AllItems.WRENCH.isIn(player.getMainHandItem()) || AllItems.WRENCH.isIn(player.getOffhandItem());
	}
}
