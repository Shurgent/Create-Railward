package ua.pp.shurgent.railward.gametest;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.simibubi.create.Create;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.StructureTransform;
import com.simibubi.create.content.fluids.FluidNetwork;
import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.CarriageContraption;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.entity.TravellingPoint;
import com.simibubi.create.content.trains.graph.DimensionPalette;
import com.simibubi.create.content.trains.station.GlobalStation;
import com.simibubi.create.content.trains.station.StationBlockEntity;
import com.simibubi.create.content.trains.track.TrackBlock;
import com.simibubi.create.content.trains.track.TrackShape;
import com.simibubi.create.content.trains.bogey.AbstractBogeyBlock;
import com.simibubi.create.content.contraptions.actors.trainControls.ControlsBlock;
import com.simibubi.create.foundation.fluid.CombinedTankWrapper;
import com.simibubi.create.foundation.utility.BlockFace;
import com.simibubi.create.foundation.utility.Pair;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.fluids.FluidStack;
//? if >=1.20.1
/*import net.minecraftforge.common.capabilities.ForgeCapabilities;*/
//? if <1.20.1
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
//? if <1.20.1
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import ua.pp.shurgent.railward.AllBlocks;
import ua.pp.shurgent.railward.Railward;
import ua.pp.shurgent.railward.block.train.parts.BulkCargoContraptionAssembly;
import ua.pp.shurgent.railward.block.train.parts.BulkCargoContraptionHelper;
import ua.pp.shurgent.railward.block.train.parts.BulkCargoContainerBlock;
import ua.pp.shurgent.railward.block.train.parts.BulkCargoContainerBlockEntity;
import ua.pp.shurgent.railward.block.train.parts.BulkCargoStorage;
import ua.pp.shurgent.railward.block.train.parts.CisternBlock;
import ua.pp.shurgent.railward.block.train.parts.CisternBlockEntity;
import ua.pp.shurgent.railward.block.train.parts.CisternContraptionHelper;
import ua.pp.shurgent.railward.block.train.station.FluidTerminalBlockEntity;
import ua.pp.shurgent.railward.block.train.station.UnloadingBunkerBlockEntity;
import ua.pp.shurgent.railward.block.train.station.UnloadingHatchBlockEntity;
import ua.pp.shurgent.railward.compat.StationArrivalSelection;
import ua.pp.shurgent.railward.compat.WorldTimeProjection;

@GameTestHolder(Railward.MOD_ID)
@PrefixGameTestTemplate(false)
public final class RailwardServerGameTests {

	private static final String EMPTY_TEMPLATE = "empty";

	private RailwardServerGameTests() {
	}

