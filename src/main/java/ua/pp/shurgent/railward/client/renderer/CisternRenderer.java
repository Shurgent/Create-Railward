package ua.pp.shurgent.railward.client.renderer;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix3f;
import com.mojang.math.Matrix4f;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;
import ua.pp.shurgent.railward.Railward;
import ua.pp.shurgent.railward.block.train.parts.CisternBlockEntity;

public class CisternRenderer extends SmartBlockEntityRenderer<CisternBlockEntity> {

	private static final ResourceLocation HULL_TEXTURE =
		Railward.asResource("block/train/parts/cistern");
	private static final int WHITE = 0xFFFFFF;
	private static final int DEFAULT_SIDES = 8;
	private static final int LARGE_SIDES = 16;
	private static final float UV_TILE = 16;
	private static final float TEXTURE_PIXELS = 64;
	private static final TextureRegion SIDE_REGION = new TextureRegion(0, 0, 16, 16);
	private static final TextureRegion TOP_REGION = new TextureRegion(16, 0, 16, 16);
	private static final TextureRegion BOTTOM_REGION = new TextureRegion(32, 0, 16, 16);
	private static final TextureRegion CAP_REGION = new TextureRegion(48, 0, 16, 16);
	private static final TextureRegion UPPER_BEVEL_REGION = new TextureRegion(0, 16, 16, 16);
	private static final TextureRegion LOWER_BEVEL_REGION = new TextureRegion(16, 16, 16, 16);
	private static final TextureRegion END_BEVEL_REGION = new TextureRegion(32, 16, 16, 16);
	private static final TextureRegion SUPPORT_REGION = new TextureRegion(48, 16, 16, 16);
	private static final TextureRegion SIDE_VARIANT_9_REGION = new TextureRegion(0, 32, 16, 16);
	private static final TextureRegion SIDE_VARIANT_10_REGION = new TextureRegion(16, 32, 16, 16);
	private static final TextureRegion SIDE_VARIANT_11_REGION = new TextureRegion(32, 32, 16, 16);
	private static final TextureRegion SIDE_VARIANT_12_REGION = new TextureRegion(48, 32, 16, 16);
	private static final TextureRegion UPPER_BEVEL_LEFT_EDGE_REGION = new TextureRegion(0, 48, 16, 16);
	private static final TextureRegion UPPER_BEVEL_RIGHT_EDGE_REGION = new TextureRegion(16, 48, 16, 16);
	private static final TextureRegion LOWER_BEVEL_LEFT_EDGE_REGION = new TextureRegion(32, 48, 16, 16);
	private static final TextureRegion LOWER_BEVEL_RIGHT_EDGE_REGION = new TextureRegion(48, 48, 16, 16);
	private static final float SUPPORT_THICKNESS = 2 / 16f;
	private static final float SUPPORT_HALF_THICKNESS = SUPPORT_THICKNESS / 2;
	private static final float SUPPORT_SMALL_HEIGHT = 4 / 16f;
	private static final float SUPPORT_MEDIUM_HEIGHT = 14 / 16f;
	private static final float EPSILON = 1.0E-4f;

	public CisternRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	protected void renderSafe(CisternBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffers,
		int light, int overlay) {
		if (!be.isController())
			return;

		int width = CisternBlockEntity.isValidWidth(be.getCisternWidth()) ? be.getCisternWidth() : 1;
		int length = Math.max(1, be.getCisternLength());
		if (length < 1)
			return;

		renderCisternModel(ms, buffers, light, be.getCisternAxis(), width, length,
			textureForBlock(be.getBlockState()
				.getBlock()));
	}

	public static void renderCisternModel(PoseStack ms, MultiBufferSource buffers, int light, Direction.Axis axis,
		int width, int length) {
		renderCisternModel(ms, buffers, light, axis, width, length, HULL_TEXTURE);
	}

	public static void renderCisternModel(PoseStack ms, MultiBufferSource buffers, int light, Direction.Axis axis,
		int width, int length, ResourceLocation texture) {
		width = CisternBlockEntity.isValidWidth(width) ? width : 1;
		length = Math.max(1, length);

		TextureAtlasSprite sprite = Minecraft.getInstance()
			.getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
			.apply(texture);
		VertexConsumer vb = buffers.getBuffer(RenderType.solid());
		PoseStack.Pose pose = ms.last();

		int sides = sideCount(width);
		Ring[] rings = createRings(width, length, sides);
		for (int i = 0; i < rings.length - 1; i++)
			renderSegment(pose, vb, sprite, light, axis, width, sides, length, rings[i], rings[i + 1]);
		renderCap(pose, vb, sprite, light, axis, sides, rings[0], false);
		renderCap(pose, vb, sprite, light, axis, sides, rings[rings.length - 1], true);
		renderSupports(pose, vb, sprite, light, axis, width, length);
	}

