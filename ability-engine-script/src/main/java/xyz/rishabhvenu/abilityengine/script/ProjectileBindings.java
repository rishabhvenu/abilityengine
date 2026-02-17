package xyz.rishabhvenu.abilityengine.script;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionType;
import org.bukkit.util.Vector;
import org.graalvm.polyglot.Value;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Provides projectile utilities for scripts.
 * Exposed as engine.projectile
 */
public final class ProjectileBindings implements Listener {
    
    private final Plugin plugin;
    private final ScriptContext scriptContext;
    private final Map<UUID, ProjectileTracker> trackedProjectiles = new ConcurrentHashMap<>();
    
    public ProjectileBindings(Plugin plugin, ScriptContext scriptContext) {
        this.plugin = plugin;
        this.scriptContext = scriptContext;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }
    
    /**
     * Spawns a projectile with configuration.
     */
    public Projectile spawn(Value config) {
        if (!config.hasMembers()) {
            throw new IllegalArgumentException("spawn() requires a config object");
        }
        
        String typeStr = config.getMember("type").asString();
        Player shooter = config.getMember("shooter").as(Player.class);
        double speed = getNumber(config, "speed", 1.0);
        
        Location loc = shooter.getEyeLocation();
        Vector dir = loc.getDirection().multiply(speed);
        
        Class<? extends Projectile> projectileClass = parseProjectileType(typeStr);
        Projectile projectile = shooter.getWorld().spawn(loc, projectileClass);
        projectile.setShooter(shooter);
        projectile.setVelocity(dir);
        
        // Configure arrow-specific properties
        if (projectile instanceof Arrow arrow) {
            if (config.hasMember("damage")) {
                arrow.setDamage(getNumber(config, "damage", 2.0));
            }
            if (config.hasMember("critical") && config.getMember("critical").asBoolean()) {
                arrow.setCritical(true);
            }
            if (config.hasMember("potion")) {
                Value potionValue = config.getMember("potion");
                if (potionValue.isString()) {
                    // Simple string form: potion: "POISON"
                    arrow.setBasePotionType(PotionType.valueOf(potionValue.asString().toUpperCase()));
                } else if (potionValue.hasMembers()) {
                    // Object form: potion: {type: "POISON", duration: 100, amplifier: 1}
                    String potionType = potionValue.getMember("type").asString();
                    arrow.setBasePotionType(PotionType.valueOf(potionType.toUpperCase()));
                    
                    // Apply custom effect if duration/amplifier specified
                    int duration = potionValue.hasMember("duration") ?
                        potionValue.getMember("duration").asInt() : 100;
                    int amplifier = potionValue.hasMember("amplifier") ?
                        potionValue.getMember("amplifier").asInt() : 0;
                    
                    org.bukkit.potion.PotionEffectType effectType = 
                        org.bukkit.potion.PotionEffectType.getByName(potionType.toUpperCase());
                    if (effectType != null) {
                        arrow.addCustomEffect(
                            new org.bukkit.potion.PotionEffect(effectType, duration, amplifier), true);
                    }
                }
            }
        }
        
        // Set up trail and callbacks
        Value onTick = config.hasMember("onTick") ? config.getMember("onTick") : null;
        Value onHit = config.hasMember("onHit") ? config.getMember("onHit") : null;
        Value trailConfig = config.hasMember("trail") ? config.getMember("trail") : null;
        int maxTicks = config.hasMember("maxTicks") ? config.getMember("maxTicks").asInt() : 200;
        
        if (onTick != null || trailConfig != null) {
            setupTrail(projectile, onTick, trailConfig, maxTicks);
        }
        
        if (onHit != null) {
            trackedProjectiles.put(projectile.getUniqueId(), 
                new ProjectileTracker(projectile, onHit));
        }
        
        return projectile;
    }
    
    private void setupTrail(Projectile projectile, Value onTick, Value trailConfig, int maxTicks) {
        final int[] ticks = {0};
        
        int taskId = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            ticks[0]++;
            
            if (!projectile.isValid() || ticks[0] > maxTicks) {
                Bukkit.getScheduler().cancelTask(scriptContext.getScheduledTasks().get(
                    scriptContext.getScheduledTasks().size() - 1));
                return;
            }
            
            // Execute onTick callback
            if (onTick != null && onTick.canExecute()) {
                try {
                    onTick.execute(projectile, ticks[0]);
                } catch (Exception e) {
                    // Ignore
                }
            }
            
            // Spawn trail particles
            if (trailConfig != null && trailConfig.hasMembers()) {
                spawnTrailParticles(projectile.getLocation(), trailConfig);
            }
        }, 1L, 1L).getTaskId();
        
        scriptContext.trackScheduledTask(taskId);
    }
    
    private void spawnTrailParticles(Location loc, Value trailConfig) {
        try {
            String particleStr = trailConfig.getMember("particle").asString();
            Particle particle = Particle.valueOf(particleStr.toUpperCase());
            int count = trailConfig.hasMember("count") ? trailConfig.getMember("count").asInt() : 5;
            double spread = trailConfig.hasMember("spread") ? getNumber(trailConfig, "spread", 0.2) : 0.2;
            
            if (trailConfig.hasMember("color") && particle == Particle.ENTITY_EFFECT) {
                String colorHex = trailConfig.getMember("color").asString();
                Color color = parseColor(colorHex);
                loc.getWorld().spawnParticle(particle, loc, count, spread, spread, spread, 0, color);
            } else {
                loc.getWorld().spawnParticle(particle, loc, count, spread, spread, spread);
            }
        } catch (Exception e) {
            // Ignore
        }
    }
    
    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        UUID id = event.getEntity().getUniqueId();
        ProjectileTracker tracker = trackedProjectiles.remove(id);
        
        if (tracker != null && tracker.onHit.canExecute()) {
            try {
                tracker.onHit.execute(event.getEntity(), event);
            } catch (Exception e) {
                // Ignore
            }
        }
    }
    
    @SuppressWarnings("unchecked")
    private Class<? extends Projectile> parseProjectileType(String type) {
        return switch (type.toUpperCase()) {
            case "ARROW" -> Arrow.class;
            case "FIREBALL" -> Fireball.class;
            case "SNOWBALL" -> Snowball.class;
            case "EGG" -> Egg.class;
            case "ENDER_PEARL" -> EnderPearl.class;
            default -> Arrow.class;
        };
    }
    
    private double getNumber(Value config, String key, double defaultValue) {
        if (!config.hasMember(key)) return defaultValue;
        Value v = config.getMember(key);
        return v.isNumber() ? v.asDouble() : defaultValue;
    }
    
    private Color parseColor(String hex) {
        if (hex.startsWith("#")) hex = hex.substring(1);
        int rgb = Integer.parseInt(hex, 16);
        return Color.fromRGB(rgb);
    }
    
    private record ProjectileTracker(Projectile projectile, Value onHit) {}
}
