package ua.pp.shurgent.railward.client.ponder;

import com.simibubi.create.content.fluids.pump.PumpBlock;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.simibubi.create.foundation.ponder.PonderPalette;
import com.simibubi.create.foundation.ponder.SceneBuilder;
import com.simibubi.create.foundation.ponder.SceneBuildingUtil;
import com.simibubi.create.foundation.ponder.Selection;
//? if create6
/*import com.simibubi.create.foundation.ponder.CreateSceneBuilder;*/

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import ua.pp.shurgent.railward.block.train.station.FluidTerminalBlockEntity;

/** The hatch/terminal pair, illustrated with one locomotive-led consist. */
public final class FluidTransferScenes {

	public static final BlockPos HATCH = new BlockPos(8, 6, 7);
	public static final BlockPos TERMINAL = HATCH.above(2);
	public static final BlockPos CISTERN = new BlockPos(4, 3, 6);
	public static final BlockPos RESERVOIR = new BlockPos(1, 1, 11);
	public static final BlockPos PUMP = new BlockPos(3, 3, 11);
	public static final BlockPos LEVER = new BlockPos(7, 9, 7);
	public static final BlockPos COMPARATOR = new BlockPos(9, 8, 7);
	public static final int[] BOGEY_X = {5, 11, 18, 25};

	private FluidTransferScenes() {}

	public static void hatch(SceneBuilder scene, SceneBuildingUtil util) {
		scene.title("cistern_hatch", "The hatch and terminal pair");
		base(scene, util);
		var train = scene.world.showIndependentSection(trainArea(util), Direction.DOWN);
		scene.world.moveSection(train, new Vec3(-4, 0, 0), 0);
		scene.world.showSection(util.select.position(TERMINAL), Direction.DOWN);
		scene.overlay.showOutline(PonderPalette.GREEN, "pair_hatch", util.select.position(HATCH.west(4)), 140);
		text(scene, util, Vec3.atCenterOf(TERMINAL), PonderPalette.GREEN,
			"The Cistern Hatch on the train and the stationary Fluid Terminal work as a pair, like Create's Portable Fluid Interface.");
		scene.world.moveSection(train, new Vec3(4, 0, 0), 45);
		wheels(scene, 4, 45);
		reach(scene, util);
		text(scene, util, Vec3.atCenterOf(TERMINAL), PonderPalette.BLUE,
			"Place the terminal directly above the hatch, at the same X and Z. Its block must be 1 or 2 blocks higher: zero or one empty block between them.");
		connection(scene, util, true, false);
		scene.idle(15);
		text(scene, util, Vec3.atCenterOf(HATCH), PonderPalette.GREEN,
			"On connection, the hose extends to the hatch and its lid opens automatically.");
		text(scene, util, Vec3.atCenterOf(CISTERN.above()), PonderPalette.WHITE,
			"A hatch accesses the fluid storage of its own carriage. Tanks on other carriages need their own hatch.");
		connection(scene, util, false, false);
		scene.idle(15);
		scene.world.moveSection(train, new Vec3(6, 0, 0), 60);
		wheels(scene, 6, 60);
		text(scene, util, Vec3.atCenterOf(HATCH).add(6, 0, 0), PonderPalette.WHITE,
			"As the pair separates, the hose retracts and the hatch lid closes.");
		scene.markAsFinished();
	}

	public static void terminal(SceneBuilder scene, SceneBuildingUtil util) {
		scene.title("fluid_terminal", "Fluid exchange through the pair");
		base(scene, util);
		var station = util.select.fromTo(0, 1, 0, 9, 9, 11).substract(trainArea(util))
			.substract(util.select.position(LEVER)).substract(util.select.position(COMPARATOR));
		scene.world.showSection(station, Direction.DOWN);
		var train = scene.world.showIndependentSection(trainArea(util), Direction.DOWN);
		scene.world.moveSection(train, new Vec3(-4, 0, 0), 0);
		text(scene, util, Vec3.atCenterOf(TERMINAL), PonderPalette.WHITE,
			"The Fluid Terminal and Cistern Hatch form a fluid-transfer pair, analogous to Create's Portable Fluid Interface.");
		scene.world.moveSection(train, new Vec3(4, 0, 0), 45);
		wheels(scene, 4, 45);
		reach(scene, util);
		text(scene, util, Vec3.atCenterOf(TERMINAL), PonderPalette.BLUE,
			"The terminal belongs directly above the hatch, 1 or 2 blocks higher. This leaves zero or one empty block between their blocks.");
		connection(scene, util, true, false);
		scene.idle(15);
		drive(scene, util, 64);
		caption(scene, util, Vec3.atCenterOf(HATCH), PonderPalette.GREEN,
			"One connection handles both loading and unloading. The hose and hatch lid automatically follow the connection state.", 180);
		for (int step = 0; step < 8; step++) {
			transfer(scene, RESERVOIR, CISTERN, 1000);
			scene.idle(8);
		}
		scene.idle(24);
		scene.world.modifyBlock(PUMP, state -> state.setValue(PumpBlock.FACING, Direction.WEST), false);
		for (int step = 0; step < 8; step++) {
			transfer(scene, CISTERN, RESERVOIR, 1000);
			scene.idle(8);
		}
		scene.idle(38);
		drive(scene, util, 0);
		scene.world.showSection(util.select.position(COMPARATOR), Direction.DOWN);
		text(scene, util, Vec3.atCenterOf(COMPARATOR), PonderPalette.GREEN,
			"The comparator reports the connection: 15 when connected and 0 when disconnected, regardless of how full the cistern is.");
		scene.world.showSection(util.select.position(LEVER), Direction.DOWN);
		scene.world.modifyBlock(LEVER, state -> state.setValue(BlockStateProperties.POWERED, true), false);
		connection(scene, util, false, true);
		caption(scene, util, Vec3.atCenterOf(LEVER), PonderPalette.RED,
			"Redstone power disables the terminal. Removing the signal allows the pair to connect again.", 140);
		scene.idle(80);
		scene.world.modifyBlock(LEVER, state -> state.setValue(BlockStateProperties.POWERED, false), false);
		connection(scene, util, true, false);
		scene.idle(70);
		connection(scene, util, false, false);
		scene.idle(15);
		scene.world.moveSection(train, new Vec3(6, 0, 0), 60);
		wheels(scene, 6, 60);
		scene.idle(65);
		scene.markAsFinished();
	}

