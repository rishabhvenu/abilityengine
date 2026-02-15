package xyz.rishabhvenu.abilityengine.script;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Source;
import xyz.rishabhvenu.abilityengine.api.AbilityItemService;
import xyz.rishabhvenu.abilityengine.api.AbilityRegistry;
import xyz.rishabhvenu.abilityengine.api.CooldownManager;
import xyz.rishabhvenu.abilityengine.core.EventTriggerRegistry;
import xyz.rishabhvenu.abilityengine.core.SessionManager;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Central orchestrator for the JavaScript scripting system.
 * Manages script loading, unloading, reloading with proper resource cleanup.
 */
public final class ScriptEngine {
    
    private final Plugin plugin;
    private final Logger logger;
    private final AbilityRegistry registry;
    private final CooldownManager cooldownManager;
    private final AbilityItemService itemService;
    private final SessionManager sessionManager;
    private final EventTriggerRegistry eventTriggerRegistry;
    
    private Engine graalEngine;
    private final Map<String, ScriptContext> loadedScripts = new HashMap<>();
    
    public ScriptEngine(
            Plugin plugin,
            AbilityRegistry registry,
            CooldownManager cooldownManager,
            AbilityItemService itemService,
            SessionManager sessionManager,
            EventTriggerRegistry eventTriggerRegistry) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.registry = registry;
        this.cooldownManager = cooldownManager;
        this.itemService = itemService;
        this.sessionManager = sessionManager;
        this.eventTriggerRegistry = eventTriggerRegistry;
    }
    
    /**
     * Loads all scripts from a directory.
     * 
     * @param directory The directory containing .js files
     * @return Number of scripts loaded
     */
    public int loadAllScripts(File directory) {
        if (!directory.exists()) {
            directory.mkdirs();
            logger.info("Created scripts directory: " + directory.getPath());
            return 0;
        }
        
        if (!directory.isDirectory()) {
            logger.warning("Scripts path is not a directory: " + directory.getPath());
            return 0;
        }
        
        // Initialize GraalVM engine if needed
        if (graalEngine == null) {
            graalEngine = Engine.newBuilder()
                .option("engine.WarnInterpreterOnly", "false")
                .build();
        }
        
        int loaded = 0;
        File[] files = directory.listFiles((dir, name) -> name.endsWith(".js"));
        
        if (files == null || files.length == 0) {
            logger.info("No script files found in " + directory.getPath());
            return 0;
        }
        
        for (File file : files) {
            try {
                loadScript(file);
                loaded++;
            } catch (Exception e) {
                logger.severe("Failed to load script " + file.getName() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        logger.info("Loaded " + loaded + " script(s)");
        return loaded;
    }
    
    /**
     * Loads a single script file.
     * 
     * @param file The script file
     */
    public void loadScript(File file) throws IOException {
        String scriptName = file.getName();
        
        // Unload existing if already loaded
        if (loadedScripts.containsKey(scriptName)) {
            unloadScript(scriptName);
        }
        
        // Create isolated context for this script
        Context context = Context.newBuilder("js")
            .engine(graalEngine)
            .allowHostAccess(HostAccess.ALL)
            .allowHostClassLookup(className -> true)
            .allowIO(true)
            .build();
        
        ScriptContext scriptContext = new ScriptContext(scriptName, context);
        
        // Create and bind the engine global object
        EngineBinding engineBinding = new EngineBinding(
            plugin,
            registry,
            cooldownManager,
            itemService,
            sessionManager,
            eventTriggerRegistry,
            scriptContext
        );
        
        context.getBindings("js").putMember("engine", engineBinding);
        
        // Load and execute the script
        Source source = Source.newBuilder("js", file).build();
        context.eval(source);
        
        // Track this script
        loadedScripts.put(scriptName, scriptContext);
        
        logger.info("Loaded script: " + scriptName + " (registered " + 
            scriptContext.getAbilityIds().size() + " abilities)");
    }
    
    /**
     * Unloads a script and cleans up all its resources.
     * 
     * @param scriptName The script filename
     */
    public void unloadScript(String scriptName) {
        ScriptContext scriptContext = loadedScripts.remove(scriptName);
        if (scriptContext == null) {
            return;
        }
        
        // Unregister all abilities
        for (String abilityId : scriptContext.getAbilityIds()) {
            registry.unregister(abilityId);
        }
        
        // Cancel all scheduled tasks
        BukkitScheduler scheduler = Bukkit.getScheduler();
        for (Integer taskId : scriptContext.getScheduledTasks()) {
            scheduler.cancelTask(taskId);
        }
        
        // Close the script context (unregisters listeners, closes GraalVM context)
        scriptContext.close();
        
        logger.info("Unloaded script: " + scriptName);
    }
    
    /**
     * Reloads a specific script.
     * 
     * @param scriptName The script filename
     * @param scriptsDir The scripts directory
     */
    public void reloadScript(String scriptName, File scriptsDir) throws IOException {
        unloadScript(scriptName);
        
        File scriptFile = new File(scriptsDir, scriptName);
        if (!scriptFile.exists()) {
            throw new IOException("Script file not found: " + scriptName);
        }
        
        loadScript(scriptFile);
    }
    
    /**
     * Reloads all scripts.
     * 
     * @param scriptsDir The scripts directory
     */
    public void reloadAll(File scriptsDir) {
        // Unload all
        for (String scriptName : new ArrayList<>(loadedScripts.keySet())) {
            unloadScript(scriptName);
        }
        
        // Load all
        loadAllScripts(scriptsDir);
    }
    
    /**
     * Shuts down the script engine and closes all contexts.
     */
    public void shutdown() {
        // Unload all scripts
        for (String scriptName : new ArrayList<>(loadedScripts.keySet())) {
            unloadScript(scriptName);
        }
        
        // Close the shared GraalVM engine
        if (graalEngine != null) {
            graalEngine.close();
            graalEngine = null;
        }
        
        logger.info("Script engine shutdown complete");
    }
    
    /**
     * Gets all loaded script names.
     * 
     * @return Map of script name to ScriptContext
     */
    public Map<String, ScriptContext> getLoadedScripts() {
        return new HashMap<>(loadedScripts);
    }
}
