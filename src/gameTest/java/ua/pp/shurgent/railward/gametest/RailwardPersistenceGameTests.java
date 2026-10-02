package ua.pp.shurgent.railward.gametest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.mojang.logging.LogUtils;
import com.simibubi.create.Create;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.actors.trainControls.ControlsBlock;
import com.simibubi.create.content.contraptions.glue.SuperGlueEntity;
import com.simibubi.create.content.decoration.slidingDoor.SlidingDoorBlock;
import com.simibubi.create.content.trains.bogey.AbstractBogeyBlock;
import com.simibubi.create.content.trains.entity.CarriageContraption;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.graph.DiscoveredPath;
import com.simibubi.create.content.trains.graph.EdgePointType;
import com.simibubi.create.content.trains.graph.TrackGraph;
import com.simibubi.create.content.trains.graph.TrackGraphHelper;
import com.simibubi.create.content.trains.graph.TrackGraphLocation;
import com.simibubi.create.content.trains.schedule.Schedule;
import com.simibubi.create.content.trains.schedule.ScheduleEntry;
import com.simibubi.create.content.trains.schedule.ScheduleRuntime.State;
import com.simibubi.create.content.trains.schedule.condition.ScheduledDelay;
import com.simibubi.create.content.trains.schedule.destination.ChangeTitleInstruction;
import com.simibubi.create.content.trains.schedule.destination.DestinationInstruction;
import com.simibubi.create.content.trains.station.GlobalStation;
import com.simibubi.create.content.trains.station.StationBlockEntity;
import com.simibubi.create.content.trains.signal.SignalBlockEntity;
import com.simibubi.create.content.trains.signal.SignalBlockEntity.SignalState;
import com.simibubi.create.content.trains.signal.SignalBoundary;
import com.simibubi.create.content.trains.signal.TrackEdgePoint;
import com.simibubi.create.content.trains.track.TrackBlock;
import com.simibubi.create.content.trains.track.TrackShape;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;
import net.minecraftforge.gametest.GameTestHolder;
//? if >=1.20.1
/*import net.minecraftforge.common.capabilities.ForgeCapabilities;*/
//? if <1.20.1
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import org.slf4j.Logger;
import ua.pp.shurgent.railward.AllBlocks;
import ua.pp.shurgent.railward.AllBlockEntities;
import ua.pp.shurgent.railward.AllEdgePointTypes;
import ua.pp.shurgent.railward.Railward;
import ua.pp.shurgent.railward.RailwardConfig;
import ua.pp.shurgent.railward.block.train.parts.BulkCargoContraptionHelper;
import ua.pp.shurgent.railward.block.train.parts.BulkCargoContainerBlock;
import ua.pp.shurgent.railward.block.train.parts.BulkCargoContainerBlockEntity;
import ua.pp.shurgent.railward.block.train.parts.BulkCargoStorage;
import ua.pp.shurgent.railward.block.train.parts.CabSignalAspect;
import ua.pp.shurgent.railward.block.train.parts.CabSignalBlock;
import ua.pp.shurgent.railward.block.train.parts.CabSignalTrainState;
import ua.pp.shurgent.railward.block.train.parts.CisternBlock;
import ua.pp.shurgent.railward.block.train.parts.CisternBlockEntity;
import ua.pp.shurgent.railward.block.train.parts.CisternContraptionHelper;
import ua.pp.shurgent.railward.block.train.parts.LocomotiveBufferLightsBlock;
import ua.pp.shurgent.railward.block.train.parts.LocomotiveBufferLightsVariant;
import ua.pp.shurgent.railward.block.train.parts.ModernTrainControllerBlock;
import ua.pp.shurgent.railward.block.train.parts.ModernTrainControllerAssembly;
import ua.pp.shurgent.railward.block.train.parts.ModernTrainControllerState;
import ua.pp.shurgent.railward.block.train.parts.TrainDirectionState;
import ua.pp.shurgent.railward.block.train.parts.TrainDirectionState.DirectionState;
import ua.pp.shurgent.railward.block.train.parts.TrainCouplerBlock;
import ua.pp.shurgent.railward.block.train.parts.TrainCouplerHandler;
import ua.pp.shurgent.railward.block.train.parts.door.TrainDoorBlock;
import ua.pp.shurgent.railward.block.train.signal.SpeedSignEdgePoint;
import ua.pp.shurgent.railward.block.train.signal.WhistleSignBlockEntity;
import ua.pp.shurgent.railward.block.train.signal.WhistleSignEdgePoint;
import ua.pp.shurgent.railward.block.train.station.UnloadingBunkerBlockEntity;
import ua.pp.shurgent.railward.block.train.station.UnloadingHatchBlockEntity;
import ua.pp.shurgent.railward.compat.RailwardComponents;
import ua.pp.shurgent.railward.compat.RailwardEntity;
import ua.pp.shurgent.railward.compat.RailwardStructureInfo;

@GameTestHolder(Railward.MOD_ID)
@Mod.EventBusSubscriber(modid = Railward.MOD_ID)
public final class RailwardPersistenceGameTests {

	public static final String PHASE_PROPERTY = "railward.persistenceTestPhase";
	public static final String PREPARE_TEST_NAME = "railwardpersistence.prepare_train_restart";
	public static final String VERIFY_TEST_NAME = "railwardpersistence.verify_train_restart";
	public static final String COUPLER_PREPARE_TEST_NAME = "railwardpersistence.prepare_coupler_fixture";
	public static final String AUTO_COUPLER_PREPARE_TEST_NAME = "railwardpersistence.prepare_auto_coupler_fixture";
	public static final String STORAGE_COUPLER_PREPARE_TEST_NAME =
		"railwardpersistence.prepare_storage_coupler_fixture";

	private static final String EMPTY_TEMPLATE = "railward:empty";
	private static final String PERSISTENCE_TRAIN_NAME = "Railward SAVE-01 probe";
	private static final String ROUTE_ORIGIN_NAME = "Railward SIGNAL-01 origin";
	private static final String SIGNAL_RETURN_NAME = "Railward SIGNAL-01 return";
	private static final String ROUTE_DESTINATION_NAME = "Railward CTRL-03 destination";
	private static final String COUPLER_TRAIN_A = "Railward COUPLE-01 A";
	private static final String COUPLER_TRAIN_B = "Railward COUPLE-01 B";
	private static final String AUTO_DISABLED_MOVER = "Railward COUPLE-02 disabled mover";
	private static final String AUTO_DISABLED_TARGET = "Railward COUPLE-02 disabled target";
	private static final String AUTO_FAST_MOVER = "Railward COUPLE-02 fast mover";
	private static final String AUTO_FAST_TARGET = "Railward COUPLE-02 fast target";
	private static final String AUTO_WEST_MOVER = "Railward COUPLE-02 west mover";
	private static final String AUTO_WEST_TARGET = "Railward COUPLE-02 west target";
	private static final String AUTO_EAST_MOVER = "Railward COUPLE-02 east mover";
	private static final String AUTO_EAST_TARGET = "Railward COUPLE-02 east target";
	private static final String STORAGE_COUPLER_TRAIN_A = "Railward COUPLE-03 A";
	private static final String STORAGE_COUPLER_TRAIN_B = "Railward COUPLE-03 B";
	private static final String STORAGE_COUPLER_FINAL_SCHEDULED = "Railward COUPLE-03 scheduled";
	private static final String STORAGE_COUPLER_FINAL_UNSCHEDULED = "Railward COUPLE-03 unscheduled";
	private static final Set<StorageSignature> STORAGE_SIGNATURES = Set.of(
		new StorageSignature(11, 1100), new StorageSignature(22, 1200),
		new StorageSignature(33, 1300), new StorageSignature(44, 1400));
	private static final Logger LOGGER = LogUtils.getLogger();
	private static final BlockPos CARGO_UNLOAD_DIRECT_CARGO = new BlockPos(0, 100, 0);
	private static final BlockPos CARGO_UNLOAD_DIRECT_HATCH = CARGO_UNLOAD_DIRECT_CARGO.above(2);
	private static final BlockPos CARGO_UNLOAD_DIRECT_SOURCE = CARGO_UNLOAD_DIRECT_HATCH.above();
	private static final BlockPos CARGO_UNLOAD_DIRECT_LOCK = CARGO_UNLOAD_DIRECT_HATCH.relative(Direction.EAST);
	private static final BlockPos CARGO_UNLOAD_BUNKER = new BlockPos(5, 100, 0);
	private static final BlockPos CARGO_UNLOAD_BUNKER_HATCH = CARGO_UNLOAD_BUNKER.above(2);
	private static final BlockPos CARGO_UNLOAD_BUNKER_SOURCE = CARGO_UNLOAD_BUNKER_HATCH.above();
	private static final BlockPos CARGO_UNLOAD_BUNKER_LOCK = CARGO_UNLOAD_BUNKER_HATCH.relative(Direction.EAST);
	private static final BlockPos CARGO_UNLOAD_BLOCKED_CARGO = new BlockPos(10, 100, 0);
	private static final BlockPos CARGO_UNLOAD_BLOCKED_HATCH = CARGO_UNLOAD_BLOCKED_CARGO.above(2);
	private static final BlockPos CARGO_UNLOAD_BLOCKED_SOURCE = CARGO_UNLOAD_BLOCKED_HATCH.above();
	private static final BlockPos CARGO_UNLOAD_BLOCKED_LOCK = CARGO_UNLOAD_BLOCKED_HATCH.relative(Direction.EAST);
	private static int networkProbeDisconnects;
	private static int cargoUnloadingProbeTicks;
	private static boolean cargoUnloadingStarted;
	private static boolean cargoUnloadingDirectTransferred;
	private static boolean cargoUnloadingBunkerExtracted;
	private static boolean cargoUnloadingServerComplete;
	private static int multiClientReadyTicks;
	private static boolean multiClientMovementStarted;
	private static boolean multiClientMovementStopped;
	private static int controllerProbeReadyTicks;
	private static int controllerProbeStage;
	private static int controllerProbeStageTicks;
	private static boolean controllerProbeStarted;
	private static int scheduledProbeReadyTicks;
	private static int scheduledProbeStage;
	private static int scheduledProbeStageTicks;
	private static int scheduledRouteProbeTicks;
	private static int scheduledRouteProbeStage;
	private static double scheduledRouteStartX;
	private static double scheduledRouteStartZ;
	private static int signalProbeTicks;
	private static int signalProbeStage;
	private static int signalProbeStageTicks;
	private static FixedSignalBoundary signalProbeBoundary;
	private static boolean signalLowLimitApplied;
	private static boolean signalHighLimitApplied;
	private static int couplerProbeTicks;
	private static int couplerProbeStage;
	private static int autoCouplerProbeTicks;
	private static int autoCouplerProbeStage;
	private static int storageCouplerProbeTicks;
	private static int storageCouplerProbeStage;
	private static Set<UUID> storageCouplerEntityIds = Set.of();
	private static Set<UUID> storageCouplerPassengerIds = Set.of();
	private static CompoundTag storageCouplerScheduleTag;
	private static boolean storageCouplerObserverDisconnected;
	private static boolean storageCouplerReconnectJoined;
	private static boolean storageCouplerReconnectDisconnected;
	private static boolean storageCouplerDriverDisconnected;

	private RailwardPersistenceGameTests() {
	}

	@SubscribeEvent
	public static void startConfiguredPhase(ServerStartedEvent event) {
		String phase = System.getProperty(PHASE_PROPERTY, "").trim();
		if ("network".equals(phase) || "network_multi".equals(phase) || "controller".equals(phase)
			|| "scheduled".equals(phase) || "scheduled_route".equals(phase) || "signal".equals(phase)
			|| "coupler".equals(phase)
			|| "coupler_auto".equals(phase) || "coupler_storage".equals(phase)
			|| "cargo_unloading".equals(phase)) {
			networkProbeDisconnects = 0;
			cargoUnloadingProbeTicks = 0;
			cargoUnloadingStarted = false;
			cargoUnloadingDirectTransferred = false;
			cargoUnloadingBunkerExtracted = false;
			cargoUnloadingServerComplete = false;
			multiClientReadyTicks = 0;
			multiClientMovementStarted = false;
			multiClientMovementStopped = false;
			controllerProbeReadyTicks = 0;
			controllerProbeStage = 0;
			controllerProbeStageTicks = 0;
			controllerProbeStarted = false;
			scheduledProbeReadyTicks = 0;
			scheduledProbeStage = -1;
			scheduledProbeStageTicks = 0;
			scheduledRouteProbeTicks = 0;
			scheduledRouteProbeStage = -1;
			scheduledRouteStartX = 0;
			scheduledRouteStartZ = 0;
			signalProbeTicks = 0;
			signalProbeStage = -1;
			signalProbeStageTicks = 0;
			signalProbeBoundary = null;
			signalLowLimitApplied = false;
			signalHighLimitApplied = false;
			couplerProbeTicks = 0;
			couplerProbeStage = 0;
			autoCouplerProbeTicks = 0;
			autoCouplerProbeStage = 0;
			storageCouplerProbeTicks = 0;
			storageCouplerProbeStage = 0;
			storageCouplerEntityIds = Set.of();
			storageCouplerPassengerIds = Set.of();
			storageCouplerScheduleTag = null;
			storageCouplerObserverDisconnected = false;
			storageCouplerReconnectJoined = false;
			storageCouplerReconnectDisconnected = false;
			storageCouplerDriverDisconnected = false;
			if ("cargo_unloading".equals(phase)) {
				prepareCargoUnloadingFixture(event.getServer().overworld());
				LOGGER.info("Railward CARGO-03 server phase ready; waiting for driver and observer probes");
			} else if ("signal".equals(phase))
				LOGGER.info("Railward SIGNAL-01 server phase ready; waiting for driver and observer probes");
			else if ("coupler_storage".equals(phase))
				LOGGER.info("Railward COUPLE-03 server phase ready; waiting for driver and observer probes");
			else if ("coupler_auto".equals(phase))
				LOGGER.info("Railward COUPLE-02 server phase ready; waiting for driver and observer probes");
			else if ("coupler".equals(phase))
				LOGGER.info("Railward COUPLE-01 server phase ready; waiting for driver and observer probes");
			else if ("scheduled_route".equals(phase))
				LOGGER.info("Railward CTRL-03 autonomous route server phase ready; waiting for two observer probes");
			else if ("scheduled".equals(phase))
				LOGGER.info("Railward CTRL-03 server phase ready; waiting for two observer probes");
			else if ("controller".equals(phase))
				LOGGER.info("Railward CTRL-01/CTRL-02 server phase ready; waiting for driver and observer probes");
			else if ("network_multi".equals(phase))
				LOGGER.info("Railward NET-02 server phase ready; waiting for driver and observer probes");
			else
				LOGGER.info("Railward NET-01 server phase ready; waiting for two late-join client probes");
			return;
		}
		String testName;
		String position;
		if ("prepare".equals(phase)) {
			testName = PREPARE_TEST_NAME;
			position = "0 0 0";
		} else if ("verify".equals(phase)) {
			testName = VERIFY_TEST_NAME;
			position = "64 0 0";
		} else if ("coupler_prepare".equals(phase)) {
			testName = COUPLER_PREPARE_TEST_NAME;
			position = "0 0 0";
		} else if ("coupler_auto_prepare".equals(phase)) {
			testName = AUTO_COUPLER_PREPARE_TEST_NAME;
			position = "0 0 0";
		} else if ("coupler_storage_prepare".equals(phase)) {
			testName = STORAGE_COUPLER_PREPARE_TEST_NAME;
			position = "0 0 0";
		} else {
			return;
		}

		String command = "execute positioned " + position + " run test run " + testName;
		//? if >=1.19.2 {
		/*event.getServer().getCommands().performPrefixedCommand(event.getServer().createCommandSourceStack(), command);
		*///?} else
		event.getServer().getCommands().performCommand(event.getServer().createCommandSourceStack(), command);
	}

