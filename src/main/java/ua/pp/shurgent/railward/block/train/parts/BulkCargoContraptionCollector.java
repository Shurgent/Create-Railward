package ua.pp.shurgent.railward.block.train.parts;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class BulkCargoContraptionCollector {

	private static final double TOP_INTAKE_MARGIN = .75;
	private static final String LAST_TICK_KEY = "RailwardBulkCargoTick";

	private BulkCargoContraptionCollector() {
	}

	public static void tick(AbstractContraptionEntity contraptionEntity) {
		long gameTime = contraptionEntity.level.getGameTime();
		if (contraptionEntity.getPersistentData()
			.getLong(LAST_TICK_KEY) == gameTime)
			return;
		contraptionEntity.getPersistentData()
			.putLong(LAST_TICK_KEY, gameTime);

		if (contraptionEntity.level.isClientSide) {
			tickClientCargoViews(contraptionEntity.getContraption());
			return;
		}

		Contraption contraption = contraptionEntity.getContraption();
		if (contraption == null)
			return;

		boolean periodicSync = contraptionEntity.tickCount % 20 == 0;
		List<BulkCargoContraptionHelper.ControllerData> containers = collectContainers(contraption);
		if (containers.isEmpty())
			return;

		AABB itemSearchBox = createItemSearchBox(contraptionEntity, containers);
		List<ItemEntity> items = contraptionEntity.level.getEntitiesOfClass(ItemEntity.class,
			itemSearchBox,
			item -> item.isAlive() && !item.getItem()
				.isEmpty());

		if (!items.isEmpty())
			for (ItemEntity itemEntity : items) {
				Vec3 localPos = contraptionEntity.toLocalVector(itemEntity.position(), 0);
				for (BulkCargoContraptionHelper.ControllerData data : containers) {
					if (!isInsideContainer(data, localPos))
						continue;
					if (BulkCargoContraptionHelper.tryInsertItemEntity(contraptionEntity, data, itemEntity))
						break;
				}
			}

		for (BulkCargoContraptionHelper.ControllerData data : containers)
			BulkCargoContraptionHelper.updateCargoLevels(contraptionEntity, data);

		if (periodicSync)
			for (BulkCargoContraptionHelper.ControllerData data : containers)
				BulkCargoContraptionHelper.syncCargo(contraptionEntity, data);
	}

	private static AABB createItemSearchBox(AbstractContraptionEntity contraptionEntity,
		List<BulkCargoContraptionHelper.ControllerData> containers) {
		AABB searchBox = null;
		for (BulkCargoContraptionHelper.ControllerData data : containers) {
			AABB bounds = toGlobalBox(contraptionEntity, data);
			searchBox = searchBox == null ? bounds : union(searchBox, bounds);
		}
		return searchBox == null ? contraptionEntity.getBoundingBox()
			.inflate(1) : searchBox.inflate(1);
	}

	private static AABB toGlobalBox(AbstractContraptionEntity contraptionEntity,
		BulkCargoContraptionHelper.ControllerData data) {
		BlockPos origin = data.controllerPos();
		double minX = origin.getX();
		double minY = origin.getY();
		double minZ = origin.getZ();
		double maxX = minX + data.sizeX();
		double maxY = minY + data.height() + TOP_INTAKE_MARGIN;
		double maxZ = minZ + data.sizeZ();

		double gx0 = Double.POSITIVE_INFINITY;
		double gy0 = Double.POSITIVE_INFINITY;
		double gz0 = Double.POSITIVE_INFINITY;
		double gx1 = Double.NEGATIVE_INFINITY;
		double gy1 = Double.NEGATIVE_INFINITY;
		double gz1 = Double.NEGATIVE_INFINITY;
		for (double x : new double[] { minX, maxX })
			for (double y : new double[] { minY, maxY })
				for (double z : new double[] { minZ, maxZ }) {
					Vec3 global = contraptionEntity.toGlobalVector(new Vec3(x, y, z), 0);
					gx0 = Math.min(gx0, global.x);
					gy0 = Math.min(gy0, global.y);
					gz0 = Math.min(gz0, global.z);
					gx1 = Math.max(gx1, global.x);
					gy1 = Math.max(gy1, global.y);
					gz1 = Math.max(gz1, global.z);
				}
		return new AABB(gx0, gy0, gz0, gx1, gy1, gz1);
	}

	private static AABB union(AABB first, AABB second) {
		return new AABB(Math.min(first.minX, second.minX), Math.min(first.minY, second.minY),
			Math.min(first.minZ, second.minZ), Math.max(first.maxX, second.maxX),
			Math.max(first.maxY, second.maxY), Math.max(first.maxZ, second.maxZ));
	}

	private static List<BulkCargoContraptionHelper.ControllerData> collectContainers(Contraption contraption) {
		List<BulkCargoContraptionHelper.ControllerData> containers = new ArrayList<>();
		Set<BlockPos> seenControllers = new HashSet<>();
		for (Map.Entry<BlockPos, StructureBlockInfo> entry : contraption.getBlocks()
			.entrySet()) {
			if (!(entry.getValue().state.getBlock() instanceof BulkCargoContainerBlock))
				continue;
			BulkCargoContraptionHelper.ControllerData data =
				BulkCargoContraptionHelper.resolveController(contraption, entry.getKey());
			if (data == null || !seenControllers.add(data.controllerPos()))
				continue;
			containers.add(data);
		}
		return containers;
	}

	private static boolean isInsideContainer(BulkCargoContraptionHelper.ControllerData data, Vec3 localPos) {
		BlockPos origin = data.controllerPos();
		double minX = origin.getX();
		double minY = origin.getY();
		double minZ = origin.getZ();
		double maxX = minX + data.sizeX();
		double maxY = minY + data.height() + TOP_INTAKE_MARGIN;
		double maxZ = minZ + data.sizeZ();
		return localPos.x >= minX && localPos.x < maxX && localPos.y >= minY && localPos.y < maxY
			&& localPos.z >= minZ && localPos.z < maxZ;
	}

	private static void tickClientCargoViews(Contraption contraption) {
		if (contraption == null)
			return;
		for (net.minecraft.world.level.block.entity.BlockEntity blockEntity : contraption.presentBlockEntities.values())
			if (blockEntity instanceof BulkCargoContainerBlockEntity be && be.hasClientCargoView()
				&& be.getCargoLevel() != null)
				be.getCargoLevel()
					.tickChaser();
	}

}
