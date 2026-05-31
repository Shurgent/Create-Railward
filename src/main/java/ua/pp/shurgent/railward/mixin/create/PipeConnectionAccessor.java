package ua.pp.shurgent.railward.mixin.create;

import java.util.Optional;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import com.simibubi.create.content.fluids.FlowSource;
import com.simibubi.create.content.fluids.PipeConnection;
import com.simibubi.create.content.fluids.PipeConnection.Flow;

@Mixin(value = PipeConnection.class, remap = false)
public interface PipeConnectionAccessor {

	@Accessor("flow")
	Optional<Flow> railward$getFlow();

	@Accessor("source")
	Optional<FlowSource> railward$getSource();

}
