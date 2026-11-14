package ua.pp.shurgent.railward.clienttest;

import java.util.List;
import com.mojang.logging.LogUtils;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.content.contraptions.actors.contraptionControls.ContraptionControlsBlockEntity;
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
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.util.Mth;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ua.pp.shurgent.railward.AllBlocks;
import ua.pp.shurgent.railward.AllBlockEntities;
import ua.pp.shurgent.railward.Railward;
import ua.pp.shurgent.railward.block.train.parts.LocomotiveBufferLightsBlock;
import ua.pp.shurgent.railward.block.train.parts.LocomotiveSpotlightBlock;
import ua.pp.shurgent.railward.block.train.parts.TrainWallPanelBlockEntity;
import ua.pp.shurgent.railward.block.train.parts.panel.LocomotiveBufferLightsAttachment;
import ua.pp.shurgent.railward.block.train.parts.panel.LocomotiveSpotlightAttachment;
import ua.pp.shurgent.railward.client.ponder.TrainLightingScenes;
import ua.pp.shurgent.railward.client.ponder.TrainLightingPonderState;
import ua.pp.shurgent.railward.client.renderer.LocomotiveBufferLightsRenderHelper.LampState;

@Mod.EventBusSubscriber(modid = Railward.MOD_ID, value = Dist.CLIENT)
public final class RailwardLightingPonderClientProbe {
	private static final int[] TIMES = {60, 315, 450, 650, 800, 885, 1100, 1250,
		70, 270, 450, 600, 695, 900, 1110, 1300};
	private static List<PonderScene> scenes;
	private static PonderScene active;
	private static int ticks, preview, frame;
	private static boolean done;

	@SubscribeEvent
	public static void tick(TickEvent.ClientTickEvent event) {
		if (done || event.phase != TickEvent.Phase.END || !Boolean.getBoolean("railward.clientProbe")
			|| !"ponder_lighting".equals(System.getProperty("railward.clientProbeMode"))) return;
		Minecraft mc = Minecraft.getInstance();
		require(++ticks < 20 * 240, "timeout");
		if (mc.level == null && mc.screen instanceof DisconnectedScreen) {
			String reason = mc.screen.getTitle().getString();
			for (var field : mc.screen.getClass().getDeclaredFields()) {
				if (!Component.class.isAssignableFrom(field.getType())) continue;
				try {
					field.setAccessible(true);
					reason += " / " + ((Component) field.get(mc.screen)).getString();
				} catch (ReflectiveOperationException | RuntimeException ignored) {}
			}
			LogUtils.getLogger().error("Railward LIGHTING-PONDER connection failed before scene playback: {}", reason);
			done = true;
			mc.stop();
			return;
		}
		if (scenes == null) {
			if (mc.level == null || mc.player == null || mc.getOverlay() != null) return;
			var buffers = compile(AllBlocks.LOCOMOTIVE_BUFFER_LIGHTS.getId());
			var spots = compile(AllBlocks.LOCOMOTIVE_SPOTLIGHT.getId());
			require(buffers.size() == 1 && spots.size() == 1, "one storyboard per item required");
			scenes = List.of(buffers.get(0), spots.get(0));
			check(scenes.get(0), false);
			check(scenes.get(1), true);
			var live = AllBlockEntities.LOCOMOTIVE_BUFFER_LIGHTS.get().create(BlockPos.ZERO,
				AllBlocks.LOCOMOTIVE_BUFFER_LIGHTS.getDefaultState());
			live.setLevel(mc.level);
			live.markVirtual();
			TrainLightingPonderState.set(live, LampState.RED);
			require(TrainLightingPonderState.get(live) == null, "preview state leaked outside Ponder");
			mc.options.hideGui = true;
			open(mc);
		}
		mc.getToasts().clear();
		if (frame == 20) { cart(active); controllerLevers(active); }
		if (++frame == 25)
			Screenshot.grab(mc.gameDirectory, "lighting-ponder-" + preview + ".png", mc.getMainRenderTarget(),
				message -> LogUtils.getLogger().info("Railward LIGHTING-PONDER screenshot: {}", message.getString()));
		if (frame < 45) return;
		if (++preview < TIMES.length) { open(mc); return; }
		done = true;
		LogUtils.getLogger().info("Railward LIGHTING-PONDER client probe passed: 2 registrations, 6+6 localized captions, "
			+ "13/2 housing variants, native panel attachments, white/red/off transitions, spotlight direction and control filter, "
			+ "no preview state outside Ponder, 3x3 cart, one bogey, tracks, opposed controls and seated direction bee, "
			+ "paired outward red lenses, single-lamp style cycling, smooth driver rotation, persistent levers on both controllers, opposing end faces, 180-degree camera orbits, 16 previews");
		mc.stop();
	}

	private static List<PonderScene> compile(ResourceLocation id) {
		//? if create6 {
		/*return PonderIndex.getSceneAccess().compile(id);
		*///?} else {
		return PonderRegistry.compile(id);
		//?}
	}

