package ua.pp.shurgent.railward.block.train.parts;

import net.minecraft.util.StringRepresentable;

public enum WagonFrameEquipmentType implements StringRepresentable {
	NONE("none"),
	RECEIVER("receiver"),
	BRAKE_CYLINDER("brake_cylinder"),
	DISTRIBUTOR("distributor"),
	BRAKE_PIPE("brake_pipe"),
	FUEL_TANK("fuel_tank");

	private final String serializedName;

	WagonFrameEquipmentType(String serializedName) {
		this.serializedName = serializedName;
	}

	public WagonFrameEquipmentType next() {
		WagonFrameEquipmentType[] values = values();
		return values[(ordinal() + 1) % values.length];
	}

	public static WagonFrameEquipmentType byName(String name) {
		for (WagonFrameEquipmentType type : values())
			if (type.serializedName.equals(name))
				return type;
		return NONE;
	}

	@Override
	public String getSerializedName() {
		return serializedName;
	}
}
