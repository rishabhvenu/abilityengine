package xyz.rishabhvenu.abilityengine.script;

import org.bukkit.entity.Player;
import org.graalvm.polyglot.Value;
import xyz.rishabhvenu.abilityengine.api.Ability;
import xyz.rishabhvenu.abilityengine.api.AbilitySession;
import xyz.rishabhvenu.abilityengine.core.BaseAbilitySession;
import xyz.rishabhvenu.abilityengine.core.SessionManager;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Provides session management API for scripts.
 * Exposed as engine.sessions
 */
public final class SessionBindings {
    
    private final SessionManager sessionManager;
    
    SessionBindings(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }
    
    /**
     * Starts a new session.
     * 
     * @param player The player
     * @param ability The ability
     * @param handlers JS object with onStart, onTick, onEnd functions
     */
    public void start(Player player, Ability ability, Value handlers) {
        if (!handlers.hasMembers()) {
            return;
        }
        
        Value onStartFunc = handlers.getMember("onStart");
        Value onTickFunc = handlers.getMember("onTick");
        Value onEndFunc = handlers.getMember("onEnd");
        
        AbilitySession session = new BaseAbilitySession(player, ability) {
            @Override
            protected void onStart() {
                if (onStartFunc != null && onStartFunc.canExecute()) {
                    try {
                        onStartFunc.execute();
                    } catch (Exception e) {
                        // Ignore script errors
                    }
                }
            }
            
            @Override
            protected void onTick() {
                if (onTickFunc != null && onTickFunc.canExecute()) {
                    try {
                        onTickFunc.execute(getTickCount());
                    } catch (Exception e) {
                        // Ignore script errors
                    }
                }
            }
            
            @Override
            protected void onEnd() {
                if (onEndFunc != null && onEndFunc.canExecute()) {
                    try {
                        onEndFunc.execute();
                    } catch (Exception e) {
                        // Ignore script errors
                    }
                }
            }
        };
        
        sessionManager.startSession(session);
    }
    
    /**
     * Ends all sessions for a player running a specific ability.
     * 
     * @param player The player
     * @param abilityId The ability ID
     */
    public void end(Player player, String abilityId) {
        List<AbilitySession> sessions = sessionManager.getActiveSessions(player);
        for (AbilitySession session : sessions) {
            if (session.ability().id().equals(abilityId)) {
                sessionManager.endSession(session);
            }
        }
    }
    
    /**
     * Gets all active sessions for a player.
     * Returns an array of ability IDs.
     * 
     * @param player The player
     * @return Array of ability IDs
     */
    public String[] getActive(Player player) {
        return sessionManager.getActiveSessions(player).stream()
            .map(session -> session.ability().id())
            .toArray(String[]::new);
    }
}
