package xyz.rishabhvenu.abilityengine.script;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;
import org.graalvm.polyglot.Value;

/**
 * Provides movement utilities for scripts.
 * Exposed as engine.movement
 */
public final class MovementBindings {
    
    private final Plugin plugin;
    
    public MovementBindings(Plugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Pulls an entity toward a target location with physics-safe velocity.
     * 
     * @param config JS object with entity, target, speed, drag, minSpeed, arrivalDistance, maxTicks, onArrival, onInterrupt
     * @param execution The execution instance for task tracking
     */
    public void pull(Value config, AbilityExecutionInstance execution) {
        if (!config.hasMembers()) {
            throw new IllegalArgumentException("pull() requires a config object");
        }
        
        Entity entity = config.getMember("entity").as(Entity.class);
        Location target = config.getMember("target").as(Location.class);
        double speed = getNumber(config, "speed", 1.0);
        double drag = getNumber(config, "drag", 0.7);
        double minSpeed = getNumber(config, "minSpeed", 0.15);
        double arrivalDistance = getNumber(config, "arrivalDistance", 1.5);
        int maxTicks = config.hasMember("maxTicks") ? config.getMember("maxTicks").asInt() : 80;
        
        Value onArrival = config.hasMember("onArrival") ? config.getMember("onArrival") : null;
        Value onInterrupt = config.hasMember("onInterrupt") ? config.getMember("onInterrupt") : null;
        
        final double[] currentSpeed = {speed};
        final int[] tickCount = {0};
        
        int taskId = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            tickCount[0]++;
            
            if (execution.isCancelled()) {
                if (onInterrupt != null && onInterrupt.canExecute()) {
                    try {
                        onInterrupt.execute();
                    } catch (Exception e) {
                        // Ignore
                    }
                }
                return; // Task will be cancelled by execution instance
            }
            
            if (!entity.isValid() || entity.isDead()) {
                execution.trackTask(-1); // Signal to stop
                return;
            }
            
            Location entityLoc = entity.getLocation();
            double dx = target.getX() - entityLoc.getX();
            double dy = target.getY() - (entityLoc.getY() + 1.0);
            double dz = target.getZ() - entityLoc.getZ();
            double remaining = Math.sqrt(dx * dx + dy * dy + dz * dz);
            
            // Check collision drag
            Vector actual = entity.getVelocity();
            double actualSpeed = Math.sqrt(
                actual.getX() * actual.getX() +
                actual.getY() * actual.getY() +
                actual.getZ() * actual.getZ()
            );
            
            if (tickCount[0] > 1 && actualSpeed < currentSpeed[0] * 0.4) {
                currentSpeed[0] = currentSpeed[0] * drag;
            }
            
            // Check stop conditions
            if (remaining <= arrivalDistance || tickCount[0] > maxTicks || currentSpeed[0] < minSpeed) {
                entity.setVelocity(new Vector(0, 0, 0));
                if (entity instanceof LivingEntity) {
                    ((LivingEntity) entity).setFallDistance(0);
                }
                
                if (onArrival != null && onArrival.canExecute()) {
                    try {
                        onArrival.execute();
                    } catch (Exception e) {
                        // Ignore
                    }
                }
                
                return; // Let execution instance handle task cleanup
            }
            
            // Set velocity
            double vx = (dx / remaining) * currentSpeed[0];
            double vy = (dy / remaining) * currentSpeed[0];
            double vz = (dz / remaining) * currentSpeed[0];
            entity.setVelocity(new Vector(vx, vy, vz));
            
            // Reset fall distance
            if (entity instanceof LivingEntity) {
                ((LivingEntity) entity).setFallDistance(0);
            }
        }, 1L, 1L).getTaskId();
        
        execution.trackTask(taskId);
    }
    
    /**
     * Dashes an entity in a direction.
     * 
     * @param config JS object with entity, direction, power, duration
     * @param execution The execution instance for task tracking
     */
    public void dash(Value config, AbilityExecutionInstance execution) {
        if (!config.hasMembers()) {
            throw new IllegalArgumentException("dash() requires a config object");
        }
        
        Entity entity = config.getMember("entity").as(Entity.class);
        Vector direction = config.getMember("direction").as(Vector.class);
        double power = getNumber(config, "power", 1.5);
        int duration = config.hasMember("duration") ? config.getMember("duration").asInt() : 0;
        
        Vector velocity = direction.clone().normalize().multiply(power);
        entity.setVelocity(velocity);
        
        if (entity instanceof LivingEntity) {
            ((LivingEntity) entity).setFallDistance(0);
        }
        
        // Sustained dash
        if (duration > 0) {
            int taskId = Bukkit.getScheduler().runTaskLater(plugin, () -> {
                entity.setVelocity(new Vector(0, 0, 0));
            }, duration).getTaskId();
            
            execution.trackTask(taskId);
        }
    }
    
    /**
     * Launches an entity in a direction.
     * 
     * @param config JS object with entity, direction, power
     */
    public void launch(Value config) {
        if (!config.hasMembers()) {
            throw new IllegalArgumentException("launch() requires a config object");
        }
        
        Entity entity = config.getMember("entity").as(Entity.class);
        Vector direction = config.getMember("direction").as(Vector.class);
        double power = getNumber(config, "power", 2.0);
        
        Vector velocity = direction.clone().normalize().multiply(power);
        entity.setVelocity(velocity);
        
        if (entity instanceof LivingEntity) {
            ((LivingEntity) entity).setFallDistance(0);
        }
    }
    
    private double getNumber(Value config, String key, double defaultValue) {
        if (!config.hasMember(key)) return defaultValue;
        Value v = config.getMember(key);
        return v.isNumber() ? v.asDouble() : defaultValue;
    }
}
