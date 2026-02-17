package xyz.rishabhvenu.abilityengine.script;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import xyz.rishabhvenu.abilityengine.core.PhaseInstance;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Manages phase/state machine execution for abilities.
 * Parses phase definitions and runs the tick loop.
 */
public final class PhaseBindings {
    
    private final Plugin plugin;
    
    public PhaseBindings(Plugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Starts a phase system for an ability execution.
     * 
     * @param execution The execution instance
     * @param phasesValue The phases object from JS config
     * @param execContext The ability execution context
     * @param graalContext The GraalVM context (for creating JS objects)
     */
    public void startPhases(
            AbilityExecutionInstance execution,
            Value phasesValue,
            AbilityExecContext execContext,
            Context graalContext) {
        
        if (phasesValue == null || !phasesValue.hasMembers()) {
            return;
        }
        
        // Parse phases into a map (LinkedHashMap to preserve order)
        Map<String, PhaseDefinition> phases = new LinkedHashMap<>();
        for (String phaseName : phasesValue.getMemberKeys()) {
            Value phaseConfig = phasesValue.getMember(phaseName);
            phases.put(phaseName, parsePhaseDefinition(phaseName, phaseConfig));
        }
        
        if (phases.isEmpty()) {
            return;
        }
        
        // Get first phase as starting phase
        String startPhaseName = phases.keySet().iterator().next();
        PhaseInstance phaseInstance = new PhaseInstance(startPhaseName);
        execution.setActivePhase(phaseInstance);
        
        // Call onStart for first phase
        PhaseDefinition startPhase = phases.get(startPhaseName);
        if (startPhase.onStart != null && startPhase.onStart.canExecute()) {
            try {
                Value phaseContext = createPhaseContext(graalContext, phaseInstance);
                startPhase.onStart.execute(execContext, phaseContext);
            } catch (Exception e) {
                plugin.getLogger().warning("Error in phase onStart (" + startPhaseName + "): " + e.getMessage());
            }
        }
        
        // Start phase tick loop
        int taskId = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (execution.isCancelled()) {
                return; // Task will be cancelled by execution instance
            }
            
            PhaseInstance currentPhase = (PhaseInstance) execution.getActivePhase();
            if (currentPhase == null) {
                return;
            }
            
            String currentPhaseName = currentPhase.getCurrentPhaseName();
            PhaseDefinition currentPhaseDef = phases.get(currentPhaseName);
            if (currentPhaseDef == null) {
                plugin.getLogger().warning("Unknown phase: " + currentPhaseName);
                return;
            }
            
            currentPhase.incrementTick();
            
            // Call onTick
            if (currentPhaseDef.onTick != null && currentPhaseDef.onTick.canExecute()) {
                try {
                    Value phaseContext = createPhaseContext(graalContext, currentPhase);
                    currentPhaseDef.onTick.execute(execContext, phaseContext);
                } catch (Exception e) {
                    plugin.getLogger().warning("Error in phase onTick (" + currentPhaseName + "): " + e.getMessage());
                }
            }
            
            // Check transition conditions
            boolean shouldTransition = false;
            
            // Check duration
            if (currentPhaseDef.duration > 0 && currentPhase.getTickCount() >= currentPhaseDef.duration) {
                shouldTransition = true;
            }
            
            // Check endWhen condition
            if (!shouldTransition && currentPhaseDef.endWhen != null && currentPhaseDef.endWhen.canExecute()) {
                try {
                    Value phaseContext = createPhaseContext(graalContext, currentPhase);
                    Object result = currentPhaseDef.endWhen.execute(execContext, phaseContext);
                    if (result instanceof Boolean && (Boolean) result) {
                        shouldTransition = true;
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Error in phase endWhen (" + currentPhaseName + "): " + e.getMessage());
                }
            }
            
            // Perform transition if needed
            if (shouldTransition) {
                // Call onEnd for current phase
                if (currentPhaseDef.onEnd != null && currentPhaseDef.onEnd.canExecute()) {
                    try {
                        Value phaseContext = createPhaseContext(graalContext, currentPhase);
                        currentPhaseDef.onEnd.execute(execContext, phaseContext);
                    } catch (Exception e) {
                        plugin.getLogger().warning("Error in phase onEnd (" + currentPhaseName + "): " + e.getMessage());
                    }
                }
                
                // Transition to next phase
                if (currentPhaseDef.next != null && !currentPhaseDef.next.isBlank()) {
                    String nextPhaseName = currentPhaseDef.next;
                    PhaseDefinition nextPhaseDef = phases.get(nextPhaseName);
                    
                    if (nextPhaseDef == null) {
                        plugin.getLogger().warning("Next phase not found: " + nextPhaseName);
                        execution.complete();
                        return;
                    }
                    
                    currentPhase.setCurrentPhaseName(nextPhaseName);
                    
                    // Call onStart for next phase
                    if (nextPhaseDef.onStart != null && nextPhaseDef.onStart.canExecute()) {
                        try {
                            Value phaseContext = createPhaseContext(graalContext, currentPhase);
                            nextPhaseDef.onStart.execute(execContext, phaseContext);
                        } catch (Exception e) {
                            plugin.getLogger().warning("Error in phase onStart (" + nextPhaseName + "): " + e.getMessage());
                        }
                    }
                } else {
                    // No next phase, complete execution
                    execution.complete();
                }
            }
        }, 1L, 1L).getTaskId();
        
        execution.trackTask(taskId);
    }
    
    private PhaseDefinition parsePhaseDefinition(String phaseName, Value config) {
        PhaseDefinition def = new PhaseDefinition();
        def.name = phaseName;
        
        if (config.hasMember("duration") && config.getMember("duration").isNumber()) {
            def.duration = config.getMember("duration").asInt();
        }
        
        if (config.hasMember("onStart")) {
            def.onStart = config.getMember("onStart");
        }
        
        if (config.hasMember("onTick")) {
            def.onTick = config.getMember("onTick");
        }
        
        if (config.hasMember("endWhen")) {
            def.endWhen = config.getMember("endWhen");
        }
        
        if (config.hasMember("onEnd")) {
            def.onEnd = config.getMember("onEnd");
        }
        
        if (config.hasMember("next") && config.getMember("next").isString()) {
            def.next = config.getMember("next").asString();
        }
        
        return def;
    }
    
    private Value createPhaseContext(Context graalContext, PhaseInstance phaseInstance) {
        Value phaseContext = graalContext.eval("js", "({})");
        phaseContext.putMember("name", phaseInstance.getCurrentPhaseName());
        phaseContext.putMember("tick", phaseInstance.getTickCount());
        
        // Add state API
        Value stateProxy = graalContext.eval("js", "({})");
        phaseContext.putMember("state", stateProxy);
        
        // Expose get/set methods that delegate to PhaseInstance
        phaseContext.putMember("get", new java.util.function.Function<String, Object>() {
            @Override
            public Object apply(String key) {
                return phaseInstance.get(key);
            }
        });
        
        phaseContext.putMember("set", new java.util.function.BiConsumer<String, Object>() {
            @Override
            public void accept(String key, Object value) {
                phaseInstance.set(key, value);
            }
        });
        
        return phaseContext;
    }
    
    private static class PhaseDefinition {
        String name;
        int duration = -1;
        Value onStart;
        Value onTick;
        Value endWhen;
        Value onEnd;
        String next;
    }
}
