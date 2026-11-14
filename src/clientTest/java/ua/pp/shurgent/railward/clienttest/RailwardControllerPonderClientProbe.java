package ua.pp.shurgent.railward.clienttest;

import java.util.List;
import com.mojang.logging.LogUtils;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.content.contraptions.actors.seat.SeatEntity;
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
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.animal.Bee;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ua.pp.shurgent.railward.AllBlocks;
import ua.pp.shurgent.railward.Railward;
import ua.pp.shurgent.railward.block.train.parts.ModernTrainControllerBlock;
import ua.pp.shurgent.railward.client.ponder.ControllerLeverElement;
import ua.pp.shurgent.railward.client.ponder.ModernTrainControllerScenes;
import ua.pp.shurgent.railward.client.ponder.TrainLightingScenes;
import ua.pp.shurgent.railward.client.ponder.TrainLightingPonderState;
import ua.pp.shurgent.railward.client.renderer.LocomotiveBufferLightsRenderHelper.LampState;

@Mod.EventBusSubscriber(modid = Railward.MOD_ID, value = Dist.CLIENT)
public final class RailwardControllerPonderClientProbe {
	private static final int[] TIMES = {60, 260, 440, 515, 565, 645, 682, 695, 820, 1060, 1190, 1300};
	private static PonderScene scene;
	private static int ticks, preview, frame;
	private static boolean done;

