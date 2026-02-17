package xyz.rishabhvenu.abilityengine.script;

import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.graalvm.polyglot.Value;
import xyz.rishabhvenu.abilityengine.api.AbilityContext;
import xyz.rishabhvenu.abilityengine.api.TriggerType;
import xyz.rishabhvenu.abilityengine.core.AbilityStateStore;

/**
 * Enhanced execution context passed to JavaScript ability callbacks.
 * Wraps AbilityContext with additional scripting conveniences:
 * - Ability-scoped state management
 * - Task ownership tracking
 * - Automatic cleanup
 */
public final class AbilityExecContext {
    
    private final AbilityContext raw;
    private final String abilityId;
    private final ScriptContext scriptContext;
    private final AbilityStateStore stateStore;
    private final Plugin plugin;
    
    // Ability-scoped state API
    public final AbilityScopedState state;
    
    public AbilityExecContext(
            AbilityContext raw,
            String abilityId,
            ScriptContext scriptContext,
            AbilityStateStore stateStore,
            Plugin plugin) {
        this.raw = raw;
        this.abilityId = abilityId;
        this.scriptContext = scriptContext;
        this.stateStore = stateStore;
        this.plugin = plugin;
        this.state = new AbilityScopedState(abilityId, stateStore);
    }
    
    // Delegate to raw AbilityContext
    public Player player() {
        return raw.player();
    }
    
    public TriggerType trigger() {
        return raw.trigger();
    }
    
    public Entity targetEntity() {
        return raw.targetEntity();
    }
    
    public Block targetBlock() {
        return raw.targetBlock();
    }
    
    public ItemStack item() {
        return raw.item();
    }
    
    public Event event() {
        return raw.event();
    }
    
    /**
     * Schedules a repeating task owned by this ability.
     * Automatically tracked and cleaned up on ability unload.
     * 
     * @param func JS function to execute
     * @param delayTicks Initial delay in ticks
     * @param periodTicks Period between executions in ticks
     * @return Task ID
     */
    public int scheduleRepeating(Value func, long delayTicks, long periodTicks) {
        if (!func.canExecute()) {
            throw new IllegalArgumentException("scheduleRepeating() requires a function");
        }
        
        int taskId = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            try {
                func.execute();
            } catch (Exception e) {
                plugin.getLogger().warning("Error in ability task (" + abilityId + "): " + e.getMessage());
            }
        }, delayTicks, periodTicks).getTaskId();
        
        scriptContext.trackAbilityTask(abilityId, taskId);
        return taskId;
    }
    
    /**
     * Schedules a delayed task owned by this ability.
     * Automatically tracked and cleaned up on ability unload.
     * 
     * @param func JS function to execute
     * @param delayTicks Delay in ticks
     * @return Task ID
     */
    public int scheduleDelayed(Value func, long delayTicks) {
        if (!func.canExecute()) {
            throw new IllegalArgumentException("scheduleDelayed() requires a function");
        }
        
        int taskId = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            try {
                func.execute();
            } catch (Exception e) {
                plugin.getLogger().warning("Error in ability task (" + abilityId + "): " + e.getMessage());
            }
        }, delayTicks).getTaskId();
        
        scriptContext.trackAbilityTask(abilityId, taskId);
        return taskId;
    }
    
    /**
     * Cancels a scheduled task.
     * 
     * @param taskId Task ID
     */
    public void cancelTask(int taskId) {
        Bukkit.getScheduler().cancelTask(taskId);
    }
    
    /**
     * Ability-scoped state management.
     * State is automatically scoped to the current ability and player.
     */
    public static final class AbilityScopedState {
        private final String abilityId;
        private final AbilityStateStore stateStore;
        
        AbilityScopedState(String abilityId, AbilityStateStore stateStore) {
            this.abilityId = abilityId;
            this.stateStore = stateStore;
        }
        
        /**
         * Sets a state value for a player (scoped to this ability).
         */
        public void set(Player player, String key, Object value) {
            stateStore.set(abilityId, player.getUniqueId(), key, value);
        }
        
        /**
         * Gets a state value for a player (scoped to this ability).
         */
        public Object get(Player player, String key) {
            return stateStore.get(abilityId, player.getUniqueId(), key);
        }
        
        /**
         * Clears all state for a player (scoped to this ability).
         */
        public void clear(Player player) {
            stateStore.clear(abilityId, player.getUniqueId());
        }
    }
}
