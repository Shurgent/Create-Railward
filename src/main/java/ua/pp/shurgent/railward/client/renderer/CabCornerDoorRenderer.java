package ua.pp.shurgent.railward.client.renderer;

import com.jozufozu.flywheel.core.PartialModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.content.decoration.slidingDoor.SlidingDoorBlockEntity;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import com.simibubi.create.foundation.render.CachedBufferer;
import com.simibubi.create.foundation.render.SuperByteBuffer;
import com.simibubi.create.foundation.utility.animation.LerpedFloat;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import ua.pp.shurgent.railward.AllPartialModels;
import ua.pp.shurgent.railward.AllPartialModels.CabCornerDoorPartialModels;
import ua.pp.shurgent.railward.block.train.parts.door.CabCornerDoorBlockEntity;
import ua.pp.shurgent.railward.client.compat.RailwardSuperByteBufferTransform;
import ua.pp.shurgent.railward.mixin.create.SlidingDoorBlockEntityAccessor;

public class CabCornerDoorRenderer extends SafeBlockEntityRenderer<CabCornerDoorBlockEntity> {

	private static final float DOOR_WIDTH_SCALE = 13 / 16f;

	public CabCornerDoorRenderer(Context context) {}

	@Override
	protected void renderSafe(CabCornerDoorBlockEntity be, float partialTicks, PoseStack ms,
			MultiBufferSource buffer, int light, int overlay) {
		BlockState state = be.getBlockState();
		CabCornerDoorPartialModels models = AllPartialModels.getCabCornerDoorModels(ForgeRegistries.BLOCKS
			.getKey(state.getBlock())
			.getPath());
		if (models == null)
			return;

		SlidingDoorBlockEntityAccessor accessor = (SlidingDoorBlockEntityAccessor) (SlidingDoorBlockEntity) be;
		LerpedFloat animation = accessor.railward$getAnimation();
		float eased = animation.getValue(partialTicks);
		eased *= eased;

		Direction facing = state.getValue(DoorBlock.FACING);
		boolean panelOnRight = state.getValue(DoorBlock.HINGE) == DoorHingeSide.RIGHT;
		VertexConsumer vb = buffer.getBuffer(RenderType.cutout());

		for (DoubleBlockHalf half : DoubleBlockHalf.values()) {
			// SuperByteBuffer's local Y rotation has the opposite handedness to VoxelShaper.
			renderPanel(models.getPanel(be.getPanel(half)), state, facing, !panelOnRight, half, partialTicks, ms, vb,
				light);
			renderDoor(models.getDoor(half, panelOnRight), state, facing, panelOnRight, half, eased, partialTicks, ms,
				vb, light);
		}
	}

	private static void renderPanel(PartialModel model, BlockState state, Direction facing, boolean panelOnRight,
			DoubleBlockHalf half, float partialTicks, PoseStack ms, VertexConsumer vb, int light) {
		int panelRotation = blockStateRotation(facing) + (panelOnRight ? 270 : 90);
		SuperByteBuffer panel = ContraptionDiffuseLighting.apply(CachedBufferer.partial(model, state), partialTicks);
		RailwardSuperByteBufferTransform.of(panel)
			.centre()
			.rotateY(panelRotation)
			.unCentre()
			.translate(0, half == DoubleBlockHalf.UPPER ? 1 - 1 / 512f : 0, 0);
		panel.light(light)
			.renderInto(ms, vb);
	}

	private static void renderDoor(PartialModel model, BlockState state, Direction facing, boolean panelOnRight,
			DoubleBlockHalf half, float eased, float partialTicks, PoseStack ms, VertexConsumer vb, int light) {
		// Keep the hinge axis 1.5 pixels clear of the panel. When open, the
		// three-pixel door is therefore flush with the panel instead of intersecting it.
		Vec3 hingePivot = new Vec3(panelOnRight ? 11.5f / 16f : 4.5f / 16f, 0, 14.5f / 16f);
		Vec3 scalePivot = new Vec3(panelOnRight ? 0 : 1, 0, 0);
		float angle = (panelOnRight ? -90 : 90) * eased;

		SuperByteBuffer door = CachedBufferer.partial(model, state);
		ContraptionDiffuseLighting.apply(door, partialTicks);
		RailwardSuperByteBufferTransform.of(door)
			.centre()
			.rotateY(blockStateRotation(facing))
			.unCentre()
			.translate(hingePivot)
			.rotateY(angle)
			.translateBack(hingePivot)
			.translate(scalePivot)
			.scale(DOOR_WIDTH_SCALE, 1, 1)
			.translateBack(scalePivot)
			.translate(0, half == DoubleBlockHalf.UPPER ? 1 - 1 / 512f : 0, 0);
		door.light(light)
			.renderInto(ms, vb);
	}

	private static int blockStateRotation(Direction facing) {
		return switch (facing) {
		case SOUTH -> 180;
		case WEST -> 90;
		case EAST -> 270;
		default -> 0;
		};
	}
}