	@SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
		if (done || event.phase != TickEvent.Phase.END || !Boolean.getBoolean("railward.clientProbe")
			|| !"ponder_controller".equals(System.getProperty("railward.clientProbeMode"))) return;
		Minecraft mc = Minecraft.getInstance();
		require(++ticks < 20 * 240, "timeout");
		if (mc.level == null && mc.screen instanceof DisconnectedScreen) {
			LogUtils.getLogger().error("Railward CONTROLLER-PONDER disconnected before scene playback");
			done = true; mc.stop(); return;
		}
		if (scene == null) {
			if (mc.level == null || mc.player == null || mc.getOverlay() != null) return;
			//? if create6 {
			/*var scenes = PonderIndex.getSceneAccess().compile(AllBlocks.MODERN_TRAIN_CONTROLLER.getId());
			*///?} else {
			var scenes = PonderRegistry.compile(AllBlocks.MODERN_TRAIN_CONTROLLER.getId());
			//?}
			require(scenes.size() == 1, "one controller storyboard required");
			scene = scenes.get(0);
			check();
			mc.options.hideGui = true;
			open(mc);
		}
		mc.getToasts().clear();
		if (frame == 20) cart();
		if (++frame == 25) Screenshot.grab(mc.gameDirectory, "controller-ponder-" + preview + ".png",
			mc.getMainRenderTarget(), message -> LogUtils.getLogger().info("Railward CONTROLLER-PONDER screenshot: {}", message.getString()));
		if (frame < 45) return;
		if (++preview < TIMES.length) { open(mc); return; }
		done = true;
		LogUtils.getLogger().info("Railward CONTROLLER-PONDER client probe passed: one controller, 6 captions, 3x3 cart, "
			+ "one bogey, seated bee, paired lamps, cab-relative forward/reverse/forward/off, fixed housing, smooth reverser/throttle/brake, "
			+ "smooth driver turn, camera orbits and replay; 12 previews");
		mc.stop();
	}

	private static void check() {
		require(!scene.getTitle().startsWith("railward.ponder."), "untranslated title");
		for (int i = 1; i <= 6; i++) require(!scene.getString("text_" + i).startsWith("railward.ponder."), "untranslated caption");
		float[] previous = {0, Direction.NORTH.toYRot()};
		for (int tick = 1; tick <= 1480; tick++) {
			scene.tick();
			if (tick == 50) cart();
			if (tick < 50) continue;
			var state = scene.getWorld().getBlockState(ModernTrainControllerScenes.CONTROLLER);
			require(ModernTrainControllerBlock.getPhysicalFacing(state) == Direction.SOUTH, "controller housing rotated");
			require(state.getValue(ModernTrainControllerBlock.OPEN), "lever-free housing needed for animated partials");
			final int time = tick;
			int[] elements = {0};
			scene.forEach(ControllerLeverElement.class, element -> {
				elements[0]++;
				float value = element.reverser(1);
				require(Math.abs(value - previous[0]) <= .501, "reverser snapped");
				previous[0] = value;
				if (time == 60 || time == 1300) require(Math.abs(value) < .001, "neutral lever missing");
				if (time == 260 || time == 1060) require(value > .99, "forward lever missing");
				if (time == 820) require(value < -.99, "reverse lever missing");
				if (time == 682) require(value > -.99 && value < .99, "missing intermediate reverser animation");
				if (time == 515) require(element.throttle(1) > .99 && element.brake(1) < .01, "throttle animation missing");
				if (time == 565) require(element.brake(1) > .99 && element.throttle(1) < .01, "brake animation missing");
			});
			require(elements[0] == 1, "one lever renderer required after replay");
			scene.forEachWorldEntity(Bee.class, bee -> {
				require(Math.abs(Mth.wrapDegrees(bee.getYRot() - previous[1])) < 10, "driver snapped");
				previous[1] = bee.getYRot();
				if (time == 260 || time == 1060) require(Math.abs(Mth.wrapDegrees(bee.getYRot() - ModernTrainControllerScenes.FORWARD.toYRot())) < .01, "wrong forward driver yaw");
				if (time == 820) require(Math.abs(Mth.wrapDegrees(bee.getYRot() - ModernTrainControllerScenes.FORWARD.getOpposite().toYRot())) < .01, "wrong reverse driver yaw");
				if (time == 695) require(Math.abs(Mth.wrapDegrees(bee.getYRot())) > 30
					&& Math.abs(Mth.wrapDegrees(bee.getYRot())) < 150, "missing intermediate driver turn");
			});
			if (tick == 260 || tick == 440 || tick == 1060) lights(LampState.WHITE, LampState.RED);
			if (tick == 695 || tick == 820) {
				lights(LampState.RED, LampState.WHITE);
				require(state.getValue(ModernTrainControllerBlock.REVERSED)
					&& state.getValue(ModernTrainControllerBlock.FACING) == Direction.NORTH, "reverse selection missing");
			}
			if (tick == 60 || tick == 1300) lights(LampState.OFF, LampState.OFF);
			if (tick == 260 || tick == 820 || tick == 1060) camera(145);
			if (tick == 440 || tick == 695) camera(325);
		}
		require(scene.isFinished(), "unfinished scene");
		require(Math.abs(scene.getTransform().xRotation.getValue(1) + 35) < .001, "isometric pitch changed");
	}

	private static void lights(LampState north, LampState south) {
		for (int x : new int[]{2,4}) {
			var a = (SmartBlockEntity) scene.getWorld().getBlockEntity(new BlockPos(x,3,1));
			var b = (SmartBlockEntity) scene.getWorld().getBlockEntity(new BlockPos(x,3,5));
			require(TrainLightingPonderState.get(a) == north && TrainLightingPonderState.get(b) == south, "wrong buffer-light colours");
		}
	}

	private static void camera(float yaw) {
		require(Math.abs(Mth.wrapDegrees(scene.getTransform().yRotation.getChaseTarget() - yaw)) < .001, "wrong camera orbit");
	}

	private static void cart() {
		var world = scene.getWorld();
		require(TrainLightingScenes.SEAT.relative(ModernTrainControllerScenes.FORWARD)
			.equals(ModernTrainControllerScenes.CONTROLLER), "forward must point from the seat toward the controller");
		int controllers = 0, bogeys = 0;
		for (BlockPos p : BlockPos.betweenClosed(0,1,0,6,5,6)) {
			var state = world.getBlockState(p);
			if (AllBlocks.MODERN_TRAIN_CONTROLLER.has(state)) controllers++;
			require(!com.simibubi.create.AllBlocks.TRAIN_CONTROLS.has(state), "ordinary controller left on cart");
			if (com.simibubi.create.AllBlocks.SMALL_BOGEY.has(state)) bogeys++;
		}
		require(controllers == 1 && bogeys == 1 && world.isEmptyBlock(TrainLightingScenes.REAR_CONTROLS), "one controller and one bogey required");
		for (BlockPos p : BlockPos.betweenClosed(2,3,2,4,3,4)) require(com.simibubi.create.AllBlocks.RAILWAY_CASING.has(world.getBlockState(p)), "3x3 deck required");
		for (int z = 0; z < 7; z++) require(com.simibubi.create.AllBlocks.TRACK.has(world.getBlockState(new BlockPos(3,1,z))), "track missing");
		int[] bees = {0};
		scene.forEachWorldEntity(Bee.class, bee -> {
			bees[0]++;
			require(bee.getVehicle() instanceof SeatEntity && bee.getVehicle().blockPosition().equals(TrainLightingScenes.SEAT), "bee must be seated");
			require(Math.abs(bee.getX() - 3.5) < .01 && Math.abs(bee.getZ() - 3.5) < .01, "driver drifted");
		});
		require(bees[0] == 1, "one bee required");
	}

	private static void open(Minecraft mc) {
		scene.begin();
		mc.setScreen(new PonderUI(List.of(scene)) {});
		scene.seekToTime(TIMES[preview]);
		frame = 0;
	}
	private static void require(boolean condition, String message) {
		if (!condition) throw new IllegalStateException("Railward CONTROLLER-PONDER: " + message);
	}
}
