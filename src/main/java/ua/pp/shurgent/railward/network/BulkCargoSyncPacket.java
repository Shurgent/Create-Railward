package ua.pp.shurgent.railward.network;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.foundation.networking.SimplePacketBase;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent.Context;
import ua.pp.shurgent.railward.block.train.parts.BulkCargoContraptionHelper;
import ua.pp.shurgent.railward.block.train.parts.BulkCargoContainerBlockEntity;

public class BulkCargoSyncPacket extends SimplePacketBase {

	private final int entityId;
	private final BlockPos controllerPos;
	private final ItemStack displayStack;
	private final int amount;
	private final int capacity;

	public BulkCargoSyncPacket(int entityId, BlockPos controllerPos, ItemStack displayStack, int amount, int capacity) {
		this.entityId = entityId;
		this.controllerPos = controllerPos;
		this.displayStack = displayStack.copy();
		if (!this.displayStack.isEmpty())
			this.displayStack.setCount(1);
		this.amount = amount;
		this.capacity = capacity;
	}

	public BulkCargoSyncPacket(FriendlyByteBuf buffer) {
		entityId = buffer.readVarInt();
		controllerPos = buffer.readBlockPos();
		displayStack = buffer.readItem();
		amount = buffer.readVarInt();
		capacity = buffer.readVarInt();
	}

	@Override
	public void write(FriendlyByteBuf buffer) {
		buffer.writeVarInt(entityId);
		buffer.writeBlockPos(controllerPos);
		buffer.writeItem(displayStack);
		buffer.writeVarInt(amount);
		buffer.writeVarInt(capacity);
	}

	@Override
	public boolean handle(Context context) {
		context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
			() -> () -> handleClient(entityId, controllerPos, displayStack, amount, capacity)));
		return true;
	}

	@OnlyIn(Dist.CLIENT)
	private static void handleClient(int entityId, BlockPos controllerPos, ItemStack displayStack, int amount,
		int capacity) {
		if (Minecraft.getInstance().level == null)
			return;
		Entity entity = Minecraft.getInstance().level.getEntity(entityId);
		if (!(entity instanceof AbstractContraptionEntity contraptionEntity))
			return;
		if (BulkCargoContraptionHelper.applyClientCargo(contraptionEntity, controllerPos, displayStack, amount,
			capacity))
			return;
		if (!(contraptionEntity.getContraption().presentBlockEntities
			.get(controllerPos) instanceof BulkCargoContainerBlockEntity be))
			return;
		be.applyClientCargo(displayStack, amount, capacity);
	}

}
