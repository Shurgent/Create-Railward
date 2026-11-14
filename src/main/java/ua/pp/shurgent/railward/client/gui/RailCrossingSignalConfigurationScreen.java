package ua.pp.shurgent.railward.client.gui;

import java.util.Locale;

import net.minecraft.core.BlockPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import ua.pp.shurgent.railward.RailwardPackets;
import ua.pp.shurgent.railward.block.train.signal.RailCrossingSignalBlockEntity;
import ua.pp.shurgent.railward.block.train.signal.RailCrossingSignalBlockEntity.CrossingSign;
import ua.pp.shurgent.railward.block.train.signal.RailCrossingSignalBlockEntity.CrossingSound;
import ua.pp.shurgent.railward.client.gui.TwoDropdownConfigurationScreen.Dropdown;
import ua.pp.shurgent.railward.compat.RailwardComponents;
import ua.pp.shurgent.railward.network.RailCrossingSignalConfigurationPacket;

@OnlyIn(Dist.CLIENT)
public final class RailCrossingSignalConfigurationScreen {

	private static final String PREFIX = "railward.rail_crossing_signal.";

	private RailCrossingSignalConfigurationScreen() {
	}

	public static void open(RailCrossingSignalBlockEntity signal) {
		BlockPos pos = signal.getBlockPos();
		TwoDropdownConfigurationScreen.open(RailwardComponents.translatable(PREFIX + "configuration"),
			new Dropdown<>(PREFIX + "sign", CrossingSign.values(), signal.getSign(),
				value -> PREFIX + "sign." + value.name().toLowerCase(Locale.ROOT)),
			new Dropdown<>(PREFIX + "sound", CrossingSound.values(), signal.getSound(),
				value -> PREFIX + "sound." + value.name().toLowerCase(Locale.ROOT)),
			(sign, sound) -> RailwardPackets.getChannel().sendToServer(
				new RailCrossingSignalConfigurationPacket(pos, sign, sound)));
	}
}
