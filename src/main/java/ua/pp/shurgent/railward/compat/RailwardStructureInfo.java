package ua.pp.shurgent.railward.compat;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;

public final class RailwardStructureInfo {

	private RailwardStructureInfo() {
	}

	public static BlockPos pos(StructureBlockInfo value) {
		//? if >=1.20.1
		/*return value.pos();
		*///? if <1.20.1
		return value.pos;
	}

	public static BlockState state(StructureBlockInfo value) {
		//? if >=1.20.1
		/*return value.state();
		*///? if <1.20.1
		return value.state;
	}

	public static CompoundTag nbt(StructureBlockInfo value) {
		//? if >=1.20.1
		/*return value.nbt();
		*///? if <1.20.1
		return value.nbt;
	}
}
