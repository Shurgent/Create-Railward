package ua.pp.shurgent.railward.clienttest;

import java.util.List;

import com.mojang.logging.LogUtils;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
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
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import ua.pp.shurgent.railward.AllBlocks;
import ua.pp.shurgent.railward.Railward;
import ua.pp.shurgent.railward.block.train.parts.CisternBlockEntity;
import ua.pp.shurgent.railward.block.train.station.CisternHatchBlockEntity;
import ua.pp.shurgent.railward.block.train.station.FluidTerminalBlockEntity;
import ua.pp.shurgent.railward.client.ponder.FluidTransferScenes;

/** Opt-in real-client scene/asset/animation regression; excluded from release JARs. */
@Mod.EventBusSubscriber(modid = Railward.MOD_ID, value = Dist.CLIENT)
public final class RailwardFluidPonderClientProbe {

	private static List<PonderScene> scenes;
	private static PonderScene active;
	private static int preview;
	private static int frame;
	private static int ticks;
	private static boolean captured;
	private static boolean complete;
	private static int previousFluid;
	private static final int[] TIMES = {400, 330, 430, 700, 760, 850, 660, 50};

	@SubscribeEvent
	public static void onClientTick(TickEvent.ClientTickEvent event) {
		if (complete || event.phase != TickEvent.Phase.END || !Boolean.getBoolean("railward.clientProbe")
			|| !"ponder_fluid".equals(System.getProperty("railward.clientProbeMode")))
			return;
		Minecraft mc = Minecraft.getInstance();
		require(++ticks < 20 * 180, "client probe timed out");
		if (scenes == null) {
			if (mc.level == null || mc.player == null || mc.getOverlay() != null)
				return;
			scenes = compile(AllBlocks.CISTERN_HATCH.getId());
			var terminalScenes = compile(AllBlocks.FLUID_TERMINAL.getId());
			require(scenes.size() == 2 && terminalScenes.size() == 2, "both items need both tutorials");
			for (var scene : List.of(scenes.get(0), scenes.get(1), terminalScenes.get(0), terminalScenes.get(1)))
				checkPlayback(scene);
			var emptySupply = compile(AllBlocks.FLUID_TERMINAL.getId()).get(1);
			((FluidTank) ((FluidTankBlockEntity) emptySupply.getWorld().getBlockEntity(FluidTransferScenes.RESERVOIR))
				.getTankInventory()).setFluid(FluidStack.EMPTY);
			checkPlayback(emptySupply);
			mc.options.hideGui = true;
			open(mc);
		}
		mc.getToasts().clear();
		frame++;
		var world = active.getWorld();
		var hatch = (CisternHatchBlockEntity) world.getBlockEntity(FluidTransferScenes.HATCH);
		var terminal = (FluidTerminalBlockEntity) world.getBlockEntity(FluidTransferScenes.TERMINAL);
		int cistern = amount(active, FluidTransferScenes.CISTERN);
		require(cistern + amount(active, FluidTransferScenes.RESERVOIR) == 16000, "fluid conservation during UI playback");
		if (preview == 1) {
			require(cistern >= previousFluid, "loading reversed unexpectedly");
			previousFluid = cistern;
		} else if (preview == 2) {
			require(cistern <= previousFluid, "unloading reversed unexpectedly");
			previousFluid = cistern;
		}
		if (frame >= 15) {
			boolean connected = preview <= 2 || preview == 4;
			require(terminal.isConnectedToHatch() == connected, "wrong rendered connection state at preview " + preview);
			require(Math.abs(terminal.getHoseExtensionDistance(1) - (connected ? 2 : 0)) < .01,
				"wrong rendered hose extension at preview " + preview);
			require(Math.abs(hatch.getLidProgress(1) - (connected ? 1 : 0)) < .01,
				"wrong rendered hatch lid at preview " + preview);
			require(terminal.isPowered() == (preview == 3), "wrong redstone state at preview " + preview);
			require(world.getBlockState(FluidTransferScenes.TERMINAL)
				.getAnalogOutputSignal(world, FluidTransferScenes.TERMINAL) == (connected ? 15 : 0),
				"wrong comparator output at preview " + preview);
		}
		if (frame == 25 && !captured) {
			captured = true;
			Screenshot.grab(mc.gameDirectory, "fluid-ponder-" + preview + ".png", mc.getMainRenderTarget(),
				message -> LogUtils.getLogger().info("Railward FLUID-PONDER screenshot: {}", message.getString()));
		}
		boolean advance = preview == 1 ? active.getCurrentTime() >= 410
			: preview == 2 ? active.getCurrentTime() >= 510 : frame >= 45;
		if (!advance)
			return;
		if (preview == 1) require(cistern == 8000, "loading did not reach 8 buckets");
		if (preview == 2) require(cistern == 0, "unloading did not return all water");
		if (++preview < TIMES.length) {
			open(mc);
			return;
		}
		complete = true;
		LogUtils.getLogger().info("Railward FLUID-PONDER client probe passed: 4 registrations, relocated user schematics, "
			+ "81 cistern parts sharing one controller, localized captions, conserved 16000 mB, 8000 mB load/unload, "
			+ "empty-supply no-op, locomotive-led single moving section, four animated bogeys, "
			+ "standard isometric camera, rear-facing cab, supply behind wagon, "
			+ "rendered lid/hose, redstone disconnect/reconnect, comparator 15/0, closed departure, 8 previews");
		mc.stop();
	}

