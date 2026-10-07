package ua.pp.shurgent.railward.compat;

//? if create6 {
/*import com.simibubi.create.api.schematic.requirement.SpecialBlockItemRequirement;
*///?} else
import com.simibubi.create.content.schematics.requirement.ISpecialBlockItemRequirement;

/**
 * Stable Railward name for Create's schematic item-requirement extension point.
 */
//? if create6
/*public interface RailwardSpecialBlockItemRequirement extends SpecialBlockItemRequirement {
*///? if !create6
public interface RailwardSpecialBlockItemRequirement extends ISpecialBlockItemRequirement {
}
