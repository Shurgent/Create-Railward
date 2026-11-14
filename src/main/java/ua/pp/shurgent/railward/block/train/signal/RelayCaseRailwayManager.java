package ua.pp.shurgent.railward.block.train.signal;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

import com.simibubi.create.content.trains.GlobalRailwayManager;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.entity.TravellingPoint;
import com.simibubi.create.content.trains.graph.TrackEdge;
import com.simibubi.create.content.trains.graph.TrackGraph;
import com.simibubi.create.content.trains.graph.TrackNode;
import com.simibubi.create.content.trains.graph.TrackNodeLocation;

import net.minecraft.world.level.Level;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import ua.pp.shurgent.railward.AllEdgePointTypes;
import ua.pp.shurgent.railward.compat.RelayGraphVersion;

/**
 * A separate block-section layer over Create's track graph. This never reads or
 * writes signal groups, reservations or navigation. As with Create's block
 * signals, all branches belong to a section until another boundary is reached.
 */
public final class RelayCaseRailwayManager {

	private static final Map<GlobalRailwayManager, State> STATES = new WeakHashMap<>();
	private static final Map<Train, List<Anchor>> RESTORED_OCCUPANCY = new WeakHashMap<>();

	private RelayCaseRailwayManager() {}

	private record Anchor(UUID boundary, boolean primary) {}

	/** Keep detached/crashed train occupancy across a server restart, as Create does. */
	public static void write(Train train, CompoundTag tag) {
		Collection<Anchor> anchors = RESTORED_OCCUPANCY.getOrDefault(train, List.of());
		for (State state : STATES.values()) {
			Footprint footprint = state.trains.get(train.id);
			if (footprint != null && footprint.train == train) {
				anchors = footprint.anchors();
				break;
			}
		}
		if (anchors.isEmpty())
			return;
		ListTag list = new ListTag();
		for (Anchor anchor : anchors) {
			CompoundTag entry = new CompoundTag();
			entry.putUUID("Boundary", anchor.boundary);
			entry.putBoolean("Primary", anchor.primary);
			list.add(entry);
		}
		tag.put("RailwardRelayOccupancy", list);
	}

	public static void read(Train train, CompoundTag tag) {
		List<Anchor> anchors = new ArrayList<>();
		for (Tag element : tag.getList("RailwardRelayOccupancy", Tag.TAG_COMPOUND)) {
			CompoundTag entry = (CompoundTag) element;
			if (entry.hasUUID("Boundary"))
				anchors.add(new Anchor(entry.getUUID("Boundary"), entry.getBoolean("Primary")));
		}
		if (!anchors.isEmpty())
			RESTORED_OCCUPANCY.put(train, anchors);
	}

	public static void tick(GlobalRailwayManager railways, Level level) {
		if (level.isClientSide || level.dimension() != Level.OVERWORLD)
			return;
		STATES.computeIfAbsent(railways, $ -> new State()).tick(railways);
	}

	static final class Section {
		private Section parent = this;
		private int rank;
		boolean occupied;
		final Set<Section> intersections = Collections.newSetFromMap(new IdentityHashMap<>());
		final List<Anchor> anchors = new ArrayList<>();

		Section root() {
			if (parent != this)
				parent = parent.root();
			return parent;
		}

		void join(Section other) {
			Section a = root();
			Section b = other.root();
			if (a == b)
				return;
			if (a.rank < b.rank)
				a.parent = b;
			else {
				b.parent = a;
				if (a.rank == b.rank)
					a.rank++;
			}
		}
	}

	private static final class State {
		private final Map<TrackGraph, GraphSections> graphs = new IdentityHashMap<>();
		private final Map<UUID, Footprint> trains = new HashMap<>();
		private boolean active;

