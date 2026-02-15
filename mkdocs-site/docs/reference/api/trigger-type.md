# TriggerType Enum

Enumeration of all supported ability triggers.

---

## Package

```java
xyz.rishabhvenu.abilityengine.api
```

## Enum Declaration

```java
public enum TriggerType
```

---

## Overview

`TriggerType` defines when an ability can be activated. Each ability can have one or more trigger types.

---

## Trigger Types

### Basic Interactions

#### `RIGHT_CLICK`

Player right-clicks with the item.

**Bukkit Events**: `PlayerInteractEvent` (RIGHT_CLICK_AIR, RIGHT_CLICK_BLOCK)

---

#### `LEFT_CLICK`

Player left-clicks (or left-click air) with the item.

**Bukkit Events**: `PlayerInteractEvent` (LEFT_CLICK_AIR, LEFT_CLICK_BLOCK)

---

### Shift Interactions

#### `SHIFT_RIGHT_CLICK`

Player sneaks and right-clicks.

**Bukkit Events**: `PlayerInteractEvent` (RIGHT_CLICK_*) + player.isSneaking()

---

#### `SHIFT_LEFT_CLICK`

Player sneaks and left-clicks.

**Bukkit Events**: `PlayerInteractEvent` (LEFT_CLICK_*) + player.isSneaking()

---

### Entity Interactions

#### `RIGHT_CLICK_ENTITY`

Player right-clicks on an entity.

**Bukkit Events**: `PlayerInteractEntityEvent`

**Context**: `targetEntity` will be populated

---

#### `LEFT_CLICK_ENTITY`

Player left-clicks/attacks an entity.

**Bukkit Events**: `EntityDamageByEntityEvent`

**Context**: `targetEntity` will be populated

---

#### `SHIFT_RIGHT_CLICK_ENTITY`

Player sneaks and right-clicks an entity.

**Bukkit Events**: `PlayerInteractEntityEvent` + player.isSneaking()

**Context**: `targetEntity` will be populated

---

#### `SHIFT_LEFT_CLICK_ENTITY`

Player sneaks and left-clicks an entity.

**Bukkit Events**: `EntityDamageByEntityEvent` + player.isSneaking()

**Context**: `targetEntity` will be populated

---

### Combat Triggers

#### `DAMAGE_DEALT`

Player damages an entity.

**Bukkit Events**: `EntityDamageByEntityEvent`

**Context**: `targetEntity` will be the damaged entity

---

#### `DAMAGE_TAKEN`

Player takes damage.

**Bukkit Events**: `EntityDamageEvent`

**Context**: `targetEntity` may be the damager (if entity)

---

### Movement Triggers

#### `MOVE`

Player moves.

**Bukkit Events**: `PlayerMoveEvent`

**Warning**: Fires very frequently! Use with caution.

---

### Special Triggers

#### `TICK`

Fires every tick (50ms) for active sessions.

**Usage**: Session-based abilities only

**Warning**: Only use with session system to avoid performance issues

---

## Usage

### In Java

```java
@Override
public Collection<TriggerType> triggers() {
    return List.of(
        TriggerType.RIGHT_CLICK,
        TriggerType.SHIFT_RIGHT_CLICK
    );
}
```

### In YAML

```yaml
triggers:
  - RIGHT_CLICK
  - SHIFT_RIGHT_CLICK
```

### In JavaScript

```javascript
engine.ability({
  id: "example",
  triggers: [
    engine.trigger.RIGHT_CLICK,
    engine.trigger.SHIFT_RIGHT_CLICK
  ],
  execute: function(ctx) {
    // ...
  }
});
```

---

## Trigger Resolution

The engine resolves triggers in this order:

1. Check if player is holding an ability item
2. Detect the player's action (right-click, left-click, etc.)
3. Check if player is sneaking
4. Determine the `TriggerType`
5. Find abilities with matching trigger
6. Execute abilities (if conditions pass and not on cooldown)

---

## Performance Considerations

### High-Frequency Triggers

These triggers fire very frequently:

- `MOVE` - Every time player moves (multiple times per second)
- `TICK` - 20 times per second (when in session)

**Recommendation**: Use sparingly and optimize execution code.

### Low-Frequency Triggers

These triggers are efficient:

- All click-based triggers
- Combat triggers
- Entity interaction triggers

---

## See Also

- [Ability](ability.md) - Using triggers in abilities
- [Trigger Reference](../trigger-reference.md) - Complete trigger guide
- [AbilityContext](ability-context.md) - Context passed to abilities
