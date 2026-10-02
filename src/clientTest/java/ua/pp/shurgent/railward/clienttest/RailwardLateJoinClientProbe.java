package ua.pp.shurgent.railward.clienttest;

import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.mojang.logging.LogUtils;
import com.simibubi.create.CreateClient;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.trains.entity.CarriageContraption;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.TrainHUD;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import ua.pp.shurgent.railward.Railward;
import ua.pp.shurgent.railward.RailwardPackets;
import ua.pp.shurgent.railward.block.train.parts.BulkCargoContainerBlock;
import ua.pp.shurgent.railward.block.train.parts.BulkCargoContainerBlockEntity;
import ua.pp.shurgent.railward.block.train.parts.CabSignalAspect;
import ua.pp.shurgent.railward.block.train.parts.CabSignalBlock;
import ua.pp.shurgent.railward.block.train.parts.CabSignalTrainState;
import ua.pp.shurgent.railward.block.train.parts.CisternBlock;
import ua.pp.shurgent.railward.block.train.parts.CisternContraptionHelper;
import ua.pp.shurgent.railward.block.train.parts.LocomotiveBufferLightsBlock;
import ua.pp.shurgent.railward.block.train.parts.LocomotiveBufferLightsVariant;
import ua.pp.shurgent.railward.block.train.parts.ModernTrainControllerBlock;
import ua.pp.shurgent.railward.block.train.parts.TrainDirectionState;
import ua.pp.shurgent.railward.block.train.parts.TrainDirectionState.DirectionState;
import ua.pp.shurgent.railward.block.train.parts.TrainCouplerBlock;
import ua.pp.shurgent.railward.block.train.parts.door.TrainDoorBlock;
import ua.pp.shurgent.railward.block.train.station.UnloadingBunkerBlockEntity;
import ua.pp.shurgent.railward.block.train.station.UnloadingHatchBlockEntity;
import ua.pp.shurgent.railward.client.sound.UnloadingBulkCargoAudio;
import ua.pp.shurgent.railward.compat.RailwardStructureInfo;
import ua.pp.shurgent.railward.compat.RailwardComponents;
import ua.pp.shurgent.railward.network.ModernTrainControllerReverserPacket;

@Mod.EventBusSubscriber(modid = Railward.MOD_ID, value = Dist.CLIENT)
public final class RailwardLateJoinClientProbe {

	private static final String ENABLED_PROPERTY = "railward.clientProbe";
	private static final String MODE_PROPERTY = "railward.clientProbeMode";
	private static final String TRAIN_NAME = "Railward SAVE-01 probe";
	private static final String AUTO_DISABLED_MOVER = "Railward COUPLE-02 disabled mover";
	private static final String AUTO_DISABLED_TARGET = "Railward COUPLE-02 disabled target";
	private static final String AUTO_FAST_MOVER = "Railward COUPLE-02 fast mover";
	private static final String AUTO_FAST_TARGET = "Railward COUPLE-02 fast target";
	private static final String AUTO_WEST_MOVER = "Railward COUPLE-02 west mover";
	private static final String AUTO_WEST_TARGET = "Railward COUPLE-02 west target";
	private static final String AUTO_EAST_MOVER = "Railward COUPLE-02 east mover";
	private static final String AUTO_EAST_TARGET = "Railward COUPLE-02 east target";
	private static final String STORAGE_COUPLER_FINAL_SCHEDULED = "Railward COUPLE-03 scheduled";
	private static final String STORAGE_COUPLER_FINAL_UNSCHEDULED = "Railward COUPLE-03 unscheduled";
	private static final Set<StorageSignature> STORAGE_SIGNATURES = Set.of(
		new StorageSignature(11, 1100), new StorageSignature(22, 1200),
		new StorageSignature(33, 1300), new StorageSignature(44, 1400));
	private static final int TIMEOUT_TICKS = 20 * 90;
	private static final int PERSISTED_CARGO_CAPACITY = BulkCargoContainerBlockEntity.STACKS_PER_BLOCK * 64;
	private static final BlockPos CARGO_UNLOAD_DIRECT_CARGO = new BlockPos(0, 100, 0);
	private static final BlockPos CARGO_UNLOAD_DIRECT_HATCH = CARGO_UNLOAD_DIRECT_CARGO.above(2);
	private static final BlockPos CARGO_UNLOAD_BUNKER = new BlockPos(5, 100, 0);
	private static final BlockPos CARGO_UNLOAD_BLOCKED_CARGO = new BlockPos(10, 100, 0);
	private static final Logger LOGGER = LogUtils.getLogger();

	private static int ticks;
	private static boolean complete;
	private static boolean multiClientInitialStateSeen;
	private static Vec3 multiClientInitialPosition;
	private static int controllerProbeStage;
	private static int controllerProbeReadyTicks;
	private static boolean controllerProbeActionSent;
	private static Vec3 controllerProbeForwardPosition;
	private static int scheduledProbeStage;
	private static int scheduledRouteProbeStage;
	private static Vec3 scheduledRouteStartPosition;
	private static DirectionState scheduledRouteDirection = DirectionState.NONE;
	private static int couplerProbeStage;
	private static Set<UUID> couplerInitialTrainIds = Set.of();
	private static UUID couplerCombinedTrainId;
	private static int autoCouplerProbeStage;
	private static Set<UUID> autoCouplerInitialTrainIds = Set.of();
	private static int storageCouplerProbeStage;
	private static int storageCouplerReconnectTicks;
	private static Set<UUID> storageCouplerInitialTrainIds = Set.of();
	private static UUID storageCouplerCombinedTrainId;
	private static Set<UUID> storageCouplerEntityIds = Set.of();
	private static Set<UUID> storageCouplerPassengerIds = Set.of();
	private static boolean cargoUnloadingHatchVisualSeen;
	private static boolean cargoUnloadingHatchAudioSeen;
	private static boolean cargoUnloadingBunkerVisualSeen;
	private static boolean cargoUnloadingBunkerAudioSeen;
	private static final Set<CabSignalAspect> signalAspectsSeen = new HashSet<>();
	private static boolean signalLowLimitSeen;
	private static boolean signalHighLimitSeen;
	private static boolean signalForwardSeen;
	private static boolean signalBackwardSeen;
	private static boolean signalHonkActive;
	private static int signalHonkEvents;
	private static Vec3 signalStartPosition;
	private static double signalMaxDistanceSqr;
	private static int signalReturnStableTicks;

	private RailwardLateJoinClientProbe() {
	}

