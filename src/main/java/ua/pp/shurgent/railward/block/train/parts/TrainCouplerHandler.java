package ua.pp.shurgent.railward.block.train.parts;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.Create;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.CarriageBogey;
import com.simibubi.create.content.trains.entity.CarriageContraption;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.entity.TravellingPoint;
import com.simibubi.create.content.trains.graph.TrackEdge;
import com.simibubi.create.foundation.utility.Couple;
import com.simibubi.create.foundation.utility.VecHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;
import ua.pp.shurgent.railward.AllBlocks;
import ua.pp.shurgent.railward.RailwardConfig;
import ua.pp.shurgent.railward.RailwardPackets;
import ua.pp.shurgent.railward.RailwardSoundEvents;
import ua.pp.shurgent.railward.mixin.create.CarriageBogeyAccessor;
import ua.pp.shurgent.railward.mixin.create.TrainAccessor;
import ua.pp.shurgent.railward.network.TrainCouplerSyncPacket;
import ua.pp.shurgent.railward.network.TrainCouplerSyncPacket.RebaseInstruction;

public class TrainCouplerHandler {

	private static final Logger LOGGER = LogUtils.getLogger();
	private static final AtomicInteger DEBUG_ATTEMPT_COUNTER = new AtomicInteger();

	private static final double STOPPED_EPSILON = 1.0E-4;
	private static final double SIDE_EPSILON = 1.0E-4;
	private static final double MAX_COUPLER_DISTANCE_SQ = 25;
	private static final double MIN_COUPLING_DISTANCE_SQ = 0.25;
	private static final double MAX_COUPLING_DISTANCE_SQ = 2.25;
	private static final double MAX_AUTO_COUPLING_DISTANCE_SQ = 1.0;
	private static final double MIN_AXIS_DOT = 0.5;
	private static final double AUTO_MOVEMENT_EPSILON_SQ = 1.0E-6;

	private static final Map<UUID, Vec3> AUTO_LAST_TRAIN_CENTERS = new HashMap<>();
	private static final List<SuppressedCouplerPair> AUTO_SUPPRESSED_COUPLERS = new ArrayList<>();

	public static void tickAutoCouplers(ServerLevel level) {
		if (!Level.OVERWORLD.equals(level.dimension()))
			return;

		int interval = Math.max(1, RailwardConfig.COMMON.trainCouplerAutoScanIntervalTicks.get());
		if (level.getGameTime() % interval != 0)
			return;

		int debugAttempt = isAutoCouplerDebugEnabled() ? DEBUG_ATTEMPT_COUNTER.incrementAndGet() : 0;
		if (!RailwardConfig.COMMON.trainCouplerAutoCoupling.get()) {
			autoDebug(debugAttempt, "auto tick skipped: disabled dimension={} gameTime={}",
				level.dimension().location(), level.getGameTime());
			return;
		}

		List<Train> trains = new ArrayList<>(Create.RAILWAYS.sided(level).trains.values());
		Map<UUID, Vec3> currentCenters = collectCurrentTrainCenters(level, trains);
		cleanupSuppressedAutoCouplers(level);
		autoDebug(debugAttempt, "auto tick: dimension={} gameTime={} trains={} centers={} previousCenters={} suppressedPairs={} interval={} maxSpeed={}",
			level.dimension().location(), level.getGameTime(), trains.size(), currentCenters.size(),
			AUTO_LAST_TRAIN_CENTERS.size(), AUTO_SUPPRESSED_COUPLERS.size(), interval,
			getAutoMaxSpeed());

		try {
			for (Train movingTrain : trains) {
				Vec3 currentCenter = currentCenters.get(movingTrain.id);
				Vec3 previousCenter = AUTO_LAST_TRAIN_CENTERS.get(movingTrain.id);
				if (currentCenter == null) {
					autoDebug(debugAttempt, "auto train skipped: {} has no current center", debugTrain(movingTrain));
					continue;
				}
				if (previousCenter == null) {
					autoDebug(debugAttempt, "auto train sampled for the first time: {} currentCenter={}",
						debugTrain(movingTrain), debugVec(currentCenter));
					continue;
				}

				Vec3 movement = currentCenter.subtract(previousCenter);
				autoDebug(debugAttempt,
					"auto train sample: {} previousCenter={} currentCenter={} movement={} movementSq={}",
					debugTrain(movingTrain), debugVec(previousCenter), debugVec(currentCenter), debugVec(movement),
					debugDistance(movement.lengthSqr()));
				if (tryAutoCouple(debugAttempt, level, movingTrain, currentCenter, movement))
					return;
			}
		} finally {
			AUTO_LAST_TRAIN_CENTERS.clear();
			AUTO_LAST_TRAIN_CENTERS.putAll(currentCenters);
		}
	}

	private static Map<UUID, Vec3> collectCurrentTrainCenters(Level level, Collection<Train> trains) {
		Map<UUID, Vec3> centers = new HashMap<>();
		for (Train train : trains) {
			Vec3 center = getTrainCenter(level, train);
			if (center != null)
				centers.put(train.id, center);
		}
		return centers;
	}

	private static Vec3 getTrainCenter(Level level, Train train) {
		Vec3 sum = Vec3.ZERO;
		int count = 0;

		for (Carriage carriage : train.carriages) {
			boolean addedCarriagePosition = false;
			Carriage.DimensionalCarriageEntity dimensional = carriage.getDimensionalIfPresent(level.dimension());
			if (dimensional != null) {
				if (dimensional.leadingAnchor() != null) {
					sum = sum.add(dimensional.leadingAnchor());
					count++;
					addedCarriagePosition = true;
				}
				if (dimensional.trailingAnchor() != null) {
					sum = sum.add(dimensional.trailingAnchor());
					count++;
					addedCarriagePosition = true;
				}
			}

			if (!addedCarriagePosition) {
				CarriageContraptionEntity entity = carriage.anyAvailableEntity();
				if (entity != null && entity.level == level) {
					sum = sum.add(entity.position());
					count++;
				}
			}
		}

		return count == 0 ? null : sum.scale(1.0 / count);
	}

	private static boolean tryAutoCouple(int debugAttempt, ServerLevel level, Train movingTrain, Vec3 trainCenter,
		Vec3 movement) {
		if (movement.lengthSqr() <= AUTO_MOVEMENT_EPSILON_SQ) {
			autoDebug(debugAttempt, "auto skipped moving train {} because movement is too small: movementSq={}",
				debugTrain(movingTrain), debugDistance(movement.lengthSqr()));
			return false;
		}

		Result movingValidation = validateMovingTrainForAuto(movingTrain);
		if (!movingValidation.success()) {
			autoDebug(debugAttempt, "auto skipped moving train {} validation={}", debugTrain(movingTrain),
				movingValidation.message());
			return false;
		}
		if (!areAllCarriagesStraightHorizontal(movingTrain)) {
			autoDebug(debugAttempt, "auto skipped moving train {} because it is not fully straight horizontal",
				debugTrain(movingTrain));
			return false;
		}

		Vec3 movementDirection = movement.normalize();
		MovingCoupler movingCoupler =
			findMovingAutoCoupler(debugAttempt, level, movingTrain, trainCenter, movementDirection);
		if (movingCoupler == null)
			return false;

		CouplingCandidate candidate =
			findAutoCouplingCandidate(debugAttempt, level, movingTrain, movingCoupler);
		if (candidate == null) {
			autoDebug(debugAttempt, "auto skipped moving train {} because no candidate coupler was found",
				debugTrain(movingTrain));
			return false;
		}

		Set<ServerPlayer> messagePlayers = collectAutoMessagePlayers(level, movingTrain);
		sendAutoCouplerPrompt(debugAttempt, messagePlayers, movingTrain, candidate.distanceSq());

		if (candidate.distanceSq() > MAX_AUTO_COUPLING_DISTANCE_SQ) {
			autoDebug(debugAttempt,
				"auto candidate is visible but too far: movingTrain={} candidateTrain={} distanceSq={} maxDistanceSq={}",
				debugTrain(movingTrain), debugTrain(candidate.train()), debugDistance(candidate.distanceSq()),
				debugDistance(MAX_AUTO_COUPLING_DISTANCE_SQ));
			return false;
		}
		if (candidate.distanceSq() < MIN_COUPLING_DISTANCE_SQ) {
			autoDebug(debugAttempt,
				"auto candidate is visible but too close: movingTrain={} candidateTrain={} distanceSq={} minDistanceSq={}",
				debugTrain(movingTrain), debugTrain(candidate.train()), debugDistance(candidate.distanceSq()),
				debugDistance(MIN_COUPLING_DISTANCE_SQ));
			return false;
		}

		autoDebug(debugAttempt, "auto coupling started: movingTrain={} movingCarriage={} movingEnd={} candidateTrain={}",
			debugTrain(movingTrain), movingCoupler.carriageIndex(), movingCoupler.end(), debugTrain(candidate.train()));
		String movingTrainName = movingTrain.name.getString();
		String attachedTrainName = candidate.train()
			.name
			.getString();
		Vec3 soundPos = movingCoupler.coupler()
			.center();
		Result result = completeCoupling(debugAttempt, level, movingTrain, movingCoupler.carriageIndex(),
			movingCoupler.end(), movingCoupler.coupler(), candidate, movingTrain, true);
		autoDebug(debugAttempt, "auto coupling finished: success={} message={}", result.success(), result.message());
		if (result.success()) {
			level.playSound(null, soundPos.x, soundPos.y, soundPos.z, RailwardSoundEvents.TRAIN_COUPLER_AUTO.get(),
				SoundSource.BLOCKS, 1, 1);
			sendAutoCoupledMessage(debugAttempt, messagePlayers, movingTrainName, attachedTrainName);
		}
		return result.success();
	}

