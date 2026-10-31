package ua.pp.shurgent.railward.clienttest;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.mojang.logging.LogUtils;
import com.simibubi.create.content.trains.signal.SignalBlockEntity.SignalState;
import com.simibubi.create.content.trains.signal.SignalBlockEntity;
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
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ua.pp.shurgent.railward.AllBlocks;
import ua.pp.shurgent.railward.Railward;
import ua.pp.shurgent.railward.block.train.signal.ModernSignalBlockEntity;
import ua.pp.shurgent.railward.block.train.signal.ModernSignalBlock;
import ua.pp.shurgent.railward.block.train.signal.ModernSignalGroundBlockEntity;

/** Loads and plays the shipped schematics in the real client; never packaged in release JARs. */
@Mod.EventBusSubscriber(modid = Railward.MOD_ID, value = Dist.CLIENT)
public final class RailwardPonderClientProbe {

	private static final List<PonderScene> previews = new ArrayList<>();
	private static int frame;
	private static int ticks;
	private static int preview;
	private static boolean started;
	private static boolean complete;
	private static boolean captured;
	private static int darkTicks;
	private static int rotationSteps;
	private static int lastRotation;
	private static final Set<Integer> renderedRotations = new HashSet<>();

	@SubscribeEvent
	public static void onClientTick(TickEvent.ClientTickEvent event) {
		if (complete || event.phase != TickEvent.Phase.END || !Boolean.getBoolean("railward.clientProbe")
			|| !"ponder".equals(System.getProperty("railward.clientProbeMode")))
			return;
		Minecraft mc = Minecraft.getInstance();
		require(++ticks <= 20 * 180, "timed out waiting for the server or scene previews");
		if (!started) {
			if (mc.getOverlay() != null || mc.level == null || mc.player == null)
				return;
			started = true;
			var tall = compile(AllBlocks.MODERN_SIGNAL.getId());
			var ground = compile(AllBlocks.MODERN_SIGNAL_GROUND.getId());
			require(tall.size() == 2 && ground.size() == 2, "each item must have installation and aspect scenes");
			for (var scene : List.of(tall.get(0), tall.get(1), ground.get(0), ground.get(1)))
				checkPlayback(scene);
			previews.add(tall.get(0));
			previews.add(tall.get(1));
			previews.add(ground.get(1));
			previews.add(tall.get(0));
			previews.add(tall.get(1));
			previews.add(ground.get(1));
			mc.options.hideGui = true;
			openPreview(mc);
		}
		// Fresh offline-server join toasts would obscure the tutorial under test.
		mc.getToasts().clear();
		frame++;
		PonderScene active = previews.get(preview);
		boolean takeSnapshot = frame == 20;
		boolean advance = frame >= 40;
		if (preview == 3) {
			var world = active.getWorld();
			BlockPos base = new BlockPos(2, 2, 2);
			int rotation = world.getBlockState(base).getValue(ModernSignalBlock.ROTATION);
			if (rotation != lastRotation) {
				require(rotation == ((lastRotation + 1) & 15), "wrench rotation skipped a step");
				rotationSteps++;
				lastRotation = rotation;
			}
			for (int part = 1; part <= 3; part++)
				require(world.getBlockState(base.above(part)).getValue(ModernSignalBlock.ROTATION) == rotation,
					"wrench rotation left a tall-signal part behind");
			renderedRotations.add(((ModernSignalBlockEntity) world.getBlockEntity(base)).getBlockState()
				.getValue(ModernSignalBlock.ROTATION));
			takeSnapshot = active.getCurrentTime() >= 910;
			advance = active.getCurrentTime() >= 930;
			if (advance)
				require(rotationSteps == 15 && renderedRotations.size() == 16, "not all 15 wrench steps rendered");
		} else if (preview >= 4) {
			BlockPos source = new BlockPos(preview == 4 ? 2 : 4, 1, 2);
			if (darkTicks == 0 && active.getCurrentTime() >= 575) {
				require(repeatedAspect(active, source.above()) == SignalState.INVALID,
					"invalid source was not rendered before disconnect");
				SignalBlockEntity cachedSource = (SignalBlockEntity) active.getWorld().getBlockEntity(source);
				active.seekToTime(700);
				require(cachedSource.isRemoved(), "schematic source was not invalidated before removal");
				darkTicks = 1;
			}
			if (darkTicks > 0) {
				require(active.getWorld().getBlockEntity(source) == null, "disconnected source still exists");
				require(repeatedAspect(active, source.above()) == null, "rendered disconnected signal relit");
				darkTicks++;
			}
			takeSnapshot = darkTicks >= 21;
			advance = darkTicks >= 81;
		}
		if (takeSnapshot && !captured) {
			captured = true;
			Screenshot.grab(mc.gameDirectory, "ponder-" + preview + ".png", mc.getMainRenderTarget(),
				message -> LogUtils.getLogger().info("Railward PONDER screenshot: {}", message.getString()));
		}
		if (!advance)
			return;
		frame = 0;
		if (++preview < previews.size()) {
			openPreview(mc);
			return;
		}
		complete = true;
		LogUtils.getLogger().info("Railward PONDER client probe passed: 4 registered storyboards, real schematic loading, "
			+ "complete playback, all four aspects, 15 rendered wrench steps, both cached-source disconnects stayed dark "
			+ "for 80 client ticks, localized titles/text, 6 rendered previews");
		mc.stop();
	}

