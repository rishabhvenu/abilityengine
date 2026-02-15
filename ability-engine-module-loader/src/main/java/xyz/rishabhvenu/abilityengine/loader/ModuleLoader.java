package xyz.rishabhvenu.abilityengine.loader;

import org.bukkit.plugin.Plugin;
import xyz.rishabhvenu.abilityengine.api.*;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.logging.Logger;

/**
 * Loads external JAR modules that provide abilities.
 * Uses ServiceLoader SPI to discover AbilityProvider/AbilityModule implementations.
 */
public final class ModuleLoader {
    
    private final Plugin plugin;
    private final Logger logger;
    private final AbilityRegistry registry;
    private final CooldownManager cooldownManager;
    private final AbilityItemService itemService;
    
    private final List<LoadedModule> loadedModules = new ArrayList<>();
    
    public ModuleLoader(
            Plugin plugin,
            AbilityRegistry registry,
            CooldownManager cooldownManager,
            AbilityItemService itemService) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.registry = registry;
        this.cooldownManager = cooldownManager;
        this.itemService = itemService;
    }
    
    /**
     * Loads all modules from a directory.
     * 
     * @param directory The directory containing .jar files
     * @return Number of modules loaded
     */
    public int loadAllModules(File directory) {
        if (!directory.exists()) {
            directory.mkdirs();
            logger.info("Created modules directory: " + directory.getPath());
            return 0;
        }
        
        if (!directory.isDirectory()) {
            logger.warning("Modules path is not a directory: " + directory.getPath());
            return 0;
        }
        
        int loaded = 0;
        File[] files = directory.listFiles((dir, name) -> name.endsWith(".jar"));
        
        if (files == null || files.length == 0) {
            logger.info("No module JARs found in " + directory.getPath());
            return 0;
        }
        
        for (File file : files) {
            try {
                loadModule(file);
                loaded++;
            } catch (Exception e) {
                logger.severe("Failed to load module " + file.getName() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        logger.info("Loaded " + loaded + " external module(s)");
        return loaded;
    }
    
    /**
     * Loads a single module JAR.
     * 
     * @param jarFile The JAR file
     */
    private void loadModule(File jarFile) throws Exception {
        logger.info("Loading module: " + jarFile.getName());
        
        // Create a URLClassLoader for this module
        URL jarUrl = jarFile.toURI().toURL();
        URLClassLoader classLoader = new URLClassLoader(
            new URL[]{jarUrl},
            plugin.getClass().getClassLoader()
        );
        
        // Use ServiceLoader to discover AbilityProvider implementations
        ServiceLoader<AbilityProvider> serviceLoader = ServiceLoader.load(
            AbilityProvider.class,
            classLoader
        );
        
        for (AbilityProvider provider : serviceLoader) {
            logger.info("Found provider: " + provider.getProviderId());
            
            // Check if this is an AbilityModule with lifecycle hooks
            if (provider instanceof AbilityModule module) {
                logger.info("Module has lifecycle hooks: " + module.getModuleName() + " v" + module.getModuleVersion());
                
                // Call onEnable
                try {
                    module.onEnable(registry, cooldownManager, itemService);
                } catch (Exception e) {
                    logger.severe("Error calling onEnable for module " + module.getProviderId() + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }
            
            // Get and register abilities
            Collection<Ability> abilities = provider.getAbilities();
            List<String> abilityIds = new ArrayList<>();
            
            for (Ability ability : abilities) {
                registry.register(ability);
                abilityIds.add(ability.id());
                logger.info("  - Registered ability: " + ability.id());
            }
            
            // Track this module
            LoadedModule loadedModule = new LoadedModule(
                provider.getProviderId(),
                provider,
                classLoader,
                abilityIds
            );
            loadedModules.add(loadedModule);
            
            logger.info("Loaded module: " + provider.getProviderId() + 
                " (" + abilityIds.size() + " abilities)");
        }
    }
    
    /**
     * Unloads all modules.
     */
    public void unloadAllModules() {
        logger.info("Unloading " + loadedModules.size() + " module(s)");
        
        for (LoadedModule module : loadedModules) {
            unloadModule(module);
        }
        
        loadedModules.clear();
    }
    
    /**
     * Unloads a specific module.
     * 
     * @param module The module to unload
     */
    private void unloadModule(LoadedModule module) {
        // Unregister all abilities
        for (String abilityId : module.abilityIds()) {
            registry.unregister(abilityId);
        }
        
        // Call onDisable if this is an AbilityModule
        if (module.provider() instanceof AbilityModule abilityModule) {
            try {
                abilityModule.onDisable();
            } catch (Exception e) {
                logger.severe("Error calling onDisable for module " + module.providerId() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        // Close the classloader
        try {
            module.classLoader().close();
        } catch (Exception e) {
            logger.warning("Error closing classloader for module " + module.providerId() + ": " + e.getMessage());
        }
        
        logger.info("Unloaded module: " + module.providerId());
    }
    
    /**
     * Gets all loaded modules.
     * 
     * @return Immutable list of loaded modules
     */
    public List<LoadedModule> getLoadedModules() {
        return Collections.unmodifiableList(loadedModules);
    }
}
