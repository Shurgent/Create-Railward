package ua.pp.shurgent.railward.gametest;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.simibubi.create.content.trains.GlobalRailwayManager;
import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.CarriageBogey;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.entity.TravellingPoint;
import com.simibubi.create.content.trains.graph.DimensionPalette;
import com.simibubi.create.content.trains.graph.EdgePointType;
import com.simibubi.create.content.trains.graph.TrackEdge;
import com.simibubi.create.content.trains.graph.TrackGraph;
import com.simibubi.create.content.trains.graph.TrackNode;
import com.simibubi.create.content.trains.graph.TrackNodeLocation;
import com.simibubi.create.content.trains.signal.SignalBoundary;
import com.simibubi.create.content.trains.track.TrackMaterial;
import com.simibubi.create.content.trains.track.TrackTargetingBehaviour;
import com.simibubi.create.foundation.utility.Couple;
import com.simibubi.create.foundation.utility.Pair;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import ua.pp.shurgent.railward.AllBlocks;
import ua.pp.shurgent.railward.AllEdgePointTypes;
import ua.pp.shurgent.railward.Railward;
import ua.pp.shurgent.railward.block.train.signal.RelayCaseBlock;
import ua.pp.shurgent.railward.block.train.signal.RelayCaseBlockEntity;
import ua.pp.shurgent.railward.block.train.signal.RelayCaseEdgePoint;
import ua.pp.shurgent.railward.block.train.signal.RelayCaseRailwayManager;

@GameTestHolder(Railward.MOD_ID)
@PrefixGameTestTemplate(false)
public final class RailwardRelayCaseGameTests {

	@GameTest(template = "empty", timeoutTicks = 20)
	public static void independentBlocksAndUnrestrictedNavigation(GameTestHelper helper) {
		Fixture fixture = new Fixture(helper);
		RelayCaseEdgePoint first = fixture.boundary(40);
		RelayCaseEdgePoint second = fixture.boundary(80);
		Train train = fixture.train(60, 58);
		fixture.tick();
		require(helper, first.isTrackOccupied(true) && !second.isTrackOccupied(true), "wrong forward block");
		require(helper, !first.isTrackOccupied(false), "forward occupancy leaked behind cabinet");
		require(helper, first.canNavigateVia(fixture.a) && first.canNavigateVia(fixture.b), "cabinet restricted routing");
		int occupied = train.occupiedSignalBlocks.size();
		boolean stopped = train.frontSignalListener().test(0d, Pair.of(first, Couple.create(fixture.a, fixture.b)));
		require(helper, !stopped && train.occupiedSignalBlocks.size() == occupied && train.reservedSignalBlocks.isEmpty(),
			"cabinet modified train signal/navigation state");

		SignalBoundary signal = new SignalBoundary();
		signal.setId(UUID.randomUUID());
		signal.setType(EdgePointType.SIGNAL);
		signal.setLocation(Couple.create(fixture.a.getLocation(), fixture.b.getLocation()), 65);
		fixture.graph.addPoint(EdgePointType.SIGNAL, signal);
		fixture.move(train, 76, 74);
		fixture.tick();
		require(helper, first.isTrackOccupied(true) && !second.isTrackOccupied(true), "Create signal split a relay block");
		fixture.move(train, 100, 98);
		fixture.tick();
		require(helper, !first.isTrackOccupied(true) && second.isTrackOccupied(true), "tail did not free previous block");
		fixture.move(train, 30, 28);
		fixture.tick();
		require(helper, !first.isTrackOccupied(true) && first.isTrackOccupied(false) && !second.isTrackOccupied(true),
			"reverse movement left stale occupancy");
		helper.succeed();
	}

	@GameTest(template = "empty", timeoutTicks = 20)
	public static void wholeTrainMultipleTrainsAndConsistChanges(GameTestHelper helper) {
		Fixture fixture = new Fixture(helper);
		RelayCaseEdgePoint first = fixture.boundary(40);
		RelayCaseEdgePoint second = fixture.boundary(60);
		CarriageBogey front = fixture.bogey(91, 89);
		CarriageBogey rear = fixture.bogey(31, 29);
		Train longTrain = fixture.addTrain(new Carriage(front, rear, 60));
		fixture.tick();
		require(helper, first.isTrackOccupied(true) && second.isTrackOccupied(true), "body between bogeys was ignored");
		Train other = fixture.train(51, 49);
		fixture.manager.trains.remove(longTrain.id);
		fixture.tick();
		require(helper, first.isTrackOccupied(true) && !second.isTrackOccupied(true), "removing one train cleared another");
		other.carriages.set(0, new Carriage(fixture.bogey(101, 99), null, 0));
		other.carriages.get(0).setTrain(other);
		fixture.tick();
		require(helper, !first.isTrackOccupied(true) && second.isTrackOccupied(true), "consist replacement kept stale footprint");
		fixture.manager.trains.clear();
		fixture.tick();
		require(helper, !first.isTrackOccupied(true) && !second.isTrackOccupied(true), "removed train left occupied blocks");
		helper.succeed();
	}

