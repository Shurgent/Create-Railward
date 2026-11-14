package ua.pp.shurgent.railward.client.ponder;

import com.simibubi.create.AllItems;
import com.simibubi.create.foundation.ponder.PonderPalette;
import com.simibubi.create.foundation.ponder.SceneBuilder;
import com.simibubi.create.foundation.ponder.SceneBuildingUtil;
import com.simibubi.create.foundation.ponder.Selection;
//? if create6 {
/*import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
*///?} else {
import com.simibubi.create.foundation.ponder.element.InputWindowElement;
//?}
import com.simibubi.create.foundation.utility.Pointing;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import ua.pp.shurgent.railward.AllBlocks;

/** Virtual shunting demonstrations; actual train topology remains server-authoritative. */
public final class CouplerScenes {

	public static final BlockPos WAGON_COUPLER = new BlockPos(13, 2, 7);
	public static final BlockPos LOCO_COUPLER = new BlockPos(16, 2, 7);
	public static final BlockPos LOCO_CONTROLS = new BlockPos(21, 3, 8);
	public static final BlockPos SECOND_LOCO_CONTROLS = new BlockPos(19, 3, 6);
	public static final BlockPos WAGON_CONTROLS = new BlockPos(13, 3, 6);
	public static final int[] WAGON_BOGEYS = {5, 11};
	public static final int[] LOCO_BOGEYS = {20, 27};

	private CouplerScenes() {}

	public static void automatic(SceneBuilder scene, SceneBuildingUtil util) {
		scene.title("coupler_automatic", "Automatic train coupling");
		base(scene, util);
		var wagon = scene.world.showIndependentSection(wagonArea(util), Direction.DOWN);
		var locomotive = scene.world.showIndependentSection(locomotiveArea(util), Direction.DOWN);
		text(scene, util, WAGON_COUPLER, PonderPalette.BLUE,
			"Place couplers at the outer ends of the trains, facing each other at the same height.");
		text(scene, util, WAGON_COUPLER, PonderPalette.WHITE,
			"Both trains must be aligned on the same track network, entirely on straight, level track. The receiving train must be stopped.");
		text(scene, util, LOCO_COUPLER, PonderPalette.RED,
			"Remove schedules from both trains before coupling. Automatic coupling must be enabled in the server configuration.");
		caption(scene, util, LOCO_COUPLER, PonderPalette.BLUE,
			"Approach slowly. Coupling will not engage above the configured speed limit.", 160);
		scene.world.moveSection(locomotive, new Vec3(-2, 0, 0), 65);
		wheels(scene, LOCO_BOGEYS, -2, 65);
		scene.idle(70);
		scene.overlay.showOutline(PonderPalette.GREEN, "coupled", util.select.fromTo(WAGON_COUPLER, WAGON_COUPLER.east()), 120);
		scene.idle(100);
		caption(scene, util, WAGON_COUPLER, PonderPalette.GREEN,
			"The couplers join the two trains automatically. The locomotive can now pull the wagon as one train.", 160);
		scene.world.moveSection(locomotive, new Vec3(3, 0, 0), 65);
		scene.world.moveSection(wagon, new Vec3(3, 0, 0), 65);
		wheels(scene, LOCO_BOGEYS, 3, 65);
		wheels(scene, WAGON_BOGEYS, 3, 65);
		scene.idle(175);
		scene.markAsFinished();
	}

