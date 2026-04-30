package ua.pp.shurgent.railward.client.renderer;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix3f;
import com.mojang.math.Matrix4f;
import com.mojang.math.Quaternion;
import com.mojang.math.Vector3f;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import com.simibubi.create.foundation.utility.animation.LerpedFloat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.client.model.data.EmptyModelData;
import ua.pp.shurgent.railward.block.train.parts.BulkCargoContainerBlock;
import ua.pp.shurgent.railward.block.train.parts.BulkCargoContainerBlockEntity;

public class BulkCargoContainerRenderer extends SmartBlockEntityRenderer<BulkCargoContainerBlockEntity> {

	private static final int WHITE = 0xFFFFFF;
	private static final int DEFAULT_ITEM_MATERIAL_COLOR = 0x6C6C6C;
	private static final float BLOCK_SURFACE_TILE = .5f;
	private static final float ITEM_SURFACE_TILE = 1f;
	private static final float DECORATION_LIFT = 1 / 96f;
	private static final float MODEL_DECORATION_SCALE = 1.35f;
	private static final float MODEL_DECORATION_MAX_FOOTPRINT = .72f;
	private static final float GROUND_MODEL_HALF_EXTENT = .125f;
	private static final float FALLBACK_MODEL_FOOTPRINT = .55f;
	private static final float MODEL_HEIGHT_JITTER = .001f;
	private static final ResourceLocation ITEM_MATERIAL_NOISE =
		resourceLocation("minecraft:block/gravel");
	private static final Map<ResourceLocation, Integer> AVERAGE_COLOR_CACHE = new HashMap<>();
	private static final Map<BakedModel, ModelFootprint> MODEL_FOOTPRINT_CACHE = new IdentityHashMap<>();

	private static final Decoration[] BLOCK_DECORATIONS = {
		new Decoration(.20f, .23f, .37f, 13),
		new Decoration(.67f, .25f, .39f, -21),
		new Decoration(.43f, .54f, .35f, 43),
		new Decoration(.79f, .73f, .35f, 78),
		new Decoration(.18f, .80f, .38f, -52)
	};
	private static final Decoration[] ITEM_DECORATIONS = {
		new Decoration(.17f, .18f, .41f, 30),
		new Decoration(.52f, .22f, .35f, -28),
		new Decoration(.82f, .37f, .40f, 24),
		new Decoration(.31f, .52f, .32f, 77),
		new Decoration(.65f, .67f, .44f, -8),
		new Decoration(.24f, .83f, .37f, -43)
	};