	@GameTest(template = "empty", timeoutTicks = 20)
	public static void topologyChangesAndNbtRestore(GameTestHelper helper) {
		Fixture fixture = new Fixture(helper);
		RelayCaseEdgePoint first = fixture.boundary(40);
		Train train = fixture.train(75, 73);
		fixture.tick();
		require(helper, first.isTrackOccupied(true), "initial block not occupied");
		RelayCaseEdgePoint added = fixture.boundary(60);
		fixture.tick();
		require(helper, !first.isTrackOccupied(true) && added.isTrackOccupied(true), "new cabinet did not split block");
		fixture.graph.removePoint(AllEdgePointTypes.RELAY_CASE, added.id);
		fixture.tick();
		require(helper, first.isTrackOccupied(true), "removed cabinet did not merge blocks");

		DimensionPalette dimensions = new DimensionPalette();
		CompoundTag graphTag = fixture.graph.write(dimensions);
		CompoundTag trainTag = train.write(dimensions);
		TrackGraph restoredGraph = TrackGraph.read(graphTag, dimensions);
		Train restoredTrain = Train.read(trainTag, Map.of(restoredGraph.id, restoredGraph), dimensions);
		GlobalRailwayManager restored = new GlobalRailwayManager();
		restored.trackNetworks.put(restoredGraph.id, restoredGraph);
		restored.trains.put(restoredTrain.id, restoredTrain);
		RelayCaseRailwayManager.tick(restored, helper.getLevel());
		RelayCaseEdgePoint restoredPoint = restoredGraph.getPoint(AllEdgePointTypes.RELAY_CASE, first.id);
		require(helper, restoredPoint != null && restoredPoint.isTrackOccupied(true), "NBT restore lost occupied block");
		restoredTrain.graph = null;
		RelayCaseRailwayManager.tick(restored, helper.getLevel());
		CompoundTag detachedTag = restoredTrain.write(dimensions);
		Train detached = Train.read(detachedTag, Map.of(restoredGraph.id, restoredGraph), dimensions);
		GlobalRailwayManager restarted = new GlobalRailwayManager();
		restarted.trackNetworks.put(restoredGraph.id, restoredGraph);
		restarted.trains.put(detached.id, detached);
		RelayCaseRailwayManager.tick(restarted, helper.getLevel());
		require(helper, restoredPoint.isTrackOccupied(true), "detached train lost occupancy after restart");
		restarted.trains.clear();
		RelayCaseRailwayManager.tick(restarted, helper.getLevel());
		require(helper, !restoredPoint.isTrackOccupied(true), "removed detached train kept block occupied");
		helper.succeed();
	}

	@GameTest(template = "empty", timeoutTicks = 20)
	public static void shortBlockHighSpeedPassageIsNotMissed(GameTestHelper helper) {
		Fixture fixture = new Fixture(helper);
		RelayCaseEdgePoint first = fixture.boundary(40);
		RelayCaseEdgePoint second = fixture.boundary(41);
		Train train = fixture.train(35, 33);
		fixture.tick();
		fixture.move(train, 50, 48);
		first.onTrainEntered();
		second.onTrainEntered();
		fixture.tick();
		require(helper, first.isTrackOccupied(true), "complete passage through short block was missed");
		fixture.tick();
		require(helper, !first.isTrackOccupied(true), "short-block pulse did not clear");
		helper.succeed();
	}

