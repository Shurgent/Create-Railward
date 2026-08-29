package ua.pp.shurgent.railward;

import org.apache.commons.lang3.tuple.Pair;

import net.minecraftforge.common.ForgeConfigSpec;

public class RailwardConfig {

	public static final ForgeConfigSpec COMMON_SPEC;
	public static final Common COMMON;
	public static final ForgeConfigSpec CLIENT_SPEC;
	public static final Client CLIENT;

	static {
		Pair<Common, ForgeConfigSpec> commonPair = new ForgeConfigSpec.Builder().configure(Common::new);
		COMMON = commonPair.getLeft();
		COMMON_SPEC = commonPair.getRight();

		Pair<Client, ForgeConfigSpec> clientPair = new ForgeConfigSpec.Builder().configure(Client::new);
		CLIENT = clientPair.getLeft();
		CLIENT_SPEC = clientPair.getRight();
	}

	public static class Client {
		public final ForgeConfigSpec.BooleanValue locomotiveSpotlightProjectedLight;
		public final ForgeConfigSpec.IntValue locomotiveSpotlightBeamLength;
		public final ForgeConfigSpec.IntValue locomotiveSpotlightBeamWidth;
		public final ForgeConfigSpec.IntValue dieselExhaustParticleRenderDistance;

		private Client(ForgeConfigSpec.Builder builder) {
			builder.push("locomotiveSpotlight");
			locomotiveSpotlightProjectedLight = builder
				.comment("Enable the client-side projected light cast by locomotive spotlights.")
				.define("projectedLight", true);
			locomotiveSpotlightBeamLength = builder
				.comment("Projected spotlight beam length in blocks.")
				.defineInRange("beamLength", 128, 8, 128);
			locomotiveSpotlightBeamWidth = builder
				.comment("Full projected spotlight beam width at its far end, in blocks.")
				.defineInRange("beamWidth", 32, 1, 64);
			builder.pop();

			builder.push("dieselExhaust");
			dieselExhaustParticleRenderDistance = builder
				.comment("Diesel exhaust particle render distance in blocks. Set to 0 to disable the particles.")
				.defineInRange("particleRenderDistance", 128, 0, 256);
			builder.pop();
		}
	}

	public static class Common {
		public final ForgeConfigSpec.BooleanValue trainCouplerDebugLogging;
		public final ForgeConfigSpec.BooleanValue trainCouplerAutoDebugLogging;
		public final ForgeConfigSpec.BooleanValue trainCouplerAutoCoupling;
		public final ForgeConfigSpec.IntValue trainCouplerAutoScanIntervalTicks;
		public final ForgeConfigSpec.DoubleValue trainCouplerAutoMaxSpeed;
		public final ForgeConfigSpec.IntValue speedSignStepKmh;
		public final ForgeConfigSpec.IntValue speedSignMaxSpeedKmh;

		private Common(ForgeConfigSpec.Builder builder) {
			builder.push("trainCoupler");
			trainCouplerAutoCoupling = builder
				.comment("Enable automatic Train Coupler coupling when a slow moving train approaches a stopped train.")
				.define("trainCouplerAutoCoupling", true);
			trainCouplerAutoScanIntervalTicks = builder
				.comment("How often automatic Train Couplers scan for matching couplers, in ticks.")
				.defineInRange("trainCouplerAutoScanIntervalTicks", 5, 1, 200);
			trainCouplerAutoMaxSpeed = builder
				.comment("Maximum train speed for automatic coupling. Default is slightly above Create's lowest train speed notch.")
				.defineInRange("trainCouplerAutoMaxSpeed", 0.09D, 0.01D, 0.2D);
			builder.pop();

			builder.push("speedSign");
			speedSignStepKmh = builder
				.comment("Speed Sign speed increment in km/h. The minimum selectable speed is equal to this value.")
				.defineInRange("speedSignStepKmh", 5, 1, 1000);
			speedSignMaxSpeedKmh = builder
				.comment("Maximum selectable Speed Sign speed in km/h. Values not divisible by the step are rounded down in the UI.")
				.defineInRange("speedSignMaxSpeedKmh", 150, 1, 1000);
			builder.pop();

			builder.push("debug");
			trainCouplerDebugLogging = builder
				.comment("Enable detailed Train Coupler debug logging. Disabled by default because it is very verbose.")
				.define("trainCouplerDebugLogging", false);
			trainCouplerAutoDebugLogging = builder
				.comment("Enable detailed automatic Train Coupler debug logging. Enabled temporarily while the feature is being tested.")
				.define("trainCouplerAutoDebugLogging", false);
			builder.pop();
		}
	}

}
