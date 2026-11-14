package ua.pp.shurgent.railward.gametest;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.simibubi.create.Create;
import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.CarriageBogey;
import com.simibubi.create.content.trains.entity.CarriageContraption;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.entity.TravellingPoint;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import ua.pp.shurgent.railward.AllBlocks;
import ua.pp.shurgent.railward.Railward;
import ua.pp.shurgent.railward.RailwardConfig;
import ua.pp.shurgent.railward.block.train.parts.TrainCouplerHandler;

@GameTestHolder(Railward.MOD_ID)
@PrefixGameTestTemplate(false)
public final class RailwardCouplerDimensionGameTests {

	@GameTest(template = "empty", timeoutTicks = 20)
	public static void autoCouplerStateIsIsolatedBetweenDimensions(GameTestHelper helper) throws Exception {
		List<ServerLevel> levels = List.of(helper.getLevel().getServer().getLevel(Level.OVERWORLD),
			helper.getLevel().getServer().getLevel(Level.NETHER), helper.getLevel().getServer().getLevel(Level.END));
		List<Train> fixtures = new ArrayList<>();
		List<CarriageContraptionEntity> entities = new ArrayList<>();
		boolean enabled = RailwardConfig.COMMON.trainCouplerAutoCoupling.get();
		int interval = RailwardConfig.COMMON.trainCouplerAutoScanIntervalTicks.get();
		try {
			RailwardConfig.COMMON.trainCouplerAutoCoupling.set(true);
			RailwardConfig.COMMON.trainCouplerAutoScanIntervalTicks.set(1);
			TrainCouplerHandler.clearAutoCouplers();
			for (ServerLevel level : levels) {
				for (int i = 0; i < 2; i++) {
					Train train = fixture(level, new Vec3(0.5 + i, 100, 0.5));
					fixtures.add(train);
					CarriageContraptionEntity entity = train.carriages.get(0).anyAvailableEntity();
					entities.add(entity);
					Create.RAILWAYS.trains.put(train.id, train);
				}
				TrainCouplerHandler.tickAutoCouplers(level);
			}

			for (int i = 0; i < levels.size(); i++) {
				ServerLevel level = levels.get(i);
				Map<UUID, Vec3> history = history(level);
				require(helper, history.size() == 2 && history.containsKey(fixtures.get(i * 2).id)
					&& history.containsKey(fixtures.get(i * 2 + 1).id),
					"history is missing or contains trains from another dimension: " + level.dimension().location());
			}

			ServerLevel nether = levels.get(1);
			Object first = couplerRef(entities.get(2));
			Object second = couplerRef(entities.get(3));
			invoke("suppressAutoCoupling", new Class<?>[] { ServerLevel.class, first.getClass(), second.getClass() },
				nether, first, second);
			TrainCouplerHandler.tickAutoCouplers(levels.get(0));
			TrainCouplerHandler.tickAutoCouplers(levels.get(2));
			require(helper, suppressed(nether, first, second),
				"another dimension cleared the Nether recoupling lock");
			require(helper, !suppressed(levels.get(0), first, second),
				"the Nether lock leaked into the Overworld");

			entities.get(3).setPos(3.5, 100, 0.5);
			TrainCouplerHandler.tickAutoCouplers(nether);
			require(helper, history(nether).get(fixtures.get(3).id).x > 3,
				"the Nether movement history did not update");

			fixtures.get(2).carriages.get(0).getDimensional(Level.OVERWORLD);
			TrainCouplerHandler.tickAutoCouplers(nether);
			require(helper, !history(nether).containsKey(fixtures.get(2).id),
				"a carriage spanning a portal remained eligible for automatic coupling");
		} finally {
			for (Train train : fixtures)
				Create.RAILWAYS.trains.remove(train.id);
			for (CarriageContraptionEntity entity : entities)
				entity.discard();
			RailwardConfig.COMMON.trainCouplerAutoCoupling.set(enabled);
			RailwardConfig.COMMON.trainCouplerAutoScanIntervalTicks.set(interval);
			TrainCouplerHandler.clearAutoCouplers();
		}
		helper.succeed();
	}

	private static Train fixture(ServerLevel level, Vec3 position) {
		CarriageBogey bogey = new CarriageBogey(com.simibubi.create.AllBlocks.SMALL_BOGEY.get(), false, null,
			new TravellingPoint(), new TravellingPoint());
		Carriage carriage = new Carriage(bogey, null, 0);
		Train train = new Train(UUID.randomUUID(), UUID.randomUUID(), null,
			new ArrayList<>(List.of(carriage)), new ArrayList<>(), false);
		CarriageContraption contraption = new CarriageContraption(Direction.NORTH);
		contraption.bounds = new AABB(BlockPos.ZERO);
		contraption.getBlocks().put(BlockPos.ZERO,
			new StructureBlockInfo(BlockPos.ZERO, AllBlocks.TRAIN_COUPLER.getDefaultState(), null));
		CarriageContraptionEntity entity = CarriageContraptionEntity.create(level, contraption);
		entity.setPos(position.x, position.y, position.z);
		entity.setCarriage(carriage);
		carriage.getDimensional(level).entity = new WeakReference<>(entity);
		return train;
	}

	@SuppressWarnings("unchecked")
	private static Map<UUID, Vec3> history(ServerLevel level) throws Exception {
		Object state = invoke("autoState", new Class<?>[] { ServerLevel.class }, level);
		Field field = state.getClass().getDeclaredField("lastTrainCenters");
		field.setAccessible(true);
		return (Map<UUID, Vec3>) field.get(state);
	}

	private static Object couplerRef(CarriageContraptionEntity entity) throws Exception {
		Class<?> type = Class.forName(TrainCouplerHandler.class.getName() + "$CouplerRef");
		Method method = type.getDeclaredMethod("from", CarriageContraptionEntity.class, BlockPos.class,
			net.minecraft.world.level.block.state.BlockState.class);
		method.setAccessible(true);
		return method.invoke(null, entity, BlockPos.ZERO, AllBlocks.TRAIN_COUPLER.getDefaultState());
	}

	private static boolean suppressed(ServerLevel level, Object first, Object second) throws Exception {
		return (boolean) invoke("isAutoCouplingSuppressed",
			new Class<?>[] { ServerLevel.class, first.getClass(), second.getClass() }, level, first, second);
	}

	private static Object invoke(String name, Class<?>[] types, Object... arguments) throws Exception {
		Method method = TrainCouplerHandler.class.getDeclaredMethod(name, types);
		method.setAccessible(true);
		return method.invoke(null, arguments);
	}

	private static void require(GameTestHelper helper, boolean condition, String message) {
		if (!condition)
			helper.fail(message);
	}
}
