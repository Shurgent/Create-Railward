package ua.pp.shurgent.railward.block.train.bogey;

import com.simibubi.create.content.trains.bogey.AbstractBogeyBlockEntity;
import com.simibubi.create.content.trains.bogey.BogeyStyle;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import ua.pp.shurgent.railward.RailwardBogeyStyles;

public class FreightBogey18_100BlockEntity extends AbstractBogeyBlockEntity {

	public FreightBogey18_100BlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Override
	public BogeyStyle getDefaultStyle() {
		return RailwardBogeyStyles.FREIGHT_18_100;
	}
}
