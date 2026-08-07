package ua.pp.shurgent.railward.client.renderer;

import java.util.Set;

import com.mojang.blaze3d.shaders.Program;
import com.mojang.logging.LogUtils;

import net.minecraft.resources.ResourceLocation;

import org.slf4j.Logger;

public final class LocomotiveSpotlightForwardShaderPatcher {

	private static final Logger LOGGER = LogUtils.getLogger();
	private static final Set<String> VANILLA_TERRAIN_PROGRAMS = Set.of("rendertype_solid", "rendertype_cutout",
		"rendertype_cutout_mipped", "rendertype_translucent", "rendertype_tripwire");
	private static final Set<String> VANILLA_TWO_SIDED_TERRAIN_PROGRAMS = Set.of("rendertype_cutout",
		"rendertype_cutout_mipped", "rendertype_tripwire");
	private static final Set<String> VANILLA_ENTITY_PROGRAMS = Set.of("rendertype_entity_solid",
		"rendertype_entity_cutout", "rendertype_entity_cutout_no_cull",
		"rendertype_entity_cutout_no_cull_z_offset", "rendertype_entity_smooth_cutout",
		"rendertype_entity_translucent");
	private static final Set<String> VANILLA_BAKED_ENTITY_PROGRAMS = Set.of("rendertype_armor_cutout_no_cull",
		"rendertype_entity_no_outline", "rendertype_entity_translucent_cull",
		"rendertype_item_entity_translucent_cull");

