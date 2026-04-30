package ua.pp.shurgent.railward.mixin.create;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.MountedStorageManager;

@Mixin(value = Contraption.class, remap = false)
public interface ContraptionAccessor {

	@Accessor("storage")
	MountedStorageManager railward$getStorage();

}
