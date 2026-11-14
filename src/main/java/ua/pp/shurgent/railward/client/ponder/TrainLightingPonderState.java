package ua.pp.shurgent.railward.client.ponder;

import java.util.Map;
import java.util.WeakHashMap;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
//? if create6 {
/*import net.createmod.ponder.api.level.PonderLevel;
*///?} else {
import com.simibubi.create.foundation.ponder.PonderWorld;
//?}
import ua.pp.shurgent.railward.client.renderer.LocomotiveBufferLightsRenderHelper.LampState;

/** Scripted lighting belongs only to virtual Ponder block entities, never to a live train. */
public final class TrainLightingPonderState {

	private static final Map<SmartBlockEntity, LampState> STATES = new WeakHashMap<>();
	private TrainLightingPonderState() {}

	public static void set(SmartBlockEntity be, LampState state) {
		if (isPonderLevel(be)) {
			// Ponder 1.0.91 does not mark dynamically created BEs virtual in its lowercase add hook.
			be.markVirtual();
			STATES.put(be, state);
		}
	}

	public static LampState get(SmartBlockEntity be) {
		return be.isVirtual() && isPonderLevel(be) ? STATES.get(be) : null;
	}

	private static boolean isPonderLevel(SmartBlockEntity be) {
		//? if create6 {
		/*return be.getLevel() instanceof PonderLevel;
		*///?} else {
		return be.getLevel() instanceof PonderWorld;
		//?}
	}
}
