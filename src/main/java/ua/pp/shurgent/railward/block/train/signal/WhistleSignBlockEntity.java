package ua.pp.shurgent.railward.block.train.signal;

import java.util.List;

import com.simibubi.create.content.contraptions.StructureTransform;
import com.simibubi.create.content.trains.track.TrackTargetingBehaviour;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import ua.pp.shurgent.railward.AllEdgePointTypes;
import ua.pp.shurgent.railward.compat.RailwardTransformableBlockEntity;

public class WhistleSignBlockEntity extends SmartBlockEntity implements RailwardTransformableBlockEntity {

	public TrackTargetingBehaviour<WhistleSignEdgePoint> edgePoint;

	public WhistleSignBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
		behaviours.add(edgePoint = new TrackTargetingBehaviour<>(this, AllEdgePointTypes.WHISTLE_SIGN));
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
