package ua.pp.shurgent.railward.client.ponder;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.contraptions.actors.trainControls.ControlsBlock;
import com.simibubi.create.content.trains.bogey.AbstractBogeyBlock;
import com.simibubi.create.content.trains.track.TrackBlock;
import com.simibubi.create.content.trains.track.TrackShape;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.ponder.PonderPalette;
import com.simibubi.create.foundation.ponder.SceneBuilder;
import com.simibubi.create.foundation.ponder.SceneBuildingUtil;
//? if create6 {
/*import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.WorldSectionElement;
*///?} else {
import com.simibubi.create.foundation.ponder.ElementLink;
import com.simibubi.create.foundation.ponder.element.WorldSectionElement;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import ua.pp.shurgent.railward.block.train.signal.SpeedSignBlock;
import ua.pp.shurgent.railward.block.train.signal.SpeedSignBlockEntity;

public final class SpeedSignScenes {

	public static final BlockPos SIGN = new BlockPos(5, 1, 2);
	public static final BlockPos TARGET = new BlockPos(5, 1, 4);
	public static final BlockPos BOGEY = new BlockPos(2, 2, 4);
	static final int SIGN_ROTATION = 12; // Face west toward the train approaching along +X.
	private static final int EXAMPLE_LIMIT = 20;

	private SpeedSignScenes() {}

	public static void limit(SceneBuilder scene, SceneBuildingUtil util) {
		scene.title("speed_sign", "Setting a train speed limit");
		setupStage(scene, util);

		ItemStack sign = ua.pp.shurgent.railward.AllBlocks.SPEED_SIGN.asStack();
		TrainLightingScenes.controls(scene, TARGET, sign);
		directionArrow(scene, 125);
		caption(scene, PonderPalette.WHITE, 125,
			"Face the intended direction of travel and right-click the rail with the Speed Limit Sign in hand.");
		scene.idle(125);

		ItemStack linkedSign = sign.copy();
		linkedSign.getOrCreateTag().put("SelectedPos", NbtUtils.writeBlockPos(TARGET));
		// Create uses the negative targeting axis when the player looks along +X.
		linkedSign.getOrCreateTag().putBoolean("SelectedDirection", false);
		TrainLightingScenes.controls(scene, SIGN, linkedSign);
		scene.world.setBlock(SIGN, ua.pp.shurgent.railward.AllBlocks.SPEED_SIGN.getDefaultState()
			.setValue(SpeedSignBlock.ROTATION, SIGN_ROTATION), false);
		scene.world.modifyBlockEntityNBT(util.select.position(SIGN), SpeedSignBlockEntity.class, tag -> {
			tag.put("TargetTrack", NbtUtils.writeBlockPos(TARGET.subtract(SIGN)));
			tag.putBoolean("TargetDirection", false);
			tag.putInt("ScrollValue", SpeedSignBlockEntity.DEFAULT_SPEED_KMH);
		});
		scene.world.showSection(util.select.position(SIGN), Direction.DOWN);
		caption(scene, PonderPalette.GREEN, 145,
			"Place the sign beside the selected rail. The track marker shows the direction in which the limit applies.");
		scene.idle(145);

		Vec3 valueBox = Vec3.atLowerCornerOf(SIGN).add(5 / 16d, 13.5 / 16d, .5);
		scene.overlay.showFilterSlotInput(valueBox, Direction.WEST, 85);
		TrainLightingScenes.controls(scene, SIGN, ItemStack.EMPTY);
		caption(scene, PonderPalette.BLUE, 180,
			"Hold right-click on the small panel below the number to set the limit in km/h.");
		scene.idle(35);
		scene.world.modifyBlockEntity(SIGN, SpeedSignBlockEntity.class, be -> be.speedLimit.setValue(EXAMPLE_LIMIT));
		scene.idle(145);

		var cart = cart(scene, util);
		caption(scene, PonderPalette.GREEN, 185,
			"When the front of the train passes the marked point in that direction, the sign sets the train's speed limit.");
		scene.idle(15);
		// The leading wheel point is one block ahead of the small bogey center.
		move(scene, cart, 2, 35);
		move(scene, cart, 2, 100);
		scene.idle(35);

		caption(scene, PonderPalette.BLUE, 225,
			"On the return trip the train keeps its current limit. The last setting stays in effect until the speed limit is changed again.");
		move(scene, cart, -4, 200);
		scene.idle(25);

		TrainLightingScenes.controls(scene, SIGN, AllItems.WRENCH.asStack());
		caption(scene, PonderPalette.WHITE, 145,
			"A wrench turns the sign face. Its direction of action comes from the rail selection.");
		scene.idle(25);
		scene.world.modifyBlock(SIGN, state -> state.cycle(SpeedSignBlock.ROTATION), false);
		scene.idle(120);
		scene.markAsFinished();
	}

