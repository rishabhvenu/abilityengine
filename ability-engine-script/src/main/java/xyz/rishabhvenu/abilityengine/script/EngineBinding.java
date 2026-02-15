package xyz.rishabhvenu.abilityengine.script;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredListener;
import org.graalvm.polyglot.Value;
import xyz.rishabhvenu.abilityengine.api.*;
import xyz.rishabhvenu.abilityengine.core.EventTriggerRegistry;
import xyz.rishabhvenu.abilityengine.core.SessionManager;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * The global 'engine' object exposed to all scripts.
 * Provides the primary scripting API.
 */
public final class EngineBinding {
    
    private final Plugin plugin;
    private final AbilityRegistry registry;
    private final CooldownManager cooldownManager;
    private final AbilityItemService itemService;
    private final SessionManager sessionManager;
    private final EventTriggerRegistry eventTriggerRegistry;
    private final ScriptContext scriptContext;
    
    // Sub-APIs
    public final TriggerConstants trigger = new TriggerConstants();
    public final ConditionBindings condition = new ConditionBindings();
    public final SessionBindings sessions;
    public final CooldownBindings cooldowns;
    public final ItemBindings items;
    
    public EngineBinding(
            Plugin plugin,
            AbilityRegistry registry,
            CooldownManager cooldownManager,
            AbilityItemService itemService,
            SessionManager sessionManager,
            EventTriggerRegistry eventTriggerRegistry,
            ScriptContext scriptContext) {
        this.plugin = plugin;
        this.registry = registry;
        this.cooldownManager = cooldownManager;
        this.itemService = itemService;
        this.sessionManager = sessionManager;
        this.eventTriggerRegistry = eventTriggerRegistry;
        this.scriptContext = scriptContext;
        
        this.sessions = new SessionBindings(sessionManager);
        this.cooldowns = new CooldownBindings(cooldownManager);
        this.items = new ItemBindings(itemService);
    }
    
    /**
     * Registers an ability from a JS config object.
     * 
     * @param config JS object with id, triggers, conditions, cooldown, execute
     */
    public void ability(Value config) {
        if (!config.hasMembers()) {
            throw new IllegalArgumentException("ability() requires a config object");
        }
        
        // Extract ID
        String id = config.getMember("id").asString();
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Ability must have an id");
        }
        
        // Extract triggers
        Value triggersValue = config.getMember("triggers");
        Collection<TriggerType> triggers = parseTriggers(triggersValue);
        
        // Extract conditions
        Value conditionsValue = config.getMember("conditions");
        List<Condition> conditions = parseConditions(conditionsValue);
        
        // Extract cooldown (in seconds)
        Value cooldownValue = config.getMember("cooldown");
        Duration cooldown = Duration.ZERO;
        if (cooldownValue != null && !cooldownValue.isNull()) {
            if (cooldownValue.isNumber()) {
                cooldown = Duration.ofSeconds(cooldownValue.asLong());
            }
        }
        
        // Extract execute function
        Value executeFunc = config.getMember("execute");
        if (executeFunc == null || !executeFunc.canExecute()) {
            throw new IllegalArgumentException("Ability must have an execute function");
        }
        
        // Create and register the ability
        ScriptAbility ability = new ScriptAbility(id, triggers, conditions, cooldown, executeFunc);
        registry.register(ability);
        scriptContext.trackAbility(id);
        
