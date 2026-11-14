package ua.pp.shurgent.railward.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import ua.pp.shurgent.railward.AllBlockEntities;
import ua.pp.shurgent.railward.AllBlocks;
import ua.pp.shurgent.railward.Railward;
import ua.pp.shurgent.railward.block.train.signal.RailCrossingSignalBlock;
import ua.pp.shurgent.railward.block.train.signal.RailCrossingSignalBlockEntity;
import ua.pp.shurgent.railward.block.train.signal.RailCrossingSignalBlockEntity.CrossingSign;
import ua.pp.shurgent.railward.block.train.signal.RailCrossingSignalBlockEntity.CrossingSound;
import ua.pp.shurgent.railward.block.train.signal.RailCrossingSignalBlockEntity.Lamp;
import ua.pp.shurgent.railward.block.train.signal.RailCrossingSignalItem;

@GameTestHolder(Railward.MOD_ID)
@PrefixGameTestTemplate(false)
public class RailwardCrossingSignalGameTests {

	@GameTest(template = "empty", timeoutTicks = 35)
	public static void crossingRequiresCabinetAndFollowsItsState(GameTestHelper helper) {
		BlockPos cabinet = helper.absolutePos(new BlockPos(1, 1, 1));
		BlockPos power = cabinet.east();
		BlockPos support = helper.absolutePos(new BlockPos(3, 1, 3));
		BlockPos pos = support.above();
		helper.getLevel().setBlockAndUpdate(cabinet, AllBlocks.RELAY_CASE.getDefaultState());
		helper.getLevel().setBlockAndUpdate(support, Blocks.STONE.defaultBlockState());
		FakePlayer player = FakePlayerFactory.getMinecraft(helper.getLevel());
		ItemStack previous = player.getMainHandItem();
		float previousYaw = player.getYRot();
		boolean previousSneaking = player.isShiftKeyDown();
		ItemStack stack = new ItemStack(AllBlocks.RAIL_CROSSING_SIGNAL.get(), 2);
		stack.getOrCreateTag().putString("CustomData", "keep");
		try {
			player.setItemInHand(InteractionHand.MAIN_HAND, stack);
			player.setYRot(Direction.WEST.toYRot());
			player.setShiftKeyDown(false);
			UseOnContext placement = useOn(player, support, Direction.UP);
			UseOnContext selection = useOn(player, cabinet, Direction.NORTH);
			require(helper, !stack.useOn(placement).consumesAction(), "Unbound crossing signal placed");
			require(helper, stack.useOn(selection).consumesAction(), "Relay Cabinet selection failed");
			player.setShiftKeyDown(true);
			require(helper, stack.useOn(selection).consumesAction(), "Shift-right-click on the cabinet was ignored");
			require(helper, RailCrossingSignalItem.selectedCabinet(stack, helper.getLevel()) == null,
				"Shift-right-click on the cabinet retained the selection");
			player.setShiftKeyDown(false);
			require(helper, stack.useOn(selection).consumesAction(), "Relay Cabinet reselection failed");
			player.setShiftKeyDown(true);
			require(helper, stack.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND).getResult()
				.consumesAction(), "Shift-right-click in air was ignored");
			require(helper, RailCrossingSignalItem.selectedCabinet(stack, helper.getLevel()) == null,
				"Shift-right-click in air retained the selection");
			player.setShiftKeyDown(false);
			require(helper, stack.useOn(selection).consumesAction(),
				"Relay Cabinet selection failed");
			helper.getLevel().setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
			require(helper, !stack.useOn(placement).consumesAction(), "Crossing signal placed into an occupied space");
			require(helper, cabinet.equals(RailCrossingSignalItem.selectedCabinet(stack, helper.getLevel())),
				"Failed placement cleared the cabinet selection");
			helper.getLevel().setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
			require(helper, stack.useOn(placement).consumesAction(), "Bound crossing signal did not place");
			require(helper, !stack.getTag().contains("CabinetPos") && !stack.getTag().contains("CabinetDimension"),
				"Successful placement retained cabinet data");
			require(helper, "keep".equals(stack.getTag().getString("CustomData")), "Clearing removed unrelated item data");
		} finally {
			player.setItemInHand(InteractionHand.MAIN_HAND, previous);
			player.setYRot(previousYaw);
			player.setShiftKeyDown(previousSneaking);
		}

