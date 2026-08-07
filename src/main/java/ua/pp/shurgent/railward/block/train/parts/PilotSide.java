package ua.pp.shurgent.railward.block.train.parts;

import net.minecraft.util.StringRepresentable;

public enum PilotSide implements StringRepresentable {
	LEFT("left"),
	RIGHT("right");

	private final String serializedName;

	PilotSide(String serializedName) {
		this.serializedName = serializedName;
	}

	public PilotSide opposite() {
		return this == LEFT ? RIGHT : LEFT;
	}

	@Override
	public String getSerializedName() {
		return serializedName;
	}
}
