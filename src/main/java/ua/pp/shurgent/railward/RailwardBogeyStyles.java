package ua.pp.shurgent.railward;

import com.simibubi.create.AllBogeyStyles;
import com.simibubi.create.Create;
import com.simibubi.create.content.trains.bogey.BogeySizes;
import com.simibubi.create.content.trains.bogey.BogeyStyle;
//? if create6
/*import com.simibubi.create.content.trains.bogey.BogeyStyle.SizeRenderer;
*///? if create6
/*import ua.pp.shurgent.railward.client.renderer.RailwardBogeyVisual;
*/
import net.minecraft.resources.ResourceLocation;

import ua.pp.shurgent.railward.compat.RailwardComponents;
import ua.pp.shurgent.railward.client.renderer.FreightBogey18_100Renderer;
import ua.pp.shurgent.railward.client.renderer.TripleAxleEqualizedLocomotiveBogeyRenderer;

public class RailwardBogeyStyles {

	public static final BogeyStyle FREIGHT_18_100 = createFreight();

	public static final BogeyStyle TRIPLE_AXLE_EQUALIZED_LOCOMOTIVE = createTripleAxle();

	private static BogeyStyle createFreight() {
		//? if create6 {
		/*return new BogeyStyle.Builder(Railward.asResource("freight_18_100"), standardCycleGroup())
			.displayName(RailwardComponents.translatable("railward.bogey.style.freight_18_100"))
			.size(BogeySizes.SMALL, AllBlocks.FREIGHT_BOGEY_18_100,
				() -> () -> new SizeRenderer(new FreightBogey18_100Renderer(),
					(context, width, inContraption) -> new RailwardBogeyVisual(context,
						AllPartialModels.BOGEY_18_100_FRAME, AllPartialModels.BOGEY_18_100_WHEELSET,
						11.25f / 16f, 13f / 16f, -1, 1)))
			.build();
		*///?} else {
		return AllBogeyStyles.create(Railward.asResource("freight_18_100"), standardCycleGroup())
			.displayName(RailwardComponents.translatable("railward.bogey.style.freight_18_100"))
			.size(BogeySizes.SMALL, () -> FreightBogey18_100Renderer::new, AllBlocks.FREIGHT_BOGEY_18_100)
			.build();
		//?}
	}

	private static BogeyStyle createTripleAxle() {
		//? if create6 {
		/*return new BogeyStyle.Builder(Railward.asResource("triple_axle_equalized_locomotive"), standardCycleGroup())
			.displayName(RailwardComponents.translatable("railward.bogey.style.triple_axle_equalized_locomotive"))
			.size(BogeySizes.SMALL, AllBlocks.TRIPLE_AXLE_EQUALIZED_LOCOMOTIVE_BOGEY,
				() -> () -> new SizeRenderer(new TripleAxleEqualizedLocomotiveBogeyRenderer(),
					(context, width, inContraption) -> new RailwardBogeyVisual(context,
						AllPartialModels.BOGEY_TRIPLE_AXLE_EQUALIZED_LOCOMOTIVE_FRAME,
						AllPartialModels.BOGEY_TRIPLE_AXLE_EQUALIZED_LOCOMOTIVE_WHEELSET,
						12f / 16f, 13.5f / 16f, -2, 0, 2)))
			.build();
		*///?} else {
		return AllBogeyStyles.create(Railward.asResource("triple_axle_equalized_locomotive"), standardCycleGroup())
			.displayName(RailwardComponents.translatable("railward.bogey.style.triple_axle_equalized_locomotive"))
			.size(BogeySizes.SMALL, () -> TripleAxleEqualizedLocomotiveBogeyRenderer::new,
				AllBlocks.TRIPLE_AXLE_EQUALIZED_LOCOMOTIVE_BOGEY)
			.build();
		//?}
	}

	public static void register() {
	}

	private static ResourceLocation standardCycleGroup() {
		//? if create6
		/*return AllBogeyStyles.STANDARD_CYCLE_GROUP;
		*///? if !create6
		return Create.asResource(AllBogeyStyles.STANDARD_CYCLE_GROUP);
	}

	private RailwardBogeyStyles() {
	}
}
