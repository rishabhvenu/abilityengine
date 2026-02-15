package xyz.rishabhvenu.abilityengine.config.actions;

import xyz.rishabhvenu.abilityengine.api.AbilityContext;
import xyz.rishabhvenu.abilityengine.config.ActionExecutor;

import java.util.Map;

/**
 * Heals the player.
 */
public final class HealAction implements ActionExecutor {
    
    @Override
    public void execute(AbilityContext context, Map<String, Object> params) {
        double amount = params.containsKey("amount") ? ((Number) params.get("amount")).doubleValue() : 1.0;
        
        double newHealth = Math.min(
            context.player().getHealth() + amount,
            context.player().getMaxHealth()
        );
        
        context.player().setHealth(newHealth);
    }
}
