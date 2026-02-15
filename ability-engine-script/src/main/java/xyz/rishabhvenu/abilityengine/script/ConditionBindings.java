package xyz.rishabhvenu.abilityengine.script;

import org.bukkit.entity.Player;
import org.graalvm.polyglot.Value;
import xyz.rishabhvenu.abilityengine.api.Condition;
import xyz.rishabhvenu.abilityengine.api.Conditions;

/**
 * Provides condition builders for scripts.
 * Exposed as engine.condition
 */
public final class ConditionBindings {
    
    public Condition sneaking() {
        return Conditions.sneaking();
    }
    
    public Condition notSneaking() {
        return Conditions.notSneaking();
    }
    
    public Condition healthAbove(double threshold) {
        return Conditions.healthAbove(threshold);
    }
    
    public Condition healthBelow(double threshold) {
        return Conditions.healthBelow(threshold);
    }
    
    public Condition yAbove(double y) {
        return Conditions.yAbove(y);
    }
    
    public Condition yBelow(double y) {
        return Conditions.yBelow(y);
    }
    
    public Condition hasTarget() {
        return Conditions.hasTarget();
    }
    
    public Condition custom(Value jsFunction) {
        return context -> {
            if (jsFunction.canExecute()) {
                Object result = jsFunction.execute(context);
                if (result instanceof Boolean) {
                    return (Boolean) result;
                }
            }
            return false;
        };
    }
    
    ConditionBindings() {}
}
