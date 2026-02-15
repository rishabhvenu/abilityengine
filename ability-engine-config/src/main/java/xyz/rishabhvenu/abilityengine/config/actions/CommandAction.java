package xyz.rishabhvenu.abilityengine.config.actions;

import org.bukkit.Bukkit;
import xyz.rishabhvenu.abilityengine.api.AbilityContext;
import xyz.rishabhvenu.abilityengine.config.ActionExecutor;

import java.util.Map;

/**
 * Executes a console command.
 */
public final class CommandAction implements ActionExecutor {
    
    @Override
    public void execute(AbilityContext context, Map<String, Object> params) {
        String command = (String) params.get("command");
        if (command == null) {
            return;
        }
        
        // Replace placeholders
        command = command.replace("{player}", context.player().getName());
        
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
    }
}
