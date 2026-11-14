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
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
//? if >=1.20.1
/*import net.minecraftforge.common.capabilities.ForgeCapabilities;*/
//? if <1.20.1
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ua.pp.shurgent.railward.AllBlocks;
import ua.pp.shurgent.railward.Railward;
import ua.pp.shurgent.railward.block.train.parts.CisternBlockEntity;
import ua.pp.shurgent.railward.client.ponder.CisternScenes;

/** Native storyboard/renderer check, isolated from release sources. */
@Mod.EventBusSubscriber(modid = Railward.MOD_ID, value = Dist.CLIENT)
public final class RailwardCisternPonderClientProbe {

	private static final int[] TIMES = {80, 350, 500, 780, 1050, 1270, 1480, 60, 250, 480, 710};
	private static List<PonderScene> scenes;
	private static PonderScene active;
	private static int ticks, preview, frame;
	private static boolean done;

	@SubscribeEvent
	public static void tick(TickEvent.ClientTickEvent event) {
		if (done || event.phase != TickEvent.Phase.END || !Boolean.getBoolean("railward.clientProbe")
			|| !"ponder_cistern".equals(System.getProperty("railward.clientProbeMode"))) return;
		Minecraft mc = Minecraft.getInstance();
		require(++ticks < 20 * 240, "timeout");
		if (scenes == null) {
			if (mc.level == null || mc.player == null || mc.getOverlay() != null) return;
			for (ResourceLocation id : List.of(AllBlocks.CISTERN.getId(), AllBlocks.CISTERN_BLUE.getId(),
				AllBlocks.CISTERN_LIGHT_BLUE.getId(), AllBlocks.CISTERN_BLACK.getId(), AllBlocks.CISTERN_LIGHT_GRAY.getId(),
				AllBlocks.CISTERN_YELLOW.getId(), AllBlocks.CISTERN_ORANGE.getId())) {
				List<PonderScene> registered = compile(id);
				require(registered.size() == 2, "two storyboards required for " + id);
				for (int index = 0; index < 2; index++) {
					PonderScene scene = registered.get(index);
					require(!scene.getTitle().startsWith("railward.ponder."), "untranslated title");
					for (int i = 1; i <= (index == 0 ? 7 : 4); i++)
						require(!scene.getString("text_" + i).startsWith("railward.ponder."), "untranslated caption");
				}
				if (scenes == null) scenes = registered;
			}
			check(scenes.get(0), false);
			check(scenes.get(1), true);
			mc.options.hideGui = true;
			open(mc);
		}
		mc.getToasts().clear();
		if (++frame == 25)
			Screenshot.grab(mc.gameDirectory, "cistern-ponder-" + preview + ".png", mc.getMainRenderTarget(),
				message -> LogUtils.getLogger().info("Railward CISTERN-PONDER screenshot: {}", message.getString()));
		if (frame < 45) return;
		if (++preview < TIMES.length) { open(mc); return; }
		done = true;
		LogUtils.getLogger().info("Railward CISTERN-PONDER client probe passed: 14 registrations, 7+4 localized captions, "
			+ "1/3/5 diameters, full sections, 11-block length, separate colours, both axes, real pipe capability rules, "
			+ "standard isometry, 11 previews");
		mc.stop();
	}

	private static List<PonderScene> compile(ResourceLocation id) {
		//? if create6 {
		/*return PonderIndex.getSceneAccess().compile(id);
		*///?} else {
		return PonderRegistry.compile(id);
		//?}
	}