	public static ResourceLocation textureForBlock(Block block) {
		if (block == null)
			return HULL_TEXTURE;
		ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(block);
		if (blockId == null || !Railward.MOD_ID.equals(blockId.getNamespace()))
			return HULL_TEXTURE;
		return Railward.asResource("block/" + blockId.getPath());
	}

	private static int sideCount(int width) {
		return width == CisternBlockEntity.WIDTH_LARGE ? LARGE_SIDES : DEFAULT_SIDES;
	}

	private static Ring[] createRings(int width, int length, int sides) {
		float center = width / 2f;
		float radius = radiusForApothem(width / 2f, sides);
		if (width == 1)
			return new Ring[] {
				new Ring(0, center, radius),
				new Ring(length, center, radius)
			};

		float bevel = Math.min(width == CisternBlockEntity.WIDTH_SMALL ? .5f : .55f, length / 2f);
		float capApothem = width == CisternBlockEntity.WIDTH_LARGE ? 1.5f
			: width == CisternBlockEntity.WIDTH_SMALL ? 1 : .5f;
		float flatRadius = radiusForApothem(capApothem, sides);
		if (length == 1)
			return new Ring[] {
				new Ring(0, center, flatRadius),
				new Ring(.5f, center, radius),
				new Ring(1, center, flatRadius)
			};
		return new Ring[] {
			new Ring(0, center, flatRadius),
			new Ring(bevel, center, radius),
			new Ring(length - bevel, center, radius),
			new Ring(length, center, flatRadius)
		};
	}

	private static float radiusForApothem(float apothem, int sides) {
		return apothem / Mth.cos(Mth.TWO_PI / (sides * 2f));
	}

	private static void renderSegment(PoseStack.Pose pose, VertexConsumer vb, TextureAtlasSprite sprite, int light,
		Direction.Axis axis, int width, int sides, int length, Ring a, Ring b) {
		float from = a.longPos;
		float to = b.longPos;
		if (to <= from + EPSILON)
			return;

		float cursor = from;
		while (cursor < to - EPSILON) {
			float boundary = Mth.floor(cursor) + 1;
			if (boundary <= cursor + EPSILON)
				boundary += 1;
			float next = Math.min(to, boundary);
			float segmentLength = to - from;
			Ring start = a.lerp(b, (cursor - from) / segmentLength);
			Ring end = a.lerp(b, (next - from) / segmentLength);
			float u0 = tileStart(cursor);
			float u1 = tileEnd(next);
			int cell = Mth.clamp(Mth.floor((cursor + next) * .5f), 0, length - 1);
			renderSegmentTile(pose, vb, sprite, light, axis, width, sides, length, cell, start, end, u0, u1);
			cursor = next;
		}
	}

