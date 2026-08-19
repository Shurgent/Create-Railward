package ua.pp.shurgent.railward.block.train.parts.door;

import java.util.Locale;

public enum CabCornerDoorPanelVariant {
	SMOOTH,
	PLAIN,
	HOOD,
	GRILLE,
	STRIPE_YELLOW,
	STRIPE_YELLOW_SMOOTH,
	STRIPE_YELLOW_GRILLE,
	STRIPE_RED,
	STRIPE_RED_SMOOTH,
	STRIPE_RED_DOUBLE_SMOOTH;

	private static final CabCornerDoorPanelVariant[] VALUES = values();

	public CabCornerDoorPanelVariant next() {
		return VALUES[(ordinal() + 1) % VALUES.length];
	}

	public String getSerializedName() {
		return name().toLowerCase(Locale.ROOT);
	}

	public String getModelSuffix() {
		return this == PLAIN ? "" : "_" + getSerializedName();
	}

	public static CabCornerDoorPanelVariant byName(String name) {
		for (CabCornerDoorPanelVariant variant : VALUES)
			if (variant.getSerializedName().equals(name))
				return variant;
		return SMOOTH;
	}
}
