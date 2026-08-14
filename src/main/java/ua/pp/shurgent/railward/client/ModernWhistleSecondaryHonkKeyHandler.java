package ua.pp.shurgent.railward.client;

import java.util.UUID;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.InputConstants;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.actors.trainControls.ControlsHandler;
import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.foundation.utility.ControlsUtil;

import net.minecraft.client.KeyMapping;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import ua.pp.shurgent.railward.RailwardPackets;
import ua.pp.shurgent.railward.block.train.parts.ModernWhistleTrainHelper;
import ua.pp.shurgent.railward.network.ModernWhistleSecondaryHonkRequestPacket;

public class ModernWhistleSecondaryHonkKeyHandler {

	public static final KeyMapping SECONDARY_HONK = new KeyMapping("key.railward.secondary_honk",
		InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_H, "key.categories.railward");

	private static UUID activeTrainId;
	private static boolean sentPressed;

	private ModernWhistleSecondaryHonkKeyHandler() {
	}

	public static void onClientTick(ClientTickEvent event) {
		if (event.phase != Phase.END)
			return;

		Train train = getControlledModernWhistleTrain();
		UUID trainId = train == null ? null : train.id;
		boolean pressed = train != null && ControlsUtil.isActuallyPressed(SECONDARY_HONK);

		if (sentPressed && (!pressed || !activeTrainId.equals(trainId)))
			send(activeTrainId, false);
		if (pressed && (!sentPressed || !trainId.equals(activeTrainId)))
			send(trainId, true);

		sentPressed = pressed;
		activeTrainId = pressed ? trainId : null;
	}

	private static Train getControlledModernWhistleTrain() {
		AbstractContraptionEntity contraption = ControlsHandler.getContraption();
		if (!(contraption instanceof CarriageContraptionEntity carriageEntity))
			return null;
		Carriage carriage = carriageEntity.getCarriage();
		if (carriage == null || carriage.train == null)
			return null;
		if (ModernWhistleTrainHelper.resolveTrainLeadSecondarySoundType(carriageEntity) == null)
			return null;
		return carriage.train;
	}

	private static void send(UUID trainId, boolean pressed) {
		if (trainId != null)
			RailwardPackets.getChannel()
				.sendToServer(new ModernWhistleSecondaryHonkRequestPacket(trainId, pressed));
	}
}
