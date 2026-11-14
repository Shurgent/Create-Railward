package ua.pp.shurgent.railward.client.ponder;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;

import com.simibubi.create.foundation.ponder.PonderPalette;
import com.simibubi.create.foundation.ponder.SceneBuilder;
import com.simibubi.create.foundation.ponder.SceneBuildingUtil;
//? if create6 {
/*import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.element.PonderElementBase;
import net.createmod.ponder.foundation.ui.PonderUI;
*///?} else {
import com.simibubi.create.foundation.ponder.PonderScene;
import com.simibubi.create.foundation.ponder.element.PonderElement;
import com.simibubi.create.foundation.ponder.ui.PonderUI;
//?}
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.sounds.SoundSource;
//? if >=1.19.2
/*import net.minecraft.util.RandomSource;*/
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import ua.pp.shurgent.railward.AllBlocks;
import ua.pp.shurgent.railward.block.train.signal.RailCrossingSignalBlock;
import ua.pp.shurgent.railward.block.train.signal.RailCrossingSignalBlockEntity;
import ua.pp.shurgent.railward.block.train.signal.RailCrossingSignalBlockEntity.CrossingSign;
import ua.pp.shurgent.railward.block.train.signal.RailCrossingSignalBlockEntity.CrossingSound;
import ua.pp.shurgent.railward.block.train.signal.RailCrossingSignalBlockEntity.Lamp;
import ua.pp.shurgent.railward.block.train.signal.RelayCaseBlock;
import ua.pp.shurgent.railward.block.train.signal.RelayCaseBlockEntity;

public final class RailCrossingSignalScenes {

	public static final BlockPos SIGNAL = new BlockPos(5, 2, 2);
	public static final BlockPos CABINET = new BlockPos(3, 1, 6);
	private static final Direction FACING = Direction.NORTH;
	private static final Map<RailCrossingSignalBlockEntity, Flash> FLASHES = new WeakHashMap<>();

	private RailCrossingSignalScenes() {
	}

	public static void crossing(SceneBuilder scene, SceneBuildingUtil util) {
		scene.title("rail_crossing_signal", "Setting up a rail crossing signal");
		SpeedSignScenes.setupStage(scene, util);
		scene.scaleSceneView(.9f);
		scene.setSceneOffsetY(-1.75f);
		var road = util.select.fromTo(4, 0, 0, 4, 0, 8);
		scene.world.setBlocks(road, Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState(), false);
		scene.world.setBlock(CABINET, AllBlocks.RELAY_CASE.getDefaultState()
			.setValue(RelayCaseBlock.FACING, FACING), false);
		scene.world.modifyBlockEntityNBT(util.select.position(CABINET), RelayCaseBlockEntity.class, tag -> {
			tag.put("TargetTrack", NbtUtils.writeBlockPos(new BlockPos(3, 1, 4).subtract(CABINET)));
			tag.putBoolean("TargetDirection", false);
		});
		scene.world.showSection(util.select.position(CABINET), Direction.DOWN);
		scene.idle(15);

		ItemStack signal = AllBlocks.RAIL_CROSSING_SIGNAL.asStack();
		TrainLightingScenes.controls(scene, CABINET, signal);
		scene.overlay.showOutline(PonderPalette.OUTPUT, "crossing_cabinet", util.select.position(CABINET), 210);
		SpeedSignScenes.caption(scene, PonderPalette.WHITE, 135,
			"With the Rail Crossing Signal in hand, right-click the Relay Cabinet that monitors the track section.");
		scene.idle(135);

		TrainLightingScenes.controls(scene, SIGNAL, signal);
		SpeedSignScenes.caption(scene, PonderPalette.GREEN, 135,
			"The selected cabinet is highlighted. Place the signal beside the crossing.");
		scene.idle(25);
		scene.world.setBlock(SIGNAL.below(), Blocks.ANDESITE_WALL.defaultBlockState(), false);
		scene.world.setBlock(SIGNAL, AllBlocks.RAIL_CROSSING_SIGNAL.getDefaultState()
			.setValue(RailCrossingSignalBlock.FACING, FACING), false);
		scene.world.modifyBlockEntityNBT(util.select.position(SIGNAL), RailCrossingSignalBlockEntity.class,
			tag -> tag.putLong("CabinetPos", CABINET.asLong()));
		scene.world.showSection(util.select.fromTo(SIGNAL.below(), SIGNAL), Direction.DOWN);
		scene.idle(110);

		var cart = SpeedSignScenes.cart(scene, util);
		var audio = new CrossingAudio();
		scene.addInstruction(s -> s.addElement(audio));
		SpeedSignScenes.caption(scene, PonderPalette.WHITE, 90,
			"While the track section is free, the signal stays dark and silent.");
		scene.idle(85);
		scene.world.modifyBlock(CABINET, state -> state.setValue(RelayCaseBlock.OCCUPIED, true)
			.setValue(RelayCaseBlock.TRAIN_PRESENT, true), false);
		scene.addInstruction(s -> {
			if (s.getWorld().getBlockEntity(SIGNAL) instanceof RailCrossingSignalBlockEntity be) {
				be.markVirtual();
				FLASHES.put(be, new Flash(new WeakReference<>(s), s.getCurrentTime()));
				audio.play(s);
			}
		});
		SpeedSignScenes.caption(scene, PonderPalette.RED, 140,
			"When the track section is occupied, the red lamps flash alternately and the warning sounds.");
		SpeedSignScenes.move(scene, cart, 5, 140);

		scene.world.modifyBlock(CABINET, state -> state.setValue(RelayCaseBlock.OCCUPIED, false)
			.setValue(RelayCaseBlock.TRAIN_PRESENT, false), false);
		scene.addInstruction(s -> {
			if (s.getWorld().getBlockEntity(SIGNAL) instanceof RailCrossingSignalBlockEntity be)
				FLASHES.remove(be);
			audio.stopPlayback();
		});
		SpeedSignScenes.caption(scene, PonderPalette.GREEN, 100,
			"Once the whole train clears the section, both lamps go out and the warning stops.");
		scene.idle(100);

		Vec3 settings = Vec3.atCenterOf(SIGNAL).add(Vec3.atLowerCornerOf(FACING.getNormal()).scale(2 / 16d));
		scene.overlay.showFilterSlotInput(settings, FACING, 105);
		TrainLightingScenes.controls(scene, SIGNAL, ItemStack.EMPTY);
		SpeedSignScenes.caption(scene, PonderPalette.BLUE, 140,
			"Right-click the white square on the front of the signal to open its settings.");
		scene.idle(140);

		SpeedSignScenes.caption(scene, PonderPalette.WHITE, 175,
			"Choose no sign, a single-track sign, or a multi-track sign. The sound can be set to Small Bell, Bell, or Siren.");
		scene.idle(30);
		previewSign(scene, CrossingSign.NONE);
		scene.idle(40);
		previewSign(scene, CrossingSign.SINGLE_TRACK);
		scene.idle(40);
		previewSign(scene, CrossingSign.MULTIPLE_TRACKS);
		scene.idle(65);
		scene.markAsFinished();
	}

