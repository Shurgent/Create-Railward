package ua.pp.shurgent.railward.block.train.parts;

import com.simibubi.create.content.equipment.wrench.IWrenchable;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.material.MaterialColor;

public class TrainWallBlueBlock extends Block implements IWrenchable {

    public TrainWallBlueBlock(BlockBehaviour.Properties properties) {
        super(BlockBehaviour.Properties.of(Material.METAL).strength(1.0f, 6f).sound(SoundType.METAL).color(MaterialColor.COLOR_BLUE));
    }

	@Override
	public InteractionResult onWrenched(BlockState state, UseOnContext context) {
		return InteractionResult.FAIL;
	}
}
