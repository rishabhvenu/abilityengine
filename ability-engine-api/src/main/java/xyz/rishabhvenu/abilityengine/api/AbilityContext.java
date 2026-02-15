package xyz.rishabhvenu.abilityengine.api;

import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Context passed to an ability when it executes.
 * Contains all relevant information about the trigger event.
 * 
 * @param player The player who triggered the ability
 * @param trigger The type of trigger that fired
 * @param targetEntity The target entity if applicable (e.g., entity clicks)
 * @param targetBlock The target block if applicable (e.g., block clicks)
 * @param item The item held when the ability was triggered
 * @param event The raw Bukkit event (for advanced use, may be null)
 */
public record AbilityContext(
    Player player,
    TriggerType trigger,
    @Nullable Entity targetEntity,
    @Nullable Block targetBlock,
    @Nullable ItemStack item,
    @Nullable Event event
) {
    /**
     * Creates a basic context with just player and trigger.
     */
    public static AbilityContext of(Player player, TriggerType trigger) {
        return new AbilityContext(player, trigger, null, null, null, null);
    }
}
