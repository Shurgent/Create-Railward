package ua.pp.shurgent.railward.clienttest;

import java.util.EnumMap;
import java.util.Map;

import com.mojang.logging.LogUtils;
import com.simibubi.create.CreateClient;
//? if create6
/*import net.createmod.catnip.outliner.Outliner;*/

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
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
import ua.pp.shurgent.railward.client.renderer.RailCrossingSignalHighlight;

/** Separate-JVM client probe. RCON positions the player and clears the cabinet after both flashes. */
@Mod.EventBusSubscriber(modid = Railward.MOD_ID, value = Dist.CLIENT)
public final class RailwardCrossingSignalClientProbe {

	private static final BlockPos SIGNAL = new BlockPos(0, 100, 0);
	private static final Map<CrossingSound, TickableSoundInstance> SOUNDS = new EnumMap<>(CrossingSound.class);
	private static int ticks;
	private static int ticksNearSignal;
	private static boolean capturedSettingsOutline;
	private static boolean configured;
	private static boolean selectedRing;
	private static boolean left;
	private static boolean right;
	private static boolean readyForRelease;
	private static boolean complete;
	private static Lamp previousLamp;

	@SubscribeEvent
	public static void onSound(PlaySoundEvent event) {
		if (!enabled() || !(event.getSound() instanceof TickableSoundInstance sound)
			|| sound.getX() != .5 || sound.getY() != 100.5 || sound.getZ() != .5)
			return;
		for (CrossingSound mode : CrossingSound.values())
			if (mode.getEvent().getLocation().equals(sound.getLocation())) {
				require(SOUNDS.put(mode, sound) == null, "started a duplicate loop for " + mode);
				require(sound.isLooping(), "crossing audio did not loop");
			}
	}

	@SubscribeEvent
	public static void tick(TickEvent.ClientTickEvent event) {
		if (complete || event.phase != TickEvent.Phase.END || !enabled())
			return;
		require(++ticks < 20 * 150, "timed out");
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || mc.player == null || mc.getOverlay() != null || mc.player.getY() < 99
			|| mc.player.distanceToSqr(.5, 100, .5) > 16
			|| !(mc.level.getBlockEntity(SIGNAL) instanceof RailCrossingSignalBlockEntity signal))
			return;
		if (++ticksNearSignal < 60)
			return;

		if (!selectedRing) {
			if (!isPlaying(mc, CrossingSound.SMALL_BELL))
				return;
			if (!capturedSettingsOutline && mc.hitResult instanceof BlockHitResult hit
				&& hit.getBlockPos().equals(SIGNAL) && RailCrossingSignalBlock.isConfigurationHit(signal.getBlockState(), hit)) {
				Screenshot.grab(mc.gameDirectory, "settings-outline.png", mc.getMainRenderTarget(),
					message -> LogUtils.getLogger().info("Railward CROSSING screenshot: {}", message.getString()));
				capturedSettingsOutline = true;
				return;
			}
			verifyHighlights(mc, signal);
			require(signal.getSign() == CrossingSign.SINGLE_TRACK && signal.getSound() == CrossingSound.SMALL_BELL,
				"wrong initial settings");
			selectSound(mc, signal, CrossingSound.BELL);
			selectedRing = true;
			return;
		}
		if (!configured) {
			if (!isPlaying(mc, CrossingSound.BELL))
				return;
			require(SOUNDS.get(CrossingSound.SMALL_BELL).isStopped(), "bell continued after changing to ring");
			selectSound(mc, signal, CrossingSound.SIREN);
			configured = true;
			mc.options.hideGui = true;
		}