	private static void renderSegmentTile(PoseStack.Pose pose, VertexConsumer vb, TextureAtlasSprite sprite, int light,
		Direction.Axis axis, int width, int sides, int length, int cell, Ring a, Ring b, float u0, float u1) {
		for (int side = 0; side < sides; side++) {
			Point a0 = a.point(side, sides);
			Point a1 = a.point(side + 1, sides);
			Point b0 = b.point(side, sides);
			Point b1 = b.point(side + 1, sides);
			Point normal = normalForSegment(a, b, a0, a1, b0);
			TextureRegion region = regionFor(normal, sides);
			boolean useSideVariants = width > 1;
			boolean useBevelVariants = canUseBevelEdgeVariants(width, normal, region);
			boolean mirrorU = (useSideVariants && region == SIDE_REGION || useBevelVariants)
				&& ((normal.cross < 0) != (axis == Direction.Axis.Z));
			if (useSideVariants && region == SIDE_REGION)
				region = sideRegionForCell(cell, length, mirrorU);
			else if (useBevelVariants)
				region = bevelEdgeRegionForCell(region, cell, length, mirrorU);
			float tileU0 = mirrorU ? u1 : u0;
			float tileU1 = mirrorU ? u0 : u1;
			float vA0 = 0;
			float vA1 = UV_TILE;
			float vB0 = 0;
			float vB1 = UV_TILE;
			if (usesHeightOrientedV(region)) {
				float minY = Math.min(Math.min(a0.y, a1.y), Math.min(b0.y, b1.y));
				float maxY = Math.max(Math.max(a0.y, a1.y), Math.max(b0.y, b1.y));
				vA0 = heightV(a0.y, minY, maxY);
				vA1 = heightV(a1.y, minY, maxY);
				vB0 = heightV(b0.y, minY, maxY);
				vB1 = heightV(b1.y, minY, maxY);
			}
			if (axis == Direction.Axis.X) {
				quad(pose, vb, sprite, shadeColor(WHITE, shade(normal)), light, axis, region, a.longPos, a0.cross,
					a0.y, tileU0, vA0, b.longPos, b0.cross, b0.y, tileU1, vB0, b.longPos, b1.cross, b1.y, tileU1,
					vB1, a.longPos, a1.cross, a1.y, tileU0, vA1, normal.longAxis, normal.y, normal.cross);
			} else {
				quad(pose, vb, sprite, shadeColor(WHITE, shade(normal)), light, axis, region, a.longPos, a0.cross,
					a0.y, tileU0, vA0, a.longPos, a1.cross, a1.y, tileU0, vA1, b.longPos, b1.cross, b1.y,
					tileU1, vB1, b.longPos, b0.cross, b0.y, tileU1, vB0, normal.longAxis, normal.y, normal.cross);
			}
		}
	}

	private static void renderSupports(PoseStack.Pose pose, VertexConsumer vb, TextureAtlasSprite sprite, int light,
		Direction.Axis axis, int width, int length) {
		if (width == 1) {
			renderCenteredSupport(pose, vb, sprite, light, axis, .5f);
			float positiveCenter = length - .5f;
			if (positiveCenter > .5f + EPSILON)
				renderCenteredSupport(pose, vb, sprite, light, axis, positiveCenter);
			return;
		}

		float height = width == CisternBlockEntity.WIDTH_SMALL ? SUPPORT_MEDIUM_HEIGHT : 1;
		int crossStart = width == CisternBlockEntity.WIDTH_LARGE ? 1 : 0;
		int crossEnd = width == CisternBlockEntity.WIDTH_LARGE ? width - 1 : width;
		renderSupportRow(pose, vb, sprite, light, axis, crossStart, crossEnd, 1 - SUPPORT_THICKNESS, 1, height);
		renderSupportRow(pose, vb, sprite, light, axis, crossStart, crossEnd, length - 1,
			length - 1 + SUPPORT_THICKNESS, height);
	}

	private static void renderCenteredSupport(PoseStack.Pose pose, VertexConsumer vb, TextureAtlasSprite sprite, int light,
		Direction.Axis axis, float longCenter) {
		renderSupportRow(pose, vb, sprite, light, axis, 0, 1, longCenter - SUPPORT_HALF_THICKNESS,
			longCenter + SUPPORT_HALF_THICKNESS, SUPPORT_SMALL_HEIGHT);
	}

	private static void renderSupportRow(PoseStack.Pose pose, VertexConsumer vb, TextureAtlasSprite sprite, int light,
		Direction.Axis axis, int crossStart, int crossEnd, float longMin, float longMax, float height) {
		for (int cross = crossStart; cross < crossEnd; cross++)
			renderSupportBlock(pose, vb, sprite, light, axis, longMin, longMax, cross, cross + 1, 0, height,
				cross == crossStart, cross == crossEnd - 1);
	}

