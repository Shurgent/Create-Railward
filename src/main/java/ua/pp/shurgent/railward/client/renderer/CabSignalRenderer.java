package ua.pp.shurgent.railward.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import ua.pp.shurgent.railward.block.surface.SurfaceMountedBlock;
import ua.pp.shurgent.railward.block.train.parts.CabSignalBlockEntity;

public class CabSignalRenderer extends SmartBlockEntityRenderer<CabSignalBlockEntity> {

	public CabSignalRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	protected void renderSafe(CabSignalBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffers,
		int light, int overlay) {
		BlockState state = be.getBlockState();
		Direction facing = state.getValue(SurfaceMountedBlock.FACING);
		CabSignalRenderHelper.render(state, facing, be.getAnchor(), ms, buffers, light);
	}
}
