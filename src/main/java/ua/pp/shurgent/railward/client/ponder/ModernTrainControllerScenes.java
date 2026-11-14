package ua.pp.shurgent.railward.client.ponder;

import com.simibubi.create.foundation.ponder.PonderPalette;
import com.simibubi.create.foundation.ponder.SceneBuilder;
import com.simibubi.create.foundation.ponder.SceneBuildingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import ua.pp.shurgent.railward.AllBlocks;
import ua.pp.shurgent.railward.block.train.parts.ModernTrainControllerBlock;
import ua.pp.shurgent.railward.client.renderer.LocomotiveBufferLightsRenderHelper.LampState;

/** One fixed cab, two selected directions; no live train or server state is changed. */
public final class ModernTrainControllerScenes {
	public static final BlockPos CONTROLLER = TrainLightingScenes.FRONT_CONTROLS;
	// The controller's FACING points toward its operator; the cart front is beyond the controller.
	public static final Direction FORWARD = Direction.NORTH;
	private ModernTrainControllerScenes() {}

	public static void controls(SceneBuilder scene, SceneBuildingUtil util) {
		TrainLightingScenes.setupCart(scene, util, "modern_train_controller", "The controller and reverser");
		scene.world.setBlock(CONTROLLER, AllBlocks.MODERN_TRAIN_CONTROLLER.getDefaultState()
			.setValue(ModernTrainControllerBlock.FACING, Direction.SOUTH)
			.setValue(ModernTrainControllerBlock.OPEN, true), false);
		scene.world.showSection(util.select.position(CONTROLLER), Direction.DOWN);
		scene.addInstruction(s -> s.addElement(new ControllerLeverElement(CONTROLLER)));
		TrainLightingScenes.bufferPair(scene, util, TrainLightingScenes.BUFFER_FIRST);
		TrainLightingScenes.bufferPair(scene, util, TrainLightingScenes.BUFFER_SECOND);
		scene.world.showSection(util.select.fromTo(TrainLightingScenes.BUFFER_FIRST, TrainLightingScenes.BUFFER_FIRST.east(2)), Direction.DOWN);
		scene.world.showSection(util.select.fromTo(TrainLightingScenes.BUFFER_SECOND, TrainLightingScenes.BUFFER_SECOND.east(2)), Direction.DOWN);
		TrainLightingScenes.text(scene, PonderPalette.WHITE,
			"This controller combines throttle, brake and reverser. One controller can drive a train in either direction.");

		TrainLightingScenes.controls(scene, CONTROLLER, ItemStack.EMPTY);
		state(scene, true, false, false, false);
		direction(scene, FORWARD);
		highlight(scene, PonderPalette.GREEN, 300);
		TrainLightingScenes.orbitText(scene, PonderPalette.GREEN,
			"With the reverser set to forward, Buffer Lights are white in the selected direction and red at the rear.", 180);

		highlight(scene, PonderPalette.BLUE, 190);
		TrainLightingScenes.caption(scene, PonderPalette.BLUE,
			"Before changing direction, stop completely and release both throttle and brake. The reverser cannot switch while either input is held.");
		state(scene, true, false, true, false);
		scene.idle(50);
		state(scene, true, false, false, true);
		scene.idle(50);
		state(scene, true, false, false, false);
		scene.idle(90);

		state(scene, true, true, false, false);
		direction(scene, FORWARD.getOpposite());
		highlight(scene, PonderPalette.GREEN, 300);
		TrainLightingScenes.orbitText(scene, PonderPalette.GREEN,
			"Press the reverser key (R by default) while controlling the train to select reverse travel. The white and red Buffer Lights swap ends.", -180);

		state(scene, true, false, false, false);
		direction(scene, FORWARD);
		highlight(scene, PonderPalette.BLUE, 190);
		TrainLightingScenes.text(scene, PonderPalette.BLUE,
			"Press the reverser key again at a standstill to return to forward travel. Both directions use this same controller.");

		state(scene, false, false, false, false);
		TrainLightingScenes.setPair(scene, TrainLightingScenes.BUFFER_FIRST, LampState.OFF);
		TrainLightingScenes.setPair(scene, TrainLightingScenes.BUFFER_SECOND, LampState.OFF);
		TrainLightingScenes.text(scene, PonderPalette.WHITE,
			"When train control ends, the levers return to neutral. Without active manual or scheduled control, the Buffer Lights switch off.");
		scene.markAsFinished();
	}

	private static void state(SceneBuilder scene, boolean active, boolean reversed, boolean throttle, boolean braking) {
		scene.world.modifyBlock(CONTROLLER, state -> state
			.setValue(ModernTrainControllerBlock.ACTIVE, active)
			.setValue(ModernTrainControllerBlock.REVERSED, reversed)
			.setValue(ModernTrainControllerBlock.FACING, reversed ? Direction.NORTH : Direction.SOUTH)
			.setValue(ModernTrainControllerBlock.THROTTLE, throttle)
			.setValue(ModernTrainControllerBlock.BRAKING, braking), false);
	}

	private static void direction(SceneBuilder scene, Direction selected) {
		TrainLightingScenes.turnDriver(scene, selected);
		TrainLightingScenes.setPair(scene, TrainLightingScenes.BUFFER_FIRST,
			selected == Direction.NORTH ? LampState.WHITE : LampState.RED);
		TrainLightingScenes.setPair(scene, TrainLightingScenes.BUFFER_SECOND,
			selected == Direction.SOUTH ? LampState.WHITE : LampState.RED);
	}

	private static void highlight(SceneBuilder scene, PonderPalette colour, int duration) {
		scene.overlay.chaseBoundingBoxOutline(colour, CONTROLLER,
			new net.minecraft.world.phys.AABB(CONTROLLER), duration);
	}
}