	private static final String LIGHT_UNIFORMS = """
		uniform int RailwardSpotlightCount;
		uniform float RailwardSpotlightLength;
		uniform float RailwardSpotlightRadius;
		uniform vec3 RailwardSpotlightPosition0;
		uniform vec3 RailwardSpotlightPosition1;
		uniform vec3 RailwardSpotlightPosition2;
		uniform vec3 RailwardSpotlightPosition3;
		uniform vec3 RailwardSpotlightDirection0;
		uniform vec3 RailwardSpotlightDirection1;
		uniform vec3 RailwardSpotlightDirection2;
		uniform vec3 RailwardSpotlightDirection3;

		float railwardSpotlightFactorMode(vec3 surfacePosition, vec3 surfaceNormal, vec3 lightPosition,
		        vec3 lightDirection, float twoSided) {
		    vec3 fromLight = surfacePosition - lightPosition;
		    float axialDistance = dot(fromLight, lightDirection);
		    if (axialDistance <= 0.0 || axialDistance >= RailwardSpotlightLength) {
		        return 0.0;
		    }

		    float nearRadius = min(2.0, RailwardSpotlightRadius);
		    float radius = mix(nearRadius, RailwardSpotlightRadius,
		        axialDistance / max(RailwardSpotlightLength, 0.001));
		    float radialDistance = length(fromLight - lightDirection * axialDistance);
		    float radialFade = 1.0 - smoothstep(radius * 0.72, radius, radialDistance);
		    if (radialFade <= 0.0) {
		        return 0.0;
		    }

		    float incidenceDot = dot(surfaceNormal, normalize(-fromLight));
		    float incidence = mix(max(incidenceDot, 0.0), abs(incidenceDot), twoSided);
		    if (incidence <= 0.0) {
		        return 0.0;
		    }

		    float surfaceFactor = 0.2 + 0.8 * pow(incidence, 0.65);
		    float distanceFade = 1.0 - smoothstep(0.0, RailwardSpotlightLength, length(fromLight));
		    float nearFade = smoothstep(0.02, 0.2, axialDistance);
		    return radialFade * distanceFade * nearFade * surfaceFactor;
		}

		vec3 railwardApplySpotlightStrengthMode(vec3 currentLighting, vec3 surfacePosition, vec3 surfaceNormal,
		        float strength, float twoSided) {
		    if (RailwardSpotlightCount <= 0) {
		        return currentLighting;
		    }

		    float intensity = railwardSpotlightFactorMode(surfacePosition, surfaceNormal,
		        RailwardSpotlightPosition0, RailwardSpotlightDirection0, twoSided);
		    if (RailwardSpotlightCount > 1) {
		        intensity = max(intensity, railwardSpotlightFactorMode(surfacePosition, surfaceNormal,
		            RailwardSpotlightPosition1, RailwardSpotlightDirection1, twoSided));
		    }
		    if (RailwardSpotlightCount > 2) {
		        intensity = max(intensity, railwardSpotlightFactorMode(surfacePosition, surfaceNormal,
		            RailwardSpotlightPosition2, RailwardSpotlightDirection2, twoSided));
		    }
		    if (RailwardSpotlightCount > 3) {
		        intensity = max(intensity, railwardSpotlightFactorMode(surfacePosition, surfaceNormal,
		            RailwardSpotlightPosition3, RailwardSpotlightDirection3, twoSided));
		    }

		    float ambientBrightness = max(currentLighting.r, max(currentLighting.g, currentLighting.b));
		    float environmentFactor = 1.0 - smoothstep(0.65, 0.95, ambientBrightness);
		    vec3 spotlightLighting = vec3(1.0, 0.94, 0.84)
		        * clamp(intensity * environmentFactor * 1.35 * strength, 0.0, 1.0);
		    return max(currentLighting, spotlightLighting);
		}

		vec3 railwardApplySpotlightStrength(vec3 currentLighting, vec3 surfacePosition, vec3 surfaceNormal,
		        float strength) {
		    return railwardApplySpotlightStrengthMode(currentLighting, surfacePosition, surfaceNormal, strength, 0.0);
		}

		vec3 railwardApplySpotlight(vec3 currentLighting, vec3 surfacePosition, vec3 surfaceNormal) {
		    return railwardApplySpotlightStrengthMode(currentLighting, surfacePosition, surfaceNormal, 1.15, 0.0);
		}

		vec3 railwardApplySpotlightTwoSided(vec3 currentLighting, vec3 surfacePosition, vec3 surfaceNormal) {
		    return railwardApplySpotlightStrengthMode(currentLighting, surfacePosition, surfaceNormal, 1.15, 1.0);
		}

		float railwardSpotlightVolumeFactor(vec3 surfacePosition, vec3 lightPosition, vec3 lightDirection) {
		    vec3 fromLight = surfacePosition - lightPosition;
		    float axialDistance = dot(fromLight, lightDirection);
		    if (axialDistance <= 0.0 || axialDistance >= RailwardSpotlightLength) {
		        return 0.0;
		    }

		    float nearRadius = min(2.0, RailwardSpotlightRadius);
		    float radius = mix(nearRadius, RailwardSpotlightRadius,
		        axialDistance / max(RailwardSpotlightLength, 0.001));
		    float radialDistance = length(fromLight - lightDirection * axialDistance);
		    float radialFade = 1.0 - smoothstep(radius * 0.72, radius, radialDistance);
		    float distanceFade = 1.0 - smoothstep(0.0, RailwardSpotlightLength, length(fromLight));
		    float nearFade = smoothstep(0.02, 0.2, axialDistance);
		    return radialFade * distanceFade * nearFade;
		}

		vec3 railwardApplySpotlightVolume(vec3 currentLighting, vec3 surfacePosition) {
		    if (RailwardSpotlightCount <= 0) {
		        return currentLighting;
		    }

		    float intensity = railwardSpotlightVolumeFactor(surfacePosition,
		        RailwardSpotlightPosition0, RailwardSpotlightDirection0);
		    if (RailwardSpotlightCount > 1) {
		        intensity = max(intensity, railwardSpotlightVolumeFactor(surfacePosition,
		            RailwardSpotlightPosition1, RailwardSpotlightDirection1));
		    }
		    if (RailwardSpotlightCount > 2) {
		        intensity = max(intensity, railwardSpotlightVolumeFactor(surfacePosition,
		            RailwardSpotlightPosition2, RailwardSpotlightDirection2));
		    }
		    if (RailwardSpotlightCount > 3) {
		        intensity = max(intensity, railwardSpotlightVolumeFactor(surfacePosition,
		            RailwardSpotlightPosition3, RailwardSpotlightDirection3));
		    }

		    float ambientBrightness = max(currentLighting.r, max(currentLighting.g, currentLighting.b));
		    float environmentFactor = 1.0 - smoothstep(0.65, 0.95, ambientBrightness);
		    vec3 spotlightLighting = vec3(1.0, 0.94, 0.84)
		        * clamp(intensity * environmentFactor * 1.35, 0.0, 1.0);
		    return max(currentLighting, spotlightLighting);
		}

		""";