	private static void renderSupportBlock(PoseStack.Pose pose, VertexConsumer vb, TextureAtlasSprite sprite, int light,
		Direction.Axis axis, float l0, float l1, float c0, float c1, float y0, float y1, boolean renderCrossMin,
		boolean renderCrossMax) {
		if (axis == Direction.Axis.X) {
			supportQuad(pose, vb, sprite, light, axis, l0, c1, y1, l1, c1, y1, l1, c0, y1, l0, c0, y1, 0, 1, 0);
			supportQuad(pose, vb, sprite, light, axis, l0, c0, y0, l1, c0, y0, l1, c1, y0, l0, c1, y0, 0, -1, 0);
			if (renderCrossMin)
				supportQuad(pose, vb, sprite, light, axis, l1, c0, y0, l0, c0, y0, l0, c0, y1, l1, c0, y1, 0, 0,
					-1);
			if (renderCrossMax)
				supportQuad(pose, vb, sprite, light, axis, l0, c1, y0, l1, c1, y0, l1, c1, y1, l0, c1, y1, 0, 0,
					1);
			supportQuad(pose, vb, sprite, light, axis, l0, c0, y0, l0, c1, y0, l0, c1, y1, l0, c0, y1, -1, 0, 0);
			supportQuad(pose, vb, sprite, light, axis, l1, c1, y0, l1, c0, y0, l1, c0, y1, l1, c1, y1, 1, 0, 0);
			return;
		}

		supportQuad(pose, vb, sprite, light, axis, l1, c0, y1, l1, c1, y1, l0, c1, y1, l0, c0, y1, 0, 1, 0);
		supportQuad(pose, vb, sprite, light, axis, l0, c0, y0, l0, c1, y0, l1, c1, y0, l1, c0, y0, 0, -1, 0);
		if (renderCrossMin)
			supportQuad(pose, vb, sprite, light, axis, l0, c0, y0, l1, c0, y0, l1, c0, y1, l0, c0, y1, 0, 0,
				-1);
		if (renderCrossMax)
			supportQuad(pose, vb, sprite, light, axis, l1, c1, y0, l0, c1, y0, l0, c1, y1, l1, c1, y1, 0, 0, 1);
		supportQuad(pose, vb, sprite, light, axis, l0, c1, y0, l0, c0, y0, l0, c0, y1, l0, c1, y1, -1, 0, 0);
		supportQuad(pose, vb, sprite, light, axis, l1, c0, y0, l1, c1, y0, l1, c1, y1, l1, c0, y1, 1, 0, 0);
	}

	private static void supportQuad(PoseStack.Pose pose, VertexConsumer vb, TextureAtlasSprite sprite, int light,
		Direction.Axis axis, float l1, float c1, float y1, float l2, float c2, float y2, float l3, float c3,
		float y3, float l4, float c4, float y4, float nl, float ny, float nc) {
		int color = shadeColor(WHITE, shade(new Point(nl, ny, nc)));
		quad(pose, vb, sprite, color, light, axis, SUPPORT_REGION, l1, c1, y1, 0, UV_TILE, l2, c2, y2, UV_TILE,
			UV_TILE, l3, c3, y3, UV_TILE, 0, l4, c4, y4, 0, 0, nl, ny, nc);
	}

	private static void renderCap(PoseStack.Pose pose, VertexConsumer vb, TextureAtlasSprite sprite, int light,
		Direction.Axis axis, int sides, Ring ring, boolean positive) {
		float normalLong = positive ? 1 : -1;
		int color = shadeColor(WHITE, .78f);

		List<CapPoint> outline = new ArrayList<>(sides);
		for (int side = 0; side < sides; side++) {
			Point point = ring.point(side, sides);
			outline.add(new CapPoint(point.cross, point.y));
		}

		int minCross = Mth.floor(ring.center - ring.radius - EPSILON);
		int maxCross = (int) Math.ceil(ring.center + ring.radius + EPSILON);
		int minY = Mth.floor(ring.center - ring.radius - EPSILON);
		int maxY = (int) Math.ceil(ring.center + ring.radius + EPSILON);

		for (int crossCell = minCross; crossCell < maxCross; crossCell++)
			for (int yCell = minY; yCell < maxY; yCell++) {
				List<CapPoint> polygon = clippedCapCell(outline, crossCell, yCell);
				if (polygon.size() < 3)
					continue;
				CapPoint hub = centroid(polygon);
				for (int i = 0; i < polygon.size(); i++) {
					CapPoint p0 = polygon.get(i);
					CapPoint p1 = polygon.get((i + 1) % polygon.size());
					renderCapTriangle(pose, vb, sprite, color, light, axis, ring.longPos, normalLong, positive,
						crossCell, yCell, hub, p0, p1);
				}
			}
	}

	private static Point normalForSegment(Ring a, Ring b, Point a0, Point a1, Point b0) {
		float longDelta = b.longPos - a.longPos;
		float crossDelta = b0.cross - a0.cross;
		float yDelta = b0.y - a0.y;
		float edgeCross = a1.cross - a0.cross;
		float edgeY = a1.y - a0.y;
		float longAxis = edgeCross * yDelta - edgeY * crossDelta;
		float cross = edgeY * longDelta;
		float y = -edgeCross * longDelta;
		float length = Mth.sqrt(longAxis * longAxis + cross * cross + y * y);
		if (length <= 1.0E-4f)
			return new Point(0, 1, 0);
		return new Point(longAxis / length, y / length, cross / length);
	}

