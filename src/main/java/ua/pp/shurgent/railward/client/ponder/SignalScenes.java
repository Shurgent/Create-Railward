package ua.pp.shurgent.railward.client.ponder;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.trains.signal.SignalBlockEntity;
import com.simibubi.create.content.trains.signal.SignalBlockEntity.SignalState;
import com.simibubi.create.foundation.ponder.PonderPalette;
import com.simibubi.create.foundation.ponder.SceneBuilder;
import com.simibubi.create.foundation.ponder.SceneBuildingUtil;
//? if !create6 {
import com.simibubi.create.foundation.ponder.element.InputWindowElement;
//?}
import com.simibubi.create.foundation.utility.Pointing;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import ua.pp.shurgent.railward.block.train.signal.ModernSignalBlock;
import ua.pp.shurgent.railward.block.train.signal.ModernSignalBlockEntity;
import ua.pp.shurgent.railward.block.train.signal.ModernSignalGroundBlockEntity;

public final class SignalScenes {

	private static final BlockPos MAIN_SOURCE = new BlockPos(2, 1, 2);
	private static final BlockPos GROUND_SOURCE = new BlockPos(4, 1, 2);
	private static final BlockPos MAIN = MAIN_SOURCE.above();
	private static final BlockPos GROUND = GROUND_SOURCE.above();

	private SignalScenes() {
	}

	public static void placement(SceneBuilder scene, SceneBuildingUtil util) {
		scene.title("signal_placement", "Installing Railward signals");
		base(scene, util);
		controls(scene, new Vec3(3.5, 1.2, 4.5), AllBlocks.TRACK_SIGNAL.asStack());
		text(scene, new Vec3(3.5, 1.2, 4.5), PonderPalette.WHITE,
			"First, use a Create Train Signal on a track to select its position and direction.");
		scene.world.showSection(util.select.position(MAIN_SOURCE), Direction.DOWN);
		aspect(scene, util, MAIN_SOURCE, SignalState.GREEN);
		scene.overlay.showLine(PonderPalette.GREEN, Vec3.atCenterOf(MAIN_SOURCE), new Vec3(2.5, 1.2, 4.5), 140);
		text(scene, Vec3.atCenterOf(MAIN_SOURCE), PonderPalette.GREEN,
			"Place that Create signal beside the track. It controls the railway signal section.");
		scene.world.showSection(util.select.fromTo(MAIN, MAIN.above(3)), Direction.DOWN);
		text(scene, Vec3.atCenterOf(MAIN.above(3)), PonderPalette.WHITE,
			"Place the Railward signal directly above it. The tall signal needs four blocks of free height.");
		text(scene, Vec3.atCenterOf(MAIN_SOURCE), PonderPalette.BLUE,
			"The Railward block searches 1 to 4 blocks straight down from its base for a Create signal. Leave gaps or hide the source below the surface.");
		scene.world.showSection(util.select.position(GROUND_SOURCE), Direction.DOWN);
		aspect(scene, util, GROUND_SOURCE, SignalState.GREEN);
		scene.world.showSection(util.select.position(GROUND), Direction.DOWN);
		text(scene, Vec3.atCenterOf(GROUND), PonderPalette.WHITE,
			"The compact ground signal connects in exactly the same way.");
		controls(scene, Vec3.atCenterOf(MAIN), AllItems.WRENCH.asStack());
		scene.overlay.showText(140).text(
			"Use a wrench on the Railward base to rotate it in 16 steps. Its facing does not change the track direction selected by the Create signal.")
			.colored(PonderPalette.WHITE).pointAt(Vec3.atCenterOf(MAIN)).placeNearTarget().attachKeyFrame();
		for (int step = 0; step < 15; step++) {
			scene.world.modifyBlocks(util.select.fromTo(MAIN, MAIN.above(3)),
				state -> state.cycle(ModernSignalBlock.ROTATION), false);
			scene.idle(6);
		}
		scene.idle(60);
		text(scene, Vec3.atCenterOf(MAIN_SOURCE), PonderPalette.RED,
			"Railward repeats the Create signal's lights. Track links, signal modes and redstone control belong to the Create signal below.");
		scene.markAsFinished();
	}

	public static void mainSignal(SceneBuilder scene, SceneBuildingUtil util) {
		scene.title("signal_aspects", "Reading the tall signal");
		base(scene, util);
		scene.world.showSection(util.select.fromTo(MAIN_SOURCE, MAIN.above(3)), Direction.DOWN);
		aspect(scene, util, MAIN_SOURCE, SignalState.GREEN);
		text(scene, Vec3.atCenterOf(MAIN.above(3)), PonderPalette.GREEN,
			"Green repeats Create's clear signal: the protected section is available.");
		aspect(scene, util, MAIN_SOURCE, SignalState.RED);
		text(scene, Vec3.atCenterOf(MAIN.above(2)), PonderPalette.RED,
			"Red repeats Create's stop signal: the section is occupied, reserved, or blocked by redstone.");
		aspect(scene, util, MAIN_SOURCE, SignalState.YELLOW);
		text(scene, Vec3.atCenterOf(MAIN.above(3)), PonderPalette.WHITE,
			"Yellow repeats Create's chain-signal warning: only some routes ahead are available. It is not an automatic speed limit.");
		aspect(scene, util, MAIN_SOURCE, SignalState.INVALID);
		text(scene, Vec3.atCenterOf(MAIN.above(2)), PonderPalette.RED,
			"A brief flashing red light means the Create signal is invalid. Check its track connection.");
		disconnect(scene, MAIN_SOURCE);
		text(scene, Vec3.atCenterOf(MAIN), PonderPalette.WHITE,
			"No lights? Check for a Create signal directly below the Railward base, no more than four blocks away.");
		text(scene, new Vec3(3.5, 1.2, 4.5), PonderPalette.BLUE,
			"Create signals guide scheduled trains. When driving manually, watch and obey the lights yourself.");
		scene.markAsFinished();
	}

