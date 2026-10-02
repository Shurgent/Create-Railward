package ua.pp.shurgent.railward.gametest;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.simibubi.create.content.contraptions.actors.trainControls.ControlsBlock;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.schedule.Schedule;
import com.simibubi.create.content.trains.schedule.ScheduleEntry;
import com.simibubi.create.content.trains.schedule.ScheduleRuntime;
import com.simibubi.create.content.trains.schedule.condition.ScheduledDelay;
import com.simibubi.create.content.trains.schedule.destination.DestinationInstruction;
import com.simibubi.create.content.trains.station.StationBlockEntity;
import com.simibubi.create.content.trains.track.TrackBlock;
import com.simibubi.create.content.trains.track.TrackShape;
import com.simibubi.create.content.trains.bogey.AbstractBogeyBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.gametest.GameTestHolder;
import ua.pp.shurgent.railward.Railward;
import ua.pp.shurgent.railward.compat.RailwardComponents;

/**
 * Opt-in CC:Tweaked integration coverage. All CC classes are resolved by name
 * inside the generated test so this class remains loadable in ordinary runs
 * where the optional mod is deliberately absent.
 */
@GameTestHolder(Railward.MOD_ID)
public final class RailwardComputerCraftGameTests {

	public static final String ENABLED_PROPERTY = "railward.ccTest";
	private static final String TEST_NAME = "railwardcomputercraft.station_peripheral_api";
	private static final String EMPTY_TEMPLATE = "railward:empty";
	private static final String STATION_NAME = "Railward CC-01 Central";
	private static final String RENAMED_STATION_NAME = "Railward CC-01 Renamed";
	private static final String TRAIN_NAME = "Railward CC-01 train";

	private RailwardComputerCraftGameTests() {
	}

	@GameTestGenerator
	public static Collection<TestFunction> generateComputerCraftTest() {
		if (!Boolean.getBoolean(ENABLED_PROPERTY))
			return List.of();
		return List.of(new TestFunction(
			"railwardComputerCraft",
			TEST_NAME,
			EMPTY_TEMPLATE,
			220,
			1,
			true,
			RailwardComputerCraftGameTests::testStationPeripheral
		));
	}

	private static void testStationPeripheral(GameTestHelper helper) {
		require(helper, ModList.get().isLoaded("computercraft"),
			"CC-01 was enabled but CC:Tweaked was not loaded");

		BlockPos stationPos = new BlockPos(12, 1, 5);
		BlockPos targetTrackPos = new BlockPos(12, 1, 8);
		BlockPos bogeyPos = new BlockPos(11, 2, 8);
		BlockPos attachedPos = new BlockPos(10, 2, 8);
		BlockPos controlsPos = new BlockPos(10, 2, 9);
		for (int x = 1; x <= 20; x++)
			helper.setBlock(new BlockPos(x, 1, 8), com.simibubi.create.AllBlocks.TRACK.get()
				.defaultBlockState()
				.setValue(TrackBlock.SHAPE, TrackShape.XO));
		helper.setBlock(stationPos, com.simibubi.create.AllBlocks.TRACK_STATION.get());
		helper.setBlock(bogeyPos, com.simibubi.create.AllBlocks.SMALL_BOGEY.get()
			.defaultBlockState()
			.setValue(AbstractBogeyBlock.AXIS, Direction.Axis.X));
		helper.setBlock(attachedPos, Blocks.SLIME_BLOCK);
		helper.setBlock(controlsPos, com.simibubi.create.AllBlocks.TRAIN_CONTROLS.get()
			.defaultBlockState()
			.setValue(ControlsBlock.FACING, Direction.WEST));

		helper.runAfterDelay(5, () -> {
			BlockEntity blockEntity = helper.getBlockEntity(stationPos);
			require(helper, blockEntity instanceof StationBlockEntity,
				"CC-01 station block entity was not created");
			StationBlockEntity station = (StationBlockEntity) blockEntity;
			CompoundTag stationTag = station.saveWithFullMetadata();
			stationTag.put("TargetTrack", NbtUtils.writeBlockPos(targetTrackPos.subtract(stationPos)));
			stationTag.putBoolean("TargetDirection", false);
			station.load(stationTag);
			station.setChanged();

			helper.runAfterDelay(25, () -> {
				require(helper, station.edgePoint.hasValidTrack() && station.getStation() != null,
					"CC-01 station was not registered on the Create track graph");
				station.getStation().name = STATION_NAME;
				require(helper, station.enterAssemblyMode(null),
					"CC-01 station did not enter assembly mode");
				station.refreshAssemblyInfo();
				station.assemble(null);
				Train train = station.getStation().getPresentTrain();
				require(helper, train != null && train.carriages.size() == 1,
					"CC-01 station did not assemble its one-carriage train");
				require(helper, station.exitAssemblyMode(),
					"CC-01 station did not leave assembly mode");
				train.name = RailwardComponents.literal(TRAIN_NAME);
				train.runtime.setSchedule(createSchedule(), true);
				train.runtime.state = ScheduleRuntime.State.POST_TRANSIT;
				train.runtime.paused = false;

				try {
					assertPeripheralApi(helper, station, train);
				} catch (ReflectiveOperationException exception) {
					throw new IllegalStateException("CC-01 could not invoke the transformed station peripheral", exception);
				} finally {
					// The fixture puts the runtime directly into POST_TRANSIT so the
					// peripheral can expose deterministic state synchronously. Remove
					// that synthetic schedule before Create ticks its condition arrays.
					train.runtime.discardSchedule();
				}
				helper.succeed();
			});
		});
	}

