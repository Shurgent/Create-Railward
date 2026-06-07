package ua.pp.shurgent.railward.block.train.signal;

import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.graph.TrackNode;
import com.simibubi.create.content.trains.signal.TrackEdgePoint;
import com.simibubi.create.foundation.utility.Couple;
import com.simibubi.create.foundation.utility.Pair;

import net.minecraftforge.network.PacketDistributor;
import ua.pp.shurgent.railward.RailwardPackets;
import ua.pp.shurgent.railward.network.WhistleSignHonkPacket;

public class WhistleSignTrainHandler {

	private WhistleSignTrainHandler() {
	}

	public static void onFrontEdgePoint(Train train, Pair<TrackEdgePoint, Couple<TrackNode>> couple) {
		if (!(couple.getFirst() instanceof WhistleSignEdgePoint whistleSign))
			return;
		if (!isNpcControlled(train))
			return;

		TrackNode approachSide = couple.getSecond()
			.getSecond();
		if (!whistleSign.canTriggerFrom(approachSide))
			return;

		RailwardPackets.getChannel()
			.send(PacketDistributor.ALL.noArg(), new WhistleSignHonkPacket(train));
	}

	private static boolean isNpcControlled(Train train) {
		return train.runtime != null
			&& train.runtime.getSchedule() != null
			&& !train.runtime.paused
			&& train.navigation != null
			&& train.navigation.isActive();
	}
}