	private LocomotiveSpotlightForwardShaderPatcher() {
	}

	public static String patchVanilla(Program.Type type, String name, String source) {
		if ("particle".equals(name))
			return type == Program.Type.VERTEX ? patchParticleVertex(source) : patchParticleFragment(source);
		if ("rendertype_translucent_moving_block".equals(name))
			return type == Program.Type.VERTEX ? patchImmediateTranslucentVertex(name, source, false)
				: patchImmediateTranslucentFragment(name, source);
		if ("rendertype_translucent_no_crumbling".equals(name))
			return type == Program.Type.VERTEX ? patchImmediateTranslucentVertex(name, source, true)
				: patchImmediateTranslucentFragment(name, source);
		if (VANILLA_TERRAIN_PROGRAMS.contains(name))
			return type == Program.Type.VERTEX ? patchVanillaVertex(name, source) : patchVanillaFragment(name, source);
		if (VANILLA_ENTITY_PROGRAMS.contains(name))
			return type == Program.Type.VERTEX ? patchEntityVertex(name, source) : patchEntityFragment(name, source);
		if (VANILLA_BAKED_ENTITY_PROGRAMS.contains(name))
			return type == Program.Type.VERTEX ? patchBakedEntityVertex(name, source)
				: patchBakedEntityFragment(name, source);
		return source;
	}

	private static String patchImmediateTranslucentVertex(String name, String source, boolean customLightSampler) {
		String projectionUniform = "uniform mat4 ProjMat;";
		String outputAnchor = "out vec4 normal;";
		String lightAnchor = customLightSampler
			? "texCoord2 = UV2;"
			: "vertexColor = Color * texelFetch(Sampler2, UV2 / 16, 0);";
		if (!source.contains(projectionUniform) || !source.contains(outputAnchor) || !source.contains(lightAnchor)) {
			LOGGER.warn("Could not inject Railward spotlight into immediate translucent vertex shader {}", name);
			return source;
		}

		String uniforms = projectionUniform + "\nuniform mat3 RailwardInverseViewRotation;";
		if (customLightSampler)
			uniforms += "\nuniform sampler2D RailwardLightSampler;";
		String lightSample = customLightSampler
			? "texture(RailwardLightSampler, UV2)"
			: "texelFetch(Sampler2, UV2 / 16, 0)";
		String replacement = (customLightSampler ? lightAnchor + "\n    " : "vertexColor = Color;\n    ")
			+ "railwardLightColor = " + lightSample + ";\n"
			+ "    railwardPosition = RailwardInverseViewRotation * Position;\n"
			+ "    railwardNormal = RailwardInverseViewRotation * Normal;";
		return source.replace(projectionUniform, uniforms)
			.replace(outputAnchor, outputAnchor + "\nout vec4 railwardLightColor;"
				+ "\nout vec3 railwardPosition;\nout vec3 railwardNormal;")
			.replace(lightAnchor, replacement);
	}

