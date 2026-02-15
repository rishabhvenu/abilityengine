package xyz.rishabhvenu.abilityengine.api;

import java.util.Collection;

/**
 * Interface for external modules to provide abilities.
 * Implementations will be discovered and loaded by the module loader (Phase 2).
 */
public interface AbilityProvider {
    /**
     * Returns the collection of abilities provided by this provider.
     * Called once during plugin initialization.
     * 
     * @return Collection of abilities to register
     */
    Collection<Ability> getAbilities();
    
    /**
     * Returns a unique identifier for this provider.
     * 
     * @return Provider ID
     */
    String getProviderId();
}
