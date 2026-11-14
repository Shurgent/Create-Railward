package ua.pp.shurgent.railward.clienttest;

import java.util.List;

import com.mojang.logging.LogUtils;
//? if create6 {
/*import net.createmod.ponder.api.element.WorldSectionElement;
import net.createmod.ponder.foundation.PonderIndex;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.ui.PonderUI;
*///?} else {
import com.simibubi.create.foundation.ponder.PonderRegistry;
import com.simibubi.create.foundation.ponder.PonderScene;
import com.simibubi.create.foundation.ponder.element.WorldSectionElement;
import com.simibubi.create.foundation.ponder.ui.PonderUI;
//?}
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ua.pp.shurgent.railward.AllBlocks;
import ua.pp.shurgent.railward.Railward;
import ua.pp.shurgent.railward.block.train.signal.SpeedSignBlock;
import ua.pp.shurgent.railward.block.train.signal.SpeedSignBlockEntity;
import ua.pp.shurgent.railward.block.train.signal.WhistleSignBlock;
import ua.pp.shurgent.railward.block.train.signal.WhistleSignBlockEntity;
import ua.pp.shurgent.railward.client.ponder.SpeedSignScenes;

@Mod.EventBusSubscriber(modid = Railward.MOD_ID, value = Dist.CLIENT)
public final class RailwardSpeedSignPonderClientProbe {
	private static final int[] WHISTLE_TIMES = {20, 160, 255, 418, 548, 700};
	private static final int[] SPEED_TIMES = {20, 160, 275, 355, 466, 600, 675, 920};
	private static PonderScene scene;
	private static int ticks, preview, frame;
	private static boolean done;
	private static boolean whistle;
	private static int whistles;

	@SubscribeEvent
	public static void tick(TickEvent.ClientTickEvent event) {
		if (done || event.phase != TickEvent.Phase.END || !Boolean.getBoolean("railward.clientProbe")) return;
		String mode = System.getProperty("railward.clientProbeMode");
		whistle = "ponder_whistle_sign".equals(mode);
		if (!whistle && !"ponder_speed_sign".equals(mode)) return;
		Minecraft mc = Minecraft.getInstance();
		require(++ticks < 20 * 180, "timeout");
		if (mc.level == null && mc.screen instanceof DisconnectedScreen) {
			LogUtils.getLogger().error("Railward track sign Ponder disconnected before playback");
			done = true;
			mc.stop();
			return;
		}
		if (scene == null) {
			if (mc.level == null || mc.player == null || mc.getOverlay() != null) return;
			scene = compile(AllBlocks.SPEED_SIGN.getId());
			checkPlayback();
			if (whistle) {
				scene = compile(AllBlocks.WHISTLE_SIGN.getId());
				checkWhistlePlayback();
			}
			mc.options.hideGui = true;
			open(mc);
		}
		mc.getToasts().clear();
		if (++frame == 20)
			Screenshot.grab(mc.gameDirectory, (whistle ? "whistle-sign-ponder-" : "speed-sign-ponder-") + preview + ".png", mc.getMainRenderTarget(),
				message -> LogUtils.getLogger().info("Railward SPEED-SIGN-PONDER screenshot: {}", message.getString()));
		if (frame < 36) return;
		if (++preview < (whistle ? WHISTLE_TIMES : SPEED_TIMES).length) {
			open(mc);
			return;
		}
		done = true;
		if (whistle) {
			require(whistles == 1, "expected exactly one audible forward crossing across the live previews");
			LogUtils.getLogger().info("Railward WHISTLE-SIGN-PONDER client probe passed with one forward whistle, silent seek and return, wrench rotation, six previews and speed sign playback");
		} else {
			LogUtils.getLogger().info("Railward SPEED-SIGN-PONDER client probe passed with native target and limit, slower motion, retained limit and eight previews");
		}
		mc.stop();
	}

	private static PonderScene compile(net.minecraft.resources.ResourceLocation id) {
		//? if create6 {
		/*var scenes = PonderIndex.getSceneAccess().compile(id);
		*///?} else {
		var scenes = PonderRegistry.compile(id);
		//?}
		require(scenes.size() == 1, "one storyboard required");
		return scenes.get(0);
	}

	@SubscribeEvent
	public static void sound(PlaySoundEvent event) {
		if (whistle && event.getSound() != null
			&& com.simibubi.create.AllSoundEvents.WHISTLE_TRAIN.getMainEvent().getLocation().equals(event.getSound().getLocation())) {
			require(Math.abs(cartOffset() - 2) < .08, "whistle did not sound at the leading wheel crossing");
			whistles++;
		}
	}