	public BulkCargoContainerRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	protected void renderSafe(BulkCargoContainerBlockEntity be, float partialTicks, PoseStack ms,
		MultiBufferSource buffers, int light, int overlay) {
		ItemStack displayStack = be.getDisplayStack();
		if (displayStack.isEmpty())
			return;

		boolean virtualCargoView = be.hasClientCargoView();
		LerpedFloat cargoLevel = be.getCargoLevel();
		if (!virtualCargoView && cargoLevel == null)
			return;

		float fill = virtualCargoView ? be.getFillState() : cargoLevel.getValue(partialTicks);
		if (fill <= 0)
			return;

		BakedModel itemModel = Minecraft.getInstance()
			.getItemRenderer()
			.getModel(displayStack, be.getLevel(), null, 0);
		TextureAtlasSprite itemSprite = itemModel.getParticleIcon(EmptyModelData.INSTANCE);
		if (itemSprite == null)
			return;

		CargoRenderMode renderMode = getRenderMode(displayStack, itemModel);
		float modelFootprint = renderMode == CargoRenderMode.MODEL_ITEM
			? getModelFootprint(itemModel, displayStack.getItem() instanceof BlockItem)
			: 0;
		TextureAtlasSprite surfaceSprite = renderMode == CargoRenderMode.SIMPLE_BLOCK ? itemSprite
			: getItemMaterialNoise();
		int surfaceColor = renderMode == CargoRenderMode.SIMPLE_BLOCK ? WHITE : getAverageSpriteColor(itemSprite);
		float surfaceTile = renderMode == CargoRenderMode.SIMPLE_BLOCK ? BLOCK_SURFACE_TILE : ITEM_SURFACE_TILE;

		if (virtualCargoView) {
			renderVirtualCell(be, fill, displayStack, renderMode, buffers, light, itemSprite, surfaceSprite,
				surfaceColor, surfaceTile, modelFootprint, ms);
			return;
		}
		if (!be.isController())
			return;

		float wallInset = 2 / 16f + 1 / 128f;
		float xMin = wallInset;
		float xMax = be.getSizeX() - wallInset;
		float zMin = wallInset;
		float zMax = be.getSizeZ() - wallInset;
		float yMin = 2 / 16f;
		float yMax = Mth.clamp(fill * be.getHeight(), yMin + 1 / 64f, be.getHeight() - 1 / 64f);
		if (xMax <= xMin || zMax <= zMin || yMax <= yMin)
			return;

		VertexConsumer vb = buffers.getBuffer(RenderType.solid());
		PoseStack.Pose pose = ms.last();
		renderTop(pose, vb, surfaceSprite, surfaceColor, light, surfaceTile, xMin, xMax, yMax, zMin, zMax);
		renderNorthSouth(pose, vb, surfaceSprite, surfaceColor, light, surfaceTile, xMin, xMax, yMin, yMax, zMin,
			zMax);
		renderWestEast(pose, vb, surfaceSprite, surfaceColor, light, surfaceTile, xMin, xMax, yMin, yMax, zMin,
			zMax);
		renderSurfaceDecorations(displayStack, renderMode, itemSprite, buffers, light, ms, xMin, xMax, yMax,
			zMin, zMax, modelFootprint);
	}

	private void renderVirtualCell(BulkCargoContainerBlockEntity be, float fill, ItemStack displayStack,
		CargoRenderMode renderMode, MultiBufferSource buffers, int light, TextureAtlasSprite itemSprite,
		TextureAtlasSprite surfaceSprite, int surfaceColor, float surfaceTile, float modelFootprint, PoseStack ms) {
		int layer = be.getClientCargoLayer();
		float fillBlocks = fill * be.getHeight();
		float localFill = Mth.clamp(fillBlocks - layer, 0, 1);
		if (localFill <= 0)
			return;

		BlockState state = be.getBlockState();
		float inset = 1 / 128f;
		float wallInset = 2 / 16f + inset;
		float xMin = state.getValue(BulkCargoContainerBlock.WEST) ? wallInset : 0;
		float xMax = state.getValue(BulkCargoContainerBlock.EAST) ? 1 - wallInset : 1;
		float zMin = state.getValue(BulkCargoContainerBlock.NORTH) ? wallInset : 0;
		float zMax = state.getValue(BulkCargoContainerBlock.SOUTH) ? 1 - wallInset : 1;
		float yMin = state.getValue(BulkCargoContainerBlock.BOTTOM) ? 2 / 16f : 0;
		float yMax = Mth.clamp(localFill, yMin + 1 / 64f, 1 - 1 / 64f);
		if (xMax <= xMin || zMax <= zMin || yMax <= yMin)
			return;

		VertexConsumer vb = buffers.getBuffer(RenderType.solid());
		PoseStack.Pose pose = ms.last();
		renderTop(pose, vb, surfaceSprite, surfaceColor, light, surfaceTile, xMin, xMax, yMax, zMin, zMax);
		if (state.getValue(BulkCargoContainerBlock.NORTH))
			renderNorth(pose, vb, surfaceSprite, surfaceColor, light, surfaceTile, xMin, xMax, yMin, yMax, zMin);
		if (state.getValue(BulkCargoContainerBlock.SOUTH))
			renderSouth(pose, vb, surfaceSprite, surfaceColor, light, surfaceTile, xMin, xMax, yMin, yMax, zMax);
		if (state.getValue(BulkCargoContainerBlock.WEST))
			renderWest(pose, vb, surfaceSprite, surfaceColor, light, surfaceTile, xMin, yMin, yMax, zMin, zMax);
		if (state.getValue(BulkCargoContainerBlock.EAST))
			renderEast(pose, vb, surfaceSprite, surfaceColor, light, surfaceTile, xMax, yMin, yMax, zMin, zMax);

		int topLayer = Mth.clamp(Mth.ceil(fillBlocks) - 1, 0, be.getHeight() - 1);
		if (layer == topLayer)
			renderSurfaceDecorations(displayStack, renderMode, itemSprite, buffers, light, ms, xMin, xMax, yMax, zMin,
				zMax, modelFootprint);
	}

