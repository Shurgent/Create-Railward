package ua.pp.shurgent.railward.mixin.create;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.ContraptionWorld;

@Mixin(value = ContraptionWorld.class, remap = false)
public interface ContraptionWorldAccessor {

	@Accessor("contraption")
	Contraption railward$getContraption();
}
