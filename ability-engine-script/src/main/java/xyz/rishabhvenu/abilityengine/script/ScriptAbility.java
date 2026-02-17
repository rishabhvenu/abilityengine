package xyz.rishabhvenu.abilityengine.script;

import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.plugin.Plugin;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import xyz.rishabhvenu.abilityengine.api.*;
import xyz.rishabhvenu.abilityengine.core.*;

import java.time.Duration;
import java.util.*;

/**
 * Bridges a JavaScript-defined ability to the Java Ability interface.
 */
final class ScriptAbility implements Ability {
    
    private final String id;
    private final Collection<TriggerType> triggers;
    private final List<Condition> conditions;
    private final Duration cooldown;
    private final String permission;
    private final boolean showBossBar;
    private final String bossBarColor;
    private final String bossBarLabel;
    private final org.graalvm.polyglot.Value executeFunction;
    private final org.graalvm.polyglot.Value onProjectileHit;
    private final org.graalvm.polyglot.Value onProjectileTick;
    private final org.graalvm.polyglot.Value onExpire;
    private final org.graalvm.polyglot.Value onCancel;
    private final org.graalvm.polyglot.Value phasesValue;
    private final org.graalvm.polyglot.Value onInterruptCallback;
    private final Set<InterruptType> interruptTypes;
    
    // Dependencies for creating exec context
    private final ScriptContext scriptContext;
    private final AbilityStateStore stateStore;
    private final Plugin plugin;
    private final BossBarManager bossBarManager;
    private final CooldownManager cooldownManager;
    private final ExecutionTracker executionTracker;
    private final PhaseBindings phaseBindings;
    private final Context graalContext;
    private final EntityControlManager entityControlManager;
    private final InterruptManager interruptManager;
    
    ScriptAbility(
            String id,
            Collection<TriggerType> triggers,
            List<Condition> conditions,
            Duration cooldown,
            String permission,
            boolean showBossBar,
            String bossBarColor,
            String bossBarLabel,
            org.graalvm.polyglot.Value executeFunction,
            org.graalvm.polyglot.Value onProjectileHit,
            org.graalvm.polyglot.Value onProjectileTick,
            org.graalvm.polyglot.Value onExpire,
            org.graalvm.polyglot.Value onCancel,
            org.graalvm.polyglot.Value phasesValue,
            org.graalvm.polyglot.Value onInterruptCallback,
            Set<InterruptType> interruptTypes,
            ScriptContext scriptContext,
            AbilityStateStore stateStore,
            Plugin plugin,
            BossBarManager bossBarManager,
            CooldownManager cooldownManager,
            ExecutionTracker executionTracker,
            PhaseBindings phaseBindings,
            Context graalContext,
            EntityControlManager entityControlManager,
            InterruptManager interruptManager) {
        this.id = id;
        this.triggers = triggers;
        this.conditions = conditions;
        this.cooldown = cooldown;
        this.permission = permission;
        this.showBossBar = showBossBar;
        this.bossBarColor = bossBarColor;
        this.bossBarLabel = bossBarLabel;
        this.executeFunction = executeFunction;
        this.onProjectileHit = onProjectileHit;
        this.onProjectileTick = onProjectileTick;
        this.onExpire = onExpire;
        this.onCancel = onCancel;
        this.phasesValue = phasesValue;
        this.onInterruptCallback = onInterruptCallback;
        this.interruptTypes = interruptTypes;
        this.scriptContext = scriptContext;
        this.stateStore = stateStore;
        this.plugin = plugin;
        this.bossBarManager = bossBarManager;
        this.cooldownManager = cooldownManager;
        this.executionTracker = executionTracker;
        this.phaseBindings = phaseBindings;
        this.graalContext = graalContext;
        this.entityControlManager = entityControlManager;
        this.interruptManager = interruptManager;
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
                // Create execution instance
                AbilityExecutionInstance execution = new AbilityExecutionInstance(
                    id,
                    context.player().getUniqueId(),
                    plugin,
                    executionTracker,
                    context,
                    onInterruptCallback,
                    entityControlManager
                );
                
                // Register interrupts if defined
                if (interruptTypes != null && !interruptTypes.isEmpty()) {
                    interruptManager.registerInterrupts(execution, interruptTypes);
                }
                
                // Wrap in enhanced execution context
                AbilityExecContext execContext = new AbilityExecContext(
                    context,
                    id,
                    scriptContext,
                    stateStore,
                    plugin,
                    cooldownManager,
                    bossBarManager,
                    execution
                );
                
                // Start phases if defined
                if (phasesValue != null && phasesValue.hasMembers()) {
                    phaseBindings.startPhases(execution, phasesValue, execContext, graalContext);
                }
                
                // Execute the main function
                executeFunction.execute(execContext);
                
                // Auto-trigger boss bar if configured
                if (showBossBar && !cooldown.isZero()) {
                    BarColor color = parseBarColor(bossBarColor);
                    bossBarManager.showCooldownBar(
                        plugin,
                        context.player(),
                        id,
                        bossBarLabel,
                        (int) cooldown.toSeconds(),
                        color,
                        BarStyle.SOLID
                    );
                }
            } catch (Exception e) {
                throw new RuntimeException("Error executing script ability " + id, e);
            }
        }
    }
    
    private BarColor parseBarColor(String colorName) {
        try {
            return BarColor.valueOf(colorName.toUpperCase());
        } catch (IllegalArgumentException e) {
            return BarColor.GREEN;
        }
    }
    
    @Override
    public Duration cooldown() {
        return cooldown;
    }
    
    @Override
    public String permission() {
        return permission;
    }
    
    public org.graalvm.polyglot.Value getOnProjectileHit() {
        return onProjectileHit;
    }
    
    public org.graalvm.polyglot.Value getOnProjectileTick() {
        return onProjectileTick;
    }
    
    public org.graalvm.polyglot.Value getOnExpire() {
        return onExpire;
    }
    
    public org.graalvm.polyglot.Value getOnCancel() {
        return onCancel;
    }
}