	private static Schedule createSchedule() {
		Schedule schedule = new Schedule();
		ScheduleEntry entry = new ScheduleEntry();
		DestinationInstruction destination = new DestinationInstruction();
		CompoundTag destinationData = new CompoundTag();
		destinationData.putString("Text", STATION_NAME);
		destination.setData(destinationData);
		entry.instruction = destination;
		ScheduledDelay delay = new ScheduledDelay();
		CompoundTag delayData = new CompoundTag();
		delayData.putInt("Value", 5);
		delayData.putInt("TimeUnit", 1);
		delay.setData(delayData);
		entry.conditions.add(List.of(delay));
		schedule.entries.add(entry);
		schedule.cyclic = false;
		return schedule;
	}

	private static void assertPeripheralApi(GameTestHelper helper, StationBlockEntity station, Train train)
		throws ReflectiveOperationException {
		Class<?> peripheralClass = Class.forName(
			"com.simibubi.create.compat.computercraft.implementation.peripherals.StationPeripheral");
		Class<?> argumentsClass = Class.forName("dan200.computercraft.api.lua.IArguments");
		Class<?> objectArgumentsClass = Class.forName("dan200.computercraft.api.lua.ObjectArguments");
		Object peripheral = peripheralClass.getConstructor(StationBlockEntity.class).newInstance(station);
		Method stationIdMethod = peripheralClass.getMethod("getStationId");
		Method arrivalsMethod = peripheralClass.getMethod("getArrivals", argumentsClass);
		Method scheduleMethod = peripheralClass.getMethod("getTrainSchedule");
		requireLuaFunction(helper, stationIdMethod);
		requireLuaFunction(helper, arrivalsMethod);
		requireLuaFunction(helper, scheduleMethod);

		String stationId = (String) invoke(stationIdMethod, peripheral);
		require(helper, station.getStation().getId().toString().equals(stationId),
			"getStationId returned the wrong physical station UUID");
		station.getStation().name = RENAMED_STATION_NAME;
		require(helper, stationId.equals(invoke(stationIdMethod, peripheral)),
			"getStationId changed after the station was renamed");
		station.getStation().name = STATION_NAME;

		Object emptyArguments = objectArgumentsClass.getConstructor(Object[].class)
			.newInstance((Object) new Object[0]);
		Map<?, ?> arrivals = asTable(helper, invoke(arrivalsMethod, peripheral, emptyArguments),
			"getArrivals result");
		require(helper, arrivals.size() == 1, "getArrivals returned " + arrivals.size() + " rows instead of one");
		Map<?, ?> arrival = asTable(helper, firstRow(arrivals), "getArrivals first row");
		require(helper, TRAIN_NAME.equals(arrival.get("train_name")),
			"getArrivals returned the wrong train name");
		require(helper, STATION_NAME.equals(arrival.get("station_filter")),
			"getArrivals returned the wrong destination filter");
		require(helper, Boolean.TRUE.equals(arrival.get("platform_assigned"))
			&& stationId.equals(arrival.get("station_id"))
			&& Boolean.TRUE.equals(arrival.get("at_station")),
			"getArrivals did not report the present train's assigned physical platform");
		require(helper, number(arrival, "schedule_entry").intValue() == 1
			&& number(arrival, "arrival_ticks").longValue() == 0
			&& Boolean.TRUE.equals(arrival.get("arrival_predictable")),
			"getArrivals returned inconsistent live call timing or schedule position");
		require(helper, !String.valueOf(arrival.get("arrival_time")).isBlank()
			&& number(arrival, "clock_rate").doubleValue() >= 0,
			"getArrivals did not expose the projected world-clock fields");

		Object oneArgument = objectArgumentsClass.getConstructor(Object[].class)
			.newInstance((Object) new Object[] { 1 });
		require(helper, asTable(helper, invoke(arrivalsMethod, peripheral, oneArgument),
			"limited getArrivals result").size() == 1,
			"getArrivals(1) did not enforce the documented result limit");
		Object zeroArgument = objectArgumentsClass.getConstructor(Object[].class)
			.newInstance((Object) new Object[] { 0 });
		requireLuaFailure(helper, arrivalsMethod, peripheral, zeroArgument, "limit must be greater than zero");

		Map<?, ?> runtimeSchedule = asTable(helper, invoke(scheduleMethod, peripheral),
			"getTrainSchedule result");
		require(helper, train.id.toString().equals(runtimeSchedule.get("train_id"))
			&& TRAIN_NAME.equals(runtimeSchedule.get("train_name")),
			"getTrainSchedule returned the wrong train identity");
		require(helper, number(runtimeSchedule, "current_entry").intValue() == 1
			&& number(runtimeSchedule, "entry_count").intValue() == 1
			&& "post_transit".equals(runtimeSchedule.get("state"))
			&& Boolean.FALSE.equals(runtimeSchedule.get("paused")),
			"getTrainSchedule returned inconsistent runtime state");
		require(helper, runtimeSchedule.containsKey("entries") && runtimeSchedule.containsKey("cyclic")
			&& runtimeSchedule.containsKey("departure_predictable"),
			"getTrainSchedule omitted Create schedule data or Railward departure fields");
	}

