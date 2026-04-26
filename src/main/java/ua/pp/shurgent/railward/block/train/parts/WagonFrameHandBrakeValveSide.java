package ua.pp.shurgent.railward.block.train.parts;

import net.minecraft.util.StringRepresentable;

public enum WagonFrameHandBrakeValveSide implements StringRepresentable {
	NONE("none"),
	LEFT("left"),
	RIGHT("right");

	private final String serializedName;

	WagonFrameHandBrakeValveSide(String serializedName) {
		this.serializedName = serializedName;
	}

	public static WagonFrameHandBrakeValveSide byName(String name) {
		for (WagonFrameHandBrakeValveSide side : values())
			if (side.serializedName.equals(name))
				return side;
		return NONE;
	}

	@Override
	public String getSerializedName() {
		return serializedName;
	}
}
