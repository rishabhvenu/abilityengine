package xyz.rishabhvenu.abilityengine.script;

import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.graalvm.polyglot.Value;
import xyz.rishabhvenu.abilityengine.api.AbilityContext;
import xyz.rishabhvenu.abilityengine.api.CooldownManager;
import xyz.rishabhvenu.abilityengine.api.TriggerType;
import xyz.rishabhvenu.abilityengine.core.AbilityStateStore;
import xyz.rishabhvenu.abilityengine.core.BossBarManager;
import xyz.rishabhvenu.abilityengine.core.PhaseInstance;

import java.time.Duration;

/**
 * Enhanced execution context passed to JavaScript ability callbacks.
 * Wraps AbilityContext with additional scripting conveniences:
 * - Ability-scoped state management
 * - Task ownership tracking
 * - Automatic cleanup
 * - Phase API access
 * - Cooldown override
 */
public final class AbilityExecContext {
    
    private final AbilityContext raw;
    private final String abilityId;
    private final ScriptContext scriptContext;
    private final AbilityStateStore stateStore;
    private final Plugin plugin;
    private final CooldownManager cooldownManager;
    private final BossBarManager bossBarManager;
    private final AbilityExecutionInstance execution;
    
    // Ability-scoped state API
    public final AbilityScopedState state;
    
    public AbilityExecContext(
            AbilityContext raw,
            String abilityId,
            ScriptContext scriptContext,
            AbilityStateStore stateStore,
            Plugin plugin,
            CooldownManager cooldownManager,
            BossBarManager bossBarManager,
            AbilityExecutionInstance execution) {
        this.raw = raw;
        this.abilityId = abilityId;
        this.scriptContext = scriptContext;
        this.stateStore = stateStore;
        this.plugin = plugin;
        this.cooldownManager = cooldownManager;
        this.bossBarManager = bossBarManager;
        this.execution = execution;
        this.state = stateStore != null ? new AbilityScopedState(abilityId, stateStore) : null;
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
        
        // Track on execution instance if available, otherwise on script context
        if (execution != null) {
            execution.trackTask(taskId);
        } else if (scriptContext != null) {
            scriptContext.trackAbilityTask(abilityId, taskId);
        }
        
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
        
        // Track on execution instance if available, otherwise on script context
        if (execution != null) {
            execution.trackTask(taskId);
        } else if (scriptContext != null) {
            scriptContext.trackAbilityTask(abilityId, taskId);
        }
        
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
     * Gets the current phase instance (if phases are active).
     * 
     * @return PhaseInstance or null if no phases
     */
    public PhaseInstance phase() {
        if (execution == null) {
            return null;
        }
        return (PhaseInstance) execution.getActivePhase();
    }
    
    /**
     * Gets the execution instance.
     * 
     * @return The execution instance
     */
    public AbilityExecutionInstance execution() {
        return execution;
    }
    
    /**
     * Overrides the cooldown for this ability.
     * Updates both the cooldown manager and refreshes the boss bar.
     * 
     * @param seconds New cooldown duration in seconds
     */
    public void overrideCooldown(double seconds) {
        if (cooldownManager == null || bossBarManager == null) {
            return;
        }
        
        Duration newCooldown = Duration.ofMillis((long) (seconds * 1000));
        cooldownManager.setCooldown(raw.player(), abilityId, newCooldown);
        
        // Refresh boss bar if one exists
        bossBarManager.removeBar(raw.player(), abilityId);
        if (seconds > 0) {
            bossBarManager.showCooldownBar(
                plugin,
                raw.player(),
                abilityId,
                abilityId,
                (int) Math.ceil(seconds),
                BarColor.GREEN,
                BarStyle.SOLID
            );
        }
    }
    
    /**
     * Shortens the remaining cooldown by a percentage.
     * 
     * @param percent Percentage to reduce (0-100)
     */
    public void shortenCooldown(double percent) {
        if (cooldownManager == null || bossBarManager == null) {
            return;
        }
        
        Duration remaining = cooldownManager.getRemainingCooldown(raw.player().getUniqueId(), abilityId);
        if (remaining.isZero()) {
            return;
        }
        
        double factor = 1.0 - (percent / 100.0);
        long newMillis = (long) (remaining.toMillis() * factor);
        double newSeconds = newMillis / 1000.0;
        
        overrideCooldown(newSeconds);
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
