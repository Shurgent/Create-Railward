package ua.pp.shurgent.railward.client.ponder;

//? if create6 {
/*import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.instruction.PonderInstruction;
*///?} else {
import com.simibubi.create.foundation.ponder.PonderScene;
import com.simibubi.create.foundation.ponder.instruction.PonderInstruction;
//?}
import com.simibubi.create.foundation.ponder.PonderPalette;
import com.simibubi.create.foundation.ponder.SceneBuilder;
import com.simibubi.create.foundation.ponder.SceneBuildingUtil;
//? if !create6 {
import com.simibubi.create.foundation.ponder.element.InputWindowElement;
//?}
import com.simibubi.create.foundation.utility.Pointing;
import com.simibubi.create.content.contraptions.actors.contraptionControls.ContraptionControlsBlockEntity;
import com.simibubi.create.content.contraptions.actors.trainControls.ControlsBlock;
import com.simibubi.create.content.contraptions.actors.seat.SeatEntity;
import com.simibubi.create.content.trains.bogey.AbstractBogeyBlock;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.phys.Vec3;
import ua.pp.shurgent.railward.AllBlocks;
import ua.pp.shurgent.railward.block.train.parts.LocomotiveBufferLightsBlock;
import ua.pp.shurgent.railward.block.train.parts.LocomotiveBufferLightsVariant;
import ua.pp.shurgent.railward.block.train.parts.LocomotiveSpotlightBlock;
import ua.pp.shurgent.railward.block.train.parts.LocomotiveSpotlightVariant;
import ua.pp.shurgent.railward.block.train.parts.TrainWallPanelBlock;
import ua.pp.shurgent.railward.block.train.parts.TrainWallPanelBlockEntity;
import ua.pp.shurgent.railward.block.train.parts.panel.LocomotiveBufferLightsAttachment;
import ua.pp.shurgent.railward.block.train.parts.panel.LocomotiveSpotlightAttachment;
import ua.pp.shurgent.railward.client.renderer.LocomotiveBufferLightsRenderHelper.LampState;

/** A one-bogey inspection cart and its seated driver demonstrate directional lighting. */
public final class TrainLightingScenes {

	public static final BlockPos FIRST = new BlockPos(3, 3, 1);
	public static final BlockPos SECOND = new BlockPos(3, 3, 5);
	public static final BlockPos BUFFER_FIRST = FIRST.west();
	public static final BlockPos BUFFER_SECOND = SECOND.west();
	public static final BlockPos CONTROL = new BlockPos(4, 4, 4);
	public static final BlockPos BOGEY = new BlockPos(3, 2, 3);
	public static final BlockPos SEAT = new BlockPos(3, 4, 3);
	public static final BlockPos FRONT_CONTROLS = SEAT.north();
	public static final BlockPos REAR_CONTROLS = SEAT.south();
	private TrainLightingScenes() {}

	public static void buffers(SceneBuilder scene, SceneBuildingUtil util) {
		setup(scene, util, "buffer_lights", "Locomotive buffer lights");
		bufferPair(scene, util, BUFFER_FIRST);
		scene.world.showSection(util.select.fromTo(BUFFER_FIRST, BUFFER_FIRST.east(2)), Direction.DOWN);
		text(scene, PonderPalette.WHITE,
			"Mount Buffer Lights on a locomotive's end face. The block behind them provides support.");
		controls(scene, BUFFER_FIRST, com.simibubi.create.AllItems.WRENCH.asStack());
		caption(scene, PonderPalette.BLUE,
			"Right-click with a wrench to cycle through 13 housing styles. Every style uses the same automatic lighting rules.");
		for (int i = 0; i < 12; i++) {
			scene.world.modifyBlocks(util.select.position(BUFFER_FIRST), state -> state.cycle(LocomotiveBufferLightsBlock.VARIANT), false);
			scene.idle(14);
		}
		scene.idle(22);
		panel(scene, util, BUFFER_FIRST, false);
		text(scene, PonderPalette.GREEN,
			"Right-click a Railward wall panel with Buffer Lights to mount them directly on it. Wrench-click the lights themselves to change their style.");
		bufferPair(scene, util, BUFFER_FIRST);
		bufferPair(scene, util, BUFFER_SECOND);
		scene.world.showSection(util.select.fromTo(BUFFER_SECOND, BUFFER_SECOND.east(2)), Direction.DOWN);
		selectController(scene, Direction.NORTH);
		setPair(scene, BUFFER_FIRST, LampState.WHITE);
		setPair(scene, BUFFER_SECOND, LampState.RED);
		orbitText(scene, PonderPalette.GREEN,
			"In the direction of the selected controller, Buffer Lights shine white. At the rear they shine red.", 180);
		selectController(scene, Direction.SOUTH);
		setPair(scene, BUFFER_FIRST, LampState.RED);
		setPair(scene, BUFFER_SECOND, LampState.WHITE);
		orbitText(scene, PonderPalette.BLUE,
			"Select the opposite controller and the colours swap: white ahead, red at the rear. This also works with an active train schedule.", -180);
		setPair(scene, BUFFER_FIRST, LampState.OFF);
		setPair(scene, BUFFER_SECOND, LampState.OFF);
		text(scene, PonderPalette.WHITE,
			"Buffer Lights are off outside an assembled train or when there is no active manual or scheduled control. Simply stopping does not switch them off.");
		scene.markAsFinished();
	}

