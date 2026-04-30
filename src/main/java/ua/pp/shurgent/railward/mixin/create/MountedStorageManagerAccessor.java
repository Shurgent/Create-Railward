package ua.pp.shurgent.railward.mixin.create;

import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import com.simibubi.create.content.contraptions.MountedStorage;
import com.simibubi.create.content.contraptions.MountedStorageManager;

import net.minecraft.core.BlockPos;

@Mixin(value = MountedStorageManager.class, remap = false)
public interface MountedStorageManagerAccessor {

	@Accessor("storage")
	Map<BlockPos, MountedStorage> railward$getStorage();

}
