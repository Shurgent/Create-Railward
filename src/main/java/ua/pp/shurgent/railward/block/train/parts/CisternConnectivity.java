package ua.pp.shurgent.railward.block.train.parts;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fluids.FluidStack;

public final class CisternConnectivity {

	private static final int MAX_BLOCKS = CisternBlockEntity.WIDTH_LARGE * CisternBlockEntity.WIDTH_LARGE
		* CisternBlockEntity.MAX_LENGTH;

	private CisternConnectivity() {
	}

	public static void formFrom(CisternBlockEntity start) {
		Level level = start.getLevel();
		if (level == null || level.isClientSide)
			return;

		Direction.Axis axis = start.getCisternAxis();
		Set<BlockPos> cluster = collectCluster(level, start.getBlockPos(), start.getBlockState()
			.getBlock(), axis);
		Bounds bounds = Bounds.of(cluster, axis);
		if (bounds == null || !bounds.isValid(level, start.getBlockState()
			.getBlock(), axis)) {
			makeSingles(level, cluster);
			return;
		}

		FluidStack mergedFluid = collectCompatibleFluid(level, cluster);
		if (mergedFluid == null)
			return;

		for (BlockPos pos : sorted(cluster)) {
			if (level.getBlockEntity(pos) instanceof CisternBlockEntity cistern)
				cistern.drainFluidForConnectivity();
		}

		BlockPos controllerPos = bounds.origin();
		for (BlockPos pos : bounds.positions()) {
			CisternBlockEntity cistern = getCistern(level, pos);
			if (cistern == null)
				continue;
			if (pos.equals(controllerPos))
				cistern.becomeController(bounds.width(), bounds.length(), mergedFluid);
			else
				cistern.becomePart(controllerPos, bounds.width(), bounds.length());
		}
	}

	public static void split(CisternBlockEntity start, @Nullable BlockPos removedPos) {
		Level level = start.getLevel();
		if (level == null || level.isClientSide)
			return;

		CisternBlockEntity controller = start.getControllerBE();
		if (controller == null)
			controller = start;

		Bounds bounds = Bounds.fromController(controller);
		if (bounds == null || bounds.volume() <= 1)
			return;

		FluidStack fluid = controller.drainFluidForConnectivity();
		List<BlockPos> remaining = new ArrayList<>();
		for (BlockPos pos : bounds.positions())
			if (!pos.equals(removedPos) && getCistern(level, pos) != null)
				remaining.add(pos);

		distributeAsSingles(level, remaining, fluid);
	}

	private static void makeSingles(Level level, Set<BlockPos> positions) {
		FluidStack fluid = collectCompatibleFluid(level, positions);
		for (BlockPos pos : sorted(positions)) {
			CisternBlockEntity cistern = getCistern(level, pos);
			if (cistern == null)
				continue;
			cistern.drainFluidForConnectivity();
		}
		distributeAsSingles(level, new ArrayList<>(positions), fluid == null ? FluidStack.EMPTY : fluid);
	}

	private static void distributeAsSingles(Level level, List<BlockPos> positions, FluidStack fluid) {
		int capacityPerBlock = CisternBlockEntity.getCapacityMultiplier();
		FluidStack remaining = fluid.copy();
		for (BlockPos pos : sorted(positions)) {
			CisternBlockEntity cistern = getCistern(level, pos);
			if (cistern == null)
				continue;
			FluidStack partFluid = FluidStack.EMPTY;
			if (!remaining.isEmpty()) {
				partFluid = remaining.copy();
				partFluid.setAmount(Math.min(capacityPerBlock, remaining.getAmount()));
				remaining.shrink(partFluid.getAmount());
			}
			cistern.becomeSingle(partFluid);
		}
	}

	@Nullable
	private static FluidStack collectCompatibleFluid(Level level, Set<BlockPos> positions) {
		Set<CisternBlockEntity> holders = new LinkedHashSet<>();
		for (BlockPos pos : sorted(positions)) {
			CisternBlockEntity cistern = getCistern(level, pos);
			if (cistern == null)
				continue;
			CisternBlockEntity controller = cistern.getControllerBE();
			holders.add(controller == null ? cistern : controller);
			holders.add(cistern);
		}

		FluidStack merged = FluidStack.EMPTY;
		for (CisternBlockEntity holder : holders) {
			FluidStack fluid = holder.getFluidForConnectivity();
			if (fluid.isEmpty())
				continue;
			if (merged.isEmpty())
				merged = fluid.copy();
			else if (merged.isFluidEqual(fluid))
				merged.grow(fluid.getAmount());
			else
				return null;
		}
		return merged;
	}

