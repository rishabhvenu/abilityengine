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

### DOUBLE_SHIFT

Player double-taps sneak key within 400ms.

**Bukkit Events**: `PlayerToggleSneakEvent` (with timestamp tracking)

**Use cases**: Quick dodges, teleports, mobility abilities

**Example**:

```javascript
engine.ability({
  id: "dash",
  trigger: "DOUBLE_SHIFT",
  cooldown: 5,
  execute: function(ctx) {
    var dir = ctx.player().getLocation().getDirection();
    ctx.player().setVelocity(dir.multiply(2.5));
  }
});
```

---

### HOLD_SHIFT

Player holds the sneak key.

**Bukkit Events**: `PlayerToggleSneakEvent` (while sneaking = true)

**Frequency**: Fires once when sneak starts

**Use cases**: Charging abilities, mode switches, stealth abilities

**Example**:

```javascript
engine.ability({
  id: "stealth",
  trigger: "HOLD_SHIFT",
  execute: function(ctx) {
    engine.effects.potion(ctx.player(), "INVISIBILITY", 200, 0);
  }
});
```

---

### JUMP

Player jumps (was on ground, now airborne with upward velocity).

**Bukkit Events**: `PlayerMoveEvent` (with ground state + velocity checks)

**Use cases**: Double jumps, aerial abilities, parkour mechanics

**Example**:

```javascript
engine.ability({
  id: "double_jump",
  trigger: "JUMP",
  cooldown: 3,
  conditions: [engine.condition.custom(function(ctx) {
    return !ctx.player().isOnGround();  // Already in air
  })],
  execute: function(ctx) {
    var vel = ctx.player().getVelocity();
    vel.setY(0.8);  // Boost upward
    ctx.player().setVelocity(vel);
  }
});
```

---

### LAND

Player lands (was airborne, now on ground).

**Bukkit Events**: `PlayerMoveEvent` (with ground state transition)

**Use cases**: Ground pound, landing effects, fall damage abilities

**Example**:

```javascript
engine.ability({
  id: "ground_pound",
  trigger: "LAND",
  execute: function(ctx) {
    engine.effects.explosion(ctx.player().getLocation(), 2.0, false, false);
  }
});
```

---

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

### KILL_ENTITY

Player kills an entity.

**Bukkit Events**: `EntityDeathEvent` (with killer check)

**Context**: `targetEntity` is the killed entity

**Use cases**: Kill streaks, essence collection, combat bonuses

**Example**:

```javascript
engine.ability({
  id: "soul_harvest",
  trigger: "KILL_ENTITY",
  execute: function(ctx) {
    ctx.player().setHealth(Math.min(
      ctx.player().getHealth() + 2.0,
      ctx.player().getMaxHealth()
    ));
  }
});
```

---

## Projectile Triggers

### PROJECTILE_HIT

Player's projectile hits an entity or block.

**Bukkit Events**: `ProjectileHitEvent` (with shooter check)

**Context**: `targetEntity` or `targetBlock` is populated, `event` contains hit details

**Use cases**: Explosive arrows, trap placement, projectile effects

**Example**:

```javascript
engine.ability({
  id: "explosive_arrow",
  trigger: "PROJECTILE_HIT",
  execute: function(ctx) {
    var loc = ctx.event().getHitBlock() 
      ? ctx.event().getHitBlock().getLocation()
      : ctx.event().getHitEntity().getLocation();
    engine.effects.explosion(loc, 2.0, false, false);
  }
});
```

---

## Lifecycle Triggers

### ON_JOIN

Player joins the server.

**Bukkit Events**: `PlayerJoinEvent`

**Use cases**: Welcome kits, daily rewards, player state restoration

**Example**:

```javascript
engine.ability({
  id: "welcome_kit",
  trigger: "ON_JOIN",
  execute: function(ctx) {
    engine.items.give(ctx.player(), "starter_sword");
    ctx.player().sendMessage("§aWelcome! Here's a starter kit.");
  }
});
```

---

### ON_QUIT

Player leaves the server.

**Bukkit Events**: `PlayerQuitEvent`

**Use cases**: Cleanup, saving state, end abilities

**Example**:

```javascript
engine.ability({
  id: "logout_cleanup",
  trigger: "ON_QUIT",
  execute: function(ctx) {
    engine.sessions.end(ctx.player(), "active_aura");
    ctx.state.clear();  // Clear ability state
  }
});
```

---

## Special Triggers

### CUSTOM

For use with custom events via `engine.listen()`.

**Use cases**: Plugin integrations, custom mechanics, advanced event handling

**Example**:

```javascript
engine.listen("org.bukkit.event.player.PlayerExpChangeEvent", function(event) {
  // Trigger custom abilities when player gains XP
  // Can dispatch to abilities with CUSTOM trigger
});
```

---

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
- **Quick movements**: DOUBLE_SHIFT, JUMP, LAND
- **Entity-specific**: *_ENTITY triggers
- **Combat**: DAMAGE_DEALT, DAMAGE_TAKEN, KILL_ENTITY
- **Projectile effects**: PROJECTILE_HIT
- **Lifecycle**: ON_JOIN, ON_QUIT
- **Continuous effects**: Use sessions with TICK

### Performance Considerations

**High Frequency** (use with caution):

- `MOVE` - Multiple times per second
- `TICK` - 20 times per second
- `JUMP`, `LAND` - Can fire frequently during parkour

**Medium Frequency**:

- `DOUBLE_SHIFT` - Only on double-tap (limited by cooldown)
- `HOLD_SHIFT` - Only when shift is pressed

**Low Frequency** (safe):

- All click-based triggers
- Combat triggers (DAMAGE_DEALT, DAMAGE_TAKEN, KILL_ENTITY)
- Entity interactions
- Lifecycle triggers (ON_JOIN, ON_QUIT)
- Projectile triggers (PROJECTILE_HIT)

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
