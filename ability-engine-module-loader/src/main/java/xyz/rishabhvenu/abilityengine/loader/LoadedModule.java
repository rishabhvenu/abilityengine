package xyz.rishabhvenu.abilityengine.loader;

import xyz.rishabhvenu.abilityengine.api.AbilityProvider;

import java.net.URLClassLoader;
import java.util.List;

/**
 * Tracks a loaded external module.
 */
public record LoadedModule(
    String providerId,
    AbilityProvider provider,
    URLClassLoader classLoader,
    List<String> abilityIds
) {}
