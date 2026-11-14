package ua.pp.shurgent.railward.clienttest;

import java.util.List;

import com.mojang.logging.LogUtils;
//? if create6 {
/*import net.createmod.ponder.foundation.PonderIndex;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.ui.PonderUI;
*///?} else {
import com.simibubi.create.foundation.ponder.PonderRegistry;
import com.simibubi.create.foundation.ponder.PonderScene;
import com.simibubi.create.foundation.ponder.ui.PonderUI;
//?}
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ua.pp.shurgent.railward.AllBlocks;
import ua.pp.shurgent.railward.Railward;
import ua.pp.shurgent.railward.block.train.signal.RailCrossingSignalBlock;
import ua.pp.shurgent.railward.block.train.signal.RailCrossingSignalBlockEntity;
import ua.pp.shurgent.railward.block.train.signal.RailCrossingSignalBlockEntity.CrossingSign;
import ua.pp.shurgent.railward.block.train.signal.RailCrossingSignalBlockEntity.CrossingSound;
import ua.pp.shurgent.railward.block.train.signal.RailCrossingSignalBlockEntity.Lamp;
import ua.pp.shurgent.railward.block.train.signal.RelayCaseBlock;
import ua.pp.shurgent.railward.client.ponder.RailCrossingSignalScenes;

@Mod.EventBusSubscriber(modid = Railward.MOD_ID, value = Dist.CLIENT)
public final class RailwardCrossingPonderClientProbe {
	private static PonderScene scene;
	private static int ticks;
	private static boolean done;
	private static TickableSoundInstance sound;
	private static Lamp previousLamp = Lamp.OFF;
	private static boolean capturedActive;
	private static boolean capturedSettings;

	@SubscribeEvent
	public static void tick(TickEvent.ClientTickEvent event) {
		if (!enabled() || done || event.phase != TickEvent.Phase.END)
			return;
		require(++ticks < 20 * 180, "timeout");
		Minecraft mc = Minecraft.getInstance();
		if (scene == null) {
			if (mc.level == null || mc.player == null || mc.getOverlay() != null)
				return;
			//? if create6 {
			/*var scenes = PonderIndex.getSceneAccess().compile(AllBlocks.RAIL_CROSSING_SIGNAL.getId());
			*///?} else {
			var scenes = PonderRegistry.compile(AllBlocks.RAIL_CROSSING_SIGNAL.getId());
			//?}
			require(scenes.size() == 1, "expected one crossing storyboard");
			scene = scenes.get(0);
			require(!scene.getTitle().startsWith("railward.ponder."), "untranslated title");
			for (int i = 1; i <= 7; i++)
				require(!scene.getString("text_" + i).startsWith("railward.ponder."), "untranslated caption " + i);
			mc.options.hideGui = true;
			mc.setScreen(new PonderUI(List.of(scene)) {});
			checkSeek();
			scene.begin();
			scene.seekToTime(335);
			return;
		}

		mc.getToasts().clear();
		int time = scene.getCurrentTime();
		Lamp lamp = RailCrossingSignalScenes.lamp(signal());
		if (!capturedActive && time >= 431 && lamp != Lamp.OFF && previousLamp == lamp && sound != null) {
			require(mc.getSoundManager().isActive(sound), "warning never reached the sound engine");
			capture(mc, "warning");
			capturedActive = true;
		}
		previousLamp = lamp;
		if (time >= 565 && sound != null)
			require(sound.isStopped() && !mc.getSoundManager().isActive(sound), "warning channel stayed active after release");
		if (!capturedSettings && time >= 665) {
			require(sound != null, "warning did not play before the crossing cleared");
			capture(mc, "settings");
			capturedSettings = true;
		}
		if (scene.isFinished()) {
			require(capturedActive && capturedSettings, "missing live warning or settings preview");
			require(signal().getSign() == CrossingSign.MULTIPLE_TRACKS, "wrong final crossing sign");
			done = true;
			LogUtils.getLogger().info("Railward CROSSING-PONDER client probe passed");
			mc.stop();
		}
	}

	private static void checkSeek() {
		scene.begin();
		scene.seekToTime(325);
		var world = scene.getWorld();
		require(world.getBlockState(RailCrossingSignalScenes.CABINET).getValue(RelayCaseBlock.FACING) == Direction.NORTH,
			"cabinet was not turned clockwise");
		require(signal().getBlockState().getValue(RailCrossingSignalBlock.FACING) == Direction.NORTH,
			"signal was not turned clockwise");
		require(world.getBlockState(RailCrossingSignalScenes.SIGNAL.below()).is(Blocks.ANDESITE_WALL),
			"signal was not placed on the post");
		require(RailCrossingSignalScenes.lamp(signal()) == Lamp.OFF, "free crossing lit the signal");
		scene.seekToTime(405);
		Lamp first = RailCrossingSignalScenes.lamp(signal());
		require(first != Lamp.OFF && scene.getWorld().getBlockState(RailCrossingSignalScenes.CABINET)
			.getValue(RelayCaseBlock.OCCUPIED), "crossing did not activate");
		scene.seekToTime(415);
		require(RailCrossingSignalScenes.lamp(signal()) != first, "lamps did not alternate after 10 ticks");
		scene.seekToTime(550);
		require(RailCrossingSignalScenes.lamp(signal()) == Lamp.OFF, "crossing stayed active after release");
		scene.seekToTime(810);
		require(signal().getSign() == CrossingSign.NONE, "missing no-sign preview");
		scene.seekToTime(850);
		require(signal().getSign() == CrossingSign.SINGLE_TRACK, "missing single-track preview");
		scene.seekToTime(890);
		require(signal().getSign() == CrossingSign.MULTIPLE_TRACKS, "missing multi-track preview");
		require(sound == null, "seeking emitted warning audio");
	}

	@SubscribeEvent
	public static void onSound(PlaySoundEvent event) {
		if (enabled() && event.getSound() instanceof TickableSoundInstance cue
			&& cue.getLocation().equals(CrossingSound.SMALL_BELL.getEvent().getLocation())) {
			require(sound == null && cue.isLooping(), "expected exactly one looping warning");
			sound = cue;
		}
	}

	private static RailCrossingSignalBlockEntity signal() {
		return (RailCrossingSignalBlockEntity) scene.getWorld().getBlockEntity(RailCrossingSignalScenes.SIGNAL);
	}

	private static void capture(Minecraft mc, String label) {
		Screenshot.grab(mc.gameDirectory, "crossing-ponder-" + label + ".png", mc.getMainRenderTarget(),
			message -> LogUtils.getLogger().info("Railward CROSSING-PONDER screenshot: {}", message.getString()));
	}

	private static boolean enabled() {
		return Boolean.getBoolean("railward.clientProbe") && "ponder_crossing_signal".equals(System.getProperty("railward.clientProbeMode"));
	}

	private static void require(boolean condition, String message) {
		if (!condition)
			throw new IllegalStateException("Railward CROSSING-PONDER: " + message);
	}
}
