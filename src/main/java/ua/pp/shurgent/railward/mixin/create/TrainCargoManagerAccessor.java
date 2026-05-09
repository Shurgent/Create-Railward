package ua.pp.shurgent.railward.mixin.create;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import com.simibubi.create.content.contraptions.minecart.TrainCargoManager;

@Mixin(value = TrainCargoManager.class, remap = false)
public interface TrainCargoManagerAccessor {

	@Invoker("changeDetected")
	void railward$changeDetected();

}
