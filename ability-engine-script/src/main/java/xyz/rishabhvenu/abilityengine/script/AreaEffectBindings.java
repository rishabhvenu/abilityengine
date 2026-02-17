package xyz.rishabhvenu.abilityengine.script;

import org.bukkit.*;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.AreaEffectCloudApplyEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.graalvm.polyglot.Value;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Provides area effect cloud utilities for scripts.
 * Exposed as engine.areaEffect
 */
public final class AreaEffectBindings implements Listener {
    
    private final Plugin plugin;
    private final Set<UUID> excludeCasterClouds = new HashSet<>();
    
    public AreaEffectBindings(Plugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }
    
    /**
     * Spawns an area effect cloud with configuration.
     */
    public AreaEffectCloud spawn(Value config) {
        if (!config.hasMembers()) {
            throw new IllegalArgumentException("spawn() requires a config object");
        }
        
        Location location = config.getMember("location").as(Location.class);
        LivingEntity source = config.hasMember("source") ? 
            config.getMember("source").as(LivingEntity.class) : null;
        
        float radius = getFloat(config, "radius", 3.0f);
        int durationTicks = (int) getNumber(config, "duration", 100.0);
        
        AreaEffectCloud cloud = location.getWorld().spawn(location, AreaEffectCloud.class);
        
        if (source != null) {
            cloud.setSource(source);
        }
        
        cloud.setRadius(radius);
        cloud.setDuration(durationTicks);
        
        // Handle color
        if (config.hasMember("color")) {
            String colorHex = config.getMember("color").asString();
            cloud.setColor(parseColor(colorHex));
        }
        
        // Handle potion effect
        if (config.hasMember("potion")) {
            Value potionConfig = config.getMember("potion");
            if (potionConfig.hasMembers()) {
                String type = potionConfig.getMember("type").asString();
                int duration = potionConfig.hasMember("duration") ? 
                    potionConfig.getMember("duration").asInt() : 100;
                int amplifier = potionConfig.hasMember("amplifier") ? 
                    potionConfig.getMember("amplifier").asInt() : 0;
                
                PotionEffectType effectType = PotionEffectType.getByName(type.toUpperCase());
                if (effectType != null) {
                    cloud.addCustomEffect(new PotionEffect(effectType, duration, amplifier), true);
                }
            }
        }
        
        // Handle radius shrink (accepts boolean true for auto-calc, or a number for explicit rate)
        if (config.hasMember("radiusShrink")) {
            Value shrinkVal = config.getMember("radiusShrink");
            if (shrinkVal.isNumber()) {
                cloud.setRadiusPerTick(-1.0f * (float) shrinkVal.asDouble());
            } else if (shrinkVal.isBoolean() && shrinkVal.asBoolean()) {
                cloud.setRadiusPerTick(-radius / durationTicks);
            }
        }
        
        // Handle excludeCaster
        if (config.hasMember("excludeCaster") && config.getMember("excludeCaster").asBoolean()) {
            cloud.setMetadata("ae_exclude_caster", new FixedMetadataValue(plugin, true));
            excludeCasterClouds.add(cloud.getUniqueId());
        }
        
        return cloud;
    }
    
    @EventHandler
    public void onAreaEffectCloudApply(AreaEffectCloudApplyEvent event) {
        AreaEffectCloud cloud = event.getEntity();
        
        if (!cloud.hasMetadata("ae_exclude_caster")) {
            return;
        }
        
        org.bukkit.projectiles.ProjectileSource source = cloud.getSource();
        if (source instanceof LivingEntity living) {
            event.getAffectedEntities().remove(living);
        }
    }
    
    private double getNumber(Value config, String key, double defaultValue) {
        if (!config.hasMember(key)) return defaultValue;
        Value v = config.getMember(key);
        return v.isNumber() ? v.asDouble() : defaultValue;
    }
    
    private float getFloat(Value config, String key, float defaultValue) {
        return (float) getNumber(config, key, defaultValue);
    }
    
    private Color parseColor(String hex) {
        if (hex.startsWith("#")) hex = hex.substring(1);
        int rgb = Integer.parseInt(hex, 16);
        return Color.fromRGB(rgb);
    }
}
