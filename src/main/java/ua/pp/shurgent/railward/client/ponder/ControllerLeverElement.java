package ua.pp.shurgent.railward.client.ponder;

import com.mojang.blaze3d.vertex.PoseStack;
import com.jozufozu.flywheel.core.PartialModel;
import com.simibubi.create.foundation.render.CachedBufferer;
//? if create6 {
/*import net.createmod.ponder.api.element.PonderSceneElement;
import net.createmod.ponder.foundation.element.PonderElementBase;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.api.level.PonderLevel;
import net.minecraft.client.gui.GuiGraphics;
*///?} else {
import com.simibubi.create.foundation.ponder.PonderScene;
import com.simibubi.create.foundation.ponder.PonderWorld;
import com.simibubi.create.foundation.ponder.element.PonderSceneElement;
//?}
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import ua.pp.shurgent.railward.AllPartialModels;
import ua.pp.shurgent.railward.block.train.parts.ModernTrainControllerBlock;
import ua.pp.shurgent.railward.client.compat.RailwardSuperByteBufferTransform;

/** Scene-local rendering of the same three partials/pivots used on moving trains. */
//? if create6 {
/*public final class ControllerLeverElement extends PonderElementBase implements PonderSceneElement {
*///?} else {
public final class ControllerLeverElement extends PonderSceneElement {
//?}
	private final BlockPos position;
	private float reverser, previousReverser, throttle, previousThrottle, brake, previousBrake;
	public ControllerLeverElement(BlockPos position) { this.position = position; }

	@Override public void reset(PonderScene scene) {
		reverser = previousReverser = throttle = previousThrottle = brake = previousBrake = 0;
	}

	@Override public void tick(PonderScene scene) {
		BlockState state = scene.getWorld().getBlockState(position);
		if (!(state.getBlock() instanceof ModernTrainControllerBlock)) return;
		boolean active = state.getValue(ModernTrainControllerBlock.ACTIVE);
		previousReverser = reverser;
		previousThrottle = throttle;
		previousBrake = brake;
		reverser += ((active ? state.getValue(ModernTrainControllerBlock.REVERSED) ? -1 : 1 : 0) - reverser) * .25f;
		throttle += ((active && state.getValue(ModernTrainControllerBlock.THROTTLE) ? 1 : 0) - throttle) * .3f;
		brake += ((active && state.getValue(ModernTrainControllerBlock.BRAKING) ? 1 : 0) - brake) * .3f;
	}

	public float reverser(float pt) { return Mth.lerp(pt, previousReverser, reverser); }
	public float throttle(float pt) { return Mth.lerp(pt, previousThrottle, throttle); }
	public float brake(float pt) { return Mth.lerp(pt, previousBrake, brake); }

	//? if create6 {
	/*@Override public void renderFirst(PonderLevel world, MultiBufferSource buffer, GuiGraphics graphics, float pt) {}
	@Override public void renderLayer(PonderLevel world, MultiBufferSource buffer, RenderType type, GuiGraphics graphics, float pt) {
		if (type == RenderType.solid()) render(world.getBlockState(position), buffer, graphics.pose(), pt);
	}
	@Override public void renderLast(PonderLevel world, MultiBufferSource buffer, GuiGraphics graphics, float pt) {}
	*///?} else {
	@Override public void renderFirst(PonderWorld world, MultiBufferSource buffer, PoseStack ms, float pt) {}
	@Override public void renderLayer(PonderWorld world, MultiBufferSource buffer, RenderType type, PoseStack ms, float pt) {
		if (type == RenderType.solid()) render(world.getBlockState(position), buffer, ms, pt);
	}
	@Override public void renderLast(PonderWorld world, MultiBufferSource buffer, PoseStack ms, float pt) {}
	//?}

	private void render(BlockState state, MultiBufferSource buffers, PoseStack ms, float pt) {
		if (!(state.getBlock() instanceof ModernTrainControllerBlock)) return;
		ms.pushPose();
		ms.translate(position.getX(), position.getY(), position.getZ());
		part(AllPartialModels.MODERN_TRAIN_CONTROLLER_REVERSER, state,
			new Vec3(12.5 / 16d, 10 / 16d, 6.5 / 16d), 45 * reverser(pt), buffers, ms);
		part(AllPartialModels.MODERN_TRAIN_CONTROLLER_THROTTLE, state,
			new Vec3(13.5 / 16d, 10 / 16d, 9.5 / 16d), -90 * throttle(pt), buffers, ms);
		part(AllPartialModels.MODERN_TRAIN_CONTROLLER_BRAKE, state,
			new Vec3(2.5 / 16d, 11 / 16d, 10.5 / 16d), 90 * brake(pt), buffers, ms);
		ms.popPose();
	}

	private static void part(PartialModel partial, BlockState state, Vec3 pivot, float angle,
		MultiBufferSource buffers, PoseStack ms) {
		var model = CachedBufferer.partial(partial, state);
		int rotation = switch (ModernTrainControllerBlock.getPhysicalFacing(state)) {
			case SOUTH -> 180;
			case WEST -> 270;
			case EAST -> 90;
			default -> 0;
		};
		// Rotate the buffer itself so vertices and diffuse-lighting normals agree.
		RailwardSuperByteBufferTransform.of(model).centre().rotateY(rotation).unCentre()
			.translate(pivot).rotateY(angle).translateBack(pivot);
		model.light(LightTexture.FULL_BRIGHT).renderInto(ms, buffers.getBuffer(RenderType.solid()));
	}
}
