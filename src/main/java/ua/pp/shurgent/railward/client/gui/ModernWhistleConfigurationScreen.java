package ua.pp.shurgent.railward.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.gui.AbstractSimiScreen;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.widget.IconButton;

import net.minecraft.client.Minecraft;
//? if >=1.20.1
/*import net.minecraft.client.gui.GuiGraphics;*/
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import ua.pp.shurgent.railward.RailwardPackets;
import ua.pp.shurgent.railward.block.train.parts.ModernWhistleBlockEntity;
import ua.pp.shurgent.railward.block.train.parts.ModernWhistleSoundType;
import ua.pp.shurgent.railward.compat.RailwardComponents;
import ua.pp.shurgent.railward.network.ModernWhistleConfigurationPacket;

@OnlyIn(Dist.CLIENT)
public class ModernWhistleConfigurationScreen extends AbstractSimiScreen {

	private static final int WINDOW_WIDTH = 280;
	private static final int WINDOW_HEIGHT = 210;
	private static final int FIELD_X = 130;
	private static final int FIELD_WIDTH = 130;
	private static final int FIELD_HEIGHT = 22;
	private static final int PRIMARY_FIELD_Y = 48;
	private static final int SECONDARY_FIELD_Y = 86;
	private static final int OPTION_HEIGHT = 20;
	private static final int PRIMARY_DROPDOWN = 0;
	private static final int SECONDARY_DROPDOWN = 1;

	private final BlockPos blockPos;
	private ModernWhistleSoundType primary;
	private ModernWhistleSoundType secondary;
	private int openDropdown = -1;

	private ModernWhistleConfigurationScreen(BlockPos blockPos, ModernWhistleSoundType primary,
		ModernWhistleSoundType secondary) {
		super(RailwardComponents.translatable("railward.modern_whistle.configuration"));
		this.blockPos = blockPos.immutable();
		this.primary = primary;
		this.secondary = secondary;
	}

	public static void open(ModernWhistleBlockEntity whistle) {
		Minecraft.getInstance()
			.setScreen(new ModernWhistleConfigurationScreen(whistle.getBlockPos(), whistle.getPrimarySoundType(),
				whistle.getSecondarySoundType()));
	}

	@Override
	protected void init() {
		setWindowSize(WINDOW_WIDTH, WINDOW_HEIGHT);
		super.init();

		int buttonY = guiTop + WINDOW_HEIGHT - 28;
		IconButton cancelButton = new IconButton(guiLeft + WINDOW_WIDTH / 2 - 20, buttonY,
			AllIcons.I_DISABLE);
		cancelButton.setToolTip(RailwardComponents.translatable("railward.modern_whistle.cancel"));
		cancelButton.withCallback(this::onClose);

		IconButton applyButton = new IconButton(guiLeft + WINDOW_WIDTH / 2 + 2, buttonY, AllIcons.I_CONFIRM);
		applyButton.setToolTip(RailwardComponents.translatable("railward.modern_whistle.apply"));
		applyButton.withCallback(this::applyAndClose);
		addRenderableWidgets(cancelButton, applyButton);
	}

	@Override
	protected void renderWindow(PoseStack gui, int mouseX, int mouseY, float partialTicks) {
		drawPanel(gui);

		int titleX = guiLeft + (WINDOW_WIDTH - font.width(title)) / 2;
		font.draw(gui, title, titleX, guiTop + 16, 0xF3E7CA);
		font.draw(gui, RailwardComponents.translatable("railward.modern_whistle.primary"), guiLeft + 20,
			guiTop + PRIMARY_FIELD_Y + 7, 0xE8E8E8);
		font.draw(gui, RailwardComponents.translatable("railward.modern_whistle.secondary"), guiLeft + 20,
			guiTop + SECONDARY_FIELD_Y + 7, 0xE8E8E8);

		drawDropdownField(gui, mouseX, mouseY, PRIMARY_DROPDOWN, PRIMARY_FIELD_Y, primary);
		drawDropdownField(gui, mouseX, mouseY, SECONDARY_DROPDOWN, SECONDARY_FIELD_Y, secondary);
	}

	@Override
	protected void renderWindowForeground(PoseStack gui, int mouseX, int mouseY, float partialTicks) {
		if (openDropdown != -1)
			drawDropdownOptions(gui, mouseX, mouseY, openDropdown);
		super.renderWindowForeground(gui, mouseX, mouseY, partialTicks);
	}

	private void drawPanel(PoseStack gui) {
		fill(gui, guiLeft, guiTop, guiLeft + WINDOW_WIDTH, guiTop + WINDOW_HEIGHT, 0xFF171C20);
		fill(gui, guiLeft + 1, guiTop + 1, guiLeft + WINDOW_WIDTH - 1, guiTop + WINDOW_HEIGHT - 1, 0xFF9B6A2F);
		fill(gui, guiLeft + 3, guiTop + 3, guiLeft + WINDOW_WIDTH - 3, guiTop + WINDOW_HEIGHT - 3, 0xFF252D32);
		fill(gui, guiLeft + 3, guiTop + 36, guiLeft + WINDOW_WIDTH - 3, guiTop + 38, 0xFF9B6A2F);
	}

