package xyz.rishabhvenu.abilityengine.script;

import xyz.rishabhvenu.abilityengine.api.TriggerType;

/**
 * Provides trigger type constants for scripts.
 * Exposed as engine.trigger
 */
public final class TriggerConstants {
    
    public final String RIGHT_CLICK = TriggerType.RIGHT_CLICK.name();
    public final String LEFT_CLICK = TriggerType.LEFT_CLICK.name();
    public final String SHIFT_RIGHT_CLICK = TriggerType.SHIFT_RIGHT_CLICK.name();
    public final String SHIFT_LEFT_CLICK = TriggerType.SHIFT_LEFT_CLICK.name();
    public final String RIGHT_CLICK_ENTITY = TriggerType.RIGHT_CLICK_ENTITY.name();
    public final String LEFT_CLICK_ENTITY = TriggerType.LEFT_CLICK_ENTITY.name();
    public final String SHIFT_RIGHT_CLICK_ENTITY = TriggerType.SHIFT_RIGHT_CLICK_ENTITY.name();
    public final String SHIFT_LEFT_CLICK_ENTITY = TriggerType.SHIFT_LEFT_CLICK_ENTITY.name();
    public final String DAMAGE_DEALT = TriggerType.DAMAGE_DEALT.name();
    public final String DAMAGE_TAKEN = TriggerType.DAMAGE_TAKEN.name();
    public final String MOVE = TriggerType.MOVE.name();
    public final String TICK = TriggerType.TICK.name();
    
    TriggerConstants() {}
}