	private static List<PonderScene> compile(ResourceLocation item) {
		//? if create6 {
		/*return PonderIndex.getSceneAccess().compile(item);
		*///?} else {
		return PonderRegistry.compile(item);
		//?}
	}

	private static void checkPlayback(PonderScene scene) {
		require(!scene.getTitle().startsWith("railward.ponder."), "untranslated title");
		int textCount = scene.getTitle().equals(compileTitle()) ? 7 : 6;
		for (int i = 1; i <= textCount; i++)
			require(!scene.getString("text_" + i).startsWith("railward.ponder."), "untranslated caption " + i);
		var seen = EnumSet.noneOf(SignalState.class);
		for (int i = 0; i < 1500; i++) {
			scene.tick();
			var world = scene.getWorld();
			require(world.getBlockState(new BlockPos(3, 1, 4)).is(com.simibubi.create.AllBlocks.TRACK.get()),
				"missing schematic track");
			require(world.getBlockEntity(new BlockPos(2, 2, 2)) instanceof ModernSignalBlockEntity,
				"missing tall signal block entity");
			require(world.getBlockEntity(new BlockPos(4, 2, 2)) instanceof ModernSignalGroundBlockEntity,
				"missing ground signal block entity");
			SignalState a = ((ModernSignalBlockEntity) world.getBlockEntity(new BlockPos(2, 2, 2))).signalState;
			SignalState b = ((ModernSignalGroundBlockEntity) world.getBlockEntity(new BlockPos(4, 2, 2))).signalState;
			if (a != null) seen.add(a);
			if (b != null) seen.add(b);
		}
		require(scene.isFinished(), "playback did not finish");
		if (textCount == 6) {
			require(seen.equals(EnumSet.allOf(SignalState.class)), "missing aspects " + seen);
			var world = scene.getWorld();
			require(((ModernSignalBlockEntity) world.getBlockEntity(new BlockPos(2, 2, 2))).signalState == null
				&& ((ModernSignalGroundBlockEntity) world.getBlockEntity(new BlockPos(4, 2, 2))).signalState == null,
				"disconnected signal still lit");
		}
	}

	private static String compileTitle() {
		return net.minecraft.client.resources.language.I18n.get("railward.ponder.signal_placement.header");
	}

	private static void openPreview(Minecraft mc) {
		PonderScene scene = previews.get(preview);
		scene.begin();
		mc.setScreen(new PreviewScreen(scene));
		scene.seekToTime(preview == 0 ? 700 : preview == 3 ? 790 : preview >= 4 ? 550 : 60);
		captured = false;
		darkTicks = 0;
		rotationSteps = 0;
		lastRotation = scene.getWorld().getBlockState(new BlockPos(2, 2, 2)).getValue(ModernSignalBlock.ROTATION);
		renderedRotations.clear();
		renderedRotations.add(lastRotation);
	}

	private static SignalState repeatedAspect(PonderScene scene, BlockPos pos) {
		var be = scene.getWorld().getBlockEntity(pos);
		return be instanceof ModernSignalBlockEntity tall ? tall.signalState
			: ((ModernSignalGroundBlockEntity) be).signalState;
	}

	private static final class PreviewScreen extends PonderUI {
		private PreviewScreen(PonderScene scene) {
			super(List.of(scene));
		}
	}

	private static void require(boolean condition, String message) {
		if (!condition) throw new IllegalStateException("Railward PONDER: " + message);
	}
}
