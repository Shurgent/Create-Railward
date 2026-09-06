package ua.pp.shurgent.railward.compat;

import java.util.UUID;

/** Standalone regression checks; no Minecraft bootstrap or test library needed. */
public final class StationArrivalSelectionTest {
	public static void main(String[] args) {
		UUID p1 = new UUID(0, 1);
		UUID p2 = new UUID(0, 2);
		String sameName = "\\QSpawn Central\\E";
		String platforms = "\\QSpawn Central P\\E.*\\Q\\E";

		check(StationArrivalSelection.matches(p1, p1, "Spawn Central", sameName),
			"selected station receives its arrival");
		check(!StationArrivalSelection.matches(p2, p1, "Spawn Central", sameName),
			"identical station names must not leak assigned arrivals");
		check(!StationArrivalSelection.matches(p1, p2, "Spawn Central P1", platforms),
			"a wildcard must not override the selected UUID");
		check(StationArrivalSelection.matches(p2, p2, "Renamed", sameName),
			"renaming does not invalidate an active platform assignment");
		check(StationArrivalSelection.matches(p1, null, "Spawn Central P1", platforms)
			&& StationArrivalSelection.matches(p2, null, "Spawn Central P2", platforms),
			"unassigned wildcard call is visible at both candidates");
		check(!StationArrivalSelection.matches(p1, null, "Other P1", platforms),
			"unrelated names are excluded");
		check(StationArrivalSelection.matches(p1, null, "Spawn Central", sameName)
			&& StationArrivalSelection.matches(p2, null, "Spawn Central", sameName),
			"an exact name is still ambiguous before assignment");
		check(!StationArrivalSelection.matches(p1, null, "Spawn Central", null),
			"unresolved schedule instruction is not a forecast");
		check(!StationArrivalSelection.matches(p1, null, "Spawn Central", "["),
			"malformed filter is ignored like Create's navigation");
		check(StationArrivalSelection.matches(p1, null, "A.B?", "\\QA.B?\\E")
			&& !StationArrivalSelection.matches(p1, null, "AxBy", "\\QA.B?\\E"),
			"non-wildcard punctuation stays literal");
		System.out.println("StationArrivalSelection: 10 regression checks passed");
	}

	private static void check(boolean condition, String message) {
		if (!condition)
			throw new AssertionError(message);
	}
}
