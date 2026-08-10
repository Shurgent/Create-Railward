package ua.pp.shurgent.railward.block.train.parts;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

import com.simibubi.create.Create;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import com.simibubi.create.content.trains.entity.Navigation;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.entity.TravellingPoint;
import com.simibubi.create.content.trains.entity.TravellingPoint.ITrackSelector;
import com.simibubi.create.content.trains.graph.TrackNode;
import com.simibubi.create.content.trains.signal.SignalBlock.SignalType;
import com.simibubi.create.content.trains.signal.SignalBlockEntity.SignalState;
import com.simibubi.create.content.trains.signal.SignalBoundary;
import com.simibubi.create.content.trains.signal.SignalEdgeGroup;
import com.simibubi.create.content.trains.signal.TrackEdgePoint;
import com.simibubi.create.foundation.utility.Couple;
import com.simibubi.create.foundation.utility.Pair;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;
import ua.pp.shurgent.railward.AllBlocks;
import ua.pp.shurgent.railward.RailwardPackets;
import ua.pp.shurgent.railward.RailwardSoundEvents;
import ua.pp.shurgent.railward.block.train.parts.TrainDirectionState.DirectionState;
import ua.pp.shurgent.railward.network.CabSignalTrainStatePacket;

public final class CabSignalTrainState {

	private static final String NBT_RED_LATCH = "RailwardCabSignalRedLatch";
	private static final String NBT_BOUNDARY = "Boundary";
	private static final String NBT_GROUP = "Group";
	private static final String NBT_DIRECTION = "Direction";
	private static final double SCAN_EPSILON = 1 / 64d;
	private static final double MAX_SCAN_DISTANCE = 8192;
	private static final int PERIODIC_SYNC_INTERVAL = 100;

	private static final Map<UUID, CabSignalAspect> CLIENT_ASPECTS = new HashMap<>();
	private static final Map<UUID, ServerState> SERVER_STATES = new HashMap<>();

	private CabSignalTrainState() {
	}

	public static CabSignalAspect resolve(CarriageContraptionEntity entity) {
		Carriage carriage = entity.getCarriage();
		if (carriage == null || carriage.train == null)
			return CabSignalAspect.OFF;
		return CLIENT_ASPECTS.getOrDefault(carriage.train.id, CabSignalAspect.OFF);
	}

	public static void setClientAspect(UUID trainId, CabSignalAspect aspect) {
		CLIENT_ASPECTS.put(trainId, aspect);
	}

	public static void removeClientAspect(UUID trainId) {
		CLIENT_ASPECTS.remove(trainId);
	}

	public static void clearClientAspects() {
		CLIENT_ASPECTS.clear();
	}

	public static void tickAll(Level level) {
		if (level.isClientSide || level.dimension() != Level.OVERWORLD || Create.RAILWAYS == null
			|| Create.RAILWAYS.trains == null)
			return;

		Set<UUID> present = new HashSet<>();
		for (Train train : Create.RAILWAYS.trains.values()) {
			present.add(train.id);
			ServerState serverState = SERVER_STATES.computeIfAbsent(train.id, id -> new ServerState());
			boolean installed = hasCabSignal(train);
			CabSignalAspect aspect = installed ? calculateAspect(train, serverState) : CabSignalAspect.OFF;
			if (!installed && serverState.aspect == CabSignalAspect.OFF) {
				serverState.forceSync = false;
				continue;
			}
			boolean changed = aspect != serverState.aspect;
			boolean periodic = Math.floorMod(level.getGameTime() + train.id.getLeastSignificantBits(),
				PERIODIC_SYNC_INTERVAL) == 0;
			if (!changed && !serverState.forceSync && !periodic)
				continue;

			CabSignalAspect previous = serverState.aspect;
			serverState.aspect = aspect;
			serverState.forceSync = false;
			sendToTracking(train, aspect);
			if (installed && aspect == CabSignalAspect.RED && previous != CabSignalAspect.RED)
				playRedWarning(train, serverState.latch == null ? TrainDirectionState.resolveServer(train)
					: serverState.latch.direction);
		}
		SERVER_STATES.keySet().retainAll(present);
	}