	private static Result validateMovingTrainForAuto(Train train) {
		if (train.graph == null || train.derailed)
			return Result.fail("railward.train_coupler.error.train_invalid");
		if (train.runtime.getSchedule() != null)
			return Result.fail("railward.train_coupler.error.scheduled");
		if (!train.canDisassemble())
			return Result.fail("railward.train_coupler.error.not_aligned");
		double maxSpeed = getAutoMaxSpeed();
		double speed = Math.abs(train.speed);
		if (speed <= STOPPED_EPSILON)
			return Result.fail("railward.train_coupler.error.train_moving");
		if (speed > maxSpeed)
			return Result.fail("railward.train_coupler.error.too_fast");
		return Result.success("");
	}

	private static double getAutoMaxSpeed() {
		return RailwardConfig.COMMON.trainCouplerAutoMaxSpeed.get();
	}

	private static MovingCoupler findMovingAutoCoupler(int debugAttempt, Level level, Train train, Vec3 trainCenter,
		Vec3 movementDirection) {
		List<CouplerLocation> couplers = collectOuterCouplers(debugAttempt, level, train);
		CouplerLocation best = null;
		double bestProjection = -Double.MAX_VALUE;

		for (CouplerLocation coupler : couplers) {
			double projection = coupler.coupler()
				.center()
				.subtract(trainCenter)
				.dot(movementDirection);
			autoDebug(debugAttempt, "auto moving coupler candidate: train={} carriage={} end={} center={} projection={}",
				debugTrain(train), coupler.carriageIndex(), coupler.end(), debugVec(coupler.coupler()
					.center()), debugDistance(projection));

			if (projection <= bestProjection)
				continue;

			bestProjection = projection;
			best = coupler;
		}

		if (best == null) {
			autoDebug(debugAttempt, "auto skipped moving train {} because no outer coupler was found", debugTrain(train));
			return null;
		}

		autoDebug(debugAttempt, "auto moving coupler selected: train={} carriage={} end={} center={} projection={}",
			debugTrain(train), best.carriageIndex(), best.end(), debugVec(best.coupler()
				.center()), debugDistance(bestProjection));
		return new MovingCoupler(best.carriageIndex(), best.end(), best.coupler(), movementDirection);
	}

	private static CouplingCandidate findAutoCouplingCandidate(int debugAttempt, Level level, Train movingTrain,
		MovingCoupler movingCoupler) {
		CouplingCandidate best = null;
		double bestDistance = MAX_COUPLER_DISTANCE_SQ;

		autoDebug(debugAttempt, "auto candidate search started: trainsInLevel={} maxDistanceSq={}",
			Create.RAILWAYS.sided(level).trains.size(), debugDistance(MAX_COUPLER_DISTANCE_SQ));
		for (Train candidateTrain : Create.RAILWAYS.sided(level).trains.values()) {
			if (candidateTrain == movingTrain)
				continue;

			Result validation = validateTrainReady(candidateTrain);
			if (!validation.success()) {
				autoDebug(debugAttempt, "auto candidate train skipped: {} validation={}", debugTrain(candidateTrain),
					validation.message());
				continue;
			}
			if (candidateTrain.graph != movingTrain.graph) {
				autoDebug(debugAttempt, "auto candidate train skipped: {} different graph", debugTrain(candidateTrain));
				continue;
			}
			if (!areAllCarriagesStraightHorizontal(candidateTrain)) {
				autoDebug(debugAttempt, "auto candidate train skipped: {} not fully straight horizontal",
					debugTrain(candidateTrain));
				continue;
			}

			for (CouplerLocation location : collectOuterCouplers(debugAttempt, level, candidateTrain)) {
				Vec3 offset = location.coupler()
					.center()
					.subtract(movingCoupler.coupler()
						.center());
				double forwardDistance = offset.dot(movingCoupler.direction());
				if (forwardDistance <= 0) {
					autoDebug(debugAttempt,
						"auto candidate coupler rejected: train={} carriage={} center={} behind moving direction forwardDistance={}",
						debugTrain(candidateTrain), location.carriageIndex(), debugVec(location.coupler()
							.center()), debugDistance(forwardDistance));
					continue;
				}

				if (isAutoCouplingSuppressed(movingCoupler.coupler(), location.coupler())) {
					autoDebug(debugAttempt,
						"auto candidate coupler rejected: train={} carriage={} center={} suppressed until separation",
						debugTrain(candidateTrain), location.carriageIndex(), debugVec(location.coupler()
							.center()));
					continue;
				}

				CouplerEnd end = resolveCouplingEndByRelativePosition(level, candidateTrain, location.carriageIndex(),
					location.coupler(), movingCoupler.coupler());
				if (end == null)
					end = location.end();
				if (end == null || !isOuterEnd(candidateTrain, location.carriageIndex(), end)) {
					autoDebug(debugAttempt,
						"auto candidate coupler rejected: train={} carriage={} resolvedEnd={} is not outer",
						debugTrain(candidateTrain), location.carriageIndex(), end);
					continue;
				}

				double distance = movingCoupler.coupler()
					.center()
					.distanceToSqr(location.coupler()
						.center());
				autoDebug(debugAttempt,
					"auto candidate coupler: train={} carriage={} end={} center={} distanceSq={} forwardDistance={}",
					debugTrain(candidateTrain), location.carriageIndex(), end, debugVec(location.coupler()
						.center()), debugDistance(distance), debugDistance(forwardDistance));
				if (distance >= bestDistance)
					continue;

				bestDistance = distance;
				best = new CouplingCandidate(candidateTrain, location.carriageIndex(), end, location.coupler(), distance);
			}
		}

		autoDebug(debugAttempt, "auto candidate search result: train={} carriage={} end={} distanceSq={}",
			best == null ? null : debugTrain(best.train()), best == null ? null : best.carriageIndex(),
			best == null ? null : best.end(), best == null ? null : debugDistance(bestDistance));
		return best;
	}

	private static List<CouplerLocation> collectOuterCouplers(int debugAttempt, Level level, Train train) {
		List<CouplerLocation> couplers = new ArrayList<>();
		int lastCarriageIndex = train.carriages.size() - 1;
		int[] carriageIndices = lastCarriageIndex == 0 ? new int[] { 0 } : new int[] { 0, lastCarriageIndex };

		for (int carriageIndex : carriageIndices) {
			if (carriageIndex < 0 || carriageIndex >= train.carriages.size())
				continue;

			Carriage carriage = train.carriages.get(carriageIndex);
			CarriageContraptionEntity entity = carriage.anyAvailableEntity();
			if (entity == null || entity.level != level) {
				autoDebug(debugAttempt,
					"outer coupler search skipped carriage: train={} carriage={} entityMissingOrWrongLevel entity={}",
					debugTrain(train), carriageIndex, entity == null ? null : entity.getUUID());
				continue;
			}

			Contraption contraption = entity.getContraption();
			if (!(contraption instanceof CarriageContraption)) {
				autoDebug(debugAttempt,
					"outer coupler search skipped carriage: train={} carriage={} is not carriage contraption",
					debugTrain(train), carriageIndex);
				continue;
			}

			for (StructureBlockInfo info : contraption.getBlocks()
				.values()) {
				if (!AllBlocks.TRAIN_COUPLER.has(info.state))
					continue;

				CouplerEnd end = resolveCouplerEnd(level, entity, carriage, info.pos, info.state);
				if (end == null || !isOuterEnd(train, carriageIndex, end)) {
					autoDebug(debugAttempt,
						"outer coupler rejected: train={} carriage={} localPos={} resolvedEnd={} outer={}",
						debugTrain(train), carriageIndex, info.pos, end,
						end != null && isOuterEnd(train, carriageIndex, end));
					continue;
				}

				CouplerRef coupler = CouplerRef.from(entity, info.pos, info.state);
				if (coupler == null)
					continue;

				couplers.add(new CouplerLocation(carriageIndex, end, coupler));
				autoDebug(debugAttempt, "outer coupler found: train={} carriage={} end={} localPos={} center={}",
					debugTrain(train), carriageIndex, end, info.pos, debugVec(coupler.center()));
			}
		}

		autoDebug(debugAttempt, "outer coupler search result: train={} count={}", debugTrain(train), couplers.size());
		return couplers;
	}

