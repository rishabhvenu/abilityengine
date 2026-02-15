package xyz.rishabhvenu.abilityengine.config.actions;

import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import xyz.rishabhvenu.abilityengine.api.AbilityContext;
import xyz.rishabhvenu.abilityengine.config.ActionExecutor;

import java.util.Map;

/**
 * Applies a potion effect to the player or target.
 */
public final class PotionEffectAction implements ActionExecutor {
    
    @Override
    public void execute(AbilityContext context, Map<String, Object> params) {
        String effectName = (String) params.get("effect");
        if (effectName == null) {
            return;
        }
        
        PotionEffectType effectType = PotionEffectType.getByName(effectName.toUpperCase());
        if (effectType == null) {
            return;
        }
        
        int duration = params.containsKey("duration") ? ((Number) params.get("duration")).intValue() : 100;
        int amplifier = params.containsKey("amplifier") ? ((Number) params.get("amplifier")).intValue() : 0;
        
        PotionEffect effect = new PotionEffect(effectType, duration, amplifier);
        
        // Apply to target if present and is living entity, otherwise to player
        if (context.targetEntity() instanceof LivingEntity target) {
            target.addPotionEffect(effect);
        } else {
            context.player().addPotionEffect(effect);
        }
    }
}
