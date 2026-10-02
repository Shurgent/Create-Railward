package ua.pp.shurgent.railward.block.train.signal;

import java.util.OptionalInt;

import com.simibubi.create.AllPackets;
import com.simibubi.create.content.trains.TrainHUDUpdatePacket;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.graph.TrackNode;
import com.simibubi.create.content.trains.signal.TrackEdgePoint;
import com.simibubi.create.foundation.utility.Couple;
import com.simibubi.create.foundation.utility.Pair;

import net.minecraft.util.Mth;
import net.minecraftforge.network.PacketDistributor;

public class SpeedSignTrainHandler {

	private static final int THROTTLE_STEPS = 18;
	private static final double KMH_PER_BLOCK_PER_TICK = 72;

	private SpeedSignTrainHandler() {
	}

	public static void onFrontEdgePoint(Train train, Pair<TrackEdgePoint, Couple<TrackNode>> couple) {
		if (!(couple.getFirst() instanceof SpeedSignEdgePoint speedSign))
			return;

		TrackNode approachSide = couple.getSecond()
			.getSecond();
		OptionalInt speedLimitKmh = speedSign.getSpeedLimitKmh(approachSide);
		if (speedLimitKmh.isEmpty())
			return;

		applyThrottleLimit(train, speedLimitKmh.getAsInt());
	}

	private static void applyThrottleLimit(Train train, int speedLimitKmh) {
		double maxSpeedKmh = train.maxSpeed() * KMH_PER_BLOCK_PER_TICK;
		if (maxSpeedKmh <= 0)
			return;

		int step = Mth.clamp((int) Math.round(speedLimitKmh / maxSpeedKmh * THROTTLE_STEPS), 1, THROTTLE_STEPS);
		double throttle = step / (double) THROTTLE_STEPS;
		train.throttle = throttle;
		clampTargetSpeed(train, train.maxSpeed() * throttle);
		syncThrottle(train);
	}

	private static void clampTargetSpeed(Train train, double maxSpeed) {
		if (Math.abs(train.targetSpeed) <= maxSpeed)
			return;
		train.targetSpeed = Math.copySign(maxSpeed, train.targetSpeed);
	}

	private static void syncThrottle(Train train) {
		AllPackets.getChannel()
			.send(PacketDistributor.ALL.noArg(), new TrainHUDUpdatePacket(train));
	}
}
