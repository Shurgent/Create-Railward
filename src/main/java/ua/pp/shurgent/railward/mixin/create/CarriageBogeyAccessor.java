package ua.pp.shurgent.railward.mixin.create;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import com.simibubi.create.content.trains.bogey.AbstractBogeyBlock;
import com.simibubi.create.content.trains.entity.CarriageBogey;
import com.simibubi.create.content.trains.entity.TravellingPoint;
import com.simibubi.create.foundation.utility.Couple;

@Mixin(value = CarriageBogey.class, remap = false)
public interface CarriageBogeyAccessor {

	@Accessor("type")
	AbstractBogeyBlock<?> railward$getType();

	@Accessor("upsideDown")
	boolean railward$isUpsideDown();

	@Accessor("points")
	Couple<TravellingPoint> railward$getPoints();

}
