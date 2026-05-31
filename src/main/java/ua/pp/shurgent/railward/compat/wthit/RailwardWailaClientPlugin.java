package ua.pp.shurgent.railward.compat.wthit;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;

import mcp.mobius.waila.api.IBlockComponentProvider;
import mcp.mobius.waila.api.IEntityComponentProvider;
import mcp.mobius.waila.api.IRegistrar;
import mcp.mobius.waila.api.TooltipPosition;
import ua.pp.shurgent.railward.block.train.parts.BulkCargoContainerBlockEntity;
import ua.pp.shurgent.railward.block.train.parts.CisternBlockEntity;

public final class RailwardWailaClientPlugin {

	private RailwardWailaClientPlugin() {
	}

	public static void register(IRegistrar registrar) {
		BulkCargoContainerWailaProvider provider = new BulkCargoContainerWailaProvider();
		registrar.addComponent((IBlockComponentProvider) provider, TooltipPosition.BODY,
			BulkCargoContainerBlockEntity.class);
		registrar.addComponent((IEntityComponentProvider) provider, TooltipPosition.BODY,
			AbstractContraptionEntity.class);

		CisternWailaProvider cisternProvider = new CisternWailaProvider();
		registrar.addComponent((IBlockComponentProvider) cisternProvider, TooltipPosition.BODY,
			CisternBlockEntity.class);
		registrar.addComponent((IEntityComponentProvider) cisternProvider, TooltipPosition.BODY,
			AbstractContraptionEntity.class);
	}

}
