package xyz.rishabhvenu.abilityengine.plugin;

import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import xyz.rishabhvenu.abilityengine.api.AbilityItemService;
import xyz.rishabhvenu.abilityengine.api.AbilityRegistry;
import xyz.rishabhvenu.abilityengine.api.CooldownManager;
import xyz.rishabhvenu.abilityengine.config.ConfigAbilityLoader;
import xyz.rishabhvenu.abilityengine.core.*;
import xyz.rishabhvenu.abilityengine.loader.ModuleLoader;
import xyz.rishabhvenu.abilityengine.plugin.commands.AbilityCommand;
import xyz.rishabhvenu.abilityengine.script.ScriptEngine;

import java.io.File;

/**
 * Main plugin class for AbilityEngine.
 */
public final class AbilityEnginePlugin extends JavaPlugin {
    
    private AbilityRegistry registry;
    private CooldownManager cooldownManager;
    private AbilityItemService itemService;
    private SessionManager sessionManager;
    private TriggerDispatcher triggerDispatcher;
    private EventTriggerRegistry eventTriggerRegistry;
    private ConfigAbilityLoader configLoader;
    private ModuleLoader moduleLoader;
    private ScriptEngine scriptEngine;
    
    @Override
    public void onEnable() {
        getLogger().info("AbilityEngine is starting...");
        
        // Initialize core components
        registry = new AbilityRegistryImpl();
        cooldownManager = new CooldownManagerImpl();
        itemService = new AbilityItemServiceImpl(this, registry);
        sessionManager = new SessionManager(this);
        triggerDispatcher = new TriggerDispatcher(this, registry, itemService, cooldownManager);
        eventTriggerRegistry = new EventTriggerRegistry(this);
        configLoader = new ConfigAbilityLoader(getLogger(), registry);
        
        // Start session manager
        sessionManager.start();
        
        // Register event listeners
        Bukkit.getPluginManager().registerEvents(triggerDispatcher, this);
        Bukkit.getPluginManager().registerEvents(eventTriggerRegistry, this);
        
        // Load config abilities
        File abilitiesDir = new File(getDataFolder(), "abilities");
        int loaded = configLoader.loadAbilities(abilitiesDir);
        getLogger().info("Loaded " + loaded + " abilities from configuration");
        
        // Load external modules
        moduleLoader = new ModuleLoader(this, registry, cooldownManager, itemService);
        File modulesDir = new File(getDataFolder(), "modules");
        int modulesLoaded = moduleLoader.loadAllModules(modulesDir);
        getLogger().info("Loaded " + modulesLoaded + " external module(s)");
        
        // Load scripts
        scriptEngine = new ScriptEngine(this, registry, cooldownManager, itemService, sessionManager, eventTriggerRegistry);
        File scriptsDir = new File(getDataFolder(), "scripts");
        int scriptsLoaded = scriptEngine.loadAllScripts(scriptsDir);
        getLogger().info("Loaded " + scriptsLoaded + " script(s)");
        
        // Register Brigadier commands
        LifecycleEventManager<Plugin> manager = this.getLifecycleManager();
        manager.registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            AbilityCommand abilityCommand = new AbilityCommand(this, registry, itemService, configLoader, scriptEngine, moduleLoader);
            Commands commands = event.registrar();
            commands.register(abilityCommand.createCommand().build(), "AbilityEngine commands");
        });
        
        getLogger().info("AbilityEngine enabled successfully!");
    }
    
    @Override
    public void onDisable() {
        getLogger().info("AbilityEngine is shutting down...");
        
        // Shutdown script engine
        if (scriptEngine != null) {
            scriptEngine.shutdown();
        }
        
        // Unload external modules
        if (moduleLoader != null) {
            moduleLoader.unloadAllModules();
        }
        
        // Stop session manager (cleans up all active sessions)
        if (sessionManager != null) {
            sessionManager.stop();
        }
        
        getLogger().info("AbilityEngine disabled successfully!");
    }
    
    // Public API accessors for external modules (Phase 2)
    
    public AbilityRegistry getRegistry() {
        return registry;
    }
    
    public CooldownManager getCooldownManager() {
        return cooldownManager;
    }
    
    public AbilityItemService getItemService() {
        return itemService;
    }
    
    public SessionManager getSessionManager() {
        return sessionManager;
    }
    
    public EventTriggerRegistry getEventTriggerRegistry() {
        return eventTriggerRegistry;
    }
    
    public ScriptEngine getScriptEngine() {
        return scriptEngine;
    }
    
    public ModuleLoader getModuleLoader() {
        return moduleLoader;
    }
}
