package ua.pp.shurgent.railward.block.train.parts;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.items.IItemHandler;

public final class BulkCargoContainerConnectivity {

	private static final Comparator<BlockPos> BLOCK_ORDER = Comparator.comparingInt((BlockPos pos) -> pos.getY())
		.thenComparingInt(pos -> pos.getX())
		.thenComparingInt(pos -> pos.getZ());

	private BulkCargoContainerConnectivity() {
	}

	public static void formFrom(BulkCargoContainerBlockEntity start) {
		Level level = start.getLevel();
		if (level == null || level.isClientSide)
			return;

		Block block = start.getBlockState()
			.getBlock();
		Set<BlockPos> component = collectComponent(level, start.getBlockPos(), block);
		if (component.isEmpty())
			return;

		List<BlockPos> sorted = component.stream()
			.sorted(BLOCK_ORDER)
			.toList();
		List<ItemStack> contents = new ArrayList<>();
		for (BlockPos pos : sorted)
			if (level.getBlockEntity(pos) instanceof BulkCargoContainerBlockEntity be)
				contents.addAll(be.takeInventoryContents());

		Bounds bounds = Bounds.of(component);
		if (bounds.isValidComponent(component)) {
			applyMultiblock(level, sorted, contents, bounds);
			return;
		}

		applySingles(level, sorted, contents);
	}

	public static void updateCargoLevels(BulkCargoContainerBlockEntity be) {
		BulkCargoContainerBlockEntity controllerBE = be.getControllerBE();
		if (controllerBE == null || controllerBE.getLevel() == null || controllerBE.getLevel()
			.isClientSide)
			return;

		updateBlockStates(controllerBE.getLevel(), controllerBE.getBlockPos(), controllerBE.getSizeX(),
			controllerBE.getSizeZ(), controllerBE.getHeight(), controllerBE.getInventoryForConnectivity());
	}

	private static Set<BlockPos> collectComponent(Level level, BlockPos start, Block block) {
		Set<BlockPos> visited = new HashSet<>();
		Queue<BlockPos> frontier = new ArrayDeque<>();
		frontier.add(start);

		int limit = BulkCargoContainerBlockEntity.MAX_SIDE * BulkCargoContainerBlockEntity.MAX_SIDE
			* BulkCargoContainerBlockEntity.MAX_HEIGHT + 1;
		while (!frontier.isEmpty() && visited.size() <= limit) {
			BlockPos current = frontier.poll();
			if (!visited.add(current))
				continue;
			if (level.getBlockState(current)
				.getBlock() != block)
				continue;
			if (!(level.getBlockEntity(current) instanceof BulkCargoContainerBlockEntity))
				continue;

			for (Direction direction : Direction.values()) {
				BlockPos next = current.relative(direction);
				if (!visited.contains(next) && level.getBlockState(next)
					.getBlock() == block)
					frontier.add(next);
			}
		}

		visited.removeIf(pos -> level.getBlockState(pos)
			.getBlock() != block || !(level.getBlockEntity(pos) instanceof BulkCargoContainerBlockEntity));
		return visited;
	}

	private static void applyMultiblock(Level level, List<BlockPos> positions, List<ItemStack> contents, Bounds bounds) {
		BlockPos controllerPos = new BlockPos(bounds.minX, bounds.minY, bounds.minZ);
		BulkCargoContainerBlockEntity controllerBE = null;

		for (BlockPos pos : positions) {
			if (!(level.getBlockEntity(pos) instanceof BulkCargoContainerBlockEntity be))
				continue;
			be.preventConnectivityUpdate();
			boolean controllerPart = pos.equals(controllerPos);
			be.setMultiblock(controllerPos, bounds.sizeX(), bounds.sizeZ(), bounds.height(), controllerPart);
			if (controllerPart)
				controllerBE = be;
		}

		if (controllerBE == null) {
			drop(level, controllerPos, contents);
			return;
		}

		int slots = bounds.volume() * BulkCargoContainerBlockEntity.STACKS_PER_BLOCK;
		List<ItemStack> overflow = controllerBE.replaceInventoryContents(contents, slots);
		drop(level, controllerPos, overflow);
		updateBlockStates(level, controllerPos, bounds.sizeX(), bounds.sizeZ(), bounds.height(),
			controllerBE.getInventoryForConnectivity());
	}

	private static void applySingles(Level level, List<BlockPos> positions, List<ItemStack> contents) {
		List<ItemStack> remaining = contents;
		for (BlockPos pos : positions) {
			if (!(level.getBlockEntity(pos) instanceof BulkCargoContainerBlockEntity be))
				continue;
			be.preventConnectivityUpdate();
			be.setSingle();
			remaining = be.replaceInventoryContents(remaining, BulkCargoContainerBlockEntity.STACKS_PER_BLOCK);
			updateBlockStates(level, pos, 1, 1, 1, be.getInventoryForConnectivity());
		}
		if (!remaining.isEmpty())
			drop(level, positions.isEmpty() ? BlockPos.ZERO : positions.get(0), remaining);
	}

	private static void updateBlockStates(Level level, BlockPos controllerPos, int sizeX, int sizeZ, int height,
		IItemHandler inventory) {
		float fillBlocks = BulkCargoStorage.getFillState(inventory) * height;
		boolean xLong = sizeX >= sizeZ;
		for (int x = 0; x < sizeX; x++)
			for (int y = 0; y < height; y++)
				for (int z = 0; z < sizeZ; z++) {
					BlockPos pos = controllerPos.offset(x, y, z);
					BlockState state = level.getBlockState(pos);
					if (!(state.getBlock() instanceof BulkCargoContainerBlock))
						continue;

					float localFill = Mth.clamp(fillBlocks - y, 0, 1);
					int cargoLevel = localFill <= 0 ? 0 : Mth.clamp(Mth.ceil(localFill * 16), 1, 16);
					BlockState updated = withContainerState(state, x == 0, x == sizeX - 1, z == 0, z == sizeZ - 1,
						y == 0, y == height - 1, xLong, cargoLevel);
					if (updated != state)
						level.setBlock(pos, updated, 22);
				}
		pushEntitiesAboveCargo(level, controllerPos, sizeX, sizeZ, height, fillBlocks);
	}