	@SubscribeEvent
	public static void positionNetworkProbe(PlayerLoggedInEvent event) {
		String phase = System.getProperty(PHASE_PROPERTY, "").trim();
		if (!("network".equals(phase) || "network_multi".equals(phase) || "controller".equals(phase)
			|| "scheduled".equals(phase) || "scheduled_route".equals(phase) || "signal".equals(phase)
			|| "coupler".equals(phase)
			|| "coupler_auto".equals(phase) || "coupler_storage".equals(phase)
			|| "cargo_unloading".equals(phase))
			|| RailwardEntity.level(event.getPlayer()).isClientSide)
			return;
		if ("cargo_unloading".equals(phase)) {
			event.getPlayer().setInvulnerable(true);
			event.getPlayer().teleportTo(2.5, 101, 5.5);
			LOGGER.info("Railward CARGO-03 positioned client probe {} next to the unloading fixtures",
				event.getPlayer().getName().getString());
			return;
		}
		CarriageContraptionEntity carriage =
			"coupler".equals(phase) || "coupler_auto".equals(phase) || "coupler_storage".equals(phase)
				? couplerCarriages().get(0) : persistedCarriage();
		if ("coupler_storage".equals(phase)
			&& "RailwardRejoin".equals(event.getPlayer().getName().getString())) {
			if (!storageCouplerObserverDisconnected || storageCouplerProbeStage < 3)
				throw new IllegalStateException("COUPLE-03 reconnect client joined before the final split was ready");
			storageCouplerReconnectJoined = true;
			assertStorageCouplerState(couplerTrains(), 2, 2, true);
			LOGGER.info("Railward COUPLE-03 reconnect client joined and the server state remained valid");
		}
		event.getPlayer().setInvulnerable(true);
		event.getPlayer().teleportTo(carriage.getX(), carriage.getY() + 3, carriage.getZ());
		String testId = "coupler_storage".equals(phase) ? "COUPLE-03"
			: "coupler_auto".equals(phase) ? "COUPLE-02" : "coupler".equals(phase) ? "COUPLE-01"
			: "signal".equals(phase) ? "SIGNAL-01"
			: "scheduled_route".equals(phase) ? "CTRL-03 route"
			: "scheduled".equals(phase) ? "CTRL-03" : "controller".equals(phase) ? "CTRL-01/CTRL-02"
			: "network_multi".equals(phase) ? "NET-02" : "NET-01";
		LOGGER.info("Railward {} positioned client probe {} next to the persisted carriage at {}, {}, {}",
			testId, event.getPlayer().getName().getString(),
			carriage.getX(), carriage.getY(), carriage.getZ());
	}

	@SubscribeEvent
	public static void advanceMultiClientProbe(TickEvent.ServerTickEvent event) {
		if (event.phase != TickEvent.Phase.END)
			return;
		String phase = System.getProperty(PHASE_PROPERTY, "").trim();
		if (!("network_multi".equals(phase) || "controller".equals(phase) || "scheduled".equals(phase)
			|| "scheduled_route".equals(phase) || "signal".equals(phase) || "coupler".equals(phase)
			|| "coupler_auto".equals(phase)
			|| "coupler_storage".equals(phase) || "cargo_unloading".equals(phase)))
			return;
		MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
		if (server == null)
			throw new IllegalStateException("Network probe server tick did not have a current dedicated server");
		if ("cargo_unloading".equals(phase)) {
			advanceCargoUnloadingProbe(server);
			return;
		}
		if ("coupler".equals(phase)) {
			advanceCouplerProbe(server);
			return;
		}
		if ("coupler_auto".equals(phase)) {
			advanceAutoCouplerProbe(server);
			return;
		}
		if ("coupler_storage".equals(phase)) {
			advanceStorageCouplerProbe(server);
			return;
		}
		if ("controller".equals(phase)) {
			advanceControllerProbe(server);
			return;
		}
		if ("scheduled".equals(phase)) {
			advanceScheduledProbe(server);
			return;
		}
		if ("scheduled_route".equals(phase)) {
			advanceScheduledRouteProbe(server);
			return;
		}
		if ("signal".equals(phase)) {
			advanceSignalProbe(server);
			return;
		}
		if (!multiClientMovementStarted && server.getPlayerList().getPlayerCount() < 2)
			return;

		CarriageContraptionEntity entity = persistedCarriage();
		Train train = entity.getCarriage().train;
		if (++multiClientReadyTicks == 300) {
			applyMultiClientUpdate(entity, train);
			multiClientMovementStarted = true;
			LOGGER.info("Railward NET-02 applied the shared live update and started train movement");
		}
		if (multiClientMovementStarted && !multiClientMovementStopped) {
			train.speed = .025;
			train.targetSpeed = .025;
			train.manualTick = true;
		}
		if (multiClientReadyTicks == 340) {
			train.speed = 0;
			train.targetSpeed = 0;
			train.manualTick = true;
			multiClientMovementStopped = true;
			LOGGER.info("Railward NET-02 completed the bounded train movement");
		}
	}

	@SubscribeEvent
	public static void stopAfterNetworkProbes(PlayerLoggedOutEvent event) {
		String phase = System.getProperty(PHASE_PROPERTY, "").trim();
		if (!("network".equals(phase) || "network_multi".equals(phase) || "controller".equals(phase)
			|| "scheduled".equals(phase) || "scheduled_route".equals(phase) || "signal".equals(phase)
			|| "coupler".equals(phase)
			|| "coupler_auto".equals(phase) || "coupler_storage".equals(phase)
			|| "cargo_unloading".equals(phase))
			|| RailwardEntity.level(event.getPlayer()).isClientSide)
			return;
		String testId = "cargo_unloading".equals(phase) ? "CARGO-03"
			: "coupler_storage".equals(phase) ? "COUPLE-03"
			: "coupler_auto".equals(phase) ? "COUPLE-02" : "coupler".equals(phase) ? "COUPLE-01"
			: "signal".equals(phase) ? "SIGNAL-01"
			: "scheduled_route".equals(phase) ? "CTRL-03 route"
			: "scheduled".equals(phase) ? "CTRL-03" : "controller".equals(phase) ? "CTRL-01/CTRL-02"
			: "network_multi".equals(phase) ? "NET-02" : "NET-01";
		if ("scheduled_route".equals(phase) && scheduledRouteProbeStage < 4)
			throw new IllegalStateException("CTRL-03 route client disconnected before the autonomous route completed; "
				+ "server stage=" + scheduledRouteProbeStage);
		if ("signal".equals(phase) && signalProbeStage < 4)
			throw new IllegalStateException("SIGNAL-01 client disconnected before both directional passes completed; "
				+ "server stage=" + signalProbeStage);
		if ("coupler".equals(phase) && couplerProbeStage < 2)
			throw new IllegalStateException("COUPLE-01 client disconnected before coupling and decoupling completed; "
				+ "server stage=" + couplerProbeStage);
		if ("coupler_auto".equals(phase) && autoCouplerProbeStage < 6)
			throw new IllegalStateException("COUPLE-02 client disconnected before all automatic coupling scenarios completed; "
				+ "server stage=" + autoCouplerProbeStage);
		if ("coupler_storage".equals(phase)) {
			handleStorageCouplerDisconnect(event, testId);
			return;
		}
		if ("cargo_unloading".equals(phase) && !cargoUnloadingServerComplete)
			throw new IllegalStateException("CARGO-03 client disconnected before both unloading paths completed");
		if (++networkProbeDisconnects < 2) {
			LOGGER.info("Railward {} observed client probe disconnect {}/2", testId, networkProbeDisconnects);
			return;
		}
		MinecraftServer server = event.getPlayer().getServer();
		if (server == null)
			throw new IllegalStateException(testId + " client disconnected without a dedicated server");
		LOGGER.info("Railward {} server phase passed; both client probes disconnected cleanly", testId);
		server.saveEverything(false, true, true);
		server.halt(false);
	}

	private static void prepareCargoUnloadingFixture(ServerLevel level) {
		for (int x = -2; x <= 12; x++)
			for (int z = -2; z <= 6; z++) {
				level.setBlock(new BlockPos(x, 99, z), Blocks.STONE.defaultBlockState(), 3);
				for (int y = 100; y <= 104; y++)
					level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 3);
			}

