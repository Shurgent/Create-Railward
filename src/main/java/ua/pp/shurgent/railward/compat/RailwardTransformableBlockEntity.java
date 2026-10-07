package ua.pp.shurgent.railward.compat;

import com.simibubi.create.content.contraptions.StructureTransform;

import net.minecraft.world.level.block.entity.BlockEntity;

//? if create6 {
/*import com.simibubi.create.api.contraption.transformable.TransformableBlockEntity;
*///?} else
import com.simibubi.create.content.contraptions.ITransformableBlockEntity;

/**
 * Bridges Create 0.5's instance transform callback to Create 6's registry-style
 * callback without duplicating the transformation logic in every block entity.
 */
//? if create6
/*public interface RailwardTransformableBlockEntity extends TransformableBlockEntity {
*///? if !create6
public interface RailwardTransformableBlockEntity extends ITransformableBlockEntity {

	// Railward implementations keep the Create 0.5-shaped operation as their
	// shared implementation method.
	void transform(StructureTransform transform);

	//? if create6 {
	/*@Override
	default void transform(BlockEntity blockEntity, StructureTransform transform) {
		((RailwardTransformableBlockEntity) blockEntity).transform(transform);
	}
	*///?}
}
