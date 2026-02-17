package xyz.rishabhvenu.abilityengine.script;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

import java.util.Collection;

/**
 * Provides raycast utilities for scripts.
 * Exposed as engine.raycast()
 */
public final class RaycastBindings {
    
    /**
     * Performs a raycast with block and entity detection.
     * 
     * @param config JS object with origin, direction, maxDistance, detect, entityRadius, callbacks
     * @param graalContext The GraalVM context for creating result objects
     * @return Result object with type, location, entity, block
     */
    public Value raycast(Value config, Context graalContext) {
        // Validate main-thread execution
        if (!Bukkit.isPrimaryThread()) {
            throw new IllegalStateException("Raycast must be called from the main thread");
        }
        
        if (!config.hasMembers()) {
            throw new IllegalArgumentException("raycast() requires a config object");
        }
        
        // Parse config
        Location origin = config.getMember("origin").as(Location.class);
        Vector direction = config.getMember("direction").as(Vector.class);
        double maxDistance = getNumber(config, "maxDistance", 50.0);
        double entityRadius = getNumber(config, "entityRadius", 1.5);
        
        // Parse detect array
        boolean detectBlock = true;
        boolean detectEntity = true;
        if (config.hasMember("detect") && config.getMember("detect").hasArrayElements()) {
            Value detectArray = config.getMember("detect");
            long size = detectArray.getArraySize();
            detectBlock = false;
            detectEntity = false;
            for (long i = 0; i < size; i++) {
                String type = detectArray.getArrayElement(i).asString();
                if ("BLOCK".equalsIgnoreCase(type)) {
                    detectBlock = true;
                } else if ("ENTITY".equalsIgnoreCase(type)) {
                    detectEntity = true;
                }
            }
        }
        
        // Extract callbacks
        Value onHitBlock = config.hasMember("onHitBlock") ? config.getMember("onHitBlock") : null;
        Value onHitEntity = config.hasMember("onHitEntity") ? config.getMember("onHitEntity") : null;
        Value onMiss = config.hasMember("onMiss") ? config.getMember("onMiss") : null;
        
        World world = origin.getWorld();
        Vector normalizedDir = direction.clone().normalize();
        
        // Block raytrace
        RayTraceResult blockResult = null;
        double blockDistance = Double.MAX_VALUE;
        if (detectBlock) {
            blockResult = world.rayTraceBlocks(origin, normalizedDir, maxDistance);
            if (blockResult != null) {
                blockDistance = origin.distance(blockResult.getHitPosition().toLocation(world));
            }
        }
        
        // Entity detection via stepping
        Entity hitEntity = null;
        double entityDistance = Double.MAX_VALUE;
        if (detectEntity) {
            double stepSize = 0.5;
            int steps = (int) Math.ceil(maxDistance / stepSize);
            
            for (int i = 0; i <= steps; i++) {
                double distance = i * stepSize;
                if (distance > maxDistance) break;
                
                Location checkLoc = origin.clone().add(normalizedDir.clone().multiply(distance));
                Collection<Entity> nearby = world.getNearbyEntities(checkLoc, entityRadius, entityRadius, entityRadius);
                
                for (Entity entity : nearby) {
                    // Skip if entity is the origin player (if origin is a player's eye location)
                    if (entity instanceof Player) {
                        Player player = (Player) entity;
                        if (player.getEyeLocation().distanceSquared(origin) < 0.1) {
                            continue;
                        }
                    }
                    
                    double dist = origin.distance(entity.getLocation());
                    if (dist < entityDistance) {
                        hitEntity = entity;
                        entityDistance = dist;
                    }
                }
                
                // Stop if we found an entity closer than the block
                if (hitEntity != null && entityDistance < blockDistance) {
                    break;
                }
            }
        }
        
        // Determine winner
        Value result = graalContext.eval("js", "({})");
        
        if (hitEntity != null && entityDistance < blockDistance) {
            // Entity hit
            result.putMember("type", "ENTITY");
            result.putMember("location", hitEntity.getLocation());
            result.putMember("entity", hitEntity);
            result.putMember("block", null);
            
            if (onHitEntity != null && onHitEntity.canExecute()) {
                try {
                    onHitEntity.execute(result);
                } catch (Exception e) {
                    // Ignore callback errors
                }
            }
        } else if (blockResult != null) {
            // Block hit
            result.putMember("type", "BLOCK");
            result.putMember("location", blockResult.getHitPosition().toLocation(world));
            result.putMember("entity", null);
            result.putMember("block", blockResult.getHitBlock());
            
            if (onHitBlock != null && onHitBlock.canExecute()) {
                try {
                    onHitBlock.execute(result);
                } catch (Exception e) {
                    // Ignore callback errors
                }
            }
        } else {
            // Miss
            Location endLocation = origin.clone().add(normalizedDir.clone().multiply(maxDistance));
            result.putMember("type", "MISS");
            result.putMember("location", endLocation);
            result.putMember("entity", null);
            result.putMember("block", null);
            
            if (onMiss != null && onMiss.canExecute()) {
                try {
                    onMiss.execute(endLocation);
                } catch (Exception e) {
                    // Ignore callback errors
                }
            }
        }
        
        return result;
    }
    
    private double getNumber(Value config, String key, double defaultValue) {
        if (!config.hasMember(key)) return defaultValue;
        Value v = config.getMember(key);
        return v.isNumber() ? v.asDouble() : defaultValue;
    }
}
