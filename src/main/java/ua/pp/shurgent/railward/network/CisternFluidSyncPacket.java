package ua.pp.shurgent.railward.network;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.foundation.networking.SimplePacketBase;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent.Context;
import ua.pp.shurgent.railward.block.train.parts.CisternContraptionHelper;

public class CisternFluidSyncPacket extends SimplePacketBase {

	private final int entityId;
	private final BlockPos controllerPos;
	private final FluidStack fluid;

	public CisternFluidSyncPacket(int entityId, BlockPos controllerPos, FluidStack fluid) {
		this.entityId = entityId;
		this.controllerPos = controllerPos;
		this.fluid = fluid.copy();
	}

	public CisternFluidSyncPacket(FriendlyByteBuf buffer) {
		entityId = buffer.readVarInt();
		controllerPos = buffer.readBlockPos();
		fluid = FluidStack.readFromPacket(buffer);
	}

	@Override
	public void write(FriendlyByteBuf buffer) {
		buffer.writeVarInt(entityId);
		buffer.writeBlockPos(controllerPos);
		fluid.writeToPacket(buffer);
	}

	@Override
	public boolean handle(Context context) {
		context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
			() -> () -> handleClient(entityId, controllerPos, fluid)));
		return true;
	}

	@OnlyIn(Dist.CLIENT)
	private static void handleClient(int entityId, BlockPos controllerPos, FluidStack fluid) {
		if (Minecraft.getInstance().level == null)
			return;
		Entity entity = Minecraft.getInstance().level.getEntity(entityId);
		if (!(entity instanceof AbstractContraptionEntity contraptionEntity))
			return;
		CisternContraptionHelper.applyClientFluid(contraptionEntity, controllerPos, fluid);
	}

}
