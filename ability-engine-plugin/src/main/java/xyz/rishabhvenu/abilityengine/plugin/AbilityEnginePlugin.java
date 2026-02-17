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
    private AbilityStateStore stateStore;
    private BossBarManager bossBarManager;
    private ConfigAbilityLoader configLoader;
    private ModuleLoader moduleLoader;
    private ScriptEngine scriptEngine;
    private ExecutionTracker executionTracker;
    private EntityControlManager entityControlManager;
    private InterruptManager interruptManager;
    
    @Override
    public void onEnable() {
        getLogger().info("AbilityEngine is starting...");
        
        // Initialize core components
        registry = new AbilityRegistryImpl();
        cooldownManager = new CooldownManagerImpl();
        itemService = new AbilityItemServiceImpl(this, registry);
        sessionManager = new SessionManager(this);
        stateStore = new AbilityStateStore();
        bossBarManager = new BossBarManager();
        executionTracker = new ExecutionTracker();
        entityControlManager = new EntityControlManager(this);
        interruptManager = new InterruptManager(executionTracker);
        triggerDispatcher = new TriggerDispatcher(this, registry, itemService, cooldownManager);
        eventTriggerRegistry = new EventTriggerRegistry(this);
        configLoader = new ConfigAbilityLoader(getLogger(), registry);
        
        // Start session manager
        sessionManager.start();
        
        // Start entity control manager
        entityControlManager.start();
        
        // Register event listeners
        Bukkit.getPluginManager().registerEvents(triggerDispatcher, this);
        Bukkit.getPluginManager().registerEvents(eventTriggerRegistry, this);
        Bukkit.getPluginManager().registerEvents(sessionManager, this);
        Bukkit.getPluginManager().registerEvents(entityControlManager, this);
        Bukkit.getPluginManager().registerEvents(interruptManager, this);
        
        // Register cleanup listener for player quit
        Bukkit.getPluginManager().registerEvents(new org.bukkit.event.Listener() {
            @org.bukkit.event.EventHandler
            public void onPlayerQuit(org.bukkit.event.player.PlayerQuitEvent event) {
                java.util.UUID playerId = event.getPlayer().getUniqueId();
                // Clear player state
                stateStore.clearPlayer(playerId);
                // Clear cooldowns
                cooldownManager.clearCooldowns(playerId);
                // Clear boss bars
                bossBarManager.removeAllBars(playerId);
            }
        }, this);
        
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
        scriptEngine = new ScriptEngine(this, registry, cooldownManager, itemService, sessionManager, eventTriggerRegistry, stateStore, bossBarManager, executionTracker, entityControlManager, interruptManager);
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
        
        // Stop entity control manager
        if (entityControlManager != null) {
            entityControlManager.stop();
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
    
    public AbilityStateStore getStateStore() {
        return stateStore;
    }
}