	public static void manual(SceneBuilder scene, SceneBuildingUtil util) {
		scene.title("coupler_manual", "Coupling and uncoupling with a wrench");
		base(scene, util);
		var wagon = scene.world.showIndependentSection(wagonArea(util), Direction.DOWN);
		var locomotive = scene.world.showIndependentSection(locomotiveArea(util), Direction.DOWN);
		scene.world.moveSection(locomotive, new Vec3(-2, 0, 0), 0);
		text(scene, util, WAGON_COUPLER, PonderPalette.WHITE,
			"For manual coupling, stop both trains close together with their couplers facing each other. Keep both trains aligned on the same straight, level track network and remove their schedules.");
		controls(scene, WAGON_COUPLER);
		caption(scene, util, WAGON_COUPLER, PonderPalette.GREEN,
			"Right-click either facing coupler with Create's wrench to join the trains. Only one click is needed.", 160);
		scene.idle(30);
		scene.idle(145);
		caption(scene, util, WAGON_COUPLER, PonderPalette.GREEN,
			"The joined carriages move together. The same wrench interaction also separates carriages within a train.", 160);
		scene.world.moveSection(locomotive, new Vec3(2, 0, 0), 45);
		scene.world.moveSection(wagon, new Vec3(2, 0, 0), 45);
		wheels(scene, LOCO_BOGEYS, 2, 45);
		wheels(scene, WAGON_BOGEYS, 2, 45);
		scene.idle(175);
		controls(scene, WAGON_COUPLER.east(2));
		caption(scene, util, WAGON_COUPLER.east(2), PonderPalette.BLUE,
			"Stop the train, then right-click a coupler between the carriages with the wrench to split it at that connection.", 160);
		scene.idle(30);
		scene.idle(145);
		text(scene, util, WAGON_COUPLER.east(2), PonderPalette.WHITE,
			"Uncoupling also works with a paused schedule; an active schedule must be paused first. Coupling requires both schedules to be removed.");
		caption(scene, util, WAGON_COUPLER.east(2), PonderPalette.BLUE,
			"A freshly uncoupled pair will not immediately reconnect automatically. Move the trains apart before approaching again.", 160);
		scene.world.moveSection(locomotive, new Vec3(3, 0, 0), 65);
		wheels(scene, LOCO_BOGEYS, 3, 65);
		scene.idle(175);
		scene.markAsFinished();
	}