	private static List<CapPoint> clippedCapCell(List<CapPoint> outline, int crossCell, int yCell) {
		List<CapPoint> polygon = new ArrayList<>(4);
		polygon.add(new CapPoint(crossCell, yCell));
		polygon.add(new CapPoint(crossCell + 1, yCell));
		polygon.add(new CapPoint(crossCell + 1, yCell + 1));
		polygon.add(new CapPoint(crossCell, yCell + 1));

		for (int i = 0; i < outline.size() && !polygon.isEmpty(); i++) {
			CapPoint edgeStart = outline.get(i);
			CapPoint edgeEnd = outline.get((i + 1) % outline.size());
			polygon = clipPolygon(polygon, edgeStart, edgeEnd);
		}
		return polygon;
	}

	private static List<CapPoint> clipPolygon(List<CapPoint> polygon, CapPoint edgeStart, CapPoint edgeEnd) {
		List<CapPoint> result = new ArrayList<>(polygon.size() + 1);
		CapPoint previous = polygon.get(polygon.size() - 1);
		boolean previousInside = isInside(previous, edgeStart, edgeEnd);

		for (CapPoint current : polygon) {
			boolean currentInside = isInside(current, edgeStart, edgeEnd);
			if (currentInside) {
				if (!previousInside)
					result.add(intersection(previous, current, edgeStart, edgeEnd));
				result.add(current);
			} else if (previousInside) {
				result.add(intersection(previous, current, edgeStart, edgeEnd));
			}
			previous = current;
			previousInside = currentInside;
		}
		return result;
	}

	private static boolean isInside(CapPoint point, CapPoint edgeStart, CapPoint edgeEnd) {
		float edgeCross = edgeEnd.cross - edgeStart.cross;
		float edgeY = edgeEnd.y - edgeStart.y;
		float pointCross = point.cross - edgeStart.cross;
		float pointY = point.y - edgeStart.y;
		return edgeCross * pointY - edgeY * pointCross >= -EPSILON;
	}

	private static CapPoint intersection(CapPoint start, CapPoint end, CapPoint edgeStart, CapPoint edgeEnd) {
		float segmentCross = end.cross - start.cross;
		float segmentY = end.y - start.y;
		float edgeCross = edgeEnd.cross - edgeStart.cross;
		float edgeY = edgeEnd.y - edgeStart.y;
		float denominator = segmentCross * edgeY - segmentY * edgeCross;
		if (Mth.abs(denominator) <= EPSILON)
			return end;

		float startToEdgeCross = edgeStart.cross - start.cross;
		float startToEdgeY = edgeStart.y - start.y;
		float t = (startToEdgeCross * edgeY - startToEdgeY * edgeCross) / denominator;
		t = Mth.clamp(t, 0, 1);
		return new CapPoint(Mth.lerp(t, start.cross, end.cross), Mth.lerp(t, start.y, end.y));
	}

	private static CapPoint centroid(List<CapPoint> polygon) {
		float cross = 0;
		float y = 0;
		for (CapPoint point : polygon) {
			cross += point.cross;
			y += point.y;
		}
		return new CapPoint(cross / polygon.size(), y / polygon.size());
	}

	private static void renderCapTriangle(PoseStack.Pose pose, VertexConsumer vb, TextureAtlasSprite sprite, int color,
		int light, Direction.Axis axis, float longPos, float normalLong, boolean positive, int crossCell, int yCell,
		CapPoint hub, CapPoint p0, CapPoint p1) {
		float hubU = capTileU(hub, crossCell);
		float hubV = capTileV(hub, yCell);
		float p0U = capTileU(p0, crossCell);
		float p0V = capTileV(p0, yCell);
		float p1U = capTileU(p1, crossCell);
		float p1V = capTileV(p1, yCell);

		if (positive == (axis == Direction.Axis.Z)) {
			quad(pose, vb, sprite, color, light, axis, CAP_REGION, longPos, hub.cross, hub.y, hubU, hubV, longPos,
				p0.cross, p0.y, p0U, p0V, longPos, p1.cross, p1.y, p1U, p1V, longPos, hub.cross, hub.y, hubU, hubV,
				normalLong, 0, 0);
		} else {
			quad(pose, vb, sprite, color, light, axis, CAP_REGION, longPos, hub.cross, hub.y, hubU, hubV, longPos,
				p1.cross, p1.y, p1U, p1V, longPos, p0.cross, p0.y, p0U, p0V, longPos, hub.cross, hub.y, hubU, hubV,
				normalLong, 0, 0);
		}
	}