	private static String patchImmediateTranslucentFragment(String name, String source) {
		String inputAnchor = "in vec4 normal;";
		String outputAnchor = "out vec4 fragColor;";
		String colorAnchor = "vec4 color = texture(Sampler0, texCoord0) * vertexColor;";
		if (!source.contains(inputAnchor) || !source.contains(outputAnchor) || !source.contains(colorAnchor)) {
			LOGGER.warn("Could not inject Railward spotlight into immediate translucent fragment shader {}", name);
			return source;
		}

		String replacement = colorAnchor + "\n"
			+ "    vec3 surfaceNormal = railwardNormal"
			+ " * inversesqrt(max(dot(railwardNormal, railwardNormal), 1.0e-8));\n"
			+ "    color.rgb *= railwardApplySpotlight(railwardLightColor.rgb, railwardPosition, surfaceNormal);";
		return source.replace(inputAnchor, inputAnchor + "\nin vec4 railwardLightColor;"
			+ "\nin vec3 railwardPosition;\nin vec3 railwardNormal;")
			.replace(outputAnchor, outputAnchor + "\n\n" + LIGHT_UNIFORMS)
			.replace(colorAnchor, replacement);
	}

	private static String patchParticleVertex(String source) {
		String outputAnchor = "out vec4 vertexColor;";
		String lightAnchor = "vertexColor = Color * texelFetch(Sampler2, UV2 / 16, 0);";
		if (!source.contains(outputAnchor) || !source.contains(lightAnchor)) {
			LOGGER.warn("Could not inject Railward spotlight into the vanilla particle vertex shader");
			return source;
		}

		String replacement = "vertexColor = Color;\n"
			+ "    railwardLightColor = texelFetch(Sampler2, UV2 / 16, 0);\n"
			+ "    railwardPosition = Position;";
		return source.replace(outputAnchor,
			outputAnchor + "\nout vec4 railwardLightColor;\nout vec3 railwardPosition;")
			.replace(lightAnchor, replacement);
	}

	private static String patchParticleFragment(String source) {
		String inputAnchor = "in vec4 vertexColor;";
		String outputAnchor = "out vec4 fragColor;";
		String fogAnchor = "fragColor = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);";
		if (!source.contains(inputAnchor) || !source.contains(outputAnchor) || !source.contains(fogAnchor)) {
			LOGGER.warn("Could not inject Railward spotlight into the vanilla particle fragment shader");
			return source;
		}

		String replacement = "color.rgb *= railwardApplySpotlightVolume(railwardLightColor.rgb, railwardPosition);\n"
			+ "    color.a *= railwardLightColor.a;\n    " + fogAnchor;
		return source.replace(inputAnchor,
			inputAnchor + "\nin vec4 railwardLightColor;\nin vec3 railwardPosition;")
			.replace(outputAnchor, outputAnchor + "\n\n" + LIGHT_UNIFORMS)
			.replace(fogAnchor, replacement);
	}

	public static String patchEmbeddium(ResourceLocation location, String source) {
		if (!"sodium".equals(location.getNamespace()))
			return source;
		return switch (location.getPath()) {
		case "blocks/block_layer_opaque.vsh" -> patchEmbeddiumVertex(source);
		case "blocks/block_layer_opaque.fsh" -> patchEmbeddiumFragment(source);
		default -> source;
		};
	}

	public static String patchFlywheelSource(String source) {
		if (source.contains("uniform sampler3D uLightVolume;") && source.contains("uniform mat4 uModel;"))
			return "#define RAILWARD_CONTRAPTION_CONTEXT\n" + source;

		String originalReturn = "return vec4(tex.rgb * FLWLight(r.light).rgb * r.diffuse, tex.a) * r.color;";
		if (!source.contains(originalReturn))
			return source;

		String prefix = "in vec3 v2f_railwardWorldPos;\n"
			+ "in vec3 v2f_railwardNormal;\n\n" + LIGHT_UNIFORMS + "\n";
		String replacement = "vec3 railwardLighting = railwardApplySpotlight(FLWLight(r.light).rgb,\n"
			+ "        v2f_railwardWorldPos, normalize(v2f_railwardNormal));\n"
			+ "    return vec4(tex.rgb * railwardLighting * r.diffuse, tex.a) * r.color;";
		return prefix + source.replace(originalReturn, replacement);
	}

