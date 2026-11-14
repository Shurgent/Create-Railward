package ua.pp.shurgent.railward.gametest;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.mojang.logging.LogUtils;
import com.simibubi.create.Create;
import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.CarriageBogey;
import com.simibubi.create.content.trains.entity.CarriageContraption;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.entity.TravellingPoint;
import com.simibubi.create.content.trains.graph.TrackEdge;
import com.simibubi.create.content.trains.graph.TrackGraph;
import com.simibubi.create.content.trains.graph.TrackNode;
import com.simibubi.create.content.trains.graph.TrackNodeLocation;
import com.simibubi.create.content.trains.track.TrackMaterial;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;
import ua.pp.shurgent.railward.AllBlocks;
import ua.pp.shurgent.railward.Railward;
import ua.pp.shurgent.railward.RailwardConfig;
import ua.pp.shurgent.railward.block.train.parts.TrainCouplerHandler;
import ua.pp.shurgent.railward.block.train.parts.TrainCouplerInteractionBehaviour;
import ua.pp.shurgent.railward.compat.RailwardMovementBehaviours;
import ua.pp.shurgent.railward.compat.TrainCouplerCompatibility;

@GameTestHolder(Railward.MOD_ID)
@PrefixGameTestTemplate(false)
public final class RailwardSnrCouplerGameTests {
	private static final List<String> STYLES = List.of("link", "linkless", "knuckle", "knuckle_split", "screwlink");

	@GameTest(template = "empty", batch = "snr_compat", timeoutTicks = 20)
	public static void couplerInteractionSurvivesContraptionSerialization(GameTestHelper helper) {
		checkInteraction(helper, AllBlocks.TRAIN_COUPLER.getDefaultState());
		require(helper, !TrainCouplerCompatibility.isCoupler(Blocks.IRON_BLOCK.defaultBlockState()),
			"an ordinary block was recognized as a coupler");
		Block deco = block("link_and_pin");
		if (deco != null) {
			for (String path : List.of("link_and_pin", "headstock", "copycat_headstock")) {
				Block snr = block(path);
				require(helper, snr != null, "missing SnR block: " + path);
				for (String style : STYLES)
					for (Direction facing : Direction.Plane.HORIZONTAL) {
						BlockState state = styled(snr.defaultBlockState(), style)
							.setValue(HorizontalDirectionalBlock.FACING, facing);
						checkInteraction(helper, state);
					}
				if (!path.equals("link_and_pin"))
					for (String style : List.of("plain", "buffer"))
						require(helper, !TrainCouplerCompatibility.isCoupler(styled(snr.defaultBlockState(), style)),
							"a plain/buffer headstock became a coupler");
			}
		}
		helper.succeed();
	}

	@GameTest(template = "empty", batch = "snr_compat", timeoutTicks = 40)
	public static void snrCouplersUseProductionManualAndAutomaticPaths(GameTestHelper helper) {
		if (block("link_and_pin") == null) {
			LogUtils.getLogger().info("[Railward SnR compatibility] No decorative SnR blocks installed; integration cases skipped");
			helper.succeed();
			return;
		}
		boolean enabled = RailwardConfig.COMMON.trainCouplerAutoCoupling.get();
		int interval = RailwardConfig.COMMON.trainCouplerAutoScanIntervalTicks.get();
		try {
			RailwardConfig.COMMON.trainCouplerAutoCoupling.set(true);
			RailwardConfig.COMMON.trainCouplerAutoScanIntervalTicks.set(1);
			for (String style : STYLES) {
				BlockState deco = styled(block("link_and_pin").defaultBlockState(), style);
				BlockState headstock = styled(block("headstock").defaultBlockState(), style);
				BlockState copycat = styled(block("copycat_headstock").defaultBlockState(), style);
				for (BlockState first : List.of(deco, headstock, copycat))
					for (BlockState second : List.of(deco, headstock, copycat))
						for (boolean automatic : List.of(false, true))
							scenario(helper, first, second, automatic, true);
			}
			BlockState knuckle = styled(block("link_and_pin").defaultBlockState(), "knuckle");
			for (boolean automatic : List.of(false, true)) {
				scenario(helper, knuckle, AllBlocks.TRAIN_COUPLER.getDefaultState(), automatic, false);
				scenario(helper, AllBlocks.TRAIN_COUPLER.getDefaultState(), knuckle, automatic, false);
				scenario(helper, knuckle, styled(block("link_and_pin").defaultBlockState(), "screwlink"), automatic, false);
				scenario(helper, knuckle, styled(block("headstock").defaultBlockState(), "screwlink"), automatic, false);
				scenario(helper, styled(block("headstock").defaultBlockState(), "knuckle"),
					styled(block("copycat_headstock").defaultBlockState(), "screwlink"), automatic, false);
			}
			LogUtils.getLogger().info("[Railward SnR compatibility] PASS: 90 matching merge/split cycles and 10 incompatible pairs");
		} finally {
			RailwardConfig.COMMON.trainCouplerAutoCoupling.set(enabled);
			RailwardConfig.COMMON.trainCouplerAutoScanIntervalTicks.set(interval);
			TrainCouplerHandler.clearAutoCouplers();
		}
		helper.succeed();
	}

