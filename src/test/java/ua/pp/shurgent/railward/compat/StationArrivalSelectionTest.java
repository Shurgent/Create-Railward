package ua.pp.shurgent.railward.compat;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class StationArrivalSelectionTest {
	private static final UUID PLATFORM_1 = new UUID(0, 1);
	private static final UUID PLATFORM_2 = new UUID(0, 2);
	private static final String SAME_NAME = "\\QSpawn Central\\E";
	private static final String PLATFORMS = "\\QSpawn Central P\\E.*\\Q\\E";

	@Test
	void assignedArrivalUsesPhysicalStationId() {
		assertTrue(StationArrivalSelection.matches(PLATFORM_1, PLATFORM_1, "Spawn Central", SAME_NAME));
		assertFalse(StationArrivalSelection.matches(PLATFORM_2, PLATFORM_1, "Spawn Central", SAME_NAME));
		assertFalse(StationArrivalSelection.matches(PLATFORM_1, PLATFORM_2, "Spawn Central P1", PLATFORMS));
		assertTrue(StationArrivalSelection.matches(PLATFORM_2, PLATFORM_2, "Renamed", SAME_NAME));
	}

	@Test
	void unresolvedWildcardMatchesOnlyCandidateNames() {
		assertTrue(StationArrivalSelection.matches(PLATFORM_1, null, "Spawn Central P1", PLATFORMS));
		assertTrue(StationArrivalSelection.matches(PLATFORM_2, null, "Spawn Central P2", PLATFORMS));
		assertFalse(StationArrivalSelection.matches(PLATFORM_1, null, "Other P1", PLATFORMS));
	}

	@Test
	void unresolvedExactNameRemainsAmbiguous() {
		assertTrue(StationArrivalSelection.matches(PLATFORM_1, null, "Spawn Central", SAME_NAME));
		assertTrue(StationArrivalSelection.matches(PLATFORM_2, null, "Spawn Central", SAME_NAME));
	}

	@Test
	void invalidOrMissingFiltersDoNotMatch() {
		assertFalse(StationArrivalSelection.matches(PLATFORM_1, null, "Spawn Central", null));
		assertFalse(StationArrivalSelection.matches(PLATFORM_1, null, "Spawn Central", "["));
	}

	@Test
	void quotedPunctuationRemainsLiteral() {
		assertTrue(StationArrivalSelection.matches(PLATFORM_1, null, "A.B?", "\\QA.B?\\E"));
		assertFalse(StationArrivalSelection.matches(PLATFORM_1, null, "AxBy", "\\QA.B?\\E"));
	}
}
