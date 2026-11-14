package ua.pp.shurgent.railward.client.gui;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import ua.pp.shurgent.railward.RailwardPackets;
import ua.pp.shurgent.railward.block.train.parts.ModernWhistleBlockEntity;
import ua.pp.shurgent.railward.block.train.parts.ModernWhistleSoundType;
import ua.pp.shurgent.railward.client.gui.TwoDropdownConfigurationScreen.Dropdown;
import ua.pp.shurgent.railward.compat.RailwardComponents;
import ua.pp.shurgent.railward.network.ModernWhistleConfigurationPacket;

@OnlyIn(Dist.CLIENT)
public final class ModernWhistleConfigurationScreen {

	private ModernWhistleConfigurationScreen() {
	}

	public static void open(ModernWhistleBlockEntity whistle) {
		TwoDropdownConfigurationScreen.open(RailwardComponents.translatable("railward.modern_whistle.configuration"),
			new Dropdown<>("railward.modern_whistle.primary", ModernWhistleSoundType.values(),
				whistle.getPrimarySoundType(), ModernWhistleSoundType::getTranslationKey),
			new Dropdown<>("railward.modern_whistle.secondary", ModernWhistleSoundType.values(),
				whistle.getSecondarySoundType(), ModernWhistleSoundType::getTranslationKey),
			(primary, secondary) -> RailwardPackets.getChannel().sendToServer(
				new ModernWhistleConfigurationPacket(whistle.getBlockPos(), primary, secondary)));
	}
}