	public static void groundSignal(SceneBuilder scene, SceneBuildingUtil util) {
		scene.title("ground_signal_aspects", "Reading the ground signal");
		base(scene, util);
		scene.world.showSection(util.select.fromTo(GROUND_SOURCE, GROUND), Direction.DOWN);
		aspect(scene, util, GROUND_SOURCE, SignalState.RED);
		text(scene, Vec3.atCenterOf(GROUND), PonderPalette.BLUE,
			"Blue is the ground signal's stop light. It repeats a red Create signal.");
		aspect(scene, util, GROUND_SOURCE, SignalState.GREEN);
		text(scene, Vec3.atCenterOf(GROUND), PonderPalette.WHITE,
			"White repeats a green Create signal: the protected section is available.");
		aspect(scene, util, GROUND_SOURCE, SignalState.YELLOW);
		text(scene, Vec3.atCenterOf(GROUND_SOURCE), PonderPalette.WHITE,
			"A yellow Create signal also produces white here. This two-light head does not distinguish green from yellow.");
		aspect(scene, util, GROUND_SOURCE, SignalState.INVALID);
		text(scene, Vec3.atCenterOf(GROUND), PonderPalette.BLUE,
			"Brief blue flashes mean the Create signal is invalid. Check its track connection.");
		disconnect(scene, GROUND_SOURCE);
		text(scene, Vec3.atCenterOf(GROUND), PonderPalette.WHITE,
			"Without a Create signal 1 to 4 blocks directly below, both lights stay off.");
		text(scene, new Vec3(3.5, 1.2, 4.5), PonderPalette.RED,
			"The compact head changes the appearance only. Create still controls train routing and signal sections.");
		scene.markAsFinished();
	}

	private static void base(SceneBuilder scene, SceneBuildingUtil util) {
		scene.configureBasePlate(0, 0, 7);
		scene.scaleSceneView(.85f);
		scene.setSceneOffsetY(-.5f);
		// Look under the lamp hoods, rather than down onto their opaque tops.
		scene.addInstruction(s -> s.getTransform().xRotation.startWithValue(-15));
		scene.showBasePlate();
		scene.world.showSection(util.select.fromTo(0, 1, 4, 6, 1, 4), Direction.DOWN);
		scene.idle(15);
	}

	private static void aspect(SceneBuilder scene, SceneBuildingUtil util, BlockPos source, SignalState state) {
		// Set the initial aspect even before rendering initializes the section's BE tickers.
		scene.world.modifyBlockEntityNBT(util.select.position(source), SignalBlockEntity.class,
			tag -> tag.putString("State", state.name()));
		scene.world.modifyBlockEntity(source.above(), ModernSignalBlockEntity.class, be -> be.signalState = state);
		scene.world.modifyBlockEntity(source.above(), ModernSignalGroundBlockEntity.class, be -> be.signalState = state);
		scene.idle(15);
	}

	private static void disconnect(SceneBuilder scene, BlockPos source) {
		// SchematicWorld drops the source from its map without setting its removed flag.
		// Invalidate it first so a rendered repeater discards its cached source on the next tick.
		scene.world.modifyBlockEntity(source, SignalBlockEntity.class, SignalBlockEntity::setRemoved);
		scene.world.setBlock(source, Blocks.AIR.defaultBlockState(), false);
		scene.world.modifyBlockEntity(source.above(), ModernSignalBlockEntity.class, be -> be.signalState = null);
		scene.world.modifyBlockEntity(source.above(), ModernSignalGroundBlockEntity.class, be -> be.signalState = null);
		scene.idle(15);
	}

	private static void text(SceneBuilder scene, Vec3 target, PonderPalette color, String english) {
		scene.overlay.showText(140).text(english).colored(color).pointAt(target).placeNearTarget().attachKeyFrame();
		scene.idle(150);
	}

	private static void controls(SceneBuilder scene, Vec3 target, ItemStack item) {
		//? if create6 {
		/*scene.overlay().showControls(target, Pointing.DOWN, 100).rightClick().withItem(item);
		*///?} else {
		scene.overlay.showControls(new InputWindowElement(target, Pointing.DOWN).rightClick().withItem(item), 100);
		//?}
	}
}
