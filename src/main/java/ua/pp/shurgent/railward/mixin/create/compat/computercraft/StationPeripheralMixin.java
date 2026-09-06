package ua.pp.shurgent.railward.mixin.create.compat.computercraft;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;

import com.simibubi.create.Create;
import com.simibubi.create.compat.computercraft.implementation.CreateLuaTable;
import com.simibubi.create.compat.computercraft.implementation.peripherals.StationPeripheral;
import com.simibubi.create.compat.computercraft.implementation.peripherals.SyncedPeripheral;
import com.simibubi.create.content.trains.display.GlobalTrainDisplayData.TrainDeparturePrediction;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.graph.EdgePointType;
import com.simibubi.create.content.trains.schedule.Schedule;
import com.simibubi.create.content.trains.schedule.ScheduleEntry;
import com.simibubi.create.content.trains.schedule.ScheduleRuntime;
import com.simibubi.create.content.trains.schedule.condition.ScheduledDelay;
import com.simibubi.create.content.trains.schedule.condition.ScheduleWaitCondition;
import com.simibubi.create.content.trains.schedule.destination.DestinationInstruction;
import com.simibubi.create.content.trains.station.GlobalStation;
import com.simibubi.create.content.trains.station.StationBlockEntity;

import dan200.computercraft.api.lua.IArguments;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import ua.pp.shurgent.railward.compat.WorldTimeProjection;
import ua.pp.shurgent.railward.compat.StationArrivalSelection;

/**
 * Adds machine-readable arrival and schedule data to Create's station
 * peripheral. {@link ua.pp.shurgent.railward.mixin.RailwardMixinPlugin}
 * prevents this mixin from being applied when CC:Tweaked is not installed.
 */
@Mixin(value = StationPeripheral.class, remap = false)
abstract class StationPeripheralMixin extends SyncedPeripheral<StationBlockEntity> {

	private static final long UNKNOWN_TICKS = -1;
	private static final long MINECRAFT_DAY_TICKS = 24_000;
	private static final long MINECRAFT_CLOCK_OFFSET = 6_000;

	protected StationPeripheralMixin(StationBlockEntity blockEntity) {
		super(blockEntity);
	}

	/**
	 * Gets assigned calls and unassigned schedule candidates for this station.
	 * Unknown ETAs use Create's {@code -1} sentinel and are sorted last.
	 */
	@LuaFunction(mainThread = true)
	public final CreateLuaTable getArrivals(IArguments arguments) throws LuaException {
		GlobalStation station = getStationOrThrow();
		Level level = blockEntity.getLevel();
		if (level == null)
			throw new LuaException("station is not loaded");

		int limit = arguments.optInt(0).orElse(Integer.MAX_VALUE);
		if (limit < 1)
			throw new LuaException("limit must be greater than zero");

		boolean daylightCycle = level.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT);
		WorldTimeProjection.Snapshot clock =
			WorldTimeProjection.capture(level, daylightCycle);
		List<TrainDeparturePrediction> arrivals = new ArrayList<>();
		Map<TrainDeparturePrediction, Integer> scheduleEntries = new IdentityHashMap<>();
		Map<TrainDeparturePrediction, Integer> stableArrivalTicks = new IdentityHashMap<>();
		Map<TrainDeparturePrediction, String> estimateSources = new IdentityHashMap<>();
		Map<TrainDeparturePrediction, GlobalStation> assignedStations = new IdentityHashMap<>();

		for (Train train : Create.RAILWAYS.sided(level).trains.values()) {
			if (train.runtime.paused || train.runtime.getSchedule() == null)
				continue;
			if (train.derailed || train.graph == null)
				continue;
			// A matching name in an unrelated railway graph is not a candidate.
			if (train.graph.getPoint(EdgePointType.STATION, station.getId()) != station)
				continue;

			List<TrainDeparturePrediction> trainPredictions =
				new ArrayList<>(train.runtime.submitPredictions());
			calculateStableArrivalTicks(
				train,
				trainPredictions,
				stableArrivalTicks,
				estimateSources
			);

			int scheduleCursor = train.runtime.currentEntry;
			boolean firstPrediction = true;
			for (TrainDeparturePrediction prediction : trainPredictions) {
				int entryIndex = resolvePredictionEntry(train, prediction, scheduleCursor, firstPrediction);
				GlobalStation assignedStation = firstPrediction ? getCurrentPredictionStation(train) : null;
				firstPrediction = false;
				if (entryIndex >= 0)
					scheduleCursor = entryIndex + 1;

				DestinationInstruction instruction = getDestinationInstruction(train, entryIndex);
				if (StationArrivalSelection.matches(
					station.getId(), assignedStation == null ? null : assignedStation.getId(),
					station.name, instruction == null ? null : instruction.getFilterForRegex()
				)) {
					arrivals.add(prediction);
					scheduleEntries.put(prediction, entryIndex);
					if (assignedStation != null)
						assignedStations.put(prediction, assignedStation);
				}
			}
		}