	public static String patchFlywheelVertexFooter(String source) {
		String declarations = "out float v2f_diffuse;";
		String normalized = "v.normal = normalize(v.normal);";
		if (!source.contains(declarations) || !source.contains(normalized)) {
			LOGGER.warn("Could not inject Railward spotlight varyings into a Flywheel vertex template");
			return source;
		}

		return source.replace(declarations, declarations + "\n\t\t\tout vec3 v2f_railwardWorldPos;"
			+ "\n\t\t\tout vec3 v2f_railwardNormal;")
			.replace(normalized, normalized + "\n\t\t\t    #if defined(RAILWARD_CONTRAPTION_CONTEXT)"
				+ "\n\t\t\t    v2f_railwardWorldPos = v.pos;"
				+ "\n\t\t\t    #else"
				+ "\n\t\t\t    v2f_railwardWorldPos = v.pos - uCameraPos;"
				+ "\n\t\t\t    #endif"
				+ "\n\t\t\t    v2f_railwardNormal = v.normal;");
	}

	private static String patchEntityVertex(String name, String source) {
		String outputAnchor = "out vec4 normal;";
		String positionAnchor = "lightMapColor = texelFetch(Sampler2, UV2 / 16, 0);";
		if (!source.contains(outputAnchor) || !source.contains(positionAnchor)) {
			LOGGER.warn("Could not inject Railward spotlight into vanilla entity vertex shader {}", name);
			return source;
		}

		return source.replace(outputAnchor, outputAnchor + "\nout vec3 railwardPosition;\nout vec3 railwardNormal;")
			.replace(positionAnchor, positionAnchor + "\n    railwardPosition = IViewRotMat * Position;"
				+ "\n    railwardNormal = IViewRotMat * Normal;");
	}

	private static String patchEntityFragment(String name, String source) {
		String inputAnchor = "in vec4 normal;";
		String outputAnchor = "out vec4 fragColor;";
		String lightAnchor = "color *= lightMapColor;";
		if (!source.contains(inputAnchor) || !source.contains(outputAnchor) || !source.contains(lightAnchor)) {
			LOGGER.warn("Could not inject Railward spotlight into vanilla entity fragment shader {}", name);
			return source;
		}

		String replacement = "vec3 surfaceNormal = railwardNormal"
			+ " * inversesqrt(max(dot(railwardNormal, railwardNormal), 1.0e-8));\n"
			+ "    color.rgb *= railwardApplySpotlightStrength(lightMapColor.rgb, railwardPosition, surfaceNormal, 1.15);\n"
			+ "    color.a *= lightMapColor.a;";
		return source.replace(inputAnchor, inputAnchor + "\nin vec3 railwardPosition;\nin vec3 railwardNormal;")
			.replace(outputAnchor, outputAnchor + "\n\n" + LIGHT_UNIFORMS)
			.replace(lightAnchor, replacement);
	}

	private static String patchBakedEntityVertex(String name, String source) {
		String outputAnchor = "out vec4 normal;";
		String lightAnchor = "vertexColor = minecraft_mix_light(Light0_Direction, Light1_Direction, Normal, Color)"
			+ " * texelFetch(Sampler2, UV2 / 16, 0);";
		if (!source.contains(outputAnchor) || !source.contains(lightAnchor)) {
			LOGGER.warn("Could not inject Railward spotlight into baked-light entity vertex shader {}", name);
			return source;
		}

		String inverseViewRotation = "IViewRotMat";
		if (!source.contains("uniform mat3 IViewRotMat;")) {
			String projectionUniform = "uniform mat4 ProjMat;";
			if (!source.contains(projectionUniform)) {
				LOGGER.warn("Could not inject Railward inverse view rotation into entity vertex shader {}", name);
				return source;
			}
			source = source.replace(projectionUniform,
				projectionUniform + "\nuniform mat3 RailwardInverseViewRotation;");
			inverseViewRotation = "RailwardInverseViewRotation";
		}

		String replacement = "vertexColor = minecraft_mix_light(Light0_Direction, Light1_Direction, Normal, Color);\n"
			+ "    railwardLightColor = texelFetch(Sampler2, UV2 / 16, 0);\n"
			+ "    railwardPosition = " + inverseViewRotation + " * Position;\n"
			+ "    railwardNormal = " + inverseViewRotation + " * Normal;";
		return source.replace(outputAnchor, outputAnchor + "\nout vec4 railwardLightColor;"
			+ "\nout vec3 railwardPosition;\nout vec3 railwardNormal;")
			.replace(lightAnchor, replacement);
	}