	private void renderSurfaceDecorations(ItemStack displayStack, CargoRenderMode renderMode,
		TextureAtlasSprite itemSprite, MultiBufferSource buffers, int light, PoseStack ms, float xMin, float xMax,
		float y, float zMin, float zMax, float modelFootprint) {
		if (renderMode == CargoRenderMode.SIMPLE_BLOCK)
			renderBlockDecorations(itemSprite, buffers.getBuffer(RenderType.solid()), light, ms, xMin, xMax, y, zMin,
				zMax);
		else if (renderMode == CargoRenderMode.MODEL_ITEM)
			renderModelDecorations(displayStack, buffers, light, ms, xMin, xMax, y, zMin, zMax, modelFootprint);
		else
			renderItemDecorations(itemSprite, buffers.getBuffer(RenderType.cutout()), light, ms, xMin, xMax, y, zMin,
				zMax);
	}

	private void renderModelDecorations(ItemStack displayStack, MultiBufferSource buffers, int light, PoseStack ms,
		float xMin, float xMax, float y, float zMin, float zMax, float modelFootprint) {
		boolean layOnSide = displayStack.getItem() instanceof BlockItem;
		forEachSurfaceCell(xMin, xMax, zMin, zMax, (x0, x1, z0, z1) -> {
			float cellScale = Math.min(x1 - x0, z1 - z0);
			if (cellScale <= .35f)
				return;

			for (Decoration decoration : ITEM_DECORATIONS) {
				float size = getModelDecorationSize(cellScale, modelFootprint);
				float x = Mth.lerp(decoration.x(), x0, x1);
				float z = Mth.lerp(decoration.z(), z0, z1);
				ms.pushPose();
				ms.translate(x, y + DECORATION_LIFT + size * GROUND_MODEL_HALF_EXTENT
					+ decoration.heightJitter() * MODEL_HEIGHT_JITTER, z);
				ms.mulPose(Vector3f.YP.rotationDegrees(decoration.yaw()));
				if (layOnSide)
					ms.mulPose(Vector3f.XP.rotationDegrees(90));
				ms.scale(size, size, size);
				Minecraft.getInstance()
					.getItemRenderer()
					.renderStatic(displayStack, ItemTransforms.TransformType.GROUND, light,
						OverlayTexture.NO_OVERLAY, ms, buffers, 0);
				ms.popPose();
			}
		});
	}

	private void renderBlockDecorations(TextureAtlasSprite sprite, VertexConsumer vb, int light, PoseStack ms,
		float xMin, float xMax, float y, float zMin, float zMax) {
		forEachSurfaceCell(xMin, xMax, zMin, zMax, (x0, x1, z0, z1) -> {
			float cellScale = Math.min(x1 - x0, z1 - z0);
			if (cellScale <= .35f)
				return;

			for (Decoration decoration : BLOCK_DECORATIONS) {
				float size = decoration.size() * cellScale;
				float x = Mth.lerp(decoration.x(), x0, x1);
				float z = Mth.lerp(decoration.z(), z0, z1);
				ms.pushPose();
				ms.translate(x, y + DECORATION_LIFT, z);
				ms.mulPose(Vector3f.YP.rotationDegrees(decoration.yaw()));
				ms.scale(size, size, size);
				renderTexturedCube(ms.last(), vb, sprite, WHITE, light);
				ms.popPose();
			}
		});
	}

