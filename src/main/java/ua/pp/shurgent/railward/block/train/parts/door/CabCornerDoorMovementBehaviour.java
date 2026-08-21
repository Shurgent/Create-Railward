package ua.pp.shurgent.railward.block.train.parts.door;

import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.decoration.slidingDoor.SlidingDoorMovementBehaviour;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.phys.Vec3;

/**
 * Uses the side opposite the hood-side panel as the accessible side of this
 * corner door. The door leaf itself is perpendicular to that side, so Create's
 * default door-facing calculation would classify both mirrored variants as the
 * same station side.
 */
public class CabCornerDoorMovementBehaviour extends SlidingDoorMovementBehaviour {

	@Override
	protected Direction getDoorFacing(MovementContext context) {
		Direction doorFacing = context.state.getValue(DoorBlock.FACING);
		DoorHingeSide hinge = context.state.getValue(DoorBlock.HINGE);
		Direction panelSide = hinge == DoorHingeSide.RIGHT
			? doorFacing.getClockWise()
			: doorFacing.getCounterClockWise();
		Direction localDoorSide = panelSide.getOpposite();

		Vec3 worldSide = context.rotation.apply(Vec3.atLowerCornerOf(localDoorSide.getNormal()));
		return Direction.getNearest(worldSide.x, worldSide.y, worldSide.z);
	}
}
