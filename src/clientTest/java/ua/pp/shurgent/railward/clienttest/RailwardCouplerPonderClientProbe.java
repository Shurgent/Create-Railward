package ua.pp.shurgent.railward.clienttest;

import java.util.List;
import com.mojang.logging.LogUtils;
import com.simibubi.create.content.trains.bogey.AbstractBogeyBlockEntity;
import com.simibubi.create.foundation.ponder.element.WorldSectionElement;
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
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import ua.pp.shurgent.railward.AllBlocks;
import ua.pp.shurgent.railward.Railward;
import ua.pp.shurgent.railward.block.train.parts.TrainCouplerBlock;
import ua.pp.shurgent.railward.client.ponder.CouplerScenes;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Exercises virtual storyboards and their real UI; never included in release JARs. */
@Mod.EventBusSubscriber(modid = Railward.MOD_ID, value = Dist.CLIENT)
public final class RailwardCouplerPonderClientProbe {

	private static List<PonderScene> scenes;
	private static PonderScene active;
	private static int ticks, preview, frame;
	private static boolean done;
	private static final int[] TIMES = {60, 590, 760, 230, 560, 920, 80, 810, 1130};
	private static final int[] TEXT_COUNTS = {5, 6, 19};

	@SubscribeEvent
	public static void tick(TickEvent.ClientTickEvent event) {
		if (done || event.phase != TickEvent.Phase.END || !Boolean.getBoolean("railward.clientProbe")
			|| !"ponder_coupler".equals(System.getProperty("railward.clientProbeMode"))) return;
		Minecraft mc = Minecraft.getInstance();
		require(++ticks < 20 * 240, "timeout");
		if (scenes == null) {
			if (mc.level == null || mc.player == null || mc.getOverlay() != null) return;
			//? if create6 {
			/*scenes = PonderIndex.getSceneAccess().compile(AllBlocks.TRAIN_COUPLER.getId());
			*///?} else {
			scenes = PonderRegistry.compile(AllBlocks.TRAIN_COUPLER.getId());
			//?}
			require(scenes.size() == 3, "three coupler storyboards required");
			for (int index = 0; index < scenes.size(); index++) check(scenes.get(index), index);
			mc.options.hideGui = true;
			open(mc);
		}
		mc.getToasts().clear();
		if (preview == 7) require(com.simibubi.create.AllBlocks.TRAIN_CONTROLS.has(
			active.getWorld().getBlockState(CouplerScenes.WAGON_CONTROLS)), "missing second controls in naming example");
		if (preview == 8) require(active.getWorld().getBlockState(CouplerScenes.LOCO_CONTROLS).isAir()
			&& active.getWorld().getBlockState(CouplerScenes.SECOND_LOCO_CONTROLS).isAir()
			&& active.getWorld().getBlockState(CouplerScenes.WAGON_CONTROLS).isAir(), "no-controls example still has controls");
		if (++frame == 25)
			Screenshot.grab(mc.gameDirectory, "coupler-ponder-" + preview + ".png", mc.getMainRenderTarget(),
				message -> LogUtils.getLogger().info("Railward COUPLER-PONDER screenshot: {}", message.getString()));
		if (frame < 45) return;
		if (++preview < TIMES.length) { open(mc); return; }
		done = true;
		LogUtils.getLogger().info("Railward COUPLER-PONDER client probe passed: 3 registrations, 5/6/19 localized texts, "
			+ "facing couplers at matching height, correct wheel directions, automatic arrival and joint departure, "
			+ "manual joint movement and independent departure, naming controls examples, standard isometry, 9 previews");
		mc.stop();
	}

	private static void check(PonderScene scene, int index) {
		require(!scene.getTitle().startsWith("railward.ponder."), "untranslated header");
		for (int i = 1; i <= TEXT_COUNTS[index]; i++)
			require(!scene.getString("text_" + i).startsWith("railward.ponder."), "untranslated text " + i);
		var world = scene.getWorld();
		var first = world.getBlockState(CouplerScenes.WAGON_COUPLER);
		var second = world.getBlockState(CouplerScenes.LOCO_COUPLER);
		require(AllBlocks.TRAIN_COUPLER.has(first) && AllBlocks.TRAIN_COUPLER.has(second), "missing facing pair");
		require(first.getValue(TrainCouplerBlock.FACING) == second.getValue(TrainCouplerBlock.FACING).getOpposite(), "wrong coupler orientation");
		require(CouplerScenes.WAGON_COUPLER.getY() == CouplerScenes.LOCO_COUPLER.getY(), "different coupler heights");
		float previous = 0;
		int reverse = 0, forward = 0;
		for (int tick = 1; tick <= 1450; tick++) {
			scene.tick();
			var bogey = (AbstractBogeyBlockEntity) world.getBlockEntity(new BlockPos(CouplerScenes.LOCO_BOGEYS[0], 2, 7));
			float angle = bogey.getVirtualAngle(1);
			float step = Mth.wrapDegrees(angle - previous);
			if (step < -.001f) reverse++;
			if (step > .001f) forward++;
			if (index == 0 && tick > 545 && tick < 600) require(step <= .001f, "approach wheels rotate against -X movement");
			if (index == 0 && tick > 715 && tick < 770) require(step >= -.001f, "departure wheels rotate against +X movement");
			previous = angle;
			if (index == 0 && tick == 690) require(hasOffset(scene, -2), "locomotive did not close the coupling gap");
			if (index == 1 && tick == 700) require(hasOffset(scene, 2), "wagon did not follow the coupled locomotive");
		}
		require(scene.isFinished(), "unfinished scene");
		require(Math.abs(scene.getTransform().xRotation.getValue(1) + 35) < .001
			&& Math.abs(scene.getTransform().yRotation.getValue(1) - 145) < .001, "keep standard isometric camera");
		if (index == 0) {
			require(reverse >= 60 && forward >= 60, "missing approach/departure wheel animation");
			require(hasOffset(scene, 3) && hasOffset(scene, 1), "automatic joint departure changed the coupling gap");
		} else if (index == 1) {
			require(hasOffset(scene, 2) && hasOffset(scene, 3), "manual split did not leave the wagon behind");
			require(reverse == 0 && forward >= 100, "wrong manual wheel animation");
		} else {
			require(world.getBlockState(CouplerScenes.LOCO_CONTROLS).isAir()
				&& world.getBlockState(CouplerScenes.SECOND_LOCO_CONTROLS).isAir()
				&& world.getBlockState(CouplerScenes.WAGON_CONTROLS).isAir(), "controls retained after no-controls example");
			require(hasOffset(scene, -1.25), "naming split should show separated parts");
		}
	}

	private static boolean hasOffset(PonderScene scene, double x) {
		boolean[] found = {false};
		scene.forEach(WorldSectionElement.class, section -> { if (Math.abs(section.getAnimatedOffset().x - x) < .001) found[0] = true; });
		return found[0];
	}

	private static void open(Minecraft mc) {
		active = scenes.get(preview / 3);
		active.begin();
		mc.setScreen(new PonderUI(List.of(active)) {});
		active.seekToTime(TIMES[preview]);
		frame = 0;
	}

	private static void require(boolean condition, String message) {
		if (!condition) throw new IllegalStateException("Railward COUPLER-PONDER: " + message);
	}
}
