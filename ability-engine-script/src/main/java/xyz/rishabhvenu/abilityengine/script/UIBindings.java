package xyz.rishabhvenu.abilityengine.script;

import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import xyz.rishabhvenu.abilityengine.core.BossBarManager;

/**
 * Provides UI utilities for scripts.
 * Exposed as engine.ui
 */
public final class UIBindings {
    
    private final BossBarManager bossBarManager;
    private final Plugin plugin;
    
    UIBindings(BossBarManager bossBarManager, Plugin plugin) {
        this.bossBarManager = bossBarManager;
        this.plugin = plugin;
    }
    
    /**
     * Shows a cooldown boss bar for a player.
     * 
     * @param player The player
     * @param abilityId The ability ID
     * @param durationSeconds Duration in seconds
     * @param label Optional label (defaults to abilityId)
     * @param colorName Optional color name (defaults to "GREEN")
     */
    public void cooldownBar(Player player, String abilityId, int durationSeconds, 
                            String label, String colorName) {
        String actualLabel = label != null ? label : abilityId;
        BarColor color = parseColor(colorName != null ? colorName : "GREEN");
        
        bossBarManager.showCooldownBar(plugin, player, abilityId, actualLabel, 
                                       durationSeconds, color, BarStyle.SOLID);
    }
    
    /**
     * Removes a boss bar for a player and ability.
     */
    public void removeBar(Player player, String abilityId) {
        bossBarManager.removeBar(player, abilityId);
    }
    
    private BarColor parseColor(String colorName) {
        try {
            return BarColor.valueOf(colorName.toUpperCase());
        } catch (IllegalArgumentException e) {
            return BarColor.GREEN;
        }
    }
}
