# Scripting API Reference

Complete JavaScript API reference for the `engine` global object.

---

## engine.ability(config)

Registers an ability from a JavaScript object.

```javascript
engine.ability({
  id: "ability_id",
  trigger: "RIGHT_CLICK",  // or triggers: ["RIGHT_CLICK", "SHIFT"]
  conditions: [engine.condition.sneaking()],
  cooldown: {
    seconds: 5,
    showBossBar: true,
    bossBarColor: "BLUE"
  },
  permission: "ability.myability",
  onTrigger: function(ctx) {
    // Ability logic
  }
});
```

**Config Object**:

- `id` (String, required) - Unique ability identifier
- `trigger` or `triggers` (String or Array, optional) - Trigger type(s) (default: `"RIGHT_CLICK"`)
- `conditions` (Array, optional) - Conditions (default: `[]`)
- `cooldown` (Number or Object, optional) - Cooldown config (default: `0`)
  - Number form: cooldown in seconds
  - Object form: `{seconds, showBossBar, bossBarColor, bossBarLabel}`
- `permission` (String, optional) - Permission node required
- `execute` or `onTrigger` (Function, required) - Main execution function
- `onProjectileHit` (Function, optional) - Called when projectile hits
- `onProjectileTick` (Function, optional) - Called every tick for projectile
- `onExpire` (Function, optional) - Called when session expires
- `onCancel` (Function, optional) - Called when session is cancelled

**Context Object** (`ctx`):

- `ctx.player()` - Player object (method call!)
- `ctx.trigger()` - TriggerType
- `ctx.targetEntity()` - Entity or null
- `ctx.targetBlock()` - Block or null
- `ctx.item()` - ItemStack or null
- `ctx.event()` - Event or null
- `ctx.state.get(key)` - Get ability-scoped state
- `ctx.state.set(key, value)` - Set ability-scoped state
- `ctx.state.clear()` - Clear ability-scoped state
- `ctx.scheduleRepeating(func, delay, period)` - Schedule ability-scoped task
- `ctx.scheduleDelayed(func, delay)` - Schedule ability-scoped delayed task
- `ctx.cancelTask(taskId)` - Cancel task

---

## engine.item(config)

Creates and registers a named item template with auto-wired abilities.

```javascript
engine.item({
  id: "legendary_bow",
  type: "BOW",
  name: "&5&lLegendary Bow",
  lore: [
    "&aFire Arrow &7- Left Click",
    "&aExplosion &7- Right Click"
  ],
  abilities: ["fire_arrow", "explosion"],
  unbreakable: true,
  enchantments: {
    "ARROW_INFINITE": 1,
    "POWER": 3
  }
});
```

**Config Object**:

- `id` (String, required) - Unique item template ID
- `type` (String, required) - Material type (e.g., `"BOW"`, `"DIAMOND_SWORD"`)
- `name` (String, optional) - Display name with `&` color codes
- `lore` (Array of Strings, optional) - Lore lines with `&` color codes
- `abilities` (Array of String, required) - Ability IDs to attach (triggers auto-detected)
- `unbreakable` (Boolean, optional) - Make item unbreakable
- `enchantments` (Object, optional) - Enchantments with levels

---

## Triggers

### engine.trigger.*

Access trigger constants:

**Click Triggers:**
- `engine.trigger.RIGHT_CLICK`
- `engine.trigger.LEFT_CLICK`
- `engine.trigger.SHIFT_RIGHT_CLICK`
- `engine.trigger.SHIFT_LEFT_CLICK`
- `engine.trigger.RIGHT_CLICK_ENTITY`
- `engine.trigger.LEFT_CLICK_ENTITY`
- `engine.trigger.SHIFT_RIGHT_CLICK_ENTITY`
- `engine.trigger.SHIFT_LEFT_CLICK_ENTITY`

**Movement Triggers:**
- `engine.trigger.DOUBLE_SHIFT` - Double-tap sneak within 400ms
- `engine.trigger.HOLD_SHIFT` - Holding sneak key
- `engine.trigger.JUMP` - Player jumps
- `engine.trigger.LAND` - Player lands
- `engine.trigger.MOVE` - Any player movement

**Combat Triggers:**
- `engine.trigger.DAMAGE_DEALT` - Player damages entity
- `engine.trigger.DAMAGE_TAKEN` - Player takes damage
- `engine.trigger.KILL_ENTITY` - Player kills entity

**Projectile Triggers:**
- `engine.trigger.PROJECTILE_HIT` - Player's projectile hits

**Lifecycle Triggers:**
- `engine.trigger.ON_JOIN` - Player joins server
- `engine.trigger.ON_QUIT` - Player leaves server
- `engine.trigger.TICK` - Every tick for active sessions

**Custom:**
- `engine.trigger.CUSTOM` - For custom events

---

## Conditions

### engine.condition.sneaking()

Checks if player is sneaking.

