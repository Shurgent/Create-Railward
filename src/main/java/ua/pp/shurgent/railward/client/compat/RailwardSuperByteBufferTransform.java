package ua.pp.shurgent.railward.client.compat;

import com.simibubi.create.foundation.render.SuperByteBuffer;

import net.minecraft.world.phys.Vec3;

/** Degree-based local transforms for Create 5 and Flywheel 1.x buffers. */
public final class RailwardSuperByteBufferTransform {

	private final SuperByteBuffer buffer;

	private RailwardSuperByteBufferTransform(SuperByteBuffer buffer) {
		this.buffer = buffer;
	}

	public static RailwardSuperByteBufferTransform of(SuperByteBuffer buffer) {
		return new RailwardSuperByteBufferTransform(buffer);
	}

	public RailwardSuperByteBufferTransform centre() {
		//? if create6
		/*buffer.center();
		*///? if !create6
		buffer.centre();
		return this;
	}

	public RailwardSuperByteBufferTransform unCentre() {
		//? if create6
		/*buffer.uncenter();
		*///? if !create6
		buffer.unCentre();
		return this;
	}

	public RailwardSuperByteBufferTransform translate(double x, double y, double z) {
		buffer.translate(x, y, z);
		return this;
	}

	public RailwardSuperByteBufferTransform translate(Vec3 offset) {
		buffer.translate(offset);
		return this;
	}

	public RailwardSuperByteBufferTransform translateBack(Vec3 offset) {
		buffer.translateBack(offset);
		return this;
	}

	public RailwardSuperByteBufferTransform scale(float x, float y, float z) {
		buffer.scale(x, y, z);
		return this;
	}

	public RailwardSuperByteBufferTransform rotateX(double degrees) {
		//? if create6
		/*buffer.rotateXDegrees((float) degrees);
		*///? if !create6
		buffer.rotateX(degrees);
		return this;
	}

	public RailwardSuperByteBufferTransform rotateY(double degrees) {
		//? if create6
		/*buffer.rotateYDegrees((float) degrees);
		*///? if !create6
		buffer.rotateY(degrees);
		return this;
	}

	public RailwardSuperByteBufferTransform rotateZ(double degrees) {
		//? if create6
		/*buffer.rotateZDegrees((float) degrees);
		*///? if !create6
		buffer.rotateZ(degrees);
		return this;
	}
}
