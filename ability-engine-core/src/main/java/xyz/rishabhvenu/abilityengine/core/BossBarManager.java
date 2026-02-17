package xyz.rishabhvenu.abilityengine.core;

import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages boss bars for cooldown display.
 * Auto-syncs boss bar progress with cooldown timers.
 */
public final class BossBarManager {
    
    // Map: "playerUUID:abilityId" -> ActiveBar
    private final Map<String, ActiveBar> activeBars = new ConcurrentHashMap<>();
    
    /**
     * Shows a cooldown boss bar for a player and ability.
     * The boss bar's progress fills from 0 to 1 over the duration.
     * 
     * @param plugin The plugin instance
     * @param player The player
     * @param abilityId The ability ID
     * @param label The label to display
     * @param durationSeconds Duration in seconds
     * @param color Boss bar color
     * @param style Boss bar style
     */
    public void showCooldownBar(Plugin plugin, Player player, String abilityId,
                                 String label, int durationSeconds,
                                 BarColor color, BarStyle style) {
        String key = makeKey(player.getUniqueId(), abilityId);
        
        // Remove existing bar if present
        ActiveBar existing = activeBars.remove(key);
        if (existing != null) {
            existing.bar.removeAll();
            Bukkit.getScheduler().cancelTask(existing.taskId);
        }
        
        // Create new boss bar
        BossBar bar = Bukkit.createBossBar(label, color, style);
        bar.addPlayer(player);
        bar.setProgress(0.0);
        
        int totalTicks = durationSeconds * 20;
        final int[] elapsed = {0};
        
        int taskId = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            elapsed[0]++;
            double progress = Math.min((double) elapsed[0] / totalTicks, 1.0);
            bar.setProgress(progress);
            
            if (elapsed[0] >= totalTicks || !player.isOnline()) {
                bar.removeAll();
                Bukkit.getScheduler().cancelTask(activeBars.get(key).taskId);
                activeBars.remove(key);
            }
        }, 1L, 1L).getTaskId();
        
        activeBars.put(key, new ActiveBar(bar, taskId));
    }
    
    /**
     * Removes a boss bar for a specific player and ability.
     * 
     * @param player The player
     * @param abilityId The ability ID
     */
    public void removeBar(Player player, String abilityId) {
        String key = makeKey(player.getUniqueId(), abilityId);
        ActiveBar existing = activeBars.remove(key);
        if (existing != null) {
            existing.bar.removeAll();
            Bukkit.getScheduler().cancelTask(existing.taskId);
        }
    }
    
    /**
     * Removes all boss bars for a specific player.
     * Called on player quit.
     * 
     * @param playerId The player UUID
     */
    public void removeAllBars(UUID playerId) {
        String playerPrefix = playerId.toString() + ":";
        activeBars.entrySet().removeIf(entry -> {
            if (entry.getKey().startsWith(playerPrefix)) {
                entry.getValue().bar.removeAll();
                Bukkit.getScheduler().cancelTask(entry.getValue().taskId);
                return true;
            }
            return false;
        });
    }
    
    private String makeKey(UUID playerId, String abilityId) {
        return playerId.toString() + ":" + abilityId;
    }
    
    private record ActiveBar(BossBar bar, int taskId) {}
}
