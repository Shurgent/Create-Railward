package ua.pp.shurgent.railward;

import org.apache.commons.lang3.tuple.Pair;

import net.minecraftforge.common.ForgeConfigSpec;

public class RailwardConfig {

	public static final ForgeConfigSpec COMMON_SPEC;
	public static final Common COMMON;

	static {
		Pair<Common, ForgeConfigSpec> pair = new ForgeConfigSpec.Builder().configure(Common::new);
		COMMON = pair.getLeft();
		COMMON_SPEC = pair.getRight();
	}

	public static class Common {
		public final ForgeConfigSpec.BooleanValue trainCouplerDebugLogging;

		private Common(ForgeConfigSpec.Builder builder) {
			builder.push("debug");
			trainCouplerDebugLogging = builder
				.comment("Enable detailed Train Coupler debug logging. Disabled by default because it is very verbose.")
				.define("trainCouplerDebugLogging", false);
			builder.pop();
		}
	}

}
