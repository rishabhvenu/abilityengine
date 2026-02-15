package xyz.rishabhvenu.abilityengine.config.actions;

import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import xyz.rishabhvenu.abilityengine.api.AbilityContext;
import xyz.rishabhvenu.abilityengine.config.ActionExecutor;

import java.util.Map;

/**
 * Plays a sound at the player's location.
 */
public final class PlaySoundAction implements ActionExecutor {
    
    @Override
    public void execute(AbilityContext context, Map<String, Object> params) {
        String soundName = (String) params.get("sound");
        if (soundName == null) {
            return;
        }
        
        Sound sound;
        try {
            sound = Sound.valueOf(soundName.toUpperCase());
        } catch (IllegalArgumentException e) {
            return;
        }
        
        float volume = params.containsKey("volume") ? ((Number) params.get("volume")).floatValue() : 1.0f;
        float pitch = params.containsKey("pitch") ? ((Number) params.get("pitch")).floatValue() : 1.0f;
        
        context.player().playSound(
            context.player().getLocation(),
            sound,
            SoundCategory.PLAYERS,
            volume,
            pitch
        );
    }
}