	private static Set<ServerPlayer> collectAutoMessagePlayers(ServerLevel level, Train movingTrain) {
		Set<ServerPlayer> players = new HashSet<>();
		for (Carriage carriage : movingTrain.carriages) {
			carriage.forEachPresentEntity(entity -> {
				Optional<UUID> controllingPlayer = entity.getControllingPlayer();
				if (controllingPlayer.isEmpty())
					return;

				if (level.getPlayerByUUID(controllingPlayer.get()) instanceof ServerPlayer player)
					players.add(player);
			});
		}

		return players;
	}

	private static void sendAutoCouplerPrompt(int debugAttempt, Set<ServerPlayer> players, Train movingTrain,
		double distanceSq) {
		if (players.isEmpty()) {
			autoDebug(debugAttempt,
				"auto prompt skipped: moving train {} has no controlling server player distanceSq={}",
				debugTrain(movingTrain), debugDistance(distanceSq));
			return;
		}

		double gap = Math.max(0, Math.sqrt(distanceSq) - 1.0);
		String distance = String.format(Locale.ROOT, "%.2f", gap);
		Component message =
			new TranslatableComponent("railward.train_coupler.auto.distance", distance).withStyle(ChatFormatting.YELLOW);
		for (ServerPlayer player : players) {
			autoDebug(debugAttempt, "auto prompt sent: player={} train={} gapMeters={}", player.getGameProfile()
				.getName(), debugTrain(movingTrain), distance);
			player.displayClientMessage(message, true);
		}
	}

	private static void sendAutoCoupledMessage(int debugAttempt, Set<ServerPlayer> players, String movingTrainName,
		String attachedTrainName) {
		if (players.isEmpty()) {
			autoDebug(debugAttempt, "auto success message skipped: no server players were collected");
			return;
		}

		Component message =
			new TranslatableComponent("railward.train_coupler.auto.coupled", movingTrainName, attachedTrainName)
				.withStyle(ChatFormatting.GREEN);
		for (ServerPlayer player : players) {
			autoDebug(debugAttempt, "auto success message sent: player={} movingTrainName={} attachedTrainName={}",
				player.getGameProfile()
					.getName(),
				movingTrainName, attachedTrainName);
			AllSoundEvents.CONFIRM.playOnServer(player.level, player.blockPosition(), 1, 1);
			player.displayClientMessage(message, true);
		}
	}

	private static void suppressAutoCoupling(CouplerRef first, CouplerRef second) {
		if (first.endpoint() == null || second.endpoint() == null)
			return;

		SuppressedCouplerPair pair = new SuppressedCouplerPair(first.endpoint(), second.endpoint());
		AUTO_SUPPRESSED_COUPLERS.removeIf(pair::matches);
		AUTO_SUPPRESSED_COUPLERS.add(pair);
	}

	private static boolean isAutoCouplingSuppressed(CouplerRef first, CouplerRef second) {
		if (first.endpoint() == null || second.endpoint() == null)
			return false;

		for (SuppressedCouplerPair pair : AUTO_SUPPRESSED_COUPLERS)
			if (pair.matches(first.endpoint(), second.endpoint()))
				return true;
		return false;
	}

	private static void cleanupSuppressedAutoCouplers(ServerLevel level) {
		Iterator<SuppressedCouplerPair> iterator = AUTO_SUPPRESSED_COUPLERS.iterator();
		while (iterator.hasNext()) {
			SuppressedCouplerPair pair = iterator.next();
			Vec3 first = resolveSuppressedCouplerCenter(level, pair.first());
			Vec3 second = resolveSuppressedCouplerCenter(level, pair.second());
			if (first == null || second == null || first.distanceToSqr(second) > MAX_COUPLING_DISTANCE_SQ)
				iterator.remove();
		}
	}

	private static Vec3 resolveSuppressedCouplerCenter(ServerLevel level, CouplerEndpoint endpoint) {
		Entity entity = level.getEntity(endpoint.entityId());
		if (!(entity instanceof CarriageContraptionEntity carriageEntity))
			return null;

		StructureBlockInfo info = carriageEntity.getContraption()
			.getBlocks()
			.get(endpoint.localPos());
		if (info == null || !AllBlocks.TRAIN_COUPLER.has(info.state))
			return null;

		return carriageEntity.toGlobalVector(VecHelper.getCenterOf(endpoint.localPos()), 1);
	}

	public static Result tryDecouple(Level level, CarriageContraptionEntity carriageEntity, BlockPos localPos) {
		int debugAttempt = DEBUG_ATTEMPT_COUNTER.incrementAndGet();
		debug(debugAttempt, "interaction started: level={} entity={} localPos={}", level.dimension().location(),
			carriageEntity.getUUID(), localPos);

		StructureBlockInfo clickedInfo = carriageEntity.getContraption()
			.getBlocks()
			.get(localPos);
		if (clickedInfo == null || !AllBlocks.TRAIN_COUPLER.has(clickedInfo.state)) {
			debug(debugAttempt, "clicked block is not a coupler: info={}", clickedInfo);
			return Result.fail("railward.train_coupler.error.not_coupler");
		}

		Carriage carriage = carriageEntity.getCarriage();
		if (carriage == null || carriage.train == null) {
			debug(debugAttempt, "clicked carriage is not attached: carriage={}", carriage);
			return Result.fail("railward.train_coupler.error.not_attached");
		}

		Train train = carriage.train;
		int carriageIndex = train.carriages.indexOf(carriage);
		if (carriageIndex < 0) {
			debug(debugAttempt, "clicked carriage is missing from train: train={}", debugTrain(train));
			return Result.fail("railward.train_coupler.error.carriage_index");
		}

		debug(debugAttempt, "clicked train: {} clickedCarriage={} localPos={} state={} center={}", debugTrain(train),
			carriageIndex, localPos, debugState(clickedInfo.state),
			debugVec(carriageEntity.toGlobalVector(VecHelper.getCenterOf(localPos), 1)));
		debugTrainGeometry(debugAttempt, level, train);

		Result commonValidation = validateTrainReady(train);
		if (!commonValidation.success()) {
			debug(debugAttempt, "clicked train rejected by readiness validation: {}", commonValidation.message());
			return commonValidation;
		}

		CouplerEnd clickedEnd = resolveCouplerEnd(level, carriageEntity, carriage, localPos, clickedInfo.state);
		if (clickedEnd == null) {
			debug(debugAttempt, "clicked coupler end could not be resolved");
			return Result.fail("railward.train_coupler.error.end_unknown");
		}

		CouplerRef clickedCoupler = CouplerRef.from(carriageEntity, localPos, clickedInfo.state);
		if (clickedCoupler == null) {
			debug(debugAttempt, "clicked coupler reference could not be resolved");
			return Result.fail("railward.train_coupler.error.orientation_unknown");
		}
		debug(debugAttempt, "clicked coupler resolved: end={} center={}", clickedEnd, debugVec(clickedCoupler.center()));

		int splitIndex = clickedEnd == CouplerEnd.TRAILING ? carriageIndex + 1 : carriageIndex;
		int otherCarriageIndex = clickedEnd == CouplerEnd.TRAILING ? carriageIndex + 1 : carriageIndex - 1;
		if (otherCarriageIndex < 0 || otherCarriageIndex >= train.carriages.size()) {
			debug(debugAttempt, "clicked coupler is on outer train end, trying to couple with another train");
			return tryCouple(debugAttempt, level, train, carriageIndex, clickedEnd, clickedCoupler);
		}

		Carriage otherCarriage = train.carriages.get(otherCarriageIndex);
		if (!isStraightHorizontal(carriage) || !isStraightHorizontal(otherCarriage)) {
			debug(debugAttempt, "decouple rejected: adjacent carriages are not straight horizontal");
			return Result.fail("railward.train_coupler.error.carriages_straight");
		}

		CouplerRef otherCoupler =
			findPairedCoupler(debugAttempt, level, otherCarriage, clickedCoupler, clickedEnd.opposite());
		if (otherCoupler == null) {
			debug(debugAttempt, "decouple rejected: paired coupler not found");
			return Result.fail("railward.train_coupler.error.adjacent_missing");
		}
		suppressAutoCoupling(clickedCoupler, otherCoupler);

		Train newTrain = splitTrain(level, train, splitIndex);
		if (newTrain == null) {
			debug(debugAttempt, "decouple failed while splitting train");
			return Result.fail("railward.train_coupler.error.split_failed");
		}

		debug(debugAttempt, "decouple succeeded: original={} new={}", debugTrain(train), debugTrain(newTrain));
		return Result.success("railward.train_coupler.success.decoupled");
	}

	private static Result validateTrainReady(Train train) {
		if (train.graph == null || train.derailed)
			return Result.fail("railward.train_coupler.error.train_invalid");
		if (!isStopped(train))
			return Result.fail("railward.train_coupler.error.train_moving");
		if (train.runtime.getSchedule() != null)
			return Result.fail("railward.train_coupler.error.scheduled");
		if (!train.canDisassemble())
			return Result.fail("railward.train_coupler.error.not_aligned");
		return Result.success("");
	}

