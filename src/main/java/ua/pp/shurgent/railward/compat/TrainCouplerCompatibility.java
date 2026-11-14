package ua.pp.shurgent.railward.compat;

import java.util.Set;

import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.trains.entity.CarriageContraption;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraftforge.registries.ForgeRegistries;
import ua.pp.shurgent.railward.block.train.parts.TrainCouplerInteractionBehaviour;

public final class TrainCouplerCompatibility {

	private static final Set<String> SNR_STYLES = Set.of("link", "linkless", "knuckle", "knuckle_split", "screwlink");
	private static final CouplerType RAILWARD = new CouplerType("railward", "default");

	private TrainCouplerCompatibility() {
	}

	public static CouplerType typeOf(BlockState state) {
		ResourceLocation id = ForgeRegistries.BLOCKS.getKey(state.getBlock());
		if (id == null)
			return null;
		Property<?> style = state.getBlock().getStateDefinition().getProperty("style");
		return typeOf(id.toString(), style == null ? "" : propertyValue(state, style));
	}

	public static CouplerType typeOf(String blockId, String style) {
		if ("railward:train/parts/train_coupler".equals(blockId))
			return RAILWARD;
		if (blockId == null || style == null || !SNR_STYLES.contains(style))
			return null;
		return switch (blockId) {
		case "railways:link_and_pin", "railways:headstock", "railways:copycat_headstock" -> new CouplerType("snr", style);
		default -> null;
		};
	}

	public static boolean isCoupler(BlockState state) {
		return typeOf(state) != null;
	}

	public static boolean canCouple(CouplerType first, CouplerType second) {
		return first != null && first.equals(second);
	}

	public static void registerInteractions() {
		for (String path : new String[] { "link_and_pin", "headstock", "copycat_headstock" }) {
			ResourceLocation id = new ResourceLocation("railways", path);
			if (!ForgeRegistries.BLOCKS.containsKey(id))
				continue;
			Block block = ForgeRegistries.BLOCKS.getValue(id);
			RailwardMovementBehaviours.registerInteraction(block,
				new TrainCouplerInteractionBehaviour(RailwardMovementBehaviours.getInteraction(block.defaultBlockState())));
		}
	}

	public static void restoreInteractions(Contraption contraption) {
		if (!(contraption instanceof CarriageContraption))
			return;
		// Older saved trains do not list decorative SnR blocks as interactors.
		for (var info : contraption.getBlocks().values()) {
			if (!isCoupler(info.state))
				continue;
			var interaction = RailwardMovementBehaviours.getInteraction(info.state);
			if (interaction instanceof TrainCouplerInteractionBehaviour)
				contraption.getInteractors().putIfAbsent(info.pos, interaction);
		}
	}

	private static <T extends Comparable<T>> String propertyValue(BlockState state, Property<T> property) {
		return property.getName(state.getValue(property));
	}

	public record CouplerType(String family, String style) {
		public boolean isRailward() {
			return this.equals(RAILWARD);
		}
	}
}
