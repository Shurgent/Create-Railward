package ua.pp.shurgent.railward.compat;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

class TrainCouplerCompatibilityTest {
	private static final List<String> STYLES = List.of("link", "linkless", "knuckle", "knuckle_split", "screwlink");

	@Test
	void railwardDoesNotCoupleToSteamNRails() {
		var own = TrainCouplerCompatibility.typeOf("railward:train/parts/train_coupler", "");
		assertTrue(TrainCouplerCompatibility.canCouple(own, own));
		for (String style : STYLES)
			for (String block : List.of("link_and_pin", "headstock", "copycat_headstock")) {
				var foreign = TrainCouplerCompatibility.typeOf("railways:" + block, style);
				assertNotNull(foreign);
				assertFalse(TrainCouplerCompatibility.canCouple(own, foreign));
				assertFalse(TrainCouplerCompatibility.canCouple(foreign, own));
			}
	}

	@Test
	void standaloneCouplersRequireExactlyTheSameStyle() {
		for (String first : STYLES)
			for (String second : STYLES)
				assertEquals(first.equals(second), TrainCouplerCompatibility.canCouple(
					TrainCouplerCompatibility.typeOf("railways:link_and_pin", first),
					TrainCouplerCompatibility.typeOf("railways:link_and_pin", second)));
	}

	@Test
	void woodenAndCopycatHeadstocksShareOnlyMatchingStyles() {
		for (String first : STYLES)
			for (String second : STYLES)
				for (String block : List.of("headstock", "copycat_headstock"))
					assertEquals(first.equals(second), TrainCouplerCompatibility.canCouple(
						TrainCouplerCompatibility.typeOf("railways:headstock", first),
						TrainCouplerCompatibility.typeOf("railways:" + block, second)));
	}

	@Test
	void standaloneAndHeadstockShareMatchingHeadsInBothDirections() {
		for (String style : STYLES)
			for (String block : List.of("headstock", "copycat_headstock")) {
				var standalone = TrainCouplerCompatibility.typeOf("railways:link_and_pin", style);
				var headstock = TrainCouplerCompatibility.typeOf("railways:" + block, style);
				assertTrue(TrainCouplerCompatibility.canCouple(standalone, headstock));
				assertTrue(TrainCouplerCompatibility.canCouple(headstock, standalone));
				String differentStyle = style.equals("knuckle") ? "screwlink" : "knuckle";
				assertFalse(TrainCouplerCompatibility.canCouple(standalone,
					TrainCouplerCompatibility.typeOf("railways:" + block, differentStyle)));
			}
	}

	@Test
	void buffersUnknownStylesAndItemIdsAreNotCouplers() {
		for (String block : List.of("headstock", "copycat_headstock", "link_and_pin"))
			for (String style : List.of("plain", "buffer", "unknown", ""))
				assertNull(TrainCouplerCompatibility.typeOf("railways:" + block, style));
		assertNull(TrainCouplerCompatibility.typeOf("railways:knuckle_coupler", "knuckle"));
		assertNull(TrainCouplerCompatibility.typeOf("railways:track_coupler", "knuckle"));
		assertFalse(TrainCouplerCompatibility.canCouple(null, null));
	}
}