	private static Result tryCouple(int debugAttempt, Level level, Train clickedTrain, int clickedCarriageIndex,
		CouplerEnd clickedEnd, CouplerRef clickedCoupler) {
		debug(debugAttempt, "couple start: clickedTrain={} clickedCarriage={} clickedEnd={} clickedCenter={}",
			debugTrain(clickedTrain), clickedCarriageIndex, clickedEnd, debugVec(clickedCoupler.center()));

		if (clickedCarriageIndex != 0 && clickedCarriageIndex != clickedTrain.carriages.size() - 1) {
			debug(debugAttempt, "couple rejected: clicked carriage is not first or last in clicked train");
			return Result.fail("railward.train_coupler.error.outer_end");
		}
		if (!areAllCarriagesStraightHorizontal(clickedTrain)) {
			debug(debugAttempt, "couple rejected: clicked train is not fully straight horizontal");
			return Result.fail("railward.train_coupler.error.train_straight");
		}

		CouplingCandidate candidate =
			findCouplingCandidate(debugAttempt, level, clickedTrain, clickedCarriageIndex, clickedCoupler);
		if (candidate == null) {
			debug(debugAttempt, "couple rejected: no matching coupler candidate found");
			return Result.fail("railward.train_coupler.error.other_missing");
		}

		return completeCoupling(debugAttempt, level, clickedTrain, clickedCarriageIndex, clickedEnd, clickedCoupler,
			candidate, null, false);
	}

	private static Result completeCoupling(int debugAttempt, Level level, Train clickedTrain, int clickedCarriageIndex,
		CouplerEnd clickedEnd, CouplerRef clickedCoupler, CouplingCandidate candidate,
		Train trainToStopBeforeCombine, boolean forceClickedTrainName) {
		Train otherTrain = candidate.train();
		debug(debugAttempt, "selected candidate: train={} carriage={} end={} center={} distanceSq={}",
			debugTrain(otherTrain), candidate.carriageIndex(), candidate.end(), debugVec(candidate.coupler()
				.center()), debugDistance(candidate.distanceSq()));
		debugTrainGeometry(debugAttempt, level, otherTrain);

		if (candidate.distanceSq() > MAX_COUPLING_DISTANCE_SQ) {
			debug(debugAttempt, "couple rejected: selected coupler is too far distanceSq={} maxDistanceSq={}",
				debugDistance(candidate.distanceSq()), debugDistance(MAX_COUPLING_DISTANCE_SQ));
			return Result.fail("railward.train_coupler.error.too_far");
		}
		if (candidate.distanceSq() < MIN_COUPLING_DISTANCE_SQ) {
			debug(debugAttempt, "couple rejected: selected coupler is too close distanceSq={} minDistanceSq={}",
				debugDistance(candidate.distanceSq()), debugDistance(MIN_COUPLING_DISTANCE_SQ));
			return Result.fail("railward.train_coupler.error.too_close");
		}

		if (clickedTrain.graph != otherTrain.graph) {
			debug(debugAttempt, "couple rejected: selected train is on a different graph");
			return Result.fail("railward.train_coupler.error.same_graph");
		}
		if (!areAllCarriagesStraightHorizontal(otherTrain)) {
			debug(debugAttempt, "couple rejected: selected train is not fully straight horizontal");
			return Result.fail("railward.train_coupler.error.other_straight");
		}

		CouplerEnd effectiveClickedEnd =
			resolveCouplingEndByRelativePosition(level, clickedTrain, clickedCarriageIndex, clickedCoupler,
				candidate.coupler());
		debug(debugAttempt, "effective clicked end by relative position: {}", effectiveClickedEnd);
		if (effectiveClickedEnd == null)
			effectiveClickedEnd = clickedEnd;
		if (!isOuterEnd(clickedTrain, clickedCarriageIndex, effectiveClickedEnd)) {
			debug(debugAttempt, "couple rejected: effective clicked end is not an outer end: {}", effectiveClickedEnd);
			return Result.fail("railward.train_coupler.error.outer_end");
		}

		CouplerEnd effectiveCandidateEnd =
			resolveCouplingEndByRelativePosition(level, otherTrain, candidate.carriageIndex(), candidate.coupler(),
				clickedCoupler);
		debug(debugAttempt, "effective candidate end by relative position: {}", effectiveCandidateEnd);
		if (effectiveCandidateEnd == null)
			effectiveCandidateEnd = candidate.end();
		if (!isOuterEnd(otherTrain, candidate.carriageIndex(), effectiveCandidateEnd)) {
			debug(debugAttempt, "couple rejected: effective candidate end is not an outer end: {}",
				effectiveCandidateEnd);
			return Result.fail("railward.train_coupler.error.other_missing");
		}
		boolean reverseOtherTrain = false;
		if (effectiveCandidateEnd != effectiveClickedEnd.opposite()) {
			if (effectiveCandidateEnd != effectiveClickedEnd) {
				debug(debugAttempt, "couple rejected: incompatible ends clicked={} candidate={} expectedCandidate={}",
					effectiveClickedEnd, effectiveCandidateEnd, effectiveClickedEnd.opposite());
				return Result.fail("railward.train_coupler.error.incompatible_ends");
			}
			reverseOtherTrain = true;
		}

		List<RebaseInstruction> rebases = List.of();
		if (reverseOtherTrain) {
			debug(debugAttempt, "same-end coupling detected, virtually reversing candidate train: train={} end={}",
				debugTrain(otherTrain), effectiveCandidateEnd);
			rebases = reverseTrainInPlace(debugAttempt, level, otherTrain);
			if (rebases == null) {
				debug(debugAttempt, "couple rejected: candidate train could not be virtually reversed");
				return Result.fail("railward.train_coupler.error.couple_failed");
			}
			effectiveCandidateEnd = effectiveCandidateEnd.opposite();
			debug(debugAttempt, "candidate train reversed: newCandidateEnd={} rebases={}", effectiveCandidateEnd,
				rebases.size());
		}

		Train frontTrain;
		Train backTrain;
		if (effectiveClickedEnd == CouplerEnd.TRAILING && effectiveCandidateEnd == CouplerEnd.LEADING) {
			frontTrain = clickedTrain;
			backTrain = otherTrain;
		} else if (effectiveCandidateEnd == CouplerEnd.TRAILING && effectiveClickedEnd == CouplerEnd.LEADING) {
			frontTrain = otherTrain;
			backTrain = clickedTrain;
		} else {
			debug(debugAttempt, "couple rejected after reverse resolution: clicked={} candidate={}", effectiveClickedEnd,
				effectiveCandidateEnd);
			if (reverseOtherTrain)
				reverseTrainInPlace(debugAttempt, level, otherTrain);
			return Result.fail("railward.train_coupler.error.incompatible_ends");
		}
		int spacing = determineCoupledSpacing(debugAttempt, level, frontTrain, backTrain);
		debug(debugAttempt, "couple order resolved: front={} back={} spacing={}", debugTrain(frontTrain),
			debugTrain(backTrain), spacing);
		if (spacing <= 0) {
			debug(debugAttempt, "couple rejected: spacing could not be determined");
			if (reverseOtherTrain)
				reverseTrainInPlace(debugAttempt, level, otherTrain);
			return Result.fail("railward.train_coupler.error.spacing_failed");
		}

		if (trainToStopBeforeCombine != null)
			stopTrain(trainToStopBeforeCombine);

		Train combinedTrain =
			combineTrains(level, frontTrain, backTrain, spacing, clickedTrain, rebases, forceClickedTrainName);
		if (combinedTrain == null) {
			debug(debugAttempt, "couple failed while combining trains");
			if (reverseOtherTrain)
				reverseTrainInPlace(debugAttempt, level, otherTrain);
			return Result.fail("railward.train_coupler.error.couple_failed");
		}

		debug(debugAttempt, "couple succeeded: combined={}", debugTrain(combinedTrain));
		return Result.success("railward.train_coupler.success.coupled");
	}

	private static boolean isStopped(Train train) {
		return Math.abs(train.speed) <= STOPPED_EPSILON && Math.abs(train.targetSpeed) <= STOPPED_EPSILON
			&& train.speedBeforeStall == null;
	}

	private static void stopTrain(Train train) {
		train.speed = 0;
		train.targetSpeed = 0;
		train.speedBeforeStall = null;
	}

	private static boolean isOuterEnd(Train train, int carriageIndex, CouplerEnd end) {
		return end == CouplerEnd.LEADING ? carriageIndex == 0 : carriageIndex == train.carriages.size() - 1;
	}