	@GameTest(template = "empty", timeoutTicks = 20)
	public static void branchesRebuildAfterDisconnect(GameTestHelper helper) {
		Fixture fixture = new Fixture(helper);
		RelayCaseEdgePoint first = fixture.boundary(40);
		TrackNodeLocation location = new TrackNodeLocation(200, 100, 80).in(helper.getLevel());
		fixture.graph.loadNode(location, TrackGraph.nextNodeId(), new Vec3(0, 1, 0));
		TrackNode branch = fixture.graph.locateNode(location);
		TrackEdge edge = new TrackEdge(fixture.b, branch, null, TrackMaterial.ANDESITE);
		fixture.graph.putConnection(fixture.b, branch, edge);
		fixture.graph.putConnection(branch, fixture.b, new TrackEdge(branch, fixture.b, null, TrackMaterial.ANDESITE));
		CarriageBogey bogey = new CarriageBogey(com.simibubi.create.AllBlocks.SMALL_BOGEY.get(), false, null,
			new TravellingPoint(fixture.b, branch, edge, 30, false),
			new TravellingPoint(fixture.b, branch, edge, 28, false));
		fixture.addTrain(new Carriage(bogey, null, 0));
		fixture.tick();
		require(helper, first.isTrackOccupied(true), "branch was not part of block");
		fixture.graph.disconnectNodes(fixture.b, branch);
		fixture.tick();
		require(helper, !first.isTrackOccupied(true), "disconnected branch kept block occupied");
		helper.succeed();
	}

	@GameTest(template = "empty", timeoutTicks = 20)
	public static void crossingTrackWithoutCabinetAndDimensionIsolation(GameTestHelper helper) {
		Fixture fixture = new Fixture(helper);
		RelayCaseEdgePoint first = fixture.boundary(40);
		TrackGraph crossing = new TrackGraph();
		TrackNodeLocation c = new TrackNodeLocation(60, 100, -100).in(helper.getLevel());
		TrackNodeLocation d = new TrackNodeLocation(60, 100, 100).in(helper.getLevel());
		crossing.loadNode(c, TrackGraph.nextNodeId(), new Vec3(0, 1, 0));
		crossing.loadNode(d, TrackGraph.nextNodeId(), new Vec3(0, 1, 0));
		TrackNode cn = crossing.locateNode(c), dn = crossing.locateNode(d);
		TrackEdge edge = new TrackEdge(cn, dn, null, TrackMaterial.ANDESITE);
		crossing.putConnection(cn, dn, edge);
		crossing.putConnection(dn, cn, new TrackEdge(dn, cn, null, TrackMaterial.ANDESITE));
		fixture.edge.getEdgeData().addIntersection(fixture.graph, UUID.randomUUID(), 60, cn, dn, 100);
		fixture.manager.trackNetworks.put(crossing.id, crossing);
		CarriageBogey bogey = new CarriageBogey(com.simibubi.create.AllBlocks.SMALL_BOGEY.get(), false, null,
			new TravellingPoint(cn, dn, edge, 101, false), new TravellingPoint(cn, dn, edge, 99, false));
		Train train = new Train(UUID.randomUUID(), UUID.randomUUID(), crossing,
			new ArrayList<>(List.of(new Carriage(bogey, null, 0))), new ArrayList<>(), false);
		fixture.manager.trains.put(train.id, train);
		fixture.tick();
		require(helper, first.isTrackOccupied(true), "crossing train without its own cabinet was ignored");
		fixture.manager.trains.remove(train.id);
		fixture.tick();
		require(helper, !first.isTrackOccupied(true), "crossing train left stale occupancy");

		Fixture nether = new Fixture(helper, net.minecraft.world.level.Level.NETHER);
		nether.boundary(40);
		Train netherTrain = nether.train(60, 58);
		fixture.manager.trackNetworks.put(nether.graph.id, nether.graph);
		fixture.manager.trains.put(netherTrain.id, netherTrain);
		fixture.tick();
		require(helper, !first.isTrackOccupied(true), "train at same coordinates in Nether occupied Overworld block");
		Fixture otherManager = new Fixture(helper);
		otherManager.boundary(40);
		otherManager.train(60, 58);
		otherManager.tick();
		require(helper, !first.isTrackOccupied(true), "another railway manager changed this cabinet");
		helper.succeed();
	}