	private static BlockState withContainerState(BlockState state, boolean west, boolean east, boolean north,
		boolean south, boolean bottom, boolean top, boolean xLong, int cargoLevel) {
		return state.setValue(BulkCargoContainerBlock.WEST, west)
			.setValue(BulkCargoContainerBlock.EAST, east)
			.setValue(BulkCargoContainerBlock.NORTH, north)
			.setValue(BulkCargoContainerBlock.SOUTH, south)
			.setValue(BulkCargoContainerBlock.BOTTOM, bottom)
			.setValue(BulkCargoContainerBlock.TOP, top)
			.setValue(BulkCargoContainerBlock.X_LONG, xLong)
			.setValue(BulkCargoContainerBlock.CARGO_LEVEL, cargoLevel);
	}

	private static void drop(Level level, BlockPos pos, List<ItemStack> stacks) {
		for (ItemStack stack : stacks)
			if (!stack.isEmpty())
				Containers.dropItemStack(level, pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5, stack);
	}

	private static void pushEntitiesAboveCargo(Level level, BlockPos controllerPos, int sizeX, int sizeZ, int height,
		float fillBlocks) {
		double surfaceY = getCargoSurfaceY(controllerPos.getY(), height, fillBlocks);
		if (Double.isNaN(surfaceY))
			return;

		AABB bounds = new AABB(controllerPos.getX(), controllerPos.getY(), controllerPos.getZ(),
			controllerPos.getX() + sizeX, surfaceY + .25, controllerPos.getZ() + sizeZ);
		for (Entity entity : level.getEntities((Entity) null, bounds,
			entity -> shouldPushEntity(entity, controllerPos, sizeX, sizeZ, surfaceY))) {
			double dy = surfaceY - entity.getY();
			if (dy <= 1.0E-4)
				continue;
			entity.move(MoverType.PISTON, new Vec3(0, dy, 0));
			if (entity.getY() + 1.0E-4 < surfaceY)
				entity.setPos(entity.getX(), surfaceY, entity.getZ());
			entity.setOnGround(true);
		}
	}

	private static boolean shouldPushEntity(Entity entity, BlockPos controllerPos, int sizeX, int sizeZ,
		double surfaceY) {
		if (!entity.isAlive() || entity.isPassenger())
			return false;
		if (!(entity instanceof LivingEntity))
			return false;
		if (entity instanceof Player player && player.isSpectator())
			return false;
		if (entity.getY() + 1.0E-4 >= surfaceY)
			return false;
		double x = entity.getX();
		double z = entity.getZ();
		return x >= controllerPos.getX() && x < controllerPos.getX() + sizeX && z >= controllerPos.getZ()
			&& z < controllerPos.getZ() + sizeZ;
	}

	private static double getCargoSurfaceY(int controllerY, int height, float fillBlocks) {
		if (fillBlocks <= 0)
			return Double.NaN;
		if (fillBlocks >= height)
			return controllerY + height;

		int layer = Mth.clamp(Mth.floor(fillBlocks - 1.0E-5f), 0, height - 1);
		float localFill = Mth.clamp(fillBlocks - layer, 0, 1);
		int cargoLevel = localFill <= 0 ? 0 : Mth.clamp(Mth.ceil(localFill * 16), 1, 16);
		return controllerY + layer + cargoLevel / 16.0;
	}

	private record Bounds(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
		static Bounds of(Set<BlockPos> positions) {
			int minX = Integer.MAX_VALUE;
			int minY = Integer.MAX_VALUE;
			int minZ = Integer.MAX_VALUE;
			int maxX = Integer.MIN_VALUE;
			int maxY = Integer.MIN_VALUE;
			int maxZ = Integer.MIN_VALUE;
			for (BlockPos pos : positions) {
				minX = Math.min(minX, pos.getX());
				minY = Math.min(minY, pos.getY());
				minZ = Math.min(minZ, pos.getZ());
				maxX = Math.max(maxX, pos.getX());
				maxY = Math.max(maxY, pos.getY());
				maxZ = Math.max(maxZ, pos.getZ());
			}
			return new Bounds(minX, minY, minZ, maxX, maxY, maxZ);
		}

		boolean isValidComponent(Set<BlockPos> positions) {
			if (sizeX() < 1 || sizeX() > BulkCargoContainerBlockEntity.MAX_SIDE)
				return false;
			if (sizeZ() < 1 || sizeZ() > BulkCargoContainerBlockEntity.MAX_SIDE)
				return false;
			if (height() < 1 || height() > BulkCargoContainerBlockEntity.MAX_HEIGHT)
				return false;
			if (positions.size() != volume())
				return false;
			for (int x = minX; x <= maxX; x++)
				for (int y = minY; y <= maxY; y++)
					for (int z = minZ; z <= maxZ; z++)
						if (!positions.contains(new BlockPos(x, y, z)))
							return false;
			return true;
		}

		int sizeX() {
			return maxX - minX + 1;
		}

		int sizeZ() {
			return maxZ - minZ + 1;
		}

		int height() {
			return maxY - minY + 1;
		}

		int volume() {
			return sizeX() * sizeZ() * height();
		}
	}

}
