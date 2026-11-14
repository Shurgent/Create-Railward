package ua.pp.shurgent.railward.gametest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.simibubi.create.content.trains.GlobalRailwayManager;
import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.CarriageBogey;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.entity.TravellingPoint;
import com.simibubi.create.content.trains.entity.TravellingPoint.IEdgePointListener;
import com.simibubi.create.content.trains.graph.EdgePointType;
import com.simibubi.create.content.trains.graph.TrackEdge;
import com.simibubi.create.content.trains.graph.TrackGraph;
import com.simibubi.create.content.trains.graph.TrackNode;
import com.simibubi.create.content.trains.graph.TrackNodeLocation;
import com.simibubi.create.content.trains.signal.SignalBoundary;
import com.simibubi.create.content.trains.signal.TrackEdgePoint;
import com.simibubi.create.content.trains.track.TrackMaterial;
import com.simibubi.create.foundation.utility.Couple;
import com.simibubi.create.foundation.utility.Pair;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import ua.pp.shurgent.railward.AllEdgePointTypes;
import ua.pp.shurgent.railward.Railward;
import ua.pp.shurgent.railward.block.train.signal.RelayCaseEdgePoint;
import ua.pp.shurgent.railward.block.train.signal.RelayCaseRailwayManager;
import ua.pp.shurgent.railward.block.train.signal.SpeedSignEdgePoint;

@GameTestHolder(Railward.MOD_ID)
@PrefixGameTestTemplate(false)
public final class RailwardTrainPassageGameTests {
	private static final Map<Train, Passages> FOREIGN_LISTENERS = new IdentityHashMap<>();

	/** Called only by the test mixin, which owns the formerly conflicting invocation. */
	public static IEdgePointListener wrapForeignListener(Train train, IEdgePointListener original) {
		Passages passages = FOREIGN_LISTENERS.get(train);
		if (passages == null)
			return original;
		return (distance, point) -> {
			passages.calls.merge(point.getFirst().id, 1, Integer::sum);
			boolean stopped = original.test(distance, point);
			passages.results.add(stopped);
			return stopped;
		};
	}

	@GameTest(template = "empty", timeoutTicks = 20)
	public static void foreignRedirectPreservesSpeedSignsAndRelayPassages(GameTestHelper helper) {
		Fixture fixture = new Fixture(helper);
		RelayCaseEdgePoint first = fixture.add(AllEdgePointTypes.RELAY_CASE.create(), 40);
		RelayCaseEdgePoint second = fixture.add(AllEdgePointTypes.RELAY_CASE.create(), 41);
		SpeedSignEdgePoint speed = fixture.add(AllEdgePointTypes.SPEED_SIGN.create(), 43);
		speed.updateSpeedLimit(BlockPos.ZERO, true, 10);
		Passages passages = new Passages();
		FOREIGN_LISTENERS.put(fixture.train, passages);
		try {
			fixture.tick();
			fixture.train.throttle = 1;
			fixture.train.targetSpeed = 1;
			fixture.carriage.travel(helper.getLevel(), fixture.graph, 10, null, null, 3);
			fixture.tick();
			require(helper, first.isTrackOccupied(true), "Railward listener was lost behind the foreign redirect");
			require(helper, fixture.train.throttle < 1 && fixture.train.targetSpeed <= fixture.train.maxSpeed() * fixture.train.throttle,
				"speed sign did not limit the real moving carriage");
			require(helper, passages.calls.size() == 3 && passages.calls.values().stream().allMatch(count -> count == 1),
				"forward passage callbacks were duplicated or lost");
			fixture.tick();
			require(helper, !first.isTrackOccupied(true), "short-block pulse remained latched");

			fixture.train.throttle = 1;
			fixture.train.targetSpeed = -1;
			fixture.carriage.travel(helper.getLevel(), fixture.graph, -12, null, null, 3);
			fixture.tick();
			require(helper, first.isTrackOccupied(true), "reverse short-block crossing was missed");
			require(helper, fixture.train.throttle == 1, "one-way speed sign fired in reverse");
			require(helper, passages.calls.size() == 3 && passages.calls.values().stream().allMatch(count -> count == 2)
				&& passages.results.stream().noneMatch(Boolean::booleanValue), "reverse callbacks/results changed");
			fixture.tick();
			require(helper, !first.isTrackOccupied(true) && !second.isTrackOccupied(true), "reverse passage did not release");
		} finally {
			FOREIGN_LISTENERS.remove(fixture.train);
		}
		helper.succeed();
	}

