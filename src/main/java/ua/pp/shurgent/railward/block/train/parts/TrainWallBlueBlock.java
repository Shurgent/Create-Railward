package ua.pp.shurgent.railward.block.train.parts;

import com.simibubi.create.content.equipment.wrench.IWrenchable;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MaterialColor;

public class TrainWallBlueBlock extends Block implements IWrenchable {

    public TrainWallBlueBlock(BlockBehaviour.Properties properties) {
        this(properties, MaterialColor.COLOR_BLUE);
    }

    public TrainWallBlueBlock(BlockBehaviour.Properties properties, MaterialColor color) {
        super(properties.strength(1.0f, 6f).sound(SoundType.METAL).color(color));
    }

	@Override
	public InteractionResult onWrenched(BlockState state, UseOnContext context) {
		return InteractionResult.FAIL;
	}
}
