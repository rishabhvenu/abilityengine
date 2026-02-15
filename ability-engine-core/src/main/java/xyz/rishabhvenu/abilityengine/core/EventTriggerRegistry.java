package xyz.rishabhvenu.abilityengine.core;

import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.Plugin;
import xyz.rishabhvenu.abilityengine.api.Ability;
import xyz.rishabhvenu.abilityengine.api.AbilityContext;
import xyz.rishabhvenu.abilityengine.api.Condition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Logger;

/**
 * Registry for event-based ability triggers.
 * Allows abilities to fire on arbitrary Bukkit events (advanced path).
 */
public final class EventTriggerRegistry implements Listener {
    
    private final Plugin plugin;
    private final Logger logger;
    private final Map<Class<? extends Event>, List<EventAbilityBinding>> bindings = new HashMap<>();
    
    public EventTriggerRegistry(Plugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }
    
    /**
     * Registers an ability to fire when a specific event occurs.
     * 
     * @param eventClass The event class to listen for
     * @param ability The ability to fire
     * @param contextBuilder Function to build AbilityContext from the event
     * @param condition Optional additional condition (null = always fire)
     * @param <T> Event type
     */
    public <T extends Event> void registerEventTrigger(
            Class<T> eventClass,
            Ability ability,
            Function<T, AbilityContext> contextBuilder,
            Condition condition) {
        
        EventAbilityBinding binding = new EventAbilityBinding(ability, contextBuilder, condition);
        
        // Add to our tracking map
        bindings.computeIfAbsent(eventClass, k -> new ArrayList<>()).add(binding);
        
        // Register the actual Bukkit event listener
        plugin.getServer().getPluginManager().registerEvent(
            eventClass,
            this,
            EventPriority.NORMAL,
            createEventExecutor(binding),
            plugin,
            false
        );
        
        logger.info("Registered event trigger: " + eventClass.getSimpleName() + " -> " + ability.id());
    }
    
    /**
     * Unregisters all event triggers for an ability.
     * 
     * @param abilityId The ability ID
     */
    public void unregisterEventTriggers(String abilityId) {
        bindings.values().forEach(list -> 
            list.removeIf(binding -> binding.ability.id().equals(abilityId))
        );
    }
    
    @SuppressWarnings("unchecked")
    private <T extends Event> EventExecutor createEventExecutor(EventAbilityBinding binding) {
        return (listener, event) -> {
            try {
                // Build context from event
                AbilityContext context = ((Function<T, AbilityContext>) binding.contextBuilder).apply((T) event);
                
                if (context == null) {
                    return;
                }
                
                // Check additional condition if present
                if (binding.condition != null && !binding.condition.test(context)) {
                    return;
                }
                
                // Execute the ability
                binding.ability.execute(context);
                
            } catch (Exception e) {
                logger.severe("Error executing event-triggered ability " + binding.ability.id() + ": " + e.getMessage());
            }
        };
    }
    
    /**
     * Internal class to track event-ability bindings.
     */
    private static final class EventAbilityBinding {
        final Ability ability;
        final Function<?, AbilityContext> contextBuilder;
        final Condition condition;
        
        EventAbilityBinding(Ability ability, Function<?, AbilityContext> contextBuilder, Condition condition) {
            this.ability = ability;
            this.contextBuilder = contextBuilder;
            this.condition = condition;
        }
    }
}