	@SubscribeEvent
	public static void onClientTick(TickEvent.ClientTickEvent event) {
		if (complete || event.phase != TickEvent.Phase.END || !Boolean.getBoolean(ENABLED_PROPERTY))
			return;
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.level == null || minecraft.player == null)
			return;
		String mode = System.getProperty(MODE_PROPERTY, "late_join");
		boolean cargoUnloadingProbe = "cargo_unloading".equals(mode);
		if (!cargoUnloadingProbe && CreateClient.RAILWAYS == null)
			return;
		boolean multiClient = "multi_client".equals(mode);
		boolean controllerProbe = "controller".equals(mode);
		boolean scheduledProbe = "scheduled".equals(mode);
		boolean scheduledRouteProbe = "scheduled_route".equals(mode);
		boolean signalProbe = "signal".equals(mode);
		boolean couplerProbe = "coupler".equals(mode);
		boolean autoCouplerProbe = "coupler_auto".equals(mode);
		boolean storageCouplerProbe = "coupler_storage".equals(mode);
		String testId = cargoUnloadingProbe ? "CARGO-03"
			: storageCouplerProbe ? "COUPLE-03" : autoCouplerProbe ? "COUPLE-02"
			: couplerProbe ? "COUPLE-01"
			: signalProbe ? "SIGNAL-01"
			: scheduledRouteProbe ? "CTRL-03 route" : scheduledProbe ? "CTRL-03"
			: controllerProbe ? "CTRL-01/CTRL-02"
			: multiClient ? "NET-02" : "NET-01";
		int timeoutTicks = storageCouplerProbe ? 20 * 300 : signalProbe ? 20 * 180
			: scheduledRouteProbe ? 20 * 150 : TIMEOUT_TICKS;
		if (++ticks > timeoutTicks)
			throw new IllegalStateException("Railward " + testId + " timed out waiting for the probe state");
		if (cargoUnloadingProbe) {
			advanceCargoUnloadingProbe(minecraft);
			return;
		}
		if (couplerProbe) {
			advanceCouplerProbe(minecraft);
			return;
		}
		if (autoCouplerProbe) {
			advanceAutoCouplerProbe(minecraft);
			return;
		}
		if (storageCouplerProbe) {
			advanceStorageCouplerProbe(minecraft);
			return;
		}

		List<Train> matches = CreateClient.RAILWAYS.trains.values().stream()
			.filter(train -> TRAIN_NAME.equals(train.name.getString()))
			.toList();
		if (matches.size() != 1) {
			logWaiting("persisted train metadata", matches.size(), null, null);
			return;
		}
		Train train = matches.get(0);
		CarriageContraptionEntity entity = findEntity(minecraft, train);
		if (entity == null || !(entity.getContraption() instanceof CarriageContraption)) {
			logWaiting("tracked carriage entity", matches.size(), entity, null);
			return;
		}
		DirectionState direction = TrainDirectionState.resolveSynced(entity);
		if (!multiClient && !controllerProbe && !scheduledProbe && !scheduledRouteProbe && !signalProbe
			&& direction != DirectionState.BACKWARD) {
			logWaiting("server-authoritative direction", matches.size(), entity, direction);
			return;
		}

		Contraption contraption = entity.getContraption();
		StructureBlockInfo cargo = findBlock(contraption, BulkCargoContainerBlock.class);
		StructureBlockInfo cistern = findBlock(contraption, CisternBlock.class);
		StructureBlockInfo controller = findBlock(contraption, ModernTrainControllerBlock.class);
		StructureBlockInfo bufferLights = findBlock(contraption, LocomotiveBufferLightsBlock.class);
		StructureBlockInfo cabSignal = findBlock(contraption, CabSignalBlock.class);
		List<StructureBlockInfo> trainDoors = findBlocks(contraption, TrainDoorBlock.class);
		if (cargo == null || cistern == null || controller == null || bufferLights == null
			|| signalProbe && cabSignal == null
			|| multiClient && trainDoors.size() != 2) {
			if (ticks % 200 == 0)
				LOGGER.info("Railward {} client probe waiting for custom blocks: cargo={}, cistern={}, controller={}, bufferLights={}, trainDoorHalves={}",
					testId, cargo != null, cistern != null, controller != null, bufferLights != null,
					trainDoors.size());
			return;
		}

		BulkCargoContainerBlockEntity cargoView = contraption.presentBlockEntities.values().stream()
			.filter(BulkCargoContainerBlockEntity.class::isInstance)
			.map(BulkCargoContainerBlockEntity.class::cast)
			.filter(BulkCargoContainerBlockEntity::hasClientCargoView)
			.findFirst()
			.orElse(null);
		IFluidHandler fluids = CisternContraptionHelper.getFluidHandler(contraption,
			RailwardStructureInfo.pos(cistern));
		int itemCount = cargoView == null ? -1 : cargoView.getStoredAmount();
		int fluidAmount = fluids == null ? -1 : fluids.getFluidInTank(0).getAmount();
		BlockPos cargoPos = RailwardStructureInfo.pos(cargo);
		int cargoLevel = RailwardStructureInfo.state(cargo).getValue(BulkCargoContainerBlock.CARGO_LEVEL);
		boolean cargoControllerReady = cargoView != null && cargoView.isClientCargoViewFor(cargoPos)
			&& cargoView.getClientCargoLayer() == 0
			&& cargoView.getCapacityItems() == PERSISTED_CARGO_CAPACITY;
		if (controllerProbe) {
			advanceControllerProbe(minecraft, entity, direction, controller);
			return;
		}
		if (scheduledProbe) {
			advanceScheduledProbe(minecraft, entity, direction, controller, bufferLights);
			return;
		}
		if (scheduledRouteProbe) {
			advanceScheduledRouteProbe(minecraft, entity, direction, controller, bufferLights);
			return;
		}
		if (signalProbe) {
			advanceSignalProbe(minecraft, train, entity, direction);
			return;
		}
		if (multiClient) {
			if (!multiClientInitialStateSeen && direction == DirectionState.BACKWARD && itemCount == 96
				&& cargoView != null && cargoView.getDisplayStack().is(Items.IRON_INGOT)
				&& cargoControllerReady && cargoLevel == 1
				&& fluids != null && fluids.getFluidInTank(0).getFluid() == Fluids.WATER && fluidAmount == 1500
				&& RailwardStructureInfo.state(controller).getValue(ModernTrainControllerBlock.REVERSED)
				&& RailwardStructureInfo.state(bufferLights).getValue(LocomotiveBufferLightsBlock.VARIANT)
					== LocomotiveBufferLightsVariant.TYPE_3
				&& trainDoors.stream().allMatch(info ->
					RailwardStructureInfo.state(info).getValue(TrainDoorBlock.OPEN))) {
				multiClientInitialStateSeen = true;
				multiClientInitialPosition = entity.position();
				LOGGER.info("Railward NET-02 {} captured the initial shared train state", minecraft.player.getName().getString());
			}
			if (!multiClientInitialStateSeen || direction != DirectionState.FORWARD || cargoView == null
				|| itemCount != 112 || !cargoView.getDisplayStack().is(Items.IRON_INGOT)
				|| !cargoControllerReady || cargoLevel != 2 || fluids == null
				|| fluids.getFluidInTank(0).getFluid() != Fluids.WATER || fluidAmount != 2000
				|| RailwardStructureInfo.state(controller).getValue(ModernTrainControllerBlock.REVERSED)
				|| RailwardStructureInfo.state(controller).getValue(ModernTrainControllerBlock.ACTIVE)
				|| RailwardStructureInfo.state(controller).getValue(ModernTrainControllerBlock.THROTTLE)
				|| RailwardStructureInfo.state(controller).getValue(ModernTrainControllerBlock.BRAKING)
				|| RailwardStructureInfo.state(bufferLights).getValue(LocomotiveBufferLightsBlock.VARIANT)
					!= LocomotiveBufferLightsVariant.TYPE_4
				|| trainDoors.stream().anyMatch(info ->
					RailwardStructureInfo.state(info).getValue(TrainDoorBlock.OPEN))
				|| entity.position().distanceToSqr(multiClientInitialPosition) <= .01) {
				if (ticks % 200 == 0)
					LOGGER.info("Railward NET-02 {} waiting for shared update: initial={}, direction={}, cargo={}, cargoLevel={}, cargoController={}, fluid={} mB, doorOpen={}, moved={}",
						minecraft.player.getName().getString(), multiClientInitialStateSeen, direction, itemCount,
						cargoLevel, cargoControllerReady, fluidAmount, trainDoors.stream().anyMatch(info ->
							RailwardStructureInfo.state(info).getValue(TrainDoorBlock.OPEN)),
						multiClientInitialPosition != null
							&& entity.position().distanceToSqr(multiClientInitialPosition) > .01);
				return;
			}

			complete = true;
			LOGGER.info("Railward NET-02 {} passed: both snapshots, mounted cargo controller and fill 1 -> 2, movement, both train-door halves closed, FORWARD direction, forward idle controller, TYPE_4 lights, 112 iron ingots and 2000 mB water",
				minecraft.player.getName().getString());
			minecraft.stop();
			return;
		}

