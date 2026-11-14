package ua.pp.shurgent.railward.client.ponder;

import com.simibubi.create.foundation.ponder.PonderPalette;
import com.simibubi.create.foundation.ponder.SceneBuilder;
import com.simibubi.create.foundation.ponder.SceneBuildingUtil;
import com.simibubi.create.foundation.ponder.Selection;
//? if !create6 {
import com.simibubi.create.foundation.ponder.element.InputWindowElement;
//?}
import com.simibubi.create.foundation.utility.Pointing;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import ua.pp.shurgent.railward.AllBlocks;
import ua.pp.shurgent.railward.block.train.parts.CisternBlock;
import ua.pp.shurgent.railward.block.train.parts.CisternBlockEntity;

/** Uses the real cistern renderer; server-side multiblock formation is staged for playback. */
public final class CisternScenes {

	public static final BlockPos SMALL = new BlockPos(4, 1, 5);
	public static final BlockPos LARGE = new BlockPos(4, 1, 4);
	public static final BlockPos THIN = new BlockPos(1, 1, 6);
	private CisternScenes() {}

	public static void building(SceneBuilder scene, SceneBuildingUtil util) {
		setup(scene, "cistern_building", "Building a cistern");
		form(scene, util, SMALL, 1, 5, Direction.Axis.X, AllBlocks.CISTERN.get());
		scene.world.showSection(area(util, SMALL, 1, 5, Direction.Axis.X), Direction.DOWN);
		text(scene, PonderPalette.GREEN,
			"Matching Cistern blocks join horizontally. Their axes must point in the same direction.");
		clear(scene, util);

		// Reveal an entire solid cross-section before its controller draws the combined mesh.
		BlockPos first = SMALL.east(4);
		for (int y = 0; y < 3; y++) for (int z = 0; z < 3; z++) {
			BlockPos p = first.offset(0, y, z);
			form(scene, util, p, 1, 1, Direction.Axis.X, AllBlocks.CISTERN.get());
			scene.world.showSection(util.select.position(p), Direction.DOWN);
			scene.idle(6);
		}
		scene.idle(20);
		form(scene, util, first, 3, 1, Direction.Axis.X, AllBlocks.CISTERN.get());
		text(scene, PonderPalette.GREEN,
			"For a wider cistern, fill a complete 3 by 3 cross-section, including its centre. A hollow shell will not combine.");
		caption(scene, PonderPalette.BLUE,
			"Right-click either end with matching Cistern blocks to add a whole cross-section. A 3 by 3 section needs 9 blocks and free space.", 240);
		controls(scene, Vec3.atLowerCornerOf(first).add(0, 1.5, 1.5), AllBlocks.CISTERN.asStack(), 9);
		for (int length = 2; length <= 5; length++) {
			BlockPos start = first.west(length - 1);
			form(scene, util, start, 3, length, Direction.Axis.X, AllBlocks.CISTERN.get());
			scene.world.showSection(area(util, start, 3, 1, Direction.Axis.X), Direction.DOWN);
			scene.idle(15);
		}
		scene.idle(190);
		clear(scene, util);

		form(scene, util, LARGE.east(), 5, 3, Direction.Axis.X, AllBlocks.CISTERN_LIGHT_GRAY.get());
		scene.world.showSection(area(util, LARGE.east(), 5, 3, Direction.Axis.X), Direction.DOWN);
		caption(scene, PonderPalette.GREEN,
			"The largest diameter is 5 blocks: fill a solid 5 by 5 cross-section. Adding another section consumes 25 matching blocks.", 210);
		controls(scene, Vec3.atLowerCornerOf(LARGE.east()).add(0, 2.5, 2.5), AllBlocks.CISTERN_LIGHT_GRAY.asStack(), 25);
		scene.idle(25);
		form(scene, util, LARGE, 5, 4, Direction.Axis.X, AllBlocks.CISTERN_LIGHT_GRAY.get());
		scene.world.showSection(area(util, LARGE, 5, 1, Direction.Axis.X), Direction.DOWN);
		scene.idle(190);
		clear(scene, util);

		form(scene, util, THIN, 1, 11, Direction.Axis.X, AllBlocks.CISTERN_YELLOW.get());
		scene.world.showSection(area(util, THIN, 1, 11, Direction.Axis.X), Direction.DOWN);
		scene.overlay.showOutline(PonderPalette.BLUE, "length", area(util, THIN, 1, 11, Direction.Axis.X), 180);
		text(scene, PonderPalette.BLUE,
			"All three diameters, 1, 3 and 5 blocks, support lengths from 1 to 11 blocks. The maximum size is 5 by 5 by 11.");
		clear(scene, util);

		BlockPos green = new BlockPos(3, 1, 5), blue = green.east(3);
		form(scene, util, green, 3, 3, Direction.Axis.X, AllBlocks.CISTERN.get());
		form(scene, util, blue, 3, 3, Direction.Axis.X, AllBlocks.CISTERN_BLUE.get());
		scene.world.showSection(area(util, green, 3, 6, Direction.Axis.X), Direction.DOWN);
		scene.overlay.showOutline(PonderPalette.GREEN, "green", area(util, green, 3, 3, Direction.Axis.X), 180);
		scene.overlay.showOutline(PonderPalette.BLUE, "blue", area(util, blue, 3, 3, Direction.Axis.X), 180);
		text(scene, PonderPalette.BLUE,
			"Different colours stay separate even when they touch. Each cistern keeps its own fluid storage.");
		clear(scene, util);

		BlockPos turned = new BlockPos(5, 1, 4);
		form(scene, util, turned, 3, 5, Direction.Axis.Z, AllBlocks.CISTERN_ORANGE.get());
		scene.world.showSection(area(util, turned, 3, 5, Direction.Axis.Z), Direction.DOWN);
		text(scene, PonderPalette.WHITE,
			"Cisterns can extend along either horizontal axis. Face along the desired axis when placing their blocks.");
		scene.markAsFinished();
	}