	private static CouplerEnd resolveCouplerEnd(Level level, CarriageContraptionEntity entity, Carriage carriage,
		BlockPos localPos, BlockState state) {
		Vec3 center = entity.toGlobalVector(VecHelper.getCenterOf(localPos), 1);
		if (!carriage.isOnTwoBogeys()) {
			CouplerEnd singleBogeyEnd = resolveSingleBogeyCouplerEnd(carriage, center);
			if (singleBogeyEnd != null)
				return singleBogeyEnd;
		}

		Carriage.DimensionalCarriageEntity dimensional = carriage.getDimensionalIfPresent(level.dimension());
		if (dimensional == null || dimensional.leadingAnchor() == null || dimensional.trailingAnchor() == null)
			return resolveCouplerEndFromLocal(entity, localPos, state);

		double leadingDistance = center.distanceToSqr(dimensional.leadingAnchor());
		double trailingDistance = center.distanceToSqr(dimensional.trailingAnchor());

		if (Math.abs(leadingDistance - trailingDistance) <= SIDE_EPSILON)
			return resolveCouplerEndFromLocal(entity, localPos, state);
		return leadingDistance < trailingDistance ? CouplerEnd.LEADING : CouplerEnd.TRAILING;
	}

	private static CouplerEnd resolveCouplingEndByRelativePosition(Level level, Train train, int carriageIndex,
		CouplerRef ownCoupler, CouplerRef otherCoupler) {
		if (carriageIndex < 0 || carriageIndex >= train.carriages.size())
			return null;

		Vec3 axis = getCarriageAxis(level, train.carriages.get(carriageIndex));
		if (axis == null)
			return null;

		Vec3 offset = otherCoupler.center()
			.subtract(ownCoupler.center());
		if (offset.lengthSqr() <= SIDE_EPSILON)
			return null;

		double dot = offset.normalize()
			.dot(axis);
		if (Math.abs(dot) < MIN_AXIS_DOT)
			return null;
		if (Math.abs(dot) <= SIDE_EPSILON)
			return null;
		return dot > 0 ? CouplerEnd.TRAILING : CouplerEnd.LEADING;
	}

	private static Vec3 getCarriageAxis(Level level, Carriage carriage) {
		Carriage.DimensionalCarriageEntity dimensional = carriage.getDimensionalIfPresent(level.dimension());
		if (dimensional != null && dimensional.leadingAnchor() != null && dimensional.trailingAnchor() != null) {
			Vec3 axis = dimensional.trailingAnchor()
				.subtract(dimensional.leadingAnchor());
			if (axis.lengthSqr() > SIDE_EPSILON)
				return axis.normalize();
		}

		if (carriage.train == null || carriage.train.graph == null)
			return null;
		if (carriage.getLeadingPoint()
			.edge == null
			|| carriage.getTrailingPoint()
				.edge == null)
			return null;

		Vec3 axis = carriage.getTrailingPoint()
			.getPosition(carriage.train.graph)
			.subtract(carriage.getLeadingPoint()
				.getPosition(carriage.train.graph));
		if (axis.lengthSqr() <= SIDE_EPSILON)
			return null;
		return axis.normalize();
	}

	private static CouplerEnd resolveSingleBogeyCouplerEnd(Carriage carriage, Vec3 center) {
		if (carriage.train == null || carriage.train.graph == null)
			return null;
		if (carriage.getLeadingPoint()
			.edge == null
			|| carriage.getTrailingPoint()
				.edge == null)
			return null;

		Vec3 leadingPoint = carriage.getLeadingPoint()
			.getPosition(carriage.train.graph);
		Vec3 trailingPoint = carriage.getTrailingPoint()
			.getPosition(carriage.train.graph);
		double leadingDistance = center.distanceToSqr(leadingPoint);
		double trailingDistance = center.distanceToSqr(trailingPoint);
		if (Math.abs(leadingDistance - trailingDistance) <= SIDE_EPSILON)
			return null;

		return leadingDistance < trailingDistance ? CouplerEnd.LEADING : CouplerEnd.TRAILING;
	}

	private static CouplerEnd resolveCouplerEndFromLocal(CarriageContraptionEntity entity, BlockPos localPos,
		BlockState state) {
		if (!(entity.getContraption() instanceof CarriageContraption contraption))
			return null;

		Direction assemblyDirection = contraption.getAssemblyDirection();
		int coord = getCoordinateAlong(localPos, assemblyDirection);
		if (coord < 0)
			return CouplerEnd.LEADING;
		if (coord > 0)
			return CouplerEnd.TRAILING;

		if (!state.hasProperty(TrainCouplerBlock.FACING))
			return null;

		Direction facing = state.getValue(TrainCouplerBlock.FACING);
		if (facing.getAxis() != assemblyDirection.getAxis())
			return null;
		return facing == assemblyDirection ? CouplerEnd.TRAILING : CouplerEnd.LEADING;
	}

	private static int getCoordinateAlong(BlockPos pos, Direction direction) {
		return direction.getAxis()
			.choose(pos.getX(), pos.getY(), pos.getZ()) * direction.getAxisDirection()
				.getStep();
	}

	private static boolean isStraightHorizontal(Carriage carriage) {
		if (!isStraightHorizontal(carriage.leadingBogey()
			.leading()) || !isStraightHorizontal(carriage.leadingBogey()
				.trailing()))
			return false;
		if (!carriage.isOnTwoBogeys())
			return true;
		return isStraightHorizontal(carriage.trailingBogey()
			.leading()) && isStraightHorizontal(carriage.trailingBogey()
				.trailing());
	}

	private static boolean areAllCarriagesStraightHorizontal(Train train) {
		for (Carriage carriage : train.carriages)
			if (!isStraightHorizontal(carriage))
				return false;
		return true;
	}

	private static boolean isStraightHorizontal(TravellingPoint point) {
		TrackEdge edge = point.edge;
		if (edge == null || edge.isTurn() || edge.isInterDimensional())
			return false;

		Vec3 first = point.node1.getLocation()
			.getLocation();
		Vec3 second = point.node2.getLocation()
			.getLocation();
		return Mth.equal(first.y, second.y);
	}

	private static CouplerRef findPairedCoupler(int debugAttempt, Level level, Carriage otherCarriage,
		CouplerRef clickedCoupler, CouplerEnd expectedEnd) {
		CarriageContraptionEntity otherEntity = otherCarriage.anyAvailableEntity();
		if (otherEntity == null || otherEntity.level != level) {
			debug(debugAttempt, "paired search skipped: adjacent carriage has no entity in this level");
			return null;
		}

		Contraption contraption = otherEntity.getContraption();
		if (!(contraption instanceof CarriageContraption)) {
			debug(debugAttempt, "paired search skipped: adjacent entity is not a carriage contraption");
			return null;
		}

		CouplerRef best = null;
		double bestDistance = MAX_COUPLER_DISTANCE_SQ;

		for (StructureBlockInfo info : contraption.getBlocks()
			.values()) {
			if (!AllBlocks.TRAIN_COUPLER.has(info.state))
				continue;

			CouplerEnd end = resolveCouplerEnd(level, otherEntity, otherCarriage, info.pos, info.state);
			if (end != expectedEnd) {
				debug(debugAttempt, "paired candidate rejected: localPos={} state={} end={} expectedEnd={}", info.pos,
					debugState(info.state), end, expectedEnd);
				continue;
			}

			CouplerRef candidate = CouplerRef.from(otherEntity, info.pos, info.state);
			if (candidate == null)
				continue;

			double distance = clickedCoupler.center()
				.distanceToSqr(candidate.center());
			debug(debugAttempt, "paired candidate: localPos={} state={} center={} end={} distanceSq={}", info.pos,
				debugState(info.state), debugVec(candidate.center()), end, debugDistance(distance));
			if (distance >= bestDistance)
				continue;

			bestDistance = distance;
			best = candidate;
		}

		debug(debugAttempt, "paired search result: center={} distanceSq={}", best == null ? null : debugVec(best.center()),
			best == null ? null : debugDistance(bestDistance));
		return best;
	}

