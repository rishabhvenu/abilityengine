package xyz.rishabhvenu.abilityengine.api;

import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * Registry for managing abilities.
 * Provides O(1) lookup by ID.
 */
public interface AbilityRegistry {
    /**
     * Registers an ability.
     * If an ability with the same ID already exists, it will be replaced.
     * 
     * @param ability The ability to register
     */
    void register(Ability ability);
    
    /**
     * Gets an ability by its ID.
     * 
     * @param id The ability ID
     * @return The ability, or null if not found
     */
    @Nullable
    Ability get(String id);
    
    /**
     * Returns all registered abilities.
     * 
     * @return Immutable collection of all abilities
     */
    Collection<Ability> getAll();
    
    /**
     * Unregisters an ability by its ID.
     * 
     * @param id The ability ID
     * @return true if the ability was removed, false if it didn't exist
     */
    boolean unregister(String id);
    
    /**
     * Checks if an ability is registered.
     * 
     * @param id The ability ID
     * @return true if registered
     */
    default boolean isRegistered(String id) {
        return get(id) != null;
    }
}
