package ua.pp.shurgent.railward.client.renderer;

import com.jozufozu.flywheel.core.PartialModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import com.simibubi.create.foundation.render.CachedBufferer;
import com.simibubi.create.foundation.render.RenderTypes;

import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.state.BlockState;
import ua.pp.shurgent.railward.AllPartialModels;
import ua.pp.shurgent.railward.block.train.signal.RailCrossingSignalBlock;
import ua.pp.shurgent.railward.block.train.signal.RailCrossingSignalBlockEntity;
import ua.pp.shurgent.railward.block.train.signal.RailCrossingSignalBlockEntity.CrossingSign;
import ua.pp.shurgent.railward.block.train.signal.RailCrossingSignalBlockEntity.Lamp;
import ua.pp.shurgent.railward.client.compat.RailwardSuperByteBufferTransform;
import ua.pp.shurgent.railward.client.ponder.RailCrossingSignalScenes;

public class RailCrossingSignalRenderer extends SmartBlockEntityRenderer<RailCrossingSignalBlockEntity> {

	public RailCrossingSignalRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	protected void renderSafe(RailCrossingSignalBlockEntity signal, float partialTicks, PoseStack ms,
		MultiBufferSource buffers, int light, int overlay) {
		BlockState state = signal.getBlockState();
		Lamp lamp = RailCrossingSignalScenes.lamp(signal);
		CrossingSign sign = signal.getSign();

		if (sign != CrossingSign.NONE)
			renderPart(AllPartialModels.CROSSING_SIGN_1, state, ms, buffers, RenderType.cutout(), light, false);
		if (sign == CrossingSign.MULTIPLE_TRACKS)
			renderPart(AllPartialModels.CROSSING_SIGN_2, state, ms, buffers, RenderType.cutout(), light, false);

		renderLamp(lamp == Lamp.LEFT, AllPartialModels.CROSSING_LEFT_LAMP_ON, AllPartialModels.CROSSING_LEFT_LAMP_OFF,
			AllPartialModels.CROSSING_LEFT_LENS, AllPartialModels.CROSSING_LEFT_GLOW, state, ms, buffers, light);
		renderLamp(lamp == Lamp.RIGHT, AllPartialModels.CROSSING_RIGHT_LAMP_ON, AllPartialModels.CROSSING_RIGHT_LAMP_OFF,
			AllPartialModels.CROSSING_RIGHT_LENS, AllPartialModels.CROSSING_RIGHT_GLOW, state, ms, buffers, light);
	}

	private static void renderLamp(boolean active, PartialModel on, PartialModel off, PartialModel lens, PartialModel glow,
		BlockState state, PoseStack ms, MultiBufferSource buffers, int light) {
		int lampLight = active ? LightTexture.FULL_BRIGHT : light;
		renderPart(active ? on : off, state, ms, buffers, RenderType.cutout(), lampLight, active);
		renderPart(lens, state, ms, buffers, RenderType.translucent(), lampLight, active);
		if (active)
			renderPart(glow, state, ms, buffers, RenderTypes.getAdditive(), lampLight, true);
	}

	private static void renderPart(PartialModel model, BlockState state, PoseStack ms, MultiBufferSource buffers,
		RenderType layer, int light, boolean emissive) {
		var buffer = CachedBufferer.partial(model, state);
		double rotation = 180 - state.getValue(RailCrossingSignalBlock.FACING).toYRot();
		RailwardSuperByteBufferTransform.of(buffer).centre().rotateY(rotation).unCentre();
		if (emissive)
			buffer.disableDiffuse();
		buffer.light(light).renderInto(ms, buffers.getBuffer(layer));
	}
}
