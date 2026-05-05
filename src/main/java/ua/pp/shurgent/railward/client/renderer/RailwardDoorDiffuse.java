package ua.pp.shurgent.railward.client.renderer;

import com.jozufozu.flywheel.util.DiffuseLightCalculator;
import com.mojang.math.Vector3f;

import net.minecraft.client.Minecraft;

class RailwardDoorDiffuse {

	static final DiffuseLightCalculator CAMERA_CORRECTED = (x, y, z, shaded) -> {
		Vector3f normal = new Vector3f(x, y, z);
		normal.transform(Minecraft.getInstance()
				.gameRenderer
				.getMainCamera()
				.rotation());
		return DiffuseLightCalculator.forCurrentLevel()
				.getDiffuse(normal.x(), normal.y(), normal.z(), shaded);
	};
}
