package xyz.rishabhvenu.abilityengine.core;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages frozen entities.
 * Suppresses movement by zeroing velocity and canceling movement events.
 * Thread-safe using ConcurrentHashMap.
 */
public final class EntityControlManager implements Listener {
    
    private final Plugin plugin;
    private final Map<UUID, FreezeConfig> frozenEntities = new ConcurrentHashMap<>();
    private int suppressionTaskId = -1;
    
    public EntityControlManager(Plugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Starts the velocity suppression task.
     * Should be called during plugin enable.
     */
    public void start() {
        // Repeating task that zeros velocity for frozen entities
        suppressionTaskId = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Map.Entry<UUID, FreezeConfig> entry : frozenEntities.entrySet()) {
                Entity entity = Bukkit.getEntity(entry.getKey());
                if (entity != null && entity.isValid()) {
                    if (entry.getValue().preventMovement) {
                        entity.setVelocity(new Vector(0, 0, 0));
                    }
                }
            }
        }, 1L, 1L).getTaskId();
    }
    
    /**
     * Stops the velocity suppression task.
     * Should be called during plugin disable.
     */
    public void stop() {
        if (suppressionTaskId != -1) {
            Bukkit.getScheduler().cancelTask(suppressionTaskId);
            suppressionTaskId = -1;
        }
        
        // Unfreeze all entities
        for (UUID entityId : frozenEntities.keySet()) {
            unfreeze(entityId);
        }
    }
    
    /**
     * Freezes an entity.
     * 
     * @param entityId The entity UUID
     * @param preventMovement Whether to prevent movement
     * @param preventRotation Whether to prevent rotation (not yet implemented)
     * @param duration Duration in ticks (0 for permanent)
     */
    public void freeze(UUID entityId, boolean preventMovement, boolean preventRotation, int duration) {
        FreezeConfig config = new FreezeConfig(preventMovement, preventRotation);
        frozenEntities.put(entityId, config);
        
        // Schedule auto-unfreeze if duration is specified
        if (duration > 0) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                unfreeze(entityId);
            }, duration);
        }
    }
    
    /**
     * Unfreezes an entity.
     * 
     * @param entityId The entity UUID
     */
    public void unfreeze(UUID entityId) {
        frozenEntities.remove(entityId);
    }
    
    /**
     * Checks if an entity is frozen.
     * 
     * @param entityId The entity UUID
     * @return true if frozen
     */
    public boolean isFrozen(UUID entityId) {
        return frozenEntities.containsKey(entityId);
    }
    
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        FreezeConfig config = frozenEntities.get(player.getUniqueId());
        
        if (config != null && config.preventMovement) {
            // Only cancel if player actually moved (not just head rotation)
            if (event.getFrom().distanceSquared(event.getTo()) > 0.001) {
                event.setCancelled(true);
            }
        }
    }
    
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        UUID entityId = event.getEntity().getUniqueId();
        unfreeze(entityId);
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        unfreeze(playerId);
    }
    
    private static class FreezeConfig {
        final boolean preventMovement;
        final boolean preventRotation;
        
        FreezeConfig(boolean preventMovement, boolean preventRotation) {
            this.preventMovement = preventMovement;
            this.preventRotation = preventRotation;
        }
    }
}