	private void renderItemDecorations(TextureAtlasSprite sprite, VertexConsumer vb, int light, PoseStack ms,
		float xMin, float xMax, float y, float zMin, float zMax) {
		forEachSurfaceCell(xMin, xMax, zMin, zMax, (x0, x1, z0, z1) -> {
			float cellScale = Math.min(x1 - x0, z1 - z0);
			if (cellScale <= .35f)
				return;

			for (Decoration decoration : ITEM_DECORATIONS) {
				float size = decoration.size() * cellScale;
				float x = Mth.lerp(decoration.x(), x0, x1);
				float z = Mth.lerp(decoration.z(), z0, z1);
				ms.pushPose();
				ms.translate(x, y + DECORATION_LIFT, z);
				ms.mulPose(Vector3f.YP.rotationDegrees(decoration.yaw()));
				ms.scale(size, size, size);
				renderFlatItemSprite(ms.last(), vb, sprite, light);
				ms.popPose();
			}
		});
	}

	private void forEachSurfaceCell(float xMin, float xMax, float zMin, float zMax, SurfaceCellConsumer consumer) {
		for (int x = Mth.floor(xMin); x < Mth.ceil(xMax); x++)
			for (int z = Mth.floor(zMin); z < Mth.ceil(zMax); z++) {
				float x0 = Math.max(xMin, x);
				float x1 = Math.min(xMax, x + 1);
				float z0 = Math.max(zMin, z);
				float z1 = Math.min(zMax, z + 1);
				if (x1 <= x0 || z1 <= z0)
					continue;
				consumer.accept(x0, x1, z0, z1);
			}
	}

	private void renderTexturedCube(PoseStack.Pose pose, VertexConsumer vb, TextureAtlasSprite sprite, int color,
		int light) {
		float min = -.5f;
		float max = .5f;
		float top = 1;
		quad(pose, vb, sprite, color, light, min, top, max, max, top, max, max, top, min, min, top, min, 0, 1, 0);
		quad(pose, vb, sprite, color, light, min, 0, min, max, 0, min, max, 0, max, min, 0, max, 0, -1, 0);
		quad(pose, vb, sprite, color, light, max, 0, min, min, 0, min, min, top, min, max, top, min, 0, 0, -1);
		quad(pose, vb, sprite, color, light, min, 0, max, max, 0, max, max, top, max, min, top, max, 0, 0, 1);
		quad(pose, vb, sprite, color, light, min, 0, min, min, 0, max, min, top, max, min, top, min, -1, 0, 0);
		quad(pose, vb, sprite, color, light, max, 0, max, max, 0, min, max, top, min, max, top, max, 1, 0, 0);
	}

	private void renderFlatItemSprite(PoseStack.Pose pose, VertexConsumer vb, TextureAtlasSprite sprite, int light) {
		float min = -.5f;
		float max = .5f;
		float thickness = 1 / 64f;
		quad(pose, vb, sprite, WHITE, light, min, thickness, max, max, thickness, max, max, thickness, min, min,
			thickness, min, 0, 1, 0);
		quad(pose, vb, sprite, WHITE, light, min, 0, min, max, 0, min, max, 0, max, min, 0, max, 0, -1, 0);
	}

	private void renderTop(PoseStack.Pose pose, VertexConsumer vb, TextureAtlasSprite sprite, int color, int light,
		float tileSize, float xMin, float xMax, float y, float zMin, float zMax) {
		forEachTile(xMin, xMax, zMin, zMax, tileSize, (x0, x1, z0, z1) -> quad(pose, vb, sprite, color, light,
			x0, y, z1, x1, y, z1, x1, y, z0, x0, y, z0, 0, 1, 0));
	}

	private void renderNorthSouth(PoseStack.Pose pose, VertexConsumer vb, TextureAtlasSprite sprite, int color,
		int light, float tileSize, float xMin, float xMax, float yMin, float yMax, float zMin, float zMax) {
		renderNorth(pose, vb, sprite, color, light, tileSize, xMin, xMax, yMin, yMax, zMin);
		renderSouth(pose, vb, sprite, color, light, tileSize, xMin, xMax, yMin, yMax, zMax);
	}

