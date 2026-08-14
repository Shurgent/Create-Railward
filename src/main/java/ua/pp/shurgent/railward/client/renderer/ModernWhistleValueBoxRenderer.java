package ua.pp.shurgent.railward.client.renderer;

import java.util.List;

import com.simibubi.create.CreateClient;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import com.simibubi.create.foundation.utility.Components;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.phys.BlockHitResult;
import ua.pp.shurgent.railward.block.train.parts.ModernWhistleBlockEntity;

public class ModernWhistleValueBoxRenderer {

	private ModernWhistleValueBoxRenderer() {
	}

	public static void tick() {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.level == null || !(minecraft.hitResult instanceof BlockHitResult hitResult))
			return;
		if (!(minecraft.level.getBlockEntity(hitResult.getBlockPos()) instanceof ModernWhistleBlockEntity whistle))
			return;

		ScrollValueBehaviour behaviour = BlockEntityBehaviour.get(minecraft.level, whistle.getBlockPos(),
			ScrollValueBehaviour.TYPE);
		if (behaviour == null || !behaviour.testHit(hitResult.getLocation()))
			return;

		List<MutableComponent> tooltip = List.of(
			Components.translatable("railward.modern_whistle.configuration"),
			Components.translatable("railward.modern_whistle.right_click_to_configure"));
		CreateClient.VALUE_SETTINGS_HANDLER.showHoverTip(tooltip);
	}
}