	private void drawDropdownField(PoseStack gui, int mouseX, int mouseY, int dropdown, int relativeY,
		ModernWhistleSoundType value) {
		int x = guiLeft + FIELD_X;
		int y = guiTop + relativeY;
		boolean hovered = isInside(mouseX, mouseY, x, y, FIELD_WIDTH, FIELD_HEIGHT);
		boolean open = openDropdown == dropdown;
		int background = open ? 0xFF36454D : hovered ? 0xFF303D44 : 0xFF1B2226;

		fill(gui, x, y, x + FIELD_WIDTH, y + FIELD_HEIGHT, open ? 0xFFD29B4C : 0xFF865D2C);
		fill(gui, x + 1, y + 1, x + FIELD_WIDTH - 1, y + FIELD_HEIGHT - 1, background);
		Component name = RailwardComponents.translatable(value.getTranslationKey());
		font.draw(gui, name, x + 8, y + 7, 0xF2F2F2);
		drawDownArrow(gui, x + FIELD_WIDTH - 16, y + 9, open ? 0xFFF7C66B : 0xFFC7CDD0);
	}

	private void drawDropdownOptions(PoseStack gui, int mouseX, int mouseY, int dropdown) {
		int x = guiLeft + FIELD_X;
		int fieldY = dropdown == PRIMARY_DROPDOWN ? PRIMARY_FIELD_Y : SECONDARY_FIELD_Y;
		int y = guiTop + fieldY + FIELD_HEIGHT;
		ModernWhistleSoundType[] values = ModernWhistleSoundType.values();

		gui.pushPose();
		gui.translate(0, 0, 300);
		fill(gui, x, y, x + FIELD_WIDTH, y + values.length * OPTION_HEIGHT + 2, 0xFFD29B4C);
		for (int i = 0; i < values.length; i++) {
			int optionY = y + 1 + i * OPTION_HEIGHT;
			boolean hovered = isInside(mouseX, mouseY, x + 1, optionY, FIELD_WIDTH - 2, OPTION_HEIGHT);
			ModernWhistleSoundType selected = dropdown == PRIMARY_DROPDOWN ? primary : secondary;
			int background = hovered ? 0xFF3D515B : values[i] == selected ? 0xFF303D44 : 0xFF1B2226;
			fill(gui, x + 1, optionY, x + FIELD_WIDTH - 1, optionY + OPTION_HEIGHT, background);
			font.draw(gui, RailwardComponents.translatable(values[i].getTranslationKey()), x + 8, optionY + 6, 0xF2F2F2);
		}
		gui.popPose();
	}

	private void drawDownArrow(PoseStack gui, int x, int y, int color) {
		fill(gui, x, y, x + 7, y + 1, color);
		fill(gui, x + 1, y + 1, x + 6, y + 2, color);
		fill(gui, x + 2, y + 2, x + 5, y + 3, color);
		fill(gui, x + 3, y + 3, x + 4, y + 4, color);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button != 0)
			return super.mouseClicked(mouseX, mouseY, button);

		if (openDropdown != -1) {
			int option = getClickedOption(mouseX, mouseY, openDropdown);
			if (option != -1) {
				ModernWhistleSoundType selected = ModernWhistleSoundType.values()[option];
				if (openDropdown == PRIMARY_DROPDOWN)
					primary = selected;
				else
					secondary = selected;
				openDropdown = -1;
				return true;
			}
		}

		if (isInsideField(mouseX, mouseY, PRIMARY_FIELD_Y)) {
			openDropdown = openDropdown == PRIMARY_DROPDOWN ? -1 : PRIMARY_DROPDOWN;
			return true;
		}
		if (isInsideField(mouseX, mouseY, SECONDARY_FIELD_Y)) {
			openDropdown = openDropdown == SECONDARY_DROPDOWN ? -1 : SECONDARY_DROPDOWN;
			return true;
		}

		openDropdown = -1;
		return super.mouseClicked(mouseX, mouseY, button);
	}

	private int getClickedOption(double mouseX, double mouseY, int dropdown) {
		int fieldY = dropdown == PRIMARY_DROPDOWN ? PRIMARY_FIELD_Y : SECONDARY_FIELD_Y;
		int x = guiLeft + FIELD_X;
		int y = guiTop + fieldY + FIELD_HEIGHT + 1;
		for (int i = 0; i < ModernWhistleSoundType.values().length; i++)
			if (isInside(mouseX, mouseY, x + 1, y + i * OPTION_HEIGHT, FIELD_WIDTH - 2, OPTION_HEIGHT))
				return i;
		return -1;
	}

	private boolean isInsideField(double mouseX, double mouseY, int relativeY) {
		return isInside(mouseX, mouseY, guiLeft + FIELD_X, guiTop + relativeY, FIELD_WIDTH, FIELD_HEIGHT);
	}

	private static boolean isInside(double mouseX, double mouseY, int x, int y, int width, int height) {
		return mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
	}

	private void applyAndClose() {
		RailwardPackets.getChannel()
			.sendToServer(new ModernWhistleConfigurationPacket(blockPos, primary, secondary));
		onClose();
	}
}