	private static void scenario(GameTestHelper helper, BlockState first, BlockState second,
		boolean automatic, boolean shouldMatch) {
		ServerLevel level = helper.getLevel();
		TrackGraph graph = new TrackGraph();
		TrackNode start = new TrackNode(new TrackNodeLocation(0, 0, 0).in(level), TrackGraph.nextNodeId(), new Vec3(0, 1, 0));
		TrackNode end = new TrackNode(new TrackNodeLocation(0, 0, 64).in(level), TrackGraph.nextNodeId(), new Vec3(0, 1, 0));
		graph.addNode(start);
		graph.addNode(end);
		TrackEdge edge = new TrackEdge(start, end, null, TrackMaterial.ANDESITE);
		graph.putConnection(start, end, edge);
		graph.putConnection(end, start, new TrackEdge(end, start, null, TrackMaterial.ANDESITE));
		Create.RAILWAYS.trackNetworks.put(graph.id, graph);
		List<Fixture> fixtures = new ArrayList<>();
		try {
			Fixture a = fixture(level, graph, start, end, edge, 10, 2, first);
			fixtures.add(a);
			Fixture b = fixture(level, graph, start, end, edge, 15, -2, second);
			fixtures.add(b);
			if (shouldMatch) {
				String style = TrainCouplerCompatibility.typeOf(first).style();
				BlockState wrongHead = styled(first, style.equals("knuckle") ? "screwlink" : "knuckle");
				fixtures.add(fixture(level, graph, start, end, edge, 14.75, -2, wrongHead));
			}
			if (automatic) {
				TrainCouplerHandler.clearAutoCouplers();
				TrainCouplerHandler.tickAutoCouplers(level);
				a.carriage.leadingBogey().leading().position += 0.05;
				a.carriage.leadingBogey().trailing().position += 0.05;
				a.carriage.updateContraptionAnchors();
				a.carriage.getDimensional(level).alignEntity(a.entity);
				a.carriage.train.speed = 0.03;
				a.carriage.train.targetSpeed = 0.03;
				TrainCouplerHandler.tickAutoCouplers(level);
			} else {
				var result = TrainCouplerHandler.tryDecouple(level, a.entity, a.couplerPos);
				require(helper, result.success() == shouldMatch, "manual result: " + result.message() + " " + first + " / " + second);
			}
			require(helper, (a.carriage.train == b.carriage.train) == shouldMatch,
				"wrong coupling result, auto=" + automatic + " " + first + " / " + second);
			if (shouldMatch) {
				require(helper, a.carriage.train.carriages.size() == 2, "merge lost a carriage");
				var split = TrainCouplerHandler.tryDecouple(level, a.entity, a.couplerPos);
				require(helper, split.success(), "SnR uncoupling failed: " + split.message());
				require(helper, a.carriage.train != b.carriage.train, "uncoupling kept the same train");
			}
		} finally {
			for (Train train : new ArrayList<>(Create.RAILWAYS.trains.values()))
				if (train.graph == graph)
					Create.RAILWAYS.removeTrain(train.id);
			Create.RAILWAYS.trackNetworks.remove(graph.id);
			for (Fixture fixture : fixtures)
				fixture.entity.discard();
		}
	}