		level.setBlock(CARGO_UNLOAD_DIRECT_CARGO, AllBlocks.BULK_CARGO_CONTAINER.get().defaultBlockState(), 3);
		level.setBlock(CARGO_UNLOAD_DIRECT_HATCH, AllBlocks.UNLOADING_HATCH.get().defaultBlockState(), 3);
		level.setBlock(CARGO_UNLOAD_DIRECT_SOURCE, Blocks.BARREL.defaultBlockState(), 3);
		level.setBlock(CARGO_UNLOAD_DIRECT_LOCK, Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
		setBarrelContents(level, CARGO_UNLOAD_DIRECT_SOURCE, 80);

		level.setBlock(CARGO_UNLOAD_BUNKER, AllBlocks.UNLOADING_BUNKER.get().defaultBlockState(), 3);
		level.setBlock(CARGO_UNLOAD_BUNKER_HATCH, AllBlocks.UNLOADING_HATCH.get().defaultBlockState(), 3);
		level.setBlock(CARGO_UNLOAD_BUNKER_SOURCE, Blocks.BARREL.defaultBlockState(), 3);
		level.setBlock(CARGO_UNLOAD_BUNKER_LOCK, Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
		setBarrelContents(level, CARGO_UNLOAD_BUNKER_SOURCE, 40);

		level.setBlock(CARGO_UNLOAD_BLOCKED_CARGO, AllBlocks.BULK_CARGO_CONTAINER.get().defaultBlockState(), 3);
		level.setBlock(CARGO_UNLOAD_BLOCKED_HATCH, AllBlocks.UNLOADING_HATCH.get().defaultBlockState(), 3);
		level.setBlock(CARGO_UNLOAD_BLOCKED_SOURCE, Blocks.BARREL.defaultBlockState(), 3);
		level.setBlock(CARGO_UNLOAD_BLOCKED_LOCK, Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
		setBarrelContents(level, CARGO_UNLOAD_BLOCKED_SOURCE, 24);
	}

	private static void setBarrelContents(ServerLevel level, BlockPos pos, int amount) {
		BlockEntity blockEntity = level.getBlockEntity(pos);
		if (!(blockEntity instanceof BarrelBlockEntity barrel))
			throw new IllegalStateException("CARGO-03 expected a barrel at " + pos);
		int remaining = amount;
		int slot = 0;
		while (remaining > 0) {
			int count = Math.min(64, remaining);
			barrel.setItem(slot++, new ItemStack(Items.IRON_INGOT, count));
			remaining -= count;
		}
		barrel.setChanged();
	}

	private static void advanceCargoUnloadingProbe(MinecraftServer server) {
		if (server.getPlayerList().getPlayerCount() < 2)
			return;
		ServerLevel level = server.overworld();
		if (++cargoUnloadingProbeTicks > 20 * 45)
			throw new IllegalStateException("CARGO-03 timed out waiting for both unloading paths");

		if (!cargoUnloadingStarted && cargoUnloadingProbeTicks >= 300) {
			level.removeBlock(CARGO_UNLOAD_DIRECT_LOCK, false);
			level.removeBlock(CARGO_UNLOAD_BUNKER_LOCK, false);
			cargoUnloadingStarted = true;
			LOGGER.info("Railward CARGO-03 released the direct hatch and bunker fixtures");
		}
		if (!cargoUnloadingStarted)
			return;

		if (!cargoUnloadingBunkerExtracted) {
			BlockEntity blockEntity = level.getBlockEntity(CARGO_UNLOAD_BUNKER);
			if (!(blockEntity instanceof UnloadingBunkerBlockEntity bunker))
				throw new IllegalStateException("CARGO-03 unloading bunker disappeared");
			IItemHandler exported = bunkerItemHandler(bunker);
			if (exported != null && exported.getSlots() > 0) {
				ItemStack simulated = exported.extractItem(0, 13, true);
				if (!simulated.is(Items.IRON_INGOT) || simulated.getCount() != 13
					|| barrelAmount(level, CARGO_UNLOAD_BUNKER_SOURCE) != 40)
					throw new IllegalStateException("CARGO-03 bunker simulation changed or misreported the source");
				ItemStack extracted = exported.extractItem(0, 13, false);
				if (!extracted.is(Items.IRON_INGOT) || extracted.getCount() != 13
					|| barrelAmount(level, CARGO_UNLOAD_BUNKER_SOURCE) != 27)
					throw new IllegalStateException("CARGO-03 bunker moved the wrong executed amount");
				if (!bunker.getTransferStack().is(Items.IRON_INGOT) || bunker.getTransferDistance() != 2)
					throw new IllegalStateException("CARGO-03 bunker did not publish its transfer effect state");
				cargoUnloadingBunkerExtracted = true;
				LOGGER.info("Railward CARGO-03 bunker simulated and executed an exact 13-item extraction");
			}
		}

		if (!cargoUnloadingDirectTransferred) {
			BlockEntity cargoBlockEntity = level.getBlockEntity(CARGO_UNLOAD_DIRECT_CARGO);
			BlockEntity hatchBlockEntity = level.getBlockEntity(CARGO_UNLOAD_DIRECT_HATCH);
			if (cargoBlockEntity instanceof BulkCargoContainerBlockEntity cargo && cargo.getStoredAmount() == 80) {
				if (barrelAmount(level, CARGO_UNLOAD_DIRECT_SOURCE) != 0
					|| !(hatchBlockEntity instanceof UnloadingHatchBlockEntity hatch)
					|| !hatch.getTransferStack().is(Items.IRON_INGOT) || hatch.getTransferDistance() != 2)
					throw new IllegalStateException("CARGO-03 direct hatch amount or transfer state was wrong");
				cargoUnloadingDirectTransferred = true;
				LOGGER.info("Railward CARGO-03 direct hatch transferred exactly 80 items");
			}
		}

		if (!cargoUnloadingDirectTransferred || !cargoUnloadingBunkerExtracted
			|| cargoUnloadingServerComplete)
			return;
		assertCargoUnloadingFinalState(level);
		cargoUnloadingServerComplete = true;
		LOGGER.info("Railward CARGO-03 server state passed; waiting for both clients to observe visuals and audio");
	}

	private static void assertCargoUnloadingFinalState(ServerLevel level) {
		BlockEntity directCargo = level.getBlockEntity(CARGO_UNLOAD_DIRECT_CARGO);
		BlockEntity blockedCargo = level.getBlockEntity(CARGO_UNLOAD_BLOCKED_CARGO);
		if (!(directCargo instanceof BulkCargoContainerBlockEntity direct) || direct.getStoredAmount() != 80
			|| !(blockedCargo instanceof BulkCargoContainerBlockEntity blocked) || blocked.getStoredAmount() != 0
			|| barrelAmount(level, CARGO_UNLOAD_DIRECT_SOURCE) != 0
			|| barrelAmount(level, CARGO_UNLOAD_BUNKER_SOURCE) != 27
			|| barrelAmount(level, CARGO_UNLOAD_BLOCKED_SOURCE) != 24)
			throw new IllegalStateException("CARGO-03 final source/destination inventory totals were wrong");
		AABB fixtureBounds = new AABB(-2, 99, -2, 13, 105, 3);
		if (!level.getEntitiesOfClass(ItemEntity.class, fixtureBounds).isEmpty())
			throw new IllegalStateException("CARGO-03 created server-side item entities for a visual-only transfer");
	}

	private static int barrelAmount(ServerLevel level, BlockPos pos) {
		BlockEntity blockEntity = level.getBlockEntity(pos);
		if (!(blockEntity instanceof BarrelBlockEntity barrel))
			throw new IllegalStateException("CARGO-03 expected a barrel at " + pos);
		int amount = 0;
		for (int slot = 0; slot < barrel.getContainerSize(); slot++)
			amount += barrel.getItem(slot).getCount();
		return amount;
	}

	private static IItemHandler bunkerItemHandler(UnloadingBunkerBlockEntity bunker) {
		//? if >=1.20.1
		/*return bunker.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.DOWN).orElse(null);*/
		//? if <1.20.1
		return bunker.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, Direction.DOWN).orElse(null);
	}

	private static void advanceControllerProbe(MinecraftServer server) {
		if (controllerProbeStage >= 6)
			return;
		if (!controllerProbeStarted && server.getPlayerList().getPlayerCount() < 2)
			return;
		CarriageContraptionEntity entity = persistedCarriage();
		Train train = entity.getCarriage().train;
		BlockPos controlsPos = controllerPos(entity.getContraption());
		ServerPlayer driver = server.getPlayerList().getPlayerByName("RailwardDriver");
		if (driver == null)
			throw new IllegalStateException("CTRL-01/CTRL-02 could not locate RailwardDriver");

		if (!controllerProbeStarted) {
			if (++controllerProbeReadyTicks < 300)
				return;
			train.runtime.paused = true;
			train.navigation.cancelNavigation();
			setTrainMotion(train, 0, 0);
			updateContraptionBlock(entity, ModernTrainControllerBlock.class, state -> {
				BlockState updated = state
					.setValue(ModernTrainControllerBlock.ACTIVE, false)
					.setValue(ModernTrainControllerBlock.BRAKING, false)
					.setValue(ModernTrainControllerBlock.THROTTLE, false);
				if (!updated.getValue(ModernTrainControllerBlock.REVERSED))
					updated = updated
						.setValue(ModernTrainControllerBlock.FACING,
							updated.getValue(ModernTrainControllerBlock.FACING).getOpposite())
						.setValue(ModernTrainControllerBlock.REVERSED, true);
				return updated;
			});
			entity.setControllingPlayer(driver.getUUID());
			ModernTrainControllerState.beginManualControl(entity, controlsPos, driver);
			TrainDirectionState.syncManualControls(entity, controlsPos, driver);
			controllerProbeStarted = true;
			LOGGER.info("Railward CTRL-01/CTRL-02 activated the stopped reversed controller");
			return;
		}

		controllerProbeStageTicks++;
		BlockState state = controllerState(entity.getContraption());
		switch (controllerProbeStage) {
		case 0 -> {
			if (!state.getValue(ModernTrainControllerBlock.REVERSED)) {
				controllerProbeStage = 1;
				controllerProbeStageTicks = 0;
				ModernTrainControllerState.updateManualControl(entity, controlsPos, List.of(), driver);
				setTrainMotion(train, .01, 0);
				LOGGER.info("Railward CTRL-02 accepted the stopped forward reverser change; testing moving denial");
			} else if (controllerProbeStageTicks > 600) {
				throw new IllegalStateException("CTRL-02 driver did not complete the stopped forward reverser change");
			}
		}
		case 1 -> {
			setTrainMotion(train, .01, 0);
			if (controllerProbeStageTicks >= 100) {
				requireControllerDirection(state, false, "moving denial");
				controllerProbeStage = 2;
				controllerProbeStageTicks = 0;
				ModernTrainControllerState.updateManualControl(entity, controlsPos, List.of(0), driver);
				setTrainMotion(train, .01, .01);
				LOGGER.info("Railward CTRL-02 preserved direction while moving; testing throttle denial");
			}
		}
		case 2 -> {
			setTrainMotion(train, .01, .01);
			if (controllerProbeStageTicks >= 100) {
				requireControllerDirection(state, false, "throttle denial");
				controllerProbeStage = 3;
				controllerProbeStageTicks = 0;
				ModernTrainControllerState.updateManualControl(entity, controlsPos, List.of(1), driver);
				setTrainMotion(train, .01, 0);
				LOGGER.info("Railward CTRL-02 preserved direction under throttle; testing brake denial");
			}
		}
		case 3 -> {
			setTrainMotion(train, .01, 0);
			if (controllerProbeStageTicks >= 100) {
				requireControllerDirection(state, false, "brake denial");
				controllerProbeStage = 4;
				controllerProbeStageTicks = 0;
				setTrainMotion(train, 0, 0);
				ModernTrainControllerState.updateManualControl(entity, controlsPos, List.of(), driver);
				LOGGER.info("Railward CTRL-02 preserved direction while braking; controller is stopped for reverse");
			}
		}
		case 4 -> {
			setTrainMotion(train, 0, 0);
			if (state.getValue(ModernTrainControllerBlock.REVERSED)) {
				controllerProbeStage = 5;
				controllerProbeStageTicks = 0;
				ModernTrainControllerState.updateManualControl(entity, controlsPos, List.of(0), driver);
				setTrainMotion(train, -.01, -.01);
				LOGGER.info("Railward CTRL-02 accepted the stopped backward reverser change; testing reverse throttle");
			} else if (controllerProbeStageTicks > 600) {
				throw new IllegalStateException("CTRL-02 driver did not complete the stopped backward reverser change");
			}
		}
		case 5 -> {
			setTrainMotion(train, -.01, -.01);
			if (controllerProbeStageTicks >= 100) {
				setTrainMotion(train, 0, 0);
				entity.stopControlling(controlsPos);
				controllerProbeStage = 6;
				controllerProbeStageTicks = 0;
				LOGGER.info("Railward CTRL-01 completed forward/coast/brake/backward control and released the controller");
			}
		}
		default -> setTrainMotion(train, 0, 0);
		}
	}

	private static void advanceScheduledProbe(MinecraftServer server) {
		if (scheduledProbeStage >= 6 || server.getPlayerList().getPlayerCount() < 2)
			return;
		CarriageContraptionEntity entity = persistedCarriage();
		Train train = entity.getCarriage().train;
		if (!(entity.getContraption() instanceof CarriageContraption contraption))
			throw new IllegalStateException("CTRL-03 persisted entity is not a carriage contraption");
		if (train.runtime.getSchedule() == null)
			throw new IllegalStateException("CTRL-03 persisted train has no schedule");

		if (scheduledProbeStage == -1) {
			if (++scheduledProbeReadyTicks < 300)
				return;
			int seatIndex = -1;
			for (int i = 0; i < contraption.getSeats().size(); i++)
				if (ModernTrainControllerAssembly.hasControllerFacingSeat(contraption, contraption.getSeats().get(i))) {
					seatIndex = i;
					break;
				}
			if (seatIndex < 0)
				throw new IllegalStateException("CTRL-03 could not find a conductor seat facing the modern controller");
			ServerLevel level = (ServerLevel) entity.level;
			Villager conductor = EntityType.VILLAGER.create(level);
			if (conductor == null)
				throw new IllegalStateException("CTRL-03 could not create its NPC conductor");
			conductor.moveTo(entity.position());
			level.addFreshEntity(conductor);
			entity.addSittingPassenger(conductor, seatIndex);
			if (!contraption.getSeats().get(seatIndex).equals(contraption.getSeatOf(conductor.getUUID())))
				throw new IllegalStateException("CTRL-03 NPC did not occupy the controller seat");
			train.navigation.cancelNavigation();
			train.leaveStation();
			train.runtime.state = State.IN_TRANSIT;
			train.runtime.paused = false;
			train.currentlyBackwards = false;
			train.navigation.destinationBehindTrain = false;
			setTrainMotion(train, .003, .01);
			scheduledProbeStage = 0;
			scheduledProbeStageTicks = 0;
			LOGGER.info("Railward CTRL-03 seated the NPC conductor and started scheduled forward throttle");
			return;
		}

		scheduledProbeStageTicks++;
		BlockState state = controllerState(contraption);
		switch (scheduledProbeStage) {
		case 0 -> {
			setTrainMotion(train, .003, .01);
			if (scheduledProbeStageTicks >= 20)
				requireScheduledState(train, state, DirectionState.FORWARD, true, false, "forward throttle");
			if (scheduledProbeStageTicks >= 100) {
				scheduledProbeStage = 1;
				scheduledProbeStageTicks = 0;
				setTrainMotion(train, .006, 0);
				LOGGER.info("Railward CTRL-03 switched the scheduled train to forward braking");
			}
		}
		case 1 -> {
			setTrainMotion(train, .006, 0);
			if (scheduledProbeStageTicks >= 20)
				requireScheduledState(train, state, DirectionState.FORWARD, false, true, "forward braking");
			if (scheduledProbeStageTicks >= 100) {
				scheduledProbeStage = 2;
				scheduledProbeStageTicks = 0;
				setTrainMotion(train, 0, 0);
				LOGGER.info("Railward CTRL-03 stopped with the scheduled controller idle");
			}
		}
		case 2 -> {
			setTrainMotion(train, 0, 0);
			if (scheduledProbeStageTicks >= 20)
				requireScheduledState(train, state, DirectionState.FORWARD, false, false, "scheduled stop");
			if (scheduledProbeStageTicks >= 100) {
				scheduledProbeStage = 3;
				scheduledProbeStageTicks = 0;
				train.currentlyBackwards = true;
				train.navigation.destinationBehindTrain = true;
				setTrainMotion(train, -.003, -.01);
				LOGGER.info("Railward CTRL-03 switched to scheduled backward throttle");
			}
		}
		case 3 -> {
			setTrainMotion(train, -.003, -.01);
			if (scheduledProbeStageTicks >= 20)
				requireScheduledState(train, state, DirectionState.BACKWARD, true, false, "backward throttle");
			if (scheduledProbeStageTicks >= 100) {
				scheduledProbeStage = 4;
				scheduledProbeStageTicks = 0;
				setTrainMotion(train, -.006, 0);
				LOGGER.info("Railward CTRL-03 switched the scheduled train to backward braking");
			}
		}
		case 4 -> {
			setTrainMotion(train, -.006, 0);
			if (scheduledProbeStageTicks >= 20)
				requireScheduledState(train, state, DirectionState.BACKWARD, false, true, "backward braking");
			if (scheduledProbeStageTicks >= 100) {
				scheduledProbeStage = 5;
				scheduledProbeStageTicks = 0;
				setTrainMotion(train, 0, 0);
				train.runtime.paused = true;
				LOGGER.info("Railward CTRL-03 paused the schedule and released the NPC controller");
			}
		}
		case 5 -> {
			setTrainMotion(train, 0, 0);
			if (scheduledProbeStageTicks >= 20) {
				if (state.getValue(ModernTrainControllerBlock.ACTIVE)
					|| state.getValue(ModernTrainControllerBlock.THROTTLE)
					|| state.getValue(ModernTrainControllerBlock.BRAKING)
					|| TrainDirectionState.resolveServer(train) != DirectionState.NONE)
					throw new IllegalStateException("CTRL-03 did not return to idle after schedule pause");
				scheduledProbeStage = 6;
				LOGGER.info("Railward CTRL-03 server sequence completed: forward, brake, stop, reverse, pause");
			}
		}
		default -> throw new IllegalStateException("Unknown CTRL-03 stage " + scheduledProbeStage);
		}
	}

	private static void advanceScheduledRouteProbe(MinecraftServer server) {
		if (scheduledRouteProbeStage >= 4 || server.getPlayerList().getPlayerCount() < 2)
			return;
		CarriageContraptionEntity entity = persistedCarriage();
		Train train = entity.getCarriage().train;
		if (!(entity.getContraption() instanceof CarriageContraption contraption))
			throw new IllegalStateException("CTRL-03 route entity is not a carriage contraption");
		if (++scheduledRouteProbeTicks > 20 * 90)
			throw new IllegalStateException("CTRL-03 autonomous route timed out at stage " + scheduledRouteProbeStage
				+ ": destination=" + (train.navigation.destination == null ? "<none>" : train.navigation.destination.name)
				+ ", currentStation=" + (train.getCurrentStation() == null ? "<none>" : train.getCurrentStation().name)
				+ ", runtimeState=" + train.runtime.state + ", paused=" + train.runtime.paused
				+ ", speed=" + train.speed + ", targetSpeed=" + train.targetSpeed);

		if (scheduledRouteProbeStage == -1) {
			if (scheduledRouteProbeTicks < 300)
				return;
			int seatIndex = -1;
			for (int i = 0; i < contraption.getSeats().size(); i++)
				if (ModernTrainControllerAssembly.hasControllerFacingSeat(contraption, contraption.getSeats().get(i))) {
					seatIndex = i;
					break;
				}
			if (seatIndex < 0)
				throw new IllegalStateException("CTRL-03 route could not find a conductor seat facing the controller");
			ServerLevel level = (ServerLevel) entity.level;
			Villager conductor = EntityType.VILLAGER.create(level);
			if (conductor == null)
				throw new IllegalStateException("CTRL-03 route could not create its NPC conductor");
			conductor.moveTo(entity.position());
			level.addFreshEntity(conductor);
			entity.addSittingPassenger(conductor, seatIndex);
			if (!contraption.getSeats().get(seatIndex).equals(contraption.getSeatOf(conductor.getUUID())))
				throw new IllegalStateException("CTRL-03 route NPC did not occupy the controller seat");
			train.carriages.forEach(carriage -> carriage.updateConductors());
			boolean forwardConductor = train.hasForwardConductor();
			boolean backwardConductor = train.hasBackwardConductor();
			if (!forwardConductor && !backwardConductor)
				throw new IllegalStateException("CTRL-03 route NPC was seated but Create did not recognize a conductor");

			if (train.graph == null)
				throw new IllegalStateException("CTRL-03 route train has no track graph");
			Collection<GlobalStation> allStations = train.graph.getPoints(EdgePointType.STATION);
			ArrayList<GlobalStation> matchingStations = new ArrayList<>();
			for (GlobalStation station : allStations)
				if (ROUTE_DESTINATION_NAME.equals(station.name))
					matchingStations.add(station);
			DiscoveredPath route = train.navigation.findPathTo(matchingStations, Double.MAX_VALUE);
			if (route == null)
				throw new IllegalStateException("CTRL-03 route preflight could not reach '" + ROUTE_DESTINATION_NAME
					+ "'; stations=" + allStations.stream().map(station -> station.name).toList()
					+ ", matching=" + matchingStations.size() + ", forwardConductor=" + forwardConductor
					+ ", backwardConductor=" + backwardConductor + ", doubleEnded=" + train.doubleEnded
					+ ", currentStation=" + (train.getCurrentStation() == null ? "<none>" : train.getCurrentStation().name));
			LOGGER.info("Railward CTRL-03 route preflight found {} station(s) and a {}-block path to {} "
				+ "(forwardConductor={}, backwardConductor={}, signedDistance={})", allStations.size(),
				route.distance, route.destination.name, forwardConductor, backwardConductor, route.distance);

			Schedule schedule = new Schedule();
			ScheduleEntry destinationEntry = new ScheduleEntry();
			DestinationInstruction destination = new DestinationInstruction();
			CompoundTag destinationData = new CompoundTag();
			destinationData.putString("Text", ROUTE_DESTINATION_NAME);
			destination.setData(destinationData);
			destinationEntry.instruction = destination;
			destinationEntry.conditions.add(List.of(new ScheduledDelay()));
			schedule.entries.add(destinationEntry);
			schedule.cyclic = false;
			train.runtime.discardSchedule();
			train.runtime.setSchedule(schedule, true);
			scheduledRouteStartX = entity.getX();
			scheduledRouteStartZ = entity.getZ();
			scheduledRouteProbeStage = 0;
			LOGGER.info("Railward CTRL-03 route seated the NPC and submitted an autonomous destination schedule for {}",
				ROUTE_DESTINATION_NAME);
			return;
		}

		BlockState state = controllerState(contraption);
		DirectionState direction = TrainDirectionState.resolveServer(train);
		double movedSqr = Math.pow(entity.getX() - scheduledRouteStartX, 2)
			+ Math.pow(entity.getZ() - scheduledRouteStartZ, 2);
		switch (scheduledRouteProbeStage) {
		case 0 -> {
			if (train.navigation.destination == null
				|| !ROUTE_DESTINATION_NAME.equals(train.navigation.destination.name)
				|| !state.getValue(ModernTrainControllerBlock.ACTIVE)
				|| !state.getValue(ModernTrainControllerBlock.THROTTLE)
				|| state.getValue(ModernTrainControllerBlock.BRAKING)
				|| direction == DirectionState.NONE)
				return;
			scheduledRouteProbeStage = 1;
			LOGGER.info("Railward CTRL-03 route began under Create navigation: direction={}, target distance={}",
				direction, train.navigation.distanceToDestination);
		}
		case 1 -> {
			if (movedSqr < 16 || !state.getValue(ModernTrainControllerBlock.ACTIVE)
				|| state.getValue(ModernTrainControllerBlock.THROTTLE)
				|| !state.getValue(ModernTrainControllerBlock.BRAKING)
				|| direction == DirectionState.NONE)
				return;
			scheduledRouteProbeStage = 2;
			LOGGER.info("Railward CTRL-03 route observed autonomous movement and braking after {} blocks",
				Math.sqrt(movedSqr));
		}
		case 2 -> {
			if (train.getCurrentStation() == null || !ROUTE_DESTINATION_NAME.equals(train.getCurrentStation().name)
				|| train.runtime.state != State.POST_TRANSIT
				|| !state.getValue(ModernTrainControllerBlock.ACTIVE)
				|| state.getValue(ModernTrainControllerBlock.THROTTLE)
				|| state.getValue(ModernTrainControllerBlock.BRAKING))
				return;
			scheduledRouteProbeStage = 3;
			LOGGER.info("Railward CTRL-03 route arrived at {} and entered its scheduled station delay",
				ROUTE_DESTINATION_NAME);
		}
		case 3 -> {
			if (!train.runtime.paused || !train.runtime.completed
				|| state.getValue(ModernTrainControllerBlock.ACTIVE)
				|| state.getValue(ModernTrainControllerBlock.THROTTLE)
				|| state.getValue(ModernTrainControllerBlock.BRAKING)
				|| direction != DirectionState.NONE)
				return;
			scheduledRouteProbeStage = 4;
			LOGGER.info("Railward CTRL-03 autonomous route server sequence passed: depart, move, brake, arrive, idle");
		}
		default -> throw new IllegalStateException("Unknown CTRL-03 route stage " + scheduledRouteProbeStage);
		}
	}

	private static void advanceSignalProbe(MinecraftServer server) {
		if (signalProbeStage >= 4 || server.getPlayerList().getPlayerCount() < 2)
			return;
		CarriageContraptionEntity entity = persistedCarriage();
		Train train = entity.getCarriage().train;
		if (++signalProbeTicks > 20 * 180)
			throw new IllegalStateException("SIGNAL-01 timed out at stage " + signalProbeStage
				+ ": station=" + (train.getCurrentStation() == null ? "<none>" : train.getCurrentStation().name)
				+ ", destination=" + (train.navigation.destination == null ? "<none>" : train.navigation.destination.name)
				+ ", runtimeState=" + train.runtime.state + ", paused=" + train.runtime.paused
				+ ", speed=" + train.speed + ", throttle=" + train.throttle
				+ ", cab=" + CabSignalTrainState.resolveServer(train));

		if (signalProbeStage == -1) {
			if (signalProbeTicks < 300)
				return;
			prepareSignalProbe(entity, train);
			signalProbeStage = 0;
			signalProbeStageTicks = 0;
			return;
		}

		captureSignalThrottle(train);
		switch (signalProbeStage) {
		case 0 -> {
			if (!train.navigation.isActive() || TrainDirectionState.resolveServer(train) == DirectionState.NONE)
				return;
			if (advanceSignalAspectSequence(train, "outbound")) {
				signalProbeStage = 1;
				signalProbeStageTicks = 0;
			}
		}
		case 1 -> {
			if (train.getCurrentStation() == null
				|| !ROUTE_DESTINATION_NAME.equals(train.getCurrentStation().name)
				|| train.runtime.state != State.POST_TRANSIT)
				return;
			setSignalProbeState(SignalState.INVALID);
			startSignalSchedule(train, SIGNAL_RETURN_NAME, 1, "return");
			signalProbeStage = 2;
			signalProbeStageTicks = 0;
			LOGGER.info("Railward SIGNAL-01 outbound pass reached the destination; submitted a fresh return schedule");
		}
		case 2 -> {
			if (!train.navigation.isActive() || TrainDirectionState.resolveServer(train) == DirectionState.NONE)
				return;
			if (advanceSignalAspectSequence(train, "return")) {
				signalProbeStage = 3;
				signalProbeStageTicks = 0;
			}
		}
		case 3 -> {
			if (!train.runtime.paused || !train.runtime.completed || train.getCurrentStation() == null
				|| !SIGNAL_RETURN_NAME.equals(train.getCurrentStation().name))
				return;
			if (!signalLowLimitApplied || !signalHighLimitApplied)
				throw new IllegalStateException("SIGNAL-01 did not apply both directional speed limits: low="
					+ signalLowLimitApplied + ", high=" + signalHighLimitApplied + ", throttle=" + train.throttle);
			signalProbeStage = 4;
			LOGGER.info("Railward SIGNAL-01 server sequence passed: all four Create aspects and both "
				+ "directional speed/whistle sign passages completed");
		}
		default -> throw new IllegalStateException("Unknown SIGNAL-01 stage " + signalProbeStage);
		}
	}

	private static void prepareSignalProbe(CarriageContraptionEntity entity, Train train) {
		if (!(entity.getContraption() instanceof CarriageContraption contraption)
			|| contraption.getBlocks().values().stream()
				.noneMatch(info -> AllBlocks.CAB_SIGNAL.has(RailwardStructureInfo.state(info))))
			throw new IllegalStateException("SIGNAL-01 fixture has no mounted cab signal");

		int seatIndex = -1;
		for (int i = 0; i < contraption.getSeats().size(); i++)
			if (ModernTrainControllerAssembly.hasControllerFacingSeat(contraption, contraption.getSeats().get(i))) {
				seatIndex = i;
				break;
			}
		if (seatIndex < 0)
			throw new IllegalStateException("SIGNAL-01 could not find its conductor seat");
		ServerLevel level = (ServerLevel) entity.level;
		Villager conductor = EntityType.VILLAGER.create(level);
		if (conductor == null)
			throw new IllegalStateException("SIGNAL-01 could not create its NPC conductor");
		conductor.moveTo(entity.position());
		level.addFreshEntity(conductor);
		entity.addSittingPassenger(conductor, seatIndex);
		train.carriages.forEach(carriage -> carriage.updateConductors());
		if (!train.hasForwardConductor() && !train.hasBackwardConductor())
			throw new IllegalStateException("SIGNAL-01 NPC was seated but Create did not recognize a conductor");

		BlockPos carriagePos = entity.blockPosition();
		int trackY = carriagePos.getY() - 1;
		int trackZ = carriagePos.getZ();
		BlockPos speedTrackPos = new BlockPos(20, trackY, trackZ);
		BlockPos whistleTrackPos = new BlockPos(26, trackY, trackZ);
		BlockPos signalTrackPos = new BlockPos(32, trackY, trackZ);

		SpeedSignEdgePoint speed = addSignalProbePoint(level, speedTrackPos,
			AllEdgePointTypes.SPEED_SIGN, AllEdgePointTypes.SPEED_SIGN.create());
		speed.updateSpeedLimit(speedTrackPos.above().north(2), true, 20);
		speed.updateSpeedLimit(speedTrackPos.above().south(2), false, 60);

		WhistleSignEdgePoint whistle = addSignalProbePoint(level, whistleTrackPos,
			AllEdgePointTypes.WHISTLE_SIGN, AllEdgePointTypes.WHISTLE_SIGN.create());
		whistle.blockEntityAdded(new WhistleSignBlockEntity(AllBlockEntities.WHISTLE_SIGN.get(),
			whistleTrackPos.above().north(2), AllBlocks.WHISTLE_SIGN.get().defaultBlockState()), true);
		whistle.blockEntityAdded(new WhistleSignBlockEntity(AllBlockEntities.WHISTLE_SIGN.get(),
			whistleTrackPos.above().south(2), AllBlocks.WHISTLE_SIGN.get().defaultBlockState()), false);

		signalProbeBoundary = (FixedSignalBoundary) addSignalProbePoint(level, signalTrackPos,
			EdgePointType.SIGNAL, new FixedSignalBoundary());
		signalProbeBoundary.blockEntityAdded(new SignalBlockEntity(
			com.simibubi.create.AllBlockEntityTypes.TRACK_SIGNAL.get(), signalTrackPos.above().north(2),
			com.simibubi.create.AllBlocks.TRACK_SIGNAL.get().defaultBlockState()), true);
		signalProbeBoundary.blockEntityAdded(new SignalBlockEntity(
			com.simibubi.create.AllBlockEntityTypes.TRACK_SIGNAL.get(), signalTrackPos.above().south(2),
			com.simibubi.create.AllBlocks.TRACK_SIGNAL.get().defaultBlockState()), false);
		setSignalProbeState(SignalState.INVALID);

		Create.RAILWAYS.markTracksDirty();
		startSignalSchedule(train, ROUTE_DESTINATION_NAME, 1, "outbound");
		LOGGER.info("Railward SIGNAL-01 installed two-sided 20/60 km/h speed signs, whistle signs and a "
			+ "fixed-aspect signal, then submitted the outbound NPC schedule");
	}

	private static void startSignalSchedule(Train train, String stationName, int waitSeconds, String leg) {
		if (train.graph == null)
			throw new IllegalStateException("SIGNAL-01 " + leg + " route train has no track graph");
		// Create keeps Navigation.destination reserved while the schedule is in POST_TRANSIT.
		// Clear it before preflighting the opposite direction, otherwise findPathTo() deliberately
		// refuses to reverse out of the previous route.
		train.runtime.discardSchedule();
		Collection<GlobalStation> allStations = train.graph.getPoints(EdgePointType.STATION);
		ArrayList<GlobalStation> matchingStations = new ArrayList<>();
		for (GlobalStation station : allStations)
			if (stationName.equals(station.name))
				matchingStations.add(station);
		DiscoveredPath route = train.navigation.findPathTo(matchingStations, Double.MAX_VALUE);
		if (route == null)
			throw new IllegalStateException("SIGNAL-01 " + leg + " route preflight could not reach '"
				+ stationName + "'; stations=" + allStations.stream().map(station -> station.name).toList()
				+ ", matching=" + matchingStations.size() + ", forwardConductor=" + train.hasForwardConductor()
				+ ", backwardConductor=" + train.hasBackwardConductor() + ", doubleEnded=" + train.doubleEnded
				+ ", currentStation=" + (train.getCurrentStation() == null ? "<none>" : train.getCurrentStation().name));

		Schedule schedule = new Schedule();
		schedule.entries.add(signalDestinationEntry(stationName, waitSeconds));
		schedule.cyclic = false;
		train.throttle = .05;
		train.runtime.setSchedule(schedule, true);
		LOGGER.info("Railward SIGNAL-01 {} route preflight found {} station(s) and a {}-block path to {} "
			+ "(forwardConductor={}, backwardConductor={}, doubleEnded={})", leg, allStations.size(),
			route.distance, route.destination.name, train.hasForwardConductor(), train.hasBackwardConductor(),
			train.doubleEnded);
	}

	private static ScheduleEntry signalDestinationEntry(String stationName, int waitSeconds) {
		ScheduleEntry entry = new ScheduleEntry();
		DestinationInstruction destination = new DestinationInstruction();
		CompoundTag destinationData = new CompoundTag();
		destinationData.putString("Text", stationName);
		destination.setData(destinationData);
		entry.instruction = destination;
		ScheduledDelay delay = new ScheduledDelay();
		CompoundTag delayData = new CompoundTag();
		delayData.putInt("Value", waitSeconds);
		delayData.putInt("TimeUnit", 1);
		delay.setData(delayData);
		entry.conditions.add(List.of(delay));
		return entry;
	}

	private static boolean advanceSignalAspectSequence(Train train, String leg) {
		signalProbeStageTicks++;
		if (signalProbeStageTicks == 1)
			setSignalProbeState(SignalState.INVALID);
		if (signalProbeStageTicks == 25) {
			requireSignalAspect(train, CabSignalAspect.WHITE, leg + " invalid");
			setSignalProbeState(SignalState.GREEN);
		}
		if (signalProbeStageTicks == 50) {
			requireSignalAspect(train, CabSignalAspect.GREEN, leg + " green");
			setSignalProbeState(SignalState.YELLOW);
		}
		if (signalProbeStageTicks == 75) {
			requireSignalAspect(train, CabSignalAspect.YELLOW, leg + " yellow");
			setSignalProbeState(SignalState.RED);
		}
		if (signalProbeStageTicks < 100)
			return false;
		requireSignalAspect(train, CabSignalAspect.YELLOW_RED, leg + " red");
		setSignalProbeState(SignalState.GREEN);
		train.throttle = 1;
		LOGGER.info("Railward SIGNAL-01 {} aspect sequence passed: WHITE, GREEN, YELLOW, YELLOW_RED", leg);
		return true;
	}

	private static void requireSignalAspect(Train train, CabSignalAspect expected, String stage) {
		CabSignalAspect actual = CabSignalTrainState.resolveServer(train);
		if (actual != expected)
			throw new IllegalStateException("SIGNAL-01 cab aspect mismatch during " + stage
				+ ": expected=" + expected + ", actual=" + actual);
	}

	private static void setSignalProbeState(SignalState state) {
		if (signalProbeBoundary == null)
			return;
		signalProbeBoundary.cachedStates.set(true, state);
		signalProbeBoundary.cachedStates.set(false, state);
	}

	private static void captureSignalThrottle(Train train) {
		double low = throttleForLimit(train, 20);
		double high = throttleForLimit(train, 60);
		signalLowLimitApplied |= Math.abs(train.throttle - low) < 1.0E-4;
		signalHighLimitApplied |= Math.abs(train.throttle - high) < 1.0E-4;
	}

	private static double throttleForLimit(Train train, int speedKmh) {
		double maxSpeedKmh = train.maxSpeed() * 72;
		int step = Math.max(1, Math.min(18, (int) Math.round(speedKmh / maxSpeedKmh * 18)));
		return step / 18d;
	}

	private static <T extends TrackEdgePoint> T addSignalProbePoint(ServerLevel level, BlockPos trackPos,
		com.simibubi.create.content.trains.graph.EdgePointType<T> type, T point) {
		TrackGraphLocation location = TrackGraphHelper.getGraphLocationAt(level, trackPos,
			AxisDirection.POSITIVE, new Vec3(1, 0, 0));
		if (location == null)
			throw new IllegalStateException("SIGNAL-01 could not resolve track graph location at " + trackPos);
		point.setType(type);
		point.setId(UUID.randomUUID());
		point.setLocation(location.edge, location.position);
		location.graph.addPoint(type, point);
		return point;
	}

	private static void requireScheduledState(Train train, BlockState state, DirectionState direction,
		boolean throttle, boolean braking, String stage) {
		boolean actualActive = state.getValue(ModernTrainControllerBlock.ACTIVE);
		boolean actualThrottle = state.getValue(ModernTrainControllerBlock.THROTTLE);
		boolean actualBraking = state.getValue(ModernTrainControllerBlock.BRAKING);
		DirectionState actualDirection = TrainDirectionState.resolveServer(train);
		if (!actualActive || actualThrottle != throttle || actualBraking != braking
			|| actualDirection != direction)
			throw new IllegalStateException("CTRL-03 server state mismatch during " + stage
				+ ": active=" + actualActive + ", throttle=" + actualThrottle + ", braking=" + actualBraking
				+ ", direction=" + actualDirection + ", schedulePaused=" + train.runtime.paused
				+ ", runtimeState=" + train.runtime.state + ", speed=" + train.speed
				+ ", targetSpeed=" + train.targetSpeed);
	}

	private static void setTrainMotion(Train train, double speed, double targetSpeed) {
		train.speed = speed;
		train.targetSpeed = targetSpeed;
		train.manualTick = true;
	}

	private static void requireControllerDirection(BlockState state, boolean reversed, String stage) {
		if (state.getValue(ModernTrainControllerBlock.REVERSED) != reversed)
			throw new IllegalStateException("CTRL-02 reverser changed during " + stage);
	}

	private static BlockPos controllerPos(Contraption contraption) {
		return RailwardStructureInfo.pos(controllerInfo(contraption));
	}

	private static BlockState controllerState(Contraption contraption) {
		return RailwardStructureInfo.state(controllerInfo(contraption));
	}

	private static StructureBlockInfo controllerInfo(Contraption contraption) {
		return contraption.getBlocks().values().stream()
			.filter(info -> RailwardStructureInfo.state(info).getBlock() instanceof ModernTrainControllerBlock)
			.findFirst()
			.orElseThrow(() -> new IllegalStateException("Controller probe could not locate the modern controller"));
	}

	private static CarriageContraptionEntity persistedCarriage() {
		return Create.RAILWAYS.trains.values().stream()
			.filter(train -> PERSISTENCE_TRAIN_NAME.equals(train.name.getString()))
			.flatMap(train -> train.carriages.stream())
			.map(com.simibubi.create.content.trains.entity.Carriage::anyAvailableEntity)
			.filter(java.util.Objects::nonNull)
			.findFirst()
			.orElseThrow(() -> new IllegalStateException("Network probe could not locate the persisted carriage"));
	}

	private static void advanceCouplerProbe(MinecraftServer server) {
		if (couplerProbeStage >= 2 || server.getPlayerList().getPlayerCount() < 2)
			return;
		if (++couplerProbeTicks < (couplerProbeStage == 0 ? 300 : 160))
			return;

		List<Train> trains = couplerTrains();
		if (couplerProbeStage == 0) {
			if (trains.size() != 2 || trains.stream().anyMatch(train -> train.carriages.size() != 1))
				throw new IllegalStateException("COUPLE-01 expected two one-carriage trains before coupling, found "
					+ describeTrainTopology(trains));
			Train clickedTrain = trains.stream()
				.filter(train -> COUPLER_TRAIN_A.equals(train.name.getString()))
				.findFirst()
				.orElse(trains.get(0));
			CarriageContraptionEntity entity = requireCouplerEntity(clickedTrain.carriages.get(0));
			TrainCouplerHandler.Result result = TrainCouplerHandler.tryDecouple(entity.level, entity,
				requireCouplerPos(entity));
			if (!result.success())
				throw new IllegalStateException("COUPLE-01 could not couple the fixture trains: " + result.message());

			List<Train> combined = couplerTrains();
			if (combined.size() != 1 || combined.get(0).carriages.size() != 2)
				throw new IllegalStateException("COUPLE-01 server topology did not become one two-carriage train: "
					+ describeTrainTopology(combined));
			assertServerEntityBindings(combined);
			couplerProbeStage = 1;
			couplerProbeTicks = 0;
			LOGGER.info("Railward COUPLE-01 coupled two one-carriage trains through the production interaction path");
			return;
		}

		if (trains.size() != 1 || trains.get(0).carriages.size() != 2)
			throw new IllegalStateException("COUPLE-01 lost its coupled topology before decoupling: "
				+ describeTrainTopology(trains));
		Train combined = trains.get(0);
		CarriageContraptionEntity entity = requireCouplerEntity(combined.carriages.get(0));
		TrainCouplerHandler.Result result = TrainCouplerHandler.tryDecouple(entity.level, entity,
			requireCouplerPos(entity));
		if (!result.success())
			throw new IllegalStateException("COUPLE-01 could not decouple the combined train: " + result.message());

		List<Train> split = couplerTrains();
		if (split.size() != 2 || split.stream().anyMatch(train -> train.carriages.size() != 1))
			throw new IllegalStateException("COUPLE-01 server topology did not return to two one-carriage trains: "
				+ describeTrainTopology(split));
		assertServerEntityBindings(split);
		couplerProbeStage = 2;
		couplerProbeTicks = 0;
		LOGGER.info("Railward COUPLE-01 decoupled the train and restored two valid one-carriage topologies");
	}

	private static void advanceStorageCouplerProbe(MinecraftServer server) {
		if (storageCouplerProbeStage >= 3 || server.getPlayerList().getPlayerCount() < 2)
			return;
		if (++storageCouplerProbeTicks > 20 * 120)
			throw new IllegalStateException("COUPLE-03 timed out at server stage " + storageCouplerProbeStage
				+ " with topology " + describeTrainTopology(couplerTrains()));

		if (storageCouplerProbeStage == 0) {
			if (storageCouplerProbeTicks < 300)
				return;
			List<Train> trains = couplerTrains();
			assertStorageCouplerState(trains, 2, 2, false);
			spawnStorageCouplerPassengers((ServerLevel) server.overworld(), trains);
			storageCouplerEntityIds = collectEntityIds(trains);
			storageCouplerPassengerIds = collectSeatedPassengerIds(trains);
			if (storageCouplerPassengerIds.size() != 2)
				throw new IllegalStateException("COUPLE-03 expected two seated fixture passengers, found "
					+ storageCouplerPassengerIds);
			storageCouplerProbeStage = 1;
			storageCouplerProbeTicks = 0;
			LOGGER.info("Railward COUPLE-03 captured two two-carriage trains, four storage signatures and two passengers");
			return;
		}

		if (storageCouplerProbeStage == 1) {
			if (storageCouplerProbeTicks < 160)
				return;
			List<Train> trains = couplerTrains();
			assertStorageCouplerState(trains, 2, 2, false);
			Train clickedTrain = trains.stream()
				.filter(train -> STORAGE_COUPLER_TRAIN_A.equals(train.name.getString()))
				.findFirst()
				.orElseThrow(() -> new IllegalStateException("COUPLE-03 could not locate fixture train A"));
			CarriageContraptionEntity entity = requireTrainCouplerEntity(clickedTrain);
			TrainCouplerHandler.Result result = TrainCouplerHandler.tryDecouple(entity.level, entity,
				requireCouplerPos(entity));
			if (!result.success())
				throw new IllegalStateException("COUPLE-03 could not couple the storage trains: " + result.message());
			assertStorageCouplerState(couplerTrains(), 1, 4, false);
			storageCouplerProbeStage = 2;
			storageCouplerProbeTicks = 0;
			LOGGER.info("Railward COUPLE-03 coupled the storage trains without changing cargo, fluid or passengers");
			return;
		}

		if (storageCouplerProbeTicks < 180)
			return;
		List<Train> trains = couplerTrains();
		assertStorageCouplerState(trains, 1, 4, false);
		Train combined = trains.get(0);
		Schedule schedule = new Schedule();
		ScheduleEntry scheduleEntry = new ScheduleEntry();
		ChangeTitleInstruction instruction = new ChangeTitleInstruction();
		CompoundTag instructionData = new CompoundTag();
		instructionData.putString("Text", "Railward COUPLE-03 retained schedule");
		instruction.setData(instructionData);
		scheduleEntry.instruction = instruction;
		schedule.entries.add(scheduleEntry);
		combined.runtime.setSchedule(schedule, true);
		combined.runtime.paused = true;
		storageCouplerScheduleTag = schedule.write().copy();

		CarriageContraptionEntity entity = requireTrainCouplerEntity(combined);
		TrainCouplerHandler.Result result = TrainCouplerHandler.tryDecouple(entity.level, entity,
			requireCouplerPos(entity));
		if (!result.success())
			throw new IllegalStateException("COUPLE-03 could not split the paused scheduled train: " + result.message());
		List<Train> split = couplerTrains();
		assertStorageCouplerState(split, 2, 2, true);
		Train scheduled = split.stream().filter(train -> train.runtime.getSchedule() != null).findFirst()
			.orElseThrow(() -> new IllegalStateException("COUPLE-03 lost its paused schedule after splitting"));
		scheduled.name = RailwardComponents.literal(STORAGE_COUPLER_FINAL_SCHEDULED);
		split.stream().filter(train -> train != scheduled).findFirst().orElseThrow()
			.name = RailwardComponents.literal(STORAGE_COUPLER_FINAL_UNSCHEDULED);
		Create.RAILWAYS.markTracksDirty();
		storageCouplerProbeStage = 3;
		storageCouplerProbeTicks = 0;
		LOGGER.info("Railward COUPLE-03 restored two two-carriage trains with one paused schedule and stable storage identities");
	}

	private static void spawnStorageCouplerPassengers(ServerLevel level, List<Train> trains) {
		int seats = 0;
		for (Train train : trains)
			for (com.simibubi.create.content.trains.entity.Carriage carriage : train.carriages) {
				CarriageContraptionEntity entity = requireCouplerEntity(carriage);
				if (!(entity.getContraption() instanceof CarriageContraption contraption)
					|| contraption.getSeats().isEmpty())
					continue;
				Villager passenger = EntityType.VILLAGER.create(level);
				if (passenger == null)
					throw new IllegalStateException("COUPLE-03 could not create a fixture passenger");
				passenger.moveTo(entity.position());
				level.addFreshEntity(passenger);
				entity.addSittingPassenger(passenger, 0);
				if (!contraption.getSeats().get(0).equals(contraption.getSeatOf(passenger.getUUID())))
					throw new IllegalStateException("COUPLE-03 passenger did not occupy its fixture seat");
				seats++;
			}
		if (seats != 2)
			throw new IllegalStateException("COUPLE-03 expected exactly two fixture seats, found " + seats);
	}

	private static void assertStorageCouplerState(List<Train> trains, int trainCount, int carriagesPerTrain,
		boolean expectSchedule) {
		if (trains.size() != trainCount
			|| trains.stream().anyMatch(train -> train.carriages.size() != carriagesPerTrain))
			throw new IllegalStateException("COUPLE-03 topology mismatch: " + describeTrainTopology(trains));
		assertServerEntityBindings(trains);
		Set<StorageSignature> signatures = new HashSet<>();
		int couplers = 0;
		for (Train train : trains)
			for (com.simibubi.create.content.trains.entity.Carriage carriage : train.carriages) {
				IItemHandler items = carriage.storage.getItems();
				for (int slot = 0; slot < items.getSlots(); slot++)
					if (!items.getStackInSlot(slot).isEmpty() && !items.getStackInSlot(slot).is(Items.IRON_INGOT))
						throw new IllegalStateException("COUPLE-03 found an unexpected cargo item "
							+ items.getStackInSlot(slot));
				IFluidHandler fluids = carriage.storage.getFluids();
				int fluidAmount = 0;
				for (int tank = 0; tank < fluids.getTanks(); tank++) {
					FluidStack fluid = fluids.getFluidInTank(tank);
					if (!fluid.isEmpty() && fluid.getFluid() != Fluids.WATER)
						throw new IllegalStateException("COUPLE-03 found an unexpected stored fluid " + fluid);
					fluidAmount += fluid.getAmount();
				}
				signatures.add(new StorageSignature(countItems(items), fluidAmount));
				couplers += requireCouplerEntity(carriage).getContraption().getBlocks().values().stream()
					.filter(info -> RailwardStructureInfo.state(info).getBlock() instanceof TrainCouplerBlock)
					.count();
			}
		if (!signatures.equals(STORAGE_SIGNATURES))
			throw new IllegalStateException("COUPLE-03 storage signatures changed: " + signatures);
		if (couplers != 2)
			throw new IllegalStateException("COUPLE-03 expected two boundary couplers, found " + couplers);
		if (!storageCouplerEntityIds.isEmpty() && !collectEntityIds(trains).equals(storageCouplerEntityIds))
			throw new IllegalStateException("COUPLE-03 carriage entity identities changed");
		if (!storageCouplerPassengerIds.isEmpty()
			&& !collectSeatedPassengerIds(trains).equals(storageCouplerPassengerIds))
			throw new IllegalStateException("COUPLE-03 passenger identities or seat bindings changed");
		long schedules = trains.stream().filter(train -> train.runtime.getSchedule() != null).count();
		if (expectSchedule) {
			if (schedules != 1)
				throw new IllegalStateException("COUPLE-03 expected one retained schedule, found " + schedules);
			Train scheduled = trains.stream().filter(train -> train.runtime.getSchedule() != null).findFirst().orElseThrow();
			if (!scheduled.runtime.paused || storageCouplerScheduleTag == null
				|| !scheduled.runtime.getSchedule().write().equals(storageCouplerScheduleTag))
				throw new IllegalStateException("COUPLE-03 changed or resumed the retained schedule");
		} else if (schedules != 0) {
			throw new IllegalStateException("COUPLE-03 fixture unexpectedly contained a schedule");
		}
	}

	private static Set<UUID> collectEntityIds(List<Train> trains) {
		return trains.stream().flatMap(train -> train.carriages.stream())
			.map(RailwardPersistenceGameTests::requireCouplerEntity)
			.map(CarriageContraptionEntity::getUUID)
			.collect(java.util.stream.Collectors.toUnmodifiableSet());
	}

	private static Set<UUID> collectSeatedPassengerIds(List<Train> trains) {
		return trains.stream().flatMap(train -> train.carriages.stream())
			.map(RailwardPersistenceGameTests::requireCouplerEntity)
			.flatMap(entity -> entity.getPassengers().stream()
				.filter(passenger -> passenger instanceof Villager
					&& entity.getContraption().getSeatOf(passenger.getUUID()) != null))
			.map(net.minecraft.world.entity.Entity::getUUID)
			.collect(java.util.stream.Collectors.toUnmodifiableSet());
	}

	private static void handleStorageCouplerDisconnect(PlayerLoggedOutEvent event, String testId) {
		if (storageCouplerProbeStage < 3)
			throw new IllegalStateException("COUPLE-03 client disconnected before coupling and decoupling completed; "
				+ "server stage=" + storageCouplerProbeStage);
		String name = event.getPlayer().getName().getString();
		if ("RailwardObserver".equals(name))
			storageCouplerObserverDisconnected = true;
		else if ("RailwardRejoin".equals(name))
			storageCouplerReconnectDisconnected = true;
		else if ("RailwardDriver".equals(name))
			storageCouplerDriverDisconnected = true;
		LOGGER.info("Railward {} observed {} disconnect; observer={}, reconnectJoined={}, reconnect={}, driver={}",
			testId, name, storageCouplerObserverDisconnected, storageCouplerReconnectJoined,
			storageCouplerReconnectDisconnected, storageCouplerDriverDisconnected);
		if (!storageCouplerObserverDisconnected || !storageCouplerReconnectJoined
			|| !storageCouplerReconnectDisconnected || !storageCouplerDriverDisconnected)
			return;
		MinecraftServer server = event.getPlayer().getServer();
		if (server == null)
			throw new IllegalStateException("COUPLE-03 client disconnected without a dedicated server");
		assertStorageCouplerState(couplerTrains(), 2, 2, true);
		LOGGER.info("Railward COUPLE-03 server phase passed; reconnect and all client probes completed cleanly");
		server.saveEverything(false, true, true);
		server.halt(false);
	}

	private static void advanceAutoCouplerProbe(MinecraftServer server) {
		if (autoCouplerProbeStage >= 6 || server.getPlayerList().getPlayerCount() < 2)
			return;
		if (++autoCouplerProbeTicks > 20 * 90)
			throw new IllegalStateException("COUPLE-02 timed out at server stage " + autoCouplerProbeStage
				+ " with topology " + describeTrainTopology(couplerTrains()));

		switch (autoCouplerProbeStage) {
		case 0 -> {
			if (autoCouplerProbeTicks < 200)
				return;
			List<Train> trains = couplerTrains();
			if (trains.size() != 8 || trains.stream().anyMatch(train -> train.carriages.size() != 1))
				throw new IllegalStateException("COUPLE-02 expected eight independent fixture trains, found "
					+ describeTrainTopology(trains));
			assertServerEntityBindings(trains);
			RailwardConfig.COMMON.trainCouplerAutoCoupling.set(false);
			autoCouplerProbeStage = 1;
			autoCouplerProbeTicks = 0;
			LOGGER.info("Railward COUPLE-02 started the disabled-auto-coupling scenario");
		}
		case 1 -> {
			driveNamedTrainToward(AUTO_DISABLED_MOVER, AUTO_DISABLED_TARGET, .025);
			if (autoCouplerProbeTicks < 10)
				return;
			assertNamedPairRemainsSeparate(AUTO_DISABLED_MOVER, AUTO_DISABLED_TARGET, "disabled auto-coupling");
			stopTrainForProbe(requireNamedCouplerTrain(AUTO_DISABLED_MOVER));
			RailwardConfig.COMMON.trainCouplerAutoCoupling.set(true);
			autoCouplerProbeStage = 2;
			autoCouplerProbeTicks = 0;
			LOGGER.info("Railward COUPLE-02 rejected coupling while automatic coupling was disabled");
		}
		case 2 -> {
			double excessiveSpeed = RailwardConfig.COMMON.trainCouplerAutoMaxSpeed.get() + .01;
			driveNamedTrainToward(AUTO_FAST_MOVER, AUTO_FAST_TARGET, excessiveSpeed);
			if (autoCouplerProbeTicks < 6)
				return;
			assertNamedPairRemainsSeparate(AUTO_FAST_MOVER, AUTO_FAST_TARGET, "above-limit speed");
			stopTrainForProbe(requireNamedCouplerTrain(AUTO_FAST_MOVER));
			autoCouplerProbeStage = 3;
			autoCouplerProbeTicks = 0;
			LOGGER.info("Railward COUPLE-02 rejected coupling above the configured speed limit {}",
				RailwardConfig.COMMON.trainCouplerAutoMaxSpeed.get());
		}
		case 3 -> {
			Train combined = findNamedCouplerTrain(AUTO_WEST_MOVER);
			if (combined == null)
				throw new IllegalStateException("COUPLE-02 lost the westbound moving train");
			if (combined.carriages.size() != 2) {
				driveNamedTrainToward(AUTO_WEST_MOVER, AUTO_WEST_TARGET, .025);
				return;
			}
			if (findNamedCouplerTrain(AUTO_WEST_TARGET) != null)
				throw new IllegalStateException("COUPLE-02 westbound merge retained the consumed target train");
			stopTrainForProbe(combined);
			assertServerEntityBindings(couplerTrains());
			autoCouplerProbeStage = 4;
			autoCouplerProbeTicks = 0;
			LOGGER.info("Railward COUPLE-02 coupled the westbound moving train below the configured speed limit");
		}
		case 4 -> {
			if (autoCouplerProbeTicks < 100)
				return;
			autoCouplerProbeStage = 5;
			autoCouplerProbeTicks = 0;
			LOGGER.info("Railward COUPLE-02 started the opposite-direction permitted scenario");
		}
		case 5 -> {
			Train combined = findNamedCouplerTrain(AUTO_EAST_MOVER);
			if (combined == null)
				throw new IllegalStateException("COUPLE-02 lost the eastbound moving train");
			if (combined.carriages.size() != 2) {
				driveNamedTrainToward(AUTO_EAST_MOVER, AUTO_EAST_TARGET, .025);
				return;
			}
			if (findNamedCouplerTrain(AUTO_EAST_TARGET) != null)
				throw new IllegalStateException("COUPLE-02 eastbound merge retained the consumed target train");
			stopTrainForProbe(combined);
			List<Train> trains = couplerTrains();
			long singleCarriage = trains.stream().filter(train -> train.carriages.size() == 1).count();
			long twoCarriage = trains.stream().filter(train -> train.carriages.size() == 2).count();
			if (trains.size() != 6 || singleCarriage != 4 || twoCarriage != 2)
				throw new IllegalStateException("COUPLE-02 final topology was invalid: "
					+ describeTrainTopology(trains));
			assertNamedPairRemainsSeparate(AUTO_DISABLED_MOVER, AUTO_DISABLED_TARGET, "disabled final topology");
			assertNamedPairRemainsSeparate(AUTO_FAST_MOVER, AUTO_FAST_TARGET, "speed-limit final topology");
			assertServerEntityBindings(trains);
			autoCouplerProbeStage = 6;
			autoCouplerProbeTicks = 0;
			LOGGER.info("Railward COUPLE-02 completed all four scenarios with valid server topology");
		}
		default -> throw new IllegalStateException("Unknown COUPLE-02 server stage " + autoCouplerProbeStage);
		}
	}

	private static void driveNamedTrainToward(String movingName, String targetName, double speedMagnitude) {
		Train moving = requireNamedCouplerTrain(movingName);
		Train target = requireNamedCouplerTrain(targetName);
		CarriageContraptionEntity movingEntity = requireCouplerEntity(moving.carriages.get(0));
		CarriageContraptionEntity targetEntity = requireCouplerEntity(target.carriages.get(0));
		Vec3 leading = moving.carriages.get(0).getLeadingPoint().getPosition(moving.graph);
		Vec3 trailing = moving.carriages.get(0).getTrailingPoint().getPosition(moving.graph);
		Vec3 positiveDirection = leading.subtract(trailing);
		Vec3 targetDirection = targetEntity.position().subtract(movingEntity.position());
		if (positiveDirection.lengthSqr() < 1.0E-6 || targetDirection.lengthSqr() < 1.0E-6)
			throw new IllegalStateException("COUPLE-02 could not resolve the approach direction for " + movingName);
		double speed = positiveDirection.dot(targetDirection) >= 0 ? Math.abs(speedMagnitude) : -Math.abs(speedMagnitude);
		setTrainMotion(moving, speed, speed);
	}

	private static void stopTrainForProbe(Train train) {
		setTrainMotion(train, 0, 0);
		train.speedBeforeStall = null;
	}

	private static void assertNamedPairRemainsSeparate(String firstName, String secondName, String scenario) {
		Train first = requireNamedCouplerTrain(firstName);
		Train second = requireNamedCouplerTrain(secondName);
		if (first == second || first.carriages.size() != 1 || second.carriages.size() != 1)
			throw new IllegalStateException("COUPLE-02 unexpectedly merged the " + scenario + " pair: "
				+ describeTrainTopology(couplerTrains()));
	}

	private static Train requireNamedCouplerTrain(String name) {
		Train train = findNamedCouplerTrain(name);
		if (train == null)
			throw new IllegalStateException("COUPLE-02 could not find fixture train " + name + ": "
				+ describeTrainTopology(couplerTrains()));
		return train;
	}

	private static Train findNamedCouplerTrain(String name) {
		return couplerTrains().stream()
			.filter(train -> name.equals(train.name.getString()))
			.findFirst()
			.orElse(null);
	}

	private static List<Train> couplerTrains() {
		return Create.RAILWAYS.trains.values().stream()
			.filter(train -> train.carriages.stream().anyMatch(carriage -> {
				CarriageContraptionEntity entity = carriage.anyAvailableEntity();
				return entity != null && entity.getContraption().getBlocks().values().stream()
					.anyMatch(info -> RailwardStructureInfo.state(info).getBlock() instanceof TrainCouplerBlock);
			}))
			.toList();
	}

	private static List<CarriageContraptionEntity> couplerCarriages() {
		return couplerTrains().stream()
			.flatMap(train -> train.carriages.stream())
			.map(RailwardPersistenceGameTests::requireCouplerEntity)
			.toList();
	}

	private static CarriageContraptionEntity requireCouplerEntity(
		com.simibubi.create.content.trains.entity.Carriage carriage) {
		CarriageContraptionEntity entity = carriage.anyAvailableEntity();
		if (entity == null)
			throw new IllegalStateException("COUPLE-01 carriage entity is not loaded");
		return entity;
	}

	private static CarriageContraptionEntity requireTrainCouplerEntity(Train train) {
		return train.carriages.stream()
			.map(RailwardPersistenceGameTests::requireCouplerEntity)
			.filter(entity -> entity.getContraption().getBlocks().values().stream()
				.anyMatch(info -> RailwardStructureInfo.state(info).getBlock() instanceof TrainCouplerBlock))
			.findFirst()
			.orElseThrow(() -> new IllegalStateException("Coupler probe train had no loaded coupler entity"));
	}

	private static BlockPos requireCouplerPos(CarriageContraptionEntity entity) {
		List<BlockPos> positions = entity.getContraption().getBlocks().values().stream()
			.filter(info -> RailwardStructureInfo.state(info).getBlock() instanceof TrainCouplerBlock)
			.map(RailwardStructureInfo::pos)
			.toList();
		if (positions.size() != 1)
			throw new IllegalStateException("COUPLE-01 expected exactly one coupler per fixture carriage, found "
				+ positions.size());
		return positions.get(0);
	}

	private static void assertServerEntityBindings(List<Train> trains) {
		for (Train train : trains)
			for (int i = 0; i < train.carriages.size(); i++) {
				CarriageContraptionEntity entity = requireCouplerEntity(train.carriages.get(i));
				if (entity.getCarriage() != train.carriages.get(i) || entity.getCarriage().train != train
					|| !train.id.equals(entity.trainId) || entity.carriageIndex != i)
					throw new IllegalStateException("COUPLE-01 found an invalid server entity binding for train "
						+ train.id + " carriage " + i);
			}
	}

	private static String describeTrainTopology(List<Train> trains) {
		return trains.stream()
			.map(train -> train.name.getString() + "/" + train.id + "=" + train.carriages.size())
			.toList()
			.toString();
	}

	private static void applyMultiClientUpdate(CarriageContraptionEntity entity, Train train) {
		Contraption contraption = entity.getContraption();
		if (!(contraption instanceof CarriageContraption))
			throw new IllegalStateException("NET-02 persisted entity is not a carriage contraption");

		BlockPos cargoPos = new BlockPos(-2, 0, 0);
		BulkCargoContraptionHelper.ControllerData cargoData =
			BulkCargoContraptionHelper.resolveController(contraption, cargoPos);
		IItemHandlerModifiable cargo = BulkCargoContraptionHelper.getItemHandler(contraption, cargoPos);
		if (cargoData == null || !cargoData.controllerPos().equals(cargoPos)
			|| cargoData.sizeX() != 1 || cargoData.sizeZ() != 1 || cargoData.height() != 1
			|| cargo == null || countItems(cargo) != 96
			|| !BulkCargoStorage.insert(cargo, new ItemStack(Items.IRON_INGOT, 16), false).isEmpty())
			throw new IllegalStateException("NET-02 could not apply the mounted cargo update");
		BulkCargoContraptionHelper.onCargoContentsChanged(entity, cargoData, cargo);
		StructureBlockInfo updatedCargoInfo = contraption.getBlocks().get(cargoPos);
		if (updatedCargoInfo == null
			|| RailwardStructureInfo.state(updatedCargoInfo).getValue(BulkCargoContainerBlock.CARGO_LEVEL) != 2
			|| countItems(cargo) != 112)
			throw new IllegalStateException("NET-02 mounted cargo controller or fill level did not update");

		BlockPos cisternPos = new BlockPos(-1, 0, -1);
		CisternContraptionHelper.ControllerData cisternData =
			CisternContraptionHelper.resolveController(contraption, cisternPos);
		IFluidHandler fluid = CisternContraptionHelper.getFluidHandler(contraption, cisternPos);
		if (cisternData == null || fluid == null
			|| fluid.fill(new FluidStack(Fluids.WATER, 500), IFluidHandler.FluidAction.EXECUTE) != 500)
			throw new IllegalStateException("NET-02 could not apply the mounted fluid update");
		CisternContraptionHelper.syncFluid(entity, cisternData);

		updateContraptionBlock(entity, ModernTrainControllerBlock.class, state -> state
			.setValue(ModernTrainControllerBlock.REVERSED, false)
			.setValue(ModernTrainControllerBlock.ACTIVE, false)
			.setValue(ModernTrainControllerBlock.BRAKING, false)
			.setValue(ModernTrainControllerBlock.THROTTLE, false));
		updateContraptionBlock(entity, LocomotiveBufferLightsBlock.class,
			state -> state.setValue(LocomotiveBufferLightsBlock.VARIANT, LocomotiveBufferLightsVariant.TYPE_4));
		updateContraptionBlocks(entity, TrainDoorBlock.class, state -> state
			.setValue(TrainDoorBlock.OPEN, false)
			.setValue(SlidingDoorBlock.VISIBLE, true));

		train.leaveStation();
		train.runtime.state = State.IN_TRANSIT;
		train.currentlyBackwards = false;
		if (train.navigation != null)
			train.navigation.destinationBehindTrain = false;
		train.speed = .025;
		train.targetSpeed = .025;
		train.manualTick = true;
		TrainDirectionState.syncScheduledControls(train, entity.level);
		Create.RAILWAYS.markTracksDirty();
	}

	private static void updateContraptionBlock(CarriageContraptionEntity entity, Class<?> blockType,
		java.util.function.UnaryOperator<BlockState> update) {
		StructureBlockInfo info = entity.getContraption().getBlocks().values().stream()
			.filter(candidate -> blockType.isInstance(RailwardStructureInfo.state(candidate).getBlock()))
			.findFirst()
			.orElseThrow(() -> new IllegalStateException("NET-02 could not find " + blockType.getSimpleName()));
		BlockState updated = update.apply(RailwardStructureInfo.state(info));
		BlockPos pos = RailwardStructureInfo.pos(info);
		entity.setBlock(pos, new StructureBlockInfo(pos, updated, RailwardStructureInfo.nbt(info)));
	}

	private static void updateContraptionBlocks(CarriageContraptionEntity entity, Class<?> blockType,
		java.util.function.UnaryOperator<BlockState> update) {
		List<StructureBlockInfo> matches = entity.getContraption().getBlocks().values().stream()
			.filter(candidate -> blockType.isInstance(RailwardStructureInfo.state(candidate).getBlock()))
			.toList();
		if (matches.isEmpty())
			throw new IllegalStateException("NET-02 could not find " + blockType.getSimpleName());
		for (StructureBlockInfo info : matches) {
			BlockState updated = update.apply(RailwardStructureInfo.state(info));
			BlockPos pos = RailwardStructureInfo.pos(info);
			entity.setBlock(pos, new StructureBlockInfo(pos, updated, RailwardStructureInfo.nbt(info)));
		}
	}

	@GameTestGenerator
	public static Collection<TestFunction> generatePersistenceTest() {
		String phase = System.getProperty(PHASE_PROPERTY, "").trim();
		if ("prepare".equals(phase))
			return List.of(test(PREPARE_TEST_NAME, RailwardPersistenceGameTests::prepareTrainForRestart));
		if ("verify".equals(phase))
			return List.of(test(VERIFY_TEST_NAME, RailwardPersistenceGameTests::verifyTrainAfterRestart));
		if ("coupler_prepare".equals(phase))
			return List.of(test(COUPLER_PREPARE_TEST_NAME, RailwardPersistenceGameTests::prepareCouplerFixture));
		if ("coupler_auto_prepare".equals(phase))
			return List.of(test(AUTO_COUPLER_PREPARE_TEST_NAME,
				RailwardPersistenceGameTests::prepareAutoCouplerFixture));
		if ("coupler_storage_prepare".equals(phase))
			return List.of(test(STORAGE_COUPLER_PREPARE_TEST_NAME,
				RailwardPersistenceGameTests::prepareStorageCouplerFixture));
		return List.of();
	}

	private static TestFunction test(String name, java.util.function.Consumer<GameTestHelper> function) {
		int timeout = AUTO_COUPLER_PREPARE_TEST_NAME.equals(name) ? 600
			: STORAGE_COUPLER_PREPARE_TEST_NAME.equals(name) ? 420
			: COUPLER_PREPARE_TEST_NAME.equals(name) ? 320 : 200;
		return new TestFunction("railwardPersistence", name, EMPTY_TEMPLATE, timeout, 1, true, function);
	}

	private static void prepareAutoCouplerFixture(GameTestHelper helper) {
		List<AutoCouplerTrainSpec> specs = List.of(
			new AutoCouplerTrainSpec(AUTO_DISABLED_MOVER, 12, 5, 11, 10, 11, Direction.WEST, false),
			new AutoCouplerTrainSpec(AUTO_DISABLED_TARGET, 7, 11, 8, 9, 8, Direction.EAST, true),
			new AutoCouplerTrainSpec(AUTO_FAST_MOVER, 30, 5, 29, 28, 29, Direction.WEST, false),
			new AutoCouplerTrainSpec(AUTO_FAST_TARGET, 25, 11, 26, 27, 26, Direction.EAST, true),
			new AutoCouplerTrainSpec(AUTO_WEST_MOVER, 48, 5, 47, 46, 47, Direction.WEST, false),
			new AutoCouplerTrainSpec(AUTO_WEST_TARGET, 43, 11, 44, 45, 44, Direction.EAST, true),
			new AutoCouplerTrainSpec(AUTO_EAST_TARGET, 66, 5, 65, 64, 65, Direction.WEST, false),
			new AutoCouplerTrainSpec(AUTO_EAST_MOVER, 61, 11, 62, 63, 62, Direction.EAST, true));

		for (int x = 1; x <= 72; x++)
			helper.setBlock(new BlockPos(x, 1, 8), com.simibubi.create.AllBlocks.TRACK.get()
				.defaultBlockState()
				.setValue(TrackBlock.SHAPE, TrackShape.XO));
		for (AutoCouplerTrainSpec spec : specs)
			helper.setBlock(new BlockPos(spec.stationX(), 1, spec.stationZ()),
				com.simibubi.create.AllBlocks.TRACK_STATION.get());

		helper.runAfterDelay(5, () -> {
			for (AutoCouplerTrainSpec spec : specs)
				configureFixtureStation(helper, new BlockPos(spec.stationX(), 1, spec.stationZ()),
					new BlockPos(spec.stationX(), 1, 8), spec.targetDirection());
			assembleAutoCouplerFixtureTrain(helper, specs, 0);
		});
	}

	private static void assembleAutoCouplerFixtureTrain(GameTestHelper helper, List<AutoCouplerTrainSpec> specs,
		int index) {
		if (index >= specs.size()) {
			helper.runAfterDelay(15, () -> {
				List<Train> trains = couplerTrains();
				require(helper, trains.size() == 8 && trains.stream().allMatch(train -> train.carriages.size() == 1),
					"auto-coupler fixture did not contain eight one-carriage trains: "
						+ describeTrainTopology(trains));
				for (AutoCouplerTrainSpec spec : specs)
					require(helper, findNamedCouplerTrain(spec.name()) != null,
						"auto-coupler fixture was missing " + spec.name());
				Create.RAILWAYS.markTracksDirty();
				MinecraftServer server = helper.getLevel().getServer();
				require(helper, server != null, "auto-coupler fixture dedicated server was unavailable");
				LOGGER.info("Railward COUPLE-02 fixture prepare phase passed; saving four isolated train pairs");
				helper.succeed();
				server.saveEverything(false, true, true);
				server.halt(false);
			});
			return;
		}

		AutoCouplerTrainSpec spec = specs.get(index);
		BlockPos bogeyPos = new BlockPos(spec.bogeyX(), 2, 8);
		BlockPos couplerPos = new BlockPos(spec.couplerX(), 2, 8);
		BlockPos controlsPos = new BlockPos(spec.controlsX(), 3, 8);
		helper.setBlock(bogeyPos, com.simibubi.create.AllBlocks.SMALL_BOGEY.get().defaultBlockState()
			.setValue(AbstractBogeyBlock.AXIS, Direction.Axis.X));
		helper.setBlock(couplerPos, AllBlocks.TRAIN_COUPLER.get().defaultBlockState()
			.setValue(TrainCouplerBlock.FACING, spec.facing()));
		helper.setBlock(controlsPos, com.simibubi.create.AllBlocks.TRAIN_CONTROLS.get().defaultBlockState()
			.setValue(ControlsBlock.FACING, spec.facing()));
		for (BlockPos[] glued : List.of(
			new BlockPos[] { bogeyPos, couplerPos }, new BlockPos[] { bogeyPos, controlsPos }))
			helper.getLevel().addFreshEntity(new SuperGlueEntity(helper.getLevel(),
				SuperGlueEntity.span(helper.absolutePos(glued[0]), helper.absolutePos(glued[1]))));

		helper.runAfterDelay(30, () -> {
			BlockEntity blockEntity = helper.getBlockEntity(new BlockPos(spec.stationX(), 1, spec.stationZ()));
			require(helper, blockEntity instanceof StationBlockEntity,
				"auto-coupler fixture station was missing for " + spec.name());
			assembleFixtureTrain(helper, (StationBlockEntity) blockEntity, spec.name());
			assembleAutoCouplerFixtureTrain(helper, specs, index + 1);
		});
	}

	private static void prepareCouplerFixture(GameTestHelper helper) {
		BlockPos stationAPos = new BlockPos(12, 1, 5);
		BlockPos stationATrackPos = new BlockPos(12, 1, 8);
		BlockPos bogeyAPos = new BlockPos(11, 2, 8);
		BlockPos couplerAPos = new BlockPos(10, 2, 8);
		BlockPos controlsAPos = new BlockPos(11, 3, 8);
		BlockPos stationBPos = new BlockPos(7, 1, 11);
		BlockPos stationBTrackPos = new BlockPos(7, 1, 8);
		BlockPos bogeyBPos = new BlockPos(8, 2, 8);
		BlockPos couplerBPos = new BlockPos(9, 2, 8);
		BlockPos controlsBPos = new BlockPos(8, 3, 8);

		for (int x = 1; x <= 18; x++)
			helper.setBlock(new BlockPos(x, 1, 8), com.simibubi.create.AllBlocks.TRACK.get()
				.defaultBlockState()
				.setValue(TrackBlock.SHAPE, TrackShape.XO));
		helper.setBlock(stationAPos, com.simibubi.create.AllBlocks.TRACK_STATION.get());
		helper.setBlock(stationBPos, com.simibubi.create.AllBlocks.TRACK_STATION.get());
		helper.setBlock(bogeyAPos, com.simibubi.create.AllBlocks.SMALL_BOGEY.get().defaultBlockState()
			.setValue(AbstractBogeyBlock.AXIS, Direction.Axis.X));
		helper.setBlock(couplerAPos, AllBlocks.TRAIN_COUPLER.get().defaultBlockState()
			.setValue(TrainCouplerBlock.FACING, Direction.WEST));
		helper.setBlock(controlsAPos, com.simibubi.create.AllBlocks.TRAIN_CONTROLS.get().defaultBlockState()
			.setValue(ControlsBlock.FACING, Direction.WEST));
		for (BlockPos[] glued : List.of(
			new BlockPos[] { bogeyAPos, couplerAPos }, new BlockPos[] { bogeyAPos, controlsAPos }))
			helper.getLevel().addFreshEntity(new SuperGlueEntity(helper.getLevel(),
				SuperGlueEntity.span(helper.absolutePos(glued[0]), helper.absolutePos(glued[1]))));

		helper.runAfterDelay(5, () -> {
			StationBlockEntity stationA = configureFixtureStation(helper, stationAPos, stationATrackPos, false);
			StationBlockEntity stationB = configureFixtureStation(helper, stationBPos, stationBTrackPos, true);
			helper.runAfterDelay(30, () -> {
				Train trainA = assembleFixtureTrain(helper, stationA, COUPLER_TRAIN_A);
				helper.setBlock(bogeyBPos, com.simibubi.create.AllBlocks.SMALL_BOGEY.get().defaultBlockState()
					.setValue(AbstractBogeyBlock.AXIS, Direction.Axis.X));
				helper.setBlock(couplerBPos, AllBlocks.TRAIN_COUPLER.get().defaultBlockState()
					.setValue(TrainCouplerBlock.FACING, Direction.EAST));
				helper.setBlock(controlsBPos, com.simibubi.create.AllBlocks.TRAIN_CONTROLS.get().defaultBlockState()
					.setValue(ControlsBlock.FACING, Direction.EAST));
				for (BlockPos[] glued : List.of(
					new BlockPos[] { bogeyBPos, couplerBPos }, new BlockPos[] { bogeyBPos, controlsBPos }))
					helper.getLevel().addFreshEntity(new SuperGlueEntity(helper.getLevel(),
						SuperGlueEntity.span(helper.absolutePos(glued[0]), helper.absolutePos(glued[1]))));
				helper.runAfterDelay(10, () -> {
					Train trainB = assembleFixtureTrain(helper, stationB, COUPLER_TRAIN_B);
					helper.runAfterDelay(15, () -> {
					List<Train> trains = couplerTrains();
					require(helper, trains.size() == 2 && trains.stream().allMatch(train -> train.carriages.size() == 1),
						"coupler fixture did not contain two one-carriage trains: " + describeTrainTopology(trains));
					require(helper, requireCouplerEntity(trainA.carriages.get(0)).position().distanceToSqr(
						requireCouplerEntity(trainB.carriages.get(0)).position()) < 25,
						"coupler fixture trains were not assembled near each other");
					Create.RAILWAYS.markTracksDirty();
					MinecraftServer server = helper.getLevel().getServer();
					require(helper, server != null, "coupler fixture dedicated server was unavailable");
					LOGGER.info("Railward COUPLE-01 fixture prepare phase passed; saving two one-carriage trains");
					helper.succeed();
					server.saveEverything(false, true, true);
					server.halt(false);
					});
				});
			});
		});
	}

	private static void prepareStorageCouplerFixture(GameTestHelper helper) {
		BlockPos stationAPos = new BlockPos(17, 1, 5);
		BlockPos stationATrackPos = new BlockPos(17, 1, 8);
		BlockPos stationBPos = new BlockPos(4, 1, 11);
		BlockPos stationBTrackPos = new BlockPos(4, 1, 8);

		for (int x = 1; x <= 22; x++)
			helper.setBlock(new BlockPos(x, 1, 8), com.simibubi.create.AllBlocks.TRACK.get()
				.defaultBlockState()
				.setValue(TrackBlock.SHAPE, TrackShape.XO));
		helper.setBlock(stationAPos, com.simibubi.create.AllBlocks.TRACK_STATION.get());
		helper.setBlock(stationBPos, com.simibubi.create.AllBlocks.TRACK_STATION.get());

		helper.runAfterDelay(5, () -> {
			StationBlockEntity stationA = configureFixtureStation(helper, stationAPos, stationATrackPos, false);
			StationBlockEntity stationB = configureFixtureStation(helper, stationBPos, stationBTrackPos, true);
			placeStorageCarriage(helper, 16, Direction.WEST, null, false);
			placeStorageCarriage(helper, 12, Direction.WEST, 11, true);
			helper.runAfterDelay(5, () -> {
				fillStorageCarriage(helper, 16, 11, 1100);
				fillStorageCarriage(helper, 12, 22, 1200);
				helper.runAfterDelay(30, () -> {
					assembleFixtureTrain(helper, stationA, STORAGE_COUPLER_TRAIN_A, 2);
					placeStorageCarriage(helper, 5, Direction.EAST, null, false);
					placeStorageCarriage(helper, 9, Direction.EAST, 10, true);
					helper.runAfterDelay(5, () -> {
						fillStorageCarriage(helper, 5, 33, 1300);
						fillStorageCarriage(helper, 9, 44, 1400);
						helper.runAfterDelay(30, () -> {
							assembleFixtureTrain(helper, stationB, STORAGE_COUPLER_TRAIN_B, 2);
							helper.runAfterDelay(20, () -> {
								assertStorageCouplerState(couplerTrains(), 2, 2, false);
								Create.RAILWAYS.markTracksDirty();
								MinecraftServer server = helper.getLevel().getServer();
								require(helper, server != null,
									"storage coupler fixture dedicated server was unavailable");
								LOGGER.info("Railward COUPLE-03 fixture prepare phase passed; saving four storage carriages");
								helper.succeed();
								server.saveEverything(false, true, true);
								server.halt(false);
							});
						});
					});
				});
			});
		});
	}

	private static void placeStorageCarriage(GameTestHelper helper, int bogeyX, Direction facing,
		Integer couplerX, boolean seat) {
		BlockPos bogeyPos = new BlockPos(bogeyX, 2, 8);
		BlockPos cargoPos = new BlockPos(bogeyX, 2, 7);
		BlockPos cisternPos = new BlockPos(bogeyX, 2, 9);
		BlockPos topPos = new BlockPos(bogeyX, 3, 8);
		helper.setBlock(bogeyPos, com.simibubi.create.AllBlocks.SMALL_BOGEY.get().defaultBlockState()
			.setValue(AbstractBogeyBlock.AXIS, Direction.Axis.X));
		helper.setBlock(cargoPos, AllBlocks.BULK_CARGO_CONTAINER.get());
		helper.setBlock(cisternPos, AllBlocks.CISTERN.get().defaultBlockState()
			.setValue(CisternBlock.AXIS, Direction.Axis.X));
		helper.setBlock(topPos, seat
			? com.simibubi.create.AllBlocks.SEATS.get(DyeColor.RED).get().defaultBlockState()
			: com.simibubi.create.AllBlocks.TRAIN_CONTROLS.get().defaultBlockState()
				.setValue(ControlsBlock.FACING, facing));
		List<BlockPos> attached = new ArrayList<>(List.of(cargoPos, cisternPos, topPos));
		if (couplerX != null) {
			BlockPos couplerPos = new BlockPos(couplerX, 2, 8);
			helper.setBlock(couplerPos, AllBlocks.TRAIN_COUPLER.get().defaultBlockState()
				.setValue(TrainCouplerBlock.FACING, facing));
			attached.add(couplerPos);
		}
		for (BlockPos attachedPos : attached)
			helper.getLevel().addFreshEntity(new SuperGlueEntity(helper.getLevel(),
				SuperGlueEntity.span(helper.absolutePos(bogeyPos), helper.absolutePos(attachedPos))));
	}

	private static void fillStorageCarriage(GameTestHelper helper, int bogeyX, int items, int fluid) {
		BulkCargoContainerBlockEntity cargo = bulkCargoAt(helper, new BlockPos(bogeyX, 2, 7));
		require(helper, cargo.tryInsert(new ItemStack(Items.IRON_INGOT, items), false).isEmpty(),
			"COUPLE-03 fixture cargo could not accept " + items + " iron ingots");
		CisternBlockEntity cistern = cisternAt(helper, new BlockPos(bogeyX, 2, 9));
		require(helper, cistern.getTankInventory().fill(new FluidStack(Fluids.WATER, fluid),
			IFluidHandler.FluidAction.EXECUTE) == fluid,
			"COUPLE-03 fixture cistern could not accept " + fluid + " mB of water");
	}

	private static StationBlockEntity configureFixtureStation(GameTestHelper helper, BlockPos stationPos,
		BlockPos targetTrackPos, boolean targetDirection) {
		BlockEntity blockEntity = helper.getBlockEntity(stationPos);
		require(helper, blockEntity instanceof StationBlockEntity,
			"coupler fixture station did not create its block entity at " + stationPos);
		StationBlockEntity station = (StationBlockEntity) blockEntity;
		CompoundTag stationTag = station.saveWithFullMetadata();
		stationTag.put("TargetTrack", NbtUtils.writeBlockPos(targetTrackPos.subtract(stationPos)));
		stationTag.putBoolean("TargetDirection", targetDirection);
		station.load(stationTag);
		station.setChanged();
		return station;
	}

	private static Train assembleFixtureTrain(GameTestHelper helper, StationBlockEntity station, String name) {
		return assembleFixtureTrain(helper, station, name, 1);
	}

	private static Train assembleFixtureTrain(GameTestHelper helper, StationBlockEntity station, String name,
		int expectedCarriages) {
		require(helper, station.edgePoint.hasValidTrack(), "coupler fixture station lost its target track");
		require(helper, station.getStation() != null, "coupler fixture station was not registered in the track graph");
		require(helper, station.enterAssemblyMode(null), "coupler fixture station did not enter assembly mode");
		station.refreshAssemblyInfo();
		station.assemble(null);
		Train train = station.getStation().getPresentTrain();
		require(helper, train != null && train.carriages.size() == expectedCarriages,
			"coupler fixture station did not assemble " + expectedCarriages + " carriage(s) for " + name);
		require(helper, station.exitAssemblyMode(), "coupler fixture station did not leave assembly mode");
		train.name = RailwardComponents.literal(name);
		return train;
	}

	private static void prepareTrainForRestart(GameTestHelper helper) {
		BlockPos stationPos = new BlockPos(12, 1, 5);
		BlockPos targetTrackPos = new BlockPos(12, 1, 8);
		BlockPos returnStationPos = new BlockPos(4, 1, 11);
		BlockPos returnTrackPos = new BlockPos(4, 1, 8);
		BlockPos destinationStationPos = new BlockPos(44, 1, 5);
		BlockPos destinationTrackPos = new BlockPos(44, 1, 8);
		BlockPos bogeyPos = new BlockPos(11, 2, 8);
		BlockPos slimePos = new BlockPos(10, 2, 8);
		BlockPos cargoPos = new BlockPos(9, 2, 8);
		BlockPos cisternPos = new BlockPos(10, 2, 7);
		BlockPos controlsPos = new BlockPos(10, 3, 8);
		BlockPos conductorSeatPos = new BlockPos(11, 3, 8);
		BlockPos cabSignalPos = new BlockPos(10, 3, 7);
		BlockPos bufferLightsPos = new BlockPos(10, 2, 9);
		BlockPos doorLowerPos = new BlockPos(10, 3, 9);
		BlockPos doorUpperPos = doorLowerPos.above();

		for (int x = 1; x <= 48; x++)
			helper.setBlock(new BlockPos(x, 1, 8), com.simibubi.create.AllBlocks.TRACK.get()
				.defaultBlockState()
				.setValue(TrackBlock.SHAPE, TrackShape.XO));
		helper.setBlock(stationPos, com.simibubi.create.AllBlocks.TRACK_STATION.get());
		helper.setBlock(returnStationPos, com.simibubi.create.AllBlocks.TRACK_STATION.get());
		helper.setBlock(destinationStationPos, com.simibubi.create.AllBlocks.TRACK_STATION.get());
		helper.setBlock(bogeyPos, com.simibubi.create.AllBlocks.SMALL_BOGEY.get()
			.defaultBlockState()
			.setValue(AbstractBogeyBlock.AXIS, Direction.Axis.X));
		helper.setBlock(slimePos, Blocks.SLIME_BLOCK);
		helper.setBlock(cargoPos, AllBlocks.BULK_CARGO_CONTAINER.get());
		helper.setBlock(cisternPos, AllBlocks.CISTERN.get().defaultBlockState()
			.setValue(CisternBlock.AXIS, Direction.Axis.X));
		helper.setBlock(controlsPos, AllBlocks.MODERN_TRAIN_CONTROLLER.get()
			.defaultBlockState()
			.setValue(ControlsBlock.FACING, Direction.WEST)
			.setValue(ModernTrainControllerBlock.REVERSED, true));
		helper.setBlock(conductorSeatPos, com.simibubi.create.AllBlocks.SEATS.get(DyeColor.RED).get());
		helper.setBlock(cabSignalPos, AllBlocks.CAB_SIGNAL.get().defaultBlockState()
			.setValue(CabSignalBlock.FACING, Direction.UP));
		helper.getLevel().addFreshEntity(new SuperGlueEntity(helper.getLevel(),
			SuperGlueEntity.span(helper.absolutePos(controlsPos), helper.absolutePos(conductorSeatPos))));
		helper.getLevel().addFreshEntity(new SuperGlueEntity(helper.getLevel(),
			SuperGlueEntity.span(helper.absolutePos(cisternPos), helper.absolutePos(cabSignalPos))));
		helper.getLevel().addFreshEntity(new SuperGlueEntity(helper.getLevel(),
			SuperGlueEntity.span(helper.absolutePos(bogeyPos), helper.absolutePos(conductorSeatPos))));
		helper.setBlock(bufferLightsPos, AllBlocks.LOCOMOTIVE_BUFFER_LIGHTS.get()
			.defaultBlockState()
			.setValue(LocomotiveBufferLightsBlock.FACING, Direction.SOUTH)
			.setValue(LocomotiveBufferLightsBlock.VARIANT, LocomotiveBufferLightsVariant.TYPE_3));
		TrainDoorBlock doorBlock = (TrainDoorBlock) AllBlocks.hingedTrainDoorBlocks()[0].get();
		BlockState doorState = doorBlock.defaultBlockState()
			.setValue(TrainDoorBlock.FACING, Direction.SOUTH)
			.setValue(TrainDoorBlock.HINGE, DoorHingeSide.LEFT)
			.setValue(TrainDoorBlock.OPEN, false)
			.setValue(TrainDoorBlock.POWERED, false)
			.setValue(SlidingDoorBlock.VISIBLE, true);
		helper.setBlock(doorLowerPos, doorState.setValue(TrainDoorBlock.HALF, DoubleBlockHalf.LOWER));
		helper.setBlock(doorUpperPos, doorState.setValue(TrainDoorBlock.HALF, DoubleBlockHalf.UPPER));
		helper.getLevel().addFreshEntity(new SuperGlueEntity(helper.getLevel(),
			SuperGlueEntity.span(helper.absolutePos(controlsPos), helper.absolutePos(doorLowerPos))));
		helper.getLevel().addFreshEntity(new SuperGlueEntity(helper.getLevel(),
			SuperGlueEntity.span(helper.absolutePos(doorLowerPos), helper.absolutePos(doorUpperPos))));

		helper.runAfterDelay(5, () -> {
			BulkCargoContainerBlockEntity cargo = bulkCargoAt(helper, cargoPos);
			require(helper, cargo.tryInsert(new ItemStack(Items.IRON_INGOT, 80), false).isEmpty(),
				"cargo could not be prepared before station assembly");
			CisternBlockEntity cistern = cisternAt(helper, cisternPos);
			require(helper, cistern.getTankInventory().fill(new FluidStack(Fluids.WATER, 1000),
				IFluidHandler.FluidAction.EXECUTE) == 1000,
				"cistern could not be prepared before station assembly");

			BlockEntity blockEntity = helper.getBlockEntity(stationPos);
			require(helper, blockEntity instanceof StationBlockEntity,
				"track station did not create its block entity");
			StationBlockEntity station = (StationBlockEntity) blockEntity;
			CompoundTag stationTag = station.saveWithFullMetadata();
			stationTag.put("TargetTrack", NbtUtils.writeBlockPos(targetTrackPos.subtract(stationPos)));
			stationTag.putBoolean("TargetDirection", false);
			station.load(stationTag);
			station.setChanged();

			BlockEntity returnBlockEntity = helper.getBlockEntity(returnStationPos);
			require(helper, returnBlockEntity instanceof StationBlockEntity,
				"return track station did not create its block entity");
			StationBlockEntity returnStation = (StationBlockEntity) returnBlockEntity;
			CompoundTag returnStationTag = returnStation.saveWithFullMetadata();
			returnStationTag.put("TargetTrack",
				NbtUtils.writeBlockPos(returnTrackPos.subtract(returnStationPos)));
			returnStationTag.putBoolean("TargetDirection", true);
			returnStation.load(returnStationTag);
			returnStation.setChanged();

			BlockEntity destinationBlockEntity = helper.getBlockEntity(destinationStationPos);
			require(helper, destinationBlockEntity instanceof StationBlockEntity,
				"destination track station did not create its block entity");
			StationBlockEntity destinationStation = (StationBlockEntity) destinationBlockEntity;
			CompoundTag destinationStationTag = destinationStation.saveWithFullMetadata();
			destinationStationTag.put("TargetTrack",
				NbtUtils.writeBlockPos(destinationTrackPos.subtract(destinationStationPos)));
			destinationStationTag.putBoolean("TargetDirection", false);
			destinationStation.load(destinationStationTag);
			destinationStation.setChanged();

			helper.runAfterDelay(25, () -> {
				require(helper, SuperGlueEntity.isGlued(helper.getLevel(), helper.absolutePos(bogeyPos), Direction.UP, null),
					"conductor seat was not glued to its bogey before assembly");
				require(helper, station.edgePoint.hasValidTrack(), "station lost its target track");
				require(helper, station.getStation() != null,
					"station edge point was not registered in the track graph");
				require(helper, station.updateName(ROUTE_ORIGIN_NAME),
					"origin station could not be named for the signal round-trip probe");
				require(helper, returnStation.edgePoint.hasValidTrack(),
					"return station lost its target track");
				require(helper, returnStation.getStation() != null,
					"return station edge point was not registered in the track graph");
				require(helper, returnStation.updateName(SIGNAL_RETURN_NAME),
					"return station could not be named for the signal round-trip probe");
				require(helper, destinationStation.edgePoint.hasValidTrack(),
					"destination station lost its target track");
				require(helper, destinationStation.getStation() != null,
					"destination station edge point was not registered in the track graph");
				require(helper, destinationStation.updateName(ROUTE_DESTINATION_NAME),
					"destination station could not be named for the autonomous route probe");
				require(helper, station.enterAssemblyMode(null), "station did not enter assembly mode");
				station.refreshAssemblyInfo();
				station.assemble(null);

				Train train = station.getStation().getPresentTrain();
				require(helper, train != null && train.carriages.size() == 1,
					"station did not assemble the persistence train");
				require(helper, station.exitAssemblyMode(), "station did not leave assembly mode");

				helper.runAfterDelay(10, () -> {
					CarriageContraptionEntity entity = train.carriages.get(0)
						.getDimensional(helper.getLevel())
						.entity.get();
					require(helper, entity != null, "assembled carriage entity was not present");
					Contraption contraption = entity.getContraption();
					require(helper, contraption instanceof CarriageContraption,
						"assembled entity did not contain a carriage contraption");
					require(helper, contraption.getBlocks().values().stream()
						.anyMatch(info -> RailwardStructureInfo.state(info).getBlock() instanceof ModernTrainControllerBlock),
						"assembled train did not capture its modern controller");
				require(helper, contraption.getBlocks().values().stream()
					.anyMatch(info -> AllBlocks.CAB_SIGNAL.has(RailwardStructureInfo.state(info))),
					"assembled train did not capture its cab signal");
				require(helper, ((CarriageContraption) contraption).getSeats().size() == 1,
					"assembled train did not capture its conductor seat");
				List<StructureBlockInfo> doorInfos = contraption.getBlocks().values().stream()
					.filter(info -> RailwardStructureInfo.state(info).getBlock() instanceof TrainDoorBlock)
					.toList();
				require(helper, doorInfos.size() == 2 && doorInfos.stream()
					.allMatch(info -> RailwardStructureInfo.state(info).getValue(TrainDoorBlock.OPEN)),
					"assembled train did not capture both station-open train-door halves: " + doorInfos.stream()
						.map(info -> RailwardStructureInfo.pos(info) + " " + RailwardStructureInfo.state(info))
						.toList());

					BlockPos mountedCargoPos = new BlockPos(-2, 0, 0);
					BulkCargoContraptionHelper.ControllerData mountedCargoData =
						BulkCargoContraptionHelper.resolveController(contraption, mountedCargoPos);
					require(helper, mountedCargoData != null
						&& mountedCargoData.controllerPos().equals(mountedCargoPos)
						&& mountedCargoData.sizeX() == 1 && mountedCargoData.sizeZ() == 1
						&& mountedCargoData.height() == 1,
						"assembled train selected the wrong mounted cargo controller");
					StructureBlockInfo mountedCargoInfo = contraption.getBlocks().get(mountedCargoPos);
					require(helper, mountedCargoInfo != null
						&& RailwardStructureInfo.state(mountedCargoInfo)
							.getValue(BulkCargoContainerBlock.CARGO_LEVEL) == 1,
						"assembled train exposed the wrong initial mounted cargo fill level");
					IItemHandlerModifiable movingCargo = BulkCargoContraptionHelper.getItemHandler(contraption,
						mountedCargoPos);
					require(helper, movingCargo != null && countItems(movingCargo) == 80,
						"assembled train did not preserve cargo");
					require(helper, BulkCargoStorage.insert(movingCargo,
						new ItemStack(Items.IRON_INGOT, 16), false).isEmpty(),
						"assembled train cargo did not accept the persistence update");
					BulkCargoContraptionHelper.onCargoContentsChanged(entity, mountedCargoData, movingCargo);
					require(helper, countItems(movingCargo) == 96
						&& RailwardStructureInfo.state(contraption.getBlocks().get(mountedCargoPos))
							.getValue(BulkCargoContainerBlock.CARGO_LEVEL) == 1,
						"prepared mounted cargo amount or fill level was inconsistent");

					IFluidHandler movingFluid = CisternContraptionHelper.getFluidHandler(contraption,
						new BlockPos(-1, 0, -1));
					require(helper, movingFluid != null && movingFluid.getFluidInTank(0).getFluid() == Fluids.WATER
						&& movingFluid.getFluidInTank(0).getAmount() == 1000,
						"assembled train did not preserve cistern fluid");
					require(helper, movingFluid.fill(new FluidStack(Fluids.WATER, 500),
						IFluidHandler.FluidAction.EXECUTE) == 500,
						"assembled train cistern did not accept the persistence update");

					train.name = RailwardComponents.literal(PERSISTENCE_TRAIN_NAME);
					train.currentlyBackwards = true;
					train.throttle = .625;
					Schedule schedule = new Schedule();
					ScheduleEntry scheduleEntry = new ScheduleEntry();
					ChangeTitleInstruction instruction = new ChangeTitleInstruction();
					CompoundTag instructionData = new CompoundTag();
					instructionData.putString("Text", "Railward NET-01 direction probe");
					instruction.setData(instructionData);
					scheduleEntry.instruction = instruction;
					schedule.entries.add(scheduleEntry);
					train.runtime.setSchedule(schedule, true);
					TrainDirectionState.syncScheduledControls(train, helper.getLevel());
					Create.RAILWAYS.markTracksDirty();

					require(helper, train.currentStation != null,
						"assembled train was not associated with its station");
					require(helper, countItems(train.carriages.get(0).storage.getItems()) == 96,
						"prepared Train storage did not contain 96 items");
					require(helper, train.carriages.get(0).storage.getFluids().getFluidInTank(0).getAmount() == 1500,
						"prepared Train storage did not contain 1500 mB");
					succeedAndStop(helper);
				});
			});
		});
	}

	private static void verifyTrainAfterRestart(GameTestHelper helper) {
		// Force the chunk containing the prepare-phase train to load before checking
		// its restored contraption entity. The test world is disposable and the
		// prepare command is run around x=0/z=0.
		helper.getLevel().getChunk(0, 0);
		helper.runAfterDelay(40, () -> {
			List<Train> matches = Create.RAILWAYS.sided(helper.getLevel()).trains.values().stream()
				.filter(train -> PERSISTENCE_TRAIN_NAME.equals(train.name.getString()))
				.toList();
			require(helper, matches.size() == 1,
				"expected exactly one persisted SAVE-01 train, found " + matches.size());

			Train train = matches.get(0);
			require(helper, !train.invalid && train.graph != null,
				"persisted train did not restore its track graph");
			require(helper, train.carriages.size() == 1,
				"persisted train restored the wrong carriage count");
			require(helper, train.currentStation != null && train.getCurrentStation() != null,
				"persisted train did not restore its station association");
			require(helper, train.currentlyBackwards,
				"persisted train did not restore its controller direction");
			require(helper, Math.abs(train.throttle - .625) < .0001,
				"persisted train did not restore its throttle value");
			require(helper, train.runtime.getSchedule() != null && !train.runtime.paused,
				"persisted train did not restore its active schedule");
			require(helper, TrainDirectionState.resolveServer(train) == DirectionState.BACKWARD,
				"persisted train did not restore its server-authoritative backward direction");
			require(helper, countItems(train.carriages.get(0).storage.getItems()) == 96,
				"persisted train did not restore 96 cargo items");
			IFluidHandler fluids = train.carriages.get(0).storage.getFluids();
			require(helper, fluids.getFluidInTank(0).getFluid() == Fluids.WATER
				&& fluids.getFluidInTank(0).getAmount() == 1500,
				"persisted train did not restore 1500 mB of water");

			CarriageContraptionEntity entity = train.carriages.get(0)
				.getDimensional(helper.getLevel())
				.entity.get();
			require(helper, entity != null && entity.getContraption() instanceof CarriageContraption,
				"persisted carriage contraption entity was not restored");
			BlockPos mountedCargoPos = new BlockPos(-2, 0, 0);
			BulkCargoContraptionHelper.ControllerData mountedCargoData =
				BulkCargoContraptionHelper.resolveController(entity.getContraption(), mountedCargoPos);
			require(helper, mountedCargoData != null
				&& mountedCargoData.controllerPos().equals(mountedCargoPos)
				&& mountedCargoData.sizeX() == 1 && mountedCargoData.sizeZ() == 1
				&& mountedCargoData.height() == 1,
				"persisted entity restored the wrong mounted cargo controller");
			require(helper, countItems(BulkCargoContraptionHelper.getItemHandler(entity.getContraption(),
				mountedCargoPos)) == 96,
				"restored entity cargo did not match Train storage");
			StructureBlockInfo restoredCargoInfo = entity.getContraption().getBlocks().get(mountedCargoPos);
			require(helper, restoredCargoInfo != null
				&& RailwardStructureInfo.state(restoredCargoInfo)
					.getValue(BulkCargoContainerBlock.CARGO_LEVEL) == 1,
				"persisted entity restored the wrong mounted cargo fill level");
			require(helper, CisternContraptionHelper.getFluidHandler(entity.getContraption(),
				new BlockPos(-1, 0, -1)).getFluidInTank(0).getAmount() == 1500,
				"restored entity fluid did not match Train storage");
			require(helper, entity.getContraption().getBlocks().values().stream()
				.filter(info -> RailwardStructureInfo.state(info).getBlock() instanceof TrainDoorBlock)
				.filter(info -> RailwardStructureInfo.state(info).getValue(TrainDoorBlock.OPEN))
				.count() == 2,
				"persisted train did not restore both station-open train-door halves");
			require(helper, entity.getContraption().getBlocks().values().stream()
				.anyMatch(info -> AllBlocks.CAB_SIGNAL.has(RailwardStructureInfo.state(info))),
				"persisted train did not restore its cab signal");
			succeedAndStop(helper);
		});
	}

	private static void succeedAndStop(GameTestHelper helper) {
		MinecraftServer server = helper.getLevel().getServer();
		require(helper, server != null, "dedicated server was unavailable");
		LOGGER.info("Railward SAVE-01 {} phase passed; flushing and stopping the dedicated server",
			System.getProperty(PHASE_PROPERTY, "unknown"));
		helper.succeed();
		server.saveEverything(false, true, true);
		server.halt(false);
	}

	private static int countItems(IItemHandler inventory) {
		int amount = 0;
		for (int slot = 0; slot < inventory.getSlots(); slot++)
			amount += inventory.getStackInSlot(slot).getCount();
		return amount;
	}

	private static BulkCargoContainerBlockEntity bulkCargoAt(GameTestHelper helper, BlockPos pos) {
		BlockEntity blockEntity = helper.getBlockEntity(pos);
		require(helper, blockEntity instanceof BulkCargoContainerBlockEntity,
			"expected a bulk cargo block entity at " + pos);
		return (BulkCargoContainerBlockEntity) blockEntity;
	}

	private static CisternBlockEntity cisternAt(GameTestHelper helper, BlockPos pos) {
		BlockEntity blockEntity = helper.getBlockEntity(pos);
		require(helper, blockEntity instanceof CisternBlockEntity,
			"expected a cistern block entity at " + pos);
		return (CisternBlockEntity) blockEntity;
	}

	private static void require(GameTestHelper helper, boolean condition, String message) {
		if (!condition)
			helper.fail(message);
	}

	private record AutoCouplerTrainSpec(String name, int stationX, int stationZ, int bogeyX, int couplerX,
		int controlsX, Direction facing, boolean targetDirection) {
	}

	private record StorageSignature(int items, int fluid) {
	}

	private static final class FixedSignalBoundary extends SignalBoundary {
		@Override
		public void tick(TrackGraph graph, boolean preTrains) {
			// SIGNAL-01 drives the four Create aspects deterministically while the
			// real train traverses this otherwise normal graph edge point.
		}
	}
}