	@GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 20)
	public static void bulkCargoStorageConservesItems(GameTestHelper helper) {
		BulkCargoStorage.CargoItemHandler inventory = new BulkCargoStorage.CargoItemHandler(2);
		ItemStack iron = new ItemStack(Items.IRON_INGOT, 80);

		ItemStack simulatedRemainder = BulkCargoStorage.insert(inventory, iron, true);
		require(helper, simulatedRemainder.isEmpty(), "80 iron ingots should fit in two slots");
		require(helper, BulkCargoStorage.getStoredAmount(inventory) == 0,
			"simulated insertion changed the inventory");

		ItemStack remainder = BulkCargoStorage.insert(inventory, iron, false);
		require(helper, remainder.isEmpty(), "executed insertion unexpectedly returned a remainder");
		require(helper, BulkCargoStorage.getStoredAmount(inventory) == 80,
			"executed insertion did not store all 80 items");

		ItemStack rejected = BulkCargoStorage.insert(inventory, new ItemStack(Items.GOLD_INGOT, 1), false);
		require(helper, rejected.getCount() == 1, "mixed cargo was accepted");
		require(helper, BulkCargoStorage.getStoredAmount(inventory) == 80,
			"rejected mixed cargo changed the stored amount");

		ItemStack simulatedExtraction = BulkCargoStorage.extractOneStack(inventory, true);
		require(helper, simulatedExtraction.getCount() == 64, "simulated extraction should return one stack");
		require(helper, BulkCargoStorage.getStoredAmount(inventory) == 80,
			"simulated extraction changed the inventory");

		ItemStack extracted = BulkCargoStorage.extractOneStack(inventory, false);
		require(helper, extracted.getCount() == 64, "executed extraction should return one stack");
		require(helper, BulkCargoStorage.getStoredAmount(inventory) == 16,
			"executed extraction did not leave the expected remainder");
		helper.succeed();
	}

	@GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 20)
	public static void bulkCargoRejectsInvalidAndBlacklistedItems(GameTestHelper helper) {
		BulkCargoStorage.CargoItemHandler inventory = new BulkCargoStorage.CargoItemHandler(1);

		require(helper, inventory.isItemValid(0, new ItemStack(Items.IRON_INGOT)),
			"ordinary non-block cargo should be accepted");
		require(helper, inventory.isItemValid(0, new ItemStack(Items.OAK_SLAB)),
			"slab cargo should be accepted");
		require(helper, !inventory.isItemValid(0, new ItemStack(Items.TORCH)),
			"non-full model block cargo should be rejected");
		require(helper, !inventory.isItemValid(0, com.simibubi.create.AllItems.WRENCH.asStack()),
			"items in the bulk-cargo blacklist tag should be rejected");
		helper.succeed();
	}

	@GameTest(template = EMPTY_TEMPLATE, setupTicks = 1, timeoutTicks = 40)
	public static void bulkCargoBlockEntityPersistsCargo(GameTestHelper helper) {
		BlockPos pos = new BlockPos(0, 0, 0);
		helper.setBlock(pos, AllBlocks.BULK_CARGO_CONTAINER.get());

		helper.runAfterDelay(3, () -> {
			BlockEntity placed = helper.getBlockEntity(pos);
			require(helper, placed instanceof BulkCargoContainerBlockEntity,
				"bulk cargo block did not create its registered block entity");

			BulkCargoContainerBlockEntity cargo = (BulkCargoContainerBlockEntity) placed;
			ItemStack remainder = cargo.tryInsert(new ItemStack(Items.IRON_INGOT, 80), false);
			require(helper, remainder.isEmpty(), "bulk cargo block entity did not accept the test cargo");
			require(helper, cargo.getStoredAmount() == 80, "bulk cargo block entity stored the wrong amount");

			CompoundTag saved = cargo.saveWithFullMetadata();
			BlockEntity restored = BlockEntity.loadStatic(cargo.getBlockPos(), cargo.getBlockState(), saved);
			require(helper, restored instanceof BulkCargoContainerBlockEntity,
				"saved bulk cargo data did not restore the expected block entity type");

			BulkCargoContainerBlockEntity restoredCargo = (BulkCargoContainerBlockEntity) restored;
			require(helper, restoredCargo.getStoredAmount() == 80,
				"bulk cargo amount was not preserved by NBT save/load");
			require(helper, restoredCargo.getDisplayStack().is(Items.IRON_INGOT),
				"bulk cargo item type was not preserved by NBT save/load");
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_TEMPLATE, setupTicks = 1, timeoutTicks = 40)
	public static void cisternBlockEntityPersistsFluid(GameTestHelper helper) {
		BlockPos pos = new BlockPos(0, 0, 0);
		helper.setBlock(pos, AllBlocks.CISTERN.get());

		helper.runAfterDelay(3, () -> {
			BlockEntity placed = helper.getBlockEntity(pos);
			require(helper, placed instanceof CisternBlockEntity,
				"cistern block did not create its registered block entity");

			CisternBlockEntity cistern = (CisternBlockEntity) placed;
			int filled = cistern.getTankInventory().fill(new FluidStack(Fluids.WATER, 1000),
				IFluidHandler.FluidAction.EXECUTE);
			require(helper, filled == 1000, "cistern did not accept one bucket of water");
			require(helper, cistern.getTankInventory().getFluidAmount() == 1000,
				"cistern stored the wrong fluid amount");

			CompoundTag saved = cistern.saveWithFullMetadata();
			BlockEntity restored = BlockEntity.loadStatic(cistern.getBlockPos(), cistern.getBlockState(), saved);
			require(helper, restored instanceof CisternBlockEntity,
				"saved cistern data did not restore the expected block entity type");

			CisternBlockEntity restoredCistern = (CisternBlockEntity) restored;
			require(helper, restoredCistern.getTankInventory().getFluidAmount() == 1000,
				"cistern fluid amount was not preserved by NBT save/load");
			require(helper, restoredCistern.getTankInventory().getFluid().getFluid() == Fluids.WATER,
				"cistern fluid type was not preserved by NBT save/load");
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_TEMPLATE, setupTicks = 1, timeoutTicks = 80)
	public static void bulkCargoMultiblockConservesItemsWhenSplit(GameTestHelper helper) {
		BlockPos origin = new BlockPos(1, 1, 1);
		BlockPos partPos = new BlockPos(2, 1, 1);
		helper.setBlock(origin, AllBlocks.BULK_CARGO_CONTAINER.get());
		helper.setBlock(partPos, AllBlocks.BULK_CARGO_CONTAINER.get());

		helper.runAfterDelay(5, () -> {
			BulkCargoContainerBlockEntity controller = bulkCargoAt(helper, origin);
			BulkCargoContainerBlockEntity part = bulkCargoAt(helper, partPos);
			require(helper, controller.isController(), "lowest bulk cargo block did not become controller");
			require(helper, controller.getSizeX() == 2 && controller.getSizeZ() == 1
				&& controller.getHeight() == 1, "bulk cargo blocks formed the wrong dimensions");
			require(helper, part.getControllerBE() == controller,
				"bulk cargo part did not route to the expected controller");

			ItemStack remainder = controller.tryInsert(new ItemStack(Items.IRON_INGOT, 100), false);
			require(helper, remainder.isEmpty(), "formed bulk cargo multiblock rejected test items");
			helper.destroyBlock(partPos);

			helper.runAfterDelay(5, () -> {
				BulkCargoContainerBlockEntity remaining = bulkCargoAt(helper, origin);
				require(helper, remaining.isController(), "remaining bulk cargo block is not a controller");
				require(helper, remaining.getSizeX() == 1 && remaining.getSizeZ() == 1
					&& remaining.getHeight() == 1, "remaining bulk cargo block did not become single");
				require(helper, remaining.getController().equals(remaining.getBlockPos()),
					"remaining bulk cargo block retained a stale controller reference");
				require(helper, remaining.getStoredAmount() == 100,
					"bulk cargo items were lost while splitting the multiblock");
				require(helper, helper.getBlockState(origin).getValue(BulkCargoContainerBlock.CARGO_LEVEL) == 1,
					"split bulk cargo block exposed the wrong fill level");

				helper.setBlock(partPos, AllBlocks.BULK_CARGO_CONTAINER.get());
				helper.runAfterDelay(5, () -> {
					BulkCargoContainerBlockEntity rebuilt = bulkCargoAt(helper, origin);
					BulkCargoContainerBlockEntity rebuiltPart = bulkCargoAt(helper, partPos);
					require(helper, rebuilt.isController() && rebuilt.getSizeX() == 2 && rebuilt.getSizeZ() == 1
						&& rebuilt.getHeight() == 1, "bulk cargo did not re-form after replacing its part");
					require(helper, rebuiltPart.getControllerBE() == rebuilt
						&& rebuiltPart.getController().equals(rebuilt.getBlockPos()),
						"re-formed bulk cargo part did not route to the rebuilt controller");
					require(helper, rebuilt.getStoredAmount() == 100
						&& rebuilt.getDisplayStack().is(Items.IRON_INGOT),
						"bulk cargo type or amount changed while re-forming the multiblock");
					require(helper,
						helper.getBlockState(origin).getValue(BulkCargoContainerBlock.CARGO_LEVEL) == 1
							&& helper.getBlockState(partPos).getValue(BulkCargoContainerBlock.CARGO_LEVEL) == 1,
						"re-formed bulk cargo blocks exposed inconsistent fill levels");
					helper.succeed();
				});
			});
		});
	}

	@GameTest(template = EMPTY_TEMPLATE, setupTicks = 1, timeoutTicks = 120)
	public static void bulkCargoShapesAndLayerFillStayConsistent(GameTestHelper helper) {
		BlockPos xLongOrigin = new BlockPos(1, 1, 1);
		BlockPos zLongOrigin = new BlockPos(6, 1, 1);
		placeBulkCargo(helper, xLongOrigin, 3, 2, 1);
		placeBulkCargo(helper, zLongOrigin, 1, 2, 3);

		helper.runAfterDelay(7, () -> {
			assertBulkCargoShape(helper, xLongOrigin, 3, 2, 1, true);
			assertBulkCargoShape(helper, zLongOrigin, 1, 2, 3, false);

			BulkCargoContainerBlockEntity xLongController = bulkCargoAt(helper, xLongOrigin);
			int expectedCapacity = 3 * 2 * BulkCargoContainerBlockEntity.STACKS_PER_BLOCK * 64;
			require(helper, xLongController.getCapacityItems() == expectedCapacity,
				"x-long bulk cargo exposed the wrong item capacity");
			int inserted = expectedCapacity * 5 / 8;
			insertBulkCargo(helper, xLongController, Items.IRON_INGOT, inserted);

			BulkCargoContainerBlockEntity zLongPart = bulkCargoAt(helper, zLongOrigin.offset(0, 1, 2));
			IItemHandler routed = itemCapability(zLongPart, Direction.UP).orElse(null);
			require(helper, routed != null, "z-long bulk cargo part did not expose a routed capability");
			ItemStack invalidBlock = new ItemStack(Items.TORCH);
			ItemStack blacklisted = com.simibubi.create.AllItems.WRENCH.asStack();
			require(helper, routed.insertItem(0, invalidBlock, false).getCount() == invalidBlock.getCount(),
				"z-long bulk cargo accepted an invalid block through a routed capability");
			require(helper, routed.insertItem(0, blacklisted, false).getCount() == blacklisted.getCount(),
				"z-long bulk cargo accepted blacklisted cargo through a routed capability");
			ItemStack slabRemainder = routed.insertItem(0, new ItemStack(Items.OAK_SLAB, 32), false);
			require(helper, slabRemainder.isEmpty(),
				"z-long bulk cargo rejected valid slab cargo through a routed capability");

			helper.runAfterDelay(3, () -> {
				require(helper, xLongController.getStoredAmount() == inserted
					&& xLongController.getDisplayStack().is(Items.IRON_INGOT),
					"x-long bulk cargo changed its inserted cargo");
				for (int x = 0; x < 3; x++) {
					int bottomLevel = helper.getBlockState(xLongOrigin.offset(x, 0, 0))
						.getValue(BulkCargoContainerBlock.CARGO_LEVEL);
					int topLevel = helper.getBlockState(xLongOrigin.offset(x, 1, 0))
						.getValue(BulkCargoContainerBlock.CARGO_LEVEL);
					require(helper, bottomLevel == 16 && topLevel == 4,
						"x-long bulk cargo exposed the wrong per-layer fill: " + bottomLevel + "/" + topLevel);
				}
				BulkCargoContainerBlockEntity zLongController = bulkCargoAt(helper, zLongOrigin);
				require(helper, zLongController.getStoredAmount() == 32
					&& zLongController.getDisplayStack().is(Items.OAK_SLAB),
					"z-long routed insertion changed the valid cargo type or amount");
				helper.succeed();
			});
		});
	}

	@GameTest(template = EMPTY_TEMPLATE, setupTicks = 1, timeoutTicks = 80)
	public static void bulkCargoRectangularMultiblockRoutesCapabilities(GameTestHelper helper) {
		BlockPos origin = new BlockPos(1, 1, 1);
		for (int x = 0; x < 3; x++)
			for (int y = 0; y < 2; y++)
				for (int z = 0; z < 2; z++)
					helper.setBlock(origin.offset(x, y, z), AllBlocks.BULK_CARGO_CONTAINER.get());

		helper.runAfterDelay(7, () -> {
			BulkCargoContainerBlockEntity controller = bulkCargoAt(helper, origin);
			BulkCargoContainerBlockEntity remotePart = bulkCargoAt(helper, origin.offset(2, 1, 1));
			require(helper, controller.isController(), "rectangular cargo origin did not become controller");
			require(helper, controller.getSizeX() == 3 && controller.getSizeZ() == 2
				&& controller.getHeight() == 2, "rectangular cargo formed the wrong dimensions");

			IItemHandler routed = itemCapability(remotePart, Direction.UP).orElse(null);
			require(helper, routed != null, "cargo part did not expose its routed item capability");
			require(helper, routed.getSlots() == 12 * BulkCargoContainerBlockEntity.STACKS_PER_BLOCK,
				"routed cargo capability exposed the wrong slot count");
			ItemStack remainder = routed.insertItem(0, new ItemStack(Items.IRON_INGOT, 32), false);
			require(helper, remainder.isEmpty(), "routed cargo capability rejected valid cargo");
			require(helper, controller.getStoredAmount() == 32,
				"cargo inserted through a part did not reach the controller");
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_TEMPLATE, setupTicks = 1, timeoutTicks = 140)
	public static void cisternMultiblockConservesFluidWhenSplit(GameTestHelper helper) {
		BlockPos origin = new BlockPos(1, 1, 1);
		for (int y = 0; y < CisternBlockEntity.WIDTH_SMALL; y++)
			for (int z = 0; z < CisternBlockEntity.WIDTH_SMALL; z++)
				helper.setBlock(origin.offset(0, y, z), AllBlocks.CISTERN.get().defaultBlockState()
					.setValue(CisternBlock.AXIS, Direction.Axis.X));

		helper.runAfterDelay(7, () -> {
			CisternBlockEntity controller = cisternAt(helper, origin);
			require(helper, controller.isController(), "cistern origin did not become controller");
			require(helper, controller.getCisternWidth() == CisternBlockEntity.WIDTH_SMALL
				&& controller.getCisternLength() == 1, "cistern blocks formed the wrong dimensions");

			BlockPos removed = origin.offset(0, 0, 1);
			require(helper, cisternAt(helper, removed).getControllerBE() == controller,
				"cistern part did not route to the expected controller");

			int expectedAmount = CisternBlockEntity.getCapacityMultiplier() + 1000;
			int filled = controller.getTankInventory().fill(new FluidStack(Fluids.WATER, expectedAmount),
				IFluidHandler.FluidAction.EXECUTE);
			require(helper, filled == expectedAmount, "formed cistern did not accept the test fluid");
			helper.destroyBlock(removed);

			helper.runAfterDelay(5, () -> {
				int remainingAmount = 0;
				for (int y = 0; y < CisternBlockEntity.WIDTH_SMALL; y++)
					for (int z = 0; z < CisternBlockEntity.WIDTH_SMALL; z++) {
						BlockPos pos = origin.offset(0, y, z);
						if (pos.equals(removed))
							continue;
						CisternBlockEntity remaining = cisternAt(helper, pos);
						require(helper, remaining.isController(),
							"split cistern part did not become an independent controller");
						require(helper, remaining.getCisternWidth() == 1 && remaining.getCisternLength() == 1,
							"split cistern part did not become single");
						remainingAmount += remaining.getTankInventory().getFluidAmount();
					}
				require(helper, remainingAmount == expectedAmount,
					"cistern fluid was lost while splitting the multiblock");

				helper.setBlock(removed, AllBlocks.CISTERN.get().defaultBlockState()
					.setValue(CisternBlock.AXIS, Direction.Axis.X));
				helper.runAfterDelay(7, () -> {
					CisternBlockEntity rebuilt = cisternAt(helper, origin);
					assertCisternDimensions(helper, rebuilt, CisternBlockEntity.WIDTH_SMALL, 1);
					require(helper, rebuilt.getTankInventory().getFluid().getFluid() == Fluids.WATER
						&& rebuilt.getTankInventory().getFluidAmount() == expectedAmount,
						"cistern fluid type or amount changed while re-forming the multiblock");
					for (int y = 0; y < CisternBlockEntity.WIDTH_SMALL; y++)
						for (int z = 0; z < CisternBlockEntity.WIDTH_SMALL; z++) {
							CisternBlockEntity part = cisternAt(helper, origin.offset(0, y, z));
							require(helper, part.getControllerBE() == rebuilt
								&& part.getController().equals(rebuilt.getBlockPos()),
								"re-formed cistern part retained an invalid controller reference");
						}
					helper.succeed();
				});
			});
		});
	}

	@GameTest(template = EMPTY_TEMPLATE, setupTicks = 1, timeoutTicks = 240)
	public static void cisternWidthOneSupportsEveryLengthAndAxis(GameTestHelper helper) {
		runWidthOneCisternFixture(helper, 0);
	}

	@GameTest(template = EMPTY_TEMPLATE, setupTicks = 1, timeoutTicks = 80)
	public static void cisternWidthFiveForms(GameTestHelper helper) {
		BlockPos origin = BlockPos.ZERO;
		placeCistern(helper, origin, Direction.Axis.X, CisternBlockEntity.WIDTH_LARGE, 1);

		helper.runAfterDelay(7, () -> {
			CisternBlockEntity controller = cisternAt(helper, origin);
			assertCisternDimensions(helper, controller, CisternBlockEntity.WIDTH_LARGE, 1);
			require(helper, cisternAt(helper, origin.offset(0, 4, 4)).getControllerBE() == controller,
				"width-five cistern corner did not route to its controller");
			require(helper, controller.getTankInventory().getCapacity()
				== 25 * CisternBlockEntity.getCapacityMultiplier(), "width-five cistern has the wrong capacity");
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_TEMPLATE, setupTicks = 1, timeoutTicks = 180)
	public static void cisternSupportedWidthsAxesAndSidedCapabilities(GameTestHelper helper) {
		List<CisternFixture> fixtures = List.of(
			new CisternFixture(new BlockPos(1, 1, 1), Direction.Axis.X, 1, 3),
			new CisternFixture(new BlockPos(1, 1, 4), Direction.Axis.X, CisternBlockEntity.WIDTH_SMALL, 3),
			new CisternFixture(new BlockPos(1, 1, 9), Direction.Axis.X, CisternBlockEntity.WIDTH_LARGE, 3),
			new CisternFixture(new BlockPos(6, 1, 1), Direction.Axis.Z, 1, 3),
			new CisternFixture(new BlockPos(9, 1, 1), Direction.Axis.Z, CisternBlockEntity.WIDTH_SMALL, 3),
			new CisternFixture(new BlockPos(14, 1, 1), Direction.Axis.Z, CisternBlockEntity.WIDTH_LARGE, 3));
		for (CisternFixture fixture : fixtures)
			placeCistern(helper, fixture.origin(), fixture.axis(), fixture.width(), fixture.length());

		helper.runAfterDelay(12, () -> {
			for (CisternFixture fixture : fixtures)
				assertCisternPortsAndRoutedFluid(helper, fixture);
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_TEMPLATE, setupTicks = 1, timeoutTicks = 400)
	public static void bulkCargoMaximumDimensionsForm(GameTestHelper helper) {
		BlockPos origin = BlockPos.ZERO;
		placeBulkCargo(helper, origin, BulkCargoContainerBlockEntity.MAX_SIDE,
			BulkCargoContainerBlockEntity.MAX_HEIGHT, BulkCargoContainerBlockEntity.MAX_SIDE);

		helper.runAfterDelay(20, () -> {
			BulkCargoContainerBlockEntity controller = bulkCargoAt(helper, origin);
			BulkCargoContainerBlockEntity farCorner = bulkCargoAt(helper, origin.offset(
				BulkCargoContainerBlockEntity.MAX_SIDE - 1, BulkCargoContainerBlockEntity.MAX_HEIGHT - 1,
				BulkCargoContainerBlockEntity.MAX_SIDE - 1));
			require(helper, controller.isController(), "maximum cargo origin did not become controller");
			require(helper, controller.getSizeX() == BulkCargoContainerBlockEntity.MAX_SIDE
				&& controller.getSizeZ() == BulkCargoContainerBlockEntity.MAX_SIDE
				&& controller.getHeight() == BulkCargoContainerBlockEntity.MAX_HEIGHT,
				"maximum cargo container formed the wrong dimensions");
			require(helper, farCorner.getControllerBE() == controller,
				"maximum cargo far corner did not route to its controller");
			require(helper, itemCapability(farCorner, Direction.UP)
				.map(IItemHandler::getSlots)
				.orElse(0) == BulkCargoContainerBlockEntity.MAX_SIDE * BulkCargoContainerBlockEntity.MAX_SIDE
					* BulkCargoContainerBlockEntity.MAX_HEIGHT * BulkCargoContainerBlockEntity.STACKS_PER_BLOCK,
				"maximum cargo container exposed the wrong slot count");
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_TEMPLATE, setupTicks = 1, timeoutTicks = 120)
	public static void bulkCargoOverLimitBecomesSinglesWithoutLosingItems(GameTestHelper helper) {
		BlockPos origin = new BlockPos(0, 1, 1);
		placeBulkCargo(helper, origin, BulkCargoContainerBlockEntity.MAX_SIDE, 1, 1);

		helper.runAfterDelay(7, () -> {
			BulkCargoContainerBlockEntity controller = bulkCargoAt(helper, origin);
			require(helper, controller.getSizeX() == BulkCargoContainerBlockEntity.MAX_SIDE,
				"maximum-length cargo row did not form before the over-limit step");
			ItemStack remainder = controller.tryInsert(new ItemStack(Items.IRON_INGOT, 64), false);
			require(helper, remainder.isEmpty(), "cargo row rejected the conservation test items");
			helper.setBlock(origin.offset(BulkCargoContainerBlockEntity.MAX_SIDE, 0, 0),
				AllBlocks.BULK_CARGO_CONTAINER.get());

			helper.runAfterDelay(7, () -> {
				int total = 0;
				for (int x = 0; x <= BulkCargoContainerBlockEntity.MAX_SIDE; x++) {
					BulkCargoContainerBlockEntity cargo = bulkCargoAt(helper, origin.offset(x, 0, 0));
					require(helper, cargo.isController() && cargo.getSizeX() == 1 && cargo.getSizeZ() == 1
						&& cargo.getHeight() == 1, "over-limit cargo component did not split into singles");
					total += cargo.getStoredAmount();
				}
				require(helper, total == 64, "cargo items were lost when the component exceeded MAX_SIDE");
				helper.succeed();
			});
		});
	}

	@GameTest(template = EMPTY_TEMPLATE, setupTicks = 1, timeoutTicks = 80)
	public static void incompleteBulkCargoRectangleBecomesSingles(GameTestHelper helper) {
		BlockPos origin = new BlockPos(1, 1, 1);
		helper.setBlock(origin, AllBlocks.BULK_CARGO_CONTAINER.get());
		helper.setBlock(origin.offset(1, 0, 0), AllBlocks.BULK_CARGO_CONTAINER.get());
		helper.setBlock(origin.offset(0, 0, 1), AllBlocks.BULK_CARGO_CONTAINER.get());

		helper.runAfterDelay(7, () -> {
			for (BlockPos pos : new BlockPos[] { origin, origin.offset(1, 0, 0), origin.offset(0, 0, 1) }) {
				BulkCargoContainerBlockEntity cargo = bulkCargoAt(helper, pos);
				require(helper, cargo.isController() && cargo.getSizeX() == 1 && cargo.getSizeZ() == 1
					&& cargo.getHeight() == 1, "incomplete cargo rectangle did not remain as singles");
			}
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_TEMPLATE, setupTicks = 1, timeoutTicks = 200)
	public static void cisternMaximumDimensionsForm(GameTestHelper helper) {
		BlockPos origin = BlockPos.ZERO;
		placeCistern(helper, origin, Direction.Axis.X, CisternBlockEntity.WIDTH_LARGE,
			CisternBlockEntity.MAX_LENGTH);

		helper.runAfterDelay(12, () -> {
			CisternBlockEntity controller = cisternAt(helper, origin);
			assertCisternDimensions(helper, controller, CisternBlockEntity.WIDTH_LARGE,
				CisternBlockEntity.MAX_LENGTH);
			CisternBlockEntity farCorner = cisternAt(helper, origin.offset(CisternBlockEntity.MAX_LENGTH - 1,
				CisternBlockEntity.WIDTH_LARGE - 1, CisternBlockEntity.WIDTH_LARGE - 1));
			require(helper, farCorner.getControllerBE() == controller,
				"maximum cistern far corner did not route to its controller");
			require(helper, controller.getTankInventory().getCapacity()
				== CisternBlockEntity.WIDTH_LARGE * CisternBlockEntity.WIDTH_LARGE
					* CisternBlockEntity.MAX_LENGTH * CisternBlockEntity.getCapacityMultiplier(),
				"maximum cistern has the wrong capacity");
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_TEMPLATE, setupTicks = 1, timeoutTicks = 120)
	public static void cisternOverMaximumLengthBecomesSinglesWithoutLosingFluid(GameTestHelper helper) {
		BlockPos origin = new BlockPos(0, 1, 1);
		placeCistern(helper, origin, Direction.Axis.X, 1, CisternBlockEntity.MAX_LENGTH);

		helper.runAfterDelay(7, () -> {
			CisternBlockEntity controller = cisternAt(helper, origin);
			assertCisternDimensions(helper, controller, 1, CisternBlockEntity.MAX_LENGTH);
			int expectedAmount = CisternBlockEntity.getCapacityMultiplier() + 1000;
			int filled = controller.getTankInventory().fill(new FluidStack(Fluids.WATER, expectedAmount),
				IFluidHandler.FluidAction.EXECUTE);
			require(helper, filled == expectedAmount, "maximum-length cistern rejected the test fluid");
			helper.setBlock(origin.offset(CisternBlockEntity.MAX_LENGTH, 0, 0),
				AllBlocks.CISTERN.get().defaultBlockState().setValue(CisternBlock.AXIS, Direction.Axis.X));

			helper.runAfterDelay(7, () -> {
				int total = 0;
				for (int x = 0; x <= CisternBlockEntity.MAX_LENGTH; x++) {
					CisternBlockEntity cistern = cisternAt(helper, origin.offset(x, 0, 0));
					assertCisternDimensions(helper, cistern, 1, 1);
					total += cistern.getTankInventory().getFluidAmount();
				}
				require(helper, total == expectedAmount,
					"cistern fluid was lost when the component exceeded MAX_LENGTH");
				helper.succeed();
			});
		});
	}

	@GameTest(template = EMPTY_TEMPLATE, setupTicks = 1, timeoutTicks = 80)
	public static void unsupportedCisternWidthBecomesSingles(GameTestHelper helper) {
		BlockPos origin = new BlockPos(1, 1, 1);
		placeCistern(helper, origin, Direction.Axis.X, 2, 1);

		helper.runAfterDelay(7, () -> {
			for (int y = 0; y < 2; y++)
				for (int z = 0; z < 2; z++)
					assertCisternDimensions(helper, cisternAt(helper, origin.offset(0, y, z)), 1, 1);
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_TEMPLATE, setupTicks = 1, timeoutTicks = 80)
	public static void incompatibleCisternFluidsDoNotMerge(GameTestHelper helper) {
		BlockPos firstPos = new BlockPos(1, 1, 1);
		BlockPos secondPos = firstPos.offset(1, 0, 0);
		helper.setBlock(firstPos, AllBlocks.CISTERN.get().defaultBlockState()
			.setValue(CisternBlock.AXIS, Direction.Axis.X));
		helper.setBlock(secondPos, AllBlocks.CISTERN.get().defaultBlockState()
			.setValue(CisternBlock.AXIS, Direction.Axis.X));

		CisternBlockEntity first = cisternAt(helper, firstPos);
		CisternBlockEntity second = cisternAt(helper, secondPos);
		first.getTankInventory().fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE);
		second.getTankInventory().fill(new FluidStack(Fluids.LAVA, 1000), IFluidHandler.FluidAction.EXECUTE);

		helper.runAfterDelay(7, () -> {
			CisternBlockEntity water = cisternAt(helper, firstPos);
			CisternBlockEntity lava = cisternAt(helper, secondPos);
			assertCisternDimensions(helper, water, 1, 1);
			assertCisternDimensions(helper, lava, 1, 1);
			require(helper, water.getTankInventory().getFluid().getFluid() == Fluids.WATER
				&& water.getTankInventory().getFluidAmount() == 1000,
				"water changed while incompatible cisterns attempted to merge");
			require(helper, lava.getTankInventory().getFluid().getFluid() == Fluids.LAVA
				&& lava.getTankInventory().getFluidAmount() == 1000,
				"lava changed while incompatible cisterns attempted to merge");
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_TEMPLATE, setupTicks = 1, timeoutTicks = 100)
	public static void bulkCargoSurvivesMountedStorageRoundTrip(GameTestHelper helper) {
		BlockPos origin = new BlockPos(1, 1, 1);
		placeBulkCargo(helper, origin, 2, 1, 1);

		helper.runAfterDelay(7, () -> {
			BulkCargoContainerBlockEntity controller = bulkCargoAt(helper, origin);
			ItemStack initialRemainder = controller.tryInsert(new ItemStack(Items.IRON_INGOT, 80), false);
			require(helper, initialRemainder.isEmpty(), "initial cargo did not fit before capture");
			require(helper, helper.getBlockState(origin).getValue(BulkCargoContainerBlock.CARGO_LEVEL) == 1
				&& helper.getBlockState(origin.relative(Direction.EAST))
					.getValue(BulkCargoContainerBlock.CARGO_LEVEL) == 1,
				"static cargo exposed the wrong fill before capture");

			BlockPos absoluteOrigin = helper.absolutePos(origin);
			CapturedCarriageContraption captured = new CapturedCarriageContraption();
			captured.captureBlocks(helper.getLevel(), absoluteOrigin,
				List.of(absoluteOrigin, absoluteOrigin.relative(Direction.EAST)));
			captured.beginMovement(helper.getLevel());

			IItemHandlerModifiable mounted = assertMountedBulkCargo(helper, captured, BlockPos.ZERO,
				BlockPos.ZERO.relative(Direction.EAST), 80, 1);
			ItemStack movingRemainder = BulkCargoStorage.insert(mounted,
				new ItemStack(Items.IRON_INGOT, 144), false);
			require(helper, movingRemainder.isEmpty(), "cargo could not be inserted while mounted");
			BulkCargoContraptionAssembly.normalize(captured);
			assertMountedBulkCargo(helper, captured, BlockPos.ZERO,
				BlockPos.ZERO.relative(Direction.EAST), 224, 2);

			Contraption restored = Contraption.fromNBT(helper.getLevel(), captured.writeNBT(false), false);
			assertMountedBulkCargo(helper, restored, BlockPos.ZERO,
				BlockPos.ZERO.relative(Direction.EAST), 224, 2);

			restored.addBlocksToWorld(helper.getLevel(),
				new StructureTransform(absoluteOrigin, Direction.Axis.Y, Rotation.NONE, Mirror.NONE));
			helper.runAfterDelay(7, () -> {
				BulkCargoContainerBlockEntity rebuilt = bulkCargoAt(helper, origin);
				require(helper, rebuilt.isController(), "disassembled cargo origin is not the controller");
				require(helper, rebuilt.getSizeX() == 2 && rebuilt.getSizeZ() == 1 && rebuilt.getHeight() == 1,
					"disassembled cargo restored the wrong dimensions");
				require(helper, rebuilt.getStoredAmount() == 224,
					"disassembled cargo did not restore mounted items");
				BulkCargoContainerBlockEntity rebuiltPart = bulkCargoAt(helper, origin.relative(Direction.EAST));
				require(helper, rebuiltPart.getControllerBE() == rebuilt
					&& rebuiltPart.getController().equals(rebuilt.getBlockPos()),
					"disassembled cargo part did not retain the mounted controller selection");
				IItemHandler partHandler = itemCapability(rebuiltPart,
					Direction.UP).orElse(null);
				require(helper, partHandler != null && countItems(partHandler) == 224,
					"disassembled cargo part did not route to the restored controller");
				require(helper, helper.getBlockState(origin).getValue(BulkCargoContainerBlock.CARGO_LEVEL) == 2
					&& helper.getBlockState(origin.relative(Direction.EAST))
						.getValue(BulkCargoContainerBlock.CARGO_LEVEL) == 2,
					"disassembled cargo did not restore the mounted fill level");
				helper.succeed();
			});
		});
	}

	@GameTest(template = EMPTY_TEMPLATE, setupTicks = 1, timeoutTicks = 80)
	public static void unloadingHatchTransfersAndRespectsBlockedTargets(GameTestHelper helper) {
		BlockPos sourcePos = new BlockPos(2, 4, 2);
		BlockPos hatchPos = sourcePos.below();
		BlockPos cargoPos = hatchPos.below(2);
		BlockPos blockedSourcePos = new BlockPos(7, 4, 2);
		BlockPos blockedHatchPos = blockedSourcePos.below();
		BlockPos blockedCargoPos = blockedHatchPos.below(2);

		helper.setBlock(sourcePos, Blocks.BARREL);
		helper.setBlock(hatchPos, AllBlocks.UNLOADING_HATCH.get());
		helper.setBlock(cargoPos, AllBlocks.BULK_CARGO_CONTAINER.get());
		helper.setBlock(blockedSourcePos, Blocks.BARREL);
		helper.setBlock(blockedHatchPos, AllBlocks.UNLOADING_HATCH.get());
		helper.setBlock(blockedCargoPos, AllBlocks.BULK_CARGO_CONTAINER.get());

		helper.runAfterDelay(4, () -> {
			BarrelBlockEntity source = barrelAt(helper, sourcePos);
			source.setItem(0, new ItemStack(Items.IRON_INGOT, 64));
			source.setItem(1, new ItemStack(Items.IRON_INGOT, 16));
			source.setChanged();

			BulkCargoContainerBlockEntity blockedCargo = bulkCargoAt(helper, blockedCargoPos);
			require(helper, blockedCargo.tryInsert(new ItemStack(Items.GOLD_INGOT, 16), false).isEmpty(),
				"blocked cargo fixture rejected its initial gold");
			BarrelBlockEntity blockedSource = barrelAt(helper, blockedSourcePos);
			blockedSource.setItem(0, new ItemStack(Items.IRON_INGOT, 32));
			blockedSource.setChanged();

			helper.runAfterDelay(10, () -> {
				BulkCargoContainerBlockEntity cargo = bulkCargoAt(helper, cargoPos);
				require(helper, countItems(source) == 0,
					"unloading hatch did not remove the exact source amount");
				require(helper, cargo.getStoredAmount() == 80 && cargo.getDisplayStack().is(Items.IRON_INGOT),
					"unloading hatch did not deliver the exact source amount to bulk cargo");
				require(helper, helper.getBlockState(cargoPos).getValue(BulkCargoContainerBlock.CARGO_LEVEL) == 1,
					"unloading hatch did not update the destination fill state");
				BlockEntity hatch = helper.getBlockEntity(hatchPos);
				require(helper, hatch instanceof UnloadingHatchBlockEntity
					&& ((UnloadingHatchBlockEntity) hatch).getTransferStack().is(Items.IRON_INGOT)
					&& ((UnloadingHatchBlockEntity) hatch).getTransferDistance() == 2,
					"unloading hatch did not expose synchronized transfer state");

				require(helper, countItems(blockedSource) == 32,
					"unloading hatch removed items rejected by an incompatible destination");
				require(helper, blockedCargo.getStoredAmount() == 16
					&& blockedCargo.getDisplayStack().is(Items.GOLD_INGOT),
					"incompatible unloading changed the destination cargo");

				helper.setBlock(hatchPos.relative(Direction.EAST), Blocks.REDSTONE_BLOCK);
				source.setItem(0, new ItemStack(Items.IRON_INGOT, 24));
				source.setChanged();
				helper.runAfterDelay(5, () -> {
					require(helper, countItems(source) == 24 && cargo.getStoredAmount() == 80,
						"powered unloading hatch transferred cargo despite redstone blocking");
					helper.succeed();
				});
			});
		});
	}

	@GameTest(template = EMPTY_TEMPLATE, setupTicks = 1, timeoutTicks = 80)
	public static void unloadingBunkerExposesPullOnlyTimedSource(GameTestHelper helper) {
		BlockPos sourcePos = new BlockPos(2, 5, 2);
		BlockPos hatchPos = sourcePos.below();
		BlockPos bunkerPos = hatchPos.below(2);
		helper.setBlock(sourcePos, Blocks.BARREL);
		helper.setBlock(hatchPos, AllBlocks.UNLOADING_HATCH.get());
		helper.setBlock(bunkerPos, AllBlocks.UNLOADING_BUNKER.get());

		BarrelBlockEntity source = barrelAt(helper, sourcePos);
		source.setItem(0, new ItemStack(Items.IRON_INGOT, 40));
		source.setChanged();

		helper.runAfterDelay(5, () -> {
			BlockEntity placed = helper.getBlockEntity(bunkerPos);
			require(helper, placed instanceof UnloadingBunkerBlockEntity,
				"unloading bunker block did not create its registered block entity");
			UnloadingBunkerBlockEntity bunker = (UnloadingBunkerBlockEntity) placed;
			IItemHandler exported = itemCapability(bunker, Direction.DOWN).orElse(null);
			require(helper, exported != null && exported.getSlots() == source.getContainerSize(),
				"unloading bunker did not expose the connected source inventory");

			ItemStack rejectedInsert = exported.insertItem(0, new ItemStack(Items.GOLD_INGOT, 7), false);
			require(helper, rejectedInsert.getCount() == 7 && countItems(source) == 40,
				"pull-only unloading bunker accepted an insertion");
			ItemStack simulated = exported.extractItem(0, 13, true);
			require(helper, simulated.is(Items.IRON_INGOT) && simulated.getCount() == 13
				&& countItems(source) == 40,
				"simulated bunker extraction changed the source inventory");
			ItemStack extracted = exported.extractItem(0, 13, false);
			require(helper, extracted.is(Items.IRON_INGOT) && extracted.getCount() == 13
				&& countItems(source) == 27,
				"executed bunker extraction did not move the exact amount");
			require(helper, bunker.getTransferStack().is(Items.IRON_INGOT)
				&& bunker.getTransferDistance() == 2,
				"unloading bunker did not expose synchronized transfer state");

			helper.setBlock(hatchPos.relative(Direction.EAST), Blocks.REDSTONE_BLOCK);
			helper.runAfterDelay(5, () -> {
				IItemHandler disconnected = itemCapability(bunker, Direction.DOWN).orElse(null);
				require(helper, disconnected != null && disconnected.getSlots() == 0
					&& countItems(source) == 27,
					"redstone-blocked hatch left the bunker source connected");
				helper.succeed();
			});
		});
	}

	@GameTest(template = EMPTY_TEMPLATE, setupTicks = 1, timeoutTicks = 100)
	public static void fluidNetworkConservesMultiTankSourceTransfer(GameTestHelper helper) {
		FluidTank firstTank = new FluidTank(100);
		FluidTank secondTank = new FluidTank(100);
		firstTank.setFluid(new FluidStack(Fluids.WATER, 40));
		secondTank.setFluid(new FluidStack(Fluids.WATER, 40));
		CombinedTankWrapper source = new CombinedTankWrapper(firstTank, secondTank);
		FluidTank target = new FluidTank(100);

		BlockFace sourceFace = new BlockFace(helper.absolutePos(new BlockPos(2, 2, 2)), Direction.EAST);
		FluidNetwork network = new FluidNetwork(helper.getLevel(), sourceFace,
			() -> LazyOptional.of(() -> source));
		setFluidNetworkField(network, "pauseBeforePropagation", Integer.valueOf(0));
		setFluidNetworkField(network, "transferSpeed", Integer.valueOf(64));
		setFluidNetworkField(network, "fluid", new FluidStack(Fluids.WATER, 1));
		List<Pair<BlockFace, LazyOptional<IFluidHandler>>> targets = new ArrayList<>();
		targets.add(Pair.of(new BlockFace(helper.absolutePos(new BlockPos(3, 2, 2)), Direction.WEST),
			LazyOptional.of(() -> target)));
		setFluidNetworkField(network, "targets", targets);

		network.tick();

		int remaining = firstTank.getFluidAmount() + secondTank.getFluidAmount();
		int delivered = target.getFluidAmount();
		require(helper, remaining == 16 && delivered == 64 && remaining + delivered == 80,
			"fluid network did not conserve its 80 mB multi-tank source: retained " + remaining
				+ " mB and delivered " + delivered + " mB");
		helper.succeed();
	}

	@GameTest(template = EMPTY_TEMPLATE, setupTicks = 1, timeoutTicks = 100)
	public static void cisternSurvivesMountedStorageRoundTrip(GameTestHelper helper) {
		BlockPos origin = new BlockPos(1, 1, 1);
		placeCistern(helper, origin, Direction.Axis.X, 3, 1);

		helper.runAfterDelay(7, () -> {
			CisternBlockEntity controller = cisternAt(helper, origin);
			int initialFill = controller.getTankInventory()
				.fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE);
			require(helper, initialFill == 1000, "initial fluid did not fit before capture");

			BlockPos absoluteOrigin = helper.absolutePos(origin);
			List<BlockPos> positions = new ArrayList<>();
			for (int y = 0; y < 3; y++)
				for (int z = 0; z < 3; z++)
					positions.add(absoluteOrigin.offset(0, y, z));
			CapturedCarriageContraption captured = new CapturedCarriageContraption();
			captured.captureBlocks(helper.getLevel(), absoluteOrigin, positions);
			captured.beginMovement(helper.getLevel());

			IFluidHandler mounted = CisternContraptionHelper.getFluidHandler(captured, BlockPos.ZERO);
			require(helper, mounted != null, "captured cistern has no mounted fluid handler");
			require(helper, mounted.getFluidInTank(0).getFluid() == Fluids.WATER
				&& mounted.getFluidInTank(0).getAmount() == 1000,
				"fluid changed while entering mounted storage");
			int movingFill = mounted.fill(new FluidStack(Fluids.WATER, 500), IFluidHandler.FluidAction.EXECUTE);
			require(helper, movingFill == 500, "fluid could not be inserted while mounted");

			Contraption restored = Contraption.fromNBT(helper.getLevel(), captured.writeNBT(false), false);
			IFluidHandler restoredHandler = CisternContraptionHelper.getFluidHandler(restored, BlockPos.ZERO);
			require(helper, restoredHandler != null && restoredHandler.getFluidInTank(0).getFluid() == Fluids.WATER
				&& restoredHandler.getFluidInTank(0).getAmount() == 1500,
				"mounted fluid did not survive contraption NBT");

			restored.addBlocksToWorld(helper.getLevel(),
				new StructureTransform(absoluteOrigin, Direction.Axis.Y, Rotation.NONE, Mirror.NONE));
			helper.runAfterDelay(7, () -> {
				CisternBlockEntity rebuilt = cisternAt(helper, origin);
				assertCisternDimensions(helper, rebuilt, 3, 1);
				require(helper, rebuilt.getTankInventory().getFluid().getFluid() == Fluids.WATER
					&& rebuilt.getTankInventory().getFluidAmount() == 1500,
					"disassembled cistern did not restore mounted fluid");
				CisternBlockEntity part = cisternAt(helper, origin.offset(0, 2, 2));
				require(helper, part.getController().equals(rebuilt.getBlockPos()),
					"disassembled cistern part did not route to the restored controller");
				helper.succeed();
			});
		});
	}

	@GameTest(template = EMPTY_TEMPLATE, setupTicks = 1, timeoutTicks = 220)
	public static void stationAssemblesAndDisassemblesRailwardStorage(GameTestHelper helper) {
		BlockPos stationPos = new BlockPos(12, 1, 5);
		BlockPos targetTrackPos = new BlockPos(12, 1, 8);
		BlockPos bogeyPos = new BlockPos(11, 2, 8);
		BlockPos slimePos = new BlockPos(10, 2, 8);
		BlockPos cargoPos = new BlockPos(9, 2, 8);
		BlockPos cisternPos = new BlockPos(10, 2, 7);
		BlockPos controlsPos = new BlockPos(10, 2, 9);
		BlockPos hatchPos = new BlockPos(10, 3, 8);
		BlockPos terminalPos = hatchPos.above();

		for (int x = 1; x <= 14; x++)
			helper.setBlock(new BlockPos(x, 1, 8), com.simibubi.create.AllBlocks.TRACK.get()
				.defaultBlockState()
				.setValue(TrackBlock.SHAPE, TrackShape.XO));
		helper.setBlock(stationPos, com.simibubi.create.AllBlocks.TRACK_STATION.get());
		helper.setBlock(bogeyPos, com.simibubi.create.AllBlocks.SMALL_BOGEY.get()
			.defaultBlockState()
			.setValue(AbstractBogeyBlock.AXIS, Direction.Axis.X));
		helper.setBlock(slimePos, Blocks.SLIME_BLOCK);
		helper.setBlock(cargoPos, AllBlocks.BULK_CARGO_CONTAINER.get());
		helper.setBlock(cisternPos, AllBlocks.CISTERN.get().defaultBlockState()
			.setValue(CisternBlock.AXIS, Direction.Axis.X));
		helper.setBlock(controlsPos, com.simibubi.create.AllBlocks.TRAIN_CONTROLS.get()
			.defaultBlockState()
			.setValue(ControlsBlock.FACING, Direction.WEST));
		helper.setBlock(hatchPos, AllBlocks.CISTERN_HATCH.get());
		helper.setBlock(terminalPos, AllBlocks.FLUID_TERMINAL.get());

		helper.runAfterDelay(5, () -> {
			BulkCargoContainerBlockEntity cargo = bulkCargoAt(helper, cargoPos);
			require(helper, cargo.tryInsert(new ItemStack(Items.IRON_INGOT, 80), false).isEmpty(),
				"cargo could not be prepared before station assembly");
			CisternBlockEntity cistern = cisternAt(helper, cisternPos);
			require(helper, cistern.getTankInventory().fill(new FluidStack(Fluids.WATER, 1000),
				IFluidHandler.FluidAction.EXECUTE) == 1000,
				"cistern could not be prepared before station assembly");

			BlockEntity blockEntity = helper.getBlockEntity(stationPos);
			require(helper, blockEntity instanceof StationBlockEntity,
				"track station did not create its block entity");
			StationBlockEntity station = (StationBlockEntity) blockEntity;
			CompoundTag stationTag = station.saveWithFullMetadata();
			stationTag.put("TargetTrack", NbtUtils.writeBlockPos(targetTrackPos.subtract(stationPos)));
			stationTag.putBoolean("TargetDirection", false);
			station.load(stationTag);
			station.setChanged();

			helper.runAfterDelay(25, () -> {
				require(helper, station.edgePoint.hasValidTrack(), "station lost its target track");
				require(helper, station.getStation() != null,
					"station edge point was not registered in the track graph");
				require(helper, station.enterAssemblyMode(null), "station did not enter assembly mode");
				station.refreshAssemblyInfo();
				station.assemble(null);

				Train train = station.getStation().getPresentTrain();
				require(helper, train != null, "station did not assemble a train");
				require(helper, train.carriages.size() == 1, "station assembled the wrong carriage count");
				require(helper, station.exitAssemblyMode(), "station did not leave assembly mode");

				helper.runAfterDelay(10, () -> {
					CarriageContraptionEntity entity = train.carriages.get(0)
						.getDimensional(helper.getLevel())
						.entity.get();
					require(helper, entity != null, "assembled carriage entity was not present on the server");
					Contraption contraption = entity.getContraption();
					require(helper, contraption instanceof CarriageContraption,
						"assembled entity did not contain a carriage contraption");
					require(helper, helper.getBlockState(cargoPos).isAir()
							&& helper.getBlockState(cisternPos).isAir()
							&& helper.getBlockState(hatchPos).isAir()
							&& helper.getBlockState(terminalPos).is(AllBlocks.FLUID_TERMINAL.get()),
						"Railward storage blocks remained in the world after train assembly");

					IItemHandlerModifiable movingCargo = BulkCargoContraptionHelper.getItemHandler(contraption,
						new BlockPos(-2, 0, 0));
					require(helper, movingCargo != null && countItems(movingCargo) == 80,
						"assembled train did not preserve cargo");
					require(helper, BulkCargoStorage.insert(movingCargo,
						new ItemStack(Items.IRON_INGOT, 16), false).isEmpty(),
						"assembled train cargo did not accept an update");

					IFluidHandler movingFluid = CisternContraptionHelper.getFluidHandler(contraption,
						new BlockPos(-1, 0, -1));
					require(helper, movingFluid != null && movingFluid.getFluidInTank(0).getFluid() == Fluids.WATER
						&& movingFluid.getFluidInTank(0).getAmount() == 1000,
						"assembled train did not preserve cistern fluid");
						BlockEntity terminalBlockEntity = helper.getBlockEntity(terminalPos);
						require(helper, terminalBlockEntity instanceof FluidTerminalBlockEntity,
							"fluid terminal did not create its registered block entity");
						FluidTerminalBlockEntity terminal = (FluidTerminalBlockEntity) terminalBlockEntity;
						require(helper, terminal.isConnectedToHatch(),
							"mounted cistern hatch did not connect to the fluid terminal before movement");
						IFluidHandler terminalHandler = fluidCapability(terminal, Direction.NORTH).orElse(null);
						require(helper, terminalHandler != null
							&& terminalHandler.fill(new FluidStack(Fluids.WATER, 500),
								IFluidHandler.FluidAction.EXECUTE) == 500,
							"fluid terminal did not fill the mounted cistern before movement");
						require(helper, movingFluid.getFluidInTank(0).getAmount() == 1500,
							"terminal fill did not reach the server-side mounted cistern");

					DimensionPalette dimensions = new DimensionPalette();
					CompoundTag serializedTrain = train.write(dimensions);
					Train restoredTrain = Train.read(serializedTrain,
						Create.RAILWAYS.sided(helper.getLevel()).trackNetworks, dimensions);
					require(helper, restoredTrain.id.equals(train.id) && restoredTrain.carriages.size() == 1,
						"train identity or carriage count did not survive NBT");
					require(helper, countItems(restoredTrain.carriages.get(0).storage.getItems()) == 96,
						"mounted cargo did not survive Train NBT");
					IFluidHandler persistedFluid = restoredTrain.carriages.get(0).storage.getFluids();
					require(helper, persistedFluid.getFluidInTank(0).getFluid() == Fluids.WATER
						&& persistedFluid.getFluidInTank(0).getAmount() == 1500,
						"mounted cistern fluid did not survive Train NBT");

					Carriage carriage = train.carriages.get(0);
					TravellingPoint leadingPoint = carriage.getLeadingPoint();
					TravellingPoint trailingPoint = carriage.getTrailingPoint();
					double leadingPosition = leadingPoint.position;
					double trailingPosition = trailingPoint.position;
					Vec3 entityPosition = entity.position();
					train.leaveStation();
					train.speed = .2;
					train.targetSpeed = .2;
					train.manualTick = true;

					helper.runAfterDelay(8, () -> {
						require(helper, entity.position().distanceToSqr(entityPosition) > .01,
							"assembled train entity did not move along the track");
						require(helper, leadingPoint.position != leadingPosition
							|| trailingPoint.position != trailingPosition,
							"train travelling points did not advance");
						require(helper, countItems(BulkCargoContraptionHelper.getItemHandler(contraption,
							new BlockPos(-2, 0, 0))) == 96,
							"cargo changed while the train moved");
						require(helper, CisternContraptionHelper.getFluidHandler(contraption,
							new BlockPos(-1, 0, -1)).getFluidInTank(0).getAmount() == 1500,
							"cistern fluid changed while the train moved");
						require(helper, !terminal.isConnectedToHatch(),
							"fluid terminal remained connected after the hatch moved away");

						train.speed = 0;
						train.targetSpeed = 0;
						leadingPoint.position = leadingPosition;
						trailingPoint.position = trailingPosition;
						carriage.updateContraptionAnchors();
						carriage.manageEntities(helper.getLevel());
						train.setCurrentStation(station.getStation());
						station.getStation().reserveFor(train);

						helper.runAfterDelay(8, () -> {
							require(helper, terminal.isConnectedToHatch(),
								"mounted cistern hatch did not reconnect after returning from movement");
							IFluidHandler reconnectedHandler = fluidCapability(terminal, Direction.NORTH)
								.orElse(null);
							FluidStack drained = reconnectedHandler == null ? FluidStack.EMPTY
								: reconnectedHandler.drain(new FluidStack(Fluids.WATER, 250),
									IFluidHandler.FluidAction.EXECUTE);
							require(helper, drained.getFluid() == Fluids.WATER && drained.getAmount() == 250,
								"fluid terminal did not drain the mounted cistern after movement");
							require(helper, movingFluid.getFluidInTank(0).getAmount() == 1250,
								"post-movement terminal drain did not update server-side mounted fluid");
							require(helper, station.tryDisassembleTrain(null),
								"station did not disassemble its train");
							helper.runAfterDelay(7, () -> {
								BulkCargoContainerBlockEntity rebuiltCargo = bulkCargoAt(helper, cargoPos);
								require(helper, rebuiltCargo.getStoredAmount() == 96,
									"station disassembly did not restore cargo contents");
								CisternBlockEntity rebuiltCistern = cisternAt(helper, cisternPos);
								require(helper,
									rebuiltCistern.getTankInventory().getFluid().getFluid() == Fluids.WATER
										&& rebuiltCistern.getTankInventory().getFluidAmount() == 1250,
									"station disassembly did not restore cistern contents");
								require(helper,
									!Create.RAILWAYS.sided(helper.getLevel()).trains.containsKey(train.id),
									"disassembled train remained registered");
								helper.succeed();
							});
						});
					});
				});
			});
		});
	}

	@GameTest(template = EMPTY_TEMPLATE, setupTicks = 1, timeoutTicks = 80)
	public static void arrivalForecastSelectsPhysicalStationAndFollowsWorldClock(GameTestHelper helper) {
		GlobalStation platformOne = new GlobalStation();
		platformOne.setId(UUID.randomUUID());
		platformOne.name = "Spawn Central";
		GlobalStation platformTwo = new GlobalStation();
		platformTwo.setId(UUID.randomUUID());
		platformTwo.name = "Spawn Central";
		String exactName = "\\QSpawn Central\\E";

		require(helper, StationArrivalSelection.matches(
			platformOne.getId(), null, platformOne.name, exactName),
			"unassigned exact-name call did not include the first matching platform");
		require(helper, StationArrivalSelection.matches(
			platformTwo.getId(), null, platformTwo.name, exactName),
			"unassigned exact-name call did not include the duplicate matching platform");
		require(helper, StationArrivalSelection.matches(
			platformOne.getId(), platformOne.getId(), platformOne.name, exactName),
			"assigned call did not select its physical platform");
		require(helper, !StationArrivalSelection.matches(
			platformTwo.getId(), platformOne.getId(), platformTwo.name, exactName),
			"assigned call leaked to a duplicate-name physical platform");

		platformOne.name = "Renamed Central";
		require(helper, StationArrivalSelection.matches(
			platformOne.getId(), platformOne.getId(), platformOne.name, exactName),
			"renaming the assigned platform invalidated its physical-station identity");
		platformOne.name = "Spawn Central P1";
		platformTwo.name = "Spawn Central P2";
		String wildcardPlatforms = "\\QSpawn Central P\\E.*\\Q\\E";
		require(helper, StationArrivalSelection.matches(
			platformOne.getId(), null, platformOne.name, wildcardPlatforms)
			&& StationArrivalSelection.matches(
				platformTwo.getId(), null, platformTwo.name, wildcardPlatforms),
			"wildcard call did not include both matching platform names");
		require(helper, !StationArrivalSelection.matches(
			platformOne.getId(), null, "Other P1", wildcardPlatforms),
			"wildcard call included a non-matching station name");

		ServerLevel level = helper.getLevel();
		boolean originalDaylightCycle = level.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT);
		long originalDayTime = level.getDayTime();
		level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
		level.setDayTime(6_000);
		WorldTimeProjection.Snapshot frozen = WorldTimeProjection.capture(level, false);
		require(helper, frozen.dayTicksPerServerTick() == 0 && frozen.project(200) == 6_000
			&& "frozen".equals(frozen.source()),
			"frozen daylight advanced the projected arrival clock");

		level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(true, level.getServer());
		WorldTimeProjection.capture(level, true);
		long observationStartGameTime = level.getGameTime();
		helper.runAfterDelay(20, () -> {
			long elapsedServerTicks = level.getGameTime() - observationStartGameTime;
			level.setDayTime(level.getDayTime() + elapsedServerTicks);
			WorldTimeProjection.Snapshot accelerated = WorldTimeProjection.capture(level, true);
			require(helper, "observed".equals(accelerated.source())
				&& Math.abs(accelerated.dayTicksPerServerTick() - 2) < .0001,
				"altered world clock rate was not observed as two day ticks per server tick");
			require(helper, accelerated.project(50) == accelerated.dayTime() + 100,
				"arrival projection did not follow the observed world clock rate");

			level.setDayTime(originalDayTime);
			WorldTimeProjection.capture(level, false);
			level.getGameRules().getRule(GameRules.RULE_DAYLIGHT)
				.set(originalDaylightCycle, level.getServer());
			helper.succeed();
		});
	}

	private static void placeBulkCargo(GameTestHelper helper, BlockPos origin, int sizeX, int height, int sizeZ) {
		for (int x = 0; x < sizeX; x++)
			for (int y = 0; y < height; y++)
				for (int z = 0; z < sizeZ; z++)
					helper.setBlock(origin.offset(x, y, z), AllBlocks.BULK_CARGO_CONTAINER.get());
	}

	private static void assertBulkCargoShape(GameTestHelper helper, BlockPos origin, int sizeX, int height,
		int sizeZ, boolean xLong) {
		BulkCargoContainerBlockEntity controller = bulkCargoAt(helper, origin);
		require(helper, controller.isController() && controller.getSizeX() == sizeX
			&& controller.getSizeZ() == sizeZ && controller.getHeight() == height,
			"bulk cargo formed the wrong " + sizeX + "x" + height + "x" + sizeZ + " dimensions");
		for (int x = 0; x < sizeX; x++)
			for (int y = 0; y < height; y++)
				for (int z = 0; z < sizeZ; z++) {
					BlockPos pos = origin.offset(x, y, z);
					BulkCargoContainerBlockEntity part = bulkCargoAt(helper, pos);
					require(helper, part.getControllerBE() == controller
						&& part.getController().equals(controller.getBlockPos()),
						"bulk cargo part did not route to its controller at " + pos);
					BlockState state = helper.getBlockState(pos);
					require(helper, state.getValue(BulkCargoContainerBlock.WEST) == (x == 0)
						&& state.getValue(BulkCargoContainerBlock.EAST) == (x == sizeX - 1)
						&& state.getValue(BulkCargoContainerBlock.NORTH) == (z == 0)
						&& state.getValue(BulkCargoContainerBlock.SOUTH) == (z == sizeZ - 1)
						&& state.getValue(BulkCargoContainerBlock.BOTTOM) == (y == 0)
						&& state.getValue(BulkCargoContainerBlock.TOP) == (y == height - 1)
						&& state.getValue(BulkCargoContainerBlock.X_LONG) == xLong,
						"bulk cargo boundary/orientation state was wrong at " + pos);
				}
	}

	private static void insertBulkCargo(GameTestHelper helper, BulkCargoContainerBlockEntity cargo,
		net.minecraft.world.item.Item item, int amount) {
		int remaining = amount;
		while (remaining > 0) {
			int batch = Math.min(64, remaining);
			ItemStack remainder = cargo.tryInsert(new ItemStack(item, batch), false);
			require(helper, remainder.isEmpty(), "bulk cargo rejected a valid fill-state batch");
			remaining -= batch;
		}
	}

	private static IItemHandlerModifiable assertMountedBulkCargo(GameTestHelper helper, Contraption contraption,
		BlockPos controllerPos, BlockPos partPos, int expectedAmount, int expectedLevel) {
		BulkCargoContraptionHelper.ControllerData controllerData =
			BulkCargoContraptionHelper.resolveController(contraption, controllerPos);
		BulkCargoContraptionHelper.ControllerData partData =
			BulkCargoContraptionHelper.resolveController(contraption, partPos);
		require(helper, controllerData != null && partData != null
			&& controllerData.controllerPos().equals(controllerPos)
			&& partData.controllerPos().equals(controllerPos)
			&& controllerData.sizeX() == 2 && controllerData.sizeZ() == 1 && controllerData.height() == 1
			&& partData.sizeX() == 2 && partData.sizeZ() == 1 && partData.height() == 1,
			"mounted bulk cargo did not preserve its controller selection or dimensions");
		IItemHandlerModifiable handler = BulkCargoContraptionHelper.getItemHandler(contraption, controllerPos);
		require(helper, handler != null && handler.getSlots() == 54 && countItems(handler) == expectedAmount,
			"mounted bulk cargo exposed the wrong handler, capacity or amount");
		for (BlockPos pos : List.of(controllerPos, partPos)) {
			StructureBlockInfo info = contraption.getBlocks().get(pos);
			require(helper, info != null
				&& info.state.getValue(BulkCargoContainerBlock.CARGO_LEVEL) == expectedLevel,
				"mounted bulk cargo exposed the wrong fill level at " + pos);
		}
		return handler;
	}

	private static void placeCistern(GameTestHelper helper, BlockPos origin, Direction.Axis axis, int width,
		int length) {
		for (int longIndex = 0; longIndex < length; longIndex++)
			for (int y = 0; y < width; y++)
				for (int cross = 0; cross < width; cross++) {
					BlockPos pos = axis == Direction.Axis.X ? origin.offset(longIndex, y, cross)
						: origin.offset(cross, y, longIndex);
					helper.setBlock(pos, AllBlocks.CISTERN.get().defaultBlockState()
						.setValue(CisternBlock.AXIS, axis));
				}
	}

	private static void runWidthOneCisternFixture(GameTestHelper helper, int caseIndex) {
		int caseCount = CisternBlockEntity.MAX_LENGTH * 2;
		if (caseIndex >= caseCount) {
			helper.succeed();
			return;
		}

		BlockPos origin = new BlockPos(1, 1, 1);
		Direction.Axis axis = caseIndex % 2 == 0 ? Direction.Axis.X : Direction.Axis.Z;
		int length = caseIndex / 2 + 1;
		CisternFixture fixture = new CisternFixture(origin, axis, 1, length);
		for (int offset = 0; offset < CisternBlockEntity.MAX_LENGTH; offset++) {
			helper.setBlock(origin.offset(offset, 0, 0), Blocks.AIR);
			helper.setBlock(origin.offset(0, 0, offset), Blocks.AIR);
		}
		placeCistern(helper, origin, axis, 1, length);

		helper.runAfterDelay(5, () -> {
			CisternBlockEntity controller = cisternAt(helper, origin);
			assertCisternDimensions(helper, controller, 1, length);
			BlockPos end = cisternPosition(fixture, length - 1, 0, 0);
			require(helper, cisternAt(helper, end).getControllerBE() == controller,
				"width-one cistern end did not route to its controller for " + fixture);
			require(helper, controller.getTankInventory().getCapacity()
				== length * CisternBlockEntity.getCapacityMultiplier(),
				"width-one cistern has the wrong capacity for " + fixture);
			for (int offset = 0; offset < length; offset++)
				helper.setBlock(cisternPosition(fixture, offset, 0, 0), Blocks.AIR);
			helper.runAfterDelay(2, () -> runWidthOneCisternFixture(helper, caseIndex + 1));
		});
	}

	private static void assertCisternDimensions(GameTestHelper helper, CisternBlockEntity controller, int width,
		int length) {
		require(helper, controller.isController(), "cistern origin did not become controller");
		require(helper, controller.getCisternWidth() == width && controller.getCisternLength() == length,
			"cistern formed the wrong dimensions");
	}

	private static void assertCisternPortsAndRoutedFluid(GameTestHelper helper, CisternFixture fixture) {
		CisternBlockEntity controller = cisternAt(helper, fixture.origin());
		assertCisternDimensions(helper, controller, fixture.width(), fixture.length());
		require(helper, controller.getCisternAxis() == fixture.axis(),
			"cistern controller formed on the wrong axis");
		require(helper, controller.getTankInventory().getCapacity() == fixture.width() * fixture.width()
			* fixture.length() * CisternBlockEntity.getCapacityMultiplier(),
			"cistern exposed the wrong capacity for " + fixture);

		List<IFluidHandler> routedHandlers = new ArrayList<>();
		for (int longIndex = 0; longIndex < fixture.length(); longIndex++)
			for (int y = 0; y < fixture.width(); y++)
				for (int cross = 0; cross < fixture.width(); cross++) {
					BlockPos pos = cisternPosition(fixture, longIndex, y, cross);
					CisternBlockEntity part = cisternAt(helper, pos);
					require(helper, part.getControllerBE() == controller
						&& part.getController().equals(controller.getBlockPos()),
						"cistern part did not route to its controller at " + pos);
					for (Direction side : Direction.values()) {
						boolean expected = isExpectedCisternPort(fixture, longIndex, y, cross, side);
						IFluidHandler handler = fluidCapability(part, side).orElse(null);
						require(helper, (handler != null) == expected,
							"fluid capability was " + (handler == null ? "hidden" : "exposed")
								+ " on " + side + " at " + pos + " for " + fixture);
						if (handler != null)
							routedHandlers.add(handler);
					}
				}

		int endPortsPerFace = fixture.width() == CisternBlockEntity.WIDTH_LARGE ? 9 : 1;
		int expectedPortCount = endPortsPerFace * 2
			+ (fixture.width() == 1 ? 0 : Math.max(0, fixture.length() - 2) * 4);
		require(helper, routedHandlers.size() == expectedPortCount,
			"cistern exposed " + routedHandlers.size() + " ports instead of " + expectedPortCount
				+ " for " + fixture);

		IFluidHandler firstPort = routedHandlers.get(0);
		IFluidHandler lastPort = routedHandlers.get(routedHandlers.size() - 1);
		int partialAmount = CisternBlockEntity.getCapacityMultiplier() + 250;
		require(helper, firstPort.fill(new FluidStack(Fluids.WATER, partialAmount),
			IFluidHandler.FluidAction.SIMULATE) == partialAmount,
			"routed cistern port simulated the wrong fill amount for " + fixture);
		require(helper, controller.getTankInventory().getFluidAmount() == 0,
			"simulated cistern fill mutated the controller for " + fixture);
		require(helper, firstPort.fill(new FluidStack(Fluids.WATER, partialAmount),
			IFluidHandler.FluidAction.EXECUTE) == partialAmount,
			"routed cistern port rejected the executed fill for " + fixture);
		require(helper, lastPort.getFluidInTank(0).getFluid() == Fluids.WATER
			&& lastPort.getFluidInTank(0).getAmount() == partialAmount,
			"remote cistern port did not expose the controller fluid for " + fixture);

		FluidStack simulatedDrain = lastPort.drain(250, IFluidHandler.FluidAction.SIMULATE);
		require(helper, simulatedDrain.getFluid() == Fluids.WATER && simulatedDrain.getAmount() == 250,
			"routed cistern port simulated the wrong drain for " + fixture);
		require(helper, controller.getTankInventory().getFluidAmount() == partialAmount,
			"simulated cistern drain mutated the controller for " + fixture);
		FluidStack executedDrain = lastPort.drain(250, IFluidHandler.FluidAction.EXECUTE);
		require(helper, executedDrain.getFluid() == Fluids.WATER && executedDrain.getAmount() == 250
			&& controller.getTankInventory().getFluidAmount() == partialAmount - 250,
			"routed cistern port executed the wrong drain for " + fixture);
		require(helper, lastPort.fill(executedDrain, IFluidHandler.FluidAction.EXECUTE) == 250
			&& controller.getTankInventory().getFluidAmount() == partialAmount,
			"routed cistern port did not restore the drained fluid for " + fixture);
	}

	private static BlockPos cisternPosition(CisternFixture fixture, int longIndex, int y, int cross) {
		return fixture.axis() == Direction.Axis.X ? fixture.origin().offset(longIndex, y, cross)
			: fixture.origin().offset(cross, y, longIndex);
	}

	private static boolean isExpectedCisternPort(CisternFixture fixture, int longIndex, int y, int cross,
		Direction side) {
		if (side.getAxis() == fixture.axis()) {
			boolean onNegativeEnd = side.getAxisDirection() == Direction.AxisDirection.NEGATIVE && longIndex == 0;
			boolean onPositiveEnd = side.getAxisDirection() == Direction.AxisDirection.POSITIVE
				&& longIndex == fixture.length() - 1;
			if (!onNegativeEnd && !onPositiveEnd)
				return false;
			if (fixture.width() == 1)
				return true;
			if (fixture.width() == CisternBlockEntity.WIDTH_SMALL)
				return cross == 1 && y == 1;
			return cross >= 1 && cross <= 3 && y >= 1 && y <= 3;
		}

		if (fixture.width() == 1 || longIndex == 0 || longIndex == fixture.length() - 1)
			return false;
		int center = fixture.width() / 2;
		if (side == Direction.UP)
			return y == fixture.width() - 1 && cross == center;
		if (side == Direction.DOWN)
			return y == 0 && cross == center;
		if (fixture.axis() == Direction.Axis.X)
			return side == Direction.NORTH && cross == 0 && y == center
				|| side == Direction.SOUTH && cross == fixture.width() - 1 && y == center;
		return side == Direction.WEST && cross == 0 && y == center
			|| side == Direction.EAST && cross == fixture.width() - 1 && y == center;
	}

	private static LazyOptional<IFluidHandler> fluidCapability(BlockEntity blockEntity, Direction side) {
		//? if >=1.20.1
		/*return blockEntity.getCapability(ForgeCapabilities.FLUID_HANDLER, side);*/
		//? if <1.20.1
		return blockEntity.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, side);
	}

	private static LazyOptional<IItemHandler> itemCapability(BlockEntity blockEntity, Direction side) {
		//? if >=1.20.1
		/*return blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER, side);*/
		//? if <1.20.1
		return blockEntity.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, side);
	}

	private static int countItems(IItemHandler inventory) {
		int amount = 0;
		for (int slot = 0; slot < inventory.getSlots(); slot++)
			amount += inventory.getStackInSlot(slot).getCount();
		return amount;
	}

	private static int countItems(Container inventory) {
		int amount = 0;
		for (int slot = 0; slot < inventory.getContainerSize(); slot++)
			amount += inventory.getItem(slot).getCount();
		return amount;
	}

	private static BulkCargoContainerBlockEntity bulkCargoAt(GameTestHelper helper, BlockPos pos) {
		BlockEntity blockEntity = helper.getBlockEntity(pos);
		require(helper, blockEntity instanceof BulkCargoContainerBlockEntity,
			"expected a bulk cargo block entity at " + pos);
		return (BulkCargoContainerBlockEntity) blockEntity;
	}

	private static BarrelBlockEntity barrelAt(GameTestHelper helper, BlockPos pos) {
		BlockEntity blockEntity = helper.getBlockEntity(pos);
		require(helper, blockEntity instanceof BarrelBlockEntity, "expected a barrel block entity at " + pos);
		return (BarrelBlockEntity) blockEntity;
	}

	private static CisternBlockEntity cisternAt(GameTestHelper helper, BlockPos pos) {
		BlockEntity blockEntity = helper.getBlockEntity(pos);
		require(helper, blockEntity instanceof CisternBlockEntity, "expected a cistern block entity at " + pos);
		return (CisternBlockEntity) blockEntity;
	}

	private static void require(GameTestHelper helper, boolean condition, String message) {
		if (!condition)
			helper.fail(message);
	}

	private static void setFluidNetworkField(FluidNetwork network, String fieldName, Object value) {
		try {
			Field field = FluidNetwork.class.getDeclaredField(fieldName);
			field.setAccessible(true);
			field.set(network, value);
		} catch (ReflectiveOperationException exception) {
			throw new IllegalStateException("Could not prepare FluidNetwork field " + fieldName, exception);
		}
	}

	private record CisternFixture(BlockPos origin, Direction.Axis axis, int width, int length) {
	}

	private static final class CapturedCarriageContraption extends CarriageContraption {

		private CapturedCarriageContraption() {
			super(Direction.EAST);
		}

		private void captureBlocks(Level level, BlockPos anchorPos, List<BlockPos> positions) {
			anchor = anchorPos;
			bounds = new AABB(BlockPos.ZERO);
			for (BlockPos pos : positions)
				addBlock(pos, capture(level, pos));
		}

		private void beginMovement(Level level) {
			removeBlocksFromWorld(level, BlockPos.ZERO);
			storage.createHandlers();
		}
	}
}