		RailCrossingSignalBlockEntity signal = (RailCrossingSignalBlockEntity) helper.getLevel().getBlockEntity(pos);
		require(helper, signal.getBlockState().getValue(RailCrossingSignalBlock.FACING) == Direction.EAST,
			"Placed signal did not face the player");
		require(helper, signal.getLamp() == Lamp.OFF, "A free cabinet lit the signal");
		signal.configure(CrossingSign.MULTIPLE_TRACKS, CrossingSound.SIREN);
		CompoundTag saved = signal.saveWithoutMetadata();
		signal.configure(CrossingSign.NONE, CrossingSound.BELL);
		signal.load(saved);
		require(helper, signal.getSign() == CrossingSign.MULTIPLE_TRACKS && signal.getSound() == CrossingSound.SIREN,
			"Signal settings did not survive NBT reload");

		helper.runAtTickTime(2, () -> helper.getLevel().setBlockAndUpdate(power, Blocks.REDSTONE_BLOCK.defaultBlockState()));
		helper.runAtTickTime(4, () -> require(helper, signal.getLamp() == Lamp.LEFT, "Left lamp did not light first"));
		helper.runAtTickTime(14, () -> {
			require(helper, signal.getLamp() == Lamp.RIGHT, "Right lamp did not light ten ticks later");
			RailCrossingSignalBlockEntity observer = AllBlockEntities.RAIL_CROSSING_SIGNAL.get()
				.create(pos, signal.getBlockState());
			observer.setLevel(helper.getLevel());
			observer.handleUpdateTag(signal.getUpdateTag());
			require(helper, observer.getLamp() == Lamp.RIGHT && observer.getSign() == CrossingSign.MULTIPLE_TRACKS
				&& observer.getSound() == CrossingSound.SIREN, "Initial sync lost flash phase or settings");
		});
		helper.runAtTickTime(24, () -> {
			require(helper, signal.getLamp() == Lamp.LEFT, "Flash cycle did not repeat");
			helper.getLevel().setBlockAndUpdate(cabinet, Blocks.AIR.defaultBlockState());
		});
		helper.runAtTickTime(26, () -> {
			require(helper, signal.getLamp() == Lamp.OFF, "Removing the cabinet left the signal lit");
			helper.succeed();
		});
	}

	@GameTest(template = "empty", timeoutTicks = 10)
	public static void crossingSettingsUseTheExistingPost(GameTestHelper helper) {
		BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
		RailCrossingSignalBlock signal = AllBlocks.RAIL_CROSSING_SIGNAL.get();
		Vec3 center = Vec3.atCenterOf(pos).add(0, -.5 / 16, 0);
		for (Direction facing : Direction.Plane.HORIZONTAL) {
			BlockState state = signal.defaultBlockState().setValue(RailCrossingSignalBlock.FACING, facing);
			Vec3 from = center.add(Vec3.atLowerCornerOf(facing.getNormal()).scale(2));
			BlockHitResult hit = signal.getShape(state, helper.getLevel(), pos, CollisionContext.empty())
				.clip(from, center, pos);
			require(helper, hit != null && RailCrossingSignalBlock.isConfigurationHit(state, hit),
				"Settings could not be clicked from " + facing);
			require(helper, Math.abs(hit.getLocation().distanceTo(center) - 1 / 16d) < .0001,
				"Settings hit protruded from the post in " + facing);
			Vec3 behind = center.subtract(Vec3.atLowerCornerOf(facing.getNormal()).scale(2));
			BlockHitResult backHit = signal.getShape(state, helper.getLevel(), pos, CollisionContext.empty())
				.clip(behind, center, pos);
			require(helper, backHit != null && !RailCrossingSignalBlock.isConfigurationHit(state, backHit),
				"Settings could be clicked from behind " + facing);
		}
		helper.succeed();
	}

	private static UseOnContext useOn(FakePlayer player, BlockPos pos, Direction face) {
		Vec3 hit = Vec3.atCenterOf(pos).add(Vec3.atLowerCornerOf(face.getNormal()).scale(.5));
		return new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(hit, face, pos, false));
	}

	private static void require(GameTestHelper helper, boolean condition, String message) {
		if (!condition)
			helper.fail(message);
	}
}