	private static CouplingCandidate findCouplingCandidate(int debugAttempt, Level level, Train clickedTrain,
		int clickedCarriageIndex, CouplerRef clickedCoupler) {
		CouplingCandidate best = null;
		double bestDistance = MAX_COUPLER_DISTANCE_SQ;

		debug(debugAttempt, "candidate search started: trainsInLevel={} maxDistanceSq={}",
			Create.RAILWAYS.sided(level).trains.size(), debugDistance(MAX_COUPLER_DISTANCE_SQ));
		for (Train candidateTrain : Create.RAILWAYS.sided(level).trains.values()) {
			if (candidateTrain == clickedTrain) {
				debug(debugAttempt, "candidate train skipped: same train {}", debugTrain(candidateTrain));
				continue;
			}

			Result validation = validateTrainReady(candidateTrain);
			if (!validation.success()) {
				debug(debugAttempt, "candidate train skipped: {} validation={}", debugTrain(candidateTrain),
					validation.message());
				continue;
			}
			if (candidateTrain.graph != clickedTrain.graph) {
				debug(debugAttempt, "candidate train skipped: {} different graph", debugTrain(candidateTrain));
				continue;
			}

			int lastCarriageIndex = candidateTrain.carriages.size() - 1;
			int[] carriageIndices = lastCarriageIndex == 0 ? new int[] { 0 } : new int[] { 0, lastCarriageIndex };
			for (int carriageIndex : carriageIndices) {
				if (carriageIndex < 0 || carriageIndex >= candidateTrain.carriages.size())
					continue;

				Carriage carriage = candidateTrain.carriages.get(carriageIndex);
				CarriageContraptionEntity entity = carriage.anyAvailableEntity();
				if (entity == null || entity.level != level) {
					debug(debugAttempt, "candidate carriage skipped: train={} carriage={} has no entity in this level",
						debugTrain(candidateTrain), carriageIndex);
					continue;
				}

				Contraption contraption = entity.getContraption();
				if (!(contraption instanceof CarriageContraption)) {
					debug(debugAttempt, "candidate carriage skipped: train={} carriage={} is not carriage contraption",
						debugTrain(candidateTrain), carriageIndex);
					continue;
				}

				for (StructureBlockInfo info : contraption.getBlocks()
					.values()) {
					if (!AllBlocks.TRAIN_COUPLER.has(info.state))
						continue;

					CouplerRef candidateCoupler = CouplerRef.from(entity, info.pos, info.state);
					if (candidateCoupler == null)
						continue;

					CouplerEnd end = resolveCouplingEndByRelativePosition(level, candidateTrain, carriageIndex,
						candidateCoupler, clickedCoupler);
					CouplerEnd relativeEnd = end;
					if (end == null)
						end = resolveCouplerEnd(level, entity, carriage, info.pos, info.state);
					CouplerEnd clickedRelativeEnd =
						resolveCouplingEndByRelativePosition(level, clickedTrain, clickedCarriageIndex, clickedCoupler,
							candidateCoupler);
					double distance = clickedCoupler.center()
						.distanceToSqr(candidateCoupler.center());
					boolean candidateOuter = end != null && isOuterEnd(candidateTrain, carriageIndex, end);
					boolean clickedOuter =
						clickedRelativeEnd != null && isOuterEnd(clickedTrain, clickedCarriageIndex, clickedRelativeEnd);
					boolean compatible =
						clickedRelativeEnd != null && end != null && end == clickedRelativeEnd.opposite();

					debug(debugAttempt,
						"candidate coupler: train={} carriage={} localPos={} state={} center={} distanceSq={} relativeEnd={} fallbackEnd={} clickedRelativeEnd={} candidateOuter={} clickedOuter={} compatible={}",
						debugTrain(candidateTrain), carriageIndex, info.pos, debugState(info.state),
						debugVec(candidateCoupler.center()), debugDistance(distance), relativeEnd, end, clickedRelativeEnd,
						candidateOuter, clickedOuter, compatible);

					if (end == null) {
						debug(debugAttempt, "candidate coupler rejected: end unknown");
						continue;
					}
					if (!candidateOuter) {
						debug(debugAttempt, "candidate coupler rejected: not an outer end");
						continue;
					}
					if (distance >= bestDistance) {
						debug(debugAttempt, "candidate coupler rejected: distanceSq={} currentBestDistanceSq={}",
							debugDistance(distance), debugDistance(bestDistance));
						continue;
					}

					bestDistance = distance;
					best = new CouplingCandidate(candidateTrain, carriageIndex, end, candidateCoupler, distance);
					debug(debugAttempt, "candidate coupler is new best: train={} carriage={} end={} distanceSq={}",
						debugTrain(candidateTrain), carriageIndex, end, debugDistance(bestDistance));
				}
			}
		}

		debug(debugAttempt, "candidate search result: train={} carriage={} end={} distanceSq={}",
			best == null ? null : debugTrain(best.train()), best == null ? null : best.carriageIndex(),
			best == null ? null : best.end(), best == null ? null : debugDistance(bestDistance));
		return best;
	}

	private static int determineCoupledSpacing(int debugAttempt, Level level, Train frontTrain, Train backTrain) {
		Carriage frontCarriage = frontTrain.carriages.get(frontTrain.carriages.size() - 1);
		Carriage backCarriage = backTrain.carriages.get(0);
		Carriage.DimensionalCarriageEntity frontDimensional = frontCarriage.getDimensionalIfPresent(level.dimension());
		Carriage.DimensionalCarriageEntity backDimensional = backCarriage.getDimensionalIfPresent(level.dimension());
		if (frontDimensional == null || backDimensional == null || frontDimensional.trailingAnchor() == null
			|| backDimensional.leadingAnchor() == null) {
			debug(debugAttempt,
				"spacing failed: missing anchors frontDimensional={} backDimensional={} frontTrailing={} backLeading={}",
				frontDimensional, backDimensional,
				frontDimensional == null ? null : debugVec(frontDimensional.trailingAnchor()),
				backDimensional == null ? null : debugVec(backDimensional.leadingAnchor()));
			return -1;
		}

		double distance = frontDimensional.trailingAnchor()
			.distanceTo(backDimensional.leadingAnchor());
		debug(debugAttempt, "spacing anchors: frontTrailing={} backLeading={} distance={}",
			debugVec(frontDimensional.trailingAnchor()), debugVec(backDimensional.leadingAnchor()), debugDistance(distance));
		return Math.max(1, Mth.floor(distance + 0.5));
	}

	private static Train splitTrain(Level level, Train train, int splitIndex) {
		if (splitIndex <= 0 || splitIndex >= train.carriages.size())
			return null;

		List<Carriage> originalCarriages = new ArrayList<>(train.carriages);
		List<Integer> originalSpacing = new ArrayList<>(train.carriageSpacing);
		double[] originalStress = ((TrainAccessor) train).railward$getStress();
		Train newTrain = null;
		boolean addedTrain = false;

		List<Carriage> movedCarriages = new ArrayList<>(train.carriages.subList(splitIndex, train.carriages.size()));
		List<Integer> movedSpacing = new ArrayList<>();
		for (int i = splitIndex; i < train.carriageSpacing.size(); i++)
			movedSpacing.add(train.carriageSpacing.get(i));

		try {
			while (train.carriages.size() > splitIndex)
				train.carriages.remove(train.carriages.size() - 1);
			while (train.carriageSpacing.size() > splitIndex - 1)
				train.carriageSpacing.remove(train.carriageSpacing.size() - 1);

			((TrainAccessor) train).railward$setStress(copyStress(originalStress, train.carriageSpacing.size()));

			newTrain = new Train(UUID.randomUUID(), train.owner, train.graph, movedCarriages, movedSpacing,
				hasBackwardControls(movedCarriages));
			train.doubleEnded = hasBackwardControls(train.carriages);

			rebindCarriages(train);
			rebindCarriages(newTrain);
			assignSplitNames(train, newTrain, new TextComponent(train.name.getString()));

			train.collectInitiallyOccupiedSignalBlocks();
			newTrain.collectInitiallyOccupiedSignalBlocks();
			train.updateSignalBlocks = true;
			newTrain.updateSignalBlocks = true;

			Create.RAILWAYS.addTrain(newTrain);
			addedTrain = true;
			syncTrains(train, newTrain);
			return newTrain;
		} catch (RuntimeException e) {
			if (addedTrain && newTrain != null)
				Create.RAILWAYS.removeTrain(newTrain.id);
			train.carriages.clear();
			train.carriages.addAll(originalCarriages);
			train.carriageSpacing.clear();
			train.carriageSpacing.addAll(originalSpacing);
			((TrainAccessor) train).railward$setStress(originalStress);
			rebindCarriages(train);
			return null;
		}
	}