	private static String patchBakedEntityFragment(String name, String source) {
		String inputAnchor = "in vec4 normal;";
		String outputAnchor = "out vec4 fragColor;";
		String fogAnchor = "fragColor = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);";
		if (!source.contains(inputAnchor) || !source.contains(outputAnchor) || !source.contains(fogAnchor)) {
			LOGGER.warn("Could not inject Railward spotlight into baked-light entity fragment shader {}", name);
			return source;
		}

		String replacement = "vec3 surfaceNormal = railwardNormal"
			+ " * inversesqrt(max(dot(railwardNormal, railwardNormal), 1.0e-8));\n"
			+ "    color.rgb *= railwardApplySpotlightStrength(railwardLightColor.rgb, railwardPosition, surfaceNormal, 1.15);\n"
			+ "    color.a *= railwardLightColor.a;\n    " + fogAnchor;
		return source.replace(inputAnchor, inputAnchor + "\nin vec4 railwardLightColor;"
			+ "\nin vec3 railwardPosition;\nin vec3 railwardNormal;")
			.replace(outputAnchor, outputAnchor + "\n\n" + LIGHT_UNIFORMS)
			.replace(fogAnchor, replacement);
	}

	private static String patchVanillaVertex(String name, String source) {
		String outputAnchor = "out vec4 normal;";
		String lightingAnchor = "vertexColor = Color * minecraft_sample_lightmap(Sampler2, UV2);";
		if (!source.contains(outputAnchor) || !source.contains(lightingAnchor)) {
			LOGGER.warn("Could not inject Railward spotlight into vanilla vertex shader {}", name);
			return source;
		}

		return source.replace(outputAnchor, outputAnchor + "\nuniform int RailwardContraptionPass;"
			+ "\nuniform mat3 RailwardInverseViewRotation;\nout vec4 railwardLightColor;"
			+ "\nout vec3 railwardPosition;\nout vec3 railwardNormal;")
			.replace(lightingAnchor, "vertexColor = Color;\n"
				+ "    railwardLightColor = minecraft_sample_lightmap(Sampler2, UV2);\n"
				+ "    if (RailwardContraptionPass != 0) {\n"
				+ "        railwardPosition = RailwardInverseViewRotation * pos;\n"
				+ "        railwardNormal = RailwardInverseViewRotation * Normal;\n"
				+ "    } else {\n"
				+ "        railwardPosition = pos;\n"
				+ "        railwardNormal = Normal;\n"
				+ "    }");
	}

