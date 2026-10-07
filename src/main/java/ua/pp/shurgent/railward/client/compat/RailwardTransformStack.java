package ua.pp.shurgent.railward.client.compat;

import com.mojang.blaze3d.vertex.PoseStack;

//? if create6 {
/*import dev.engine_room.flywheel.lib.transform.PoseTransformStack;
import dev.engine_room.flywheel.lib.transform.TransformStack;
*///?} else {
import com.jozufozu.flywheel.util.transform.TransformStack;
//?}

import net.minecraft.world.phys.Vec3;
import net.minecraft.core.Vec3i;

/**
 * Preserves degree-based renderer transforms across Flywheel 0.6 and 1.x.
 * Flywheel 1.x renamed the centering methods and made the short rotation
 * methods use radians, so direct API replacements would silently rotate
 * models incorrectly.
 */
public final class RailwardTransformStack {

	//? if create6
	/*private final PoseTransformStack delegate;
	*///? if !create6
	private final TransformStack delegate;

	//? if create6
	/*private RailwardTransformStack(PoseTransformStack delegate) {
	*///? if !create6
	private RailwardTransformStack(TransformStack delegate) {
		this.delegate = delegate;
	}

	public static RailwardTransformStack cast(PoseStack poseStack) {
		//? if create6
		/*return new RailwardTransformStack(TransformStack.of(poseStack));
		*///? if !create6
		return new RailwardTransformStack(TransformStack.cast(poseStack));
	}

	public RailwardTransformStack centre() {
		//? if create6
		/*delegate.center();
		*///? if !create6
		delegate.centre();
		return this;
	}

	public RailwardTransformStack unCentre() {
		//? if create6
		/*delegate.uncenter();
		*///? if !create6
		delegate.unCentre();
		return this;
	}

	public RailwardTransformStack translate(double x, double y, double z) {
		delegate.translate(x, y, z);
		return this;
	}

	public RailwardTransformStack translate(Vec3 offset) {
		delegate.translate(offset);
		return this;
	}

	public RailwardTransformStack translate(Vec3i offset) {
		delegate.translate(offset);
		return this;
	}

	public RailwardTransformStack rotateX(double degrees) {
		//? if create6
		/*delegate.rotateXDegrees((float) degrees);
		*///? if !create6
		delegate.rotateX(degrees);
		return this;
	}

	public RailwardTransformStack rotateY(double degrees) {
		//? if create6
		/*delegate.rotateYDegrees((float) degrees);
		*///? if !create6
		delegate.rotateY(degrees);
		return this;
	}

	public RailwardTransformStack rotateZ(double degrees) {
		//? if create6
		/*delegate.rotateZDegrees((float) degrees);
		*///? if !create6
		delegate.rotateZ(degrees);
		return this;
	}
}
