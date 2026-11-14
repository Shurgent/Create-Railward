package ua.pp.shurgent.railward.client.renderer;

//? if !create6 {
import com.jozufozu.flywheel.api.MaterialManager;
import com.jozufozu.flywheel.api.instance.DynamicInstance;
import com.jozufozu.flywheel.backend.instancing.blockentity.BlockEntityInstance;
import com.jozufozu.flywheel.util.AnimationTickHolder;
import com.jozufozu.flywheel.util.box.GridAlignedBB;
import com.jozufozu.flywheel.util.box.ImmutableBox;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import com.simibubi.create.content.trains.bogey.AbstractBogeyBlock;
import com.simibubi.create.content.trains.bogey.AbstractBogeyBlockEntity;
import com.simibubi.create.content.trains.bogey.BogeyRenderer;
import com.simibubi.create.content.trains.bogey.BogeySizes;
import com.simibubi.create.content.trains.bogey.BogeyStyle;

import net.minecraft.core.Direction;
import net.minecraft.world.level.LightLayer;
import ua.pp.shurgent.railward.RailwardBogeyStyles;

public final class RailwardBogeyBlockInstance extends BlockEntityInstance<AbstractBogeyBlockEntity>
	implements DynamicInstance {

	private final PoseStack pose = new PoseStack();
	private final ImmutableBox volume;
	private BogeyStyle style;
	private BogeyRenderer renderer;
	private float lastAngle = Float.NaN;

	public RailwardBogeyBlockInstance(MaterialManager materialManager, AbstractBogeyBlockEntity blockEntity) {
		super(materialManager, blockEntity);
		var position = getInstancePosition();
		pose.translate(position.getX() + .5, position.getY() + .5, position.getZ() + .5);
		if (blockState.getValue(AbstractBogeyBlock.AXIS) == Direction.Axis.X)
			pose.mulPose(Vector3f.YP.rotationDegrees(90));
		pose.translate(0, -1.5 - 1 / 128f, 0);
		volume = GridAlignedBB.from(blockEntity.getRenderBoundingBox());
	}

	@Override
	public void beginFrame() {
		BogeyStyle currentStyle = blockEntity.getStyle();
		if (style != currentStyle) {
			remove();
			style = currentStyle;
			if (RailwardBogeyStyles.supportsPlacedInstancing(style)) {
				renderer = style.createRendererInstance(BogeySizes.SMALL);
				// Both Railward renderers only need the material manager to create their models.
				renderer.initialiseContraptionModelData(materialManager, null);
				updateLight();
				lastAngle = Float.NaN;
			}
		}

		if (renderer == null)
			return;
		float angle = blockEntity.getVirtualAngle(AnimationTickHolder.getPartialTicks());
		if (angle != lastAngle) {
			renderer.render(blockEntity.getBogeyData(), angle, pose);
			lastAngle = angle;
		}
	}

	@Override
	public void updateLight() {
		if (renderer != null)
			renderer.updateLight(world.getBrightness(LightLayer.BLOCK, pos), world.getBrightness(LightLayer.SKY, pos));
	}

	@Override
	public ImmutableBox getVolume() {
		return volume;
	}

	@Override
	protected void remove() {
		if (renderer != null) {
			renderer.remove();
			renderer = null;
		}
	}
}
//?}