		sortPredictions(arrivals, stableArrivalTicks);
		CreateLuaTable result = new CreateLuaTable();
		int resultIndex = 1;
		for (TrainDeparturePrediction prediction : arrivals) {
			if (resultIndex > limit)
				break;

			int arrivalTicks = stableArrivalTicks.getOrDefault(prediction, prediction.ticks);
			boolean arrivalPredictable = arrivalTicks >= 0;
			long dwellTicks = estimateDwellTicks(
				prediction.train,
				scheduleEntries.getOrDefault(prediction, -1),
				station
			);
			boolean departurePredictable = arrivalPredictable && dwellTicks >= 0;
			long departureTicks = departurePredictable
				? arrivalTicks + dwellTicks
				: UNKNOWN_TICKS;

			CreateLuaTable arrival = new CreateLuaTable();
			arrival.putString("train_id", prediction.train.id.toString());
			arrival.putString("train_name", prediction.train.name.getString());
			arrival.putString("destination", prediction.scheduleTitle.getString());
			arrival.putString("station_name", prediction.destination);
			GlobalStation assignedStation = assignedStations.get(prediction);
			int entryIndex = scheduleEntries.getOrDefault(prediction, -1);
			DestinationInstruction instruction = getDestinationInstruction(prediction.train, entryIndex);
			arrival.putString("station_filter", instruction == null ? "" : instruction.getFilter());
			arrival.putBoolean("platform_assigned", assignedStation != null);
			arrival.putString("station_id", assignedStation == null ? "" : assignedStation.getId().toString());
			arrival.putBoolean("at_station", assignedStation != null
				&& prediction.train.runtime.state == ScheduleRuntime.State.POST_TRANSIT
				&& prediction.train.getCurrentStation() == assignedStation);
			arrival.putDouble("schedule_entry", entryIndex < 0 ? -1 : entryIndex + 1);
			// Distinguishes repeated visits to the same name in one schedule.
			// This identifies a schedule slot, not a historical journey instance.
			arrival.putString("call_id", prediction.train.id + ":" + (entryIndex + 1));
			arrival.putDouble("arrival_ticks", arrivalTicks);
			arrival.putDouble("arrival_seconds", arrivalPredictable ? arrivalTicks / 20.0 : -1);
			arrival.putDouble("create_arrival_ticks", prediction.ticks);
			arrival.putDouble(
				"create_arrival_seconds",
				prediction.ticks >= 0 ? prediction.ticks / 20.0 : -1
			);
			arrival.putString(
				"arrival_estimate_source",
				estimateSources.getOrDefault(prediction, "create")
			);
			arrival.putBoolean("predictable", arrivalPredictable);
			arrival.putBoolean("arrival_predictable", arrivalPredictable);
			arrival.putDouble("clock_rate", clock.dayTicksPerServerTick());
			arrival.putString("clock_source", clock.source());
			putProjectedTime(
				arrival,
				"arrival",
				arrivalTicks,
				clock
			);

			arrival.putDouble("dwell_ticks", dwellTicks);
			arrival.putDouble("dwell_seconds", dwellTicks >= 0 ? dwellTicks / 20.0 : -1);
			arrival.putBoolean("departure_predictable", departurePredictable);
			arrival.putDouble("departure_ticks", departureTicks);
			arrival.putDouble("departure_seconds", departurePredictable ? departureTicks / 20.0 : -1);
			putProjectedTime(
				arrival,
				"departure",
				departureTicks,
				clock
			);

			result.putTable(resultIndex++, arrival);
		}

