package ua.pp.shurgent.railward.client;

import org.lwjgl.glfw.GLFW;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.actors.trainControls.ControlsHandler;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraftforge.event.TickEvent;
import ua.pp.shurgent.railward.RailwardPackets;
import ua.pp.shurgent.railward.block.train.parts.ModernTrainControllerBlock;
import ua.pp.shurgent.railward.network.ModernTrainControllerReverserPacket;

public final class ModernTrainControllerClient {

	public static final KeyMapping REVERSER = new KeyMapping(
		"key.railward.train_reverser", GLFW.GLFW_KEY_R, "key.categories.railward");

	private ModernTrainControllerClient() {
	}

	public static void onClientTick(TickEvent.ClientTickEvent event) {
		if (event.phase != TickEvent.Phase.END)
			return;
		while (REVERSER.consumeClick()) {
			if (Minecraft.getInstance().screen == null)
				tryToggleReverser();
		}
	}

	private static void tryToggleReverser() {
		AbstractContraptionEntity entity = ControlsHandler.getContraption();
		BlockPos controlsPos = ControlsHandler.getControlsPos();
		if (entity == null || controlsPos == null || entity.getContraption() == null)
			return;
		StructureBlockInfo info = entity.getContraption().getBlocks().get(controlsPos);
		if (info == null || !(info.state.getBlock() instanceof ModernTrainControllerBlock))
			return;
		RailwardPackets.getChannel().sendToServer(new ModernTrainControllerReverserPacket(entity.getId(), controlsPos));
	}
}