	@GameTest(template = "empty", timeoutTicks = 20)
	public static void externalPowerAndAdjacentCabinetsDoNotLatch(GameTestHelper helper) throws Exception {
		Fixture fixture = new Fixture(helper);
		RelayCaseEdgePoint point = fixture.boundary(40);
		Train train = fixture.train(60, 58);
		BlockPos pos = helper.absolutePos(new BlockPos(1, 2, 1));
		BlockPos neighbor = pos.east();
		helper.getLevel().setBlockAndUpdate(pos, AllBlocks.RELAY_CASE.getDefaultState());
		helper.getLevel().setBlockAndUpdate(neighbor, AllBlocks.RELAY_CASE.getDefaultState());
		RelayCaseBlockEntity first = (RelayCaseBlockEntity) helper.getLevel().getBlockEntity(pos);
		RelayCaseBlockEntity second = (RelayCaseBlockEntity) helper.getLevel().getBlockEntity(neighbor);
		Field edgePoint = TrackTargetingBehaviour.class.getDeclaredField("edgePoint");
		edgePoint.setAccessible(true);
		edgePoint.set(first.edgePoint, point);
		point.blockEntityAdded(first, true);
		fixture.tick();
		first.refreshState();
		second.refreshState();
		require(helper, first.isOccupied() && second.isOccupied(), "neighbor did not close from train-only output");
		require(helper, helper.getLevel().getBlockState(pos).getValue(RelayCaseBlock.TRAIN_PRESENT)
			&& !helper.getLevel().getBlockState(neighbor).getValue(RelayCaseBlock.TRAIN_PRESENT), "external input was retransmitted");
		fixture.move(train, 30, 28);
		fixture.tick();
		first.refreshState();
		second.refreshState();
		require(helper, !first.isOccupied() && !second.isOccupied(), "adjacent cabinets latched after train left");
		helper.getLevel().setBlockAndUpdate(pos.west(), Blocks.REDSTONE_BLOCK.defaultBlockState());
		first.refreshState();
		require(helper, first.isOccupied() && !helper.getLevel().getBlockState(pos).getValue(RelayCaseBlock.TRAIN_PRESENT),
			"external power was not state-only");
		helper.getLevel().removeBlock(pos.west(), false);
		first.refreshState();
		require(helper, !first.isOccupied(), "external input did not release");
		for (Direction facing : Direction.Plane.HORIZONTAL) {
			var state = AllBlocks.RELAY_CASE.getDefaultState().setValue(RelayCaseBlock.FACING, facing);
			for (int i = 0; i < 4; i++)
				state = state.rotate(net.minecraft.world.level.block.Rotation.CLOCKWISE_90);
			require(helper, state.getValue(RelayCaseBlock.FACING) == facing, "four rotations changed facing");
		}
		helper.succeed();
	}

	private static final class Fixture {
		final GameTestHelper helper;
		final GlobalRailwayManager manager = new GlobalRailwayManager();
		final TrackGraph graph = new TrackGraph();
		final TrackNode a;
		final TrackNode b;
		final TrackEdge edge;

		Fixture(GameTestHelper helper) {
			this(helper, helper.getLevel().dimension());
		}

		Fixture(GameTestHelper helper, net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension) {
			this.helper = helper;
			graph.loadNode(new TrackNodeLocation(0, 100, 0).in(dimension), TrackGraph.nextNodeId(), new Vec3(0, 1, 0));
			graph.loadNode(new TrackNodeLocation(200, 100, 0).in(dimension), TrackGraph.nextNodeId(), new Vec3(0, 1, 0));
			a = graph.locateNode(new TrackNodeLocation(0, 100, 0).in(dimension));
			b = graph.locateNode(new TrackNodeLocation(200, 100, 0).in(dimension));
			edge = new TrackEdge(a, b, null, TrackMaterial.ANDESITE);
			graph.putConnection(a, b, edge);
			graph.putConnection(b, a, new TrackEdge(b, a, null, TrackMaterial.ANDESITE));
			manager.trackNetworks.put(graph.id, graph);
		}

		RelayCaseEdgePoint boundary(double position) {
			RelayCaseEdgePoint point = AllEdgePointTypes.RELAY_CASE.create();
			point.setId(UUID.randomUUID());
			point.setLocation(Couple.create(a.getLocation(), b.getLocation()), position);
			graph.addPoint(AllEdgePointTypes.RELAY_CASE, point);
			return point;
		}

		CarriageBogey bogey(double front, double back) {
			return new CarriageBogey(com.simibubi.create.AllBlocks.SMALL_BOGEY.get(), false, null,
				new TravellingPoint(a, b, edge, front, false), new TravellingPoint(a, b, edge, back, false));
		}

		Train train(double front, double back) {
			return addTrain(new Carriage(bogey(front, back), null, 0));
		}

		Train addTrain(Carriage carriage) {
			Train train = new Train(UUID.randomUUID(), UUID.randomUUID(), graph,
				new ArrayList<>(List.of(carriage)), new ArrayList<>(), false);
			manager.trains.put(train.id, train);
			return train;
		}

		void move(Train train, double front, double back) {
			train.carriages.get(0).getLeadingPoint().position = front;
			train.carriages.get(0).getTrailingPoint().position = back;
		}

		void tick() {
			RelayCaseRailwayManager.tick(manager, helper.getLevel());
		}
	}

	private static void require(GameTestHelper helper, boolean condition, String message) {
		if (!condition)
			helper.fail(message);
	}
}
