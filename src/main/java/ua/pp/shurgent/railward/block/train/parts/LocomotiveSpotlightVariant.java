package ua.pp.shurgent.railward.block.train.parts;

import java.util.Locale;

import net.minecraft.util.StringRepresentable;

public enum LocomotiveSpotlightVariant implements StringRepresentable {
	SMALL("small"),
	LARGE("large");

	private final String serializedName;

	LocomotiveSpotlightVariant(String serializedName) {
		this.serializedName = serializedName;
	}

	public LocomotiveSpotlightVariant next() {
		LocomotiveSpotlightVariant[] values = values();
		return values[(ordinal() + 1) % values.length];
	}

	@Override
	public String getSerializedName() {
		return serializedName;
	}

	public static LocomotiveSpotlightVariant byName(String name) {
		String normalized = name == null ? "" : name.toLowerCase(Locale.ROOT);
		for (LocomotiveSpotlightVariant variant : values())
			if (variant.getSerializedName().equals(normalized))
				return variant;
		return SMALL;
	}
}
