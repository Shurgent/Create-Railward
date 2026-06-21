package ua.pp.shurgent.railward.block.train.parts;

import java.util.Locale;

import net.minecraft.util.StringRepresentable;

public enum LocomotiveBufferLightsVariant implements StringRepresentable {
	TYPE_1("type_1", "1"),
	TYPE_2("type_2", "2"),
	TYPE_3("type_3", "3"),
	TYPE_4("type_4", "4"),
	TYPE_5("type_5", "5"),
	TYPE_6("type_6", "6"),
	TYPE_7("type_7", "7"),
	TYPE_8("type_8", "8"),
	TYPE_9("type_9", "9"),
	TYPE_10("type_10", "10");

	private final String serializedName;
	private final String modelSuffix;

	LocomotiveBufferLightsVariant(String serializedName, String modelSuffix) {
		this.serializedName = serializedName;
		this.modelSuffix = modelSuffix;
	}

	public LocomotiveBufferLightsVariant next() {
		LocomotiveBufferLightsVariant[] values = values();
		return values[(ordinal() + 1) % values.length];
	}

	public String getModelSuffix() {
		return modelSuffix;
	}

	@Override
	public String getSerializedName() {
		return serializedName;
	}

	public static LocomotiveBufferLightsVariant byName(String name) {
		String normalized = name == null ? "" : name.toLowerCase(Locale.ROOT);
		for (LocomotiveBufferLightsVariant variant : values())
			if (variant.getSerializedName().equals(normalized))
				return variant;
		return TYPE_1;
	}
}
