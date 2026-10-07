package ua.pp.shurgent.railward.compat;

//? if create6 {
/*import com.simibubi.create.api.behaviour.interaction.MovingInteractionBehaviour;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
*///?} else {
import com.simibubi.create.AllInteractionBehaviours;
import com.simibubi.create.AllMovementBehaviours;
import com.simibubi.create.content.contraptions.behaviour.MovementBehaviour;
import com.simibubi.create.content.contraptions.behaviour.MovingInteractionBehaviour;
//?}
import com.tterrag.registrate.util.nullness.NonNullConsumer;

import net.minecraft.world.level.block.Block;

public final class RailwardMovementBehaviours {

	private RailwardMovementBehaviours() {
	}

	public static <B extends Block> NonNullConsumer<? super B> movementBehaviour(MovementBehaviour behaviour) {
		//? if create6
		/*return MovementBehaviour.movementBehaviour(behaviour);
		*///? if !create6
		return AllMovementBehaviours.movementBehaviour(behaviour);
	}

	public static <B extends Block> NonNullConsumer<? super B> interactionBehaviour(
		MovingInteractionBehaviour behaviour) {
		//? if create6
		/*return MovingInteractionBehaviour.interactionBehaviour(behaviour);
		*///? if !create6
		return AllInteractionBehaviours.interactionBehaviour(behaviour);
	}

	public static void registerMovement(Block block, MovementBehaviour behaviour) {
		//? if create6
		/*MovementBehaviour.REGISTRY.register(block, behaviour);
		*///? if !create6
		AllMovementBehaviours.registerBehaviour(block, behaviour);
	}
}