	static void setupStage(SceneBuilder scene, SceneBuildingUtil util) {
		scene.configureBasePlate(0, 0, 9);
		scene.scaleSceneView(1f);
		scene.setSceneOffsetY(-2.9f);
		scene.showBasePlate();
		var track = util.select.fromTo(0, 1, 4, 8, 1, 4);
		scene.world.setBlocks(track, AllBlocks.TRACK.getDefaultState().setValue(TrackBlock.SHAPE, TrackShape.XO), false);
		scene.world.showSection(track, Direction.DOWN);
	}

	static ElementLink<WorldSectionElement> cart(SceneBuilder scene, SceneBuildingUtil util) {
		scene.world.setBlock(BOGEY, AllBlocks.SMALL_BOGEY.getDefaultState()
			.setValue(AbstractBogeyBlock.AXIS, Direction.Axis.X), false);
		scene.world.modifyBlockEntity(BOGEY, SmartBlockEntity.class, SmartBlockEntity::markVirtual);
		scene.world.setBlocks(util.select.fromTo(1, 3, 3, 3, 3, 5), AllBlocks.RAILWAY_CASING.getDefaultState(), false);
		scene.world.setBlock(BOGEY.above(2), AllBlocks.SEATS.get(DyeColor.RED).getDefaultState(), false);
		scene.world.setBlock(BOGEY.above(2).east(), AllBlocks.TRAIN_CONTROLS.getDefaultState()
			.setValue(ControlsBlock.FACING, Direction.WEST).setValue(ControlsBlock.VIRTUAL, true), false);
		return scene.world.showIndependentSection(util.select.fromTo(1, 2, 3, 3, 4, 5), Direction.DOWN);
	}

	static void move(SceneBuilder scene, ElementLink<WorldSectionElement> cart, int distance, int ticks) {
		scene.world.moveSection(cart, new Vec3(distance, 0, 0), ticks);
		//? if create6 {
		/*new CreateSceneBuilder(scene).world().animateBogey(BOGEY, -distance, ticks);
		*///?} else {
		scene.world.animateBogey(BOGEY, -distance, ticks);
		//?}
		scene.idle(ticks);
	}

	static void directionArrow(SceneBuilder scene, int ticks) {
		Vec3 center = Vec3.atBottomCenterOf(TARGET).add(0, .3, 0);
		Vec3 tip = center.add(.7, 0, 0);
		scene.overlay.showLine(PonderPalette.GREEN, center.add(-.7, 0, 0), tip, ticks);
		scene.overlay.showLine(PonderPalette.GREEN, center.add(.35, 0, .25), tip, ticks);
		scene.overlay.showLine(PonderPalette.GREEN, center.add(.35, 0, -.25), tip, ticks);
	}

	static void caption(SceneBuilder scene, PonderPalette color, int duration, String text) {
		scene.overlay.showText(duration).text(text).colored(color).independent(40).attachKeyFrame();
	}
}
