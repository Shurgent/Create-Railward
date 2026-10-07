package ua.pp.shurgent.railward.client.compat;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Bridges the context parameters added to ValueBoxTransform by Create 6. */
public abstract class RailwardValueBoxTransform extends ValueBoxTransform {

	public abstract Vec3 getLocalOffset(BlockState state);

	public abstract void rotate(BlockState state, PoseStack poseStack);

	public abstract boolean testHit(BlockState state, Vec3 localHit);

	//? if create6 {
	/*@Override
	public final Vec3 getLocalOffset(LevelAccessor level, BlockPos pos, BlockState state) {
		return getLocalOffset(state);
	}

	@Override
	public final void rotate(LevelAccessor level, BlockPos pos, BlockState state, PoseStack poseStack) {
		rotate(state, poseStack);
	}

	@Override
	public final boolean testHit(LevelAccessor level, BlockPos pos, BlockState state, Vec3 localHit) {
		return testHit(state, localHit);
	}
	*///?}
}
