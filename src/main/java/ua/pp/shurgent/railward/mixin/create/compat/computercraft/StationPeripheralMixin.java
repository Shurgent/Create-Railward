package ua.pp.shurgent.railward.mixin.create.compat.computercraft;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.spongepowered.asm.mixin.Mixin;

import com.simibubi.create.Create;
import com.simibubi.create.compat.computercraft.implementation.CreateLuaTable;
import com.simibubi.create.compat.computercraft.implementation.peripherals.StationPeripheral;
import com.simibubi.create.compat.computercraft.implementation.peripherals.SyncedPeripheral;
import com.simibubi.create.content.trains.display.GlobalTrainDisplayData.TrainDeparturePrediction;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.schedule.Schedule;
import com.simibubi.create.content.trains.station.GlobalStation;
import com.simibubi.create.content.trains.station.StationBlockEntity;

import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;

/**
 * Adds machine-readable arrival and schedule data to Create's station
 * peripheral. {@link ua.pp.shurgent.railward.mixin.RailwardMixinPlugin}
 * prevents this mixin from being applied when CC:Tweaked is not installed.
 */
@Mixin(value = StationPeripheral.class, remap = false)
abstract class StationPeripheralMixin extends SyncedPeripheral<StationBlockEntity> {

	protected StationPeripheralMixin(StationBlockEntity blockEntity) {
		super(blockEntity);
	}

	/**
	 * Gets predicted calls at this exact station, ordered by ETA.
	 * Unknown ETAs use Create's {@code -1} sentinel and are sorted last.
	 */
	@LuaFunction(mainThread = true)
	public final CreateLuaTable getArrivals(Optional<Integer> limitArgument) throws LuaException {
		GlobalStation station = getStationOrThrow();
		int limit = limitArgument.orElse(Integer.MAX_VALUE);
		if (limit < 1)
			throw new LuaException("limit must be greater than zero");

		List<TrainDeparturePrediction> arrivals = new ArrayList<>();
		for (Train train : Create.RAILWAYS.sided(blockEntity.getLevel()).trains.values()) {
			if (train.runtime.paused || train.runtime.getSchedule() == null)
				continue;
			if (train.derailed || train.graph == null)
				continue;

			for (TrainDeparturePrediction prediction : train.runtime.submitPredictions()) {
				if (station.name.equals(prediction.destination))
					arrivals.add(prediction);
			}
		}

		arrivals.sort(null);
		CreateLuaTable result = new CreateLuaTable();
		int resultIndex = 1;
		for (TrainDeparturePrediction prediction : arrivals) {
			if (resultIndex > limit)
				break;

			CreateLuaTable arrival = new CreateLuaTable();
			arrival.putString("train_id", prediction.train.id.toString());
			arrival.putString("train_name", prediction.train.name.getString());
			arrival.putString("destination", prediction.scheduleTitle.getString());
			arrival.putString("station_name", prediction.destination);
			arrival.putDouble("arrival_ticks", prediction.ticks);
			arrival.putDouble("arrival_seconds", prediction.ticks < 0 ? -1 : prediction.ticks / 20.0);
			arrival.putBoolean("predictable", prediction.ticks >= 0);
			result.putTable(resultIndex++, arrival);
		}

		return result;
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

		return result;
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
