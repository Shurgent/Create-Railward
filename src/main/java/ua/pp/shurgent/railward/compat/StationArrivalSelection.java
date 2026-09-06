package ua.pp.shurgent.railward.compat;

import java.util.UUID;
import java.util.regex.PatternSyntaxException;

/** Selection of a physical platform, or a candidate before Create chooses one. */
public final class StationArrivalSelection {
	private StationArrivalSelection() {}

	public static boolean matches(UUID stationId, UUID assignedId, String stationName, String filterRegex) {
		if (assignedId != null)
			return assignedId.equals(stationId);
		if (filterRegex == null)
			return false;
		try {
			return stationName.matches(filterRegex);
		} catch (PatternSyntaxException ignored) {
			// Create also ignores malformed destination filters during navigation.
			return false;
		}
	}
}