        log("Registered ability: " + id);
    }
    
    /**
     * Registers a listener for a Bukkit event.
     * 
     * @param eventClassName Event class name (short or fully qualified)
     * @param handler JS function to handle the event
     */
    public void listen(String eventClassName, Value handler) {
        if (!handler.canExecute()) {
            throw new IllegalArgumentException("listen() requires a function as the second parameter");
        }
        
        // Resolve event class
        Class<? extends Event> eventClass = resolveEventClass(eventClassName);
        if (eventClass == null) {
            throw new IllegalArgumentException("Unknown event class: " + eventClassName);
        }
        
        // Create event executor
        EventExecutor executor = (listener, event) -> {
            try {
                handler.execute(event);
            } catch (Exception e) {
                plugin.getLogger().warning("Error in script event listener for " + eventClassName + ": " + e.getMessage());
            }
        };
        
        // Register the listener
        RegisteredListener registeredListener = new RegisteredListener(
            new org.bukkit.event.Listener() {},
            executor,
            EventPriority.NORMAL,
            plugin,
            false
        );
        
        Bukkit.getPluginManager().registerEvent(
            eventClass,
            registeredListener.getListener(),
            EventPriority.NORMAL,
            executor,
            plugin,
            false
        );
        
        scriptContext.trackEventListener(registeredListener);
        log("Registered event listener: " + eventClassName);
    }
    
    /**
     * Schedules a delayed task.
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
                plugin.getLogger().warning("Error in scheduled task: " + e.getMessage());
            }
        }, delayTicks).getTaskId();
        
        scriptContext.trackScheduledTask(taskId);
        return taskId;
    }
    
    /**
     * Schedules a repeating task.
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
                plugin.getLogger().warning("Error in repeating task: " + e.getMessage());
            }
        }, delayTicks, periodTicks).getTaskId();
        
        scriptContext.trackScheduledTask(taskId);
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
     * Logs an info message.
     */
    public void log(String message) {
        plugin.getLogger().info("[Script] " + message);
    }
    
    /**
     * Logs a warning message.
     */
    public void warn(String message) {
        plugin.getLogger().warning("[Script] " + message);
    }
    
    /**
     * Logs an error message.
     */
    public void error(String message) {
        plugin.getLogger().severe("[Script] " + message);
    }
    
    // Helper methods
    
    private Collection<TriggerType> parseTriggers(Value triggersValue) {
        List<TriggerType> triggers = new ArrayList<>();
        
        if (triggersValue == null || triggersValue.isNull()) {
            triggers.add(TriggerType.RIGHT_CLICK);
            return triggers;
        }
        
        if (triggersValue.hasArrayElements()) {
            long size = triggersValue.getArraySize();
            for (long i = 0; i < size; i++) {
                Value element = triggersValue.getArrayElement(i);
                String triggerName = element.asString();
                try {
                    triggers.add(TriggerType.valueOf(triggerName.toUpperCase()));
                } catch (IllegalArgumentException e) {
                    warn("Unknown trigger type: " + triggerName);
                }
            }
        }
        
        if (triggers.isEmpty()) {
            triggers.add(TriggerType.RIGHT_CLICK);
        }
        
        return triggers;
    }
    
    private List<Condition> parseConditions(Value conditionsValue) {
        List<Condition> conditions = new ArrayList<>();
        
        if (conditionsValue == null || conditionsValue.isNull() || !conditionsValue.hasArrayElements()) {
            return conditions;
        }
        
        long size = conditionsValue.getArraySize();
        for (long i = 0; i < size; i++) {
            Value element = conditionsValue.getArrayElement(i);
            
            // Each element should be a Condition returned by engine.condition.*
            // Or a JS function
            if (element.canExecute()) {
                // Custom JS function
                conditions.add(ctx -> {
                    try {
                        Object result = element.execute(ctx);
                        return result instanceof Boolean && (Boolean) result;
                    } catch (Exception e) {
                        return false;
                    }
                });
            } else if (element.isHostObject()) {
                // Java Condition object
                conditions.add(element.asHostObject());
            }
        }
        
        return conditions;
    }
    
    @SuppressWarnings("unchecked")
    private Class<? extends Event> resolveEventClass(String className) {
        // Try short name first (e.g., "PlayerJoinEvent")
        if (!className.contains(".")) {
            // Check common packages
            String[] packages = {
                "org.bukkit.event.player.",
                "org.bukkit.event.entity.",
                "org.bukkit.event.block.",
                "org.bukkit.event.inventory.",
                "org.bukkit.event.server.",
                "org.bukkit.event.world."
            };
            
            for (String pkg : packages) {
                try {
                    return (Class<? extends Event>) Class.forName(pkg + className);
                } catch (ClassNotFoundException e) {
                    // Try next package
                }
            }
        }
        
        // Try fully qualified name
        try {
            return (Class<? extends Event>) Class.forName(className);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }
    
    // Inner binding classes
    
    public static final class CooldownBindings {
        private final CooldownManager cooldownManager;
        
        CooldownBindings(CooldownManager cooldownManager) {
            this.cooldownManager = cooldownManager;
        }
        
        public boolean isReady(Player player, String abilityId) {
            return cooldownManager.isReady(player, abilityId);
        }
        
        public void set(Player player, String abilityId, double seconds) {
            cooldownManager.setCooldown(player, abilityId, Duration.ofMillis((long) (seconds * 1000)));
        }
        
        public double remaining(Player player, String abilityId) {
            return cooldownManager.getRemainingCooldown(player.getUniqueId(), abilityId).toMillis() / 1000.0;
        }
    }
    
    public static final class ItemBindings {
        private final AbilityItemService itemService;
        
        ItemBindings(AbilityItemService itemService) {
            this.itemService = itemService;
        }
        
        public ItemStack create(String abilityId) {
            return itemService.createAbilityItem(abilityId);
        }
        
        public boolean isAbilityItem(ItemStack item) {
            return itemService.isAbilityItem(item);
        }
        
        public String getAbilityId(ItemStack item) {
            return itemService.getAbilityId(item);
        }
    }
}
