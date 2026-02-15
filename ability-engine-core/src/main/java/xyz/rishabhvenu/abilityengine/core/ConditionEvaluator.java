package xyz.rishabhvenu.abilityengine.core;

import xyz.rishabhvenu.abilityengine.api.AbilityContext;
import xyz.rishabhvenu.abilityengine.api.Condition;

import java.util.List;

/**
 * Evaluates conditions against an ability context.
 * Uses AND logic - all conditions must pass.
 */
public final class ConditionEvaluator {
    
    private ConditionEvaluator() {
        throw new UnsupportedOperationException("Utility class");
    }
    
    /**
     * Evaluates all conditions against the context.
     * 
     * @param conditions List of conditions to evaluate
     * @param context The ability context
     * @return true if all conditions pass (or list is empty)
     */
    public static boolean evaluate(List<Condition> conditions, AbilityContext context) {
        if (conditions == null || conditions.isEmpty()) {
            return true;
        }
        
        for (Condition condition : conditions) {
            try {
                if (!condition.test(context)) {
                    return false;
                }
            } catch (Exception e) {
                // Condition threw exception - treat as failure
                // This prevents a buggy condition from crashing the server
                return false;
            }
        }
        
        return true;
    }
}
