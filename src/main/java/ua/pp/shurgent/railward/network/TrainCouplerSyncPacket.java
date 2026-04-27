package ua.pp.shurgent.railward.network;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.jozufozu.flywheel.backend.Backend;
import com.jozufozu.flywheel.backend.instancing.InstancedRenderDispatcher;
import com.simibubi.create.CreateClient;
import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.Carriage.DimensionalCarriageEntity;
import com.simibubi.create.content.trains.entity.CarriageBogey;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import com.simibubi.create.content.trains.entity.CarriageSyncData;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.entity.TrainIconType;
import com.simibubi.create.foundation.networking.SimplePacketBase;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent.Context;
import ua.pp.shurgent.railward.block.train.parts.TrainCouplerVisualRebaser;

public class TrainCouplerSyncPacket extends SimplePacketBase {

	private final List<Train> trains;
	private final List<UUID> trainGraphIds;
	private final List<UUID> removedTrainIds;
	private final List<EntityBinding> bindings;
	private final List<RebaseInstruction> rebases;

	public TrainCouplerSyncPacket(Collection<Train> trains, Collection<CarriageContraptionEntity> entities) {
		this(trains, entities, List.of());
	}

	public TrainCouplerSyncPacket(Collection<Train> trains, Collection<CarriageContraptionEntity> entities,
		Collection<UUID> removedTrainIds) {
		this(trains, entities, removedTrainIds, List.of());
	}

	public TrainCouplerSyncPacket(Collection<Train> trains, Collection<CarriageContraptionEntity> entities,
		Collection<UUID> removedTrainIds, Collection<RebaseInstruction> rebases) {
		this.trains = new ArrayList<>(trains);
		this.trainGraphIds = new ArrayList<>();
		this.removedTrainIds = new ArrayList<>(removedTrainIds);
		this.bindings = new ArrayList<>();
		this.rebases = new ArrayList<>(rebases);

		for (Train train : trains)
			trainGraphIds.add(train.graph == null ? null : train.graph.id);
		for (CarriageContraptionEntity entity : entities)
			bindings.add(new EntityBinding(entity.getId(), entity.trainId, entity.carriageIndex,
				entity.getCarriageData() == null ? new CarriageSyncData() : entity.getCarriageData()
					.copy()));
	}

	public TrainCouplerSyncPacket(FriendlyByteBuf buffer) {
		trains = new ArrayList<>();
		trainGraphIds = new ArrayList<>();
		removedTrainIds = new ArrayList<>();
		bindings = new ArrayList<>();
		rebases = new ArrayList<>();

		int trainCount = buffer.readVarInt();
		for (int i = 0; i < trainCount; i++) {
			trains.add(readTrain(buffer));
			trainGraphIds.add(buffer.readBoolean() ? buffer.readUUID() : null);
		}

		int removedTrainCount = buffer.readVarInt();
		for (int i = 0; i < removedTrainCount; i++)
			removedTrainIds.add(buffer.readUUID());

		int bindingCount = buffer.readVarInt();
		for (int i = 0; i < bindingCount; i++) {
			CarriageSyncData carriageData = new CarriageSyncData();
			int entityId = buffer.readVarInt();
			UUID trainId = buffer.readUUID();
			int carriageIndex = buffer.readVarInt();
			carriageData.read(buffer);
			bindings.add(new EntityBinding(entityId, trainId, carriageIndex, carriageData));
		}

		int rebaseCount = buffer.readVarInt();
		for (int i = 0; i < rebaseCount; i++)
			rebases.add(new RebaseInstruction(buffer.readVarInt(), buffer.readBlockPos(), buffer.readEnum(Direction.class)));
	}

	@Override
	public void write(FriendlyByteBuf buffer) {
		buffer.writeVarInt(trains.size());
		for (int i = 0; i < trains.size(); i++) {
			Train train = trains.get(i);
			writeTrain(buffer, train);
			UUID graphId = i < trainGraphIds.size() ? trainGraphIds.get(i) : null;
			buffer.writeBoolean(graphId != null);
			if (graphId != null)
				buffer.writeUUID(graphId);
		}

		buffer.writeVarInt(removedTrainIds.size());
		for (UUID trainId : removedTrainIds)
			buffer.writeUUID(trainId);

		buffer.writeVarInt(bindings.size());
		for (EntityBinding binding : bindings) {
			buffer.writeVarInt(binding.entityId());
			buffer.writeUUID(binding.trainId());
			buffer.writeVarInt(binding.carriageIndex());
			binding.carriageData()
				.write(buffer);
		}

		buffer.writeVarInt(rebases.size());
		for (RebaseInstruction rebase : rebases) {
			buffer.writeVarInt(rebase.entityId());
			buffer.writeBlockPos(rebase.offset());
			buffer.writeEnum(rebase.assemblyDirection());
		}
	}

