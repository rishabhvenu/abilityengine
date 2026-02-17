package xyz.rishabhvenu.abilityengine.script;

import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.plugin.Plugin;
import xyz.rishabhvenu.abilityengine.api.*;
import xyz.rishabhvenu.abilityengine.core.AbilityStateStore;
import xyz.rishabhvenu.abilityengine.core.BossBarManager;

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
    private final String permission;
    private final boolean showBossBar;
    private final String bossBarColor;
    private final String bossBarLabel;
    private final org.graalvm.polyglot.Value executeFunction;
    private final org.graalvm.polyglot.Value onProjectileHit;
    private final org.graalvm.polyglot.Value onProjectileTick;
    private final org.graalvm.polyglot.Value onExpire;
    private final org.graalvm.polyglot.Value onCancel;
    
    // Dependencies for creating exec context
    private final ScriptContext scriptContext;
    private final AbilityStateStore stateStore;
    private final Plugin plugin;
    private final BossBarManager bossBarManager;
    
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
            ScriptContext scriptContext,
            AbilityStateStore stateStore,
            Plugin plugin,
            BossBarManager bossBarManager) {
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
        this.scriptContext = scriptContext;
        this.stateStore = stateStore;
        this.plugin = plugin;
        this.bossBarManager = bossBarManager;
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
                // Wrap in enhanced execution context
                AbilityExecContext execContext = new AbilityExecContext(
                    context,
                    id,
                    scriptContext,
                    stateStore,
                    plugin
                );
                
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