### engine.condition.notSneaking()

Checks if player is NOT sneaking.

### engine.condition.healthAbove(threshold)

Checks if health > threshold.

### engine.condition.healthBelow(threshold)

Checks if health < threshold.

### engine.condition.yAbove(y)

Checks if Y coordinate > y.

### engine.condition.yBelow(y)

Checks if Y coordinate < y.

### engine.condition.hasTarget()

Checks if target entity exists.

### engine.condition.custom(function)

Creates custom condition.

```javascript
engine.condition.custom(function(ctx) {
  return ctx.player().getWorld().getName() === "world_nether";
})
```

---

## State Management

### engine.state.set(player, abilityId, key, value)

Sets state value globally (not scoped to execution).

### engine.state.get(player, abilityId, key)

Gets state value globally.

### engine.state.clear(player, abilityId)

Clears all state for ability + player.

**Note:** Prefer `ctx.state` inside ability functions for automatic scoping.

---

## UI Utilities

### engine.ui.cooldownBar(player, abilityId, durationSeconds, label, color)

Shows a cooldown progress bar.

```javascript
engine.ui.cooldownBar(
  ctx.player(),
  "my_ability",
  10,
  "Recharging",
  "BLUE"
);
```

**Parameters:**
- `player` (Player)
- `abilityId` (String) - Unique ID for this bar
- `durationSeconds` (Number) - Duration
- `label` (String, optional) - Display text
- `color` (String, optional) - Bar color: BLUE, GREEN, PINK, PURPLE, RED, WHITE, YELLOW

### engine.ui.removeBar(player, abilityId)

Removes a boss bar.

---

## Effects Library

### engine.effects.particle(location, particleType, colorHex, count, spreadX, spreadY, spreadZ)

Spawn particles with optional color.

```javascript
engine.effects.particle(
  ctx.player().getLocation(),
  "DUST",
  "#FF0000",
  50,
  0.5, 0.5, 0.5
);
```

### engine.effects.sound(location, sound, volume, pitch)

Play sound effect.

```javascript
engine.effects.sound(
  ctx.player().getLocation(),
  "ENTITY_ARROW_SHOOT",
  1.0,
  1.2
);
```

### engine.effects.potion(target, effectType, durationTicks, amplifier)

Apply potion effect.

```javascript
engine.effects.potion(
  ctx.targetEntity(),
  "POISON",
  100,
  1
);
```

### engine.effects.knockback(entity, direction, strength)

Apply knockback.

```javascript
var dir = ctx.player().getLocation().getDirection();
engine.effects.knockback(ctx.targetEntity(), dir, 2.0);
```

### engine.effects.explosion(location, power, setFire, breakBlocks)

Create explosion.

```javascript
engine.effects.explosion(
  ctx.targetBlock().getLocation(),
  3.0,
  false,
  false
);
```

### engine.effects.decayTerrain(center, radius, rules)

Decay terrain safely.

```javascript
engine.effects.decayTerrain(
  ctx.player().getLocation(),
  5,
  "NATURE_ONLY"  // or "STONE_ONLY", "ALL_BREAKABLE"
);
```

---

## Projectile Utilities

### engine.projectile.spawn(config)

Spawns a projectile with advanced features.

```javascript
engine.projectile.spawn({
  type: "ARROW",
  shooter: ctx.player(),
  speed: 2.5,
  damage: 8,
  critical: true,
  potion: {type: "POISON", duration: 100, amplifier: 1},
  trail: {
    particle: "DUST",
    color: "#00AA00",
    count: 3,
    spread: 0.1
  },
  onTick: function(projectile, tickCount) {
    // Called every tick
  },
  onHit: function(projectile, hitResult) {
    // Called on hit
    if (hitResult.hitEntity) {
      // Do something with hit entity
    }
  },
  maxTicks: 200
});
```

**Config Object:**
- `type` (String, required) - Projectile type (e.g., `"ARROW"`, `"FIREBALL"`)
- `shooter` (LivingEntity, required) - Who shot it
- `speed` (Number, optional) - Launch speed
- `damage` (Number, optional) - Damage on hit
- `critical` (Boolean, optional) - Critical hit (arrows only)
- `potion` (Object, optional) - `{type, duration, amplifier}` - Potion effect on hit
- `trail` (Object, optional) - `{particle, color, count, spread}` - Particle trail
- `onTick` (Function, optional) - Called every tick
- `onHit` (Function, optional) - Called on hit
- `maxTicks` (Number, optional) - Auto-remove after this many ticks

---

## Area Effect Cloud Utilities

### engine.areaEffect.spawn(config)

Spawns an area effect cloud with auto-exclusion.

```javascript
engine.areaEffect.spawn({
  location: ctx.player().getLocation(),
  source: ctx.player(),
  radius: 4,
  duration: 200,
  color: "#00AA00",
  potion: {type: "POISON", duration: 60, amplifier: 0},
  excludeCaster: true,
  radiusShrink: 0.005
});
```

