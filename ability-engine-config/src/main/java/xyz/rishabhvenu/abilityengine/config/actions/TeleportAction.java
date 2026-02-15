package xyz.rishabhvenu.abilityengine.config.actions;

import org.bukkit.Location;
import xyz.rishabhvenu.abilityengine.api.AbilityContext;
import xyz.rishabhvenu.abilityengine.config.ActionExecutor;

import java.util.Map;

/**
 * Teleports the player.
 */
public final class TeleportAction implements ActionExecutor {
    
    @Override
    public void execute(AbilityContext context, Map<String, Object> params) {
        if (params.containsKey("x") && params.containsKey("y") && params.containsKey("z")) {
            // Absolute teleport
            double x = ((Number) params.get("x")).doubleValue();
            double y = ((Number) params.get("y")).doubleValue();
            double z = ((Number) params.get("z")).doubleValue();
            
            Location loc = new Location(context.player().getWorld(), x, y, z);
            context.player().teleport(loc);
            
        } else if (params.containsKey("forward")) {
            // Relative teleport forward
            double distance = ((Number) params.get("forward")).doubleValue();
            Location newLoc = context.player().getLocation()
                .add(context.player().getLocation().getDirection().multiply(distance));
            context.player().teleport(newLoc);
        }
    }
}
