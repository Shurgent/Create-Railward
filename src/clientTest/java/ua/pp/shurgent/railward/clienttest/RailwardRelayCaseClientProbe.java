package ua.pp.shurgent.railward.clienttest;

import java.util.ArrayList;
import java.util.List;
//? if >=1.19.2 {
/*import net.minecraft.util.RandomSource;
*///?} else {
import java.util.Random;
//?}

import com.mojang.logging.LogUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ua.pp.shurgent.railward.AllBlocks;
import ua.pp.shurgent.railward.Railward;
import ua.pp.shurgent.railward.block.train.signal.RelayCaseBlock;
import ua.pp.shurgent.railward.block.train.signal.RelayCaseBlockEntity;
import ua.pp.shurgent.railward.block.train.signal.RelayCaseEdgePoint.TrackMarker;

/** Separate-JVM blockstate/model probe. The disposable server is controlled over RCON. */
@Mod.EventBusSubscriber(modid = Railward.MOD_ID, value = Dist.CLIENT)
public final class RailwardRelayCaseClientProbe {
	private static int ticks;
	private static int frame;
	private static int singleMarkerFrames;
	private static boolean ready;
	private static boolean complete;

	@SubscribeEvent
	public static void tick(TickEvent.ClientTickEvent event) {
		if (complete || event.phase != TickEvent.Phase.END || !Boolean.getBoolean("railward.clientProbe"))
			return;
		String mode = System.getProperty("railward.clientProbeMode");
		if ("relay_case_marker".equals(mode)) {
			checkTrackMarkers(Minecraft.getInstance());
			return;
		}
		if (!"relay_case".equals(mode))
			return;
		require(++ticks < 20 * 150, "timeout");
		Minecraft mc = Minecraft.getInstance();
		BlockPos first = new BlockPos(0, 100, 0);
		if (mc.level == null || mc.player == null || mc.getOverlay() != null
			|| !mc.level.getBlockState(first).is(AllBlocks.RELAY_CASE.get()))
			return;
		if (!ready) {
			if (!mc.level.getBlockState(first).getValue(RelayCaseBlock.OCCUPIED))
				return;
			for (Direction facing : Direction.Plane.HORIZONTAL) {
				var state = AllBlocks.RELAY_CASE.getDefaultState().setValue(RelayCaseBlock.FACING, facing);
				var model = mc.getBlockRenderer().getBlockModel(state);
				require(model != mc.getModelManager().getMissingModel(), "missing baked model");
				//? if >=1.19.2 {
				/*var random = RandomSource.create(42);
				*///?} else {
				var random = new Random(42);
				//?}
				List<BakedQuad> quads = new ArrayList<>(model.getQuads(state, null, random));
				for (Direction side : Direction.values())
					quads.addAll(model.getQuads(state, side, random));
				require(quads.size() == 21, "model lost faces: " + facing + ": " + quads.size());
				for (BakedQuad quad : quads) {
					require(quad.isShade(), "disabled diffuse shading");
					require(quad.getSprite() == model.getParticleIcon(), "missing atlas material");
				}
			}
			for (String key : List.of("block.railward.train.signal.relay_case", "railward.relay_case.free",
				"railward.relay_case.occupied", "railward.relay_case.unbound"))
				require(!I18n.get(key).equals(key), "missing localization " + key);
			ready = true;
			mc.options.hideGui = true;
			mc.player.setPos(4.5, 101.5, -6);
			mc.player.setYRot(0);
			mc.player.setXRot(18);
		}
		if (++frame == 40 || frame == 100) {
			Screenshot.grab(mc.gameDirectory, "relay-case-" + frame + ".png", mc.getMainRenderTarget(),
				message -> LogUtils.getLogger().info("Railward RELAY screenshot: {}", message.getString()));
			if (frame == 40) {
				mc.player.setPos(12, 101.5, 3);
				mc.player.setYRot(100);
				mc.player.setXRot(22);
			} else
				LogUtils.getLogger().info("Railward RELAY client ready for release");
		}
		if (frame <= 100 || mc.level.getBlockState(first).getValue(RelayCaseBlock.OCCUPIED))
			return;
		Direction[] facings = { Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST };
		for (int i = 0; i < 4; i++) {
			BlockPos pos = first.east(i * 3);
			var state = mc.level.getBlockState(pos);
			require(state.getValue(RelayCaseBlock.FACING) == facings[i], "wrong server facing");
			require(!state.getValue(RelayCaseBlock.TRAIN_PRESENT) && !state.getValue(RelayCaseBlock.OCCUPIED), "stale output/state");
			require(mc.level.getBlockEntity(pos) instanceof RelayCaseBlockEntity relay && !relay.isOccupied(), "stale client block entity");
		}
		complete = true;
		LogUtils.getLogger().info("Railward RELAY client probe passed: four baked orientations, localization, initial occupied "
			+ "state and live release from a dedicated server, no retransmission, two native previews");
		mc.stop();
	}

	private static void checkTrackMarkers(Minecraft mc) {
		require(++ticks < 20 * 150, "track marker timeout");
		if (mc.level == null || mc.player == null || mc.getOverlay() != null)
			return;
		BlockPos first = new BlockPos(0, 100, -2);
		BlockPos second = new BlockPos(0, 100, 2);
		if (!(mc.level.getBlockEntity(second) instanceof RelayCaseBlockEntity other))
			return;
		if (!ready) {
			if (!(mc.level.getBlockEntity(first) instanceof RelayCaseBlockEntity owner)
				|| owner.getTrackMarker() != TrackMarker.DOUBLE || other.getTrackMarker() != TrackMarker.HIDDEN)
				return;
			ready = true;
			mc.options.hideGui = true;
			mc.player.setPos(3, 103, -4);
			mc.player.setYRot(34);
			mc.player.setXRot(39);
		}
		if (++frame == 30) {
			captureMarker(mc, "double");
			LogUtils.getLogger().info("Railward RELAY marker ready for removal");
		}
		if (frame <= 30 || !mc.level.getBlockState(first).isAir() || other.getTrackMarker() != TrackMarker.SINGLE)
			return;
		if (++singleMarkerFrames < 10)
			return;
		captureMarker(mc, "single");
		complete = true;
		LogUtils.getLogger().info("Railward RELAY marker probe passed: one double marker, then one single marker after removal");
		mc.stop();
	}

	private static void captureMarker(Minecraft mc, String stage) {
		Screenshot.grab(mc.gameDirectory, "relay-marker-" + stage + ".png", mc.getMainRenderTarget(),
			message -> LogUtils.getLogger().info("Railward RELAY screenshot: {}", message.getString()));
	}

	private static void require(boolean condition, String message) {
		if (!condition)
			throw new IllegalStateException("Railward RELAY client probe: " + message);
	}
}
