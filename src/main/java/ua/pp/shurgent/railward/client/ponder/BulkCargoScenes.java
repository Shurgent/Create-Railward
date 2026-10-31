package ua.pp.shurgent.railward.client.ponder;

import com.simibubi.create.foundation.ponder.PonderPalette;
import com.simibubi.create.foundation.ponder.SceneBuilder;
import com.simibubi.create.foundation.ponder.SceneBuildingUtil;
import com.simibubi.create.foundation.ponder.Selection;
//? if create6 {
/*import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
*///?} else {
import com.simibubi.create.foundation.ponder.element.InputWindowElement;
//?}
import com.simibubi.create.foundation.utility.Pointing;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
//? if >=1.20.1
/*import net.minecraftforge.common.capabilities.ForgeCapabilities;*/
//? if <1.20.1
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;
import ua.pp.shurgent.railward.AllBlocks;
import ua.pp.shurgent.railward.block.train.parts.BulkCargoContainerBlock;
import ua.pp.shurgent.railward.block.train.parts.BulkCargoContainerBlockEntity;
import ua.pp.shurgent.railward.block.train.parts.BulkCargoStorage;
import ua.pp.shurgent.railward.block.train.station.UnloadingBunkerBlockEntity;
import ua.pp.shurgent.railward.block.train.station.UnloadingHatchBlockEntity;

/** Client-only demonstrations. Inventories are real; placement and transfer timing are scripted. */
public final class BulkCargoScenes {

	public static final BlockPos EXAMPLE = new BlockPos(2, 1, 3);
	public static final BlockPos HAND_CONTAINER = new BlockPos(4, 2, 4);
	public static final BlockPos HAND_SOURCE = new BlockPos(5, 3, 4);
	public static final BlockPos HAND_OUTPUT = new BlockPos(4, 0, 4);
	public static final BlockPos WAGON = new BlockPos(4, 5, 6);
	public static final BlockPos SUPPLY = new BlockPos(7, 9, 6);
	public static final BlockPos LOADER = new BlockPos(7, 8, 6);
	public static final BlockPos BUNKER = new BlockPos(11, 2, 6);
	public static final BlockPos OUTPUT = new BlockPos(5, 0, 4);
	public static final int TRAIN_AMOUNT = 46656;

	private BulkCargoScenes() {}