	private static Set<BlockPos> collectCluster(Level level, BlockPos start, Block block, Direction.Axis axis) {
		Set<BlockPos> visited = new HashSet<>();
		Queue<BlockPos> frontier = new ArrayDeque<>();
		frontier.add(start);

		while (!frontier.isEmpty() && visited.size() <= MAX_BLOCKS) {
			BlockPos current = frontier.remove();
			if (!visited.add(current))
				continue;

			for (Direction direction : Direction.values()) {
				BlockPos next = current.relative(direction);
				if (visited.contains(next))
					continue;
				if (!isSameCistern(level, next, block, axis))
					continue;
				frontier.add(next);
			}
		}

		return visited;
	}

	private static boolean isSameCistern(Level level, BlockPos pos, Block block, Direction.Axis axis) {
		BlockState state = level.getBlockState(pos);
		return state.getBlock() == block && state.hasProperty(CisternBlock.AXIS) && state.getValue(CisternBlock.AXIS) == axis
			&& level.getBlockEntity(pos) instanceof CisternBlockEntity;
	}

	@Nullable
	private static CisternBlockEntity getCistern(Level level, BlockPos pos) {
		return level.getBlockEntity(pos) instanceof CisternBlockEntity cistern ? cistern : null;
	}

	private static List<BlockPos> sorted(Set<BlockPos> positions) {
		return positions.stream()
			.sorted(positionComparator())
			.toList();
	}

	private static List<BlockPos> sorted(List<BlockPos> positions) {
		return positions.stream()
			.sorted(positionComparator())
			.toList();
	}

	private static Comparator<BlockPos> positionComparator() {
		return Comparator.comparingInt((BlockPos pos) -> pos.getY())
			.thenComparingInt(pos -> pos.getX())
			.thenComparingInt(pos -> pos.getZ());
	}

	private record Bounds(BlockPos origin, Direction.Axis axis, int width, int length) {

		@Nullable
		static Bounds of(Set<BlockPos> positions, Direction.Axis axis) {
			if (positions.isEmpty())
				return null;
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

			int sizeX = maxX - minX + 1;
			int sizeY = maxY - minY + 1;
			int sizeZ = maxZ - minZ + 1;
			int width = axis == Direction.Axis.X ? sizeZ : sizeX;
			int crossHeight = sizeY;
			int length = axis == Direction.Axis.X ? sizeX : sizeZ;

			if (width != crossHeight)
				return null;
			return new Bounds(new BlockPos(minX, minY, minZ), axis, width, length);
		}

		@Nullable
		static Bounds fromController(CisternBlockEntity controller) {
			int width = controller.getCisternWidth();
			int length = controller.getCisternLength();
			if (width < 1 || length < 1)
				return null;
			return new Bounds(controller.getBlockPos(), controller.getCisternAxis(), width, length);
		}

		boolean isValid(Level level, Block block, Direction.Axis expectedAxis) {
			if (axis != expectedAxis)
				return false;
			if (!CisternBlockEntity.isValidWidth(width) || length < 1 || length > CisternBlockEntity.MAX_LENGTH)
				return false;
			if (volume() > MAX_BLOCKS)
				return false;
			for (BlockPos pos : positions())
				if (!isSameCistern(level, pos, block, axis))
					return false;
			return true;
		}

		List<BlockPos> positions() {
			List<BlockPos> positions = new ArrayList<>(volume());
			for (int longIndex = 0; longIndex < length; longIndex++)
				for (int y = 0; y < width; y++)
					for (int cross = 0; cross < width; cross++)
						positions.add(axis == Direction.Axis.X ? origin.offset(longIndex, y, cross)
							: origin.offset(cross, y, longIndex));
			return positions;
		}

		int volume() {
			return width * width * length;
		}
	}

}
