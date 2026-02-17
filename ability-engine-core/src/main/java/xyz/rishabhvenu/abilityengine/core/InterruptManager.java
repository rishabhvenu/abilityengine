package xyz.rishabhvenu.abilityengine.core;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages ability execution interrupts.
 * Listens for interrupt events and cancels matching executions.
 */
public final class InterruptManager implements Listener {
    
    private final ExecutionTracker executionTracker;
    
    // Map: ExecutionInstance -> Set of interrupt types
    private final Map<Object, Set<InterruptType>> registeredInterrupts = new ConcurrentHashMap<>();
    
    public InterruptManager(ExecutionTracker executionTracker) {
        this.executionTracker = executionTracker;
    }
    
    /**
     * Registers interrupts for an execution instance.
     * 
     * @param execution The execution instance
     * @param interruptTypes Set of interrupt types to watch for
     */
    public void registerInterrupts(Object execution, Set<InterruptType> interruptTypes) {
        if (interruptTypes != null && !interruptTypes.isEmpty()) {
            registeredInterrupts.put(execution, new HashSet<>(interruptTypes));
        }
    }
    
    /**
     * Unregisters interrupts for an execution instance.
     * 
     * @param execution The execution instance
     */
    public void unregisterInterrupts(Object execution) {
        registeredInterrupts.remove(execution);
    }
    
    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        
        cancelExecutions(player.getUniqueId(), InterruptType.TAKE_DAMAGE);
    }
    
    @EventHandler
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        cancelExecutions(player.getUniqueId(), InterruptType.SWITCH_ITEM);
    }
    
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        
        cancelExecutions(player.getUniqueId(), InterruptType.DEATH);
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        cancelExecutions(player.getUniqueId(), InterruptType.QUIT);
    }
    
    private void cancelExecutions(UUID playerId, InterruptType interruptType) {
        List<Object> executions = executionTracker.getExecutions(playerId);
        
        for (Object execution : executions) {
            Set<InterruptType> interruptTypes = registeredInterrupts.get(execution);
            
            if (interruptTypes != null && interruptTypes.contains(interruptType)) {
                // Call cancel() via reflection to avoid coupling
                try {
                    Method cancelMethod = execution.getClass().getMethod("cancel");
                    cancelMethod.invoke(execution);
                    unregisterInterrupts(execution);
                } catch (Exception e) {
                    // Ignore if method doesn't exist or fails
                }
            }
        }
    }
}
