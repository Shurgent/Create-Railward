package ua.pp.shurgent.railward.clienttest;

import java.util.List;
import com.mojang.logging.LogUtils;
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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ua.pp.shurgent.railward.AllBlocks;
import ua.pp.shurgent.railward.Railward;
import ua.pp.shurgent.railward.block.train.parts.BulkCargoContainerBlockEntity;
import ua.pp.shurgent.railward.block.train.parts.BulkCargoStorage;
import ua.pp.shurgent.railward.block.train.station.UnloadingHatchBlockEntity;
import ua.pp.shurgent.railward.block.train.station.UnloadingBunkerBlockEntity;
import ua.pp.shurgent.railward.client.ponder.BulkCargoScenes;

/** Opt-in bulk tutorial playback and real-rendering regression; never packaged. */
@Mod.EventBusSubscriber(modid = Railward.MOD_ID, value = Dist.CLIENT)
public final class RailwardBulkPonderClientProbe {

	private static List<PonderScene> scenes;
	private static PonderScene active;
	private static int ticks, preview, frame;
	private static boolean done, sawFall;
	private static final int[] TIMES = {280, 700, 950, 1152, 1332, 1522, 470, 1000, 1300};

	@SubscribeEvent
	public static void tick(TickEvent.ClientTickEvent event) {
		if (done || event.phase != TickEvent.Phase.END || !Boolean.getBoolean("railward.clientProbe")
			|| !"ponder_bulk".equals(System.getProperty("railward.clientProbeMode"))) return;
		Minecraft mc = Minecraft.getInstance();
		require(++ticks < 20 * 240, "timeout");
		if (scenes == null) {
			if (mc.level == null || mc.player == null || mc.getOverlay() != null) return;
			int registrations = 0;
			for (ResourceLocation item : List.of(AllBlocks.BULK_CARGO_CONTAINER.getId(), AllBlocks.BULK_CARGO_CONTAINER_BLUE.getId(),
				AllBlocks.BULK_CARGO_CONTAINER_LIGHT_BLUE.getId(), AllBlocks.UNLOADING_HATCH.getId(), AllBlocks.UNLOADING_BUNKER.getId())) {
				var registered = compile(item);
				require(registered.size() == (item.equals(AllBlocks.UNLOADING_HATCH.getId()) || item.equals(AllBlocks.UNLOADING_BUNKER.getId()) ? 1 : 2), "registration count for " + item);
				registrations += registered.size();
				for (var scene : registered) {
					require(!scene.getTitle().startsWith("railward.ponder."), "untranslated title");
					int count = scene.getTitle().equals(registered.get(0).getTitle()) && registered.size() == 2 ? 8 : 6;
					for (int i = 1; i <= count; i++) require(!scene.getString("text_" + i).startsWith("railward.ponder."), "untranslated caption");
				}
			}
			require(registrations == 8, "need all eight component/storyboard registrations");
			scenes = compile(AllBlocks.BULK_CARGO_CONTAINER.getId());
			checkStorage(scenes.get(0));
			checkTransport(scenes.get(1));
			mc.options.hideGui = true;
			open(mc);
		}
		mc.getToasts().clear();
		frame++;
		if (preview >= 6) {
			require(amount(active, BulkCargoScenes.WAGON) + amount(active, BulkCargoScenes.SUPPLY)
				+ amount(active, BulkCargoScenes.OUTPUT) == BulkCargoScenes.TRAIN_AMOUNT, "rendered cargo conservation");
			int[] entities = {0};
			active.forEachWorldEntity(ItemEntity.class, item -> { if (item.isAlive()) entities[0]++; });
			require(entities[0] == 0, "wagon transfers must use their native falling-item renderer");
			if (preview == 6) sawFall |= !((UnloadingHatchBlockEntity) active.getWorld().getBlockEntity(BulkCargoScenes.LOADER)).getFallingItems().isEmpty();
			if (preview == 7) sawFall |= !((UnloadingBunkerBlockEntity) active.getWorld().getBlockEntity(BulkCargoScenes.BUNKER)).getFallingItems().isEmpty();
		}
		if (frame == (preview == 3 ? 10 : 25))
			Screenshot.grab(mc.gameDirectory, "bulk-ponder-" + preview + ".png", mc.getMainRenderTarget(), message -> LogUtils.getLogger().info("Railward BULK-PONDER screenshot: {}", message.getString()));
		if (frame < 45) return;
		if (preview == 6 || preview == 7) require(sawFall, "missing native falling cargo at preview " + preview);
		if (++preview < TIMES.length) { open(mc); return; }
		done = true;
		LogUtils.getLogger().info("Railward BULK-PONDER client probe passed: 8 registrations, 8+6 localized captions, rectangular/square/tall storage, separate colours, 1152 dropped, 64 right-click extraction, hopper input/output, 54 normalized wagon cells, locomotive-led single section, conserved 46656 items, native loading/unloading effects, 9 previews");
		mc.stop();
	}

