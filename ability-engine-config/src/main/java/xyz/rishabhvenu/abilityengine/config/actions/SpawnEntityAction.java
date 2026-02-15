package xyz.rishabhvenu.abilityengine.config.actions;

import org.bukkit.entity.EntityType;
import xyz.rishabhvenu.abilityengine.api.AbilityContext;
import xyz.rishabhvenu.abilityengine.config.ActionExecutor;

import java.util.Map;

/**
 * Spawns an entity at the player's location.
 */
public final class SpawnEntityAction implements ActionExecutor {
    
    @Override
    public void execute(AbilityContext context, Map<String, Object> params) {
        String entityTypeName = (String) params.get("entity");
        if (entityTypeName == null) {
            return;
        }
        
        EntityType entityType;
        try {
            entityType = EntityType.valueOf(entityTypeName.toUpperCase());
        } catch (IllegalArgumentException e) {
            return;
        }
        
        context.player().getWorld().spawnEntity(
            context.player().getLocation(),
            entityType
        );
    }
}