	private static Fixture fixture(ServerLevel level, TrackGraph graph, TrackNode start, TrackNode end,
		TrackEdge edge, double position, int couplerZ, BlockState state) {
		CarriageBogey bogey = new CarriageBogey(com.simibubi.create.AllBlocks.SMALL_BOGEY.get(), false, null,
			new TravellingPoint(start, end, edge, position + 1, false),
			new TravellingPoint(start, end, edge, position - 1, false));
		Carriage carriage = new Carriage(bogey, null, 0);
		Train train = new Train(UUID.randomUUID(), UUID.randomUUID(), graph,
			new ArrayList<>(List.of(carriage)), new ArrayList<>(), false);
		CarriageContraption contraption = new CarriageContraption(Direction.NORTH);
		contraption.anchor = BlockPos.ZERO;
		BlockPos localPos = new BlockPos(0, 0, couplerZ);
		boolean railward = TrainCouplerCompatibility.typeOf(state).isRailward();
		state = state.setValue(HorizontalDirectionalBlock.FACING,
			(couplerZ > 0) == railward ? Direction.NORTH : Direction.SOUTH);
		contraption.bounds = new AABB(localPos).minmax(new AABB(BlockPos.ZERO));
		contraption.getBlocks().put(localPos, new StructureBlockInfo(localPos, state, null));
		CarriageContraptionEntity entity = CarriageContraptionEntity.create(level, contraption);
		entity.setCarriage(carriage);
		carriage.getDimensional(level).entity = new WeakReference<>(entity);
		carriage.getDimensional(level).alignEntity(entity);
		Create.RAILWAYS.addTrain(train);
		return new Fixture(carriage, entity, localPos);
	}

	private static void checkInteraction(GameTestHelper helper, BlockState state) {
		require(helper, TrainCouplerCompatibility.isCoupler(state), "missing supported coupler: " + state);
		require(helper, RailwardMovementBehaviours.getInteraction(state) instanceof TrainCouplerInteractionBehaviour,
			"wrench behavior not registered: " + state);
		CarriageContraption original = new CarriageContraption(Direction.NORTH);
		original.anchor = BlockPos.ZERO;
		original.bounds = new AABB(BlockPos.ZERO);
		original.getBlocks().put(BlockPos.ZERO, new StructureBlockInfo(BlockPos.ZERO, state, null));
		CarriageContraption restored = new CarriageContraption(Direction.NORTH);
		restored.readNBT(helper.getLevel(), original.writeNBT(false), false);
		require(helper, restored.getInteractors().get(BlockPos.ZERO) instanceof TrainCouplerInteractionBehaviour,
			"wrench behavior lost on contraption reload: " + state);
	}

	private static Block block(String path) {
		ResourceLocation id = new ResourceLocation("railways", path);
		return ForgeRegistries.BLOCKS.containsKey(id) ? ForgeRegistries.BLOCKS.getValue(id) : null;
	}

	private static BlockState styled(BlockState state, String style) {
		return withProperty(state, state.getBlock().getStateDefinition().getProperty("style"), style);
	}

	private static <T extends Comparable<T>> BlockState withProperty(BlockState state, Property<T> property, String value) {
		return state.setValue(property, property.getValue(value).orElseThrow());
	}

	private static void require(GameTestHelper helper, boolean condition, String message) {
		if (!condition)
			helper.fail(message);
	}

	private record Fixture(Carriage carriage, CarriageContraptionEntity entity, BlockPos couplerPos) {}
}