	private static void check(PonderScene scene, boolean spot) {
		require(!scene.getTitle().startsWith("railward.ponder."), "untranslated title");
		for (int i = 1; i <= 6; i++) require(!scene.getString("text_" + i).startsWith("railward.ponder."), "untranslated caption");
		int variants = 0;
		float[] previousYaw = {Direction.NORTH.toYRot()};
		for (int tick = 1; tick <= 1480; tick++) {
			scene.tick();
			if (tick == 50) cart(scene);
			if (tick >= 50) controllerLevers(scene);
			final int currentTick = tick;
			scene.forEachWorldEntity(Bee.class, bee -> {
				if (currentTick > 1) require(Math.abs(Mth.wrapDegrees(bee.getYRot() - previousYaw[0])) < 10, "driver rotation snapped");
				previousYaw[0] = bee.getYRot();
				if (currentTick == (spot ? 695 : 885)) {
					float yaw = Math.abs(Mth.wrapDegrees(bee.getYRot()));
					require(yaw > 30 && yaw < 150, "missing intermediate driver rotation");
				}
			});
			if (!spot && tick == 315) require(scene.getWorld().getBlockState(TrainLightingScenes.BUFFER_FIRST.east(2))
				.getValue(LocomotiveBufferLightsBlock.VARIANT) == ua.pp.shurgent.railward.block.train.parts.LocomotiveBufferLightsVariant.TYPE_1,
				"style cycling changed the other light");
			var first = scene.getWorld().getBlockState(spot ? TrainLightingScenes.FIRST : TrainLightingScenes.BUFFER_FIRST);
			if (tick < (spot ? 190 : 380)) {
				int ordinal = spot ? first.getValue(LocomotiveSpotlightBlock.VARIANT).ordinal()
					: first.getValue(LocomotiveBufferLightsBlock.VARIANT).ordinal();
				variants |= 1 << ordinal;
			}
			if (tick == (spot ? 270 : 450)) {
				var panel = (TrainWallPanelBlockEntity) scene.getWorld().getBlockEntity(spot ? TrainLightingScenes.FIRST : TrainLightingScenes.BUFFER_FIRST);
				require(spot ? panel.getAttachment() instanceof LocomotiveSpotlightAttachment
					: panel.getAttachment() instanceof LocomotiveBufferLightsAttachment, "missing panel attachment");
			}
			if (!spot) {
				if (tick == 650 || tick == 800) states(scene, spot, LampState.WHITE, LampState.RED);
				if (tick == 950 || tick == 1100) states(scene, spot, LampState.RED, LampState.WHITE);
				if (tick == 1250) states(scene, spot, LampState.OFF, LampState.OFF);
				if (tick == 650 || tick == 1100) camera(scene, 145);
				if (tick == 800 || tick == 950) camera(scene, 325);
				if (tick == 650 || tick == 800) driver(scene, Direction.NORTH);
				if (tick == 950 || tick == 1100) driver(scene, Direction.SOUTH);
			} else {
				if (tick == 450 || tick == 600) states(scene, spot, LampState.WHITE, LampState.OFF);
				if (tick == 750 || tick == 900 || tick == 1300) states(scene, spot, LampState.OFF, LampState.WHITE);
				if (tick == 1110) {
					states(scene, spot, LampState.OFF, LampState.OFF);
					var control = (ContraptionControlsBlockEntity) scene.getWorld().getBlockEntity(TrainLightingScenes.CONTROL);
					require(control.disabled && AllBlocks.LOCOMOTIVE_SPOTLIGHT.isIn(control.filtering.getFilter()), "control filter missing");
				}
				if (tick == 1450) states(scene, spot, LampState.OFF, LampState.OFF);
				if (tick == 450 || tick == 900) camera(scene, 145);
				if (tick == 600 || tick == 750 || tick == 1110) camera(scene, 325);
				if (tick == 450 || tick == 600) driver(scene, Direction.NORTH);
				if (tick == 750 || tick == 900 || tick == 1110 || tick == 1300) driver(scene, Direction.SOUTH);
			}
			if (tick == (spot ? 450 : 650)) {
				var a = scene.getWorld().getBlockState(spot ? TrainLightingScenes.FIRST : TrainLightingScenes.BUFFER_FIRST);
				var b = scene.getWorld().getBlockState(spot ? TrainLightingScenes.SECOND : TrainLightingScenes.BUFFER_SECOND);
				require((spot ? a.getValue(LocomotiveSpotlightBlock.FACING) : a.getValue(LocomotiveBufferLightsBlock.FACING)) == Direction.NORTH
					&& (spot ? b.getValue(LocomotiveSpotlightBlock.FACING) : b.getValue(LocomotiveBufferLightsBlock.FACING)) == Direction.SOUTH,
					"lights must face away from opposite ends");
				cart(scene);
			}
		}
		require(variants == (spot ? 3 : 8191), "not all housing variants shown");
		require(scene.isFinished(), "unfinished scene");
		require(Math.abs(scene.getTransform().xRotation.getValue(1) + 35) < .001, "isometric pitch changed");
	}