	private static String patchVanillaFragment(String name, String source) {
		String inputAnchor = "in vec4 normal;";
		String outputAnchor = "out vec4 fragColor;";
		String colorAnchor = "vec4 color = texture(Sampler0, texCoord0) * vertexColor * ColorModulator;";
		if (!source.contains(inputAnchor) || !source.contains(outputAnchor) || !source.contains(colorAnchor)) {
			LOGGER.warn("Could not inject Railward spotlight into vanilla fragment shader {}", name);
			return source;
		}

		boolean twoSided = VANILLA_TWO_SIDED_TERRAIN_PROGRAMS.contains(name);
		String normalCalculation;
		String applyFunction;
		if (twoSided) {
			normalCalculation = "vec3 normalGradient = cross(dFdx(railwardPosition), dFdy(railwardPosition));\n"
				+ "    vec3 surfaceNormal = normalGradient"
				+ " * inversesqrt(max(dot(normalGradient, normalGradient), 1.0e-8));\n"
				+ "    if (dot(surfaceNormal, -railwardPosition) < 0.0) {\n"
				+ "        surfaceNormal = -surfaceNormal;\n"
				+ "    }";
			applyFunction = "railwardApplySpotlightTwoSided";
		} else {
			normalCalculation = "vec3 surfaceNormal = railwardNormal"
				+ " * inversesqrt(max(dot(railwardNormal, railwardNormal), 1.0e-8));";
			applyFunction = "railwardApplySpotlight";
		}
		String lightingCalculation = "vec3 railwardLighting = " + applyFunction
			+ "(railwardLightColor.rgb, railwardPosition, surfaceNormal);";
		String colorReplacement = colorAnchor + "\n    " + normalCalculation + "\n    "
			+ lightingCalculation + "\n    color.rgb *= railwardLighting;";
		return source.replace(inputAnchor, inputAnchor + "\nin vec4 railwardLightColor;"
			+ "\nin vec3 railwardPosition;\nin vec3 railwardNormal;")
			.replace(outputAnchor, outputAnchor + "\n\n" + LIGHT_UNIFORMS)
			.replace(colorAnchor, colorReplacement);
	}

	private static String patchEmbeddiumVertex(String source) {
		String outputAnchor = "out vec2 v_LightCoord;";
		String positionAnchor = "vec3 position = u_RegionOffset + _draw_translation + _vert_position;";
		if (!source.contains(outputAnchor) || !source.contains(positionAnchor)) {
			LOGGER.warn("Could not inject Railward spotlight into the Embeddium vertex shader");
			return source;
		}
		return source.replace(outputAnchor, outputAnchor + "\nout vec3 v_RailwardPosition;")
			.replace(positionAnchor, positionAnchor + "\n    v_RailwardPosition = position;");
	}

	private static String patchEmbeddiumFragment(String source) {
		String inputAnchor = "in vec2 v_LightCoord; // The interpolated light map texture coordinates";
		String outputAnchor = "out vec4 fragColor; // The output fragment for the color framebuffer";
		String mainAnchor = "void main() {";
		String diffuseAnchor = "vec4 diffuseColor = (sampleBlockTex * sampleLightTex);\n    diffuseColor *= v_Color;";
		if (!source.contains(inputAnchor) || !source.contains(outputAnchor) || !source.contains(mainAnchor)
			|| !source.contains(diffuseAnchor)) {
			LOGGER.warn("Could not inject Railward spotlight into the Embeddium fragment shader");
			return source;
		}

		String normalSetup = "vec3 normalGradient = cross(dFdx(v_RailwardPosition), dFdy(v_RailwardPosition));\n"
			+ "    vec3 surfaceNormal = normalGradient * inversesqrt(max(dot(normalGradient, normalGradient), 1.0e-8));\n"
			+ "    if (dot(surfaceNormal, -v_RailwardPosition) < 0.0) {\n"
			+ "        surfaceNormal = -surfaceNormal;\n"
			+ "    }";
		String replacement = "#ifdef ALPHA_CUTOFF\n"
			+ "    vec3 railwardLighting = railwardApplySpotlightTwoSided(sampleLightTex.rgb, v_RailwardPosition, surfaceNormal);\n"
			+ "#else\n"
			+ "    vec3 railwardLighting = railwardApplySpotlight(sampleLightTex.rgb, v_RailwardPosition, surfaceNormal);\n"
			+ "#endif\n"
			+ "    vec4 diffuseColor = sampleBlockTex * v_Color;\n"
			+ "    diffuseColor.rgb *= railwardLighting;\n"
			+ "    diffuseColor.a *= sampleLightTex.a;";
		return source.replace(inputAnchor, inputAnchor + "\nin vec3 v_RailwardPosition;")
			.replace(outputAnchor, outputAnchor + "\n\n" + LIGHT_UNIFORMS)
			.replace(mainAnchor, mainAnchor + "\n    " + normalSetup)
			.replace(diffuseAnchor, replacement);
	}
}
