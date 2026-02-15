package xyz.rishabhvenu.abilityengine.api;

import org.bukkit.inventory.ItemStack;

import java.util.Arrays;

/**
 * Static factory class for creating common conditions.
 * Provides fluent API for building condition chains.
 */
public final class Conditions {
    
    private Conditions() {
        throw new UnsupportedOperationException("Utility class");
    }
    
    /**
     * Condition that checks if the player is sneaking.
     */
    public static Condition sneaking() {
        return context -> context.player().isSneaking();
    }
    
    /**
     * Condition that checks if the player is NOT sneaking.
     */
    public static Condition notSneaking() {
        return context -> !context.player().isSneaking();
    }
    
    /**
     * Condition that checks if the player is holding an ability item.
     */
    public static Condition holdingAbilityItem() {
        return context -> context.item() != null;
    }
    
    /**
     * Condition that checks if the player's health is above a threshold.
     * 
     * @param threshold Minimum health (exclusive)
     */
    public static Condition healthAbove(double threshold) {
        return context -> context.player().getHealth() > threshold;
    }
    
    /**
     * Condition that checks if the player's health is below a threshold.
     * 
     * @param threshold Maximum health (exclusive)
     */
    public static Condition healthBelow(double threshold) {
        return context -> context.player().getHealth() < threshold;
    }
    
    /**
     * Condition that checks if the player's Y coordinate is above a threshold.
     * 
     * @param y Minimum Y coordinate (exclusive)
     */
    public static Condition yAbove(double y) {
        return context -> context.player().getLocation().getY() > y;
    }
    
    /**
     * Condition that checks if the player's Y coordinate is below a threshold.
     * 
     * @param y Maximum Y coordinate (exclusive)
     */
    public static Condition yBelow(double y) {
        return context -> context.player().getLocation().getY() < y;
    }
    
    /**
     * Condition that checks if the context has a target entity.
     */
    public static Condition hasTarget() {
        return context -> context.targetEntity() != null;
    }
    
    /**
     * Condition that always checks if cooldown is ready.
     * Note: This requires access to CooldownManager, so it's typically handled
     * by the engine before condition evaluation. This is provided for completeness.
     * 
     * @param cooldownManager The cooldown manager
     * @param abilityId The ability ID
     */
    public static Condition cooldownReady(CooldownManager cooldownManager, String abilityId) {
        return context -> cooldownManager.isReady(context.player(), abilityId);
    }
    
    /**
     * Combines multiple conditions with AND logic.
     * All conditions must pass for the combined condition to pass.
     * 
     * @param conditions Conditions to combine
     */
    public static Condition and(Condition... conditions) {
        return context -> {
            for (Condition condition : conditions) {
                if (!condition.test(context)) {
                    return false;
                }
            }
            return true;
        };
    }
    
    /**
     * Combines multiple conditions with OR logic.
     * At least one condition must pass for the combined condition to pass.
     * 
     * @param conditions Conditions to combine
     */
    public static Condition or(Condition... conditions) {
        return context -> {
            for (Condition condition : conditions) {
                if (condition.test(context)) {
                    return true;
                }
            }
            return false;
        };
    }
    
    /**
     * Negates a condition.
     * 
     * @param condition Condition to negate
     */
    public static Condition not(Condition condition) {
        return context -> !condition.test(context);
    }
    
    /**
     * Condition that checks if the player is holding a specific item type.
     * 
     * @param material The material to check
     */
    public static Condition holdingMaterial(org.bukkit.Material material) {
        return context -> {
            ItemStack item = context.item();
            return item != null && item.getType() == material;
        };
    }
}
