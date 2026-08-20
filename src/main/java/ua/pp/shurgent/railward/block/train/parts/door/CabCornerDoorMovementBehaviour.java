package ua.pp.shurgent.railward.block.train.parts.door;

import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.decoration.slidingDoor.SlidingDoorMovementBehaviour;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.phys.Vec3;

/**
 * Uses the hood-side panel to determine which side of a train this corner door
 * belongs to. The door leaf itself is perpendicular to that side, so Create's
 * default door-facing calculation would classify both mirrored variants as the
 * same station side.
 */
public class CabCornerDoorMovementBehaviour extends SlidingDoorMovementBehaviour {

	@Override
	protected Direction getDoorFacing(MovementContext context) {
		Direction doorFacing = context.state.getValue(DoorBlock.FACING);
		DoorHingeSide hinge = context.state.getValue(DoorBlock.HINGE);
		Direction localSide = hinge == DoorHingeSide.RIGHT
			? doorFacing.getClockWise()
			: doorFacing.getCounterClockWise();

		Vec3 worldSide = context.rotation.apply(Vec3.atLowerCornerOf(localSide.getNormal()));
		return Direction.getNearest(worldSide.x, worldSide.y, worldSide.z);
	}
}