	private static void camera(PonderScene scene, float yaw) {
		require(Math.abs(Mth.wrapDegrees(scene.getTransform().yRotation.getChaseTarget() - yaw)) < .001, "wrong camera orbit");
	}

	private static void cart(PonderScene scene) {
		var world = scene.getWorld();
		for (BlockPos p : BlockPos.betweenClosed(2, 3, 2, 4, 3, 4))
			require(com.simibubi.create.AllBlocks.RAILWAY_CASING.has(world.getBlockState(p)), "cart deck must be exactly 3x3");
		int bogeys = 0;
		for (BlockPos p : BlockPos.betweenClosed(0, 1, 0, 6, 4, 6))
			if (com.simibubi.create.AllBlocks.SMALL_BOGEY.has(world.getBlockState(p))) bogeys++;
		require(bogeys == 1 && com.simibubi.create.AllBlocks.SMALL_BOGEY.has(world.getBlockState(TrainLightingScenes.BOGEY)), "one central bogey required");
		for (int z = 0; z < 7; z++)
			require(com.simibubi.create.AllBlocks.TRACK.has(world.getBlockState(new BlockPos(3, 1, z))), "missing track under cart");
		require(world.getBlockState(TrainLightingScenes.SEAT).getBlock() instanceof com.simibubi.create.content.contraptions.actors.seat.SeatBlock, "missing central seat");
		require(com.simibubi.create.AllBlocks.TRAIN_CONTROLS.has(world.getBlockState(TrainLightingScenes.FRONT_CONTROLS))
			&& com.simibubi.create.AllBlocks.TRAIN_CONTROLS.has(world.getBlockState(TrainLightingScenes.REAR_CONTROLS)), "two ordinary train controls required");
		require(world.getBlockState(TrainLightingScenes.FRONT_CONTROLS).getValue(com.simibubi.create.content.contraptions.actors.trainControls.ControlsBlock.FACING) == Direction.SOUTH
			&& world.getBlockState(TrainLightingScenes.REAR_CONTROLS).getValue(com.simibubi.create.content.contraptions.actors.trainControls.ControlsBlock.FACING) == Direction.NORTH, "controls must face the seated driver");
		int[] bees = {0};
		scene.forEachWorldEntity(Bee.class, bee -> {
			bees[0]++;
			require(bee.getVehicle() instanceof SeatEntity && bee.getVehicle().blockPosition().equals(TrainLightingScenes.SEAT), "bee must ride the central seat");
			require(Math.abs(bee.getX() - 3.5) < .01 && Math.abs(bee.getZ() - 3.5) < .01, "seated bee drifted");
		});
		require(bees[0] == 1, "exactly one bee driver required");
	}

	private static void controllerLevers(PonderScene scene) {
		var world = scene.getWorld();
		var virtual = com.simibubi.create.content.contraptions.actors.trainControls.ControlsBlock.VIRTUAL;
		require(world.getBlockState(TrainLightingScenes.FRONT_CONTROLS).getValue(virtual)
			&& world.getBlockState(TrainLightingScenes.REAR_CONTROLS).getValue(virtual),
			"both controllers must keep visible levers throughout the scene");
	}

	private static void driver(PonderScene scene, Direction expected) {
		scene.forEachWorldEntity(Bee.class, bee ->
			require(Math.abs(Mth.wrapDegrees(bee.getYRot() - expected.toYRot())) < .01
				&& Math.abs(Mth.wrapDegrees(bee.yHeadRot - expected.toYRot())) < .01,
				"bee must face the selected driving direction"));
	}

	private static void states(PonderScene scene, boolean spot, LampState first, LampState second) {
		for (int x : spot ? new int[]{3} : new int[]{2, 4}) {
			var a = (SmartBlockEntity) scene.getWorld().getBlockEntity(new BlockPos(x, 3, 1));
			var b = (SmartBlockEntity) scene.getWorld().getBlockEntity(new BlockPos(x, 3, 5));
			require(TrainLightingPonderState.get(a) == first && TrainLightingPonderState.get(b) == second,
				"wrong lamp pair state at x=" + x);
			if (!spot) {
				var variant = ua.pp.shurgent.railward.block.train.parts.LocomotiveBufferLightsVariant.TYPE_1;
				var mirror = ua.pp.shurgent.railward.block.train.parts.LocomotiveBufferLightsVariant.TYPE_2;
				require(a.getBlockState().getValue(LocomotiveBufferLightsBlock.VARIANT) == (x == 2 ? mirror : variant)
					&& b.getBlockState().getValue(LocomotiveBufferLightsBlock.VARIANT) == (x == 2 ? variant : mirror),
					"red lenses must be outside the pair");
			}
		}
	}

	private static void open(Minecraft mc) {
		active = scenes.get(preview / 8);
		active.begin();
		mc.setScreen(new PonderUI(List.of(active)) {});
		active.seekToTime(TIMES[preview]);
		frame = 0;
	}

	private static void require(boolean condition, String message) {
		if (!condition) throw new IllegalStateException("Railward LIGHTING-PONDER: " + message);
	}
}