		if (cargoView == null || itemCount != 96 || !cargoView.getDisplayStack().is(Items.IRON_INGOT)
			|| !cargoControllerReady || cargoLevel != 1
			|| fluids == null || fluids.getFluidInTank(0).getFluid() != Fluids.WATER || fluidAmount != 1500) {
			if (ticks % 200 == 0)
				LOGGER.info("Railward NET-01 client probe waiting for storage state: cargo={}, iron={}, cargoLevel={}, cargoController={}, fluid={} mB, water={}",
					itemCount, cargoView != null && cargoView.getDisplayStack().is(Items.IRON_INGOT), cargoLevel,
					cargoControllerReady, fluidAmount,
					fluids != null && fluids.getFluidInTank(0).getFluid() == Fluids.WATER);
			return;
		}
		if (!RailwardStructureInfo.state(controller).getValue(ModernTrainControllerBlock.REVERSED)
			|| RailwardStructureInfo.state(bufferLights).getValue(LocomotiveBufferLightsBlock.VARIANT)
				!= LocomotiveBufferLightsVariant.TYPE_3) {
			if (ticks % 200 == 0)
				LOGGER.info("Railward NET-01 client probe waiting for custom block state: reversed={}, bufferVariant={}",
					RailwardStructureInfo.state(controller).getValue(ModernTrainControllerBlock.REVERSED),
					RailwardStructureInfo.state(bufferLights).getValue(LocomotiveBufferLightsBlock.VARIANT));
			return;
		}