	public static void connections(SceneBuilder scene, SceneBuildingUtil util) {
		setup(scene, "cistern_connections", "Connecting pipes to a cistern");
		form(scene, util, SMALL, 3, 5, Direction.Axis.X, AllBlocks.CISTERN.get());
		scene.world.showSection(area(util, SMALL, 3, 5, Direction.Axis.X), Direction.DOWN);
		pipe(scene, util, SMALL.offset(-1, 1, 1), Direction.Axis.X);
		pipe(scene, util, SMALL.offset(5, 1, 1), Direction.Axis.X);
		scene.overlay.showOutline(PonderPalette.GREEN, "end", util.select.position(SMALL.offset(0, 1, 1)), 180);
		text(scene, PonderPalette.GREEN,
			"On a 3-block-wide cistern, connect pipes to the centre block of either end.");
		pipe(scene, util, SMALL.offset(2, 1, -1), Direction.Axis.Z);
		pipe(scene, util, SMALL.offset(2, 3, 1), Direction.Axis.Y);
		scene.overlay.showOutline(PonderPalette.BLUE, "side", util.select.fromTo(SMALL.offset(1, 1, 0), SMALL.offset(3, 1, 0)), 200);
		scene.overlay.showOutline(PonderPalette.BLUE, "top", util.select.fromTo(SMALL.offset(1, 2, 1), SMALL.offset(3, 2, 1)), 200);
		text(scene, PonderPalette.BLUE,
			"Side connections use the middle row; top and bottom connections use the centre line. These connections must be between the two end sections.");
		clear(scene, util);

		form(scene, util, LARGE, 5, 5, Direction.Axis.X, AllBlocks.CISTERN_LIGHT_GRAY.get());
		scene.world.showSection(area(util, LARGE, 5, 5, Direction.Axis.X), Direction.DOWN);
		pipe(scene, util, LARGE.offset(-1, 2, 2), Direction.Axis.X);
		scene.overlay.showOutline(PonderPalette.GREEN, "large_end", util.select.fromTo(LARGE.offset(0, 1, 1), LARGE.offset(0, 3, 3)), 220);
		text(scene, PonderPalette.GREEN,
			"A 5-block-wide cistern accepts end connections anywhere in the central 3 by 3 area. Its side, top and bottom connections follow the same centre-line rule.");
		clear(scene, util);

		BlockPos thin = new BlockPos(4, 1, 6);
		form(scene, util, thin, 1, 5, Direction.Axis.X, AllBlocks.CISTERN_YELLOW.get());
		scene.world.showSection(area(util, thin, 1, 5, Direction.Axis.X), Direction.DOWN);
		pipe(scene, util, thin.west(), Direction.Axis.X);
		pipe(scene, util, thin.east(5), Direction.Axis.X);
		text(scene, PonderPalette.WHITE,
			"A 1-block-wide cistern accepts pipes only at its two ends.");
		scene.markAsFinished();
	}