	private static float capTileU(CapPoint point, int crossCell) {
		return Mth.clamp((point.cross - crossCell) * UV_TILE, 0, UV_TILE);
	}

	private static float capTileV(CapPoint point, int yCell) {
		return Mth.clamp((1 - (point.y - yCell)) * UV_TILE, 0, UV_TILE);
	}

	private static void quad(PoseStack.Pose pose, VertexConsumer vb, TextureAtlasSprite sprite, int color, int light,
		Direction.Axis axis, TextureRegion region, float l1, float c1, float y1, float u1, float v1, float l2,
		float c2, float y2, float u2, float v2, float l3, float c3, float y3, float u3, float v3, float l4,
		float c4, float y4, float u4, float v4, float nl, float ny, float nc) {
		vertex(pose, vb, color, light, axis, l1, c1, y1, spriteU(sprite, region, u1),
			spriteV(sprite, region, v1), nl, ny, nc);
		vertex(pose, vb, color, light, axis, l2, c2, y2, spriteU(sprite, region, u2),
			spriteV(sprite, region, v2), nl, ny, nc);
		vertex(pose, vb, color, light, axis, l3, c3, y3, spriteU(sprite, region, u3),
			spriteV(sprite, region, v3), nl, ny, nc);
		vertex(pose, vb, color, light, axis, l4, c4, y4, spriteU(sprite, region, u4),
			spriteV(sprite, region, v4), nl, ny, nc);
	}

	private static TextureRegion regionFor(Point normal, int sides) {
		if (Mth.abs(normal.longAxis) > .08f)
			return END_BEVEL_REGION;
		if (Mth.abs(normal.y) > .92f)
			return normal.y > 0 ? TOP_REGION : BOTTOM_REGION;
		float sideThreshold = sides == LARGE_SIDES ? .98f : .92f;
		if (Mth.abs(normal.cross) > sideThreshold)
			return SIDE_REGION;
		return normal.y >= 0 ? UPPER_BEVEL_REGION : LOWER_BEVEL_REGION;
	}

	private static TextureRegion sideRegionForCell(int cell, int length, boolean mirrored) {
		int visualCell = mirrored ? length - 1 - cell : cell;
		if (visualCell <= 0 || visualCell >= length - 1)
			return SIDE_REGION;

		int innerCell = visualCell - 1;
		int innerLength = length - 2;
		if (innerLength == 1)
			return SIDE_VARIANT_12_REGION;
		if (innerLength == 2)
			return innerCell == 0 ? SIDE_VARIANT_9_REGION : SIDE_VARIANT_12_REGION;
		if (innerLength == 3)
			return switch (innerCell) {
				case 0 -> SIDE_VARIANT_9_REGION;
				case 1 -> SIDE_VARIANT_11_REGION;
				default -> SIDE_VARIANT_12_REGION;
			};

		if (innerCell == 0)
			return SIDE_VARIANT_9_REGION;
		if (innerCell == 1)
			return SIDE_VARIANT_10_REGION;
		if (innerCell == innerLength - 2)
			return SIDE_VARIANT_11_REGION;
		if (innerCell == innerLength - 1)
			return SIDE_VARIANT_12_REGION;
		return SIDE_REGION;
	}

	private static boolean canUseBevelEdgeVariants(int width, Point normal, TextureRegion region) {
		if (width <= 1 || region != UPPER_BEVEL_REGION && region != LOWER_BEVEL_REGION)
			return false;
		return width != CisternBlockEntity.WIDTH_LARGE || Mth.abs(normal.cross) > .8f;
	}