	public static void spotlight(SceneBuilder scene, SceneBuildingUtil util) {
		setup(scene, util, "locomotive_spotlight", "Using a locomotive spotlight");
		spot(scene, util, FIRST);
		scene.world.showSection(util.select.position(FIRST), Direction.DOWN);
		controls(scene, FIRST, com.simibubi.create.AllItems.WRENCH.asStack());
		caption(scene, PonderPalette.BLUE,
			"Mount a Spotlight on the locomotive's end face. Right-click it with a wrench to switch between the small and large housings.");
		for (int i = 0; i < 4; i++) {
			scene.idle(30);
			scene.world.modifyBlocks(util.select.position(FIRST), state -> state.cycle(LocomotiveSpotlightBlock.VARIANT), false);
		}
		scene.idle(70);
		panel(scene, util, FIRST, true);
		text(scene, PonderPalette.GREEN,
			"You can also right-click a Railward wall panel with a Spotlight to mount it directly. Wrench-click the mounted spotlight to change its size.");
		spot(scene, util, FIRST);
		spot(scene, util, SECOND);
		scene.world.showSection(util.select.position(SECOND), Direction.DOWN);
		selectController(scene, Direction.NORTH);
		set(scene, FIRST, LampState.WHITE);
		beam(scene, FIRST);
		orbitText(scene, PonderPalette.GREEN,
			"The spotlight in the direction of the selected controller lights the track. The rear spotlight stays off.", 180);
		selectController(scene, Direction.SOUTH);
		set(scene, FIRST, LampState.OFF);
		set(scene, SECOND, LampState.WHITE);
		beam(scene, SECOND);
		orbitText(scene, PonderPalette.BLUE,
			"Select the opposite controller to switch the active spotlight. Direction is selected by manual train controls or an active schedule.", -180);
		scene.rotateCameraY(180);
		scene.idle(50);
		scene.world.setBlock(CONTROL, com.simibubi.create.AllBlocks.CONTRAPTION_CONTROLS.getDefaultState()
			.setValue(ControlsBlock.FACING, Direction.SOUTH), false);
		scene.world.showSection(util.select.position(CONTROL), Direction.DOWN);
		scene.world.modifyBlockEntity(CONTROL, ContraptionControlsBlockEntity.class,
			be -> be.filtering.setFilter(AllBlocks.LOCOMOTIVE_SPOTLIGHT.asStack()));
		scene.world.modifyBlockEntity(CONTROL, ContraptionControlsBlockEntity.class, be -> be.disabled = true);
		set(scene, SECOND, LampState.OFF);
		controls(scene, CONTROL, ItemStack.EMPTY);
		text(scene, PonderPalette.RED,
			"To switch spotlights off independently, set a Contraption Controls filter to the Spotlight item. Toggling that control disables spotlights without changing Buffer Lights.");
		scene.world.modifyBlockEntity(CONTROL, ContraptionControlsBlockEntity.class, be -> be.disabled = false);
		set(scene, SECOND, LampState.WHITE);
		beam(scene, SECOND);
		text(scene, PonderPalette.GREEN,
			"Toggle the control again to restore the spotlight for the current direction. Without active train control, both spotlights remain off.");
		set(scene, SECOND, LampState.OFF);
		scene.idle(20);
		scene.markAsFinished();
	}

	private static void setup(SceneBuilder scene, SceneBuildingUtil util, String id, String title) {
		setupCart(scene, util, id, title);
		// Both controls keep their lever model; an outline marks the selected one.
		// The operating faces point toward the central seat.
		scene.world.setBlock(FRONT_CONTROLS, com.simibubi.create.AllBlocks.TRAIN_CONTROLS.getDefaultState()
			.setValue(ControlsBlock.FACING, Direction.SOUTH).setValue(ControlsBlock.VIRTUAL, true), false);
		scene.world.setBlock(REAR_CONTROLS, com.simibubi.create.AllBlocks.TRAIN_CONTROLS.getDefaultState()
			.setValue(ControlsBlock.FACING, Direction.NORTH).setValue(ControlsBlock.VIRTUAL, true), false);
		scene.world.showSection(util.select.position(FRONT_CONTROLS), Direction.DOWN);
		scene.world.showSection(util.select.position(REAR_CONTROLS), Direction.DOWN);
	}