	private void renderWestEast(PoseStack.Pose pose, VertexConsumer vb, TextureAtlasSprite sprite, int color,
		int light, float tileSize, float xMin, float xMax, float yMin, float yMax, float zMin, float zMax) {
		renderWest(pose, vb, sprite, color, light, tileSize, xMin, yMin, yMax, zMin, zMax);
		renderEast(pose, vb, sprite, color, light, tileSize, xMax, yMin, yMax, zMin, zMax);
	}

	private void renderNorth(PoseStack.Pose pose, VertexConsumer vb, TextureAtlasSprite sprite, int color, int light,
		float tileSize, float xMin, float xMax, float yMin, float yMax, float z) {
		forEachTile(xMin, xMax, yMin, yMax, tileSize, (x0, x1, y0, y1) -> quad(pose, vb, sprite, color, light,
			x1, y0, z, x0, y0, z, x0, y1, z, x1, y1, z, 0, 0, -1));
	}

	private void renderSouth(PoseStack.Pose pose, VertexConsumer vb, TextureAtlasSprite sprite, int color, int light,
		float tileSize, float xMin, float xMax, float yMin, float yMax, float z) {
		forEachTile(xMin, xMax, yMin, yMax, tileSize, (x0, x1, y0, y1) -> quad(pose, vb, sprite, color, light,
			x0, y0, z, x1, y0, z, x1, y1, z, x0, y1, z, 0, 0, 1));
	}

	private void renderWest(PoseStack.Pose pose, VertexConsumer vb, TextureAtlasSprite sprite, int color, int light,
		float tileSize, float x, float yMin, float yMax, float zMin, float zMax) {
		forEachTile(zMin, zMax, yMin, yMax, tileSize, (z0, z1, y0, y1) -> quad(pose, vb, sprite, color, light,
			x, y0, z0, x, y0, z1, x, y1, z1, x, y1, z0, -1, 0, 0));
	}

	private void renderEast(PoseStack.Pose pose, VertexConsumer vb, TextureAtlasSprite sprite, int color, int light,
		float tileSize, float x, float yMin, float yMax, float zMin, float zMax) {
		forEachTile(zMin, zMax, yMin, yMax, tileSize, (z0, z1, y0, y1) -> quad(pose, vb, sprite, color, light,
			x, y0, z1, x, y0, z0, x, y1, z0, x, y1, z1, 1, 0, 0));
	}

	private void quad(PoseStack.Pose pose, VertexConsumer vb, TextureAtlasSprite sprite, int color, int light,
		float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float x4,
		float y4, float z4, float nx, float ny, float nz) {
		float u0 = sprite.getU0();
		float u1 = sprite.getU1();
		float v0 = sprite.getV0();
		float v1 = sprite.getV1();
		vertex(pose, vb, color, light, x1, y1, z1, u0, v1, nx, ny, nz);
		vertex(pose, vb, color, light, x2, y2, z2, u1, v1, nx, ny, nz);
		vertex(pose, vb, color, light, x3, y3, z3, u1, v0, nx, ny, nz);
		vertex(pose, vb, color, light, x4, y4, z4, u0, v0, nx, ny, nz);
	}

	private void vertex(PoseStack.Pose pose, VertexConsumer vb, int color, int light, float x, float y, float z,
		float u, float v, float nx, float ny, float nz) {
		Matrix4f model = pose.pose();
		Matrix3f normal = pose.normal();
		int r = color >> 16 & 255;
		int g = color >> 8 & 255;
		int b = color & 255;
		vb.vertex(model, x, y, z)
			.color(r, g, b, 255)
			.uv(u, v)
			.overlayCoords(OverlayTexture.NO_OVERLAY)
			.uv2(light)
			.normal(normal, nx, ny, nz)
			.endVertex();
	}

	private TextureAtlasSprite getItemMaterialNoise() {
		return Minecraft.getInstance()
			.getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
			.apply(ITEM_MATERIAL_NOISE);
	}