	private static Train combineTrains(Level level, Train frontTrain, Train backTrain, int carriageSpacing,
		Train clickedTrain, Collection<RebaseInstruction> rebases, boolean forceClickedTrainName) {
		if (frontTrain == backTrain)
			return null;

		List<Carriage> frontCarriagesBackup = new ArrayList<>(frontTrain.carriages);
		List<Integer> frontSpacingBackup = new ArrayList<>(frontTrain.carriageSpacing);
		double[] frontStressBackup = ((TrainAccessor) frontTrain).railward$getStress();
		boolean frontDoubleEndedBackup = frontTrain.doubleEnded;
		TextComponent frontNameBackup = new TextComponent(frontTrain.name.getString());

		List<Carriage> backCarriagesBackup = new ArrayList<>(backTrain.carriages);
		List<Integer> backSpacingBackup = new ArrayList<>(backTrain.carriageSpacing);
		double[] backStressBackup = ((TrainAccessor) backTrain).railward$getStress();
		boolean backDoubleEndedBackup = backTrain.doubleEnded;
		TextComponent backNameBackup = new TextComponent(backTrain.name.getString());

		boolean removedBackTrain = false;
		try {
			TextComponent combinedName = forceClickedTrainName ? new TextComponent(clickedTrain.name.getString())
				: resolveCoupledName(clickedTrain, frontTrain, backTrain);
			double[] newStress = new double[frontStressBackup.length + backStressBackup.length + 1];
			System.arraycopy(frontStressBackup, 0, newStress, 0, frontStressBackup.length);
			System.arraycopy(backStressBackup, 0, newStress, frontStressBackup.length + 1, backStressBackup.length);

			frontTrain.carriages.addAll(backTrain.carriages);
			backTrain.carriages.clear();
			frontTrain.carriageSpacing.add(carriageSpacing);
			frontTrain.carriageSpacing.addAll(backTrain.carriageSpacing);
			backTrain.carriageSpacing.clear();

			((TrainAccessor) frontTrain).railward$setStress(newStress);
			frontTrain.doubleEnded = hasBackwardControls(frontTrain.carriages);
			frontTrain.name = combinedName;

			rebindCarriages(frontTrain);
			frontTrain.collectInitiallyOccupiedSignalBlocks();
			frontTrain.updateSignalBlocks = true;

			if (backTrain.getCurrentStation() != null)
				backTrain.getCurrentStation()
					.cancelReservation(backTrain);
			Create.RAILWAYS.removeTrain(backTrain.id);
			removedBackTrain = true;

			syncMergedTrain(frontTrain, backTrain.id, rebases);
			return frontTrain;
		} catch (RuntimeException e) {
			frontTrain.carriages.clear();
			frontTrain.carriages.addAll(frontCarriagesBackup);
			frontTrain.carriageSpacing.clear();
			frontTrain.carriageSpacing.addAll(frontSpacingBackup);
			((TrainAccessor) frontTrain).railward$setStress(frontStressBackup);
			frontTrain.doubleEnded = frontDoubleEndedBackup;
			frontTrain.name = frontNameBackup;

			backTrain.carriages.clear();
			backTrain.carriages.addAll(backCarriagesBackup);
			backTrain.carriageSpacing.clear();
			backTrain.carriageSpacing.addAll(backSpacingBackup);
			((TrainAccessor) backTrain).railward$setStress(backStressBackup);
			backTrain.doubleEnded = backDoubleEndedBackup;
			backTrain.name = backNameBackup;
			if (removedBackTrain)
				Create.RAILWAYS.addTrain(backTrain);

			rebindCarriages(frontTrain);
			rebindCarriages(backTrain);
			return null;
		}
	}

	private static List<RebaseInstruction> reverseTrainInPlace(int debugAttempt, Level level, Train train) {
		if (!areAllCarriagesLoadedInLevel(level, train)) {
			debug(debugAttempt, "virtual reverse rejected: train is not fully loaded in this level: {}",
				debugTrain(train));
			return null;
		}

		List<Carriage> originalCarriages = new ArrayList<>(train.carriages);
		List<Integer> originalSpacing = new ArrayList<>(train.carriageSpacing);
		double[] originalStress = ((TrainAccessor) train).railward$getStress();
		List<Carriage> reversedCarriages = new ArrayList<>();
		List<RebaseInstruction> rebases = new ArrayList<>();

		try {
			for (Carriage carriage : originalCarriages) {
				reverseCarriageInPlace(debugAttempt, level, carriage, rebases);
				reversedCarriages.add(carriage);
			}

			Collections.reverse(train.carriages);
			Collections.reverse(train.carriageSpacing);
			((TrainAccessor) train).railward$setStress(reverseCopy(originalStress));
			train.doubleEnded = hasBackwardControls(train.carriages);
			rebindCarriages(train);
			train.collectInitiallyOccupiedSignalBlocks();
			train.updateSignalBlocks = true;

			debug(debugAttempt, "virtual reverse succeeded: train={} rebases={}", debugTrain(train), rebases.size());
			return rebases;
		} catch (RuntimeException e) {
			debug(debugAttempt, "virtual reverse failed: train={} error={}", debugTrain(train), e.toString());
			for (int i = reversedCarriages.size() - 1; i >= 0; i--) {
				try {
					reverseCarriageInPlace(debugAttempt, level, reversedCarriages.get(i), new ArrayList<>());
				} catch (RuntimeException ignored) {
					debug(debugAttempt, "virtual reverse rollback failed for carriage {}", i);
				}
			}

			train.carriages.clear();
			train.carriages.addAll(originalCarriages);
			train.carriageSpacing.clear();
			train.carriageSpacing.addAll(originalSpacing);
			((TrainAccessor) train).railward$setStress(originalStress);
			rebindCarriages(train);
			return null;
		}
	}

	private static boolean areAllCarriagesLoadedInLevel(Level level, Train train) {
		for (Carriage carriage : train.carriages) {
			CarriageContraptionEntity entity = carriage.anyAvailableEntity();
			if (entity == null || entity.level != level)
				return false;
			if (!(entity.getContraption() instanceof CarriageContraption))
				return false;
		}
		return true;
	}

	private static void reverseCarriageInPlace(int debugAttempt, Level level, Carriage carriage,
		List<RebaseInstruction> rebases) {
		if (carriage.train == null || carriage.train.graph == null)
			throw new IllegalStateException("Carriage has no train graph");

		boolean twoBogeys = carriage.isOnTwoBogeys();
		CarriageBogey newLeadingBogey = reversedBogey(carriage.trailingBogey(), carriage.train.graph);
		CarriageBogey newTrailingBogey = twoBogeys ? reversedBogey(carriage.leadingBogey(), carriage.train.graph) : null;

		newLeadingBogey.carriage = carriage;
		if (newTrailingBogey != null)
			newTrailingBogey.carriage = carriage;

		List<RebaseInstruction> carriageRebases = new ArrayList<>();
		carriage.forEachPresentEntity(entity -> {
			if (entity.level != level)
				return;
			if (!(entity.getContraption() instanceof CarriageContraption contraption))
				throw new IllegalStateException("Carriage entity has no carriage contraption");

			Direction oldAssemblyDirection = contraption.getAssemblyDirection();
			Direction newAssemblyDirection = oldAssemblyDirection.getOpposite();
			BlockPos offset = twoBogeys ? scale(oldAssemblyDirection.getNormal(), carriage.bogeySpacing) : BlockPos.ZERO;
			TrainCouplerVisualRebaser.apply(entity, offset, newAssemblyDirection);
			carriageRebases.add(new RebaseInstruction(entity.getId(), offset, newAssemblyDirection));
			debug(debugAttempt, "carriage visual rebased: entity={} offset={} oldDirection={} newDirection={}",
				entity.getUUID(), offset, oldAssemblyDirection, newAssemblyDirection);
		});

		carriage.bogeys = Couple.create(newLeadingBogey, newTrailingBogey);
		carriage.updateContraptionAnchors();
		carriage.alignEntity(level);
		carriage.forEachPresentEntity(CarriageContraptionEntity::syncCarriage);
		rebases.addAll(carriageRebases);
	}

	private static CarriageBogey reversedBogey(CarriageBogey bogey, com.simibubi.create.content.trains.graph.TrackGraph graph) {
		CarriageBogeyAccessor accessor = (CarriageBogeyAccessor) bogey;
		Couple<TravellingPoint> points = accessor.railward$getPoints();
		return new CarriageBogey(accessor.railward$getType(), accessor.railward$isUpsideDown(), bogey.bogeyData.copy(),
			reversedPoint(points.getSecond(), graph), reversedPoint(points.getFirst(), graph));
	}

	private static TravellingPoint reversedPoint(TravellingPoint point,
		com.simibubi.create.content.trains.graph.TrackGraph graph) {
		TravellingPoint copy = new TravellingPoint(point.node1, point.node2, point.edge, point.position, point.upsideDown);
		copy.blocked = point.blocked;
		if (copy.edge != null)
			copy.reverse(graph);
		return copy;
	}

	private static BlockPos scale(Vec3i pos, int scale) {
		return new BlockPos(pos.getX() * scale, pos.getY() * scale, pos.getZ() * scale);
	}

	private static double[] reverseCopy(double[] values) {
		double[] copy = new double[values.length];
		for (int i = 0; i < values.length; i++)
			copy[i] = values[values.length - 1 - i];
		return copy;
	}

	private static double[] copyStress(double[] originalStress, int newLength) {
		double[] newStress = new double[newLength];
		System.arraycopy(originalStress, 0, newStress, 0, Math.min(originalStress.length, newStress.length));
		return newStress;
	}

	private static boolean hasBackwardControls(Collection<Carriage> carriages) {
		for (Carriage carriage : carriages) {
			CarriageContraptionEntity entity = carriage.anyAvailableEntity();
			if (entity == null || !(entity.getContraption() instanceof CarriageContraption contraption))
				continue;
			if (contraption.hasBackwardControls())
				return true;
		}
		return false;
	}

	private static boolean hasAnyControls(Collection<Carriage> carriages) {
		for (Carriage carriage : carriages) {
			CarriageContraptionEntity entity = carriage.anyAvailableEntity();
			if (entity == null || !(entity.getContraption() instanceof CarriageContraption contraption))
				continue;
			if (contraption.hasForwardControls() || contraption.hasBackwardControls())
				return true;
		}
		return false;
	}

