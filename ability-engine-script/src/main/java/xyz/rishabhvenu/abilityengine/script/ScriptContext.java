package xyz.rishabhvenu.abilityengine.script;

import org.bukkit.event.HandlerList;
import org.bukkit.plugin.RegisteredListener;
import org.graalvm.polyglot.Context;

import java.util.ArrayList;
import java.util.List;

/**
 * Tracks all resources registered by a single script.
 * Used for clean unload/reload.
 */
public final class ScriptContext {
    
    private final String scriptName;
    private final Context graalContext;
    private final List<String> abilityIds = new ArrayList<>();
    private final List<RegisteredListener> eventListeners = new ArrayList<>();
    private final List<Integer> scheduledTasks = new ArrayList<>();
    
    ScriptContext(String scriptName, Context graalContext) {
        this.scriptName = scriptName;
        this.graalContext = graalContext;
    }
    
    String getScriptName() {
        return scriptName;
    }
    
    Context getGraalContext() {
        return graalContext;
    }
    
    void trackAbility(String abilityId) {
        abilityIds.add(abilityId);
    }
    
    void trackEventListener(RegisteredListener listener) {
        eventListeners.add(listener);
    }
    
    void trackScheduledTask(int taskId) {
        scheduledTasks.add(taskId);
    }
    
    public List<String> getAbilityIds() {
        return new ArrayList<>(abilityIds);
    }
    
    List<RegisteredListener> getEventListeners() {
        return new ArrayList<>(eventListeners);
    }
    
    List<Integer> getScheduledTasks() {
        return new ArrayList<>(scheduledTasks);
    }
    
    void close() {
        // Unregister all event listeners
        for (RegisteredListener listener : eventListeners) {
            HandlerList.unregisterAll(listener.getListener());
        }
        eventListeners.clear();
        
        // Clear tracking (tasks are cancelled by ScriptEngine)
        abilityIds.clear();
        scheduledTasks.clear();
        
        // Close the GraalVM context
        if (graalContext != null) {
            try {
                graalContext.close();
            } catch (Exception e) {
                // Context or engine may already be closed
            }
        }
    }
}
