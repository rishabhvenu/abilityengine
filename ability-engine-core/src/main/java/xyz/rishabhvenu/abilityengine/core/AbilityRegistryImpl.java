package xyz.rishabhvenu.abilityengine.core;

import xyz.rishabhvenu.abilityengine.api.Ability;
import xyz.rishabhvenu.abilityengine.api.AbilityRegistry;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe implementation of AbilityRegistry using ConcurrentHashMap.
 * Provides O(1) lookup performance.
 */
public final class AbilityRegistryImpl implements AbilityRegistry {
    
    private final ConcurrentHashMap<String, Ability> abilities = new ConcurrentHashMap<>();
    
    @Override
    public void register(Ability ability) {
        if (ability == null) {
            throw new IllegalArgumentException("Cannot register null ability");
        }
        if (ability.id() == null || ability.id().isBlank()) {
            throw new IllegalArgumentException("Ability ID cannot be null or blank");
        }
        abilities.put(ability.id(), ability);
    }
    
    @Override
    public Ability get(String id) {
        return abilities.get(id);
    }
    
    @Override
    public Collection<Ability> getAll() {
        return Collections.unmodifiableCollection(abilities.values());
    }
    
    @Override
    public boolean unregister(String id) {
        return abilities.remove(id) != null;
    }
}
