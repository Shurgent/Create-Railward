package ua.pp.shurgent.railward.client.sound;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.decoration.steamWhistle.WhistleBlock;
import com.simibubi.create.content.decoration.steamWhistle.WhistleBlock.WhistleSize;
import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.CarriageContraption;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import com.simibubi.create.content.trains.entity.Train;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
//? if >=1.19.2
/*import net.minecraft.util.RandomSource;*/
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ua.pp.shurgent.railward.Railward;
import ua.pp.shurgent.railward.block.train.parts.ModernWhistleSoundType;
import ua.pp.shurgent.railward.block.train.parts.ModernWhistleTrainHelper;

@Mod.EventBusSubscriber(modid = Railward.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ModernWhistleTrainAudio {

	private static final int MANUAL_RELEASE_TICKS = 5;
	private static final int MANUAL_LOOP_FADE_OUT_TICKS = 4;
	private static final int TRAIN_WHISTLE_ATTENUATION_DISTANCE = 128;
	public static final float TRAIN_WHISTLE_VOLUME = .75f;
	private static final Map<Integer, ManualHonkState> ACTIVE_MANUAL_HONKS = new HashMap<>();
	private static final Map<UUID, HeldHonkState> ACTIVE_SECONDARY_HONKS = new HashMap<>();

	private ModernWhistleTrainAudio() {
	}

	@SubscribeEvent
	public static void onClientLogout(ClientPlayerNetworkEvent.LoggedOutEvent event) {
		clearManualHonks();
	}

	@SubscribeEvent
	public static void onClientTick(ClientTickEvent event) {
		if (event.phase != Phase.END)
			return;

		Iterator<HeldHonkState> iterator = ACTIVE_SECONDARY_HONKS.values().iterator();
		while (iterator.hasNext()) {
			if (!iterator.next().tick())
				iterator.remove();
		}
	}

	@Nullable
	public static ModernWhistleSoundType resolveTrainLeadSoundType(CarriageContraptionEntity carriageEntity) {
		return ModernWhistleTrainHelper.resolveTrainLeadSoundType(carriageEntity);
	}

	@Nullable
	public static ModernWhistleSoundType resolveTrainLeadSecondarySoundType(
		CarriageContraptionEntity carriageEntity) {
		return ModernWhistleTrainHelper.resolveTrainLeadSecondarySoundType(carriageEntity);
	}

	@Nullable
	public static ModernWhistleSoundType resolveSoundType(StructureBlockInfo blockInfo) {
		return ModernWhistleTrainHelper.resolveSoundType(blockInfo);
	}

	public static void tickManualHonk(CarriageContraptionEntity carriageEntity, ModernWhistleSoundType soundType,
		int honkTicks) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft == null || minecraft.getSoundManager() == null)
			return;
		if (carriageEntity == null || soundType == null)
			return;

		int carriageId = carriageEntity.getId();
		ManualHonkState state = ACTIVE_MANUAL_HONKS.get(carriageId);
		if (state == null || !state.matches(carriageEntity, soundType) || state.shouldRestart(honkTicks)) {
			if (state != null)
				state.stopLoop();
			state = new ManualHonkState(carriageEntity, soundType);
			ACTIVE_MANUAL_HONKS.put(carriageId, state);
		}

		state.tick(honkTicks);
	}

	public static void stopManualHonk(@Nullable CarriageContraptionEntity carriageEntity) {
		if (carriageEntity == null)
			return;
		ManualHonkState state = ACTIVE_MANUAL_HONKS.remove(carriageEntity.getId());
		if (state != null)
			state.stopLoop();
	}

	public static void setSecondaryHonk(UUID trainId, @Nullable CarriageContraptionEntity carriageEntity,
		@Nullable ModernWhistleSoundType soundType, boolean active) {
		HeldHonkState state = ACTIVE_SECONDARY_HONKS.get(trainId);
		if (!active) {
			if (state != null) {
				ACTIVE_SECONDARY_HONKS.remove(trainId);
				state.release();
			}
			return;
		}

		if (carriageEntity == null || soundType == null)
			return;
		if (state != null && state.matches(carriageEntity, soundType))
			return;
		if (state != null)
			state.stopLoop();
		ACTIVE_SECONDARY_HONKS.put(trainId, new HeldHonkState(carriageEntity, soundType));
	}

	public static void playMovingTrainSound(CarriageContraptionEntity carriageEntity, SoundEvent soundEvent, float volume,
		float pitch) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft == null || minecraft.getSoundManager() == null)
			return;

		minecraft.getSoundManager()
			.queueTickingSound(new MovingTrainOneShotSound(soundEvent, carriageEntity, volume, pitch));
	}

	public static void playDelayedTrainSound(CarriageContraptionEntity carriageEntity, SoundEvent soundEvent,
		float volume, float pitch, int delayTicks) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft == null || minecraft.getSoundManager() == null)
			return;

		minecraft.getSoundManager()
			.playDelayed(new MovingTrainOneShotSound(soundEvent, carriageEntity, volume, pitch), delayTicks);
	}

	public static void playArrivalSequence(CarriageContraptionEntity carriageEntity, ModernWhistleSoundType soundType,
		float volume, float pitch) {
		if (carriageEntity == null || soundType == null)
			return;

		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft == null || minecraft.getSoundManager() == null)
			return;

		playMovingTrainSound(carriageEntity, soundType.getStartEvent(), volume, pitch);

		int loopDelayTicks = soundType.getLoopStartDelayTicks();
		int loopDurationTicks = soundType.getArrivalLoopTicks();
		minecraft.getSoundManager().playDelayed(
			new MovingTrainLoopSound(soundType, carriageEntity, volume, pitch, loopDurationTicks), loopDelayTicks);
		playDelayedTrainSound(carriageEntity, soundType.getEndEvent(), volume, pitch,
			loopDelayTicks + loopDurationTicks);
	}

	public static boolean playWhistleSignSequence(CarriageContraptionEntity carriageEntity, float volume, float pitch) {
		if (carriageEntity == null)
			return false;

		ModernWhistleSoundType soundType = resolveTrainLeadSoundType(carriageEntity);
		if (soundType != null) {
			playArrivalSequence(carriageEntity, soundType, volume, pitch);
			return true;
		}

		TrainWhistleCandidate candidate = findFirstWhistleCandidateInTrain(carriageEntity);
		if (candidate == null)
			return false;

		return playCreateWhistle(candidate.carriageEntity, candidate.blockInfo, volume);
	}

	@Nullable
	private static TrainWhistleCandidate findFirstWhistleCandidateInTrain(
		CarriageContraptionEntity carriageEntity) {
		Carriage carriage = carriageEntity.getCarriage();
		if (carriage == null || carriage.train == null)
			return null;

		Train train = carriage.train;
		for (Carriage currentCarriage : train.carriages) {
			CarriageContraptionEntity currentEntity = currentCarriage.anyAvailableEntity();
			if (currentEntity == null)
				continue;

			WhistleCandidate candidate = findFirstWhistleCandidate(currentEntity);
			if (candidate != null)
				return new TrainWhistleCandidate(currentEntity, candidate.blockInfo);
		}

		return null;
	}

	private static boolean playCreateWhistle(CarriageContraptionEntity carriageEntity,
		StructureBlockInfo blockInfo, float volume) {
		if (!(blockInfo.state.getBlock() instanceof WhistleBlock) || blockInfo.nbt == null)
			return false;

		int whistlePitch = blockInfo.nbt.getInt("Pitch");
		WhistleSize size = blockInfo.state.getValue(WhistleBlock.SIZE);
		float pitch = (float) Math.pow(2, ((size == WhistleSize.SMALL ? 12 : 0) - whistlePitch) / 12.0);
		SoundEvent soundEvent = (size == WhistleSize.LARGE ? AllSoundEvents.WHISTLE_TRAIN_LOW
			: AllSoundEvents.WHISTLE_TRAIN).getMainEvent();
		playMovingTrainSound(carriageEntity, soundEvent, volume, pitch);
		return true;
	}

	@Nullable
	private static WhistleCandidate findFirstWhistleCandidate(CarriageContraptionEntity carriageEntity) {
		Contraption contraption = carriageEntity.getContraption();
		if (!(contraption instanceof CarriageContraption carriageContraption))
			return null;

		Direction assemblyDirection = carriageContraption.getAssemblyDirection();
		Direction.Axis axis = assemblyDirection.getAxis();
		int directionMultiplier = assemblyDirection.getAxisDirection().getStep();

		WhistleCandidate first = null;
		for (StructureBlockInfo blockInfo : contraption.getBlocks().values()) {
			if (!(blockInfo.state.getBlock() instanceof com.simibubi.create.content.decoration.steamWhistle.WhistleBlock))
				continue;

			int score = getAxisCoordinate(blockInfo.pos, axis) * directionMultiplier;
			if (first == null || score < first.score)
				first = new WhistleCandidate(blockInfo, score);
		}

		return first;
	}

	private static int getAxisCoordinate(BlockPos pos, Direction.Axis axis) {
		return switch (axis) {
		case X -> pos.getX();
		case Y -> pos.getY();
		case Z -> pos.getZ();
		};
	}

	private record WhistleCandidate(StructureBlockInfo blockInfo, int score) {
	}

	private record TrainWhistleCandidate(CarriageContraptionEntity carriageEntity,
		StructureBlockInfo blockInfo) {
	}

	private static void clearManualHonks() {
		for (ManualHonkState state : ACTIVE_MANUAL_HONKS.values())
			state.stopLoop();
		ACTIVE_MANUAL_HONKS.clear();
		for (HeldHonkState state : ACTIVE_SECONDARY_HONKS.values())
			state.stopLoop();
		ACTIVE_SECONDARY_HONKS.clear();
	}

	private abstract static class MovingTrainSound extends AbstractTickableSoundInstance {

		protected final WeakReference<CarriageContraptionEntity> carriageEntity;
		protected final float baseVolume;
		protected final float basePitch;

		protected MovingTrainSound(SoundEvent soundEvent, CarriageContraptionEntity carriageEntity, float volume,
			float pitch) {
			super(soundEvent, SoundSource.NEUTRAL
					//? if >=1.19.2
					/*, RandomSource.create()*/
			);
			this.carriageEntity = new WeakReference<>(carriageEntity);
			this.baseVolume = volume;
			this.basePitch = pitch;
			this.attenuation = Attenuation.LINEAR;
			this.relative = false;
			this.looping = false;
			this.delay = 0;
			this.volume = volume;
			this.pitch = pitch;
			syncLocation(carriageEntity);
		}

		@Override
		@Nullable
		public WeighedSoundEvents resolve(SoundManager soundManager) {
			WeighedSoundEvents soundEvents = super.resolve(soundManager);
			Sound resolvedSound = sound;
			if (resolvedSound == SoundManager.EMPTY_SOUND)
				return soundEvents;

			// Keep train-whistle range independent from playback gain.
			sound = new Sound(resolvedSound.getLocation().toString(), resolvedSound.getVolume(),
				resolvedSound.getPitch(), resolvedSound.getWeight(), resolvedSound.getType(),
				resolvedSound.shouldStream(), resolvedSound.shouldPreload(), TRAIN_WHISTLE_ATTENUATION_DISTANCE);
			return soundEvents;
		}

		@Nullable
		protected CarriageContraptionEntity getCarriageEntity() {
			CarriageContraptionEntity carriage = carriageEntity.get();
			// Finite sign sequences keep playing from the last known position after
			// the carriage leaves client tracking range.
			if (carriage == null || carriage.isRemoved())
				return null;

			syncLocation(carriage);
			return carriage;
		}

		protected void syncLocation(CarriageContraptionEntity carriageEntity) {
			Vec3 position = carriageEntity.position();
			x = position.x;
			y = position.y;
			z = position.z;
		}

		@Override
		public boolean canStartSilent() {
			return true;
		}
	}

	private static class MovingTrainOneShotSound extends MovingTrainSound {

		private MovingTrainOneShotSound(SoundEvent soundEvent, CarriageContraptionEntity carriageEntity, float volume,
			float pitch) {
			super(soundEvent, carriageEntity, volume, pitch);
		}

		@Override
		public void tick() {
			getCarriageEntity();
		}
	}

	private static class MovingTrainLoopSound extends MovingTrainSound {

		private int sustainTicks;

		private MovingTrainLoopSound(ModernWhistleSoundType soundType, CarriageContraptionEntity carriageEntity,
			float volume, float pitch, int sustainTicks) {
			super(soundType.getLoopEvent(), carriageEntity, volume, pitch);
			this.sustainTicks = sustainTicks;
			this.looping = true;
			this.volume = volume;
		}

		@Override
		public void tick() {
			getCarriageEntity();

			pitch = basePitch;
			if (sustainTicks-- <= 0) {
				stop();
				return;
			}

			volume = baseVolume;
		}
	}

	private static class ManualTrainLoopSound extends MovingTrainSound {

		private boolean stopping;
		private boolean stopped;
		private int fadeOutTicks = -1;

		private ManualTrainLoopSound(ModernWhistleSoundType soundType, CarriageContraptionEntity carriageEntity,
			float volume, float pitch) {
			super(soundType.getLoopEvent(), carriageEntity, volume, pitch);
			this.looping = true;
			this.volume = volume;
		}

		private void fadeOut() {
			if (stopped || stopping)
				return;
			stopping = true;
			fadeOutTicks = MANUAL_LOOP_FADE_OUT_TICKS;
		}

		private void stopLoop() {
			stopped = true;
		}

		@Override
		public void tick() {
			if (getCarriageEntity() == null) {
				stop();
				return;
			}

			if (stopped) {
				stop();
				return;
			}

			pitch = basePitch;
			if (stopping) {
				volume = baseVolume * Math.max(0f, (float) fadeOutTicks / MANUAL_LOOP_FADE_OUT_TICKS);
				if (fadeOutTicks-- <= 0)
					stop();
				return;
			}

			volume = baseVolume;
		}
	}

	private static class ManualHonkState {

		private final WeakReference<CarriageContraptionEntity> carriageEntity;
		private final ModernWhistleSoundType soundType;
		private ManualTrainLoopSound loopSound;
		private int loopStartDelayTicks;
		private boolean loopStarted;
		private boolean releasing;
		private int lastHonkTicks = Integer.MIN_VALUE;

		private ManualHonkState(CarriageContraptionEntity carriageEntity, ModernWhistleSoundType soundType) {
			this.carriageEntity = new WeakReference<>(carriageEntity);
			this.soundType = soundType;
			this.loopStartDelayTicks = soundType.getLoopStartDelayTicks();
			playMovingTrainSound(carriageEntity, soundType.getStartEvent(), TRAIN_WHISTLE_VOLUME, 1f);
		}

		private boolean matches(CarriageContraptionEntity carriageEntity, ModernWhistleSoundType soundType) {
			return this.carriageEntity.get() == carriageEntity && this.soundType == soundType;
		}

		private boolean shouldRestart(int honkTicks) {
			if (lastHonkTicks == Integer.MIN_VALUE)
				return false;
			if (honkTicks <= lastHonkTicks)
				return false;
			return releasing || lastHonkTicks <= MANUAL_RELEASE_TICKS;
		}

		private void tick(int honkTicks) {
			CarriageContraptionEntity carriageEntity = carriageEntity();
			if (carriageEntity == null)
				return;

			if (!releasing && honkTicks <= MANUAL_RELEASE_TICKS) {
				releasing = true;
				if (loopSound != null)
					loopSound.fadeOut();
				playMovingTrainSound(carriageEntity, soundType.getEndEvent(), TRAIN_WHISTLE_VOLUME, 1f);
				lastHonkTicks = honkTicks;
				return;
			}

			// Match the redstone-driven block logic: once release starts, never keep
			// counting down to loop-start or spawn a late sustain segment.
			if (releasing) {
				lastHonkTicks = honkTicks;
				return;
			}

			if (!loopStarted) {
				if (loopStartDelayTicks > 0) {
					loopStartDelayTicks--;
					lastHonkTicks = honkTicks;
					return;
				}

				loopSound =
					new ManualTrainLoopSound(soundType, carriageEntity, TRAIN_WHISTLE_VOLUME, 1f);
				Minecraft.getInstance()
					.getSoundManager()
					.queueTickingSound(loopSound);
				loopStarted = true;
			}

			lastHonkTicks = honkTicks;
		}

		private void stopLoop() {
			if (loopSound != null)
				loopSound.stopLoop();
		}

		@Nullable
		private CarriageContraptionEntity carriageEntity() {
			CarriageContraptionEntity entity = carriageEntity.get();
			if (entity == null || entity.isRemoved()) {
				if (loopSound != null)
					loopSound.stopLoop();
				return null;
			}
			return entity;
		}
	}

	private static class HeldHonkState {

		private final WeakReference<CarriageContraptionEntity> carriageEntity;
		private final ModernWhistleSoundType soundType;
		private ManualTrainLoopSound loopSound;
		private int loopStartDelayTicks;

		private HeldHonkState(CarriageContraptionEntity carriageEntity, ModernWhistleSoundType soundType) {
			this.carriageEntity = new WeakReference<>(carriageEntity);
			this.soundType = soundType;
			loopStartDelayTicks = soundType.getLoopStartDelayTicks();
			playMovingTrainSound(carriageEntity, soundType.getStartEvent(), TRAIN_WHISTLE_VOLUME, 1f);
		}

		private boolean matches(CarriageContraptionEntity carriageEntity, ModernWhistleSoundType soundType) {
			return this.carriageEntity.get() == carriageEntity && this.soundType == soundType;
		}

		private boolean tick() {
			CarriageContraptionEntity carriageEntity = this.carriageEntity.get();
			if (carriageEntity == null || carriageEntity.isRemoved()) {
				stopLoop();
				return false;
			}

			if (loopSound == null && loopStartDelayTicks-- <= 0) {
				loopSound = new ManualTrainLoopSound(soundType, carriageEntity, TRAIN_WHISTLE_VOLUME, 1f);
				Minecraft.getInstance().getSoundManager().queueTickingSound(loopSound);
			}
			return true;
		}

		private void release() {
			CarriageContraptionEntity carriageEntity = this.carriageEntity.get();
			stopLoop();
			if (carriageEntity != null && !carriageEntity.isRemoved())
				playMovingTrainSound(carriageEntity, soundType.getEndEvent(), TRAIN_WHISTLE_VOLUME, 1f);
		}

		private void stopLoop() {
			if (loopSound != null)
				loopSound.stopLoop();
		}
	}

}
