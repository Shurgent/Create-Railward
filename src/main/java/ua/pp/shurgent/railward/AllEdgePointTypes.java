package ua.pp.shurgent.railward;

import com.simibubi.create.content.trains.graph.EdgePointType;

import ua.pp.shurgent.railward.block.train.signal.SpeedSignEdgePoint;
import ua.pp.shurgent.railward.block.train.signal.WhistleSignEdgePoint;
import ua.pp.shurgent.railward.block.train.signal.RelayCaseEdgePoint;

public class AllEdgePointTypes {

	public static final EdgePointType<WhistleSignEdgePoint> WHISTLE_SIGN =
		EdgePointType.register(Railward.asResource("whistle_sign"), WhistleSignEdgePoint::new);
	public static final EdgePointType<SpeedSignEdgePoint> SPEED_SIGN =
		EdgePointType.register(Railward.asResource("speed_sign"), SpeedSignEdgePoint::new);
	public static final EdgePointType<RelayCaseEdgePoint> RELAY_CASE =
		EdgePointType.register(Railward.asResource("relay_case"), RelayCaseEdgePoint::new);

	public static void register() {
	}
}