	public static void names(SceneBuilder scene, SceneBuildingUtil util) {
		scene.title("coupler_names", "Train naming rules");
		base(scene, util);
		scene.world.showIndependentSection(wagonArea(util), Direction.DOWN);
		var locomotive = scene.world.showIndependentSection(locomotiveArea(util), Direction.DOWN);
		scene.world.moveSection(locomotive, new Vec3(-2, 0, 0), 0);
		labels(scene, "L-01", "W-01");
		text(scene, util, LOCO_CONTROLS.west(2), PonderPalette.BLUE,
			"Give locomotive trains distinct names; leave #001-style names for automatically numbered stock. Naming priority depends on train controls, not on name prefixes.");
		labels(scene, "L-01", "L-01");
		text(scene, util, LOCO_CONTROLS.west(2), PonderPalette.GREEN,
			"Manual coupling keeps the name of the part with train controls when only one part has them: L-01 + W-01 becomes L-01.");
		text(scene, util, WAGON_COUPLER, PonderPalette.BLUE,
			"Automatic coupling always keeps the moving train's name, even if the other train also has controls.");
		scene.world.moveSection(locomotive, new Vec3(.75, 0, 0), 20);
		wheels(scene, LOCO_BOGEYS, .75f, 20);
		labels(scene, "L-01", "#001");
		text(scene, util, WAGON_COUPLER, PonderPalette.GREEN,
			"After splitting, the part with controls keeps the name. A detached part without controls gets a free number, for example #001. Its old name is not restored.");
		scene.world.moveSection(locomotive, new Vec3(-.75, 0, 0), 20);
		wheels(scene, LOCO_BOGEYS, -.75f, 20);
		scene.idle(25);
		scene.world.setBlock(WAGON_CONTROLS, com.simibubi.create.AllBlocks.TRAIN_CONTROLS.getDefaultState()
			.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST), false);
		scene.world.showSection(util.select.position(WAGON_CONTROLS), Direction.DOWN);
		labels(scene, "L-01", "M-02");
		controls(scene, WAGON_COUPLER);
		text(scene, util, WAGON_CONTROLS, PonderPalette.BLUE,
			"If both parts have controls, or neither does, manual coupling keeps the clicked train's name. Click M-02's coupler to keep M-02.");
		scene.world.moveSection(locomotive, new Vec3(.75, 0, 0), 20);
		wheels(scene, LOCO_BOGEYS, .75f, 20);
		labels(scene, "M-02", "#002");
		text(scene, util, LOCO_CONTROLS.west(2), PonderPalette.GREEN,
			"When both separated parts have controls, the first part in the train's carriage order keeps the name; the other gets a free number.");
		scene.world.setBlock(LOCO_CONTROLS, Blocks.AIR.defaultBlockState(), false);
		scene.world.setBlock(SECOND_LOCO_CONTROLS, Blocks.AIR.defaultBlockState(), false);
		scene.world.setBlock(WAGON_CONTROLS, Blocks.AIR.defaultBlockState(), false);
		labels(scene, "#003", "#004");
		text(scene, util, WAGON_COUPLER, PonderPalette.WHITE,
			"If neither separated part has controls, both get free numbers. Numbers such as #001 and #002 are generated automatically, skipping names already in use.");
		scene.markAsFinished();
	}

	public static Selection wagonArea(SceneBuildingUtil util) {
		return util.select.fromTo(3, 2, 6, 13, 4, 8);
	}

	public static Selection locomotiveArea(SceneBuildingUtil util) {
		return util.select.fromTo(16, 1, 5, 31, 6, 9).substract(util.select.fromTo(16, 1, 7, 31, 1, 7));
	}

	private static void base(SceneBuilder scene, SceneBuildingUtil util) {
		scene.configureBasePlate(6, 0, 16);
		scene.scaleSceneView(1f);
		scene.setSceneOffsetY(-2.5f);
		scene.showBasePlate();
		scene.world.showSection(util.select.fromTo(0, 1, 7, 37, 1, 7), Direction.DOWN);
		scene.idle(15);
	}

	private static void labels(SceneBuilder scene, String locomotive, String wagon) {
		scene.overlay.showText(160).text(locomotive).colored(PonderPalette.BLUE)
			.pointAt(new Vec3(18.5, 4.5, 7.5)).placeNearTarget();
		scene.overlay.showText(160).text(wagon).colored(PonderPalette.GREEN)
			.pointAt(new Vec3(12, 8, 7.5)).placeNearTarget();
	}

	private static void controls(SceneBuilder scene, BlockPos position) {
		//? if create6 {
		/*scene.overlay().showControls(Vec3.atCenterOf(position), Pointing.DOWN, 90).rightClick().withItem(AllItems.WRENCH.asStack());
		*///?} else {
		scene.overlay.showControls(new InputWindowElement(Vec3.atCenterOf(position), Pointing.DOWN)
			.rightClick().withItem(AllItems.WRENCH.asStack()), 90);
		//?}
	}

	private static void wheels(SceneBuilder scene, int[] positions, float distance, int duration) {
		for (int x : positions) {
			//? if create6 {
			/*new CreateSceneBuilder(scene).world().animateBogey(new BlockPos(x, 2, 7), -distance, duration);
			*///?} else {
			scene.world.animateBogey(new BlockPos(x, 2, 7), -distance, duration);
			//?}
		}
	}

	private static void caption(SceneBuilder scene, SceneBuildingUtil util, BlockPos position, PonderPalette color,
		String english, int duration) {
		scene.overlay.showText(duration).text(english).colored(color).independent(40).attachKeyFrame();
		scene.overlay.showOutline(color, "coupler_focus", util.select.position(position), duration);
	}

	private static void text(SceneBuilder scene, SceneBuildingUtil util, BlockPos position, PonderPalette color, String english) {
		caption(scene, util, position, color, english, 160);
		scene.idle(175);
	}
}
