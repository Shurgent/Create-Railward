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

public class FreightBogey18_100Block extends AbstractBogeyBlock<FreightBogey18_100BlockEntity> {

	public FreightBogey18_100Block(Properties properties) {
		super(properties, BogeySizes.SMALL);
	}

	@Override
	public TrackMaterial.TrackType getTrackType(BogeyStyle style) {
		return TrackMaterial.TrackType.STANDARD;
	}

	@Override
	public double getWheelPointSpacing() {
		return 2;
	}

	@Override
	public double getWheelRadius() {
		return 15 / 32d;
	}

	@Override
	protected Vec3 getConnectorAnchorOffset() {
		return new Vec3(0, 7 / 32f, 1);
	}

	@Override
	public BogeyStyle getDefaultStyle() {
		return RailwardBogeyStyles.FREIGHT_18_100;
	}

	@Override
	public ItemStack getCloneItemStack(BlockState state, HitResult target, BlockGetter level, BlockPos pos,
			Player player) {
		return AllBlocks.RAILWAY_CASING.asStack();
	}

	@Override
	public Class<FreightBogey18_100BlockEntity> getBlockEntityClass() {
		return FreightBogey18_100BlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends FreightBogey18_100BlockEntity> getBlockEntityType() {
		return AllBlockEntities.FREIGHT_BOGEY_18_100.get();
	}
}
