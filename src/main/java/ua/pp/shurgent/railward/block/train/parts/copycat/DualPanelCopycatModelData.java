package ua.pp.shurgent.railward.block.train.parts.copycat;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.IModelData;
import net.minecraftforge.client.model.data.ModelProperty;

public final class DualPanelCopycatModelData {

	public static final ModelProperty<Direction> SECONDARY_FACING = new ModelProperty<>();
	public static final ModelProperty<BlockState> SECONDARY_MATERIAL = new ModelProperty<>();
	public static final ModelProperty<IModelData> SECONDARY_WRAPPED_DATA = new ModelProperty<>();

	private DualPanelCopycatModelData() {
	}
}
