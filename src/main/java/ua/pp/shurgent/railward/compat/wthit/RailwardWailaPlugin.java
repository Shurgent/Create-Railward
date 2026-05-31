package ua.pp.shurgent.railward.compat.wthit;

import mcp.mobius.waila.api.IRegistrar;
import mcp.mobius.waila.api.IWailaPlugin;
import mcp.mobius.waila.api.WailaPlugin;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

@WailaPlugin(id = "railward:train_parts")
public class RailwardWailaPlugin implements IWailaPlugin {

	@Override
	public void register(IRegistrar registrar) {
		DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> RailwardWailaClientPlugin.register(registrar));
	}

}
