package ua.pp.shurgent.railward.block.train.parts.panel;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import ua.pp.shurgent.railward.Railward;

public class TrainPanelAttachmentRegistry {

	private static final String TYPE_KEY = "Type";
	private static final String DATA_KEY = "Data";
	private static final Map<ResourceLocation, TrainPanelAttachmentType<?>> TYPES = new HashMap<>();

	public static final TrainPanelAttachmentType<LocomotiveBufferLightsAttachment> LOCOMOTIVE_BUFFER_LIGHTS =
		register("locomotive_buffer_lights", LocomotiveBufferLightsAttachment::new);
	public static final TrainPanelAttachmentType<LocomotiveSpotlightAttachment> LOCOMOTIVE_SPOTLIGHT =
		register("locomotive_spotlight", LocomotiveSpotlightAttachment::new);

	private TrainPanelAttachmentRegistry() {
	}

	private static <T extends TrainPanelAttachment> TrainPanelAttachmentType<T> register(String id,
		java.util.function.Supplier<T> factory) {
		TrainPanelAttachmentType<T> type = new TrainPanelAttachmentType<>(Railward.asResource(id), factory);
		TYPES.put(type.getId(), type);
		return type;
	}

	public static CompoundTag write(TrainPanelAttachment attachment) {
		CompoundTag tag = new CompoundTag();
		tag.putString(TYPE_KEY, attachment.getType()
			.getId()
			.toString());
		CompoundTag data = new CompoundTag();
		attachment.write(data);
		tag.put(DATA_KEY, data);
		return tag;
	}

	@Nullable
	public static TrainPanelAttachment read(CompoundTag tag) {
		ResourceLocation id = ResourceLocation.tryParse(tag.getString(TYPE_KEY));
		if (id == null)
			return null;
		TrainPanelAttachmentType<?> type = TYPES.get(id);
		if (type == null)
			return null;
		TrainPanelAttachment attachment = type.create();
		attachment.read(tag.getCompound(DATA_KEY));
		return attachment;
	}
}
