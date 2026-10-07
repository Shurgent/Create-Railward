package ua.pp.shurgent.railward.client.compat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.simibubi.create.content.contraptions.Contraption;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Client-side access to contraption block entities across Create generations. */
public final class RailwardContraptionBlockEntities {

	private RailwardContraptionBlockEntities() {
	}

	public static BlockEntity get(Contraption contraption, BlockPos pos) {
		//? if create6
		/*return contraption.getBlockEntityClientSide(pos);
		*///? if !create6
		return contraption.presentBlockEntities.get(pos);
	}

	public static Iterable<BlockEntity> values(Contraption contraption) {
		//? if create6
		/*return contraption.getOrCreateClientContraptionLazy().renderedBlockEntityView;
		*///? if !create6
		return contraption.presentBlockEntities.values();
	}

	public static Iterable<Map.Entry<BlockPos, BlockEntity>> entries(Contraption contraption) {
		//? if create6 {
		/*List<Map.Entry<BlockPos, BlockEntity>> entries = new ArrayList<>();
		for (BlockEntity blockEntity : contraption.getOrCreateClientContraptionLazy().renderedBlockEntityView)
			entries.add(Map.entry(blockEntity.getBlockPos(), blockEntity));
		return entries;
		*///?} else
		return contraption.presentBlockEntities.entrySet();
	}
}
