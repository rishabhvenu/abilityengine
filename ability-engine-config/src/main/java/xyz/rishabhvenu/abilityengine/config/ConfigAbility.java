package xyz.rishabhvenu.abilityengine.config;

import xyz.rishabhvenu.abilityengine.api.*;
import xyz.rishabhvenu.abilityengine.config.actions.*;

import java.time.Duration;
import java.util.*;

/**
 * Ability implementation that executes actions defined in configuration.
 */
public final class ConfigAbility implements Ability {
    
    private final String id;
    private final String displayName;
    private final Collection<TriggerType> triggers;
    private final List<Condition> conditions;
    private final Duration cooldown;
    private final List<ConfigAction> actions;
    
    private static final Map<ActionType, ActionExecutor> ACTION_EXECUTORS = new EnumMap<>(ActionType.class);
    
    static {
        ACTION_EXECUTORS.put(ActionType.LAUNCH_PROJECTILE, new LaunchProjectileAction());
        ACTION_EXECUTORS.put(ActionType.SEND_MESSAGE, new SendMessageAction());
        ACTION_EXECUTORS.put(ActionType.PLAY_SOUND, new PlaySoundAction());
        ACTION_EXECUTORS.put(ActionType.PLAY_EFFECT, new PlayEffectAction());
        ACTION_EXECUTORS.put(ActionType.DAMAGE, new DamageAction());
        ACTION_EXECUTORS.put(ActionType.HEAL, new HealAction());
        ACTION_EXECUTORS.put(ActionType.VELOCITY, new VelocityAction());
        ACTION_EXECUTORS.put(ActionType.POTION_EFFECT, new PotionEffectAction());
        ACTION_EXECUTORS.put(ActionType.TELEPORT, new TeleportAction());
        ACTION_EXECUTORS.put(ActionType.SPAWN_ENTITY, new SpawnEntityAction());
        ACTION_EXECUTORS.put(ActionType.COMMAND, new CommandAction());
    }
    
    public ConfigAbility(
            String id,
            String displayName,
            Collection<TriggerType> triggers,
            List<Condition> conditions,
            Duration cooldown,
            List<ConfigAction> actions) {
        this.id = id;
        this.displayName = displayName;
        this.triggers = triggers;
        this.conditions = conditions;
        this.cooldown = cooldown;
        this.actions = actions;
    }
    
    @Override
    public String id() {
        return id;
    }
    
    public String displayName() {
        return displayName;
    }
    
    @Override
    public Collection<TriggerType> triggers() {
        return triggers;
    }
    
    @Override
    public List<Condition> conditions() {
        return conditions;
    }
    
    @Override
    public void execute(AbilityContext context) {
        for (ConfigAction action : actions) {
            ActionExecutor executor = ACTION_EXECUTORS.get(action.type());
            if (executor != null) {
                try {
                    executor.execute(context, action.params());
                } catch (Exception e) {
                    // Log but continue executing other actions
                    System.err.println("Error executing action " + action.type() + ": " + e.getMessage());
                }
            }
        }
    }
    
    @Override
    public Duration cooldown() {
        return cooldown;
    }
    
    /**
     * Represents a configured action.
     */
    public record ConfigAction(ActionType type, Map<String, Object> params) {}
}