	private int getAverageSpriteColor(TextureAtlasSprite sprite) {
		ResourceLocation spriteName = sprite.getName();
		return AVERAGE_COLOR_CACHE.computeIfAbsent(spriteName, this::loadAverageSpriteColor);
	}

	private int loadAverageSpriteColor(ResourceLocation spriteName) {
		ResourceLocation texture = resourceLocation(spriteName.getNamespace() + ":textures/" + spriteName.getPath()
			+ ".png");
		try {
			Resource resource = Minecraft.getInstance()
				.getResourceManager()
				.getResource(texture);
			try (InputStream stream = resource.getInputStream(); NativeImage image = NativeImage.read(stream)) {
				long r = 0;
				long g = 0;
				long b = 0;
				long pixels = 0;
				for (int y = 0; y < image.getHeight(); y++)
					for (int x = 0; x < image.getWidth(); x++) {
						int pixel = image.getPixelRGBA(x, y);
						int alpha = NativeImage.getA(pixel);
						if (alpha < 32)
							continue;
						r += NativeImage.getR(pixel) * alpha;
						g += NativeImage.getG(pixel) * alpha;
						b += NativeImage.getB(pixel) * alpha;
						pixels += alpha;
					}
				if (pixels <= 0)
					return DEFAULT_ITEM_MATERIAL_COLOR;
				return brightenMaterialColor((int) (r / pixels), (int) (g / pixels), (int) (b / pixels));
			}
		} catch (IOException | RuntimeException e) {
			return DEFAULT_ITEM_MATERIAL_COLOR;
		}
	}

	private int brightenMaterialColor(int r, int g, int b) {
		r = Mth.clamp((int) (r * .85f) + 48, 0, 255);
		g = Mth.clamp((int) (g * .85f) + 48, 0, 255);
		b = Mth.clamp((int) (b * .85f) + 48, 0, 255);
		return r << 16 | g << 8 | b;
	}

	private float getModelDecorationSize(float cellScale, float modelFootprint) {
		float baseSize = MODEL_DECORATION_SCALE * cellScale;
		if (modelFootprint <= 0)
			return baseSize;
		return Math.min(baseSize, MODEL_DECORATION_MAX_FOOTPRINT * cellScale / modelFootprint);
	}

	private float getModelFootprint(BakedModel model, boolean layOnSide) {
		ModelFootprint footprint = MODEL_FOOTPRINT_CACHE.computeIfAbsent(model, this::measureModelFootprint);
		return layOnSide ? footprint.onSide() : footprint.upright();
	}

	@SuppressWarnings("deprecation")
	private ModelFootprint measureModelFootprint(BakedModel model) {
		ItemTransform groundTransform = model.getTransforms()
			.getTransform(ItemTransforms.TransformType.GROUND);
		Quaternion groundRotation = new Quaternion(groundTransform.rotation.x(), groundTransform.rotation.y(),
			groundTransform.rotation.z(), true);
		Quaternion sideRotation = Vector3f.XP.rotationDegrees(90);
		float[] maxRadius = new float[2];
		boolean foundVertices = false;

		foundVertices |= measureModelQuads(model.getQuads(null, null, new Random(42L)), groundTransform,
			groundRotation, sideRotation, maxRadius);
		for (Direction direction : Direction.values())
			foundVertices |= measureModelQuads(model.getQuads(null, direction, new Random(42L)), groundTransform,
				groundRotation, sideRotation, maxRadius);

		if (!foundVertices)
			return new ModelFootprint(FALLBACK_MODEL_FOOTPRINT, FALLBACK_MODEL_FOOTPRINT);
		return new ModelFootprint(maxRadius[0] * 2, maxRadius[1] * 2);
	}