	static void setupCart(SceneBuilder scene, SceneBuildingUtil util, String id, String title) {
		scene.title(id, title);
		scene.configureBasePlate(0, 0, 7);
		scene.scaleSceneView(1.15f);
		scene.setSceneOffsetY(-2);
		scene.showBasePlate();
		scene.world.setBlocks(util.select.fromTo(3, 1, 0, 3, 1, 6), com.simibubi.create.AllBlocks.TRACK.getDefaultState(), false);
		scene.world.showSection(util.select.fromTo(3, 1, 0, 3, 1, 6), Direction.DOWN);
		scene.world.setBlock(BOGEY, com.simibubi.create.AllBlocks.SMALL_BOGEY.getDefaultState()
			.setValue(AbstractBogeyBlock.AXIS, Direction.Axis.Z), false);
		scene.world.modifyBlockEntity(BOGEY, SmartBlockEntity.class, SmartBlockEntity::markVirtual);
		scene.world.showSection(util.select.position(BOGEY), Direction.DOWN);
		scene.world.setBlocks(util.select.fromTo(2, 3, 2, 4, 3, 4), com.simibubi.create.AllBlocks.RAILWAY_CASING.getDefaultState(), false);
		scene.world.showSection(util.select.fromTo(2, 3, 2, 4, 3, 4), Direction.DOWN);
		scene.world.setBlock(SEAT, com.simibubi.create.AllBlocks.SEATS.get(DyeColor.RED).getDefaultState(), false);
		scene.world.showSection(util.select.position(SEAT), Direction.DOWN);
		scene.world.createEntity(world -> {
			SeatEntity vehicle = new SeatEntity(world, SEAT);
			vehicle.setPos(SEAT.getX() + .5, SEAT.getY(), SEAT.getZ() + .5);
			world.addFreshEntity(vehicle);
			Bee bee = EntityType.BEE.create(world);
			if (bee == null) throw new IllegalStateException("Could not create the Ponder driver");
			bee.setNoAi(true);
			bee.setNoGravity(true);
			bee.setInvulnerable(true);
			bee.setPersistenceRequired();
			bee.noPhysics = true;
			bee.setOnGround(true);
			bee.startRiding(vehicle, true);
			bee.setPos(vehicle.getX(), vehicle.getY() + vehicle.getPassengersRidingOffset() + bee.getMyRidingOffset(), vehicle.getZ());
			faceDriver(bee, Direction.NORTH);
			return bee;
		});
	}

	private static void selectController(SceneBuilder scene, Direction direction) {
		BlockPos selected = direction == Direction.NORTH ? FRONT_CONTROLS : REAR_CONTROLS;
		scene.overlay.chaseBoundingBoxOutline(PonderPalette.GREEN, selected,
			new net.minecraft.world.phys.AABB(selected), 300);
		controls(scene, selected, ItemStack.EMPTY);
		turnDriver(scene, direction);
	}

	static void turnDriver(SceneBuilder scene, Direction direction) {
		scene.addInstruction(new TurnDriverInstruction(direction));
	}

	/** Non-blocking, resettable animation also works when Ponder replays or seeks. */
	private static final class TurnDriverInstruction extends PonderInstruction {
		private final float target;
		private int elapsed;
		private float start;
		private TurnDriverInstruction(Direction direction) { target = direction.toYRot(); }
		@Override public void reset(PonderScene scene) { elapsed = 0; }
		@Override public boolean isComplete() { return elapsed >= 30; }
		@Override public void tick(PonderScene scene) {
			float progress = ++elapsed / 30f;
			float eased = progress * progress * (3 - 2 * progress);
			scene.forEachWorldEntity(Bee.class, bee -> {
				if (elapsed == 1) start = bee.getYRot();
				float yaw = start + net.minecraft.util.Mth.wrapDegrees(target - start) * eased;
				bee.yRotO = bee.getYRot();
				bee.yBodyRotO = bee.yBodyRot;
				bee.yHeadRotO = bee.yHeadRot;
				bee.setYRot(yaw);
				bee.yBodyRot = yaw;
				bee.yHeadRot = yaw;
			});
		}
	}

