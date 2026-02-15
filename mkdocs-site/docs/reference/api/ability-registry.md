# AbilityRegistry Interface

Interface for registering and managing abilities.

---

## Package

```java
xyz.rishabhvenu.abilityengine.api
```

## Interface Declaration

```java
public interface AbilityRegistry
```

---

## Overview

`AbilityRegistry` provides O(1) ability lookup and management. All abilities (YAML, scripts, Java modules) register through this interface.

**Thread-safe**: Yes

**Implementation**: `AbilityRegistryImpl` (internal)

---

## Methods

### `register(Ability)`

```java
void register(Ability ability)
```

Registers an ability. If an ability with the same ID already exists, it will be replaced.

**Parameters**: `ability` - The ability to register

**Example**:

```java
AbilityRegistry registry = getRegistry();
Ability ability = new FireballAbility();
registry.register(ability);
```

---

### `get(String)`

```java
@Nullable
Ability get(String id)
```

Gets an ability by its ID.

**Parameters**: `id` - The ability ID

**Returns**: The ability, or `null` if not found

**Example**:

```java
Ability ability = registry.get("fireball");
if (ability != null) {
    // Use ability
}
```

---

### `getAll()`

```java
Collection<Ability> getAll()
```

Returns all registered abilities.

**Returns**: Immutable collection of all abilities

**Example**:

```java
for (Ability ability : registry.getAll()) {
    System.out.println(ability.id());
}
```

---

### `unregister(String)`

```java
boolean unregister(String id)
```

Unregisters an ability by its ID.

**Parameters**: `id` - The ability ID

**Returns**: `true` if the ability was removed, `false` if it didn't exist

**Example**:

```java
if (registry.unregister("fireball")) {
    System.out.println("Ability unregistered");
}
```

---

### `isRegistered(String)`

```java
default boolean isRegistered(String id)
```

Checks if an ability is registered.

**Parameters**: `id` - The ability ID

**Returns**: `true` if registered

**Implementation**:

```java
default boolean isRegistered(String id) {
    return get(id) != null;
}
```

---

## Usage Examples

### Basic Registration

```java
AbilityRegistry registry = getRegistry();

// Register
registry.register(new FireballAbility());
registry.register(new HealAbility());
registry.register(new DashAbility());

// List all
System.out.println("Registered abilities:");
for (Ability ability : registry.getAll()) {
    System.out.println("- " + ability.id());
}
```

### Dynamic Registration

```java
public class DynamicLoader {
    public void loadAbilities(AbilityRegistry registry, Config config) {
        for (var abilityConfig : config.getAbilities()) {
            Ability ability = createFromConfig(abilityConfig);
            registry.register(ability);
        }
    }
}
```

### Replacing Abilities

```java
// Register v1
registry.register(new FireballAbilityV1());

// Later, upgrade to v2 (replaces v1)
registry.register(new FireballAbilityV2()); // Same ID
```

### Unregistering on Disable

```java
public class MyModule implements AbilityModule {
    private List<String> registeredIds = new ArrayList<>();
    
    @Override
    public void onEnable(AbilityRegistry registry, ...) {
        for (Ability ability : getAbilities()) {
            registry.register(ability);
            registeredIds.add(ability.id());
        }
    }
    
    @Override
    public void onDisable() {
        for (String id : registeredIds) {
            registry.unregister(id);
        }
    }
}
```

---

## Implementation Details

### Storage

- Uses `ConcurrentHashMap<String, Ability>`
- O(1) lookup, registration, and unregistration
- Thread-safe for concurrent access

### ID Conflicts

When registering an ability with an existing ID:

1. Old ability is replaced
2. No warning is logged (by design)
3. Previous ability instance is discarded

**Best practice**: Use unique prefixes

```java
// Good
"mymod:fireball"
"customabilities:super_fireball"

// Avoid
"fireball"  // May conflict with other modules
```

---

## See Also

- [Ability](ability.md) - Ability interface
- [AbilityProvider](ability-provider.md) - Providing abilities
- [AbilityModule](ability-module.md) - Module lifecycle
