package ua.pp.shurgent.railward.block.train.parts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static ua.pp.shurgent.railward.block.train.parts.ModernTrainControllerState.ReverserDecision.ALLOW;
import static ua.pp.shurgent.railward.block.train.parts.ModernTrainControllerState.ReverserDecision.INACTIVE;
import static ua.pp.shurgent.railward.block.train.parts.ModernTrainControllerState.ReverserDecision.MUST_STOP;

import org.junit.jupiter.api.Test;

class ModernTrainControllerStateTest {

	@Test
	void inactiveControllerIgnoresReverser() {
		assertEquals(INACTIVE, ModernTrainControllerState.evaluateReverser(false, 0, false, false));
	}

	@Test
	void fullyStoppedIdleControllerAllowsReverser() {
		assertEquals(ALLOW, ModernTrainControllerState.evaluateReverser(true, 0, false, false));
		assertEquals(ALLOW, ModernTrainControllerState.evaluateReverser(true, 1.0E-3, false, false));
		assertEquals(ALLOW, ModernTrainControllerState.evaluateReverser(true, -1.0E-3, false, false));
	}

	@Test
	void movingControllerRejectsReverserInBothDirections() {
		assertEquals(MUST_STOP, ModernTrainControllerState.evaluateReverser(true, 1.01E-3, false, false));
		assertEquals(MUST_STOP, ModernTrainControllerState.evaluateReverser(true, -1.01E-3, false, false));
	}

	@Test
	void throttleAndBrakeRejectReverserEvenAtZeroSpeed() {
		assertEquals(MUST_STOP, ModernTrainControllerState.evaluateReverser(true, 0, true, false));
		assertEquals(MUST_STOP, ModernTrainControllerState.evaluateReverser(true, 0, false, true));
		assertEquals(MUST_STOP, ModernTrainControllerState.evaluateReverser(true, 0, true, true));
	}

	@Test
	void scheduledLeversFollowTargetSpeedAndReturnIdle() {
		assertEquals(new ModernTrainControllerState.ScheduledLeverState(false, false),
			ModernTrainControllerState.evaluateScheduledLevers(false, .02, .04));
		assertEquals(new ModernTrainControllerState.ScheduledLeverState(false, true),
			ModernTrainControllerState.evaluateScheduledLevers(true, 0, .04));
		assertEquals(new ModernTrainControllerState.ScheduledLeverState(false, true),
			ModernTrainControllerState.evaluateScheduledLevers(true, -.02, -.04));
		assertEquals(new ModernTrainControllerState.ScheduledLeverState(true, false),
			ModernTrainControllerState.evaluateScheduledLevers(true, .04, .01));
		assertEquals(new ModernTrainControllerState.ScheduledLeverState(true, false),
			ModernTrainControllerState.evaluateScheduledLevers(true, .04, 0));
		assertEquals(new ModernTrainControllerState.ScheduledLeverState(true, false),
			ModernTrainControllerState.evaluateScheduledLevers(true, .04, -.04));
		assertEquals(new ModernTrainControllerState.ScheduledLeverState(false, false),
			ModernTrainControllerState.evaluateScheduledLevers(true, 0, 0));
	}
}