**Config Object:**
- `location` (Location, required)
- `source` (LivingEntity, optional) - Source entity
- `radius` (Number, optional) - Cloud radius
- `duration` (Number, optional) - Duration in ticks
- `color` (String, optional) - Hex color (e.g., `"#00AA00"`)
- `potion` (Object, optional) - `{type, duration, amplifier}`
- `excludeCaster` (Boolean, optional) - Don't affect the caster
- `radiusShrink` (Number, optional) - Radius shrink per tick

---

## Event Listening

### engine.listen(eventClass, handler)

Registers a listener for any Bukkit event.

```javascript
engine.listen("PlayerJoinEvent", function(event) {
  event.getPlayer().sendMessage("Welcome!");
});
```

**Short Names**: `"PlayerJoinEvent"`, `"PlayerMoveEvent"`, `"EntityDamageEvent"`, etc.

**Full Names**: `"org.bukkit.event.player.PlayerJoinEvent"`

---

## Sessions

### engine.sessions.start(player, ability, handlers)

Starts a new session.

```javascript
engine.sessions.start(ctx.player(), {id: "ability_id"}, {
  onStart: function() {
    // Called once when session starts
  },
  onTick: function(tickCount) {
    // Called every tick
    if (tickCount > 200) {
      engine.sessions.end(ctx.player(), "ability_id");
    }
  },
  onEnd: function() {
    // Called once when session ends
  }
});
```

### engine.sessions.end(player, abilityId)

Ends all sessions for player running this ability.

### engine.sessions.getActive(player)

Returns array of ability IDs for active sessions.

```javascript
var active = engine.sessions.getActive(ctx.player());
```

---

## Cooldowns

### engine.cooldowns.isReady(player, abilityId)

Returns `true` if ability is ready (not on cooldown).

### engine.cooldowns.set(player, abilityId, seconds)

Sets a cooldown manually.

### engine.cooldowns.remaining(player, abilityId)

Returns remaining cooldown Duration object.

```javascript
var remaining = engine.cooldowns.remaining(ctx.player(), "fireball");
if (remaining.toSeconds() > 0) {
  ctx.player().sendMessage("Wait " + remaining.toSeconds() + " seconds");
}
```

---

## Items

### engine.items.create(abilityIdOrConfig)

Creates an ability item. Accepts either a string (ability ID) or a config object.

**Simple Form:**
```javascript
var item = engine.items.create("fireball");
```

**Extended Form:**
```javascript
var item = engine.items.create({
  type: "DIAMOND_SWORD",
  name: "&cFire Blade",
  lore: ["&7Ignites enemies"],
  abilityId: "fire_strike",
  unbreakable: true,
  enchantments: {"FIRE_ASPECT": 2}
});
```

**Config Object:**
- `type` (String, required) - Material
- `name` (String, optional) - Display name with `&` codes
- `lore` (Array, optional) - Lore lines with `&` codes
- `abilityId` or `abilities` (String/Array, optional) - Ability attachment
- `unbreakable` (Boolean, optional)
- `enchantments` (Object, optional)

### engine.items.give(player, itemIdOrAbilityId)

Gives an item to a player. Supports both item template IDs (from `engine.item()`) and ability IDs.

```javascript
engine.items.give(ctx.player(), "legendary_poison_bow");
```

### engine.items.isAbilityItem(item)

Returns `true` if item is an ability item.

### engine.items.getAbilityId(item)

Returns ability ID for item (or `null`).

---

## Scheduling

### engine.scheduleDelayed(function, delayTicks)

Schedules a one-time delayed task (global, not ability-scoped).

```javascript
engine.scheduleDelayed(function() {
  engine.log("Delayed task");
}, 20 * 5);  // 5 seconds
```

### engine.scheduleRepeating(function, delayTicks, periodTicks)

Schedules a repeating task (global, not ability-scoped).

```javascript
var taskId = engine.scheduleRepeating(function() {
  engine.log("Every second");
}, 20, 20);
```

### engine.cancelTask(taskId)

Cancels a scheduled task.

**Note:** Prefer `ctx.scheduleDelayed()` and `ctx.scheduleRepeating()` inside abilities for automatic cleanup.

---

## Logging

### engine.log(message)

Logs info message to console.

### engine.warn(message)

Logs warning message.

### engine.error(message)

Logs error message.

---

## Java Interop

Access Java classes via `Java.type()`:

```javascript
const Bukkit = Java.type("org.bukkit.Bukkit");
const Location = Java.type("org.bukkit.Location");
const Particle = Java.type("org.bukkit.Particle");

Bukkit.broadcastMessage("Hello!");
```

---

## See Also

- [Scripting Guide](../guides/scripting.md)
- [Trigger Reference](trigger-reference.md)
- [Script Examples](../examples/script-examples.md)