	private boolean measureModelQuads(List<BakedQuad> quads, @SuppressWarnings("deprecation") ItemTransform transform, Quaternion groundRotation,
		Quaternion sideRotation, float[] maxRadius) {
		boolean foundVertices = false;
		for (BakedQuad quad : quads) {
			int[] vertices = quad.getVertices();
			int stride = vertices.length / 4;
			if (stride < 3)
				continue;

			for (int i = 0; i < 4; i++) {
				int offset = i * stride;
				float x = Float.intBitsToFloat(vertices[offset]) - .5f;
				float y = Float.intBitsToFloat(vertices[offset + 1]) - .5f;
				float z = Float.intBitsToFloat(vertices[offset + 2]) - .5f;
				maxRadius[0] = Math.max(maxRadius[0],
					getTransformedHorizontalRadius(x, y, z, transform, groundRotation, sideRotation, false));
				maxRadius[1] = Math.max(maxRadius[1],
					getTransformedHorizontalRadius(x, y, z, transform, groundRotation, sideRotation, true));
				foundVertices = true;
			}
		}
		return foundVertices;
	}

	private float getTransformedHorizontalRadius(float x, float y, float z, @SuppressWarnings("deprecation") ItemTransform transform,
		Quaternion groundRotation, Quaternion sideRotation, boolean layOnSide) {
		Vector3f point = new Vector3f(x, y, z);
		point.mul(transform.scale.x(), transform.scale.y(), transform.scale.z());
		point.transform(groundRotation);
		if (layOnSide)
			point.transform(sideRotation);
		return Mth.sqrt(point.x() * point.x() + point.z() * point.z());
	}

	private CargoRenderMode getRenderMode(ItemStack stack, BakedModel itemModel) {
		if (!(stack.getItem() instanceof BlockItem blockItem))
			return itemModel.isGui3d() || itemModel.isCustomRenderer() ? CargoRenderMode.MODEL_ITEM
				: CargoRenderMode.FLAT_ITEM;

		Block block = blockItem.getBlock();
		BlockState defaultState = block.defaultBlockState();
		if (defaultState.getRenderShape() != RenderShape.MODEL)
			return CargoRenderMode.MODEL_ITEM;

		if (block instanceof SlabBlock)
			return CargoRenderMode.SIMPLE_BLOCK;

		VoxelShape shape = defaultState.getShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
		return Block.isShapeFullBlock(shape) ? CargoRenderMode.SIMPLE_BLOCK : CargoRenderMode.MODEL_ITEM;
	}

	private void forEachTile(float aMin, float aMax, float bMin, float bMax, float tileSize,
		SurfaceCellConsumer consumer) {
		int minA = Mth.floor(aMin / tileSize);
		int maxA = Mth.ceil(aMax / tileSize);
		int minB = Mth.floor(bMin / tileSize);
		int maxB = Mth.ceil(bMax / tileSize);
		for (int a = minA; a < maxA; a++)
			for (int b = minB; b < maxB; b++) {
				float a0 = Math.max(aMin, a * tileSize);
				float a1 = Math.min(aMax, (a + 1) * tileSize);
				float b0 = Math.max(bMin, b * tileSize);
				float b1 = Math.min(bMax, (b + 1) * tileSize);
				if (a1 <= a0 || b1 <= b0)
					continue;
				consumer.accept(a0, a1, b0, b1);
			}
	}

	private static ResourceLocation resourceLocation(String id) {
		ResourceLocation resourceLocation = ResourceLocation.tryParse(id);
		if (resourceLocation == null)
			throw new IllegalArgumentException("Invalid resource location: " + id);
		return resourceLocation;
	}

	@Override
	public boolean shouldRenderOffScreen(BulkCargoContainerBlockEntity be) {
		return be.isController() || be.hasClientCargoView();
	}

	private record Decoration(float x, float z, float size, float yaw) {
		float heightJitter() {
			float value = x * 13.37f + z * 7.91f + yaw * .013f;
			return value - Mth.floor(value);
		}
	}

	private record ModelFootprint(float upright, float onSide) {
	}

	private enum CargoRenderMode {
		SIMPLE_BLOCK,
		MODEL_ITEM,
		FLAT_ITEM
	}

	@FunctionalInterface
	private interface SurfaceCellConsumer {
		void accept(float x0, float x1, float z0, float z1);
	}

}
