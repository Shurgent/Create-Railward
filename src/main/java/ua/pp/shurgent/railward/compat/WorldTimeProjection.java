package ua.pp.shurgent.railward.compat;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.WeakHashMap;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.world.level.Level;

/**
 * Converts server-tick durations into the units used by a world's day-time
 * clock. Vanilla advances both clocks at the same rate, but time-control mods
 * may advance {@link Level#getDayTime()} more slowly or more quickly than
 * {@link Level#getGameTime()}.
 */
public final class WorldTimeProjection {

	private static final Logger LOGGER = LogUtils.getLogger();
	private static final String B3M_API_CLASS = "sedridor.B3M.api.B3MAPI";
	private static final int MAX_REASONABLE_OBSERVED_RATE = 128;
	private static final long OBSERVATION_WINDOW_TICKS = 2_400;

	private static final Map<Level, Observation> OBSERVATIONS = new WeakHashMap<>();

	private static boolean b3mApiResolved;
	private static boolean b3mApiFailed;
	private static Method b3mIsModEnabled;
	private static Method b3mGetTimeMultiplier;

	private WorldTimeProjection() {
	}

	public static Snapshot capture(Level level, boolean daylightCycle) {
		long dayTime = level.getDayTime();
		long gameTime = level.getGameTime();

		if (!daylightCycle) {
			resetObservation(level, gameTime, dayTime);
			return new Snapshot(dayTime, gameTime, 0, "frozen", null);
		}

		Integer b3mMultiplier = getB3mMultiplier();
		if (b3mMultiplier != null) {
			return new Snapshot(
				dayTime,
				gameTime,
				rateForB3mMultiplier(b3mMultiplier),
				"b3m",
				b3mMultiplier
			);
		}

		ObservedRate observed = observe(level, gameTime, dayTime);
		return new Snapshot(
			dayTime,
			gameTime,
			observed.rate(),
			observed.measured() ? "observed" : "vanilla",
			null
		);
	}

	private static synchronized Integer getB3mMultiplier() {
		resolveB3mApi();
		if (b3mApiFailed || b3mIsModEnabled == null || b3mGetTimeMultiplier == null)
			return null;

		try {
			if (!((Boolean) b3mIsModEnabled.invoke(null)))
				return null;
			return (Integer) b3mGetTimeMultiplier.invoke(null);
		} catch (IllegalAccessException | InvocationTargetException | LinkageError e) {
			b3mApiFailed = true;
			LOGGER.warn("B3M API could not be queried; falling back to observed world-time rate.", e);
			return null;
		}
	}

	private static void resolveB3mApi() {
		if (b3mApiResolved)
			return;
		b3mApiResolved = true;

		try {
			Class<?> apiClass = Class.forName(B3M_API_CLASS, false, WorldTimeProjection.class.getClassLoader());
			b3mIsModEnabled = apiClass.getMethod("isModEnabled");
			b3mGetTimeMultiplier = apiClass.getMethod("getTimeMultiplier");
		} catch (ClassNotFoundException e) {
			// B3M is optional. Other time mods are handled by observation.
		} catch (ReflectiveOperationException | LinkageError e) {
			b3mApiFailed = true;
			LOGGER.warn("Unsupported B3M API; falling back to observed world-time rate.", e);
		}
	}

	private static double rateForB3mMultiplier(int multiplier) {
		if (multiplier == 0)
			return 0;
		if (multiplier > 1)
			return 1.0 / multiplier;
		if (multiplier < -1)
			return Math.abs((double) multiplier);
		return 1;
	}

	private static synchronized ObservedRate observe(Level level, long gameTime, long dayTime) {
		Observation observation = OBSERVATIONS.computeIfAbsent(
			level,
			ignored -> new Observation(gameTime, dayTime)
		);

		long gameDelta = gameTime - observation.lastGameTime;
		long dayDelta = dayTime - observation.lastDayTime;
		observation.lastGameTime = gameTime;
		observation.lastDayTime = dayTime;

		if (gameDelta <= 0)
			return observation.result();

		long maximumDayDelta = saturatingMultiply(gameDelta, MAX_REASONABLE_OBSERVED_RATE);
		if (dayDelta < 0 || dayDelta > maximumDayDelta) {
			// Sleeping and /time commands are discontinuities, not clock rates.
			observation.sampleGameTicks = 0;
			observation.sampleDayTicks = 0;
			return observation.result();
		}

		if (observation.sampleGameTicks >= OBSERVATION_WINDOW_TICKS) {
			observation.sampleGameTicks /= 2;
			observation.sampleDayTicks /= 2;
		}

		observation.sampleGameTicks += gameDelta;
		observation.sampleDayTicks += dayDelta;
		if (observation.sampleGameTicks >= 20) {
			observation.rate =
				(double) observation.sampleDayTicks / observation.sampleGameTicks;
			observation.measured = true;
		}

		return observation.result();
	}

	private static synchronized void resetObservation(Level level, long gameTime, long dayTime) {
		OBSERVATIONS.put(level, new Observation(gameTime, dayTime));
	}

	private static long saturatingMultiply(long value, long multiplier) {
		if (value > Long.MAX_VALUE / multiplier)
			return Long.MAX_VALUE;
		return value * multiplier;
	}

	public record Snapshot(
		long dayTime,
		long gameTime,
		double dayTicksPerServerTick,
		String source,
		Integer b3mMultiplier
	) {

		public long project(long relativeServerTicks) {
			if (relativeServerTicks <= 0)
				return dayTime;

			long dayTicks;
			if (b3mMultiplier != null) {
				dayTicks = projectB3mTicks(relativeServerTicks, b3mMultiplier);
			} else {
				dayTicks = Math.round(relativeServerTicks * dayTicksPerServerTick);
			}
			return dayTime + dayTicks;
		}

		private long projectB3mTicks(long relativeServerTicks, int multiplier) {
			if (multiplier == 0)
				return 0;
			if (multiplier > 1) {
				long endGameTime = gameTime + relativeServerTicks;
				return Math.floorDiv(endGameTime, multiplier)
					- Math.floorDiv(gameTime, multiplier);
			}
			if (multiplier < -1)
				return relativeServerTicks * Math.abs((long) multiplier);
			return relativeServerTicks;
		}
	}

	private record ObservedRate(double rate, boolean measured) {
	}

	private static final class Observation {

		private long lastGameTime;
		private long lastDayTime;
		private long sampleGameTicks;
		private long sampleDayTicks;
		private double rate = 1;
		private boolean measured;

		private Observation(long gameTime, long dayTime) {
			lastGameTime = gameTime;
			lastDayTime = dayTime;
		}

		private ObservedRate result() {
			return new ObservedRate(rate, measured);
		}
	}
}
