package xyz.rishabhvenu.abilityengine.script;

import xyz.rishabhvenu.abilityengine.api.*;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Bridges a JavaScript-defined ability to the Java Ability interface.
 */
final class ScriptAbility implements Ability {
    
    private final String id;
    private final Collection<TriggerType> triggers;
    private final List<Condition> conditions;
    private final Duration cooldown;
    private final org.graalvm.polyglot.Value executeFunction;
    
    ScriptAbility(
            String id,
            Collection<TriggerType> triggers,
            List<Condition> conditions,
            Duration cooldown,
            org.graalvm.polyglot.Value executeFunction) {
        this.id = id;
        this.triggers = triggers;
        this.conditions = conditions;
        this.cooldown = cooldown;
        this.executeFunction = executeFunction;
    }
    
    @Override
    public String id() {
        return id;
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
        if (executeFunction != null && executeFunction.canExecute()) {
            try {
                executeFunction.execute(context);
            } catch (Exception e) {
                throw new RuntimeException("Error executing script ability " + id, e);
            }
        }
    }
    
    @Override
    public Duration cooldown() {
        return cooldown;
    }
}