	public static void storage(SceneBuilder scene, SceneBuildingUtil util) {
		scene.title("bulk_storage", "Building and using bulk storage");
		scene.configureBasePlate(0, 0, 9);
		scene.scaleSceneView(.85f);
		scene.setSceneOffsetY(-1);
		scene.showBasePlate();
		form(scene, util, EXAMPLE, 1, 1, 1, AllBlocks.BULK_CARGO_CONTAINER.get());
		var single = scene.world.showIndependentSection(area(util, EXAMPLE, 1, 1, 1), Direction.DOWN);
		text(scene, util, EXAMPLE, PonderPalette.GREEN,
			"Bulk Cargo Containers of the same colour combine into one rectangular storage.");
		scene.world.hideIndependentSection(single, Direction.UP);
		scene.idle(25);
		form(scene, util, EXAMPLE, 3, 1, 3, AllBlocks.BULK_CARGO_CONTAINER.get());
		var square = scene.world.showIndependentSection(util.select.position(EXAMPLE), Direction.DOWN);
		for (int x = 0; x < 3; x++) for (int z = 0; z < 3; z++) {
			if (x == 0 && z == 0) continue;
			scene.world.showSectionAndMerge(util.select.position(EXAMPLE.offset(x, 0, z)), Direction.DOWN, square);
			scene.idle(5);
		}
		text(scene, util, EXAMPLE, PonderPalette.GREEN,
			"Fill the whole footprint: a square base makes a wide open container.");
		scene.world.hideIndependentSection(square, Direction.UP);
		scene.idle(25);
		form(scene, util, EXAMPLE, 5, 1, 2, AllBlocks.BULK_CARGO_CONTAINER.get());
		var rectangle = scene.world.showIndependentSection(area(util, EXAMPLE, 1, 1, 2), Direction.DOWN);
		for (int x = 1; x < 5; x++) {
			scene.world.showSectionAndMerge(area(util, EXAMPLE.east(x), 1, 1, 2), Direction.DOWN, rectangle);
			scene.idle(8);
		}
		text(scene, util, EXAMPLE.east(2), PonderPalette.GREEN,
			"Rectangular bases work too: length, width and height do not have to be equal.");
		scene.world.hideIndependentSection(rectangle, Direction.UP);
		scene.idle(25);
		form(scene, util, EXAMPLE, 3, 3, 3, AllBlocks.BULK_CARGO_CONTAINER.get());
		scene.world.showSection(area(util, EXAMPLE, 3, 1, 3), Direction.DOWN);
		controls(scene, Vec3.atCenterOf(EXAMPLE.offset(1, 1, 1)), AllBlocks.BULK_CARGO_CONTAINER.asStack());
		scene.world.showSection(area(util, EXAMPLE.above(), 3, 1, 3), Direction.DOWN);
		scene.idle(25);
		scene.world.showSection(area(util, EXAMPLE.above(2), 3, 1, 3), Direction.DOWN);
		text(scene, util, EXAMPLE.above(2), PonderPalette.BLUE,
			"Right-click the top with matching container blocks to add a complete layer. You need enough blocks for that layer. Maximum size: 11 by 11 by 16.");
		scene.world.hideSection(area(util, EXAMPLE, 3, 3, 3), Direction.UP);
		scene.idle(25);
		form(scene, util, EXAMPLE, 3, 2, 2, AllBlocks.BULK_CARGO_CONTAINER.get());
		BlockPos blue = EXAMPLE.east(3);
		form(scene, util, blue, 2, 2, 2, AllBlocks.BULK_CARGO_CONTAINER_BLUE.get());
		scene.world.showSection(area(util, EXAMPLE, 3, 2, 2), Direction.DOWN);
		scene.world.showSection(area(util, blue, 2, 2, 2), Direction.DOWN);
		scene.overlay.showOutline(PonderPalette.GREEN, "green_store", area(util, EXAMPLE, 3, 2, 2), 180);
		scene.overlay.showOutline(PonderPalette.BLUE, "blue_store", area(util, blue, 2, 2, 2), 180);
		text(scene, util, blue, PonderPalette.BLUE,
			"Different colours stay separate even when they touch. Each colour keeps its own walls and inventory.");
		scene.world.hideSection(util.select.fromTo(2, 1, 3, 6, 3, 5), Direction.UP);
		scene.idle(25);
		scene.world.setBlocks(util.select.fromTo(2, 1, 3, 6, 3, 5), Blocks.AIR.defaultBlockState(), false);
		form(scene, util, HAND_CONTAINER, 1, 1, 1, AllBlocks.BULK_CARGO_CONTAINER.get());
		scene.world.showSection(util.select.position(HAND_CONTAINER), Direction.DOWN);
		caption(scene, util, HAND_CONTAINER, PonderPalette.GREEN,
			"Throw items into the open top. The container collects them; one storage holds one cargo type.", 180);
		for (int i = 0; i < 6; i++) {
			var item = scene.world.createItemEntity(Vec3.atCenterOf(HAND_CONTAINER.above(2)), new Vec3(0, -.1, 0), new ItemStack(Items.SAND, 64));
			var second = scene.world.createItemEntity(Vec3.atCenterOf(HAND_CONTAINER.above(2)).add(.15, .2, 0), new Vec3(0, -.1, 0), new ItemStack(Items.SAND, 64));
			var third = scene.world.createItemEntity(Vec3.atCenterOf(HAND_CONTAINER.above(2)).add(-.15, .4, 0), new Vec3(0, -.1, 0), new ItemStack(Items.SAND, 64));
			scene.idle(15);
			scene.world.modifyEntity(item, Entity::discard);
			scene.world.modifyEntity(second, Entity::discard);
			scene.world.modifyEntity(third, Entity::discard);
			insert(scene, HAND_CONTAINER, 192);
			scene.idle(5);
		}
		scene.idle(70);
		controls(scene, Vec3.atCenterOf(HAND_CONTAINER.above()), ItemStack.EMPTY);
		caption(scene, util, HAND_CONTAINER, PonderPalette.WHITE,
			"Right-click with an empty hand to take one stack of cargo.", 140);
		scene.world.modifyBlockEntity(HAND_CONTAINER, BulkCargoContainerBlockEntity.class, be -> be.extractOneStack(false));
		sync(scene, HAND_CONTAINER);
		var taken = scene.world.createItemEntity(Vec3.atCenterOf(HAND_CONTAINER).add(-.7, .5, -.7), new Vec3(-.04, .1, -.08), new ItemStack(Items.SAND, 64));
		scene.idle(25);
		scene.world.modifyEntity(taken, Entity::discard);
		scene.idle(125);
		scene.world.setBlock(HAND_CONTAINER.below(), Blocks.HOPPER.defaultBlockState(), false);
		scene.world.setBlock(HAND_CONTAINER.east(), Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.WEST), false);
		scene.world.setBlock(HAND_SOURCE, Blocks.BARREL.defaultBlockState(), false);
		scene.world.showSection(util.select.fromTo(4, 0, 4, 5, 3, 4).substract(util.select.position(HAND_CONTAINER)), Direction.DOWN);
		seed(scene, HAND_SOURCE, 512);
		caption(scene, util, HAND_CONTAINER, PonderPalette.GREEN,
			"Hoppers and other blocks that interact with inventories can insert and extract cargo too.", 220);
		for (int i = 0; i < 8; i++) {
			transfer(scene, HAND_SOURCE, HAND_CONTAINER, 64);
			sync(scene, HAND_CONTAINER);
			scene.idle(10);
		}
		for (int i = 0; i < 8; i++) {
			transfer(scene, HAND_CONTAINER, HAND_OUTPUT, 64);
			sync(scene, HAND_CONTAINER);
			scene.idle(10);
		}
		scene.idle(75);
		scene.markAsFinished();
	}

	public static void transport(SceneBuilder scene, SceneBuildingUtil util) {
		scene.title("bulk_transport", "Loading and unloading a bulk wagon");
		scene.configureBasePlate(1, 0, 16);
		scene.scaleSceneView(.7f);
		scene.setSceneOffsetY(-4);
		scene.showBasePlate();
		scene.world.showSection(util.select.fromTo(1, 1, 7, 14, 2, 7), Direction.DOWN);
		scene.world.showSection(util.select.fromTo(0, 3, 7, 37, 3, 7), Direction.DOWN);
		var train = scene.world.showIndependentSection(trainArea(util), Direction.DOWN);
		scene.world.moveSection(train, new Vec3(-4, 0, 0), 0);
		text(scene, util, WAGON.above(), PonderPalette.WHITE,
			"A bulk wagon is served by an overhead hatch for loading and a bunker below for unloading.");
		scene.world.moveSection(train, new Vec3(4, 0, 0), 45);
		wheels(scene, 4, 45);
		scene.idle(50);
		scene.world.showSection(util.select.fromTo(7, 8, 6, 9, 11, 8), Direction.DOWN);
		seed(scene, SUPPLY, TRAIN_AMOUNT);
		sync(scene, SUPPLY);
		text(scene, util, LOADER, PonderPalette.BLUE,
			"Place the loading hatch directly above the cargo container, 1 to 3 blocks higher than its top layer.");
		caption(scene, util, LOADER, PonderPalette.GREEN,
			"The hatch takes cargo from the inventory directly above it and feeds the wagon below. No separate portable interface is needed.", 280);
		for (int i = 1; i <= 32; i++) {
			transfer(scene, SUPPLY, WAGON, TRAIN_AMOUNT / 32);
			sync(scene, WAGON);
			sync(scene, SUPPLY);
			loadingEffect(scene, i);
			scene.idle(8);
		}
		scene.idle(35);
		scene.world.modifyBlockEntity(LOADER, UnloadingHatchBlockEntity.class, be -> {
			be.readClient(new CompoundTag());
			be.tick();
		});
		scene.world.hideSection(util.select.fromTo(7, 8, 6, 9, 11, 8), Direction.UP);
		scene.idle(25);
		scene.world.showSection(util.select.fromTo(10, 0, 4, 12, 2, 6).substract(trainArea(util)), Direction.UP);
		scene.world.moveSection(train, new Vec3(4, 0, 0), 45);
		wheels(scene, 4, 45);
		scene.idle(50);
		text(scene, util, BUNKER.above(2), PonderPalette.BLUE,
			"For unloading, a hatch in the wagon floor must line up above the bunker. Their blocks can be 1 to 3 blocks apart vertically.");
		caption(scene, util, BUNKER, PonderPalette.GREEN,
			"The bunker exposes the wagon inventory. Extract items from it with a hopper or another inventory-interacting block to unload the wagon.", 280);
		scene.world.modifyBlockEntity(BUNKER, UnloadingBunkerBlockEntity.class,
			be -> be.startUnloadingFrom(handler(be.getLevel().getBlockEntity(WAGON)), BUNKER.above(2)));
		for (int i = 0; i < 32; i++) {
			transfer(scene, BUNKER, OUTPUT, TRAIN_AMOUNT / 32);
			sync(scene, WAGON);
			sync(scene, OUTPUT);
			scene.idle(8);
		}
		scene.idle(35);
		scene.addInstruction(s -> {
			var bunker = (UnloadingBunkerBlockEntity) s.getWorld().getBlockEntity(BUNKER);
			CompoundTag tag = bunker.saveWithFullMetadata();
			tag.putInt("Timer", 0);
			bunker.readClient(tag);
			bunker.tick();
		});
		scene.world.moveSection(train, new Vec3(4, 0, 0), 60);
		wheels(scene, 4, 60);
		text(scene, util, BUNKER, PonderPalette.WHITE,
			"When the wagon leaves, the connection ends. The cargo already unloaded remains in the receiving storage.");
		scene.markAsFinished();
	}

	private static Selection trainArea(SceneBuildingUtil util) {
		return util.select.fromTo(3, 4, 6, 13, 6, 8).add(util.select.fromTo(14, 3, 5, 29, 8, 9)
			.substract(util.select.fromTo(14, 3, 7, 29, 3, 7)));
	}

	private static Selection area(SceneBuildingUtil util, BlockPos p, int x, int y, int z) {
		return util.select.fromTo(p, p.offset(x - 1, y - 1, z - 1));
	}

	private static void form(SceneBuilder scene, SceneBuildingUtil util, BlockPos origin, int sx, int sy, int sz, BulkCargoContainerBlock block) {
		for (int x = 0; x < sx; x++) for (int y = 0; y < sy; y++) for (int z = 0; z < sz; z++) {
			BlockPos pos = origin.offset(x, y, z);
			BlockState state = block.defaultBlockState().setValue(BulkCargoContainerBlock.WEST, x == 0)
				.setValue(BulkCargoContainerBlock.EAST, x == sx - 1).setValue(BulkCargoContainerBlock.NORTH, z == 0)
				.setValue(BulkCargoContainerBlock.SOUTH, z == sz - 1).setValue(BulkCargoContainerBlock.BOTTOM, y == 0)
				.setValue(BulkCargoContainerBlock.TOP, y == sy - 1).setValue(BulkCargoContainerBlock.X_LONG, sx >= sz);
			scene.world.setBlock(pos, state, false);
			scene.world.modifyBlockEntityNBT(util.select.position(pos), BulkCargoContainerBlockEntity.class, tag -> {
				tag.putInt("SizeX", sx); tag.putInt("SizeZ", sz); tag.putInt("Height", sy);
				tag.put("LastKnownPos", NbtUtils.writeBlockPos(pos));
				if (pos.equals(origin)) {
					tag.remove("Controller");
					tag.put("Inventory", new ItemStackHandler(sx * sy * sz * 27).serializeNBT());
				} else { tag.put("Controller", NbtUtils.writeBlockPos(origin)); tag.remove("Inventory"); }
			});
		}
	}

	public static IItemHandler handler(net.minecraft.world.level.block.entity.BlockEntity be) {
		return be.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY).orElseThrow(() -> new IllegalStateException("Missing Ponder inventory at " + be.getBlockPos()));
	}

	private static void seed(SceneBuilder scene, BlockPos pos, int amount) {
		scene.addInstruction(s -> ItemHandlerHelper.insertItemStacked(handler(s.getWorld().getBlockEntity(pos)), new ItemStack(Items.SAND, amount), false));
	}

	private static void insert(SceneBuilder scene, BlockPos pos, int amount) {
		seed(scene, pos, amount);
		sync(scene, pos);
	}

	private static void transfer(SceneBuilder scene, BlockPos from, BlockPos to, int amount) {
		scene.addInstruction(s -> {
			IItemHandler source = handler(s.getWorld().getBlockEntity(from));
			IItemHandler target = handler(s.getWorld().getBlockEntity(to));
			int remaining = amount;
			for (int slot = 0; slot < source.getSlots() && remaining > 0; slot++) {
				ItemStack available = source.extractItem(slot, Math.min(remaining, 64), true);
				int accepted = available.getCount() - ItemHandlerHelper.insertItemStacked(target, available, true).getCount();
				if (accepted == 0) continue;
				ItemStack extracted = source.extractItem(slot, accepted, false);
				ItemStack leftover = ItemHandlerHelper.insertItemStacked(target, extracted, false);
				if (!leftover.isEmpty()) throw new IllegalStateException("Ponder transfer lost items");
				remaining -= accepted;
			}
		});
	}

	private static void sync(SceneBuilder scene, BlockPos origin) {
		scene.addInstruction(s -> {
			var root = (BulkCargoContainerBlockEntity) s.getWorld().getBlockEntity(origin);
			IItemHandler inventory = handler(root);
			ItemStack display = BulkCargoStorage.getDisplayStack(inventory);
			int amount = BulkCargoStorage.getStoredAmount(inventory);
			int capacity = BulkCargoStorage.getCapacityItems(inventory, display);
			for (BlockPos pos : BlockPos.betweenClosed(origin, origin.offset(root.getSizeX() - 1, root.getHeight() - 1, root.getSizeZ() - 1)))
				if (s.getWorld().getBlockEntity(pos) instanceof BulkCargoContainerBlockEntity part)
					part.applyClientCargo(display, amount, capacity, pos.immutable(), origin);
		});
	}

	private static void loadingEffect(SceneBuilder scene, int sequence) {
		scene.addInstruction(s -> {
			var hatch = (UnloadingHatchBlockEntity) s.getWorld().getBlockEntity(LOADER);
			var wagon = (BulkCargoContainerBlockEntity) s.getWorld().getBlockEntity(WAGON);
			CompoundTag tag = hatch.saveWithFullMetadata();
			tag.put("TransferStack", new ItemStack(Items.SAND).save(new CompoundTag()));
			tag.putInt("TransferEffectTicks", 20); tag.putInt("TransferDistance", 2);
			tag.putInt("TransferVisualItems", 6); tag.putInt("TransferVisualSequence", sequence);
			tag.putFloat("TransferVisualEndY", WAGON.getY() + Math.max(.125f, wagon.getFillState() * 2) - LOADER.getY());
			hatch.readClient(tag);
		});
	}

	private static void wheels(SceneBuilder scene, float distance, int duration) {
		for (int x : new int[] {5, 11, 18, 25}) {
			//? if create6 {
			/*new CreateSceneBuilder(scene).world().animateBogey(new BlockPos(x, 4, 7), distance, duration);
			*///?} else {
			scene.world.animateBogey(new BlockPos(x, 4, 7), distance, duration);
			//?}
		}
	}

	private static void controls(SceneBuilder scene, Vec3 target, ItemStack item) {
		//? if create6 {
		/*scene.overlay().showControls(target, Pointing.DOWN, 100).rightClick().withItem(item);
		*///?} else {
		scene.overlay.showControls(new InputWindowElement(target, Pointing.DOWN).rightClick().withItem(item), 100);
		//?}
	}

	private static void caption(SceneBuilder scene, SceneBuildingUtil util, BlockPos target, PonderPalette colour, String english, int duration) {
		scene.overlay.showText(duration).text(english).colored(colour).independent(40).attachKeyFrame();
		scene.overlay.showOutline(colour, "bulk_focus", util.select.position(target), duration);
	}

	private static void text(SceneBuilder scene, SceneBuildingUtil util, BlockPos target, PonderPalette colour, String english) {
		caption(scene, util, target, colour, english, 160);
		scene.idle(175);
	}
}
