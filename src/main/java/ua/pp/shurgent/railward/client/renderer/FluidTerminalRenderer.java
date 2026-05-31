package ua.pp.shurgent.railward.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import com.simibubi.create.foundation.render.CachedBufferer;
import com.simibubi.create.foundation.render.SuperByteBuffer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import ua.pp.shurgent.railward.AllPartialModels;
import ua.pp.shurgent.railward.block.train.station.FluidTerminalBlockEntity;

public class FluidTerminalRenderer extends SafeBlockEntityRenderer<FluidTerminalBlockEntity> {

	public FluidTerminalRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	protected void renderSafe(FluidTerminalBlockEntity be, float partialTicks, PoseStack ms,
		MultiBufferSource buffer, int light, int overlay) {
		float extension = be.getHoseExtensionDistance(partialTicks);
		if (extension <= 1 / 64f)
			return;

		BlockState state = be.getBlockState();
		VertexConsumer vb = buffer.getBuffer(RenderType.solid());
		renderHose(AllPartialModels.FLUID_TERMINAL_HOSE_UPPER, state, Mth.clamp(extension, 0, 1), ms, vb,
			light);
		if (extension > 1)
			renderHose(AllPartialModels.FLUID_TERMINAL_HOSE_LOWER, state, Mth.clamp(extension, 1, 2), ms, vb,
				light);
	}

	private void renderHose(com.jozufozu.flywheel.core.PartialModel model, BlockState state, float extension,
		PoseStack ms, VertexConsumer vb, int light) {
		SuperByteBuffer hose = CachedBufferer.partial(model, state);
		hose.translate(0, -extension, 0)
			.light(light)
			.renderInto(ms, vb);
	}
}
