# CooldownManager Interface

Interface for managing per-player ability cooldowns.

---

## Package

```java
xyz.rishabhvenu.abilityengine.api
```

## Interface Declaration

```java
public interface CooldownManager
```

---

## Overview

`CooldownManager` handles per-player cooldowns for abilities. It tracks when abilities can be used again and provides automatic expiry.

**Thread-safe**: Yes

**Implementation**: `CooldownManagerImpl` (internal)

---

## Methods

### UUID-based Methods

#### `isReady(UUID, String)`

```java
boolean isReady(UUID playerId, String abilityId)
```

Checks if an ability is ready (not on cooldown) for a player.

**Parameters**:

- `playerId` - The player's UUID
- `abilityId` - The ability ID

**Returns**: `true` if the ability is ready to use

---

#### `setCooldown(UUID, String, Duration)`

```java
void setCooldown(UUID playerId, String abilityId, Duration duration)
```

Sets a cooldown for an ability for a player.

**Parameters**:

- `playerId` - The player's UUID
- `abilityId` - The ability ID
- `duration` - The cooldown duration

---

#### `getRemainingCooldown(UUID, String)`

```java
Duration getRemainingCooldown(UUID playerId, String abilityId)
```

Gets the remaining cooldown time for an ability.

**Parameters**:

- `playerId` - The player's UUID
- `abilityId` - The ability ID

**Returns**: Remaining cooldown duration, or `Duration.ZERO` if ready

---

#### `clearCooldowns(UUID)`

```java
void clearCooldowns(UUID playerId)
```

Clears all cooldowns for a player.

**Parameters**: `playerId` - The player's UUID

---

### Player Convenience Methods

#### `isReady(Player, String)`

```java
default boolean isReady(Player player, String abilityId)
```

Convenience method to check cooldown for a player.

**Parameters**:

- `player` - The player
- `abilityId` - The ability ID

**Returns**: `true` if the ability is ready to use

**Implementation**:

```java
default boolean isReady(Player player, String abilityId) {
    return isReady(player.getUniqueId(), abilityId);
}
```

---

#### `setCooldown(Player, String, Duration)`

```java
default void setCooldown(Player player, String abilityId, Duration duration)
```

Convenience method to set cooldown for a player.

**Parameters**:

- `player` - The player
- `abilityId` - The ability ID
- `duration` - The cooldown duration

---

## Usage Examples

### Basic Usage

```java
CooldownManager cooldowns = getCooldownManager();

// Check if ready
if (cooldowns.isReady(player, "fireball")) {
    // Execute ability
    executeAbility(player);
    
    // Set cooldown
    cooldowns.setCooldown(player, "fireball", Duration.ofSeconds(3));
}
```

### With Remaining Time

```java
if (!cooldowns.isReady(player, "fireball")) {
    Duration remaining = cooldowns.getRemainingCooldown(player, "fireball");
    player.sendMessage("§cCooldown: " + remaining.toSeconds() + "s remaining");
    return;
}
```

### Clear on Quit

```java
@EventHandler
public void onQuit(PlayerQuitEvent event) {
    cooldowns.clearCooldowns(event.getPlayer().getUniqueId());
}
```

---

## JavaScript Access

```javascript
// Check cooldown
if (engine.cooldowns.isReady(player, "fireball")) {
  // Execute ability
  
  // Set cooldown
  engine.cooldowns.set(player, "fireball", 3);
}

// Get remaining time
var remaining = engine.cooldowns.remaining(player, "fireball");
if (remaining.toSeconds() > 0) {
  player.sendMessage("§cWait " + remaining.toSeconds() + " seconds");
}
```

---

## Implementation Details

### Storage

- Uses `ConcurrentHashMap` for thread safety
- Structure: `Map<UUID, Map<String, Instant>>`
- Automatic cleanup of expired entries

### Performance

- O(1) lookup time
- Minimal memory overhead
- Automatic expiry on next check

---

## See Also

- [Ability](ability.md) - Cooldown method
- [Scripting API](../scripting-api.md) - JavaScript cooldown methods