	private static void assignSplitNames(Train firstTrain, Train secondTrain, TextComponent originalName) {
		boolean firstHasControls = hasAnyControls(firstTrain.carriages);
		boolean secondHasControls = hasAnyControls(secondTrain.carriages);
		Set<String> usedNames = collectUsedTrainNames(firstTrain.id, secondTrain.id);

		if (firstHasControls && !secondHasControls) {
			firstTrain.name = originalName;
			secondTrain.name = nextGeneratedName(usedNames);
			return;
		}
		if (!firstHasControls && secondHasControls) {
			firstTrain.name = nextGeneratedName(usedNames);
			secondTrain.name = originalName;
			return;
		}
		if (firstHasControls) {
			firstTrain.name = originalName;
			secondTrain.name = nextGeneratedName(usedNames);
			return;
		}

		firstTrain.name = nextGeneratedName(usedNames);
		secondTrain.name = nextGeneratedName(usedNames);
	}

	private static TextComponent resolveCoupledName(Train clickedTrain, Train frontTrain, Train backTrain) {
		boolean frontHasControls = hasAnyControls(frontTrain.carriages);
		boolean backHasControls = hasAnyControls(backTrain.carriages);

		if (frontHasControls && !backHasControls)
			return new TextComponent(frontTrain.name.getString());
		if (!frontHasControls && backHasControls)
			return new TextComponent(backTrain.name.getString());
		return new TextComponent(clickedTrain.name.getString());
	}

	private static Set<String> collectUsedTrainNames(UUID... excludedTrainIds) {
		Set<UUID> excluded = new HashSet<>(List.of(excludedTrainIds));
		Set<String> used = new HashSet<>();
		for (Train train : Create.RAILWAYS.trains.values()) {
			if (excluded.contains(train.id))
				continue;
			used.add(train.name.getString());
		}
		return used;
	}

	private static TextComponent nextGeneratedName(Set<String> usedNames) {
		for (int i = 1; i < 1000; i++) {
			String name = String.format("#%03d", i);
			if (usedNames.add(name))
				return new TextComponent(name);
		}

		int i = 1000;
		while (!usedNames.add("#" + i))
			i++;
		return new TextComponent("#" + i);
	}

	private static void rebindCarriages(Train train) {
		for (int i = 0; i < train.carriages.size(); i++) {
			Carriage carriage = train.carriages.get(i);
			int carriageIndex = i;
			carriage.setTrain(train);
			carriage.forEachPresentEntity(entity -> {
				entity.trainId = train.id;
				entity.carriageIndex = carriageIndex;
				entity.setCarriage(carriage);
				entity.syncCarriage();
			});
		}
	}

	private static void syncTrains(Train first, Train second) {
		List<CarriageContraptionEntity> entities = new ArrayList<>();
		collectEntities(first, entities);
		collectEntities(second, entities);

		RailwardPackets.getChannel()
			.send(PacketDistributor.ALL.noArg(), new TrainCouplerSyncPacket(List.of(first, second), entities));
	}

	private static void syncMergedTrain(Train train, UUID removedTrainId, Collection<RebaseInstruction> rebases) {
		List<CarriageContraptionEntity> entities = new ArrayList<>();
		collectEntities(train, entities);

		RailwardPackets.getChannel()
			.send(PacketDistributor.ALL.noArg(),
				new TrainCouplerSyncPacket(List.of(train), entities, List.of(removedTrainId), rebases));
	}

	private static void collectEntities(Train train, List<CarriageContraptionEntity> entities) {
		for (Carriage carriage : train.carriages)
			carriage.forEachPresentEntity(entities::add);
	}

	private static void debug(int attempt, String message, Object... args) {
		if (!isCouplerDebugEnabled())
			return;

		Object[] prefixedArgs = new Object[args.length + 1];
		prefixedArgs[0] = attempt;
		System.arraycopy(args, 0, prefixedArgs, 1, args.length);
		LOGGER.info("[Railward TrainCoupler Debug #{}] " + message, prefixedArgs);
	}

	private static void autoDebug(int attempt, String message, Object... args) {
		if (!isAutoCouplerDebugEnabled())
			return;

		Object[] prefixedArgs = new Object[args.length + 1];
		prefixedArgs[0] = attempt;
		System.arraycopy(args, 0, prefixedArgs, 1, args.length);
		LOGGER.info("[Railward AutoCoupler Debug #{}] " + message, prefixedArgs);
	}

	private static void debugTrainGeometry(int attempt, Level level, Train train) {
		if (!isCouplerDebugEnabled() || train == null)
			return;

		for (int i = 0; i < train.carriages.size(); i++) {
			Carriage carriage = train.carriages.get(i);
			Carriage.DimensionalCarriageEntity dimensional = carriage.getDimensionalIfPresent(level.dimension());
			CarriageContraptionEntity entity = carriage.anyAvailableEntity();
			Direction assemblyDirection = null;
			if (entity != null && entity.getContraption() instanceof CarriageContraption carriageContraption)
				assemblyDirection = carriageContraption.getAssemblyDirection();

			debug(attempt,
				"train geometry: train={} carriage={} twoBogeys={} straight={} assemblyDirection={} axis={} leadingPoint={} trailingPoint={} leadingAnchor={} trailingAnchor={}",
				debugTrain(train), i, carriage.isOnTwoBogeys(), isStraightHorizontal(carriage), assemblyDirection,
				debugVec(getCarriageAxis(level, carriage)), debugVec(debugPointPosition(train, carriage.getLeadingPoint())),
				debugVec(debugPointPosition(train, carriage.getTrailingPoint())),
				dimensional == null ? null : debugVec(dimensional.leadingAnchor()),
				dimensional == null ? null : debugVec(dimensional.trailingAnchor()));
		}
	}

	private static boolean isCouplerDebugEnabled() {
		return RailwardConfig.COMMON.trainCouplerDebugLogging.get()
			|| RailwardConfig.COMMON.trainCouplerAutoDebugLogging.get();
	}

	private static boolean isAutoCouplerDebugEnabled() {
		return RailwardConfig.COMMON.trainCouplerDebugLogging.get()
			|| RailwardConfig.COMMON.trainCouplerAutoDebugLogging.get();
	}

	private static Vec3 debugPointPosition(Train train, TravellingPoint point) {
		if (train == null || train.graph == null || point == null || point.edge == null)
			return null;
		try {
			return point.getPosition(train.graph);
		} catch (RuntimeException e) {
			return null;
		}
	}

	private static String debugTrain(Train train) {
		if (train == null)
			return "null";

		String name = train.name == null ? "<unnamed>" : train.name.getString();
		String id = train.id == null ? "null" : train.id.toString();
		if (id.length() > 8)
			id = id.substring(0, 8);
		return String.format(Locale.ROOT, "%s/%s carriages=%d speed=%.5f target=%.5f derailed=%s", name, id,
			train.carriages.size(), train.speed, train.targetSpeed, train.derailed);
	}

	private static String debugState(BlockState state) {
		if (state == null)
			return "null";

		Object blockName = state.getBlock()
			.getRegistryName();
		if (!state.hasProperty(TrainCouplerBlock.FACING))
			return String.valueOf(blockName);
		return blockName + "[facing=" + state.getValue(TrainCouplerBlock.FACING) + "]";
	}

	private static String debugVec(Vec3 vec) {
		if (vec == null)
			return "null";
		return String.format(Locale.ROOT, "(%.3f, %.3f, %.3f)", vec.x, vec.y, vec.z);
	}

	private static String debugDistance(double value) {
		return String.format(Locale.ROOT, "%.4f", value);
	}

	private enum CouplerEnd {
		LEADING,
		TRAILING;

		private CouplerEnd opposite() {
			return this == LEADING ? TRAILING : LEADING;
		}
	}

	private record CouplingCandidate(Train train, int carriageIndex, CouplerEnd end, CouplerRef coupler,
		double distanceSq) {
	}

	private record MovingCoupler(int carriageIndex, CouplerEnd end, CouplerRef coupler, Vec3 direction) {
	}

	private record CouplerLocation(int carriageIndex, CouplerEnd end, CouplerRef coupler) {
	}

	private record CouplerEndpoint(UUID entityId, BlockPos localPos) {
	}

	private record SuppressedCouplerPair(CouplerEndpoint first, CouplerEndpoint second) {

		private boolean matches(SuppressedCouplerPair other) {
			return matches(other.first, other.second);
		}

		private boolean matches(CouplerEndpoint left, CouplerEndpoint right) {
			return first.equals(left) && second.equals(right) || first.equals(right) && second.equals(left);
		}
	}

	private record CouplerRef(Vec3 center, CouplerEndpoint endpoint) {

		private static CouplerRef from(CarriageContraptionEntity entity, BlockPos localPos, BlockState state) {
			Vec3 center = entity.toGlobalVector(VecHelper.getCenterOf(localPos), 1);
			return new CouplerRef(center, new CouplerEndpoint(entity.getUUID(), localPos.immutable()));
		}
	}

	public record Result(boolean success, String message) {

		private static Result success(String message) {
			return new Result(true, message);
		}

		private static Result fail(String message) {
			return new Result(false, message);
		}
	}

}
