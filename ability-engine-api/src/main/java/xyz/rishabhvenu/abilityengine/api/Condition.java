package xyz.rishabhvenu.abilityengine.api;

/**
 * A condition that can be evaluated against an ability context.
 * Conditions determine whether an ability can execute.
 */
@FunctionalInterface
public interface Condition {
    /**
     * Tests this condition against the given context.
     * 
     * @param context The ability execution context
     * @return true if the condition passes, false otherwise
     */
    boolean test(AbilityContext context);
}