	private static void checkWhistlePlayback() {
		require(!scene.getTitle().startsWith("railward.ponder."), "untranslated title");
		for (int i = 1; i <= 6; i++)
			require(!scene.getString("text_" + i).startsWith("railward.ponder."), "untranslated caption");
		scene.seekToTime(200);
		whistleSign(12);
		scene.seekToTime(515);
		require(Math.abs(cartOffset() - 4) < .001, "whistle cart did not cross the marker");
		scene.seekToTime(675);
		require(Math.abs(cartOffset()) < .001, "whistle cart did not return");
		scene.seekToTime(780);
		whistleSign(13);
		scene.seekToTime(855);
		require(scene.isFinished() && whistles == 0, "unfinished or audible seek");
	}

	private static void whistleSign(int rotation) {
		var world = scene.getWorld();
		var sign = (WhistleSignBlockEntity) world.getBlockEntity(SpeedSignScenes.SIGN);
		require(sign.edgePoint.getGlobalPosition().equals(SpeedSignScenes.TARGET)
			&& sign.edgePoint.getTargetDirection() == AxisDirection.NEGATIVE, "wrong whistle target or approach direction");
		require(world.getBlockState(SpeedSignScenes.SIGN).getValue(WhistleSignBlock.ROTATION) == rotation,
			"wrong whistle sign rotation");
	}

	private static void checkPlayback() {
		require(!scene.getTitle().startsWith("railward.ponder."), "untranslated title");
		for (int i = 1; i <= 6; i++)
			require(!scene.getString("text_" + i).startsWith("railward.ponder."), "untranslated caption");
		double fastStart = 0, slowStart = 0, returnStart = 0, fast = 0, slow = 0;
		for (int tick = 1; tick <= 1030; tick++) {
			scene.tick();
			if (tick == 190) sign(50, 12);
			if (tick == 370 || tick == 745) sign(20, 12);
			if (tick == 960) sign(20, 13);
			if (tick == 480) fastStart = cartOffset();
			if (tick == 490) fast = cartOffset() - fastStart;
			if (tick == 502) {
				double leadingWheel = SpeedSignScenes.BOGEY.getX() + .5 + cartOffset() + 1;
				require(Math.abs(leadingWheel - SpeedSignScenes.TARGET.getX() - .5) < .08,
					"slowdown did not begin at the front wheel crossing");
			}
			if (tick == 540) slowStart = cartOffset();
			if (tick == 550) slow = cartOffset() - slowStart;
			if (tick == 700) returnStart = cartOffset();
			if (tick == 710) require(Math.abs(cartOffset() - returnStart + slow) < .035, "return speed differs from the retained limit");
			if (tick == 620) require(Math.abs(cartOffset() - 4) < .001, "cart did not pass the marker");
			if (tick == 925) require(Math.abs(cartOffset()) < .001, "cart did not return");
		}
		require(fast > .45 && slow > .12 && slow < fast * .55, "visible slowdown missing");
		require(scene.isFinished(), "unfinished scene");
	}

	private static double cartOffset() {
		double[] offset = {Double.NaN};
		scene.forEach(WorldSectionElement.class, section -> {
			if (section != scene.getBaseWorldSection() && section.isVisible()) {
				require(Double.isNaN(offset[0]), "unexpected second moving section");
				offset[0] = section.getAnimatedOffset().x;
			}
		});
		require(!Double.isNaN(offset[0]), "missing cart section");
		return offset[0];
	}

	private static void sign(int limit, int rotation) {
		var world = scene.getWorld();
		var be = (SpeedSignBlockEntity) world.getBlockEntity(SpeedSignScenes.SIGN);
		require(be.getSpeedLimitKmh() == SpeedSignBlockEntity.clampSpeedLimitKmh(limit), "wrong displayed limit");
		require(be.edgePoint.getGlobalPosition().equals(SpeedSignScenes.TARGET)
			&& be.edgePoint.getTargetDirection() == AxisDirection.NEGATIVE, "wrong target or passage direction");
		require(world.getBlockState(SpeedSignScenes.SIGN).getValue(SpeedSignBlock.ROTATION) == rotation, "wrong sign rotation");
	}

	private static void open(Minecraft mc) {
		scene.begin();
		mc.setScreen(new PonderUI(List.of(scene)) {});
		scene.seekToTime((whistle ? WHISTLE_TIMES : SPEED_TIMES)[preview]);
		frame = 0;
	}

	private static void require(boolean condition, String message) {
		if (!condition) throw new IllegalStateException("Railward SPEED-SIGN-PONDER: " + message);
	}
}
