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
		public final ForgeConfigSpec.BooleanValue trainCouplerAutoDebugLogging;
		public final ForgeConfigSpec.BooleanValue trainCouplerAutoCoupling;
		public final ForgeConfigSpec.IntValue trainCouplerAutoScanIntervalTicks;
		public final ForgeConfigSpec.DoubleValue trainCouplerAutoMaxSpeed;

		private Common(ForgeConfigSpec.Builder builder) {
			builder.push("trainCoupler");
			trainCouplerAutoCoupling = builder
				.comment("Enable automatic Train Coupler coupling when a slow moving train approaches a stopped train.")
				.define("trainCouplerAutoCoupling", true);
			trainCouplerAutoScanIntervalTicks = builder
				.comment("How often automatic Train Couplers scan for matching couplers, in ticks.")
				.defineInRange("trainCouplerAutoScanIntervalTicks", 5, 1, 200);
			trainCouplerAutoMaxSpeed = builder
				.comment("Maximum train speed for automatic coupling. Default matches Create's lowest train speed notch.")
				.defineInRange("trainCouplerAutoMaxSpeed", 0.06D, 0.01D, 0.12D);
			builder.pop();

			builder.push("debug");
			trainCouplerDebugLogging = builder
				.comment("Enable detailed Train Coupler debug logging. Disabled by default because it is very verbose.")
				.define("trainCouplerDebugLogging", false);
			trainCouplerAutoDebugLogging = builder
				.comment("Enable detailed automatic Train Coupler debug logging. Enabled temporarily while the feature is being tested.")
				.define("trainCouplerAutoDebugLogging", true);
			builder.pop();
		}
	}

}