	private static void checkPlayback(PonderScene scene) {
		boolean hatch = scene.getTitle().equals(I18n.get("railward.ponder.cistern_hatch.header"));
		require(!scene.getTitle().startsWith("railward.ponder."), "untranslated title");
		for (int i = 1; i <= 5; i++)
			require(!scene.getString("text_" + i).startsWith("railward.ponder."), "untranslated text " + i);
		var controller = (CisternBlockEntity) scene.getWorld().getBlockEntity(FluidTransferScenes.CISTERN);
		var world = scene.getWorld();
		require(AllBlocks.MODERN_TRAIN_CONTROLLER.has(world.getBlockState(new BlockPos(19, 3, 8)))
			&& world.getBlockState(new BlockPos(19, 3, 8)).getValue(BlockStateProperties.HORIZONTAL_FACING)
				== Direction.WEST, "locomotive cab must be at the rear, next to the wagon");
		require(com.simibubi.create.AllBlocks.MECHANICAL_PUMP.has(world.getBlockState(FluidTransferScenes.PUMP))
			&& FluidTransferScenes.PUMP.getZ() > 8 && FluidTransferScenes.PUMP.getX() < 4
			&& FluidTransferScenes.RESERVOIR.getZ() > 8,
			"supply must sit behind the wagon, with its pump beyond the wagon end");
		require(world.getBlockState(FluidTransferScenes.LEVER.below()).is(Blocks.STONE)
			&& world.getBlockState(FluidTransferScenes.COMPARATOR.below()).is(Blocks.STONE), "missing flanking controls");
		require(world.getBlockState(FluidTransferScenes.COMPARATOR).getValue(BlockStateProperties.HORIZONTAL_FACING)
			== Direction.EAST, "comparator must read the terminal from screen-left");
		for (int x = 0; x < 14; x++)
			for (int y = 1; y <= 9; y++)
				for (int z = 0; z < 12; z++)
					require(!com.simibubi.create.AllBlocks.INDUSTRIAL_IRON_BLOCK.has(world.getBlockState(new BlockPos(x, y, z))),
						"industrial-iron support remains in the station");
		require(controller.getCisternWidth() == 3 && controller.getCisternLength() == 9, "wrong cistern dimensions");
		for (int x = 4; x <= 12; x++)
			for (int y = 3; y <= 5; y++)
				for (int z = 6; z <= 8; z++) {
					var part = (CisternBlockEntity) scene.getWorld().getBlockEntity(new BlockPos(x, y, z));
					require(part.getControllerBE() == controller, "stale original-world controller reference");
				}
		int maximum = 0;
		int total = amount(scene, FluidTransferScenes.CISTERN) + amount(scene, FluidTransferScenes.RESERVOIR);
		for (int i = 0; i < 1600; i++) {
			scene.tick();
			int wagon = amount(scene, FluidTransferScenes.CISTERN);
			require(wagon + amount(scene, FluidTransferScenes.RESERVOIR) == total, "lost or duplicated fluid");
			maximum = Math.max(maximum, wagon);
		}
		require(scene.isFinished(), "scene failed to finish");
		require(Math.abs(scene.getTransform().xRotation.getValue(1) + 35) < .001
			&& Math.abs(scene.getTransform().yRotation.getValue(1) - 145) < .001,
			"fluid scenes must retain the standard isometric camera");
		require(maximum == (hatch ? 0 : Math.min(8000, total)), "wrong maximum loaded amount");
		require(amount(scene, FluidTransferScenes.CISTERN) == 0, "wagon did not finish empty");
		require(!((FluidTerminalBlockEntity) scene.getWorld().getBlockEntity(FluidTransferScenes.TERMINAL))
			.isConnectedToHatch(), "terminal remained connected at departure");
		int[] movingSections = {0};
		scene.forEach(WorldSectionElement.class, section -> {
			if (Math.abs(section.getAnimatedOffset().x - 6) < .001)
				movingSections[0]++;
		});
		require(movingSections[0] == 1, "locomotive and wagon must share a single moving section");
		for (int x : FluidTransferScenes.BOGEY_X) {
			var bogey = (AbstractBogeyBlockEntity) scene.getWorld().getBlockEntity(new BlockPos(x, 2, 7));
			require(Math.abs(bogey.getVirtualAngle(1)) > .01, "stationary bogey on moving consist: " + x);
		}
		require(scene.getWorld().getBlockState(new BlockPos(13, 2, 7)).is(AllBlocks.TRAIN_COUPLER.get())
			&& scene.getWorld().getBlockState(new BlockPos(14, 2, 7)).is(AllBlocks.TRAIN_COUPLER.get()),
			"wagon and locomotive couplers are not adjacent");
	}

	private static int amount(PonderScene scene, BlockPos pos) {
		return ((FluidTankBlockEntity) scene.getWorld().getBlockEntity(pos)).getTankInventory().getFluidAmount();
	}

	private static List<PonderScene> compile(ResourceLocation item) {
		//? if create6 {
		/*return PonderIndex.getSceneAccess().compile(item);
		*///?} else {
		return PonderRegistry.compile(item);
		//?}
	}

	private static void open(Minecraft mc) {
		active = scenes.get(preview == 0 || preview >= 6 ? 0 : 1);
		active.begin();
		mc.setScreen(new PreviewScreen(active));
		active.seekToTime(TIMES[preview]);
		frame = 0;
		captured = false;
		previousFluid = amount(active, FluidTransferScenes.CISTERN);
	}

	private static final class PreviewScreen extends PonderUI {
		private PreviewScreen(PonderScene scene) {
			super(List.of(scene));
		}
	}

	private static void require(boolean condition, String message) {
		if (!condition) throw new IllegalStateException("Railward FLUID-PONDER: " + message);
	}
}
