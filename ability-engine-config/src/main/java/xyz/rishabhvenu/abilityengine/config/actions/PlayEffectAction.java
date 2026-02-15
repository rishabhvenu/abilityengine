package xyz.rishabhvenu.abilityengine.config.actions;

import org.bukkit.Particle;
import xyz.rishabhvenu.abilityengine.api.AbilityContext;
import xyz.rishabhvenu.abilityengine.config.ActionExecutor;

import java.util.Map;

/**
 * Plays a particle effect at the player's location.
 */
public final class PlayEffectAction implements ActionExecutor {
    
    @Override
    public void execute(AbilityContext context, Map<String, Object> params) {
        String particleName = (String) params.get("particle");
        if (particleName == null) {
            return;
        }
        
        Particle particle;
        try {
            particle = Particle.valueOf(particleName.toUpperCase());
        } catch (IllegalArgumentException e) {
            return;
        }
        
        int count = params.containsKey("count") ? ((Number) params.get("count")).intValue() : 1;
        
        context.player().getWorld().spawnParticle(
            particle,
            context.player().getLocation(),
            count
        );
    }
}
