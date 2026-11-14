package ua.pp.shurgent.railward.block.train.signal;

import java.util.List;

import com.simibubi.create.content.contraptions.StructureTransform;
import com.simibubi.create.content.trains.track.TrackTargetingBehaviour;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.content.equipment.goggles.IHaveGoggleInformation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import ua.pp.shurgent.railward.AllEdgePointTypes;
import ua.pp.shurgent.railward.block.train.signal.RelayCaseEdgePoint.TrackMarker;
import ua.pp.shurgent.railward.compat.RailwardComponents;
import ua.pp.shurgent.railward.compat.RailwardTransformableBlockEntity;

public class RelayCaseBlockEntity extends SmartBlockEntity implements RailwardTransformableBlockEntity, IHaveGoggleInformation {

	public TrackTargetingBehaviour<RelayCaseEdgePoint> edgePoint;
	private TrackMarker trackMarker = TrackMarker.HIDDEN;

	public RelayCaseBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
		behaviours.add(edgePoint = new TrackTargetingBehaviour<>(this, AllEdgePointTypes.RELAY_CASE));
	}

	@Override
	public void tick() {
		super.tick();
		if (level == null || level.isClientSide || isRemoved())
			return;
		RelayCaseEdgePoint point = edgePoint.getEdgePoint();
		TrackMarker marker = point == null ? TrackMarker.SINGLE : point.getTrackMarker(worldPosition);
		if (trackMarker != marker) {
			trackMarker = marker;
			sendData();
		}
		refreshState();
	}

	public TrackMarker getTrackMarker() {
		return trackMarker;
	}

	@Override
	protected void write(CompoundTag tag, boolean clientPacket) {
		super.write(tag, clientPacket);
		if (clientPacket)
			tag.putString("TrackMarker", trackMarker.name());
	}

	@Override
	protected void read(CompoundTag tag, boolean clientPacket) {
		super.read(tag, clientPacket);
		if (clientPacket)
			trackMarker = switch (tag.getString("TrackMarker")) {
				case "SINGLE" -> TrackMarker.SINGLE;
				case "DOUBLE" -> TrackMarker.DOUBLE;
				default -> TrackMarker.HIDDEN;
			};
	}

	public void refreshState() {
		if (level == null || level.isClientSide || isRemoved())
			return;
		RelayCaseEdgePoint point = edgePoint == null ? null : edgePoint.getEdgePoint();
		boolean present = point != null && point.isTrackOccupied(edgePoint.getTargetDirection() == AxisDirection.POSITIVE);
		boolean occupied = present || level.hasNeighborSignal(worldPosition);
		BlockState state = getBlockState();
		if (state.getValue(RelayCaseBlock.TRAIN_PRESENT) == present && state.getValue(RelayCaseBlock.OCCUPIED) == occupied)
			return;
		level.setBlock(worldPosition, state.setValue(RelayCaseBlock.TRAIN_PRESENT, present)
			.setValue(RelayCaseBlock.OCCUPIED, occupied), 3);
	}

	/** Authoritative state for linked crossing equipment; also synced through blockstate. */
	public boolean isOccupied() {
		return getBlockState().getValue(RelayCaseBlock.OCCUPIED);
	}

	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean sneaking) {
		tooltip.add(RailwardComponents.translatable("railward.relay_case.header"));
		tooltip.add(RailwardComponents.translatable(isOccupied() ? "railward.relay_case.occupied" : "railward.relay_case.free"));
		if (edgePoint.getEdgePoint() == null)
			tooltip.add(RailwardComponents.translatable("railward.relay_case.unbound"));
		return true;
	}

	@Override
	protected AABB createRenderBoundingBox() {
		return new AABB(worldPosition, edgePoint.getGlobalPosition()).inflate(2);
	}

	@Override
	public void transform(StructureTransform transform) {
		//? if create6
		/*edgePoint.transform(this, transform);
		*///? if !create6
		edgePoint.transform(transform);
	}
}
