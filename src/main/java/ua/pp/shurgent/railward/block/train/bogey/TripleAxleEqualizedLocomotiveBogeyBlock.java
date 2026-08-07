package ua.pp.shurgent.railward.block.train.bogey;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.trains.bogey.AbstractBogeyBlock;
import com.simibubi.create.content.trains.bogey.BogeySizes;
import com.simibubi.create.content.trains.bogey.BogeyStyle;
import com.simibubi.create.content.trains.track.TrackMaterial;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import ua.pp.shurgent.railward.AllBlockEntities;
import ua.pp.shurgent.railward.RailwardBogeyStyles;

public class TripleAxleEqualizedLocomotiveBogeyBlock
		extends AbstractBogeyBlock<TripleAxleEqualizedLocomotiveBogeyBlockEntity> {

	public TripleAxleEqualizedLocomotiveBogeyBlock(Properties properties) {
		super(properties, BogeySizes.SMALL);
	}

	@Override
	public TrackMaterial.TrackType getTrackType(BogeyStyle style) {
		return TrackMaterial.TrackType.STANDARD;
	}

	@Override
	public double getWheelPointSpacing() {
		return 4;
	}

	@Override
	public double getWheelRadius() {
		return .5;
	}

	@Override
	protected Vec3 getConnectorAnchorOffset() {
		return new Vec3(0, 7 / 32f, 2);
	}

	@Override
	public BogeyStyle getDefaultStyle() {
		return RailwardBogeyStyles.TRIPLE_AXLE_EQUALIZED_LOCOMOTIVE;
	}

	@Override
	public ItemStack getCloneItemStack(BlockState state, HitResult target, BlockGetter level, BlockPos pos,
			Player player) {
		return AllBlocks.RAILWAY_CASING.asStack();
	}

	@Override
	public Class<TripleAxleEqualizedLocomotiveBogeyBlockEntity> getBlockEntityClass() {
		return TripleAxleEqualizedLocomotiveBogeyBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends TripleAxleEqualizedLocomotiveBogeyBlockEntity> getBlockEntityType() {
		return AllBlockEntities.TRIPLE_AXLE_EQUALIZED_LOCOMOTIVE_BOGEY.get();
	}
}