		return result;
	}

	@LuaFunction(mainThread = true)
	public final String getStationId() throws LuaException {
		return getStationOrThrow().getId().toString();
	}

	private GlobalStation getCurrentPredictionStation(Train train) {
		Schedule schedule = train.runtime.getSchedule();
		// Mirrors the first (current) prediction in Create's submitPredictions().
		if (train.runtime.state == ScheduleRuntime.State.POST_TRANSIT
			|| train.runtime.currentEntry >= schedule.entries.size())
			return train.getCurrentStation();
		return train.navigation.destination;
	}

	private DestinationInstruction getDestinationInstruction(Train train, int entryIndex) {
		Schedule schedule = train.runtime.getSchedule();
		if (entryIndex < 0 || entryIndex >= schedule.entries.size())
			return null;
		return schedule.entries.get(entryIndex).instruction instanceof DestinationInstruction destination
			? destination : null;
	}

	/**
	 * Gets the present train's schedule together with its live runtime state.
	 * The inherited getSchedule() remains available for callers which only
	 * need Create's raw schedule table.
	 */
	@LuaFunction(mainThread = true)
	public final CreateLuaTable getTrainSchedule() throws LuaException {
		Train train = getPresentTrainOrThrow();
		Schedule schedule = train.runtime.getSchedule();
		if (schedule == null)
			throw new LuaException("train doesn't have a schedule");

		CreateLuaTable result = ((StationPeripheral) (Object) this).getSchedule();
		result.putString("train_id", train.id.toString());
		result.putString("train_name", train.name.getString());
		result.putDouble("current_entry", train.runtime.currentEntry + 1);
		result.putDouble("entry_count", schedule.entries.size());
		result.putString("state", train.runtime.state.name().toLowerCase(Locale.ROOT));
		result.putBoolean("paused", train.runtime.paused);
		result.putBoolean("completed", train.runtime.completed);
		result.putBoolean("auto_schedule", train.runtime.isAutoSchedule);
		result.putString("current_destination",
			train.navigation.destination == null ? "" : train.navigation.destination.name);

		Level level = blockEntity.getLevel();
		long departureTicks = UNKNOWN_TICKS;
		if (level != null
			&& !train.runtime.paused
			&& train.runtime.state == ScheduleRuntime.State.POST_TRANSIT
			&& train.runtime.currentEntry >= 0
			&& train.runtime.currentEntry < schedule.entries.size()) {
			departureTicks = estimateRemainingDwellTicks(
				train,
				schedule.entries.get(train.runtime.currentEntry)
			);
		}

		result.putBoolean("departure_predictable", departureTicks >= 0);
		result.putDouble("departure_ticks", departureTicks);
		result.putDouble("departure_seconds", departureTicks >= 0 ? departureTicks / 20.0 : -1);
		if (level == null) {
			result.putDouble("clock_rate", -1);
			result.putString("clock_source", "unavailable");
			putUnknownTime(result, "departure");
		} else {
			WorldTimeProjection.Snapshot clock = WorldTimeProjection.capture(
				level,
				level.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT)
			);
			result.putDouble("clock_rate", clock.dayTicksPerServerTick());
			result.putString("clock_source", clock.source());
			putProjectedTime(
				result,
				"departure",
				departureTicks,
				clock
			);
		}

		return result;
	}

	private void calculateStableArrivalTicks(
		Train train,
		List<TrainDeparturePrediction> predictions,
		Map<TrainDeparturePrediction, Integer> stableTicks,
		Map<TrainDeparturePrediction, String> estimateSources
	) {
		if (predictions.isEmpty()) {
			return;
		}

		TrainDeparturePrediction currentPrediction = predictions.get(0);
		GlobalStation navigationDestination = train.navigation.destination;
		boolean hasCurrentLeg = navigationDestination != null
			&& navigationDestination.name.equals(currentPrediction.destination);

		if (!hasCurrentLeg) {
			for (TrainDeparturePrediction prediction : predictions) {
				stableTicks.put(prediction, prediction.ticks);
				estimateSources.put(prediction, "create");
			}
			return;
		}

		int rawCurrentTicks = currentPrediction.ticks;
		int currentTicks = -1;
		String source = "unpredictable";

		if (rawCurrentTicks >= 0 && train.navigation.waitingForSignal == null) {
			CompoundTag runtimeTag = train.runtime.write();
			int[] transitTimes = runtimeTag.getIntArray("TransitTimes");
			int currentEntry = train.runtime.currentEntry;
			int elapsedTicks =
				((ScheduleRuntimeAccessor) (Object) train.runtime).railward$getTicksInTransit();

			if (currentEntry >= 0
				&& currentEntry < transitTimes.length
				&& transitTimes[currentEntry] > elapsedTicks) {
				currentTicks = transitTimes[currentEntry] - elapsedTicks;
				source = "historical";
			} else {
				// Create's first-trip fallback is distance / nominal speed * 2.
				// Removing that safety factor produces a clock ETA which does
				// not count backwards at twice the rate of world time.
				currentTicks = rawCurrentTicks / 2;
				source = "live";
			}
		}

		for (TrainDeparturePrediction prediction : predictions) {
			int adjustedTicks = -1;
			if (prediction.ticks >= 0 && currentTicks >= 0) {
				long adjusted = (long) prediction.ticks - rawCurrentTicks + currentTicks;
				adjustedTicks = (int) Math.min(Integer.MAX_VALUE, Math.max(0, adjusted));
			}
			stableTicks.put(prediction, adjustedTicks);
			estimateSources.put(prediction, source);
		}
	}

	private void sortPredictions(
		List<TrainDeparturePrediction> predictions,
		Map<TrainDeparturePrediction, Integer> stableTicks
	) {
		for (int index = 1; index < predictions.size(); index++) {
			TrainDeparturePrediction current = predictions.get(index);
			int insertionIndex = index;

			while (insertionIndex > 0
				&& comparePredictions(
					predictions.get(insertionIndex - 1),
					current,
					stableTicks
				) > 0) {
				predictions.set(insertionIndex, predictions.get(insertionIndex - 1));
				insertionIndex--;
			}

			predictions.set(insertionIndex, current);
		}
	}

	private int comparePredictions(
		TrainDeparturePrediction first,
		TrainDeparturePrediction second,
		Map<TrainDeparturePrediction, Integer> stableTicks
	) {
		int firstTicks = stableTicks.getOrDefault(first, first.ticks);
		int secondTicks = stableTicks.getOrDefault(second, second.ticks);
		int firstComparable = firstTicks < 0 ? Integer.MAX_VALUE : firstTicks;
		int secondComparable = secondTicks < 0 ? Integer.MAX_VALUE : secondTicks;
		int timeComparison = Integer.compare(firstComparable, secondComparable);
		if (timeComparison != 0)
			return timeComparison;
		return first.train.name.getString().compareTo(second.train.name.getString());
	}

	private int resolvePredictionEntry(
		Train train,
		TrainDeparturePrediction prediction,
		int searchStart,
		boolean firstPrediction
	) {
		Schedule schedule = train.runtime.getSchedule();
		if (schedule == null || schedule.entries.isEmpty())
			return -1;

		int entryCount = schedule.entries.size();
		int currentEntry = train.runtime.currentEntry;
		if (firstPrediction && getCurrentPredictionStation(train) != null)
			return currentEntry >= 0 && currentEntry < entryCount ? currentEntry : -1;
		if (firstPrediction && train.runtime.state == ScheduleRuntime.State.POST_TRANSIT)
			searchStart = currentEntry + 1;

		for (int offset = 0; offset < entryCount; offset++) {
			int entryIndex;
			if (schedule.cyclic) {
				entryIndex = Math.floorMod(searchStart + offset, entryCount);
			} else {
				entryIndex = searchStart + offset;
				if (entryIndex < 0 || entryIndex >= entryCount)
					break;
			}

			if (schedule.entries.get(entryIndex).instruction instanceof DestinationInstruction destination
				&& destination.getFilter().equals(prediction.destination))
				return entryIndex;
		}

		return -1;
	}

	private long estimateDwellTicks(Train train, int entryIndex, GlobalStation station) {
		Schedule schedule = train.runtime.getSchedule();
		if (schedule == null || entryIndex < 0 || entryIndex >= schedule.entries.size())
			return UNKNOWN_TICKS;

		ScheduleEntry entry = schedule.entries.get(entryIndex);
		if (train.getCurrentStation() == station
			&& train.runtime.state == ScheduleRuntime.State.POST_TRANSIT
			&& train.runtime.currentEntry == entryIndex)
			return estimateRemainingDwellTicks(train, entry);

		return estimateFullDwellTicks(entry);
	}

	private long estimateFullDwellTicks(ScheduleEntry entry) {
		if (entry.conditions.isEmpty())
			return UNKNOWN_TICKS;

		for (List<ScheduleWaitCondition> column : entry.conditions) {
			if (column.isEmpty())
				return 0;
		}

		long shortestColumn = Long.MAX_VALUE;
		for (List<ScheduleWaitCondition> column : entry.conditions) {
			long columnTicks = 0;
			for (ScheduleWaitCondition condition : column) {
				if (!(condition instanceof ScheduledDelay scheduledDelay))
					return UNKNOWN_TICKS;
				columnTicks += scheduledDelay.totalWaitTicks();
			}
			shortestColumn = Math.min(shortestColumn, columnTicks);
		}

		return shortestColumn;
	}

	private long estimateRemainingDwellTicks(Train train, ScheduleEntry entry) {
		if (entry.conditions.isEmpty())
			return UNKNOWN_TICKS;

		CompoundTag runtimeTag = train.runtime.write();
		int[] progress = runtimeTag.getIntArray("ConditionProgress");
		ListTag contexts = runtimeTag.getList("ConditionContext", Tag.TAG_COMPOUND);
		if (progress.length < entry.conditions.size() || contexts.size() < entry.conditions.size())
			return UNKNOWN_TICKS;

		for (int columnIndex = 0; columnIndex < entry.conditions.size(); columnIndex++) {
			List<ScheduleWaitCondition> column = entry.conditions.get(columnIndex);
			if (column.isEmpty() || progress[columnIndex] >= column.size())
				return 0;
		}

		long shortestColumn = Long.MAX_VALUE;
		for (int columnIndex = 0; columnIndex < entry.conditions.size(); columnIndex++) {
			List<ScheduleWaitCondition> column = entry.conditions.get(columnIndex);
			int currentCondition = progress[columnIndex];
			long remainingTicks = 0;

			for (int conditionIndex = currentCondition; conditionIndex < column.size(); conditionIndex++) {
				ScheduleWaitCondition condition = column.get(conditionIndex);
				if (!(condition instanceof ScheduledDelay scheduledDelay))
					return UNKNOWN_TICKS;

				long conditionTicks = scheduledDelay.totalWaitTicks();
				if (conditionIndex == currentCondition) {
					conditionTicks = Math.max(
						0,
						conditionTicks - contexts.getCompound(columnIndex).getInt("Time")
					);
				}
				remainingTicks += conditionTicks;
			}
			shortestColumn = Math.min(shortestColumn, remainingTicks);
		}

		return shortestColumn;
	}

	private void putProjectedTime(
		CreateLuaTable table,
		String prefix,
		long relativeTicks,
		WorldTimeProjection.Snapshot clock
	) {
		if (relativeTicks < 0) {
			putUnknownTime(table, prefix);
			return;
		}

		long projectedDayTime = clock.project(relativeTicks);
		long day = Math.floorDiv(projectedDayTime, MINECRAFT_DAY_TICKS);
		long dayTime = Math.floorMod(projectedDayTime, MINECRAFT_DAY_TICKS);
		long clockTime = Math.floorMod(
			dayTime + MINECRAFT_CLOCK_OFFSET,
			MINECRAFT_DAY_TICKS
		);
		int hour = (int) (clockTime / 1_000);
		int minute = (int) ((clockTime % 1_000) * 60 / 1_000);

		table.putDouble(prefix + "_day", day);
		table.putDouble(prefix + "_day_time", dayTime);
		table.putDouble(prefix + "_hour", hour);
		table.putDouble(prefix + "_minute", minute);
		table.putString(prefix + "_time", String.format(Locale.ROOT, "%02d:%02d", hour, minute));
	}

	private void putUnknownTime(CreateLuaTable table, String prefix) {
		table.putDouble(prefix + "_day", -1);
		table.putDouble(prefix + "_day_time", -1);
		table.putDouble(prefix + "_hour", -1);
		table.putDouble(prefix + "_minute", -1);
		table.putString(prefix + "_time", "");
	}

	private GlobalStation getStationOrThrow() throws LuaException {
		GlobalStation station = blockEntity.getStation();
		if (station == null)
			throw new LuaException("station is not connected to a track");
		return station;
	}

	private Train getPresentTrainOrThrow() throws LuaException {
		Train train = getStationOrThrow().getPresentTrain();
		if (train == null)
			throw new LuaException("there is no train present");
		return train;
	}

}
