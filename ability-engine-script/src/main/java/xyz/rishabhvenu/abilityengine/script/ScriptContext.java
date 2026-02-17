package xyz.rishabhvenu.abilityengine.script;

import org.bukkit.boss.BossBar;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.RegisteredListener;
import org.graalvm.polyglot.Context;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Tracks all resources registered by a single script.
 * Used for clean unload/reload.
 */
public final class ScriptContext {
    
    private final String scriptName;
    private final Context graalContext;
    private final List<String> abilityIds = new ArrayList<>();
    private final List<RegisteredListener> eventListeners = new ArrayList<>();
    private final List<Integer> scheduledTasks = new ArrayList<>();
    
    // Ability-scoped resource tracking
    private final Map<String, List<Integer>> abilityTasks = new HashMap<>();
    private final Map<String, List<BossBar>> abilityBossBars = new HashMap<>();
    
    // Item templates created via engine.item()
    private final Map<String, org.bukkit.inventory.ItemStack> itemTemplates = new HashMap<>();
    
    ScriptContext(String scriptName, Context graalContext) {
        this.scriptName = scriptName;
        this.graalContext = graalContext;
    }
    
    String getScriptName() {
        return scriptName;
    }
    
    Context getGraalContext() {
        return graalContext;
    }
    
    void trackAbility(String abilityId) {
        abilityIds.add(abilityId);
    }
    
    void trackEventListener(RegisteredListener listener) {
        eventListeners.add(listener);
    }
    
    void trackScheduledTask(int taskId) {
        scheduledTasks.add(taskId);
    }
    
    /**
     * Tracks a task owned by a specific ability.
     */
    void trackAbilityTask(String abilityId, int taskId) {
        abilityTasks.computeIfAbsent(abilityId, k -> new ArrayList<>()).add(taskId);
        trackScheduledTask(taskId); // Also track globally for script cleanup
    }
    
    /**
     * Tracks a boss bar owned by a specific ability.
     */
    void trackAbilityBossBar(String abilityId, BossBar bar) {
        abilityBossBars.computeIfAbsent(abilityId, k -> new ArrayList<>()).add(bar);
    }
    
    /**
     * Registers an item template for later retrieval.
     */
    void registerItemTemplate(String itemId, org.bukkit.inventory.ItemStack template) {
        itemTemplates.put(itemId, template);
    }
    
    /**
     * Gets an item template by ID.
     */
    org.bukkit.inventory.ItemStack getItemTemplate(String itemId) {
        return itemTemplates.get(itemId);
    }
    
    /**
     * Cleans up all resources for a specific ability.
     */
    void cleanupAbility(String abilityId) {
        // Cancel all tasks
        List<Integer> tasks = abilityTasks.remove(abilityId);
        if (tasks != null) {
            for (Integer taskId : tasks) {
                org.bukkit.Bukkit.getScheduler().cancelTask(taskId);
                scheduledTasks.remove(taskId);
            }
        }
        
        // Remove boss bars
        List<BossBar> bars = abilityBossBars.remove(abilityId);
        if (bars != null) {
            for (BossBar bar : bars) {
                bar.removeAll();
            }
        }
    }
    
    /**
     * Removes a player from all boss bars.
     */
    void cleanupPlayer(UUID playerId) {
        for (List<BossBar> bars : abilityBossBars.values()) {
            for (BossBar bar : bars) {
                bar.getPlayers().stream()
                    .filter(p -> p.getUniqueId().equals(playerId))
                    .forEach(bar::removePlayer);
            }
        }
    }
    
    public List<String> getAbilityIds() {
        return new ArrayList<>(abilityIds);
    }
    
    List<RegisteredListener> getEventListeners() {
        return new ArrayList<>(eventListeners);
    }
    
    List<Integer> getScheduledTasks() {
        return new ArrayList<>(scheduledTasks);
    }
    
    void close() {
        // Unregister all event listeners
        for (RegisteredListener listener : eventListeners) {
            HandlerList.unregisterAll(listener.getListener());
        }
        eventListeners.clear();
        
        // Clear tracking (tasks are cancelled by ScriptEngine)
        abilityIds.clear();
        scheduledTasks.clear();
        abilityTasks.clear();
        
        // Remove all boss bars
        for (List<BossBar> bars : abilityBossBars.values()) {
            for (BossBar bar : bars) {
                bar.removeAll();
            }
        }
        abilityBossBars.clear();
        
        // Close the GraalVM context
        if (graalContext != null) {
            try {
                graalContext.close();
            } catch (Exception e) {
                // Context or engine may already be closed
            }
        }
    }
}