	private static void checkStorage(PonderScene scene) {
		int peak = 0;
		for (int i = 1; i <= 1800; i++) {
			scene.tick();
			if (i == 20) dimensions(scene, BulkCargoScenes.EXAMPLE, 1, 1, 1);
			if (i == 250) dimensions(scene, BulkCargoScenes.EXAMPLE, 3, 1, 3);
			if (i == 450) dimensions(scene, BulkCargoScenes.EXAMPLE, 5, 1, 2);
			if (i == 700) dimensions(scene, BulkCargoScenes.EXAMPLE, 3, 3, 3);
			if (i == 900) {
				dimensions(scene, BulkCargoScenes.EXAMPLE, 3, 2, 2);
				dimensions(scene, BulkCargoScenes.EXAMPLE.east(3), 2, 2, 2);
				require(scene.getWorld().getBlockState(BulkCargoScenes.EXAMPLE).getBlock() != scene.getWorld().getBlockState(BulkCargoScenes.EXAMPLE.east(3)).getBlock(), "colours merged");
			}
			if (i == 1262) require(amount(scene, BulkCargoScenes.HAND_CONTAINER) == 1152, "thrown stacks not collected");
			if (i == 1372) require(amount(scene, BulkCargoScenes.HAND_CONTAINER) == 1088, "right-click must take exactly 64");
			if (i >= 1442) peak = Math.max(peak, amount(scene, BulkCargoScenes.HAND_CONTAINER));
		}
		require(scene.isFinished(), "storage scene not finished");
		require(peak == 1600 && amount(scene, BulkCargoScenes.HAND_CONTAINER) == 1088, "hopper input/output failed");
		require(amount(scene, BulkCargoScenes.HAND_SOURCE) == 0 && amount(scene, BulkCargoScenes.HAND_OUTPUT) == 512, "hopper destination count");
		camera(scene);
	}

	private static void checkTransport(PonderScene scene) {
		dimensions(scene, BulkCargoScenes.WAGON, 9, 2, 3);
		var root = (BulkCargoContainerBlockEntity) scene.getWorld().getBlockEntity(BulkCargoScenes.WAGON);
		for (BlockPos pos : BlockPos.betweenClosed(BulkCargoScenes.WAGON, BulkCargoScenes.WAGON.offset(8, 1, 2)))
			require(((BulkCargoContainerBlockEntity) scene.getWorld().getBlockEntity(pos)).getControllerBE() == root, "original-world controller reference");
		int maximum = 0;
		for (int i = 1; i <= 1600; i++) {
			scene.tick();
			if (i > 230) require(amount(scene, BulkCargoScenes.WAGON) + amount(scene, BulkCargoScenes.SUPPLY)
				+ amount(scene, BulkCargoScenes.OUTPUT) == BulkCargoScenes.TRAIN_AMOUNT, "lost/duplicated train cargo");
			maximum = Math.max(maximum, amount(scene, BulkCargoScenes.WAGON));
		}
		require(scene.isFinished() && maximum == BulkCargoScenes.TRAIN_AMOUNT, "wagon failed to load");
		require(amount(scene, BulkCargoScenes.WAGON) == 0 && amount(scene, BulkCargoScenes.OUTPUT) == maximum, "wagon failed to unload");
		require(BulkCargoScenes.handler(scene.getWorld().getBlockEntity(BulkCargoScenes.BUNKER)).getSlots() == 0, "bunker remained connected after departure");
		int[] moving = {0};
		scene.forEach(WorldSectionElement.class, section -> { if (Math.abs(section.getAnimatedOffset().x - 8) < .001) moving[0]++; });
		require(moving[0] == 1, "wagon must move with its locomotive");
		camera(scene);
	}

	private static void dimensions(PonderScene scene, BlockPos pos, int x, int y, int z) {
		var be = (BulkCargoContainerBlockEntity) scene.getWorld().getBlockEntity(pos);
		require(be != null && be.getSizeX() == x && be.getHeight() == y && be.getSizeZ() == z, "wrong multiblock at " + pos);
	}

	private static void camera(PonderScene scene) {
		require(Math.abs(scene.getTransform().xRotation.getValue(1) + 35) < .001 && Math.abs(scene.getTransform().yRotation.getValue(1) - 145) < .001, "keep standard isometry");
	}

	private static int amount(PonderScene scene, BlockPos pos) {
		return BulkCargoStorage.getStoredAmount(BulkCargoScenes.handler(scene.getWorld().getBlockEntity(pos)));
	}

	private static List<PonderScene> compile(ResourceLocation item) {
		//? if create6 {
		/*return PonderIndex.getSceneAccess().compile(item);
		*///?} else {
		return PonderRegistry.compile(item);
		//?}
	}

	private static void open(Minecraft mc) {
		active = scenes.get(preview < 6 ? 0 : 1);
		active.begin();
		mc.setScreen(new PonderUI(List.of(active)) {});
		active.seekToTime(TIMES[preview]);
		frame = 0;
		sawFall = false;
	}

	private static void require(boolean value, String message) {
		if (!value) throw new IllegalStateException("Railward BULK-PONDER: " + message);
	}
}
