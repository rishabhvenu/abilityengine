package xyz.rishabhvenu.abilityengine.config.actions;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import xyz.rishabhvenu.abilityengine.api.AbilityContext;
import xyz.rishabhvenu.abilityengine.config.ActionExecutor;

import java.util.Map;

/**
 * Sends a message to the player.
 */
public final class SendMessageAction implements ActionExecutor {
    
    @Override
    public void execute(AbilityContext context, Map<String, Object> params) {
        String message = (String) params.get("message");
        if (message == null) {
            return;
        }
        
        Component component = LegacyComponentSerializer.legacyAmpersand().deserialize(message);
        context.player().sendMessage(component);
    }
}
