package ua.pp.shurgent.railward.client.ponder;

import com.simibubi.create.AllItems;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.foundation.ponder.PonderPalette;
import com.simibubi.create.foundation.ponder.SceneBuilder;
import com.simibubi.create.foundation.ponder.SceneBuildingUtil;
//? if create6 {
/*import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.ponder.api.element.ParrotPose.FacePointOfInterestPose;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.element.PonderElementBase;
import net.createmod.ponder.foundation.ui.PonderUI;
*///?} else {
import com.simibubi.create.foundation.ponder.PonderScene;
import com.simibubi.create.foundation.ponder.element.ParrotElement.FacePointOfInterestPose;
import com.simibubi.create.foundation.ponder.element.PonderElement;
import com.simibubi.create.foundation.ponder.ui.PonderUI;
//?}
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import ua.pp.shurgent.railward.AllBlocks;
import ua.pp.shurgent.railward.block.train.signal.WhistleSignBlock;
import ua.pp.shurgent.railward.block.train.signal.WhistleSignBlockEntity;

public final class WhistleSignScenes {
	private static final BlockPos WHISTLE = new BlockPos(1, 4, 3);

	private WhistleSignScenes() {}

	public static void crossing(SceneBuilder scene, SceneBuildingUtil util) {
		scene.title("whistle_sign", "Whistling at a track marker");
		SpeedSignScenes.setupStage(scene, util);
		//? if create6 {
		/*var special = new CreateSceneBuilder(scene).special();
		var effects = scene.effects();
		*///?} else {
		var special = scene.special;
		var effects = scene.effects;
		//?}
		var cue = new WhistleCue();
		scene.addInstruction(s -> s.addElement(cue));
		BlockPos signPos = SpeedSignScenes.SIGN;
		BlockPos trackPos = SpeedSignScenes.TARGET;
		ItemStack sign = AllBlocks.WHISTLE_SIGN.asStack();
		TrainLightingScenes.controls(scene, trackPos, sign);
		SpeedSignScenes.directionArrow(scene, 125);
		SpeedSignScenes.caption(scene, PonderPalette.WHITE, 125,
			"Face the intended direction of travel and right-click the rail with the Whistle Sign in hand.");
		scene.idle(125);

		ItemStack linkedSign = sign.copy();
		linkedSign.getOrCreateTag().put("SelectedPos", NbtUtils.writeBlockPos(trackPos));
		linkedSign.getOrCreateTag().putBoolean("SelectedDirection", false);
		TrainLightingScenes.controls(scene, signPos, linkedSign);
		scene.world.setBlock(signPos, AllBlocks.WHISTLE_SIGN.getDefaultState()
			.setValue(WhistleSignBlock.ROTATION, SpeedSignScenes.SIGN_ROTATION), false);
		scene.world.modifyBlockEntityNBT(util.select.position(signPos), WhistleSignBlockEntity.class, tag -> {
			tag.put("TargetTrack", NbtUtils.writeBlockPos(trackPos.subtract(signPos)));
			tag.putBoolean("TargetDirection", false);
		});
		scene.world.showSection(util.select.position(signPos), Direction.DOWN);
		SpeedSignScenes.caption(scene, PonderPalette.GREEN, 125,
			"Place the sign beside the selected rail. The track marker shows which direction triggers the whistle.");
		scene.idle(125);

		scene.world.setBlock(WHISTLE, com.simibubi.create.AllBlocks.STEAM_WHISTLE.getDefaultState(), false);
		var cart = SpeedSignScenes.cart(scene, util);
		BlockPos seat = SpeedSignScenes.BOGEY.above(2);
		var driver = special.createBirb(Vec3.atCenterOf(seat), FacePointOfInterestPose::new);
		special.conductorBirb(driver, true);
		special.movePointOfInterest(new Vec3(10, 4.5, 4.5));
		TrainLightingScenes.controls(scene, seat, AllItems.SCHEDULE.asStack());
		SpeedSignScenes.caption(scene, PonderPalette.BLUE, 120,
			"For an automatic whistle, the train needs a whistle or horn and a conductor running an active schedule.");
		scene.idle(120);

		SpeedSignScenes.caption(scene, PonderPalette.GREEN, 155,
			"The conductor sounds the train's whistle as the front of the train passes the marker in the selected direction.");
		Vec3 forward = new Vec3(2, 0, 0);
		special.moveParrot(driver, forward, 60);
		SpeedSignScenes.move(scene, cart, 2, 60);
		scene.addInstruction(cue::play);
		Vec3 outlet = Vec3.atCenterOf(WHISTLE).add(2, .65, 0);
		effects.emitParticles(outlet,
			(world, x, y, z) -> world.addParticle(ParticleTypes.NOTE, x, y, z, .5, 0, 0), .5f, 20);
		special.moveParrot(driver, forward, 60);
		SpeedSignScenes.move(scene, cart, 2, 60);
		scene.idle(35);

		SpeedSignScenes.caption(scene, PonderPalette.WHITE, 160,
			"For a whistle on the return trip, link another sign while facing the opposite direction.");
		special.movePointOfInterest(new Vec3(-2, 4.5, 4.5));
		special.moveParrot(driver, new Vec3(-4, 0, 0), 120);
		SpeedSignScenes.move(scene, cart, -4, 120);
		scene.idle(40);

		TrainLightingScenes.controls(scene, signPos, AllItems.WRENCH.asStack());
		SpeedSignScenes.caption(scene, PonderPalette.WHITE, 145,
			"A wrench turns the sign face. Its direction of action comes from the rail selection.");
		scene.idle(25);
		scene.world.modifyBlock(signPos, state -> state.cycle(WhistleSignBlock.ROTATION), false);
		scene.idle(120);
		scene.markAsFinished();
	}

	//? if create6 {
	/*private static final class WhistleCue extends PonderElementBase {
	*///?} else {
	private static final class WhistleCue extends PonderElement {
	//?}
		private boolean skipNextTick;
		private boolean skipping;

		@Override
		public void whileSkipping(PonderScene scene) {
			skipNextTick = true;
		}

		@Override
		public void tick(PonderScene scene) {
			skipping = skipNextTick;
			skipNextTick = false;
		}

		void play(PonderScene scene) {
			Minecraft minecraft = Minecraft.getInstance();
			if (skipping || !(minecraft.screen instanceof PonderUI ui) || ui.getActiveScene() != scene)
				return;
			minecraft.getSoundManager().play(SimpleSoundInstance.forUI(AllSoundEvents.WHISTLE_TRAIN.getMainEvent(), 1f, .65f));
		}
	}
}
