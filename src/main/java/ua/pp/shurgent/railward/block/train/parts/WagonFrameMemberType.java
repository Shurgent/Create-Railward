package ua.pp.shurgent.railward.block.train.parts;

import net.minecraft.util.StringRepresentable;

public enum WagonFrameMemberType implements StringRepresentable {
	NONE("none"),
	THIN("thin"),
	THICK("thick"),
	TAPER_THIN_TO_THICK("taper_thin_to_thick"),
	TAPER_THICK_TO_THIN("taper_thick_to_thin");

	private final String serializedName;

	WagonFrameMemberType(String serializedName) {
		this.serializedName = serializedName;
	}

	public WagonFrameMemberType next() {
		WagonFrameMemberType[] values = values();
		return values[(ordinal() + 1) % values.length];
	}

	public static WagonFrameMemberType byName(String name) {
		for (WagonFrameMemberType type : values())
			if (type.serializedName.equals(name))
				return type;
		return NONE;
	}

	@Override
	public String getSerializedName() {
		return serializedName;
	}
}