		void tick(GlobalRailwayManager railways) {
			for (GraphSections graph : graphs.values())
				for (Section section : graph.sections)
					section.occupied = false;
			boolean changed = graphs.keySet().removeIf(graph -> railways.trackNetworks.get(graph.id) != graph);
			boolean enabled = false;
			for (TrackGraph graph : railways.trackNetworks.values()) {
				GraphSections current = graphs.get(graph);
				long revision = ((RelayGraphVersion) graph).railward$getRelayRevision();
				enabled |= current != null && current.revision == revision ? current.hasCabinets
					: !graph.getPoints(AllEdgePointTypes.RELAY_CASE).isEmpty();
			}
			for (TrackGraph graph : railways.trackNetworks.values()) {
				GraphSections current = graphs.get(graph);
				long revision = ((RelayGraphVersion) graph).railward$getRelayRevision();
				if (current == null || current.revision != revision || active != enabled) {
					graphs.put(graph, new GraphSections(graph, revision, enabled));
					changed = true;
				}
			}
			active = enabled;
			if (changed)
				resolveIntersections();
			if (!active) {
				trains.clear();
				return;
			}

			trains.keySet().retainAll(railways.trains.keySet());
			for (Train train : railways.trains.values()) {
				if (train.invalid)
					continue;
				GraphSections graph = graphs.get(train.graph);
				Footprint footprint = trains.get(train.id);
				if (graph != null && !graph.edges.isEmpty() && !train.carriages.isEmpty()) {
					if (footprint == null || footprint.graph != graph || footprint.train != train) {
						footprint = new Footprint(graph, train);
						trains.put(train.id, footprint);
					}
					footprint.update();
					RESTORED_OCCUPANCY.remove(train);
				} else if (train.graph != null) {
					trains.remove(train.id);
					RESTORED_OCCUPANCY.remove(train);
					continue;
				}
				// Detached/crashed trains retain their last occupancy, just as Create
				// retains occupiedSignalBlocks while a train is detached.
				if (footprint != null && graphs.get(footprint.graph.graph) == footprint.graph)
					for (Section section : footprint.occupied)
						section.occupied = true;
				else if (train.graph == null) {
					Collection<Anchor> anchors = footprint != null ? footprint.anchors()
						: RESTORED_OCCUPANCY.getOrDefault(train, List.of());
					for (Anchor anchor : anchors)
						for (GraphSections candidate : graphs.values()) {
							RelayCaseEdgePoint boundary = candidate.graph.getPoint(AllEdgePointTypes.RELAY_CASE, anchor.boundary);
							if (boundary == null)
								continue;
							Section section = boundary.getSection(anchor.primary);
							if (section != null)
								section.root().occupied = true;
							break;
						}
				}
			}

			// Preserve even a complete high-speed passage through a short block
			// between two occupancy samples for one server tick.
			for (GraphSections graph : graphs.values())
				for (RelayCaseEdgePoint boundary : graph.boundaries)
					boundary.applyCrossing();
			ArrayDeque<Section> frontier = new ArrayDeque<>();
			for (GraphSections graph : graphs.values())
				for (Section section : graph.sections)
					if (section.occupied)
						frontier.add(section);
			while (!frontier.isEmpty())
				for (Section other : frontier.removeFirst().intersections)
					if (!other.occupied) {
						other.occupied = true;
						frontier.add(other);
					}
		}

		private void resolveIntersections() {
			Map<TrackNodeLocation, GraphSections> locations = new HashMap<>();
			for (GraphSections graph : graphs.values()) {
				for (Section section : graph.sections)
					section.intersections.clear();
				for (TrackNodeLocation location : graph.graph.getNodes())
					locations.put(location, graph);
			}
			for (GraphSections graph : graphs.values())
				graph.edges.forEach((edge, span) -> edge.getEdgeData().getIntersections().forEach(intersection -> {
					GraphSections target = locations.get(intersection.target.getFirst());
					if (target == null)
						return;
					TrackEdge targetEdge = target.graph.getConnection(intersection.target.map(target.graph::locateNode));
					Section a = graph.at(edge, intersection.location);
					Section b = target.at(targetEdge, intersection.targetLocation);
					if (a != null && b != null && a != b) {
						a.intersections.add(b);
						b.intersections.add(a);
					}
				}));
		}
	}

	private static final class GraphSections {
		final TrackGraph graph;
		final long revision;
		final boolean hasCabinets;
		final List<RelayCaseEdgePoint> boundaries;
		final Map<TrackEdge, Span> edges = new IdentityHashMap<>();
		final Set<Section> sections = Collections.newSetFromMap(new IdentityHashMap<>());

		GraphSections(TrackGraph graph, long revision, boolean enabled) {
			this.graph = graph;
			this.revision = revision;
			boundaries = new ArrayList<>(graph.getPoints(AllEdgePointTypes.RELAY_CASE));
			hasCabinets = !boundaries.isEmpty();
			// An intersecting track can contain a train without having a cabinet.
			// Build that graph's sections too, while the relay system is in use.
			if (!enabled)
				return;
			Map<TrackNode, Section> endpoints = new IdentityHashMap<>();
			for (TrackNodeLocation location : graph.getNodes()) {
				TrackNode node = graph.locateNode(location);
				for (TrackEdge edge : graph.getConnectionsFrom(node).values()) {
					if (edges.containsKey(edge))
						continue;
					Span span = new Span(edge);
					edges.put(edge, span);
					TrackEdge opposite = graph.getConnectionsFrom(edge.node2).get(edge.node1);
					if (opposite != null)
						edges.put(opposite, span);
					joinEndpoint(endpoints, edge.node1, span.sections[0]);
					joinEndpoint(endpoints, edge.node2, span.sections[span.sections.length - 1]);
				}
			}
			Set<Span> unique = Collections.newSetFromMap(new IdentityHashMap<>());
			unique.addAll(edges.values());
			for (Span span : unique) {
				for (int i = 0; i < span.sections.length; i++) {
					span.sections[i] = span.sections[i].root();
					sections.add(span.sections[i]);
				}
				for (int i = 0; i < span.boundaries.size(); i++) {
					RelayCaseEdgePoint boundary = span.boundaries.get(i);
					boundary.setSections(span.sections[i], span.sections[i + 1], boundary.isPrimary(span.edge.node1));
					span.sections[i].anchors.add(new Anchor(boundary.id, boundary.isPrimary(span.edge.node1)));
					span.sections[i + 1].anchors.add(new Anchor(boundary.id, !boundary.isPrimary(span.edge.node1)));
				}
			}
		}

