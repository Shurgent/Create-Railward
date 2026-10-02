package ua.pp.shurgent.railward;

import com.simibubi.create.AllBogeyStyles;
import com.simibubi.create.Create;
import com.simibubi.create.content.trains.bogey.BogeySizes;
import com.simibubi.create.content.trains.bogey.BogeyStyle;

import ua.pp.shurgent.railward.compat.RailwardComponents;
import ua.pp.shurgent.railward.client.renderer.FreightBogey18_100Renderer;
import ua.pp.shurgent.railward.client.renderer.TripleAxleEqualizedLocomotiveBogeyRenderer;

public class RailwardBogeyStyles {

	public static final BogeyStyle FREIGHT_18_100 = AllBogeyStyles
		.create(Railward.asResource("freight_18_100"),
			Create.asResource(AllBogeyStyles.STANDARD_CYCLE_GROUP))
		.displayName(RailwardComponents.translatable("railward.bogey.style.freight_18_100"))
		.size(BogeySizes.SMALL, () -> FreightBogey18_100Renderer::new, AllBlocks.FREIGHT_BOGEY_18_100)
		.build();

	public static final BogeyStyle TRIPLE_AXLE_EQUALIZED_LOCOMOTIVE = AllBogeyStyles
		.create(Railward.asResource("triple_axle_equalized_locomotive"),
			Create.asResource(AllBogeyStyles.STANDARD_CYCLE_GROUP))
		.displayName(RailwardComponents.translatable("railward.bogey.style.triple_axle_equalized_locomotive"))
		.size(BogeySizes.SMALL, () -> TripleAxleEqualizedLocomotiveBogeyRenderer::new,
			AllBlocks.TRIPLE_AXLE_EQUALIZED_LOCOMOTIVE_BOGEY)
		.build();

	public static void register() {
	}

	private RailwardBogeyStyles() {
	}
}