	private static void check(PonderScene scene, boolean connections) {
		for (int tick = 1; tick <= (connections ? 850 : 1650); tick++) {
			scene.tick();
			if (!connections) {
				if (tick == 100) dimensions(scene, CisternScenes.SMALL, 1, 5, Direction.Axis.X);
				if (tick == 350) dimensions(scene, CisternScenes.SMALL.east(4), 3, 1, Direction.Axis.X);
				if (tick == 600) dimensions(scene, CisternScenes.SMALL, 3, 5, Direction.Axis.X);
				if (tick == 820) dimensions(scene, CisternScenes.LARGE, 5, 4, Direction.Axis.X);
				if (tick == 1050) dimensions(scene, CisternScenes.THIN, 1, 11, Direction.Axis.X);
				if (tick == 1270) {
					BlockPos green = new BlockPos(3, 1, 5), blue = green.east(3);
					dimensions(scene, green, 3, 3, Direction.Axis.X);
					dimensions(scene, blue, 3, 3, Direction.Axis.X);
					require(scene.getWorld().getBlockState(green).getBlock() != scene.getWorld().getBlockState(blue).getBlock(), "colours merged");
				}
				if (tick == 1480) dimensions(scene, new BlockPos(5, 1, 4), 3, 5, Direction.Axis.Z);
			} else {
				if (tick == 100) {
					BlockPos p = CisternScenes.SMALL;
					dimensions(scene, p, 3, 5, Direction.Axis.X);
					port(scene, p.offset(4, 1, 1), Direction.EAST, true);
					port(scene, p.offset(4, 0, 0), Direction.EAST, false);
					port(scene, p.offset(2, 1, 2), Direction.SOUTH, true);
					port(scene, p.offset(2, 1, 0), Direction.NORTH, true);
					port(scene, p.offset(0, 1, 2), Direction.SOUTH, false);
					port(scene, p.offset(2, 2, 1), Direction.UP, true);
					port(scene, p.offset(2, 0, 1), Direction.DOWN, true);
					port(scene, p.offset(2, 2, 0), Direction.UP, false);
				}
				if (tick == 480) {
					BlockPos p = CisternScenes.LARGE;
					dimensions(scene, p, 5, 5, Direction.Axis.X);
					for (int y = 0; y < 5; y++) for (int z = 0; z < 5; z++)
						port(scene, p.offset(0, y, z), Direction.WEST, y >= 1 && y <= 3 && z >= 1 && z <= 3);
				}
				if (tick == 710) {
					BlockPos p = new BlockPos(4, 1, 6);
					dimensions(scene, p, 1, 5, Direction.Axis.X);
					port(scene, p, Direction.WEST, true);
					port(scene, p.east(4), Direction.EAST, true);
					port(scene, p.east(2), Direction.UP, false);
				}
			}
		}
		require(scene.isFinished(), "unfinished storyboard");
		require(Math.abs(scene.getTransform().xRotation.getValue(1) + 35) < .001
			&& Math.abs(scene.getTransform().yRotation.getValue(1) - 145) < .001, "standard isometry changed");
	}

	private static void dimensions(PonderScene scene, BlockPos p, int width, int length, Direction.Axis axis) {
		var be = scene.getWorld().getBlockEntity(p);
		require(be instanceof CisternBlockEntity, "missing controller at " + p);
		var root = (CisternBlockEntity) be;
		require(root.isController() && root.getCisternWidth() == width && root.getCisternLength() == length
			&& root.getCisternAxis() == axis, "wrong dimensions at " + p);
		BlockPos end = p.offset(axis == Direction.Axis.X ? length - 1 : width - 1, width - 1,
			axis == Direction.Axis.Z ? length - 1 : width - 1);
		for (BlockPos cell : BlockPos.betweenClosed(p, end))
			require(scene.getWorld().getBlockEntity(cell) instanceof CisternBlockEntity part
				&& part.getControllerBE() == root, "missing cell or stale controller at " + cell);
	}

	private static void port(PonderScene scene, BlockPos p, Direction side, boolean expected) {
		var be = (CisternBlockEntity) scene.getWorld().getBlockEntity(p);
		require(be.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, side).isPresent() == expected,
			"incorrect pipe access at " + p + " / " + side);
	}

	private static void open(Minecraft mc) {
		active = scenes.get(preview < 7 ? 0 : 1);
		active.begin();
		mc.setScreen(new PonderUI(List.of(active)) {});
		active.seekToTime(TIMES[preview]);
		frame = 0;
	}

	private static void require(boolean condition, String message) {
		if (!condition) throw new IllegalStateException("Railward CISTERN-PONDER: " + message);
	}
}