	private static void previewSign(SceneBuilder scene, CrossingSign sign) {
		scene.world.modifyBlockEntity(SIGNAL, RailCrossingSignalBlockEntity.class,
			be -> be.configure(sign, be.getSound()));
	}

	public static Lamp lamp(RailCrossingSignalBlockEntity signal) {
		if (!signal.isVirtual())
			return signal.getLamp();
		Flash flash = FLASHES.get(signal);
		PonderScene scene = flash == null ? null : flash.scene.get();
		if (scene == null)
			return signal.getLamp();
		return Lamp.flashing(scene.getCurrentTime() - flash.start);
	}

	private record Flash(WeakReference<PonderScene> scene, int start) {}

	//? if create6 {
	/*private static final class CrossingAudio extends PonderElementBase {
	*///?} else {
	private static final class CrossingAudio extends PonderElement {
	//?}
		private PreviewSound playing;
		private boolean skipNextTick;
		private boolean skipping;

		@Override
		public void whileSkipping(PonderScene scene) {
			skipNextTick = true;
			stopPlayback();
		}

		@Override
		public void tick(PonderScene scene) {
			skipping = skipNextTick;
			skipNextTick = false;
		}

		@Override
		public void reset(PonderScene scene) {
			stopPlayback();
			skipping = false;
			skipNextTick = false;
		}

		void play(PonderScene scene) {
			if (skipping || !isOpen(scene))
				return;
			stopPlayback();
			playing = new PreviewSound(scene);
			Minecraft.getInstance().getSoundManager().play(playing);
		}

		void stopPlayback() {
			if (playing != null) {
				playing.end();
				playing = null;
			}
		}
	}

	private static boolean isOpen(PonderScene scene) {
		return Minecraft.getInstance().screen instanceof PonderUI ui && ui.getActiveScene() == scene;
	}

	private static final class PreviewSound extends AbstractTickableSoundInstance {
		private final PonderScene scene;

		PreviewSound(PonderScene scene) {
			super(CrossingSound.SMALL_BELL.getEvent(), SoundSource.BLOCKS
				//? if >=1.19.2
				/*, RandomSource.create()*/
			);
			this.scene = scene;
			looping = true;
			relative = true;
			attenuation = Attenuation.NONE;
			volume = .65f;
		}

		@Override
		public void tick() {
			var cabinet = scene.getWorld().getBlockState(CABINET);
			if (!isOpen(scene) || !cabinet.hasProperty(RelayCaseBlock.OCCUPIED) || !cabinet.getValue(RelayCaseBlock.OCCUPIED))
				end();
		}

		void end() {
			stop();
			// Ponder keeps playing while a single-player world and its sound ticks are paused.
			Minecraft.getInstance().getSoundManager().stop(this);
		}
	}
}