	private static void requireLuaFunction(GameTestHelper helper, Method method) {
		for (Annotation annotation : method.getDeclaredAnnotations())
			if ("dan200.computercraft.api.lua.LuaFunction".equals(annotation.annotationType().getName()))
				return;
		helper.fail(method.getName() + " was not exported as a CC:Tweaked LuaFunction");
	}

	private static Object invoke(Method method, Object target, Object... arguments)
		throws ReflectiveOperationException {
		try {
			return method.invoke(target, arguments);
		} catch (InvocationTargetException exception) {
			Throwable cause = exception.getCause();
			if (cause instanceof ReflectiveOperationException reflective)
				throw reflective;
			if (cause instanceof RuntimeException runtime)
				throw runtime;
			throw new IllegalStateException(method.getName() + " failed", cause);
		}
	}

	private static void requireLuaFailure(GameTestHelper helper, Method method, Object target, Object argument,
		String expectedMessage) throws IllegalAccessException {
		try {
			method.invoke(target, argument);
			helper.fail(method.getName() + " accepted an invalid argument");
		} catch (InvocationTargetException exception) {
			Throwable cause = exception.getCause();
			require(helper, cause != null
				&& "dan200.computercraft.api.lua.LuaException".equals(cause.getClass().getName())
				&& expectedMessage.equals(cause.getMessage()),
				method.getName() + " returned the wrong Lua error for an invalid argument");
		}
	}

	private static Object firstRow(Map<?, ?> table) {
		Object row = table.get(1);
		return row == null ? table.get(1.0) : row;
	}

	private static Map<?, ?> asTable(GameTestHelper helper, Object value, String description) {
		require(helper, value instanceof Map<?, ?>, description + " was not a Lua table");
		return (Map<?, ?>) value;
	}

	private static Number number(Map<?, ?> table, String key) {
		Object value = table.get(key);
		if (!(value instanceof Number number))
			throw new IllegalStateException(key + " was not numeric: " + value);
		return number;
	}

	private static void require(GameTestHelper helper, boolean condition, String message) {
		if (!condition)
			helper.fail(message);
	}
}
