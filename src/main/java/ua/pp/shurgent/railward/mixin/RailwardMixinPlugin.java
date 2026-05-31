package ua.pp.shurgent.railward.mixin;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.objectweb.asm.tree.ClassNode;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import com.mojang.logging.LogUtils;

public class RailwardMixinPlugin implements IMixinConfigPlugin {

	private static final Logger LOGGER = LogUtils.getLogger();
	private static final String CONFIG_FILE_NAME = "railward-mixins.properties";
	private static final String FIX_CREATE_FLUID_NETWORK = "fixCreateFluidNetwork";
	private static final String FLUID_NETWORK_MIXIN = ".create.FluidNetworkMixin";
	private static final String PIPE_CONNECTION_ACCESSOR = ".create.PipeConnectionAccessor";
	private static final boolean DEFAULT_FIX_CREATE_FLUID_NETWORK = true;

	private boolean fixCreateFluidNetwork = DEFAULT_FIX_CREATE_FLUID_NETWORK;

	@Override
	public void onLoad(String mixinPackage) {
		loadConfig();
	}

	@Override
	public String getRefMapperConfig() {
		return null;
	}

	@Override
	public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
		if (mixinClassName.endsWith(FLUID_NETWORK_MIXIN) || mixinClassName.endsWith(PIPE_CONNECTION_ACCESSOR))
			return fixCreateFluidNetwork;
		return true;
	}

	@Override
	public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
	}

	@Override
	public List<String> getMixins() {
		return null;
	}

	@Override
	public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
	}

	@Override
	public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
	}

	private void loadConfig() {
		Path configPath = Path.of("config", CONFIG_FILE_NAME);
		try {
			createDefaultConfig(configPath);

			Properties properties = new Properties();
			try (InputStream input = Files.newInputStream(configPath)) {
				properties.load(input);
			}

			fixCreateFluidNetwork = readBoolean(properties, FIX_CREATE_FLUID_NETWORK,
				DEFAULT_FIX_CREATE_FLUID_NETWORK);
		} catch (IOException e) {
			fixCreateFluidNetwork = DEFAULT_FIX_CREATE_FLUID_NETWORK;
			LOGGER.warn("Failed to read {}. Using default {}={}.", configPath, FIX_CREATE_FLUID_NETWORK,
				fixCreateFluidNetwork, e);
		}
	}

	private void createDefaultConfig(Path configPath) throws IOException {
		if (Files.exists(configPath))
			return;

		Path parent = configPath.getParent();
		if (parent != null)
			Files.createDirectories(parent);

		String content = """
			# Create: Railward early mixin switches.
			# This file is read before normal Forge configs, while mixins are being selected.
			#
			# fixCreateFluidNetwork
			# Fixes a Create 0.5.1 fluid network bug where pulling fluid from a contraption
			# or train with multiple mounted fluid tanks can drain several source tanks in one
			# tick, but transfer only the last drained portion, deleting the rest.
			# Keep this true unless another mod also modifies Create's FluidNetwork.tick and
			# the game crashes or reports a mixin conflict. Set to false to disable only
			# Railward's fluid network fix.
			fixCreateFluidNetwork=true
			""";
		Files.writeString(configPath, content, StandardCharsets.UTF_8);
	}

	private boolean readBoolean(Properties properties, String key, boolean fallback) {
		String value = properties.getProperty(key);
		if (value == null)
			return fallback;
		value = value.trim();
		if ("true".equalsIgnoreCase(value))
			return true;
		if ("false".equalsIgnoreCase(value))
			return false;

		LOGGER.warn("Invalid boolean value '{}' for {} in {}. Using default {}.", value, key, CONFIG_FILE_NAME,
			fallback);
		return fallback;
	}

}
