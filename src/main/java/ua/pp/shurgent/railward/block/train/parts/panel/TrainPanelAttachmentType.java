package ua.pp.shurgent.railward.block.train.parts.panel;

import java.util.function.Supplier;

import net.minecraft.resources.ResourceLocation;

public class TrainPanelAttachmentType<T extends TrainPanelAttachment> {

	private final ResourceLocation id;
	private final Supplier<T> factory;

	public TrainPanelAttachmentType(ResourceLocation id, Supplier<T> factory) {
		this.id = id;
		this.factory = factory;
	}

	public ResourceLocation getId() {
		return id;
	}

	public T create() {
		return factory.get();
	}
}
