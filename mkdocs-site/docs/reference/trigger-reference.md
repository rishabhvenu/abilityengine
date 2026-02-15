# Trigger Reference

Complete reference for all trigger types with detailed descriptions.

---

## Basic Click Interactions

### RIGHT_CLICK

Player right-clicks with the item (air or block).

**Bukkit Events**: `PlayerInteractEvent` (RIGHT_CLICK_AIR, RIGHT_CLICK_BLOCK)

**Use cases**: Launching projectiles, activating abilities, opening menus

**Example**:

```yaml
triggers:
  - RIGHT_CLICK
```

---

### LEFT_CLICK

Player left-clicks with the item (air or block, but not entities).

**Bukkit Events**: `PlayerInteractEvent` (LEFT_CLICK_AIR, LEFT_CLICK_BLOCK)

**Use cases**: Breaking blocks, melee abilities, quick actions

**Example**:

```yaml
triggers:
  - LEFT_CLICK
```

---

## Shift-Modified Interactions

### SHIFT_RIGHT_CLICK

Player sneaks and right-clicks.

**Bukkit Events**: `PlayerInteractEvent` (RIGHT_CLICK_*) + sneaking check

**Use cases**: Secondary abilities, mode switches, advanced features

**Example**:

```yaml
triggers:
  - SHIFT_RIGHT_CLICK
```

---

### SHIFT_LEFT_CLICK

Player sneaks and left-clicks.

**Bukkit Events**: `PlayerInteractEvent` (LEFT_CLICK_*) + sneaking check

**Use cases**: Alternate attack modes, special actions

---

## Entity Interactions

### RIGHT_CLICK_ENTITY

Player right-clicks on an entity.

**Bukkit Events**: `PlayerInteractEntityEvent`

**Context**: `targetEntity` is populated

**Use cases**: Healing allies, inspecting entities, entity-specific abilities

**Example**:

```yaml
triggers:
  - RIGHT_CLICK_ENTITY
conditions:
  - has-target: true
```

---

### LEFT_CLICK_ENTITY

Player left-clicks/attacks an entity.

**Bukkit Events**: `EntityDamageByEntityEvent`

**Context**: `targetEntity` is populated

**Use cases**: Damage modifiers, special attacks, combat abilities

---

### SHIFT_RIGHT_CLICK_ENTITY

Player sneaks and right-clicks an entity.

**Bukkit Events**: `PlayerInteractEntityEvent` + sneaking check

**Context**: `targetEntity` is populated

**Use cases**: Advanced entity interactions, ally buffs

---

### SHIFT_LEFT_CLICK_ENTITY

Player sneaks and left-clicks an entity.

**Bukkit Events**: `EntityDamageByEntityEvent` + sneaking check

**Context**: `targetEntity` is populated

**Use cases**: Stealth attacks, special combat abilities

---

## Combat Triggers

### DAMAGE_DEALT

Player damages an entity (any form of damage).

**Bukkit Events**: `EntityDamageByEntityEvent`

**Context**: `targetEntity` is the damaged entity

**Use cases**: Life steal, damage amplification, on-hit effects

**Example**:

```yaml
triggers:
  - DAMAGE_DEALT
actions:
  - type: HEAL
    amount: 1.0  # Life steal
```

---

### DAMAGE_TAKEN

Player takes damage (any source).

**Bukkit Events**: `EntityDamageEvent`

**Context**: `targetEntity` may be the damager (if entity)

**Use cases**: Counter-attacks, defensive abilities, shields

**Example**:

```yaml
triggers:
  - DAMAGE_TAKEN
conditions:
  - health-below: 10.0
actions:
  - type: TELEPORT
    forward: 10.0  # Emergency escape
```

---

## Movement Triggers

### MOVE

Player moves (walks, runs, jumps, falls, etc.).

**Bukkit Events**: `PlayerMoveEvent`

**Frequency**: Very high (multiple times per second)

**Warning**: Can cause performance issues if not optimized

**Use cases**: Speed trails, movement-based effects, parkour abilities

**Best practices**:

- Use conditions to limit activation
- Keep execute() logic minimal
- Consider using sessions instead

**Example**:

```yaml
triggers:
  - MOVE
conditions:
  - sneaking: true  # Only while sneaking
actions:
  - type: PLAY_EFFECT
    particle: CLOUD
    count: 1
```

---

## Special Triggers

### TICK

Fires every tick (50ms) for active sessions only.

**Frequency**: 20 times per second

**Usage**: Session-based abilities only

**Warning**: Performance-critical - optimize carefully

**Use cases**: Continuous auras, channeling, grappling hooks

**Best practices**:

- Only use with session system
- Run expensive operations less frequently (use tick count modulo)
- Set maximum duration

**Example (JavaScript)**:

```javascript
engine.sessions.start(player, {id: "aura"}, {
  onTick: function(tickCount) {
    if (tickCount > 200) {  // 10 seconds max
      engine.sessions.end(player, "aura");
      return;
    }
    
    // Only every second
    if (tickCount % 20 === 0) {
      damageNearby(player);
    }
  }
});
```

---

## Trigger Combinations

Multiple triggers can be assigned to one ability:

```yaml
triggers:
  - RIGHT_CLICK
  - SHIFT_RIGHT_CLICK
```

The ability will activate on any of the specified triggers.

---

## Best Practices

### Choose the Right Trigger

- **Instant abilities**: RIGHT_CLICK, LEFT_CLICK
- **Advanced/alternate abilities**: SHIFT_RIGHT_CLICK, SHIFT_LEFT_CLICK
- **Entity-specific**: *_ENTITY triggers
- **Combat**: DAMAGE_DEALT, DAMAGE_TAKEN
- **Continuous effects**: Use sessions with TICK

### Performance Considerations

**High Frequency** (use with caution):

- `MOVE` - Multiple times per second
- `TICK` - 20 times per second

**Low Frequency** (safe):

- All click-based triggers
- Combat triggers
- Entity interactions

### Conditional Activation

Use conditions to reduce unnecessary executions:

```yaml
triggers:
  - MOVE
conditions:
  - sneaking: true  # Only while sneaking
  - y-above: 100    # Only above Y=100
```

---

## See Also

- [TriggerType API](api/trigger-type.md) - API documentation
- [YAML Guide](../guides/yaml-abilities.md) - Using triggers in YAML
- [Scripting Guide](../guides/scripting.md) - Using triggers in JavaScript