	private static void faceDriver(Bee bee, Direction direction) {
		float yaw = direction.toYRot();
		bee.setYRot(yaw);
		bee.yRotO = yaw;
		bee.yBodyRot = yaw;
		bee.yBodyRotO = yaw;
		bee.yHeadRot = yaw;
		bee.yHeadRotO = yaw;
	}

	static void bufferPair(SceneBuilder scene, SceneBuildingUtil util, BlockPos left) {
		buffer(scene, util, left);
		buffer(scene, util, left.east(2));
	}

	static void setPair(SceneBuilder scene, BlockPos left, LampState state) {
		set(scene, left, state);
		set(scene, left.east(2), state);
	}

	private static void buffer(SceneBuilder scene, SceneBuildingUtil util, BlockPos p) {
		scene.world.setBlock(p, AllBlocks.LOCOMOTIVE_BUFFER_LIGHTS.getDefaultState()
			.setValue(LocomotiveBufferLightsBlock.FACING, facing(p))
			.setValue(LocomotiveBufferLightsBlock.VARIANT,
				(p.getX() == 2) == (facing(p) == Direction.NORTH)
					? LocomotiveBufferLightsVariant.TYPE_2 : LocomotiveBufferLightsVariant.TYPE_1), false);
		set(scene, p, LampState.OFF);
	}

	private static void spot(SceneBuilder scene, SceneBuildingUtil util, BlockPos p) {
		scene.world.setBlock(p, AllBlocks.LOCOMOTIVE_SPOTLIGHT.getDefaultState()
			.setValue(LocomotiveSpotlightBlock.FACING, facing(p))
			.setValue(LocomotiveSpotlightBlock.VARIANT, LocomotiveSpotlightVariant.LARGE), false);
		set(scene, p, LampState.OFF);
	}

	private static void panel(SceneBuilder scene, SceneBuildingUtil util, BlockPos p, boolean spotlight) {
		scene.world.setBlock(p, AllBlocks.TRAIN_WALL_PANEL_GREEN.getDefaultState()
			.setValue(TrainWallPanelBlock.FACING, Direction.SOUTH), false);
		scene.world.showSection(util.select.position(p), Direction.DOWN);
		controls(scene, p, spotlight ? AllBlocks.LOCOMOTIVE_SPOTLIGHT.asStack() : AllBlocks.LOCOMOTIVE_BUFFER_LIGHTS.asStack());
		scene.world.modifyBlockEntity(p, TrainWallPanelBlockEntity.class, be -> be.setAttachment(spotlight
			? new LocomotiveSpotlightAttachment(LocomotiveSpotlightVariant.LARGE)
			: new LocomotiveBufferLightsAttachment(LocomotiveBufferLightsVariant.TYPE_1)));
	}

	private static void set(SceneBuilder scene, BlockPos p, LampState state) {
		scene.world.modifyBlockEntity(p, SmartBlockEntity.class, be -> TrainLightingPonderState.set(be, state));
	}

	private static void beam(SceneBuilder scene, BlockPos p) {
		int z = facing(p).getStepZ();
		Vec3 origin = Vec3.atLowerCornerOf(p).add(.5, .5, .5 - z * (3 / 16d - .01));
		for (double x : new double[] {-.65, .65}) for (double y : new double[] {-.6, .6}) {
			// Create keys line outlines by their start vector; each edge needs its own start.
			Vec3 start = origin.add(x * .02, y * .02, 0);
			scene.overlay.showLine(PonderPalette.WHITE, start, start.add(x, y, z * 1.5), 180);
		}
	}

	private static Direction facing(BlockPos p) {
		return p.getZ() == SECOND.getZ() ? Direction.SOUTH : Direction.NORTH;
	}

	static void orbitText(SceneBuilder scene, PonderPalette colour, String text, float degrees) {
		scene.overlay.showText(300).text(text).colored(colour).independent(40).attachKeyFrame();
		scene.idle(130);
		scene.rotateCameraY(degrees);
		scene.idle(170);
	}

	static void controls(SceneBuilder scene, BlockPos p, ItemStack item) {
		Vec3 target = Vec3.atCenterOf(p).add(0, 1.25, 0);
		//? if create6 {
		/*scene.overlay().showControls(target, Pointing.DOWN, 60).rightClick().withItem(item);
		*///?} else {
		scene.overlay.showControls(new InputWindowElement(target, Pointing.DOWN).rightClick().withItem(item), 60);
		//?}
	}

	static void caption(SceneBuilder scene, PonderPalette colour, String text) {
		scene.overlay.showText(180).text(text).colored(colour).independent(40).attachKeyFrame();
	}

	static void text(SceneBuilder scene, PonderPalette colour, String text) {
		caption(scene, colour, text);
		scene.idle(190);
	}
}