	private static void setup(SceneBuilder scene, String id, String title) {
		scene.title(id, title);
		scene.configureBasePlate(0, 0, 13);
		scene.scaleSceneView(.8f);
		scene.setSceneOffsetY(-1);
		scene.showBasePlate();
	}

	private static Selection area(SceneBuildingUtil util, BlockPos p, int width, int length, Direction.Axis axis) {
		return util.select.fromTo(p, p.offset(axis == Direction.Axis.X ? length - 1 : width - 1,
			width - 1, axis == Direction.Axis.Z ? length - 1 : width - 1));
	}

	private static void form(SceneBuilder scene, SceneBuildingUtil util, BlockPos p, int width, int length,
		Direction.Axis axis, CisternBlock block) {
		Selection cells = area(util, p, width, length, axis);
		scene.world.setBlocks(cells, block.defaultBlockState().setValue(CisternBlock.AXIS, axis), false);
		scene.world.modifyBlockEntityNBT(cells, CisternBlockEntity.class, tag -> {
			tag.put("Controller", NbtUtils.writeBlockPos(p));
			tag.remove("Uninitialized");
		});
		scene.world.modifyBlockEntityNBT(util.select.position(p), CisternBlockEntity.class, tag -> {
			tag.remove("Controller");
			tag.putInt("Size", width);
			tag.putInt("Height", length);
			tag.putBoolean("Window", false);
		});
	}

	private static void clear(SceneBuilder scene, SceneBuildingUtil util) {
		Selection cells = util.select.fromTo(0, 1, 0, 12, 6, 12);
		scene.world.hideSection(cells, Direction.UP);
		scene.idle(25);
		scene.world.setBlocks(cells, Blocks.AIR.defaultBlockState(), false);
	}

	private static void pipe(SceneBuilder scene, SceneBuildingUtil util, BlockPos p, Direction.Axis axis) {
		var state = com.simibubi.create.AllBlocks.FLUID_PIPE.getDefaultState();
		for (Direction face : Direction.values())
			state = state.setValue(PipeBlock.PROPERTY_BY_DIRECTION.get(face), face.getAxis() == axis);
		scene.world.setBlock(p, state, false);
		scene.world.showSection(util.select.position(p), Direction.DOWN);
	}

	private static void controls(SceneBuilder scene, Vec3 target, ItemStack stack, int count) {
		stack.setCount(count);
		//? if create6 {
		/*scene.overlay().showControls(target, Pointing.DOWN, 70).rightClick().withItem(stack);
		*///?} else {
		scene.overlay.showControls(new InputWindowElement(target, Pointing.DOWN).rightClick().withItem(stack), 70);
		//?}
	}

	private static void text(SceneBuilder scene, PonderPalette colour, String text) {
		caption(scene, colour, text, 180);
		scene.idle(190);
	}

	private static void caption(SceneBuilder scene, PonderPalette colour, String text, int duration) {
		scene.overlay.showText(duration).text(text).colored(colour).independent(40).attachKeyFrame();
	}
}
