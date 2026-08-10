package net.vulkanmod;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.fml.ModContainer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.vulkanmod.config.Config;
import net.vulkanmod.config.Platform;
import net.vulkanmod.config.UpdateChecker;
import net.vulkanmod.config.ui.shell.VolcanicScreen;
import net.vulkanmod.config.video.VideoModeManager;
import net.vulkanmod.compat.CompatBootstrap;
import net.vulkanmod.compat.CompatReport;
import net.vulkanmod.compat.RuntimeOptions;
import net.vulkanmod.render.cit.CitModelRegistrar;
import net.vulkanmod.render.fusion.FusionModelLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.util.function.Supplier;

@Mod("vulkanmod")
public class Initializer {
	public static final Logger LOGGER = LogManager.getLogger("Volcanic");
	public static final String MOD_ID = "vulkanmod";

	private static String VERSION = "0.4.9-dev";
	public static Config CONFIG;
	public static boolean firstRun;

	static {

		try {
			Platform.init();
			VideoModeManager.init();
			Path configPath = Path.of("config", "vulkanmod_settings.json");
			CONFIG = loadConfig(configPath);
		} catch (Exception e) {
			LOGGER.error("Volcanic failed to start from its saved state, running on defaults", e);
			CONFIG = new Config();
		} finally {
			// set MoltenVK config before its dylib loads
			net.vulkanmod.vulkan.MoltenVKConfig.apply();
		}
	}

	public Initializer(IEventBus modEventBus, ModContainer modContainer) {
		VERSION = modContainer.getModInfo().getVersion().toString();
		modEventBus.addListener(this::onInitializeClient);
		net.vulkanmod.sound.UiSounds.register(modEventBus);
		modEventBus.addListener(CitModelRegistrar::onRegisterAdditional);
		modEventBus.addListener((ModelEvent.RegisterGeometryLoaders event) ->
				event.register(ResourceLocation.fromNamespaceAndPath("fusion", "model"),
						FusionModelLoader.INSTANCE));
		modContainer.registerExtensionPoint(IConfigScreenFactory.class,
				(Supplier<IConfigScreenFactory>) () ->
						(container, parent) -> new VolcanicScreen(Component.translatable("vulkanmod.options.title"), parent));
	}

	private void onInitializeClient(FMLClientSetupEvent event) {
		LOGGER.info("== Volcanic ==");
		if (firstRun) {
			net.vulkanmod.config.PerformancePresetApplier.apply(
					net.vulkanmod.config.PerformancePreset.QUALITY, CONFIG, net.minecraft.client.Minecraft.getInstance());
			CONFIG.write();
			LOGGER.info("First run: started on the Quality preset");
		}
		UpdateChecker.checkForUpdates();
		CompatBootstrap.init();
		if (RuntimeOptions.diagnosticsEnabled()) {
			CompatReport.logReport();
		}
		CompatReport.logRuntimeHints();
	}

	private static Config loadConfig(Path path) {
		Config config = Config.load(path);

		if(config == null) {
			config = new Config();
			config.performancePreset = net.vulkanmod.config.PerformancePreset.QUALITY.id;
			config.write();
			firstRun = true;
		}

		return config;
	}

	public static String getVersion() {
		return VERSION;
	}
}