	@GameTest(template = "empty", timeoutTicks = 20)
	public static void foreignRedirectPreservesCreatesStopDecision(GameTestHelper helper) {
		Fixture fixture = new Fixture(helper);
		SignalBoundary signal = fixture.add(EdgePointType.SIGNAL.create(), 43);
		Passages passages = new Passages();
		FOREIGN_LISTENERS.put(fixture.train, passages);
		try {
			fixture.train.speed = 1;
			fixture.train.navigation.waitingForSignal = Pair.of(signal.id, true);
			fixture.train.navigation.distanceToSignal = 4;
			fixture.carriage.travel(helper.getLevel(), fixture.graph, 10, null, null, 3);
			require(helper, fixture.train.speed == 0 && fixture.train.navigation.distanceToSignal == 0,
				"Create's original signal-stop handler was bypassed");
			require(helper, passages.calls.getOrDefault(signal.id, 0) == 1 && passages.results.equals(List.of(true)),
				"original stop decision was dropped or invoked twice");
			require(helper, fixture.carriage.getLeadingPoint().position <= 43.000001, "carriage passed its stopping signal");
		} finally {
			FOREIGN_LISTENERS.remove(fixture.train);
		}
		helper.succeed();
	}

	private static final class Passages {
		final Map<UUID, Integer> calls = new HashMap<>();
		final List<Boolean> results = new ArrayList<>();
	}

	private static final class Fixture {
		final GameTestHelper helper;
		final GlobalRailwayManager manager = new GlobalRailwayManager();
		final TrackGraph graph = new TrackGraph();
		final TrackNode a;
		final TrackNode b;
		final TestCarriage carriage;
		final Train train;

		Fixture(GameTestHelper helper) {
			this.helper = helper;
			TrackNodeLocation first = new TrackNodeLocation(0, 100, 0).in(helper.getLevel());
			TrackNodeLocation last = new TrackNodeLocation(200, 100, 0).in(helper.getLevel());
			graph.loadNode(first, TrackGraph.nextNodeId(), new Vec3(0, 1, 0));
			graph.loadNode(last, TrackGraph.nextNodeId(), new Vec3(0, 1, 0));
			a = graph.locateNode(first);
			b = graph.locateNode(last);
			TrackEdge edge = new TrackEdge(a, b, null, TrackMaterial.ANDESITE);
			graph.putConnection(a, b, edge);
			graph.putConnection(b, a, new TrackEdge(b, a, null, TrackMaterial.ANDESITE));
			var type = com.simibubi.create.AllBlocks.SMALL_BOGEY.get();
			CarriageBogey bogey = new CarriageBogey(type, false, null,
				new TravellingPoint(a, b, edge, 39, false),
				new TravellingPoint(a, b, edge, 39 - type.getWheelPointSpacing(), false));
			carriage = new TestCarriage(bogey);
			train = new Train(UUID.randomUUID(), UUID.randomUUID(), graph,
				new ArrayList<>(List.of(carriage)), new ArrayList<>(), false);
			manager.trackNetworks.put(graph.id, graph);
			manager.trains.put(train.id, train);
		}

		<T extends TrackEdgePoint> T add(T point, double position) {
			point.setId(UUID.randomUUID());
			point.setLocation(Couple.create(a.getLocation(), b.getLocation()), position);
			addTyped(point.getType(), point);
			return point;
		}

		@SuppressWarnings({ "rawtypes", "unchecked" })
		private void addTyped(EdgePointType type, TrackEdgePoint point) {
			graph.addPoint(type, point);
		}

		void tick() {
			RelayCaseRailwayManager.tick(manager, helper.getLevel());
		}
	}

	/** Keep native bogey travel/listeners; entity anchoring and lifecycle are outside this regression. */
	private static final class TestCarriage extends Carriage {
		TestCarriage(CarriageBogey bogey) {
			super(bogey, null, 0);
		}

		@Override
		public void updateContraptionAnchors() {}

		@Override
		public void manageEntities(Level level) {}
	}

	private static void require(GameTestHelper helper, boolean condition, String message) {
		if (!condition)
			helper.fail(message);
	}
}
