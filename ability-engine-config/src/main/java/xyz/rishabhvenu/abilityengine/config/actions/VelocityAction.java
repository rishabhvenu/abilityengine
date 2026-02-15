package xyz.rishabhvenu.abilityengine.config.actions;

import org.bukkit.util.Vector;
import xyz.rishabhvenu.abilityengine.api.AbilityContext;
import xyz.rishabhvenu.abilityengine.config.ActionExecutor;

import java.util.Map;

/**
 * Applies velocity to the player or target.
 */
public final class VelocityAction implements ActionExecutor {
    
    @Override
    public void execute(AbilityContext context, Map<String, Object> params) {
        double x = params.containsKey("x") ? ((Number) params.get("x")).doubleValue() : 0.0;
        double y = params.containsKey("y") ? ((Number) params.get("y")).doubleValue() : 0.0;
        double z = params.containsKey("z") ? ((Number) params.get("z")).doubleValue() : 0.0;
        
        Vector velocity = new Vector(x, y, z);
        
        // Apply to target if present, otherwise to player
        if (context.targetEntity() != null) {
            context.targetEntity().setVelocity(velocity);
        } else {
            context.player().setVelocity(velocity);
        }
    }
}