	private static Selection trainArea(SceneBuildingUtil util) {
		// One section contains both vehicles; only the stationary rails are excluded.
		return util.select.fromTo(3, 2, 6, 13, 6, 8)
			.add(util.select.fromTo(14, 1, 5, 29, 6, 9)
				.substract(util.select.fromTo(14, 1, 7, 29, 1, 7)));
	}

	private static void base(SceneBuilder scene, SceneBuildingUtil util) {
		// Frame the hatch/terminal pair; the decorative locomotive may leave the view.
		scene.configureBasePlate(0, 0, 16);
		scene.scaleSceneView(.8f);
		scene.setSceneOffsetY(-6);
		scene.world.modifyBlockEntity(TERMINAL, FluidTerminalBlockEntity.class, FluidTerminalBlockEntity::tick);
		scene.showBasePlate();
		scene.world.showSection(util.select.fromTo(0, 1, 7, 37, 1, 7), Direction.DOWN);
		scene.idle(15);
	}

	private static void reach(SceneBuilder scene, SceneBuildingUtil util) {
		scene.overlay.showOutline(PonderPalette.BLUE, "reach", util.select.fromTo(HATCH.above(), TERMINAL), 140);
	}

	private static void connection(SceneBuilder scene, SceneBuildingUtil util, boolean connected, boolean powered) {
		scene.world.modifyBlockEntityNBT(util.select.position(TERMINAL), FluidTerminalBlockEntity.class, tag -> {
			tag.putInt("Timer", connected ? 14 : 0);
			tag.putFloat("Distance", 2);
			tag.putBoolean("Powered", powered);
		});
		scene.world.modifyBlock(COMPARATOR, state -> state.setValue(BlockStateProperties.POWERED, connected), false);
	}

	private static void drive(SceneBuilder scene, SceneBuildingUtil util, float speed) {
		//? if create6 {
		/*var world = new CreateSceneBuilder(scene).world();
		*///?} else {
		var world = scene.world;
		//?}
		world.setKineticSpeed(util.select.position(PUMP), speed);
		world.setKineticSpeed(util.select.fromTo(2, 3, 10, 3, 3, 10), -speed);
	}

	private static void transfer(SceneBuilder scene, BlockPos from, BlockPos to, int amount) {
		scene.addInstruction(s -> {
			var source = (FluidTankBlockEntity) s.getWorld().getBlockEntity(from);
			var target = (FluidTankBlockEntity) s.getWorld().getBlockEntity(to);
			var available = source.getTankInventory().drain(amount, FluidAction.SIMULATE);
			int accepted = target.getTankInventory().fill(available, FluidAction.SIMULATE);
			if (accepted > 0)
				target.getTankInventory().fill(source.getTankInventory().drain(accepted, FluidAction.EXECUTE), FluidAction.EXECUTE);
		});
	}

	private static void wheels(SceneBuilder scene, float distance, int duration) {
		for (int x : BOGEY_X) {
			//? if create6 {
			/*new CreateSceneBuilder(scene).world().animateBogey(new BlockPos(x, 2, 7), distance, duration);
			*///?} else {
			scene.world.animateBogey(new BlockPos(x, 2, 7), distance, duration);
			//?}
		}
	}

	private static void caption(SceneBuilder scene, SceneBuildingUtil util, Vec3 target, PonderPalette color,
		String english, int duration) {
		scene.overlay.showText(duration).text(english).colored(color).independent(40).attachKeyFrame();
		BlockPos focus = new BlockPos((int) Math.floor(target.x), (int) Math.floor(target.y), (int) Math.floor(target.z));
		scene.overlay.showOutline(color, "fluid_focus", util.select.position(focus), duration);
	}

	private static void text(SceneBuilder scene, SceneBuildingUtil util, Vec3 target, PonderPalette color, String english) {
		caption(scene, util, target, color, english, 140);
		scene.idle(150);
	}
}
