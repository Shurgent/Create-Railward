package ua.pp.shurgent.railward.compat;

//? if create6 {
/*import com.simibubi.create.api.contraption.transformable.TransformableBlock;
*///?} else
import com.simibubi.create.content.contraptions.ITransformableBlock;

/**
 * Stable Railward name for Create's block-transform extension point.
 */
//? if create6
/*public interface RailwardTransformableBlock extends TransformableBlock {
*///? if !create6
public interface RailwardTransformableBlock extends ITransformableBlock {
}
