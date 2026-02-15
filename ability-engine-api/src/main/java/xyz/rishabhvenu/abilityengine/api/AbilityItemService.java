package xyz.rishabhvenu.abilityengine.api;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Service for creating and managing ability-backed items.
 */
public interface AbilityItemService {
    /**
     * Creates an item bound to a specific ability.
     * The item will have appropriate metadata and PDC tags.
     * 
     * @param abilityId The ability ID
     * @return The ability item, or null if the ability doesn't exist
     */
    @Nullable
    ItemStack createAbilityItem(String abilityId);
    
    /**
     * Checks if an item is an ability item.
     * 
     * @param item The item to check
     * @return true if the item is bound to any ability
     */
    boolean isAbilityItem(@Nullable ItemStack item);
    
    /**
     * Gets the primary ability ID from an item.
     * 
     * @param item The item
     * @return The ability ID, or null if not an ability item
     */
    @Nullable
    String getAbilityId(@Nullable ItemStack item);
    
    /**
     * Gets all abilities bound to an item (includes primary and additional abilities).
     * 
     * @param item The item
     * @return List of ability IDs (empty if not an ability item)
     */
    List<String> getAbilities(@Nullable ItemStack item);
    
    /**
     * Checks if an item is bound to a specific ability.
     * 
     * @param item The item
     * @param abilityId The ability ID to check
     * @return true if the item contains this ability
     */
    boolean isAbilityItem(@Nullable ItemStack item, String abilityId);
    
    /**
     * Gets the trigger type for a specific ability on an item.
     * For multi-ability items, this returns which trigger activates this ability.
     * 
     * @param item The item
     * @param abilityId The ability ID
     * @return The trigger type, or null if not found
     */
    @Nullable
    TriggerType getAbilityTrigger(@Nullable ItemStack item, String abilityId);
}
