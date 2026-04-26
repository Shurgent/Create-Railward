package ua.pp.shurgent.railward.block.train.parts;

import net.minecraft.util.StringRepresentable;

public enum WagonFrameSide implements StringRepresentable {
	FRONT("front"),
	RIGHT("right"),
	BACK("back"),
	LEFT("left");

	private final String serializedName;

	WagonFrameSide(String serializedName) {
		this.serializedName = serializedName;
	}

	@Override
	public String getSerializedName() {
		return serializedName;
	}
}
