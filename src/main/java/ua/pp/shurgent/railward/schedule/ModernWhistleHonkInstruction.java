package ua.pp.shurgent.railward.schedule;

import java.util.List;

import com.simibubi.create.content.trains.schedule.destination.ScheduleInstruction;
import com.simibubi.create.foundation.gui.ModularGuiLineBuilder;
import com.simibubi.create.foundation.utility.Components;
import com.simibubi.create.foundation.utility.Pair;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import ua.pp.shurgent.railward.AllBlocks;

public class ModernWhistleHonkInstruction extends ScheduleInstruction {

	public static final String DURATION_KEY = "Duration";
	public static final int DEFAULT_DURATION_TICKS = 40;
	public static final int MAX_DURATION_TICKS = 100;

	private final boolean secondary;

	public ModernWhistleHonkInstruction(boolean secondary) {
		this.secondary = secondary;
		data.putInt(DURATION_KEY, DEFAULT_DURATION_TICKS);
	}

	public boolean isSecondary() {
		return secondary;
	}

	public int getDurationTicks() {
		return Mth.clamp(intData(DURATION_KEY), 1, MAX_DURATION_TICKS);
	}

	@Override
	public Pair<ItemStack, Component> getSummary() {
		return Pair.of(icon(), Components.translatable(translationKey() + ".summary", getDurationTicks()));
	}

	@Override
	public ResourceLocation getId() {
		return secondary ? RailwardScheduleInstructions.SECONDARY_HONK_ID
			: RailwardScheduleInstructions.PRIMARY_HONK_ID;
	}

	@Override
	public ItemStack getSecondLineIcon() {
		return icon();
	}

	@Override
	public boolean supportsConditions() {
		return false;
	}

	@Override
	public void initConfigurationWidgets(ModularGuiLineBuilder builder) {
		builder.addScrollInput(0, 70, (input, label) -> {
			input.withRange(1, MAX_DURATION_TICKS + 1)
				.withShiftStep(20)
				.titled(Components.translatable("railward.schedule.instruction.honk.duration"));
			label.withSuffix(" t");
		}, DURATION_KEY);
	}

	@Override
	public List<Component> getSecondLineTooltip(int slot) {
		return List.of(Components.translatable("railward.schedule.instruction.honk.duration.tooltip"));
	}

	private ItemStack icon() {
		return AllBlocks.MODERN_WHISTLE.asStack();
	}

	private String translationKey() {
		return secondary ? "railward.schedule.instruction.secondary_honk"
			: "railward.schedule.instruction.primary_honk";
	}
}