	@Nullable
	public static PassSnapshot snapshotFrontEdgePoint(Train train,
		Pair<TrackEdgePoint, Couple<TrackNode>> couple) {
		if (!(couple.getFirst() instanceof SignalBoundary signal))
			return null;
		TrackNode approachSide = couple.getSecond().getSecond();
		boolean primary = signal.isPrimary(approachSide);
		if (signal.blockEntities.get(primary).isEmpty())
			return null;

		UUID enteredGroupId = signal.groups.get(primary);
		SignalEdgeGroup enteredGroup = Create.RAILWAYS.signalEdgeGroups.get(enteredGroupId);
		SignalType type = signal.types.get(primary);
		SignalState state = signal.cachedStates.get(primary);
		boolean forcedRed = signal.isForcedRed(primary);
		boolean reservedByThisBoundary = enteredGroup != null && enteredGroup.reserved == signal;
		boolean crossRouteReserved = type != SignalType.CROSS_SIGNAL
			|| train.reservedSignalBlocks.contains(enteredGroupId);
		boolean scheduledAuthorization = TrainDirectionState.hasScheduledControls(train)
			&& reservedByThisBoundary && crossRouteReserved && !forcedRed;
		DirectionState direction = resolveTravelDirection(train);
		return new PassSnapshot(signal.id, enteredGroupId, state, direction, scheduledAuthorization);
	}

	public static void finishFrontEdgePoint(Train train, @Nullable PassSnapshot snapshot, boolean stopped) {
		if (snapshot == null || stopped)
			return;
		ServerState state = SERVER_STATES.computeIfAbsent(train.id, id -> new ServerState());
		state.latch = null;
		if (snapshot.state == SignalState.RED && !snapshot.scheduledAuthorization
			&& snapshot.enteredGroupId != null && snapshot.direction != DirectionState.NONE)
			state.latch = new RedLatch(snapshot.boundaryId, snapshot.enteredGroupId, snapshot.direction);
		state.forceSync = true;
	}

	public static void syncCurrent(CarriageContraptionEntity entity, ServerPlayer player) {
		Carriage carriage = entity.getCarriage();
		if (carriage == null || carriage.train == null)
			return;
		ServerState state = SERVER_STATES.computeIfAbsent(carriage.train.id, id -> new ServerState());
		sendToPlayer(player, carriage.train.id, state.aspect);
	}

	public static void syncAll(ServerPlayer player) {
		if (Create.RAILWAYS == null || Create.RAILWAYS.trains == null)
			return;
		for (Train train : Create.RAILWAYS.trains.values()) {
			ServerState state = SERVER_STATES.computeIfAbsent(train.id, id -> new ServerState());
			sendToPlayer(player, train.id, state.aspect);
		}
	}

	public static void removeServerState(UUID trainId) {
		SERVER_STATES.remove(trainId);
	}

	public static void clearServerStates() {
		SERVER_STATES.clear();
	}

	public static void write(Train train, CompoundTag tag) {
		ServerState state = SERVER_STATES.get(train.id);
		if (state == null || state.latch == null)
			return;
		CompoundTag latchTag = new CompoundTag();
		latchTag.putUUID(NBT_BOUNDARY, state.latch.boundaryId);
		latchTag.putUUID(NBT_GROUP, state.latch.enteredGroupId);
		latchTag.putString(NBT_DIRECTION, state.latch.direction.name());
		tag.put(NBT_RED_LATCH, latchTag);
	}

	public static void read(Train train, CompoundTag tag) {
		if (!tag.contains(NBT_RED_LATCH))
			return;
		CompoundTag latchTag = tag.getCompound(NBT_RED_LATCH);
		if (!latchTag.hasUUID(NBT_BOUNDARY) || !latchTag.hasUUID(NBT_GROUP))
			return;
		try {
			DirectionState direction = DirectionState.valueOf(latchTag.getString(NBT_DIRECTION));
			ServerState state = SERVER_STATES.computeIfAbsent(train.id, id -> new ServerState());
			state.latch = new RedLatch(latchTag.getUUID(NBT_BOUNDARY), latchTag.getUUID(NBT_GROUP), direction);
			state.forceSync = true;
		} catch (IllegalArgumentException ignored) {
		}
	}

	private static CabSignalAspect calculateAspect(Train train, ServerState state) {
		DirectionState direction = TrainDirectionState.resolveServer(train);
		if (direction == DirectionState.NONE || train.derailed || train.graph == null || train.carriages.isEmpty())
			return CabSignalAspect.OFF;

		TravellingPoint head = getHeadPoint(train, direction);
		if (head == null || head.edge == null)
			return CabSignalAspect.WHITE;
		UUID headGroupId = head.edge.getEdgeData().getGroupAtPosition(train.graph, head.position);

		if (state.latch != null) {
			if (state.latch.direction != direction)
				state.latch = null;
			else if (headGroupId != null && headGroupId.equals(state.latch.enteredGroupId))
				return CabSignalAspect.RED;
			else if (headGroupId != null)
				state.latch = null;
		}

		SignalEdgeGroup headGroup = Create.RAILWAYS.signalEdgeGroups.get(headGroupId);
		if (headGroup != null && headGroup.trains.stream().anyMatch(other -> other != train))
			return CabSignalAspect.RED;

		return findNextSignalAspect(train, head, direction);
	}

