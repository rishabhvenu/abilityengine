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
    public final String JUMP = TriggerType.JUMP.name();
    public final String LAND = TriggerType.LAND.name();
    public final String DOUBLE_SHIFT = TriggerType.DOUBLE_SHIFT.name();
    public final String HOLD_SHIFT = TriggerType.HOLD_SHIFT.name();
    public final String PROJECTILE_HIT = TriggerType.PROJECTILE_HIT.name();
    public final String KILL_ENTITY = TriggerType.KILL_ENTITY.name();
    public final String ON_JOIN = TriggerType.ON_JOIN.name();
    public final String ON_QUIT = TriggerType.ON_QUIT.name();
    public final String TICK = TriggerType.TICK.name();
    public final String CUSTOM = TriggerType.CUSTOM.name();
    
    TriggerConstants() {}
}
