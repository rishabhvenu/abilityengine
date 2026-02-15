package xyz.rishabhvenu.abilityengine.api;

/**
 * Extended interface for external modules that need lifecycle management.
 * Extends AbilityProvider with onEnable/onDisable hooks.
 * 
 * Modules should be declared in META-INF/services/xyz.rishabhvenu.abilityengine.api.AbilityProvider
 * for ServiceLoader discovery.
 */
public interface AbilityModule extends AbilityProvider {
    /**
     * Called when the module is loaded and enabled.
     * Use this to initialize resources, register listeners, or set up state.
     * 
     * @param registry The ability registry for registering abilities dynamically
     * @param cooldowns The cooldown manager
     * @param items The ability item service
     */
    void onEnable(AbilityRegistry registry, CooldownManager cooldowns, AbilityItemService items);
    
    /**
     * Called when the module is disabled (plugin shutdown or reload).
     * Use this to clean up resources, unregister listeners, cancel tasks, etc.
     */
    void onDisable();
    
    /**
     * Returns the human-readable module name.
     * Defaults to the provider ID.
     * 
     * @return Module name
     */
    default String getModuleName() {
        return getProviderId();
    }
    
    /**
     * Returns the module version.
     * 
     * @return Module version string
     */
    default String getModuleVersion() {
        return "1.0.0";
    }
}
