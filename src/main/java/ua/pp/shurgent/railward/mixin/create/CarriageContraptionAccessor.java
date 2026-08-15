package ua.pp.shurgent.railward.mixin.create;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import com.simibubi.create.content.contraptions.MountedStorageManager;
import com.simibubi.create.content.trains.entity.CarriageContraption;

@Mixin(value = CarriageContraption.class, remap = false)
public interface CarriageContraptionAccessor {

	@Accessor("storageProxy")
	MountedStorageManager railward$getStorageProxy();

	@Accessor("forwardControls")
	void railward$setForwardControls(boolean value);

	@Accessor("backwardControls")
	void railward$setBackwardControls(boolean value);

}