		private static void joinEndpoint(Map<TrackNode, Section> endpoints, TrackNode node, Section section) {
			Section previous = endpoints.putIfAbsent(node, section);
			if (previous != null)
				previous.join(section);
		}

		Section at(TrackEdge edge, double position) {
			Span span = edges.get(edge);
			return span == null ? null : span.at(edge == span.edge ? position : edge.getLength() - position);
		}
	}

	private static final class Span {
		final TrackEdge edge;
		final List<RelayCaseEdgePoint> boundaries = new ArrayList<>();
		final double[] positions;
		final Section[] sections;

		Span(TrackEdge edge) {
			this.edge = edge;
			for (var point : edge.getEdgeData().getPoints())
				if (point instanceof RelayCaseEdgePoint boundary)
					boundaries.add(boundary);
			boundaries.sort(Comparator.comparingDouble(point -> point.getLocationOn(edge)));
			positions = boundaries.stream().mapToDouble(point -> point.getLocationOn(edge)).toArray();
			sections = new Section[positions.length + 1];
			Arrays.setAll(sections, $ -> new Section());
		}

		Section at(double position) {
			int index = Arrays.binarySearch(positions, position);
			return sections[index >= 0 ? index + 1 : -index - 1];
		}
	}

	/** Re-scout the body only when a wheel changes edge/section or the consist changes. */
	private static final class Footprint {
		final GraphSections graph;
		final Train train;
		final Set<Section> occupied = Collections.newSetFromMap(new IdentityHashMap<>());
		final List<TravellingPoint> wheels = new ArrayList<>();
		final List<TrackEdge> edges = new ArrayList<>();
		final List<Section> wheelSections = new ArrayList<>();

		Footprint(GraphSections graph, Train train) {
			this.graph = graph;
			this.train = train;
		}

		Set<Anchor> anchors() {
			Set<Anchor> anchors = new HashSet<>();
			for (Section section : occupied)
				anchors.addAll(section.anchors);
			return anchors;
		}

		void update() {
			int[] index = { 0 };
			boolean[] changed = { false };
			train.forEachTravellingPoint(point -> {
				int i = index[0]++;
				Section section = graph.at(point.edge, point.position);
				if (i >= wheels.size()) {
					wheels.add(point);
					edges.add(point.edge);
					wheelSections.add(section);
					changed[0] = true;
				} else if (wheels.get(i) != point || edges.get(i) != point.edge || wheelSections.get(i) != section) {
					wheels.set(i, point);
					edges.set(i, point.edge);
					wheelSections.set(i, section);
					changed[0] = true;
				}
			});
			if (index[0] < wheels.size()) {
				wheels.subList(index[0], wheels.size()).clear();
				edges.subList(index[0], edges.size()).clear();
				wheelSections.subList(index[0], wheelSections.size()).clear();
				changed[0] = true;
			}
			if (!changed[0])
				return;
			occupied.clear();
			for (Section section : wheelSections)
				add(section);
			TravellingPoint tail = train.carriages.get(train.carriages.size() - 1).getTrailingPoint();
			if (tail.edge == null || tail.node1 == null || tail.node2 == null)
				return;
			TravellingPoint scout = new TravellingPoint(tail.node1, tail.node2, tail.edge, tail.position, tail.upsideDown);
			train.forEachTravellingPointBackwards((point, distance) -> {
				if (point.edge == null)
					return;
				scout.travel(graph.graph, distance, scout.follow(point), (d, couple) -> {
					if (couple.getFirst() instanceof RelayCaseEdgePoint relay) {
						add(relay.getSection(couple.getSecond().getFirst()));
						add(relay.getSection(couple.getSecond().getSecond()));
					}
					return false;
				}, scout.ignoreTurns());
			});
		}

		private void add(Section section) {
			if (section != null)
				occupied.add(section.root());
		}
	}
}
