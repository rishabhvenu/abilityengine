package xyz.rishabhvenu.abilityengine.config.actions;

import org.bukkit.entity.EntityType;
import org.bukkit.entity.Projectile;
import org.bukkit.util.Vector;
import xyz.rishabhvenu.abilityengine.api.AbilityContext;
import xyz.rishabhvenu.abilityengine.config.ActionExecutor;

import java.util.Map;

/**
 * Launches a projectile from the player.
 */
public final class LaunchProjectileAction implements ActionExecutor {
    
    @Override
    public void execute(AbilityContext context, Map<String, Object> params) {
        String projectileTypeName = (String) params.get("projectile");
        if (projectileTypeName == null) {
            return;
        }
        
        EntityType projectileType;
        try {
            projectileType = EntityType.valueOf(projectileTypeName.toUpperCase());
        } catch (IllegalArgumentException e) {
            return;
        }
        
        double speed = params.containsKey("speed") ? ((Number) params.get("speed")).doubleValue() : 1.0;
        
        Vector direction = context.player().getLocation().getDirection();
        Projectile projectile = context.player().launchProjectile(
            (Class<? extends Projectile>) projectileType.getEntityClass(),
            direction.multiply(speed)
        );
        
        projectile.setShooter(context.player());
    }
}
