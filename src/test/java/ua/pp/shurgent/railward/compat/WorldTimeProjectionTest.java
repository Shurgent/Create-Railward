package ua.pp.shurgent.railward.compat;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class WorldTimeProjectionTest {
	@Test
	void nonPositiveArrivalDelayKeepsCurrentDayTime() {
		WorldTimeProjection.Snapshot snapshot = snapshot(6_000, 100, 1, null);

		assertEquals(6_000, snapshot.project(0));
		assertEquals(6_000, snapshot.project(-20));
	}

	@Test
	void observedRateProjectsAndRoundsDayTicks() {
		assertEquals(6_020, snapshot(6_000, 100, 1, null).project(20));
		assertEquals(6_007, snapshot(6_000, 100, .35, null).project(20));
		assertEquals(6_000, snapshot(6_000, 100, 0, null).project(20));
	}

	@Test
	void b3mSlowModePreservesClockPhase() {
		WorldTimeProjection.Snapshot snapshot = snapshot(6_000, 5, .5, 2);

		assertEquals(6_001, snapshot.project(1));
		assertEquals(6_002, snapshot.project(4));
	}

	@Test
	void b3mFastAndFrozenModesUseExactMultipliers() {
		assertEquals(6_060, snapshot(6_000, 100, 3, -3).project(20));
		assertEquals(6_000, snapshot(6_000, 100, 0, 0).project(20));
		assertEquals(6_020, snapshot(6_000, 100, 1, 1).project(20));
		assertEquals(6_020, snapshot(6_000, 100, 1, -1).project(20));
	}

	private static WorldTimeProjection.Snapshot snapshot(long dayTime, long gameTime, double rate,
		Integer b3mMultiplier) {
		return new WorldTimeProjection.Snapshot(dayTime, gameTime, rate, "test", b3mMultiplier);
	}
}
