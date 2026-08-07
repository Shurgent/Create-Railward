package ua.pp.shurgent.railward.block.train.parts.copycat;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelProperty;

/** Model-data keys shared by the slab block entity and its client model. */
public final class CopycatSlabModelData {

	public static final ModelProperty<BlockState> BOTTOM_MATERIAL = new ModelProperty<>();
	public static final ModelProperty<BlockState> TOP_MATERIAL = new ModelProperty<>();

	private CopycatSlabModelData() {
	}
}
