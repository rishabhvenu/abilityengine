package xyz.rishabhvenu.abilityengine.core;

import org.bukkit.entity.Player;
import xyz.rishabhvenu.abilityengine.api.Ability;
import xyz.rishabhvenu.abilityengine.api.AbilitySession;

import java.util.UUID;

/**
 * Base implementation of AbilitySession with lifecycle tracking.
 */
public abstract class BaseAbilitySession implements AbilitySession {
    
    private final UUID sessionId;
    private final Player player;
    private final Ability ability;
    private boolean active;
    private int tickCount;
    
    protected BaseAbilitySession(Player player, Ability ability) {
        this.sessionId = UUID.randomUUID();
        this.player = player;
        this.ability = ability;
        this.active = false;
        this.tickCount = 0;
    }
    
    @Override
    public UUID sessionId() {
        return sessionId;
    }
    
    @Override
    public Player player() {
        return player;
    }
    
    @Override
    public Ability ability() {
        return ability;
    }
    
    @Override
    public void start() {
        this.active = true;
        this.tickCount = 0;
        onStart();
    }
    
    @Override
    public void tick() {
        if (!active) {
            return;
        }
        tickCount++;
        onTick();
    }
    
    @Override
    public void end() {
        if (!active) {
            return;
        }
        this.active = false;
        onEnd();
    }
    
    @Override
    public boolean isActive() {
        return active;
    }
    
    @Override
    public int getTickCount() {
        return tickCount;
    }
    
    /**
     * Called when the session starts.
     * Override to implement custom start logic.
     */
    protected void onStart() {
        // Default: no-op
    }
    
    /**
     * Called every tick while active.
     * Override to implement custom tick logic.
     */
    protected void onTick() {
        // Default: no-op
    }
    
    /**
     * Called when the session ends.
     * Override to implement cleanup logic.
     */
    protected void onEnd() {
        // Default: no-op
    }
}
