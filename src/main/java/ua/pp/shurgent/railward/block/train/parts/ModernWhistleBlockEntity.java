package ua.pp.shurgent.railward.block.train.parts;

import java.util.List;

import ua.pp.shurgent.railward.client.compat.RailwardTransformStack;
import ua.pp.shurgent.railward.client.compat.RailwardValueBoxTransform;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.decoration.steamWhistle.WhistleBlock.WhistleSize;
import com.simibubi.create.content.decoration.steamWhistle.WhistleBlockEntity;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import com.simibubi.create.foundation.utility.VecHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import ua.pp.shurgent.railward.client.gui.ModernWhistleConfigurationScreen;
import ua.pp.shurgent.railward.client.sound.ModernWhistleBlockEntityAudio;
import ua.pp.shurgent.railward.compat.RailwardComponents;

public class ModernWhistleBlockEntity extends WhistleBlockEntity {

	public static final String SECONDARY_SOUND_TYPE_KEY = "SecondarySoundType";
	private static final String PRIMARY_SOUND_TYPE_KEY = "PrimarySoundType";

	private boolean wasPowered;
	private ModernWhistleSoundType activeSoundType;
	private int loopStartDelay = -1;
	private WhistleSoundConfigurationBehaviour soundConfiguration;

	public ModernWhistleBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
		syncTrainWhistleProfile();
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
		soundConfiguration = new WhistleSoundConfigurationBehaviour(this);
		behaviours.add(soundConfiguration);
	}

	private void soundTypeChanged(boolean primary, ModernWhistleSoundType soundType) {
		if (level == null)
			return;

		if (primary) {
			BlockState state = getBlockState();
			if (state.getValue(ModernWhistleBlock.SOUND_TYPE) != soundType
				&& state.getBlock() instanceof ModernWhistleBlock block)
				level.setBlock(worldPosition, block.withPrimarySoundType(state, soundType), 3);
			syncTrainWhistleProfile();
		}
	}

	public ModernWhistleSoundType getPrimarySoundType() {
		return getBlockState().getValue(ModernWhistleBlock.SOUND_TYPE);
	}

	public ModernWhistleSoundType getSecondarySoundType() {
		return soundConfiguration == null ? ModernWhistleSoundType.WHISTLE : soundConfiguration.getSecondary();
	}

	public void applySoundConfiguration(ModernWhistleSoundType primary, ModernWhistleSoundType secondary) {
		if (level == null)
			return;

		soundTypeChanged(true, primary);
		if (soundConfiguration != null)
			soundConfiguration.secondaryValue = secondary.ordinal();
		setChanged();
		if (!level.isClientSide)
			notifyUpdate();
	}

	@Override
	public void tick() {
		super.tick();
		if (level == null || !level.isClientSide)
			return;

		DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> tickClientAudio());
	}

	@Override
	protected void tickAudio(WhistleSize octave, boolean active) {
	}

	@Override
	public FluidTankBlockEntity getTank() {
		return null;
	}

	public void syncTrainWhistleProfile() {
		int mappedPitch = getPrimarySoundType().getTrainPitch();
		if (pitch == mappedPitch)
			return;
		pitch = mappedPitch;
		setChanged();
		if (level != null && !level.isClientSide)
			notifyUpdate();
	}

	private void tickClientAudio() {
		BlockState state = getBlockState();
		boolean powered = state.getValue(ModernWhistleBlock.POWERED);
		ModernWhistleSoundType soundType = state.getValue(ModernWhistleBlock.SOUND_TYPE);

		if (activeSoundType == null)
			activeSoundType = soundType;

		if (powered && !wasPowered) {
			beginPlayback(soundType);
		} else if (!powered && wasPowered) {
			endPlayback(activeSoundType);
		} else if (powered && soundType != activeSoundType) {
			endPlayback(activeSoundType);
			beginPlayback(soundType);
		} else if (!powered && soundType != activeSoundType) {
			activeSoundType = soundType;
		}

		if (powered)
			tickLoop(soundType);

		wasPowered = powered;
		activeSoundType = soundType;
	}

	private void beginPlayback(ModernWhistleSoundType soundType) {
		stopLoop();
		loopStartDelay = soundType.getLoopStartDelayTicks();
		playOneShot(soundType.getStartEvent());
		activeSoundType = soundType;
	}

	private void endPlayback(ModernWhistleSoundType soundType) {
		loopStartDelay = -1;
		stopLoop();
		if (soundType != null)
			playOneShot(soundType.getEndEvent());
	}

	private void tickLoop(ModernWhistleSoundType soundType) {
		if (loopStartDelay > 0) {
			loopStartDelay--;
			return;
		}

		ModernWhistleBlockEntityAudio.keepLoopAlive(this, soundType);
	}

	private void stopLoop() {
		ModernWhistleBlockEntityAudio.stopLoop(this);
	}

	private void playOneShot(SoundEvent event) {
		Vec3 center = Vec3.atCenterOf(worldPosition);
		level.playLocalSound(center.x, center.y, center.z, event, SoundSource.BLOCKS, 1f, 1f, false);
	}

	@Override
	public void onChunkUnloaded() {
		super.onChunkUnloaded();
		stopClientAudio();
	}

	@Override
	public void invalidate() {
		super.invalidate();
		stopClientAudio();
	}

	private void stopClientAudio() {
		if (level == null || !level.isClientSide)
			return;
		DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> stopLoop());
	}

	private static class WhistleSoundConfigurationBehaviour extends ScrollValueBehaviour {

		private final ModernWhistleBlockEntity whistle;
		private int secondaryValue = ModernWhistleSoundType.WHISTLE.ordinal();

		private WhistleSoundConfigurationBehaviour(ModernWhistleBlockEntity whistle) {
			super(RailwardComponents.translatable("railward.modern_whistle.configuration"), whistle,
				new ModernWhistleValueBoxTransform(8));
			this.whistle = whistle;
			withFormatter(value -> "...");
		}

		@Override
		public boolean acceptsValueSettings() {
			return false;
		}

		@Override
		//? if create6
		/*public void onShortInteract(Player player, InteractionHand hand, Direction side, BlockHitResult hit) {
		*///? if !create6
		public void onShortInteract(Player player, InteractionHand hand, Direction side) {
			if (!player.level.isClientSide)
				return;
			DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
				() -> () -> ModernWhistleConfigurationScreen.open(whistle));
		}

		private ModernWhistleSoundType getSecondary() {
			return ModernWhistleSoundType.byOrdinal(secondaryValue);
		}

		@Override
		public void write(CompoundTag nbt, boolean clientPacket) {
			nbt.putInt(PRIMARY_SOUND_TYPE_KEY, whistle.getPrimarySoundType().ordinal());
			nbt.putInt(SECONDARY_SOUND_TYPE_KEY, secondaryValue);
		}

		@Override
		public void read(CompoundTag nbt, boolean clientPacket) {
			value = whistle.getPrimarySoundType().ordinal();
			int storedSecondary = nbt.contains(SECONDARY_SOUND_TYPE_KEY, Tag.TAG_INT)
				? nbt.getInt(SECONDARY_SOUND_TYPE_KEY)
				: ModernWhistleSoundType.WHISTLE.ordinal();
			secondaryValue = Mth.clamp(storedSecondary, 0, ModernWhistleSoundType.values().length - 1);
		}

		@Override
		public String getClipboardKey() {
			return "ModernWhistleSoundTypes";
		}
	}

	private static class ModernWhistleValueBoxTransform extends RailwardValueBoxTransform {

		private static final double X = 8;
		private static final double Y = 7;
		private static final double HIT_HALF_SIZE = 3 / 16d;
		private static final double HIT_DEPTH = 1.5 / 16d;
		private final Vec3 location;

		private ModernWhistleValueBoxTransform(double z) {
			location = VecHelper.voxelSpace(X, Y, z);
		}

		@Override
		public Vec3 getLocalOffset(BlockState state) {
			Vec3 offset = VecHelper.rotateCentered(location, -state.getValue(ModernWhistleBlock.ROTATION) * 90d,
				Direction.Axis.Y);
			return rotateForAttachment(offset, state.getValue(ModernWhistleBlock.ATTACHED_FACE), false);
		}

		@Override
		public boolean testHit(BlockState state, Vec3 localHit) {
			Vec3 hit = rotateForAttachment(localHit, state.getValue(ModernWhistleBlock.ATTACHED_FACE), true);
			hit = VecHelper.rotateCentered(hit, state.getValue(ModernWhistleBlock.ROTATION) * 90d,
				Direction.Axis.Y);
			return Math.abs(hit.x - location.x) < HIT_HALF_SIZE
				&& Math.abs(hit.y - location.y) < HIT_DEPTH
				&& Math.abs(hit.z - location.z) < HIT_HALF_SIZE;
		}

		@Override
		public void rotate(BlockState state, PoseStack ms) {
			RailwardTransformStack transform = RailwardTransformStack.cast(ms);
			applyAttachmentRotation(transform, state.getValue(ModernWhistleBlock.ATTACHED_FACE));
			transform.rotateY(-state.getValue(ModernWhistleBlock.ROTATION) * 90d)
				.rotateX(90);
		}

		@Override
		public float getScale() {
			return .375f;
		}

		private static Vec3 rotateForAttachment(Vec3 vector, Direction attachedFace, boolean inverse) {
			double sign = inverse ? -1 : 1;
			return switch (attachedFace) {
			case DOWN -> vector;
			case UP -> VecHelper.rotateCentered(vector, 180, Direction.Axis.X);
			case NORTH -> VecHelper.rotateCentered(vector, sign * 90, Direction.Axis.X);
			case SOUTH -> VecHelper.rotateCentered(vector, sign * -90, Direction.Axis.X);
			case EAST -> VecHelper.rotateCentered(vector, sign * 90, Direction.Axis.Z);
			case WEST -> VecHelper.rotateCentered(vector, sign * -90, Direction.Axis.Z);
			};
		}

		private static void applyAttachmentRotation(RailwardTransformStack transform, Direction attachedFace) {
			switch (attachedFace) {
			case DOWN -> {
			}
			case UP -> transform.rotateX(180);
			case NORTH -> transform.rotateX(90);
			case SOUTH -> transform.rotateX(-90);
			case EAST -> transform.rotateZ(90);
			case WEST -> transform.rotateZ(-90);
			}
		}
	}

}