		complete = true;
		LOGGER.info("Railward NET-01 client probe passed: train={}, direction={}, mounted cargo controller/fill restored, cargo={}, fluid={} mB, custom controller and buffer lights present",
			train.id, DirectionState.BACKWARD, cargoView.getStoredAmount(), fluids.getFluidInTank(0).getAmount());
		minecraft.stop();
	}

	private static void advanceCargoUnloadingProbe(Minecraft minecraft) {
		AABB fixtureBounds = new AABB(-2, 99, -2, 13, 105, 3);
		if (!minecraft.level.getEntitiesOfClass(ItemEntity.class, fixtureBounds).isEmpty())
			throw new IllegalStateException("CARGO-03 created client-side item entities for a visual-only transfer");

		var directHatchBlockEntity = minecraft.level.getBlockEntity(CARGO_UNLOAD_DIRECT_HATCH);
		var bunkerBlockEntity = minecraft.level.getBlockEntity(CARGO_UNLOAD_BUNKER);
		var directCargoBlockEntity = minecraft.level.getBlockEntity(CARGO_UNLOAD_DIRECT_CARGO);
		var blockedCargoBlockEntity = minecraft.level.getBlockEntity(CARGO_UNLOAD_BLOCKED_CARGO);
		if (!(directHatchBlockEntity instanceof UnloadingHatchBlockEntity directHatch)
			|| !(bunkerBlockEntity instanceof UnloadingBunkerBlockEntity bunker)
			|| !(directCargoBlockEntity instanceof BulkCargoContainerBlockEntity directCargo)
			|| !(blockedCargoBlockEntity instanceof BulkCargoContainerBlockEntity blockedCargo)) {
			if (ticks % 100 == 0)
				LOGGER.info("Railward CARGO-03 {} waiting for tracked unloading block entities",
					minecraft.player.getName().getString());
			return;
		}

		if (directHatch.getTransferDistance() == 2 && directHatch.getTransferStack().is(Items.IRON_INGOT)
			&& !directHatch.getFallingItems().isEmpty())
			cargoUnloadingHatchVisualSeen = true;
		if (UnloadingBulkCargoAudio.isLoopActive(directHatch))
			cargoUnloadingHatchAudioSeen = true;
		if (bunker.getTransferDistance() == 2 && bunker.getTransferStack().is(Items.IRON_INGOT)
			&& !bunker.getFallingItems().isEmpty())
			cargoUnloadingBunkerVisualSeen = true;
		if (UnloadingBulkCargoAudio.isLoopActive(bunker))
			cargoUnloadingBunkerAudioSeen = true;

		if (!cargoUnloadingHatchVisualSeen || !cargoUnloadingHatchAudioSeen
			|| !cargoUnloadingBunkerVisualSeen || !cargoUnloadingBunkerAudioSeen
			|| directCargo.getStoredAmount() != 80 || !directCargo.getDisplayStack().is(Items.IRON_INGOT)
			|| blockedCargo.getStoredAmount() != 0) {
			if (ticks % 100 == 0)
				LOGGER.info("Railward CARGO-03 {} waiting: hatchVisual={}, hatchAudio={}, bunkerVisual={}, bunkerAudio={}, directCargo={}, blockedCargo={}",
					minecraft.player.getName().getString(), cargoUnloadingHatchVisualSeen,
					cargoUnloadingHatchAudioSeen, cargoUnloadingBunkerVisualSeen,
					cargoUnloadingBunkerAudioSeen, directCargo.getStoredAmount(), blockedCargo.getStoredAmount());
			return;
		}

		complete = true;
		LOGGER.info("Railward CARGO-03 {} passed: hatch and bunker falling-item visuals/audio, exact direct cargo and no client item entities",
			minecraft.player.getName().getString());
		minecraft.stop();
	}

	private static void advanceCouplerProbe(Minecraft minecraft) {
		List<Train> trains = List.copyOf(CreateClient.RAILWAYS.trains.values());
		if (ticks % 40 == 0)
			LOGGER.info("Railward COUPLE-01 {} stage {} topology {}",
				minecraft.player.getName().getString(), couplerProbeStage, describeClientTopology(trains));

		switch (couplerProbeStage) {
		case 0 -> {
			if (trains.size() != 2 || trains.stream().anyMatch(train -> train.carriages.size() != 1)
				|| !hasValidCouplerBindings(minecraft, trains))
				return;
			couplerInitialTrainIds = trains.stream().map(train -> train.id).collect(Collectors.toUnmodifiableSet());
			couplerProbeStage = 1;
			LOGGER.info("Railward COUPLE-01 {} captured two independent one-carriage trains: {}",
				minecraft.player.getName().getString(), couplerInitialTrainIds);
		}
		case 1 -> {
			if (trains.size() != 1 || trains.get(0).carriages.size() != 2
				|| trains.get(0).carriageSpacing.size() != 1 || !hasValidCouplerBindings(minecraft, trains))
				return;
			Train combined = trains.get(0);
			if (!couplerInitialTrainIds.contains(combined.id))
				throw new IllegalStateException("COUPLE-01 combined train did not retain either original identity: "
					+ combined.id);
			couplerCombinedTrainId = combined.id;
			couplerProbeStage = 2;
			LOGGER.info("Railward COUPLE-01 {} observed one synchronized two-carriage train: {}",
				minecraft.player.getName().getString(), combined.id);
		}
		case 2 -> {
			if (trains.size() != 2 || trains.stream().anyMatch(train -> train.carriages.size() != 1)
				|| !hasValidCouplerBindings(minecraft, trains))
				return;
			Set<UUID> splitIds = trains.stream().map(train -> train.id).collect(Collectors.toUnmodifiableSet());
			if (!splitIds.contains(couplerCombinedTrainId) || splitIds.size() != 2
				|| splitIds.stream().allMatch(couplerInitialTrainIds::contains))
				throw new IllegalStateException("COUPLE-01 split train identities were invalid: initial="
					+ couplerInitialTrainIds + ", combined=" + couplerCombinedTrainId + ", split=" + splitIds);
			complete = true;
			LOGGER.info("Railward COUPLE-01 {} passed: two trains, coupled topology and split topology all had valid train identities, carriage order and entity bindings",
				minecraft.player.getName().getString());
			minecraft.stop();
		}
		default -> throw new IllegalStateException("Unknown coupler probe stage " + couplerProbeStage);
		}
	}

	private static void advanceAutoCouplerProbe(Minecraft minecraft) {
		List<Train> trains = List.copyOf(CreateClient.RAILWAYS.trains.values());
		if (ticks % 40 == 0)
			LOGGER.info("Railward COUPLE-02 {} stage {} topology {}",
				minecraft.player.getName().getString(), autoCouplerProbeStage, describeClientTopology(trains));

		switch (autoCouplerProbeStage) {
		case 0 -> {
			if (trains.size() != 8 || trains.stream().anyMatch(train -> train.carriages.size() != 1)
				|| !hasValidCouplerBindings(minecraft, trains))
				return;
			autoCouplerInitialTrainIds =
				trains.stream().map(train -> train.id).collect(Collectors.toUnmodifiableSet());
			autoCouplerProbeStage = 1;
			LOGGER.info("Railward COUPLE-02 {} captured eight independent one-carriage trains",
				minecraft.player.getName().getString());
		}
		case 1 -> {
			long singleCarriage = trains.stream().filter(train -> train.carriages.size() == 1).count();
			long twoCarriage = trains.stream().filter(train -> train.carriages.size() == 2).count();
			if (trains.size() != 7 || singleCarriage != 6 || twoCarriage != 1
				|| !hasValidCouplerBindings(minecraft, trains))
				return;
			if (!autoCouplerInitialTrainIds.containsAll(trains.stream().map(train -> train.id).toList()))
				throw new IllegalStateException("COUPLE-02 first automatic merge introduced an unknown train identity");
			Train westbound = findTrainByName(trains, AUTO_WEST_MOVER);
			if (westbound == null || westbound.carriages.size() != 2 || findTrainByName(trains, AUTO_WEST_TARGET) != null)
				return;
			autoCouplerProbeStage = 2;
			LOGGER.info("Railward COUPLE-02 {} observed the synchronized westbound permitted merge",
				minecraft.player.getName().getString());
		}
		case 2 -> {
			long singleCarriage = trains.stream().filter(train -> train.carriages.size() == 1).count();
			long twoCarriage = trains.stream().filter(train -> train.carriages.size() == 2).count();
			if (trains.size() != 6 || singleCarriage != 4 || twoCarriage != 2
				|| !hasValidCouplerBindings(minecraft, trains))
				return;
			if (!autoCouplerInitialTrainIds.containsAll(trains.stream().map(train -> train.id).toList()))
				throw new IllegalStateException("COUPLE-02 final topology introduced an unknown train identity");
			if (!isSingleCarriageNamed(trains, AUTO_DISABLED_MOVER)
				|| !isSingleCarriageNamed(trains, AUTO_DISABLED_TARGET)
				|| !isSingleCarriageNamed(trains, AUTO_FAST_MOVER)
				|| !isSingleCarriageNamed(trains, AUTO_FAST_TARGET))
				throw new IllegalStateException("COUPLE-02 merged a disabled or above-limit pair: "
					+ describeClientTopology(trains));
			Train westbound = findTrainByName(trains, AUTO_WEST_MOVER);
			Train eastbound = findTrainByName(trains, AUTO_EAST_MOVER);
			if (westbound == null || westbound.carriages.size() != 2 || eastbound == null
				|| eastbound.carriages.size() != 2 || findTrainByName(trains, AUTO_WEST_TARGET) != null
				|| findTrainByName(trains, AUTO_EAST_TARGET) != null)
				return;
			complete = true;
			LOGGER.info("Railward COUPLE-02 {} passed: disabled and above-limit pairs stayed separate while both permitted approach directions synchronized",
				minecraft.player.getName().getString());
			minecraft.stop();
		}
		default -> throw new IllegalStateException("Unknown automatic coupler probe stage " + autoCouplerProbeStage);
		}
	}

	private static void advanceStorageCouplerProbe(Minecraft minecraft) {
		List<Train> trains = List.copyOf(CreateClient.RAILWAYS.trains.values());
		StorageSnapshot snapshot = storageSnapshot(minecraft, trains);
		String playerName = minecraft.player.getName().getString();
		boolean reconnect = "RailwardRejoin".equals(playerName);
		if (ticks % 40 == 0)
			LOGGER.info("Railward COUPLE-03 {} stage {} topology {}, storageReady={}", playerName,
				storageCouplerProbeStage, describeClientTopology(trains), snapshot != null);

		if (reconnect) {
			if (!isStorageTopology(trains, 2, 2) || snapshot == null
				|| findTrainByName(trains, STORAGE_COUPLER_FINAL_SCHEDULED) == null
				|| findTrainByName(trains, STORAGE_COUPLER_FINAL_UNSCHEDULED) == null)
				return;
			if (++storageCouplerReconnectTicks < 100)
				return;
			complete = true;
			LOGGER.info("Railward COUPLE-03 {} passed: reconnect received the final two-train topology, four storage signatures and both seated passengers",
				playerName);
			minecraft.stop();
			return;
		}

		switch (storageCouplerProbeStage) {
		case 0 -> {
			if (!isStorageTopology(trains, 2, 2) || snapshot == null)
				return;
			storageCouplerInitialTrainIds = trains.stream().map(train -> train.id)
				.collect(Collectors.toUnmodifiableSet());
			storageCouplerEntityIds = snapshot.entityIds();
			storageCouplerPassengerIds = snapshot.passengerIds();
			storageCouplerProbeStage = 1;
			LOGGER.info("Railward COUPLE-03 {} captured the initial 2 x 2 topology", playerName);
		}
		case 1 -> {
			if (!isStorageTopology(trains, 1, 4) || snapshot == null)
				return;
			assertStorageIdentities(snapshot);
			Train combined = trains.get(0);
			if (!storageCouplerInitialTrainIds.contains(combined.id))
				throw new IllegalStateException("COUPLE-03 combined train did not retain an original identity: "
					+ combined.id);
			storageCouplerCombinedTrainId = combined.id;
			storageCouplerProbeStage = 2;
			LOGGER.info("Railward COUPLE-03 {} observed the synchronized 1 x 4 topology", playerName);
		}
		case 2 -> {
			if (!isStorageTopology(trains, 2, 2) || snapshot == null)
				return;
			assertStorageIdentities(snapshot);
			Set<UUID> splitIds = trains.stream().map(train -> train.id).collect(Collectors.toUnmodifiableSet());
			if (!splitIds.contains(storageCouplerCombinedTrainId) || splitIds.size() != 2
				|| splitIds.stream().allMatch(storageCouplerInitialTrainIds::contains))
				throw new IllegalStateException("COUPLE-03 split train identities were invalid: initial="
					+ storageCouplerInitialTrainIds + ", combined=" + storageCouplerCombinedTrainId
					+ ", split=" + splitIds);
			if ("RailwardObserver".equals(playerName)) {
				complete = true;
				LOGGER.info("Railward COUPLE-03 {} passed: 2 x 2 -> 1 x 4 -> 2 x 2 preserved storage and passengers",
					playerName);
				minecraft.stop();
				return;
			}
			storageCouplerProbeStage = 3;
			LOGGER.info("Railward COUPLE-03 {} waiting for the reconnect client", playerName);
		}
		case 3 -> {
			if (!isStorageTopology(trains, 2, 2) || snapshot == null)
				return;
			assertStorageIdentities(snapshot);
			if (minecraft.getConnection() == null || minecraft.getConnection().getOnlinePlayers().stream()
				.noneMatch(info -> "RailwardRejoin".equals(info.getProfile().getName())))
				return;
			complete = true;
			LOGGER.info("Railward COUPLE-03 {} passed: retained final state while RailwardRejoin joined",
				playerName);
			minecraft.stop();
		}
		default -> throw new IllegalStateException("Unknown storage coupler probe stage " + storageCouplerProbeStage);
		}
	}

	private static boolean isStorageTopology(List<Train> trains, int trainCount, int carriagesPerTrain) {
		return trains.size() == trainCount
			&& trains.stream().allMatch(train -> train.graph != null
				&& train.carriages.size() == carriagesPerTrain
				&& train.carriageSpacing.size() == carriagesPerTrain - 1);
	}

	private static StorageSnapshot storageSnapshot(Minecraft minecraft, List<Train> trains) {
		Set<StorageSignature> signatures = new HashSet<>();
		Set<UUID> entityIds = new HashSet<>();
		Set<UUID> passengerIds = new HashSet<>();
		int couplers = 0;
		int carriages = 0;
		for (Train train : trains)
			for (int i = 0; i < train.carriages.size(); i++) {
				CarriageContraptionEntity entity = train.carriages.get(i).anyAvailableEntity();
				if (entity == null || entity.level != minecraft.level || entity.getCarriage() != train.carriages.get(i)
					|| entity.getCarriage().train != train || !train.id.equals(entity.trainId)
					|| entity.carriageIndex != i)
					return null;
				Contraption contraption = entity.getContraption();
				BulkCargoContainerBlockEntity cargoView = contraption.presentBlockEntities.values().stream()
					.filter(BulkCargoContainerBlockEntity.class::isInstance)
					.map(BulkCargoContainerBlockEntity.class::cast)
					.filter(BulkCargoContainerBlockEntity::hasClientCargoView)
					.findFirst()
					.orElse(null);
				StructureBlockInfo cistern = findBlock(contraption, CisternBlock.class);
				IFluidHandler fluids = cistern == null ? null : CisternContraptionHelper.getFluidHandler(contraption,
					RailwardStructureInfo.pos(cistern));
				if (cargoView == null || !cargoView.getDisplayStack().is(Items.IRON_INGOT) || fluids == null
					|| fluids.getFluidInTank(0).getFluid() != Fluids.WATER)
					return null;
				signatures.add(new StorageSignature(cargoView.getStoredAmount(),
					fluids.getFluidInTank(0).getAmount()));
				entityIds.add(entity.getUUID());
				entity.getPassengers().stream()
					.filter(passenger -> passenger instanceof Villager
						&& contraption.getSeatOf(passenger.getUUID()) != null)
					.map(Entity::getUUID)
					.forEach(passengerIds::add);
				couplers += contraption.getBlocks().values().stream()
					.filter(info -> RailwardStructureInfo.state(info).getBlock() instanceof TrainCouplerBlock)
					.count();
				carriages++;
			}
		if (carriages != 4 || couplers != 2 || entityIds.size() != 4 || passengerIds.size() != 2
			|| !signatures.equals(STORAGE_SIGNATURES))
			return null;
		return new StorageSnapshot(Set.copyOf(entityIds), Set.copyOf(passengerIds));
	}

	private static void assertStorageIdentities(StorageSnapshot snapshot) {
		if (!snapshot.entityIds().equals(storageCouplerEntityIds))
			throw new IllegalStateException("COUPLE-03 carriage entity identities changed");
		if (!snapshot.passengerIds().equals(storageCouplerPassengerIds))
			throw new IllegalStateException("COUPLE-03 passenger identities or seat bindings changed");
	}

	private static Train findTrainByName(List<Train> trains, String name) {
		return trains.stream().filter(train -> name.equals(train.name.getString())).findFirst().orElse(null);
	}

	private static boolean isSingleCarriageNamed(List<Train> trains, String name) {
		Train train = findTrainByName(trains, name);
		return train != null && train.carriages.size() == 1;
	}

	private static boolean hasValidCouplerBindings(Minecraft minecraft, List<Train> trains) {
		for (Train train : trains) {
			if (train.graph == null)
				return false;
			for (int i = 0; i < train.carriages.size(); i++) {
				CarriageContraptionEntity entity = train.carriages.get(i).anyAvailableEntity();
				if (entity == null || entity.level != minecraft.level || entity.getCarriage() != train.carriages.get(i)
					|| entity.getCarriage().train != train || !train.id.equals(entity.trainId) || entity.carriageIndex != i
					|| entity.getContraption().getBlocks().values().stream()
						.filter(info -> RailwardStructureInfo.state(info).getBlock() instanceof TrainCouplerBlock)
						.count() != 1)
					return false;
			}
		}
		return true;
	}

	private static String describeClientTopology(List<Train> trains) {
		return trains.stream()
			.map(train -> train.name.getString() + "/" + train.id + "=" + train.carriages.size())
			.toList()
			.toString();
	}

	private static void advanceControllerProbe(Minecraft minecraft, CarriageContraptionEntity entity,
		DirectionState direction, StructureBlockInfo controller) {
		BlockStateSnapshot snapshot = new BlockStateSnapshot(RailwardStructureInfo.state(controller)
			.getValue(ModernTrainControllerBlock.REVERSED),
			RailwardStructureInfo.state(controller).getValue(ModernTrainControllerBlock.ACTIVE),
			RailwardStructureInfo.state(controller).getValue(ModernTrainControllerBlock.THROTTLE),
			RailwardStructureInfo.state(controller).getValue(ModernTrainControllerBlock.BRAKING));
		boolean driver = "RailwardDriver".equals(minecraft.player.getName().getString());
		BlockPos controlsPos = RailwardStructureInfo.pos(controller);
		if (ticks % 40 == 0)
			LOGGER.info("Railward CTRL-01/CTRL-02 {} stage {}: direction={}, reversed={}, active={}, throttle={}, braking={}, prompt={}",
				minecraft.player.getName().getString(), controllerProbeStage, direction, snapshot.reversed,
				snapshot.active, snapshot.throttle, snapshot.braking,
				TrainHUD.currentPrompt == null ? "<none>" : TrainHUD.currentPrompt.getString());

		switch (controllerProbeStage) {
		case 0 -> {
			if (!snapshot.active && !snapshot.throttle && !snapshot.braking
				&& direction != DirectionState.NONE) {
				controllerProbeStage = 1;
				LOGGER.info("Railward CTRL-01/CTRL-02 {} captured the initial idle state: direction={}, reversed={}",
					minecraft.player.getName().getString(), direction, snapshot.reversed);
			}
		}
		case 1 -> {
			if (!snapshot.active || !snapshot.reversed || snapshot.throttle || snapshot.braking
				|| direction != DirectionState.BACKWARD)
				return;
			if (driver) {
				// Leave the initial state visible long enough for the second client to observe it.
				if (++controllerProbeReadyTicks < 100)
					return;
				clearPrompt();
				sendReverser(entity, controlsPos);
				controllerProbeActionSent = true;
			}
			controllerProbeStage = 2;
		}
		case 2 -> {
			if (!snapshot.active || snapshot.reversed || snapshot.throttle || snapshot.braking
				|| direction != DirectionState.FORWARD || driver && !promptIs("railward.modern_train_controller.reverser.forward"))
				return;
			controllerProbeActionSent = false;
			controllerProbeForwardPosition = entity.position();
			controllerProbeStage = 3;
			LOGGER.info("Railward CTRL-02 {} observed the allowed stopped forward reverser change",
				minecraft.player.getName().getString());
		}
		case 3 -> {
			if (!snapshot.active || snapshot.reversed || snapshot.throttle || snapshot.braking
				|| direction != DirectionState.FORWARD || controllerProbeForwardPosition == null
				|| entity.position().distanceToSqr(controllerProbeForwardPosition) <= .01)
				return;
			if (!driver) {
				controllerProbeStage = 4;
				return;
			}
			if (!controllerProbeActionSent) {
				clearPrompt();
				sendReverser(entity, controlsPos);
				controllerProbeActionSent = true;
				return;
			}
			if (promptIs("railward.modern_train_controller.reverser.must_stop")) {
				controllerProbeActionSent = false;
				controllerProbeStage = 4;
			}
		}
		case 4 -> {
			if (!snapshot.active || snapshot.reversed || !snapshot.throttle || snapshot.braking
				|| direction != DirectionState.FORWARD)
				return;
			if (!driver) {
				controllerProbeStage = 5;
				return;
			}
			if (!controllerProbeActionSent) {
				clearPrompt();
				sendReverser(entity, controlsPos);
				controllerProbeActionSent = true;
				return;
			}
			if (promptIs("railward.modern_train_controller.reverser.must_stop")) {
				controllerProbeActionSent = false;
				controllerProbeStage = 5;
			}
		}
		case 5 -> {
			if (!snapshot.active || snapshot.reversed || snapshot.throttle || !snapshot.braking
				|| direction != DirectionState.FORWARD)
				return;
			if (!driver) {
				controllerProbeStage = 6;
				return;
			}
			if (!controllerProbeActionSent) {
				clearPrompt();
				sendReverser(entity, controlsPos);
				controllerProbeActionSent = true;
				return;
			}
			if (promptIs("railward.modern_train_controller.reverser.must_stop")) {
				controllerProbeActionSent = false;
				controllerProbeStage = 6;
			}
		}
		case 6 -> {
			if (!snapshot.active || snapshot.reversed || snapshot.throttle || snapshot.braking
				|| direction != DirectionState.FORWARD)
				return;
			if (driver) {
				clearPrompt();
				sendReverser(entity, controlsPos);
				controllerProbeActionSent = true;
			}
			controllerProbeStage = 7;
		}
		case 7 -> {
			if (!snapshot.active || !snapshot.reversed || snapshot.braking
				|| direction != DirectionState.BACKWARD || driver && !promptIs("railward.modern_train_controller.reverser.backward"))
				return;
			controllerProbeActionSent = false;
			controllerProbeStage = 8;
			LOGGER.info("Railward CTRL-02 {} observed all allowed and denied reverser outcomes",
				minecraft.player.getName().getString());
		}
		case 8 -> {
			if (!snapshot.active || !snapshot.reversed || !snapshot.throttle || snapshot.braking
				|| direction != DirectionState.BACKWARD)
				return;
			controllerProbeStage = 9;
		}
		case 9 -> {
			if (snapshot.active || snapshot.throttle || snapshot.braking || direction != DirectionState.NONE)
				return;
			complete = true;
			LOGGER.info("Railward CTRL-01/CTRL-02 {} passed: forward, coast, brake, backward, release and reverser guards synchronized",
				minecraft.player.getName().getString());
			minecraft.stop();
		}
		default -> throw new IllegalStateException("Unknown controller probe stage " + controllerProbeStage);
		}
	}

	private static void sendReverser(CarriageContraptionEntity entity, BlockPos controlsPos) {
		RailwardPackets.getChannel().sendToServer(new ModernTrainControllerReverserPacket(entity.getId(), controlsPos));
	}

	private static void advanceScheduledProbe(Minecraft minecraft, CarriageContraptionEntity entity,
		DirectionState direction, StructureBlockInfo controller, StructureBlockInfo bufferLights) {
		BlockStateSnapshot snapshot = new BlockStateSnapshot(RailwardStructureInfo.state(controller)
			.getValue(ModernTrainControllerBlock.REVERSED),
			RailwardStructureInfo.state(controller).getValue(ModernTrainControllerBlock.ACTIVE),
			RailwardStructureInfo.state(controller).getValue(ModernTrainControllerBlock.THROTTLE),
			RailwardStructureInfo.state(controller).getValue(ModernTrainControllerBlock.BRAKING));
		boolean lightsPresent = RailwardStructureInfo.state(bufferLights)
			.getValue(LocomotiveBufferLightsBlock.VARIANT) == LocomotiveBufferLightsVariant.TYPE_3;
		boolean npcSeated = entity.getPassengers().stream().anyMatch(passenger ->
			passenger instanceof Villager && entity.getContraption().getSeatOf(passenger.getUUID()) != null);
		if (ticks % 40 == 0)
			LOGGER.info("Railward CTRL-03 {} stage {}: direction={}, active={}, throttle={}, braking={}, NPC={}, lights={}",
				minecraft.player.getName().getString(), scheduledProbeStage, direction, snapshot.active,
				snapshot.throttle, snapshot.braking, npcSeated, lightsPresent);
		if (!lightsPresent)
			return;
		switch (scheduledProbeStage) {
		case 0 -> {
			if (snapshot.active || snapshot.throttle || snapshot.braking || direction == DirectionState.NONE)
				return;
			scheduledProbeStage = 1;
		}
		case 1 -> {
			if (!npcSeated || !snapshot.active || !snapshot.throttle || snapshot.braking
				|| direction != DirectionState.FORWARD)
				return;
			scheduledProbeStage = 2;
			LOGGER.info("Railward CTRL-03 {} observed scheduled forward throttle and seated NPC",
				minecraft.player.getName().getString());
		}
		case 2 -> {
			if (!snapshot.active || snapshot.throttle || !snapshot.braking
				|| direction != DirectionState.FORWARD)
				return;
			scheduledProbeStage = 3;
		}
		case 3 -> {
			if (!snapshot.active || snapshot.throttle || snapshot.braking
				|| direction != DirectionState.FORWARD)
				return;
			scheduledProbeStage = 4;
		}
		case 4 -> {
			if (!snapshot.active || !snapshot.throttle || snapshot.braking
				|| direction != DirectionState.BACKWARD)
				return;
			scheduledProbeStage = 5;
			LOGGER.info("Railward CTRL-03 {} observed scheduled backward throttle and direction update",
				minecraft.player.getName().getString());
		}
		case 5 -> {
			if (!snapshot.active || snapshot.throttle || !snapshot.braking
				|| direction != DirectionState.BACKWARD)
				return;
			scheduledProbeStage = 6;
		}
		case 6 -> {
			if (snapshot.active || snapshot.throttle || snapshot.braking || direction != DirectionState.NONE)
				return;
			complete = true;
			LOGGER.info("Railward CTRL-03 {} passed: NPC schedule synchronized forward throttle, brake, stop, backward throttle, brake and idle",
				minecraft.player.getName().getString());
			minecraft.stop();
		}
		default -> throw new IllegalStateException("Unknown scheduled probe stage " + scheduledProbeStage);
		}
	}

	private static void advanceScheduledRouteProbe(Minecraft minecraft, CarriageContraptionEntity entity,
		DirectionState direction, StructureBlockInfo controller, StructureBlockInfo bufferLights) {
		BlockStateSnapshot snapshot = new BlockStateSnapshot(RailwardStructureInfo.state(controller)
			.getValue(ModernTrainControllerBlock.REVERSED),
			RailwardStructureInfo.state(controller).getValue(ModernTrainControllerBlock.ACTIVE),
			RailwardStructureInfo.state(controller).getValue(ModernTrainControllerBlock.THROTTLE),
			RailwardStructureInfo.state(controller).getValue(ModernTrainControllerBlock.BRAKING));
		boolean lightsPresent = RailwardStructureInfo.state(bufferLights)
			.getValue(LocomotiveBufferLightsBlock.VARIANT) == LocomotiveBufferLightsVariant.TYPE_3;
		boolean npcSeated = entity.getPassengers().stream().anyMatch(passenger ->
			passenger instanceof Villager && entity.getContraption().getSeatOf(passenger.getUUID()) != null);
		double movedSqr = scheduledRouteStartPosition == null ? 0
			: entity.position().distanceToSqr(scheduledRouteStartPosition);
		if (ticks % 40 == 0)
			LOGGER.info("Railward CTRL-03 route {} stage {}: direction={}, active={}, throttle={}, braking={}, NPC={}, moved={}",
				minecraft.player.getName().getString(), scheduledRouteProbeStage, direction, snapshot.active,
				snapshot.throttle, snapshot.braking, npcSeated, Math.sqrt(movedSqr));
		if (!lightsPresent)
			return;
		switch (scheduledRouteProbeStage) {
		case 0 -> {
			if (snapshot.active || snapshot.throttle || snapshot.braking || direction == DirectionState.NONE)
				return;
			scheduledRouteStartPosition = entity.position();
			scheduledRouteProbeStage = 1;
		}
		case 1 -> {
			if (!npcSeated || !snapshot.active || !snapshot.throttle || snapshot.braking
				|| direction == DirectionState.NONE)
				return;
			scheduledRouteDirection = direction;
			scheduledRouteProbeStage = 2;
			LOGGER.info("Railward CTRL-03 route {} observed autonomous scheduled departure with seated NPC",
				minecraft.player.getName().getString());
		}
		case 2 -> {
			if (!snapshot.active || snapshot.throttle || !snapshot.braking
				|| direction != scheduledRouteDirection || movedSqr < 16)
				return;
			scheduledRouteProbeStage = 3;
			LOGGER.info("Railward CTRL-03 route {} observed movement and autonomous braking after {} blocks",
				minecraft.player.getName().getString(), Math.sqrt(movedSqr));
		}
		case 3 -> {
			if (!snapshot.active || snapshot.throttle || snapshot.braking
				|| direction != scheduledRouteDirection)
				return;
			scheduledRouteProbeStage = 4;
		}
		case 4 -> {
			if (snapshot.active || snapshot.throttle || snapshot.braking || direction != DirectionState.NONE)
				return;
			complete = true;
			LOGGER.info("Railward CTRL-03 route {} passed: autonomous departure, movement, braking, arrival delay and idle synchronized",
				minecraft.player.getName().getString());
			minecraft.stop();
		}
		default -> throw new IllegalStateException("Unknown scheduled route probe stage " + scheduledRouteProbeStage);
		}
	}

	private static void advanceSignalProbe(Minecraft minecraft, Train train, CarriageContraptionEntity entity,
		DirectionState direction) {
		if (signalStartPosition == null)
			signalStartPosition = entity.position();
		signalMaxDistanceSqr = Math.max(signalMaxDistanceSqr,
			entity.position().distanceToSqr(signalStartPosition));

		CabSignalAspect aspect = CabSignalTrainState.resolve(entity);
		signalAspectsSeen.add(aspect);
		signalForwardSeen |= direction == DirectionState.FORWARD;
		signalBackwardSeen |= direction == DirectionState.BACKWARD;
		double low = signalThrottleForLimit(train, 20);
		double high = signalThrottleForLimit(train, 60);
		signalLowLimitSeen |= Math.abs(train.throttle - low) < 1.0E-4;
		signalHighLimitSeen |= Math.abs(train.throttle - high) < 1.0E-4;

		boolean honkActive = train.honkTicks > 0;
		if (honkActive && !signalHonkActive) {
			signalHonkEvents++;
			LOGGER.info("Railward SIGNAL-01 {} observed whistle-sign honk event {}",
				minecraft.player.getName().getString(), signalHonkEvents);
		}
		signalHonkActive = honkActive;

		boolean allAspects = signalAspectsSeen.containsAll(Set.of(CabSignalAspect.WHITE,
			CabSignalAspect.GREEN, CabSignalAspect.YELLOW, CabSignalAspect.YELLOW_RED));
		boolean returned = signalMaxDistanceSqr > 16 * 16
			&& entity.position().distanceToSqr(signalStartPosition) < 10 * 10;
		if (returned && aspect == CabSignalAspect.OFF)
			signalReturnStableTicks++;
		else
			signalReturnStableTicks = 0;
		if (ticks % 100 == 0)
			LOGGER.info("Railward SIGNAL-01 {} waiting: aspects={}, limits={}/{}, directions={}/{}, honks={}, maxDistance={}, returned={}, speed={}, stableTicks={}",
				minecraft.player.getName().getString(), signalAspectsSeen, signalLowLimitSeen, signalHighLimitSeen,
				signalForwardSeen, signalBackwardSeen, signalHonkEvents, Math.sqrt(signalMaxDistanceSqr), returned, train.speed,
				signalReturnStableTicks);
		if (!allAspects || !signalLowLimitSeen || !signalHighLimitSeen || !signalForwardSeen
			|| !signalBackwardSeen || signalHonkEvents != 2 || signalReturnStableTicks < 20)
			return;

		complete = true;
		LOGGER.info("Railward SIGNAL-01 {} passed: WHITE/GREEN/YELLOW/YELLOW_RED, synchronized 20/60 km/h "
			+ "throttles, two directional honks and the complete round trip",
			minecraft.player.getName().getString());
		minecraft.stop();
	}

	private static double signalThrottleForLimit(Train train, int speedKmh) {
		double maxSpeedKmh = train.maxSpeed() * 72;
		int step = Math.max(1, Math.min(18, (int) Math.round(speedKmh / maxSpeedKmh * 18)));
		return step / 18d;
	}

	private static boolean promptIs(String translationKey) {
		return TrainHUD.currentPrompt != null
			&& TrainHUD.currentPrompt.getString().equals(RailwardComponents.translatable(translationKey).getString());
	}

	private static void clearPrompt() {
		TrainHUD.currentPrompt = null;
		TrainHUD.promptKeepAlive = 0;
	}

	private record BlockStateSnapshot(boolean reversed, boolean active, boolean throttle, boolean braking) {
	}

	private record StorageSignature(int items, int fluid) {
	}

	private record StorageSnapshot(Set<UUID> entityIds, Set<UUID> passengerIds) {
	}

	private static void logWaiting(String state, int trains, CarriageContraptionEntity entity,
		DirectionState direction) {
		if (ticks % 200 != 0)
			return;
		LOGGER.info("Railward NET-01 client probe waiting for {}: trains={}, entity={}, direction={}",
			state, trains, entity != null, direction == null ? DirectionState.NONE : direction);
	}

	private static CarriageContraptionEntity findEntity(Minecraft minecraft, Train train) {
		for (Entity candidate : minecraft.level.entitiesForRendering()) {
			if (!(candidate instanceof CarriageContraptionEntity entity))
				continue;
			if (entity.getCarriage() != null && entity.getCarriage().train != null
				&& train.id.equals(entity.getCarriage().train.id))
				return entity;
		}
		return null;
	}

	private static StructureBlockInfo findBlock(Contraption contraption, Class<?> blockType) {
		return contraption.getBlocks().values().stream()
			.filter(info -> blockType.isInstance(RailwardStructureInfo.state(info).getBlock()))
			.findFirst()
			.orElse(null);
	}

	private static List<StructureBlockInfo> findBlocks(Contraption contraption, Class<?> blockType) {
		return contraption.getBlocks().values().stream()
			.filter(info -> blockType.isInstance(RailwardStructureInfo.state(info).getBlock()))
			.toList();
	}

}
