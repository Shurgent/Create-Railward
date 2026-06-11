package ua.pp.shurgent.railward.client.renderer;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;

public class TrainCouplerContraptionRenderContext {
	private static final ThreadLocal<AbstractContraptionEntity> CURRENT_ENTITY = new ThreadLocal<>();

	private TrainCouplerContraptionRenderContext() {
	}

	public static void set(AbstractContraptionEntity entity) {
		CURRENT_ENTITY.set(entity);
	}

	public static AbstractContraptionEntity get() {
		return CURRENT_ENTITY.get();
	}

	public static void clear() {
		CURRENT_ENTITY.remove();
	}
}
