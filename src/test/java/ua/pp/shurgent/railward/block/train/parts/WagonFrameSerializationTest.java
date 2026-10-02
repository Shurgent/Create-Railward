package ua.pp.shurgent.railward.block.train.parts;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class WagonFrameSerializationTest {
	@Test
	void equipmentNamesRoundTripAndUnknownValuesAreSafe() {
		for (WagonFrameEquipmentType value : WagonFrameEquipmentType.values())
			assertEquals(value, WagonFrameEquipmentType.byName(value.getSerializedName()));

		assertEquals(WagonFrameEquipmentType.NONE, WagonFrameEquipmentType.byName("unknown"));
		assertEquals(WagonFrameEquipmentType.NONE, WagonFrameEquipmentType.byName(null));
	}

	@Test
	void equipmentCycleIncludesEveryValueAndWraps() {
		WagonFrameEquipmentType value = WagonFrameEquipmentType.NONE;
		for (int i = 0; i < WagonFrameEquipmentType.values().length; i++)
			value = value.next();

		assertEquals(WagonFrameEquipmentType.NONE, value);
	}

	@Test
	void memberNamesRoundTripAndUnknownValuesAreSafe() {
		for (WagonFrameMemberType value : WagonFrameMemberType.values())
			assertEquals(value, WagonFrameMemberType.byName(value.getSerializedName()));

		assertEquals(WagonFrameMemberType.NONE, WagonFrameMemberType.byName("unknown"));
		assertEquals(WagonFrameMemberType.NONE, WagonFrameMemberType.byName(null));
	}

	@Test
	void memberCycleIncludesEveryValueAndWraps() {
		WagonFrameMemberType value = WagonFrameMemberType.NONE;
		for (int i = 0; i < WagonFrameMemberType.values().length; i++)
			value = value.next();

		assertEquals(WagonFrameMemberType.NONE, value);
	}

	@Test
	void handBrakeValveNamesRoundTripAndUnknownValuesAreSafe() {
		for (WagonFrameHandBrakeValveSide value : WagonFrameHandBrakeValveSide.values())
			assertEquals(value, WagonFrameHandBrakeValveSide.byName(value.getSerializedName()));

		assertEquals(WagonFrameHandBrakeValveSide.NONE, WagonFrameHandBrakeValveSide.byName("unknown"));
		assertEquals(WagonFrameHandBrakeValveSide.NONE, WagonFrameHandBrakeValveSide.byName(null));
	}
}
