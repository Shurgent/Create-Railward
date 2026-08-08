package ua.pp.shurgent.railward.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.DrawSelectionEvent;
import ua.pp.shurgent.railward.AllBlocks;
import ua.pp.shurgent.railward.block.surface.SurfaceMountedBlock;
import ua.pp.shurgent.railward.block.surface.SurfaceMountedBlockEntity;

public final class SurfaceMountedPlacementPreview {

	private SurfaceMountedPlacementPreview() {
	}

	public static void render(DrawSelectionEvent.HighlightBlock event) {
		Minecraft minecraft = Minecraft.getInstance();
		Player player = minecraft.player;
		Level level = minecraft.level;
		if (player == null || level == null || !isHoldingCabSignal(player))
			return;

		BlockHitResult target = event.getTarget();
		BlockPos supportPos = target.getBlockPos();
		BlockState supportState = level.getBlockState(supportPos);
		if (supportState.getMaterial().isReplaceable())
			return;

		Direction facing = target.getDirection();
		BlockPos placedPos = supportPos.relative(facing);
		if (!level.getBlockState(placedPos).getMaterial().isReplaceable())
			return;

		BlockState state = AllBlocks.CAB_SIGNAL.getDefaultState().setValue(SurfaceMountedBlock.FACING, facing);
		if (!state.canSurvive(level, placedPos))
			return;

		Vec3 anchor = SurfaceMountedBlockEntity.fromWorldHit(target.getLocation(), placedPos, facing);
		Vec3 camera = event.getCamera().getPosition();
		PoseStack ms = event.getPoseStack();
		ms.pushPose();
		ms.translate(placedPos.getX() - camera.x, placedPos.getY() - camera.y, placedPos.getZ() - camera.z);
		CabSignalRenderHelper.renderGhost(state, facing, anchor, ms, event.getMultiBufferSource());
		ms.popPose();
	}

	private static boolean isHoldingCabSignal(Player player) {
		return AllBlocks.CAB_SIGNAL.isIn(player.getMainHandItem())
			|| AllBlocks.CAB_SIGNAL.isIn(player.getOffhandItem());
	}
}
