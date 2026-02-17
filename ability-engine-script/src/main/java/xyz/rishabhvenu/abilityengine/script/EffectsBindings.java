package xyz.rishabhvenu.abilityengine.script;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.graalvm.polyglot.Value;

/**
 * Provides effect utilities for scripts.
 * Exposed as engine.effects
 */
public final class EffectsBindings {
    
    /**
     * Spawns particles at a location.
     */
    public void particle(Location location, String particleType, String colorHex, 
                        int count, double spreadX, double spreadY, double spreadZ) {
        try {
            Particle particle = Particle.valueOf(particleType.toUpperCase());
            
            if (colorHex != null && particle == Particle.ENTITY_EFFECT) {
                Color color = parseColor(colorHex);
                location.getWorld().spawnParticle(particle, location, count, 
                    spreadX, spreadY, spreadZ, 0, color);
            } else {
                location.getWorld().spawnParticle(particle, location, count, 
                    spreadX, spreadY, spreadZ);
            }
        } catch (Exception e) {
            // Silently fail for invalid particles
        }
    }
    
    /**
     * Plays a sound at a location.
     */
    public void sound(Location location, String sound, double volume, double pitch) {
        location.getWorld().playSound(location, sound, (float) volume, (float) pitch);
    }
    
    /**
     * Applies a potion effect to an entity.
     */
    public void potion(LivingEntity target, String effectType, int durationTicks, int amplifier) {
        try {
            PotionEffectType type = PotionEffectType.getByName(effectType.toUpperCase());
            if (type != null) {
                target.addPotionEffect(new PotionEffect(type, durationTicks, amplifier));
            }
        } catch (Exception e) {
            // Silently fail for invalid effects
        }
    }
    
    /**
     * Applies knockback to an entity.
     */
    public void knockback(Entity target, Vector direction, double strength) {
        Vector velocity = direction.normalize().multiply(strength);
        target.setVelocity(velocity);
    }
    
    /**
     * Creates an explosion.
     */
    public void explosion(Location location, double power, boolean setFire, boolean breakBlocks) {
        location.getWorld().createExplosion(location, (float) power, setFire, breakBlocks);
    }
    
    /**
     * Decays terrain based on predefined rules.
     */
    public void decayTerrain(Location center, int radius, String rules) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    Block block = center.getBlock().getRelative(dx, dy, dz);
                    applyDecayRule(block, rules);
                }
            }
        }
    }
    
    /**
     * Decays terrain with a custom rule function.
     */
    public void decayTerrainCustom(Location center, int radius, Value ruleFunction) {
        if (!ruleFunction.canExecute()) {
            return;
        }
        
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    Block block = center.getBlock().getRelative(dx, dy, dz);
                    try {
                        Object result = ruleFunction.execute(block);
                        if (result instanceof String materialName) {
                            Material newMaterial = Material.valueOf(materialName.toUpperCase());
                            block.setType(newMaterial);
                        }
                    } catch (Exception e) {
                        // Skip on error
                    }
                }
            }
        }
    }
    
    private void applyDecayRule(Block block, String rules) {
        if (block == null || block.isEmpty()) {
            return;
        }
        
        Material type = block.getType();
        
        switch (rules.toUpperCase()) {
            case "NATURE_ONLY":
                applyNatureDecay(block, type);
                break;
            case "STONE_DECAY":
                applyStoneDecay(block, type);
                break;
            case "ICE_MELT":
                applyIceMelt(block, type);
                break;
        }
    }
    
    private void applyNatureDecay(Block block, Material type) {
        if (type == Material.GRASS_BLOCK || type == Material.MYCELIUM || type == Material.PODZOL) {
            block.setType(Material.DIRT);
        } else if (type.name().endsWith("_LEAVES")) {
            block.breakNaturally();
        } else if (type == Material.TALL_GRASS || type == Material.SHORT_GRASS ||
                   type == Material.FERN || type == Material.LARGE_FERN ||
                   type == Material.DEAD_BUSH) {
            block.setType(Material.AIR);
        }
    }
    
    private void applyStoneDecay(Block block, Material type) {
        if (type == Material.STONE) {
            block.setType(Material.COBBLESTONE);
        } else if (type == Material.DEEPSLATE) {
            block.setType(Material.COBBLED_DEEPSLATE);
        }
    }
    
    private void applyIceMelt(Block block, Material type) {
        if (type == Material.ICE || type == Material.PACKED_ICE || type == Material.BLUE_ICE) {
            block.setType(Material.WATER);
        } else if (type == Material.SNOW || type == Material.SNOW_BLOCK) {
            block.setType(Material.AIR);
        }
    }
    
    private Color parseColor(String hex) {
        if (hex.startsWith("#")) {
            hex = hex.substring(1);
        }
        int rgb = Integer.parseInt(hex, 16);
        return Color.fromRGB(rgb);
    }
}
