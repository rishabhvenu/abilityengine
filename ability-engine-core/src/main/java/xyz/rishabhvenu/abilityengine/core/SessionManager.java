package xyz.rishabhvenu.abilityengine.core;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import xyz.rishabhvenu.abilityengine.api.AbilitySession;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Manages active ability sessions.
 * Runs a tick loop and handles cleanup.
 */
public final class SessionManager implements Listener {
    
    private final Plugin plugin;
    private final Logger logger;
    private final Map<UUID, List<AbilitySession>> activeSessions = new ConcurrentHashMap<>();
    private BukkitTask tickTask;
    
    public SessionManager(Plugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }
    
    /**
     * Starts the session tick loop.
     * Should be called during plugin enable.
     */
    public void start() {
        if (tickTask != null) {
            logger.warning("SessionManager already started");
            return;
        }
        
        // Run every tick (1 tick = 50ms)
        tickTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tickAllSessions, 1L, 1L);
        Bukkit.getPluginManager().registerEvents(this, plugin);
        logger.info("SessionManager started");
    }
    
    /**
     * Stops the session tick loop and cleans up all sessions.
     * Should be called during plugin disable.
     */
    public void stop() {
        if (tickTask != null) {
            tickTask.cancel();
            tickTask = null;
        }
        
        // End all active sessions
        for (List<AbilitySession> sessions : activeSessions.values()) {
            for (AbilitySession session : sessions) {
                try {
                    session.end();
                } catch (Exception e) {
                    logger.severe("Error ending session " + session.sessionId() + ": " + e.getMessage());
                }
            }
        }
        
        activeSessions.clear();
        logger.info("SessionManager stopped");
    }
    
    /**
     * Registers a new session and starts it.
     * 
     * @param session The session to start
     */
    public void startSession(AbilitySession session) {
        UUID playerId = session.player().getUniqueId();
        activeSessions.computeIfAbsent(playerId, k -> new ArrayList<>()).add(session);
        
        try {
            session.start();
        } catch (Exception e) {
            logger.severe("Error starting session " + session.sessionId() + ": " + e.getMessage());
            activeSessions.get(playerId).remove(session);
        }
    }
    
    /**
     * Ends a specific session.
     * 
     * @param session The session to end
     */
    public void endSession(AbilitySession session) {
        UUID playerId = session.player().getUniqueId();
        List<AbilitySession> sessions = activeSessions.get(playerId);
        
        if (sessions != null) {
            sessions.remove(session);
            if (sessions.isEmpty()) {
                activeSessions.remove(playerId);
            }
        }
        
        try {
            session.end();
        } catch (Exception e) {
            logger.severe("Error ending session " + session.sessionId() + ": " + e.getMessage());
        }
    }
    
    /**
     * Gets all active sessions for a player.
     * 
     * @param player The player
     * @return List of active sessions (empty if none)
     */
    public List<AbilitySession> getActiveSessions(Player player) {
        List<AbilitySession> sessions = activeSessions.get(player.getUniqueId());
        return sessions != null ? new ArrayList<>(sessions) : Collections.emptyList();
    }
    
    /**
     * Ends all sessions for a player.
     * 
     * @param player The player
     */
    public void endAllSessions(Player player) {
        List<AbilitySession> sessions = activeSessions.remove(player.getUniqueId());
        if (sessions != null) {
            for (AbilitySession session : sessions) {
                try {
                    session.end();
                } catch (Exception e) {
                    logger.severe("Error ending session " + session.sessionId() + ": " + e.getMessage());
                }
            }
        }
    }
    
    private void tickAllSessions() {
        Iterator<Map.Entry<UUID, List<AbilitySession>>> iterator = activeSessions.entrySet().iterator();
        
        while (iterator.hasNext()) {
            Map.Entry<UUID, List<AbilitySession>> entry = iterator.next();
            List<AbilitySession> sessions = entry.getValue();
            
            // Use regular iterator to allow removal during iteration
            Iterator<AbilitySession> sessionIterator = sessions.iterator();
            while (sessionIterator.hasNext()) {
                AbilitySession session = sessionIterator.next();
                
                try {
                    if (!session.isActive()) {
                        // Session ended itself, remove it
                        sessionIterator.remove();
                        continue;
                    }
                    
                    // Check if player is still online
                    if (!session.player().isOnline()) {
                        session.end();
                        sessionIterator.remove();
                        continue;
                    }
                    
                    session.tick();
                } catch (Exception e) {
                    logger.severe("Error ticking session " + session.sessionId() + ": " + e.getMessage());
                    // End the session on error to prevent repeated exceptions
                    try {
                        session.end();
                    } catch (Exception endError) {
                        // Ignore errors during error cleanup
                    }
                    sessionIterator.remove();
                }
            }
            
            // Remove player entry if no sessions remain
            if (sessions.isEmpty()) {
                iterator.remove();
            }
        }
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        endAllSessions(event.getPlayer());
    }
}