		if (signal.getSign() != CrossingSign.MULTIPLE_TRACKS || signal.getSound() != CrossingSound.SIREN)
			return;
		Lamp lamp = signal.getLamp();
		if (lamp != previousLamp) {
			previousLamp = lamp;
			return;
		}
		if (!left && lamp == Lamp.LEFT) {
			left = true;
			capture(mc, "left");
		}
		if (!right && lamp == Lamp.RIGHT) {
			right = true;
			capture(mc, "right");
		}
		if (!readyForRelease && left && right && isPlaying(mc, CrossingSound.SIREN)) {
			require(SOUNDS.get(CrossingSound.BELL).isStopped(), "ring continued after changing to siren");
			readyForRelease = true;
			LogUtils.getLogger().info("Railward CROSSING client ready for release");
		}
		if (readyForRelease && lamp == Lamp.OFF) {
			if (!SOUNDS.get(CrossingSound.SIREN).isStopped())
				return;
			capture(mc, "off");
			complete = true;
			LogUtils.getLogger().info("Railward CROSSING client probe passed");
			mc.stop();
		}
	}

	private static boolean isPlaying(Minecraft mc, CrossingSound mode) {
		TickableSoundInstance sound = SOUNDS.get(mode);
		return sound != null && !sound.isStopped() && mc.getSoundManager().isActive(sound);
	}

	private static void verifyHighlights(Minecraft mc, RailCrossingSignalBlockEntity signal) {
		var originalHit = mc.hitResult;
		var originalStack = mc.player.getMainHandItem();
		var tick = new TickEvent.ClientTickEvent(TickEvent.Phase.END);
		//? if create6
		/*var outliner = Outliner.getInstance();*/
		//? if !create6
		var outliner = CreateClient.OUTLINER;
		var settings = Railward.asResource("crossing_settings");
		var cabinet = Railward.asResource("crossing_cabinet");
		try {
			mc.hitResult = configurationHit(mc, signal);
			ItemStack item = new ItemStack(AllBlocks.RAIL_CROSSING_SIGNAL.get());
			item.getOrCreateTag().putLong("CabinetPos", SIGNAL.east(4).asLong());
			item.getOrCreateTag().putString("CabinetDimension", mc.level.dimension().location().toString());
			mc.player.setItemInHand(InteractionHand.MAIN_HAND, item);
			RailCrossingSignalHighlight.tick(tick);
			require(outliner.getOutlines().containsKey(settings), "settings outline did not appear");
			require(outliner.getOutlines().containsKey(cabinet), "selected cabinet was not highlighted");
			item.getOrCreateTag().putString("CabinetDimension", "minecraft:the_nether");
			RailCrossingSignalHighlight.tick(tick);
			require(!outliner.getOutlines().containsKey(cabinet), "highlighted a cabinet in the wrong dimension");
			mc.hitResult = null;
			mc.player.setItemInHand(InteractionHand.MAIN_HAND, originalStack);
			RailCrossingSignalHighlight.tick(tick);
			require(!outliner.getOutlines().containsKey(settings), "settings outline remained after looking away");
			require(!outliner.getOutlines().containsKey(cabinet), "cabinet outline remained without the item");
		} finally {
			mc.hitResult = originalHit;
			mc.player.setItemInHand(InteractionHand.MAIN_HAND, originalStack);
		}
	}

	private static void selectSound(Minecraft mc, RailCrossingSignalBlockEntity signal, CrossingSound sound) {
		BlockHitResult hit = configurationHit(mc, signal);
		signal.getBlockState().use(mc.level, mc.player, InteractionHand.MAIN_HAND, hit);
		Screen screen = mc.screen;
		require(screen != null && screen.getTitle().getString().equals("Настройки сигнала переезда"),
			"plate did not open the localized settings screen");
		int x = (screen.width - 280) / 2;
		int y = (screen.height - 210) / 2;
		require(screen.mouseClicked(x + 140, y + 55, 0) && screen.mouseClicked(x + 140, y + 116, 0),
			"could not select Multi-track");
		require(screen.mouseClicked(x + 140, y + 93, 0)
			&& screen.mouseClicked(x + 140, y + 114 + sound.ordinal() * 20, 0), "could not select " + sound);
		require(screen.mouseClicked(x + 150, y + 190, 0) && mc.screen == null, "could not apply settings");
	}

	private static BlockHitResult configurationHit(Minecraft mc, RailCrossingSignalBlockEntity signal) {
		var state = signal.getBlockState();
		var center = new Vec3(.5, 100.5 - .5 / 16d, .5);
		BlockHitResult hit = state.getShape(mc.level, SIGNAL).clip(center.add(0, 0, -2), center, SIGNAL);
		require(hit != null && RailCrossingSignalBlock.isConfigurationHit(state, hit),
			"existing model shape did not expose the settings hit");
		return hit;
	}

	private static boolean enabled() {
		return Boolean.getBoolean("railward.clientProbe") && "rail_crossing_signal".equals(System.getProperty("railward.clientProbeMode"));
	}

	private static void capture(Minecraft mc, String phase) {
		Screenshot.grab(mc.gameDirectory, "crossing-" + phase + ".png", mc.getMainRenderTarget(),
			message -> LogUtils.getLogger().info("Railward CROSSING screenshot: {}", message.getString()));
	}

	private static void require(boolean condition, String message) {
		if (!condition)
			throw new IllegalStateException("Railward CROSSING client probe: " + message);
	}
}
