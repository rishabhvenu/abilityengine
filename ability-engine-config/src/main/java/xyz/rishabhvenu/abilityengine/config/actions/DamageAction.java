package xyz.rishabhvenu.abilityengine.config.actions;

import org.bukkit.entity.LivingEntity;
import xyz.rishabhvenu.abilityengine.api.AbilityContext;
import xyz.rishabhvenu.abilityengine.config.ActionExecutor;

import java.util.Map;

/**
 * Damages the target entity.
 */
public final class DamageAction implements ActionExecutor {
    
    @Override
    public void execute(AbilityContext context, Map<String, Object> params) {
        if (context.targetEntity() == null || !(context.targetEntity() instanceof LivingEntity target)) {
            return;
        }
        
        double damage = params.containsKey("damage") ? ((Number) params.get("damage")).doubleValue() : 1.0;
        target.damage(damage, context.player());
    }
}
