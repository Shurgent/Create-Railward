package ua.pp.shurgent.railward.client.renderer;

//? if create6 {
/*import java.util.Arrays;
import java.util.function.Consumer;

import com.jozufozu.flywheel.core.PartialModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.trains.bogey.BogeyVisual;
import com.simibubi.create.foundation.render.SpecialModels;

import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import net.minecraft.nbt.CompoundTag;

public final class RailwardBogeyVisual implements BogeyVisual {

	private final TransformedInstance frame;
	private final TransformedInstance[] wheels;
	private final float frameY;
	private final float wheelY;
	private final float[] wheelZ;

	public RailwardBogeyVisual(VisualizationContext context, PartialModel frameModel, PartialModel wheelModel,
		float frameY, float wheelY, float... wheelZ) {
		this.frameY = frameY;
		this.wheelY = wheelY;
		this.wheelZ = wheelZ.clone();
		var provider = context.instancerProvider();
		frame = provider.instancer(InstanceTypes.TRANSFORMED, SpecialModels.smoothLit(frameModel)).createInstance();
		var wheelInstancer = provider.instancer(InstanceTypes.TRANSFORMED, SpecialModels.smoothLit(wheelModel));
		wheels = new TransformedInstance[wheelZ.length];
		Arrays.setAll(wheels, index -> wheelInstancer.createInstance());
	}

	@Override
	public void update(CompoundTag bogeyData, float wheelAngle, PoseStack poseStack) {
		frame.setTransform(poseStack).translate(0, frameY, 0).setChanged();
		for (int index = 0; index < wheels.length; index++)
			wheels[index].setTransform(poseStack).translate(0, wheelY, wheelZ[index])
				.rotateXDegrees(wheelAngle).setChanged();
	}

	@Override
	public void hide() {
		frame.setZeroTransform().setChanged();
		for (TransformedInstance wheel : wheels)
			wheel.setZeroTransform().setChanged();
	}

	@Override
	public void updateLight(int light) {
		frame.light(light).setChanged();
		for (TransformedInstance wheel : wheels)
			wheel.light(light).setChanged();
	}

	@Override
	public void collectCrumblingInstances(Consumer<Instance> consumer) {
		consumer.accept(frame);
		for (TransformedInstance wheel : wheels)
			consumer.accept(wheel);
	}

	@Override
	public void delete() {
		frame.delete();
		for (TransformedInstance wheel : wheels)
			wheel.delete();
	}
}
*///?}
