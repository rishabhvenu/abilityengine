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

**Context Object** (`ctx`) — see [ctx Reference](#ctx-reference-abilityexeccontext) below for full details.

---

## ctx Reference (AbilityExecContext)

The `ctx` object passed to `execute` / `onTrigger` is an `AbilityExecContext`. All accessors are **method calls** (use parentheses).

### Event Data

| Method | Returns | Description |
|--------|---------|-------------|
| `ctx.player()` | `Player` | The player who triggered the ability |
| `ctx.trigger()` | `TriggerType` | Which trigger fired (e.g. `DOUBLE_SHIFT`) |
| `ctx.targetEntity()` | `Entity` or `null` | Target entity (for entity triggers / combat) |
| `ctx.targetBlock()` | `Block` or `null` | Target block (for block interactions / projectile hits) |
| `ctx.item()` | `ItemStack` or `null` | The ability item in hand |
| `ctx.event()` | `Event` or `null` | The raw Bukkit event |

### ctx.state — Ability-Scoped State

State is automatically scoped to **this ability + this player**. Cleaned up on quit and script unload.

#### ctx.state.get(key)

Returns the stored value, or `null` if not set.

- `key` (String) — State key

```javascript
var count = ctx.state.get("hitCount") || 0;
```

#### ctx.state.set(key, value)

Stores a value.

- `key` (String) — State key
- `value` (any) — Value to store

```javascript
ctx.state.set("hitCount", count + 1);
```

#### ctx.state.clear()

Removes all state for this ability + player.

```javascript
ctx.state.clear();
```

### ctx.scheduleRepeating(func, delayTicks, periodTicks)

Schedules an ability-scoped repeating task. The task is **automatically cancelled** when the ability is unloaded or the player quits.

**Parameters:**

- `func` (Function) — Callback executed each period
- `delayTicks` (Number) — Initial delay in ticks (20 ticks = 1 second)
- `periodTicks` (Number) — Interval between executions in ticks

**Returns:** `int` — Task ID (can be passed to `ctx.cancelTask()`)

```javascript
var taskId = ctx.scheduleRepeating(function() {
  engine.effects.particle(
    ctx.player().getLocation(),
    "FLAME", null, 10, 0.3, 0.3, 0.3
  );
}, 0, 5);  // Start immediately, run every 5 ticks (0.25s)
```

### ctx.scheduleDelayed(func, delayTicks)

Schedules an ability-scoped one-shot delayed task. Automatically cancelled on unload/quit.

**Parameters:**

- `func` (Function) — Callback to execute
- `delayTicks` (Number) — Delay in ticks

**Returns:** `int` — Task ID

```javascript
ctx.scheduleDelayed(function() {
  ctx.player().sendMessage("§c3 seconds have passed!");
}, 60);  // 3 seconds
```

### ctx.cancelTask(taskId)

Cancels a previously scheduled ability-scoped task.

**Parameters:**

- `taskId` (int) — The ID returned by `ctx.scheduleRepeating()` or `ctx.scheduleDelayed()`

```javascript
var taskId = ctx.scheduleRepeating(function() { /* ... */ }, 0, 20);

// Later:
ctx.cancelTask(taskId);
```

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

Creates a boss bar that fills from 0% to 100% over the given duration, then auto-removes itself. Useful for showing cooldown recharge or channel progress.

**Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `player` | Player | Yes | Player to show the bar to |
| `abilityId` | String | Yes | Unique ID for this bar (used to remove it later) |
| `durationSeconds` | Number | Yes | How long the bar takes to fill (in seconds) |
| `label` | String | No | Display text shown on the bar (defaults to ability ID) |
| `color` | String | No | Bar color (defaults to `"GREEN"`) |

**Valid colors:** `BLUE`, `GREEN`, `PINK`, `PURPLE`, `RED`, `WHITE`, `YELLOW`

```javascript
// Show a 10-second recharge bar
engine.ui.cooldownBar(
  ctx.player(),
  "my_ability",
  10,
  "Recharging...",
  "BLUE"
);
```

**Note:** If you use extended cooldown config (`cooldown: { seconds: 5, showBossBar: true }`), boss bars are created automatically — you don't need to call this manually.

### engine.ui.removeBar(player, abilityId)

Immediately removes a boss bar created by `cooldownBar()`.

**Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `player` | Player | Yes | Player whose bar to remove |
| `abilityId` | String | Yes | The same ID used when creating the bar |

```javascript
// Remove a bar early (e.g. ability was cancelled)
engine.ui.removeBar(ctx.player(), "my_ability");
```

---

## Effects Library

High-level effect utilities exposed as `engine.effects`. These abstract away GraalVM float-casting issues and Bukkit boilerplate.

### engine.effects.particle(location, particleType, colorHex, count, spreadX, spreadY, spreadZ)

Spawns particles at a location. For colored particles (`ENTITY_EFFECT`), pass a hex color string. For non-colored particle types, pass `null` for `colorHex`.

**Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `location` | Location | Yes | Where to spawn particles |
| `particleType` | String | Yes | Bukkit Particle enum name (e.g. `"FLAME"`, `"DUST"`, `"ENTITY_EFFECT"`, `"HEART"`, `"CLOUD"`) |
| `colorHex` | String or null | No | Hex color for `ENTITY_EFFECT` particles (e.g. `"#FF0000"`). Pass `null` for all other types. |
| `count` | Number | Yes | Number of particles |
| `spreadX` | Number | Yes | Horizontal spread (X axis) |
| `spreadY` | Number | Yes | Vertical spread |
| `spreadZ` | Number | Yes | Horizontal spread (Z axis) |

```javascript
// Colored entity effect particles
engine.effects.particle(
  ctx.player().getLocation(),
  "ENTITY_EFFECT", "#FF0000", 50,
  0.5, 0.5, 0.5
);

// Flame particles (no color needed)
engine.effects.particle(
  ctx.player().getLocation(),
  "FLAME", null, 20,
  0.3, 0.3, 0.3
);
```

### engine.effects.sound(location, sound, volume, pitch)

Plays a sound at a location. Handles GraalVM float casting internally.

**Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `location` | Location | Yes | Where to play the sound |
| `sound` | String | Yes | Bukkit Sound enum name (e.g. `"ENTITY_ARROW_SHOOT"`, `"ENTITY_ENDER_DRAGON_GROWL"`, `"BLOCK_ANVIL_LAND"`) |
| `volume` | Number | Yes | Volume (1.0 = normal, 0.5 = half, 2.0 = double) |
| `pitch` | Number | Yes | Pitch (1.0 = normal, 0.5 = low, 2.0 = high) |

```javascript
engine.effects.sound(
  ctx.player().getLocation(),
  "ENTITY_ARROW_SHOOT",
  1.0,
  1.2
);

engine.effects.sound(
  ctx.player().getLocation(),
  "ENTITY_WITCH_THROW",
  1.0,
  0.8
);
```

### engine.effects.potion(target, effectType, durationTicks, amplifier)

Applies a potion effect to a living entity.

**Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `target` | LivingEntity | Yes | Entity to apply the effect to |
| `effectType` | String | Yes | PotionEffectType name (e.g. `"POISON"`, `"SPEED"`, `"REGENERATION"`, `"INVISIBILITY"`, `"SLOW"`, `"BLINDNESS"`) |
| `durationTicks` | Number | Yes | Duration in ticks (20 ticks = 1 second) |
| `amplifier` | Number | Yes | Effect level (0 = level I, 1 = level II, etc.) |

```javascript
// Poison II for 5 seconds
engine.effects.potion(ctx.targetEntity(), "POISON", 100, 1);

// Speed I for 10 seconds on self
engine.effects.potion(ctx.player(), "SPEED", 200, 0);

// Invisibility I for 30 seconds
engine.effects.potion(ctx.player(), "INVISIBILITY", 600, 0);
```

### engine.effects.knockback(entity, direction, strength)

Applies a velocity-based knockback to an entity.

**Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `entity` | Entity | Yes | Entity to knock back |
| `direction` | Vector | Yes | Direction vector (e.g. from `Location.getDirection()`) |
| `strength` | Number | Yes | Knockback multiplier (1.0 = normal, 2.0 = double) |

```javascript
// Knock target away from player
var dir = ctx.player().getLocation().getDirection();
engine.effects.knockback(ctx.targetEntity(), dir, 2.0);

// Knock player upward
var up = ctx.player().getLocation().getDirection().setX(0).setZ(0).setY(1);
engine.effects.knockback(ctx.player(), up, 1.5);
```

### engine.effects.explosion(location, power, setFire, breakBlocks)

Creates an explosion at a location.

**Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `location` | Location | Yes | Center of explosion |
| `power` | Number | Yes | Explosion power (TNT = 4.0, Creeper = 3.0) |
| `setFire` | Boolean | Yes | Whether to set fire to blocks |
| `breakBlocks` | Boolean | Yes | Whether to destroy blocks |

```javascript
// Safe cosmetic explosion (no block damage)
engine.effects.explosion(
  ctx.targetBlock().getLocation(),
  3.0, false, false
);

// Destructive explosion with fire
engine.effects.explosion(
  ctx.player().getLocation(),
  4.0, true, true
);
```

### engine.effects.decayTerrain(center, radius, rules)

Decays terrain in a radius around a center point. Uses predefined rule sets to prevent accidental world destruction.

**Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `center` | Location | Yes | Center of the decay area |
| `radius` | Number | Yes | Block radius to affect |
| `rules` | String | Yes | Which blocks to decay (see below) |

**Rules:**

| Rule | Behavior |
|------|----------|
| `"NATURE_ONLY"` | Grass/mycelium/podzol → dirt; leaves break naturally; tall grass, short grass, ferns, dead bush → air |
| `"STONE_DECAY"` | Stone → cobblestone; deepslate → cobbled deepslate |
| `"ICE_MELT"` | Ice/packed ice/blue ice → water; snow/snow block → air |

```javascript
engine.effects.decayTerrain(
  ctx.player().getLocation(),
  5,
  "NATURE_ONLY"
);
```

### engine.effects.decayTerrainCustom(center, radius, ruleFunction)

Decays terrain using a custom JavaScript rule function. The function receives each `Block` and should return a material name string to replace it, or nothing to leave it unchanged.

**Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `center` | Location | Yes | Center of decay area |
| `radius` | Number | Yes | Block radius |
| `ruleFunction` | Function | Yes | Called per block; return a material name string to replace, or nothing to skip |

```javascript
engine.effects.decayTerrainCustom(
  ctx.player().getLocation(),
  3,
  function(block) {
    if (block.getType().name() === "SAND") {
      return "GLASS";  // Turn sand into glass
    }
    // Return nothing to leave block unchanged
  }
);
```

### Beam / Visual Helpers

There are currently **no built-in beam or line-drawing helpers** in the effects library. For beams, use `engine.effects.particle()` inside a `ctx.scheduleRepeating()` loop to draw particle lines between two points manually.

```javascript
// Example: simple beam between two locations
var start = ctx.player().getEyeLocation();
var dir = start.getDirection();
var step = 0.5;

for (var i = 0; i < 20; i++) {
  var point = start.clone().add(dir.clone().multiply(i * step));
  engine.effects.particle(point, "FLAME", null, 1, 0, 0, 0);
}
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

Cooldown management. Cooldowns are set automatically when an ability has a `cooldown` config, but you can also control them manually.

### engine.cooldowns.isReady(player, abilityId)

Returns `true` if the ability is off cooldown and can be used.

**Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `player` | Player | Yes | Player to check |
| `abilityId` | String | Yes | Ability ID to check |

**Returns:** `boolean`

```javascript
if (engine.cooldowns.isReady(ctx.player(), "fireball")) {
  ctx.player().sendMessage("§aFireball is ready!");
} else {
  ctx.player().sendMessage("§cFireball is on cooldown.");
}
```

### engine.cooldowns.set(player, abilityId, seconds)

Manually sets (or overrides) a cooldown. This does **not** trigger a boss bar — use `engine.ui.cooldownBar()` separately if you want visual feedback, or use the `cooldown` config object on `engine.ability()` for automatic boss bar binding.

**Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `player` | Player | Yes | Player to set cooldown for |
| `abilityId` | String | Yes | Ability ID |
| `seconds` | Number | Yes | Cooldown duration in seconds |

```javascript
// Set a 30-second cooldown manually
engine.cooldowns.set(ctx.player(), "ultimate", 30);

// Reset cooldown (make ability immediately available)
engine.cooldowns.set(ctx.player(), "fireball", 0);
```

### engine.cooldowns.remaining(player, abilityId)

Returns the remaining cooldown as a Java `Duration` object.

**Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `player` | Player | Yes | Player to check |
| `abilityId` | String | Yes | Ability ID |

**Returns:** `java.time.Duration` — call `.toSeconds()`, `.toMillis()`, or `.isZero()` on it.

```javascript
var remaining = engine.cooldowns.remaining(ctx.player(), "fireball");
if (remaining.toSeconds() > 0) {
  ctx.player().sendMessage("§cWait " + remaining.toSeconds() + "s");
}
```

---

## Items

Item creation and management. Works with both simple ability-tagged items and rich item templates from `engine.item()`.

### engine.items.create(abilityIdOrConfig)

Creates an `ItemStack` tagged as an ability item. Accepts a string (for simple items) or a config object (for advanced items).

**Simple Form** — creates a default item for the ability's material:

```javascript
var item = engine.items.create("fireball");
```

**Extended Form** — full control over the item:

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

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `type` | String | Yes | Material name (e.g. `"BOW"`, `"DIAMOND_SWORD"`) |
| `name` | String | No | Display name with `&` color codes (e.g. `"&c&lFire Blade"`) |
| `lore` | Array of String | No | Lore lines with `&` color codes |
| `abilityId` | String | No | Single ability ID to attach |
| `abilities` | Array | No | Multiple abilities to attach (as JSON array with `id` and `trigger`) |
| `unbreakable` | Boolean | No | Make item unbreakable |
| `enchantments` | Object | No | Map of enchantment name to level |

**Returns:** `ItemStack`

### engine.items.give(player, itemIdOrAbilityId)

Gives an item to a player. First checks for an item template registered with `engine.item()` by the given ID; if none found, falls back to creating a simple ability item.

**Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `player` | Player | Yes | Player to give the item to |
| `itemIdOrAbilityId` | String | Yes | Item template ID (from `engine.item()`) or ability ID |

```javascript
// Give a templated item (defined via engine.item())
engine.items.give(ctx.player(), "legendary_poison_bow");

// Give a simple ability item
engine.items.give(ctx.player(), "fireball");
```

### engine.items.isAbilityItem(item)

Returns `true` if the item has ability metadata in its PDC.

**Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `item` | ItemStack | Yes | Item to check |

**Returns:** `boolean`

```javascript
var mainHand = ctx.player().getInventory().getItemInMainHand();
if (engine.items.isAbilityItem(mainHand)) {
  ctx.player().sendMessage("You're holding an ability item!");
}
```

### engine.items.getAbilityId(item)

Returns the primary ability ID attached to an item, or `null` if it's not an ability item.

**Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `item` | ItemStack | Yes | Item to inspect |

**Returns:** `String` or `null`

```javascript
var id = engine.items.getAbilityId(ctx.item());
if (id !== null) {
  engine.log("This item has ability: " + id);
}
```

---

## Scheduling

Global scheduling utilities. These tasks are **not** tied to any specific ability and must be cancelled manually. For ability-scoped tasks that auto-cancel on quit/unload, use `ctx.scheduleRepeating()` and `ctx.scheduleDelayed()` instead.

### engine.scheduleDelayed(function, delayTicks)

Schedules a one-time delayed task.

**Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `function` | Function | Yes | Callback to execute |
| `delayTicks` | Number | Yes | Delay in ticks (20 ticks = 1 second) |

**Returns:** `int` — Task ID

```javascript
engine.scheduleDelayed(function() {
  engine.log("5 seconds have passed");
}, 20 * 5);
```

### engine.scheduleRepeating(function, delayTicks, periodTicks)

Schedules a repeating task.

**Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `function` | Function | Yes | Callback executed each period |
| `delayTicks` | Number | Yes | Initial delay in ticks |
| `periodTicks` | Number | Yes | Interval between executions in ticks |

**Returns:** `int` — Task ID

```javascript
var taskId = engine.scheduleRepeating(function() {
  engine.log("Every second");
}, 20, 20);
```

### engine.cancelTask(taskId)

Cancels a global scheduled task.

**Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `taskId` | int | Yes | ID returned by `scheduleDelayed` or `scheduleRepeating` |

```javascript
var taskId = engine.scheduleRepeating(function() { /* ... */ }, 0, 20);

// Later:
engine.cancelTask(taskId);
```

!!! tip
    Prefer `ctx.scheduleDelayed()` and `ctx.scheduleRepeating()` inside ability functions — those are automatically cancelled when the ability unloads or the player quits, preventing memory leaks.

---

## Logging

### engine.log(message)

Logs info message to console.

### engine.warn(message)

Logs warning message.

### engine.error(message)

Logs error message.

---

## Phase API (Phase 3)

### Defining Phases

Abilities can use a state machine with multiple phases:

```javascript
engine.ability({
  id: "channeled",
  phases: {
    charge: {
      duration: 20,              // Optional: auto-transition after N ticks
      onStart(ctx, phase) {},    // Called once on phase start
      onTick(ctx, phase) {},     // Called every tick
      endWhen(ctx, phase) {},    // Optional: return true to transition
      next: "release"            // Next phase name
    },
    release: {
      onStart(ctx, phase) {},
      onEnd(ctx, phase) {}       // Called when leaving this phase
    }
  }
});
```

**Phase Context (`phase`):**
- `phase.name` - Current phase name
- `phase.tick` - Tick counter for current phase
- `phase.get(key)` - Get phase-scoped state
- `phase.set(key, value)` - Set phase-scoped state

**Execution Context:**
- `ctx.phase()` - Returns current PhaseInstance (or null)

Phases automatically transition when:
- `duration` ticks elapsed
- `endWhen()` returns true
- Ability is interrupted

---

## Raycast API (Phase 3)

### engine.raycast(config)

Performs synchronous raycasting with block and entity detection.

```javascript
var result = engine.raycast({
  origin: player.getEyeLocation(),
  direction: player.getLocation().getDirection(),
  maxDistance: 50,
  detect: ["BLOCK", "ENTITY"],  // What to detect
  entityRadius: 1.5,              // Entity detection radius
  
  // Callbacks (optional)
  onHitBlock(hit) {
    // hit.type === "BLOCK"
    // hit.location, hit.block
  },
  onHitEntity(hit) {
    // hit.type === "ENTITY"
    // hit.location, hit.entity
  },
  onMiss(endLocation) {
    // No hit detected
  }
});

// Returns: { type: "BLOCK"|"ENTITY"|"MISS", location, entity, block }
```

**Notes:**
- Must be called from main thread
- Entity detection uses stepping algorithm (checks every 0.5 blocks)
- If entity closer than block, entity wins

---

## Movement API (Phase 3)

### engine.movement.pull(config, execution)

Pulls entity toward target with physics-safe velocity:

```javascript
engine.movement.pull({
  entity: player,
  target: targetLocation,
  speed: 1.2,
  drag: 0.7,                    // Collision drag multiplier
  minSpeed: 0.15,               // Stop if speed drops below
  arrivalDistance: 1.5,         // Stop when within distance
  maxTicks: 80,                 // Maximum duration
  
  onArrival() {},               // Called on successful arrival
  onInterrupt() {}              // Called if cancelled
}, ctx.execution());
```

### engine.movement.dash(config, execution)

Quick directional dash:

```javascript
engine.movement.dash({
  entity: player,
  direction: player.getLocation().getDirection(),
  power: 2.0,
  duration: 10                  // Optional: sustained push
}, ctx.execution());
```

### engine.movement.launch(config)

Single velocity impulse:

```javascript
engine.movement.launch({
  entity: target,
  direction: new Vector(0, 1, 0.5),
  power: 1.5
});
```

**Notes:**
- All movement tasks tracked on execution instance
- Auto-cancelled on ability interrupt
- Resets fall distance automatically

---

## Control API (Phase 3)

### engine.control.freeze(entity, config, execution)

Freezes entity movement:

```javascript
engine.control.freeze(target, {
  duration: 60,                 // Ticks (0 = permanent)
  preventMovement: true,
  preventRotation: false        // Not yet implemented
}, ctx.execution());
```

### engine.control.unfreeze(entity, execution)

Unfreezes entity:

```javascript
engine.control.unfreeze(target, ctx.execution());
```

### engine.control.isFrozen(entity)

Checks freeze status:

```javascript
if (engine.control.isFrozen(target)) {
  // ...
}
```

**Notes:**
- Frozen entities have velocity zeroed every tick
- Auto-unfreezes on death, quit, or duration
- All frozen entities tracked on execution instance

---

## Cooldown Override (Phase 3)

### ctx.overrideCooldown(seconds)

Sets new cooldown duration:

```javascript
ctx.overrideCooldown(5); // Set to 5 seconds
```

### ctx.shortenCooldown(percent)

Reduces remaining cooldown by percentage:

```javascript
ctx.shortenCooldown(50); // Reduce by 50%
```

**Notes:**
- Both methods sync with boss bar UI automatically
- Useful for dynamic cooldowns based on conditions
- Works with or without boss bar display

---

## Interrupt System (Phase 3)

### Defining Interrupts

Abilities can be cancelled by external events:

```javascript
engine.ability({
  id: "channeled",
  interrupts: ["TAKE_DAMAGE", "SWITCH_ITEM", "DEATH", "QUIT"],
  
  onInterrupt(ctx) {
    ctx.player().sendMessage("§cInterrupted!");
    ctx.shortenCooldown(50); // Optional: reduce cooldown penalty
  }
});
```

**Available Interrupt Types:**
- `TAKE_DAMAGE` - Player takes damage
- `SWITCH_ITEM` - Player changes held item slot
- `DEATH` - Player dies
- `QUIT` - Player leaves server

**Automatic Cleanup:**

When interrupted, execution instance automatically:
- Cancels all owned tasks (phases, movement)
- Unfreezes all owned entities
- Calls `onInterrupt()` callback
- Unregisters from tracker

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