	@Override
	public boolean handle(Context context) {
		context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
			() -> () -> handleClient(trains, trainGraphIds, removedTrainIds, bindings, rebases)));
		return true;
	}

	private static void writeTrain(FriendlyByteBuf buffer, Train train) {
		buffer.writeUUID(train.id);

		buffer.writeBoolean(train.owner != null);
		if (train.owner != null)
			buffer.writeUUID(train.owner);

		buffer.writeVarInt(train.carriages.size());
		for (Carriage carriage : train.carriages) {
			buffer.writeNbt(carriage.bogeys.getFirst()
				.write(null));
			buffer.writeBoolean(carriage.isOnTwoBogeys());
			if (carriage.isOnTwoBogeys())
				buffer.writeNbt(carriage.bogeys.getSecond()
					.write(null));
			buffer.writeVarInt(carriage.bogeySpacing);
		}

		buffer.writeVarInt(train.carriageSpacing.size());
		train.carriageSpacing.forEach(buffer::writeVarInt);

		buffer.writeBoolean(train.doubleEnded);
		buffer.writeUtf(Component.Serializer.toJson(train.name));
		buffer.writeResourceLocation(train.icon.getId());
	}

	private static Train readTrain(FriendlyByteBuf buffer) {
		UUID trainId = buffer.readUUID();
		UUID owner = buffer.readBoolean() ? buffer.readUUID() : null;

		List<Carriage> carriages = new ArrayList<>();
		List<Integer> carriageSpacing = new ArrayList<>();

		int carriageCount = buffer.readVarInt();
		for (int i = 0; i < carriageCount; i++) {
			CompoundTag firstBogey = buffer.readNbt();
			CompoundTag secondBogey = buffer.readBoolean() ? buffer.readNbt() : null;
			CarriageBogey first = CarriageBogey.read(firstBogey, null, null);
			CarriageBogey second = secondBogey == null ? null : CarriageBogey.read(secondBogey, null, null);
			carriages.add(new Carriage(first, second, buffer.readVarInt()));
		}

		int spacingCount = buffer.readVarInt();
		for (int i = 0; i < spacingCount; i++)
			carriageSpacing.add(buffer.readVarInt());

		Train train = new Train(trainId, owner, null, carriages, carriageSpacing, buffer.readBoolean());
		train.name = Component.Serializer.fromJson(buffer.readUtf());
		train.icon = TrainIconType.byId(buffer.readResourceLocation());
		return train;
	}

	@OnlyIn(Dist.CLIENT)
	private static void handleClient(List<Train> trains, List<UUID> trainGraphIds, List<UUID> removedTrainIds,
		List<EntityBinding> bindings, List<RebaseInstruction> rebases) {
		Map<UUID, Train> clientTrains = CreateClient.RAILWAYS.trains;
		for (UUID trainId : removedTrainIds)
			clientTrains.remove(trainId);

		for (int i = 0; i < trains.size(); i++) {
			Train train = trains.get(i);
			UUID graphId = i < trainGraphIds.size() ? trainGraphIds.get(i) : null;
			if (graphId != null)
				train.graph = CreateClient.RAILWAYS.trackNetworks.get(graphId);
			clientTrains.put(train.id, train);
		}

		if (Minecraft.getInstance().level == null)
			return;

		Set<Integer> affectedEntityIds = new HashSet<>();
		for (EntityBinding binding : bindings) {
			Entity entity = Minecraft.getInstance().level.getEntity(binding.entityId());
			if (!(entity instanceof CarriageContraptionEntity carriageEntity))
				continue;

			Train train = clientTrains.get(binding.trainId());
			if (train == null || binding.carriageIndex() < 0 || binding.carriageIndex() >= train.carriages.size())
				continue;

			Carriage carriage = train.carriages.get(binding.carriageIndex());
			carriageEntity.trainId = binding.trainId();
			carriageEntity.carriageIndex = binding.carriageIndex();
			carriageEntity.setCarriage(carriage);

			DimensionalCarriageEntity dimensional = carriage.getDimensional(carriageEntity.level);
			dimensional.entity = new WeakReference<>(carriageEntity);
			binding.carriageData()
				.apply(carriageEntity, carriage);
			carriage.updateContraptionAnchors();
			dimensional.alignEntity(carriageEntity);
			primeBogeyAngles(carriageEntity, carriage);
			dimensional.updateRenderedCutoff();
			affectedEntityIds.add(binding.entityId());
		}

		for (RebaseInstruction rebase : rebases) {
			Entity entity = Minecraft.getInstance().level.getEntity(rebase.entityId());
			if (!(entity instanceof CarriageContraptionEntity carriageEntity))
				continue;
			TrainCouplerVisualRebaser.apply(carriageEntity, rebase.offset(), rebase.assemblyDirection());
			affectedEntityIds.add(rebase.entityId());
		}

		for (Integer entityId : affectedEntityIds) {
			Entity entity = Minecraft.getInstance().level.getEntity(entityId);
			if (entity instanceof CarriageContraptionEntity carriageEntity)
				refreshCarriageInstance(carriageEntity);
		}
	}

	@OnlyIn(Dist.CLIENT)
	private static void refreshCarriageInstance(CarriageContraptionEntity carriageEntity) {
		if (!Backend.isOn())
			return;

		InstancedRenderDispatcher.getEntities(carriageEntity.level)
			.remove(carriageEntity);
		InstancedRenderDispatcher.getEntities(carriageEntity.level)
			.queueAdd(carriageEntity);
	}

	@OnlyIn(Dist.CLIENT)
	private static void primeBogeyAngles(CarriageContraptionEntity carriageEntity, Carriage carriage) {
		if (carriage.train == null || carriage.train.graph == null)
			return;

		boolean firstPositionUpdate = carriageEntity.firstPositionUpdate;
		carriageEntity.firstPositionUpdate = true;
		carriage.bogeys.getFirst()
			.updateAngles(carriageEntity, 0);
		if (carriage.isOnTwoBogeys())
			carriage.bogeys.getSecond()
				.updateAngles(carriageEntity, 0);
		carriageEntity.firstPositionUpdate = firstPositionUpdate;
	}

	private record EntityBinding(int entityId, UUID trainId, int carriageIndex, CarriageSyncData carriageData) {
	}

	public record RebaseInstruction(int entityId, BlockPos offset, Direction assemblyDirection) {
	}

}
