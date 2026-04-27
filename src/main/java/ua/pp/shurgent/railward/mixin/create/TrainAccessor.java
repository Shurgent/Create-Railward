package ua.pp.shurgent.railward.mixin.create;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import com.simibubi.create.content.trains.entity.Train;

@Mixin(value = Train.class, remap = false)
public interface TrainAccessor {

	@Accessor("stress")
	double[] railward$getStress();

	@Accessor("stress")
	void railward$setStress(double[] stress);

}
