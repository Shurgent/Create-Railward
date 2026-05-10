package ua.pp.shurgent.railward.block.train.parts;

import java.util.List;

import com.simibubi.create.content.decoration.steamWhistle.WhistleBlock.WhistleSize;
import com.simibubi.create.content.decoration.steamWhistle.WhistleBlockEntity;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import ua.pp.shurgent.railward.client.sound.ModernWhistleBlockEntityAudio;

public class ModernWhistleBlockEntity extends WhistleBlockEntity {

	private boolean wasPowered;
	private ModernWhistleSoundType activeSoundType;
	private int loopStartDelay = -1;

	public ModernWhistleBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
		syncTrainWhistleProfile();
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
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
		int mappedPitch = getBlockState().getValue(ModernWhistleBlock.SOUND_TYPE).getTrainPitch();
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

}