	private static boolean hasCabSignal(Train train) {
		for (Carriage carriage : train.carriages) {
			CarriageContraptionEntity entity = carriage.anyAvailableEntity();
			if (entity == null)
				continue;
			Contraption contraption = entity.getContraption();
			if (contraption != null && contraption.getBlocks().values().stream()
				.anyMatch(info -> AllBlocks.CAB_SIGNAL.has(info.state)))
				return true;
		}
		return false;
	}

	private static CabSignalAspect findNextSignalAspect(Train train, TravellingPoint head,
		DirectionState direction) {
		TravellingPoint scout = copy(head);
		ITrackSelector selector = createRouteSelector(train, scout);
		double sign = direction == DirectionState.FORWARD ? 1 : -1;
		scout.travel(train.graph, sign * SCAN_EPSILON, selector);

		CabSignalAspect[] found = { null };
		scout.travel(train.graph, sign * MAX_SCAN_DISTANCE, selector, (distance, couple) -> {
			if (!(couple.getFirst() instanceof SignalBoundary signal))
				return false;
			TrackNode approachSide = couple.getSecond().getSecond();
			boolean primary = signal.isPrimary(approachSide);
			if (signal.blockEntities.get(primary).isEmpty())
				return false;
			found[0] = map(signal.cachedStates.get(primary));
			return true;
		});
		return found[0] == null ? CabSignalAspect.WHITE : found[0];
	}

	private static ITrackSelector createRouteSelector(Train train, TravellingPoint scout) {
		Navigation navigation = train.navigation;
		if (navigation == null || !navigation.isActive())
			return scout.steer(train.manualSteer, new Vec3(0, 1, 0));
		return navigation.controlSignalScout();
	}

	@Nullable
	private static TravellingPoint getHeadPoint(Train train, DirectionState direction) {
		if (train.carriages.isEmpty())
			return null;
		return direction == DirectionState.FORWARD ? train.carriages.get(0).getLeadingPoint()
			: train.carriages.get(train.carriages.size() - 1).getTrailingPoint();
	}

	private static TravellingPoint copy(TravellingPoint point) {
		return new TravellingPoint(point.node1, point.node2, point.edge, point.position, point.upsideDown);
	}

	private static DirectionState resolveTravelDirection(Train train) {
		DirectionState controlled = TrainDirectionState.resolveServer(train);
		if (controlled != DirectionState.NONE)
			return controlled;
		if (Math.abs(train.speed) < 1.0E-4)
			return DirectionState.NONE;
		return train.speed < 0 ? DirectionState.BACKWARD : DirectionState.FORWARD;
	}

	private static CabSignalAspect map(SignalState state) {
		return switch (state) {
		case GREEN -> CabSignalAspect.GREEN;
		case YELLOW -> CabSignalAspect.YELLOW;
		case RED -> CabSignalAspect.YELLOW_RED;
		case INVALID -> CabSignalAspect.WHITE;
		};
	}

	private static void sendToTracking(Train train, CabSignalAspect aspect) {
		CabSignalTrainStatePacket packet = new CabSignalTrainStatePacket(train.id, aspect);
		for (Carriage carriage : train.carriages)
			carriage.forEachPresentEntity(entity -> RailwardPackets.getChannel()
				.send(PacketDistributor.TRACKING_ENTITY.with(() -> entity), packet));
	}

	private static void sendToPlayer(Player player, UUID trainId, CabSignalAspect aspect) {
		if (!(player instanceof ServerPlayer serverPlayer))
			return;
		RailwardPackets.getChannel().send(PacketDistributor.PLAYER.with(() -> serverPlayer),
			new CabSignalTrainStatePacket(trainId, aspect));
	}

	private static void playRedWarning(Train train, DirectionState direction) {
		if (train.carriages.isEmpty())
			return;
		Carriage carriage = direction == DirectionState.BACKWARD ? train.carriages.get(train.carriages.size() - 1)
			: train.carriages.get(0);
		CarriageContraptionEntity entity = carriage.anyAvailableEntity();
		if (entity == null)
			return;
		entity.level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
			RailwardSoundEvents.CAB_SIGNAL_RED.get(), SoundSource.BLOCKS, 1, 1);
	}

	public record PassSnapshot(UUID boundaryId, UUID enteredGroupId, SignalState state, DirectionState direction,
		boolean scheduledAuthorization) {
	}

	private record RedLatch(UUID boundaryId, UUID enteredGroupId, DirectionState direction) {
	}

	private static class ServerState {
		private CabSignalAspect aspect = CabSignalAspect.OFF;
		private RedLatch latch;
		private boolean forceSync;
	}
}