	private static TextureRegion bevelEdgeRegionForCell(TextureRegion baseRegion, int cell, int length,
		boolean mirrored) {
		int visualCell = mirrored ? length - 1 - cell : cell;
		if (visualCell <= 0 || visualCell >= length - 1)
			return baseRegion;
		if (visualCell == 1)
			return baseRegion == UPPER_BEVEL_REGION ? UPPER_BEVEL_LEFT_EDGE_REGION : LOWER_BEVEL_LEFT_EDGE_REGION;
		if (visualCell == length - 2)
			return baseRegion == UPPER_BEVEL_REGION ? UPPER_BEVEL_RIGHT_EDGE_REGION : LOWER_BEVEL_RIGHT_EDGE_REGION;
		return baseRegion;
	}

	private static boolean usesHeightOrientedV(TextureRegion region) {
		return region == SIDE_REGION || region == SIDE_VARIANT_9_REGION || region == SIDE_VARIANT_10_REGION
			|| region == SIDE_VARIANT_11_REGION || region == SIDE_VARIANT_12_REGION || region == UPPER_BEVEL_REGION
			|| region == LOWER_BEVEL_REGION || region == UPPER_BEVEL_LEFT_EDGE_REGION
			|| region == UPPER_BEVEL_RIGHT_EDGE_REGION || region == LOWER_BEVEL_LEFT_EDGE_REGION
			|| region == LOWER_BEVEL_RIGHT_EDGE_REGION;
	}

	private static float heightV(float y, float minY, float maxY) {
		if (maxY <= minY + EPSILON)
			return 0;
		return Mth.clamp((maxY - y) / (maxY - minY) * UV_TILE, 0, UV_TILE);
	}

	private static float spriteU(TextureAtlasSprite sprite, TextureRegion region, float u) {
		float pixel = region.u + Mth.clamp(u, 0, UV_TILE) / UV_TILE * region.width;
		return sprite.getU(pixel / TEXTURE_PIXELS * UV_TILE);
	}

	private static float spriteV(TextureAtlasSprite sprite, TextureRegion region, float v) {
		float pixel = region.v + Mth.clamp(v, 0, UV_TILE) / UV_TILE * region.height;
		return sprite.getV(pixel / TEXTURE_PIXELS * UV_TILE);
	}

	private static float tileStart(float coordinate) {
		float value = coordinate - Mth.floor(coordinate);
		return value * UV_TILE;
	}

	private static float tileEnd(float coordinate) {
		float value = coordinate - Mth.floor(coordinate);
		return value <= EPSILON ? UV_TILE : value * UV_TILE;
	}

	private static void vertex(PoseStack.Pose pose, VertexConsumer vb, int color, int light, Direction.Axis axis, float l,
		float c, float y, float u, float v, float nl, float ny, float nc) {
		Matrix4f model = pose.pose();
		Matrix3f normal = pose.normal();
		float x = axis == Direction.Axis.X ? l : c;
		float z = axis == Direction.Axis.X ? c : l;
		float nx = axis == Direction.Axis.X ? nl : nc;
		float nz = axis == Direction.Axis.X ? nc : nl;
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

	private static float shade(Point normal) {
		if (normal.y > .92f)
			return 1;
		if (normal.y < -.92f)
			return .54f;

		float horizontal = Mth.sqrt(normal.longAxis * normal.longAxis + normal.cross * normal.cross);
		float shade = .78f + normal.y * .16f - horizontal * .05f;
		return Mth.clamp(shade, .56f, .94f);
	}

	private static int shadeColor(int color, float shade) {
		int r = Mth.clamp((int) ((color >> 16 & 255) * shade), 0, 255);
		int g = Mth.clamp((int) ((color >> 8 & 255) * shade), 0, 255);
		int b = Mth.clamp((int) ((color & 255) * shade), 0, 255);
		return r << 16 | g << 8 | b;
	}

	@Override
	public boolean shouldRenderOffScreen(CisternBlockEntity be) {
		return be.isController();
	}

	@Override
	public int getViewDistance() {
		return 192;
	}

	private record Ring(float longPos, float center, float radius) {
		Ring lerp(Ring other, float t) {
			return new Ring(Mth.lerp(t, longPos, other.longPos), Mth.lerp(t, center, other.center),
				Mth.lerp(t, radius, other.radius));
		}

		Point point(int index, int sides) {
			float angle = ((index - .5f) / sides) * Mth.TWO_PI;
			return new Point(0, center + Mth.sin(angle) * radius, center + Mth.cos(angle) * radius);
		}
	}

	private record Point(float longAxis, float y, float cross) {
	}

	private record CapPoint(float cross, float y) {
	}

	private record TextureRegion(float u, float v, float width, float height) {
	}

}
