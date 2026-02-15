# JavaScript Scripting Guide

Write powerful abilities using JavaScript with GraalVM. This guide covers the complete scripting API.

---

## Overview

AbilityEngine's scripting system allows you to create abilities using JavaScript. It's powered by GraalVM, providing fast execution and full Java interop.

**Key Features**:

- **Clean DSL** for ability registration via `engine` global object
- **Hot reload** without server restart
- **Full Bukkit API access** via `Java.type()`
- **Event listening** for any Bukkit event
- **Session management** for stateful abilities
- **Task scheduling** for delayed and repeating tasks
- **Automatic resource cleanup** on reload

---

## Script Location

Place JavaScript files in:

```
plugins/AbilityEngine/scripts/
```

All `.js` files are automatically loaded on server start and can be reloaded with `/ability script reload`.

---

## The `engine` Global Object

Every script has access to a global `engine` object that provides the scripting API. This is the recommended way to interact with AbilityEngine.

---

## Creating Abilities

### `engine.ability(config)`

Registers an ability from a JavaScript object.

**Config Object**:

| Property | Type | Required | Default | Description |
|----------|------|----------|---------|-------------|
| `id` | String | Yes | - | Unique ability identifier |
| `triggers` | Array\<String\> | No | `["RIGHT_CLICK"]` | Trigger types |
| `conditions` | Array\<Condition\> | No | `[]` | Conditions to check |
| `cooldown` | Number | No | 0 | Cooldown in seconds |
| `execute` | Function | Yes | - | Function to execute |

**Execute Function Signature**:

```javascript
function(ctx) {
  // ctx is an AbilityContext with:
  // - ctx.player (Player)
  // - ctx.trigger (TriggerType)
  // - ctx.targetEntity (Entity, nullable)
  // - ctx.targetBlock (Block, nullable)
  // - ctx.item (ItemStack, nullable)
  // - ctx.event (Event, nullable - raw Bukkit event)
}
```

**Basic Example**:

```javascript
engine.ability({
  id: "dash",
  triggers: ["SHIFT_RIGHT_CLICK"],
  cooldown: 5,
  execute: function(ctx) {
    var dir = ctx.player.getLocation().getDirection();
    ctx.player.setVelocity(dir.multiply(2.5));
    ctx.player.sendMessage("§bDashed!");
  }
});
```

---

## Trigger Constants

Access trigger types via `engine.trigger.*`:

```javascript
engine.trigger.RIGHT_CLICK
engine.trigger.LEFT_CLICK
engine.trigger.SHIFT_RIGHT_CLICK
engine.trigger.SHIFT_LEFT_CLICK
engine.trigger.RIGHT_CLICK_ENTITY
engine.trigger.LEFT_CLICK_ENTITY
engine.trigger.SHIFT_RIGHT_CLICK_ENTITY
engine.trigger.SHIFT_LEFT_CLICK_ENTITY
engine.trigger.DAMAGE_DEALT
engine.trigger.DAMAGE_TAKEN
engine.trigger.MOVE
engine.trigger.TICK
```

**Example**:

```javascript
engine.ability({
  id: "combat_ability",
  triggers: [engine.trigger.DAMAGE_DEALT, engine.trigger.DAMAGE_TAKEN],
  execute: function(ctx) {
    ctx.player.sendMessage("Combat triggered!");
  }
});
```

---

## Conditions

### Built-in Condition Builders

Access via `engine.condition.*`:

#### `engine.condition.sneaking()`

Checks if player is sneaking.

```javascript
conditions: [engine.condition.sneaking()]
```

#### `engine.condition.notSneaking()`

Checks if player is NOT sneaking.

```javascript
conditions: [engine.condition.notSneaking()]
```

#### `engine.condition.healthAbove(threshold)`

Checks if player's health is above a threshold (exclusive).

```javascript
conditions: [engine.condition.healthAbove(5.0)]
```

#### `engine.condition.healthBelow(threshold)`

Checks if player's health is below a threshold (exclusive).

```javascript
conditions: [engine.condition.healthBelow(15.0)]
```

#### `engine.condition.yAbove(y)`

Checks if player's Y coordinate is above a value (exclusive).

```javascript
conditions: [engine.condition.yAbove(64.0)]
```

#### `engine.condition.yBelow(y)`

Checks if player's Y coordinate is below a value (exclusive).

```javascript
conditions: [engine.condition.yBelow(100.0)]
```

#### `engine.condition.hasTarget()`

Checks if a target entity exists in the context.

```javascript
conditions: [engine.condition.hasTarget()]
```

#### `engine.condition.custom(function)`

Creates a custom condition from a function.

```javascript
conditions: [
  engine.condition.custom(function(ctx) {
    return ctx.player.getWorld().getName() === "world_nether";
  })
]
```

### Multiple Conditions

All conditions use AND logic:

```javascript
engine.ability({
  id: "heal",
  triggers: ["RIGHT_CLICK"],
  conditions: [
    engine.condition.sneaking(),
    engine.condition.healthBelow(15.0)
  ],
  cooldown: 10,
  execute: function(ctx) {
    var newHealth = Math.min(
      ctx.player.getHealth() + 6,
      ctx.player.getMaxHealth()
    );
    ctx.player.setHealth(newHealth);
  }
});
```

---

## Event Listening

### `engine.listen(eventClass, handler)`

Registers a listener for any Bukkit event.

**Parameters**:

- `eventClass` (String) - Event class name (short or fully qualified)
- `handler` (Function) - Function that receives the event

**Supported Short Names**:

Common events can be referenced by short name:

- `"PlayerJoinEvent"`
- `"PlayerQuitEvent"`
- `"PlayerMoveEvent"`
- `"PlayerInteractEvent"`
- `"PlayerInteractEntityEvent"`
- `"EntityDamageEvent"`
- `"EntityDamageByEntityEvent"`
- `"BlockBreakEvent"`
- `"BlockPlaceEvent"`

Or use fully qualified names: `"org.bukkit.event.player.PlayerMoveEvent"`

**Example**:

```javascript
engine.listen("PlayerJoinEvent", function(event) {
  var player = event.getPlayer();
  player.sendMessage("§aWelcome! AbilityEngine is active.");
  engine.log(player.getName() + " joined");
});
```

**Complex Example**:

```javascript
engine.listen("EntityDamageByEntityEvent", function(event) {
  var damager = event.getDamager();
  var damaged = event.getEntity();
  
  // Check if player damaged another entity
  const Player = Java.type("org.bukkit.entity.Player");
  if (damager instanceof Player) {
    damager.sendMessage("§cYou dealt " + event.getDamage() + " damage!");
  }
});
```

---

## Session Management

Sessions allow you to create stateful abilities that run over time.

### `engine.sessions.start(player, ability, handlers)`

Starts a new session.

**Parameters**:

- `player` (Player) - The player
- `ability` (Object) - Ability object (must have `id` property)
- `handlers` (Object) - Lifecycle handlers

**Handlers Object**:

| Handler | Parameters | Description |
|---------|------------|-------------|
| `onStart` | None | Called when session starts |
| `onTick` | `tickCount` | Called every tick (50ms) |
| `onEnd` | None | Called when session ends |

**Example**:

```javascript
engine.ability({
  id: "fire_aura",
  triggers: ["RIGHT_CLICK"],
  cooldown: 30,
  execute: function(ctx) {
    ctx.player.sendMessage("§cFire Aura activated!");
    
    engine.sessions.start(ctx.player, {id: "fire_aura"}, {
      onStart: function() {
        engine.log("Fire aura started");
      },
      
      onTick: function(tickCount) {
        // Run for 10 seconds (200 ticks)
        if (tickCount > 200) {
          engine.sessions.end(ctx.player, "fire_aura");
          return;
        }
        
        // Every second, damage nearby entities
        if (tickCount % 20 === 0) {
          const LivingEntity = Java.type("org.bukkit.entity.LivingEntity");
          const Particle = Java.type("org.bukkit.Particle");
          
          var loc = ctx.player.getLocation();
          var world = ctx.player.getWorld();
          
          // Particles
          world.spawnParticle(Particle.FLAME, loc, 20, 1.0, 1.0, 1.0, 0.1);
          
          // Damage nearby entities
          var nearby = world.getNearbyEntities(loc, 3, 3, 3);
          nearby.forEach(function(entity) {
            if (entity instanceof LivingEntity && entity !== ctx.player) {
              entity.damage(2.0, ctx.player);
            }
          });
        }
      },
      
      onEnd: function() {
        ctx.player.sendMessage("§cAura ended!");
      }
    });
  }
});
```

### `engine.sessions.end(player, abilityId)`

Ends all sessions for a player running a specific ability.

```javascript
engine.sessions.end(ctx.player, "fire_aura");
```

### `engine.sessions.getActive(player)`

Returns an array of ability IDs for the player's active sessions.

```javascript
var active = engine.sessions.getActive(ctx.player);
if (active.length > 0) {
  ctx.player.sendMessage("§eActive abilities: " + active.join(", "));
}
```

---

## Cooldown Management

### `engine.cooldowns.isReady(player, abilityId)`

Checks if an ability is ready (not on cooldown).

```javascript
if (engine.cooldowns.isReady(ctx.player, "fireball")) {
  // Ability is ready
}
```

### `engine.cooldowns.set(player, abilityId, seconds)`

Sets a cooldown manually.

```javascript
engine.cooldowns.set(ctx.player, "custom_ability", 10);
```

### `engine.cooldowns.remaining(player, abilityId)`

Returns remaining cooldown time in seconds (as a Duration object).

```javascript
var remaining = engine.cooldowns.remaining(ctx.player, "fireball");
if (remaining.toSeconds() > 0) {
  ctx.player.sendMessage("§cWait " + remaining.toSeconds() + " seconds");
}
```

---

## Item Management

### `engine.items.create(abilityId)`

Creates an ability item for the given ability ID.

```javascript
var item = engine.items.create("fireball");
ctx.player.getInventory().addItem(item);
```

### `engine.items.isAbilityItem(item)`

Returns `true` if the item is an ability item.

```javascript
if (engine.items.isAbilityItem(ctx.item)) {
  engine.log("Player is holding an ability item");
}
```

### `engine.items.getAbilityId(item)`

Returns the ability ID for an item (or `null`).

```javascript
var abilityId = engine.items.getAbilityId(ctx.item);
if (abilityId !== null) {
  engine.log("Item has ability: " + abilityId);
}
```

---

## Task Scheduling

### `engine.scheduleDelayed(function, delayTicks)`

Schedules a one-time delayed task.

**Returns**: Task ID (number)

```javascript
var taskId = engine.scheduleDelayed(function() {
  engine.log("This runs after 5 seconds");
}, 20 * 5);  // 100 ticks = 5 seconds
```

### `engine.scheduleRepeating(function, delayTicks, periodTicks)`

Schedules a repeating task.

**Returns**: Task ID (number)

```javascript
var taskId = engine.scheduleRepeating(function() {
  engine.log("Every second");
}, 20, 20);  // Delay 1s, repeat every 1s
```

### `engine.cancelTask(taskId)`

Cancels a scheduled task.

```javascript
var taskId = engine.scheduleRepeating(function() {
  // ...
}, 0, 20);

// Later...
engine.cancelTask(taskId);
```

**Example - Auto-Cancel**:

```javascript
var count = 0;
var taskId = engine.scheduleRepeating(function() {
  count++;
  engine.log("Count: " + count);
  
  if (count >= 10) {
    engine.cancelTask(taskId);
    engine.log("Task completed");
  }
}, 20, 20);
```

---

## Logging

### `engine.log(message)`

Logs an info message to the console.

```javascript
engine.log("Ability executed successfully");
```

### `engine.warn(message)`

Logs a warning message.

```javascript
engine.warn("Player health is critically low");
```

### `engine.error(message)`

Logs an error message.

```javascript
engine.error("Failed to execute ability");
```

---

## Java Interop

Scripts have full access to Java classes via `Java.type()`:

### Importing Java Classes

```javascript
const Bukkit = Java.type("org.bukkit.Bukkit");
const Location = Java.type("org.bukkit.Location");
const Particle = Java.type("org.bukkit.Particle");
const Sound = Java.type("org.bukkit.Sound");
const Material = Java.type("org.bukkit.Material");
const ItemStack = Java.type("org.bukkit.ItemStack");
```

### Using Bukkit API

```javascript
// Broadcast message
const Bukkit = Java.type("org.bukkit.Bukkit");
Bukkit.broadcastMessage("§aServer event!");

// Spawn particles
const Particle = Java.type("org.bukkit.Particle");
const Location = Java.type("org.bukkit.Location");

var world = ctx.player.getWorld();
var loc = new Location(world, 0, 64, 0);
world.spawnParticle(Particle.EXPLOSION_LARGE, loc, 5);
```

### Type Checking

```javascript
const Player = Java.type("org.bukkit.entity.Player");
const LivingEntity = Java.type("org.bukkit.entity.LivingEntity");

if (ctx.targetEntity instanceof Player) {
  // Target is a player
} else if (ctx.targetEntity instanceof LivingEntity) {
  // Target is a living entity
}
```

### Creating Objects

```javascript
const ItemStack = Java.type("org.bukkit.ItemStack");
const Material = Java.type("org.bukkit.Material");

var item = new ItemStack(Material.DIAMOND, 1);
ctx.player.getInventory().addItem(item);
```

---

## Hot Reload

Scripts can be reloaded without restarting the server.

### Commands

- `/ability reload` - Reloads all abilities (YAML + scripts)
- `/ability script reload` - Reloads all scripts
- `/ability script reload <filename>` - Reloads a specific script
- `/ability script list` - Lists all loaded scripts

### What Happens on Reload

1. All abilities registered by the script are unregistered
2. All event listeners are unregistered
3. All scheduled tasks are cancelled
4. The script file is re-executed

### Best Practices

!!! tip "Fast Iteration"
    Use hot reload for rapid development:
    ```
    1. Edit script in editor
    2. Save file
    3. Run /ability script reload <filename>
    4. Test immediately
    ```

!!! warning "State Preservation"
    Reloading a script does NOT preserve state. Active sessions and scheduled tasks will be cancelled.

---

## Complete Examples

### Basic Fireball

```javascript
engine.ability({
  id: "fireball",
  triggers: ["RIGHT_CLICK"],
  conditions: [engine.condition.sneaking()],
  cooldown: 3,
  execute: function(ctx) {
    const Fireball = Java.type("org.bukkit.entity.Fireball");
    ctx.player.launchProjectile(Fireball);
    ctx.player.sendMessage("§cFireball!");
  }
});

engine.log("Loaded fireball ability");
```

### Lightning Strike

```javascript
engine.ability({
  id: "lightning",
  triggers: ["RIGHT_CLICK_ENTITY"],
  conditions: [engine.condition.hasTarget()],
  cooldown: 15,
  execute: function(ctx) {
    if (!ctx.targetEntity) return;
    
    var loc = ctx.targetEntity.getLocation();
    ctx.player.getWorld().strikeLightning(loc);
    
    const Bukkit = Java.type("org.bukkit.Bukkit");
    const Sound = Java.type("org.bukkit.Sound");
    
    Bukkit.broadcastMessage("§e" + ctx.player.getName() + " struck lightning!");
    ctx.player.playSound(ctx.player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0, 1.0);
  }
});
```

### Grappling Hook (Session-Based)

```javascript
engine.ability({
  id: "grappling_hook",
  triggers: ["RIGHT_CLICK"],
  cooldown: 8,
  execute: function(ctx) {
    var targetBlock = ctx.player.getTargetBlock(null, 30);
    if (targetBlock === null || targetBlock.isEmpty()) {
      ctx.player.sendMessage("§cNo valid target!");
      return;
    }
    
    var targetLoc = targetBlock.getLocation();
    var playerLoc = ctx.player.getLocation();
    
    // Calculate direction
    var direction = targetLoc.toVector().subtract(playerLoc.toVector()).normalize();
    
    // Start pull session
    engine.sessions.start(ctx.player, {id: "grappling_hook"}, {
      onStart: function() {
        ctx.player.sendMessage("§bGrappling!");
      },
      
      onTick: function(tickCount) {
        // Pull for 40 ticks (2 seconds)
        if (tickCount > 40) {
          engine.sessions.end(ctx.player, "grappling_hook");
          return;
        }
        
        // Apply velocity towards target
        ctx.player.setVelocity(direction.multiply(0.8));
      },
      
      onEnd: function() {
        ctx.player.sendMessage("§bGrapple complete!");
      }
    });
  }
});
```

### Welcome Message

```javascript
engine.listen("PlayerJoinEvent", function(event) {
  var player = event.getPlayer();
  
  // Welcome message
  player.sendMessage("§a§lWelcome to the server!");
  player.sendMessage("§7Use §b/ability list §7to see available abilities");
  
  // Give starter item if first join
  if (!player.hasPlayedBefore()) {
    var item = engine.items.create("fireball");
    if (item !== null) {
      player.getInventory().addItem(item);
      player.sendMessage("§eYou received a starter ability!");
    }
  }
});

engine.log("Loaded welcome script");
```

---

## Best Practices

### Use the Engine API

!!! tip "Prefer `engine` API"
    Use `engine.*` methods instead of raw Java when possible:
    - Cleaner syntax
    - Automatic resource cleanup
    - Better error handling

### Handle Errors

```javascript
engine.ability({
  id: "safe_ability",
  execute: function(ctx) {
    try {
      // Your code here
      ctx.player.sendMessage("Success!");
    } catch (e) {
      engine.error("Ability failed: " + e);
      ctx.player.sendMessage("§cAbility failed!");
    }
  }
});
```

### Clean Up Resources

!!! warning "Cancel Tasks in `onEnd`"
    If you schedule tasks in `onStart`, cancel them in `onEnd`:
    ```javascript
    var taskId;
    
    engine.sessions.start(player, ability, {
      onStart: function() {
        taskId = engine.scheduleRepeating(function() {
          // ...
        }, 0, 20);
      },
      onEnd: function() {
        engine.cancelTask(taskId);
      }
    });
    ```

### Test Thoroughly

!!! tip "Testing"
    - Test with `/ability script reload <filename>` for fast iteration
    - Check console logs for errors
    - Use `engine.log()` for debugging
    - Test edge cases (no target, low health, etc.)

### Avoid Blocking Operations

!!! warning "No Blocking"
    Never use blocking operations in scripts:
    - No `Thread.sleep()`
    - No blocking I/O
    - No infinite loops
    
    Use `engine.scheduleDelayed()` or `engine.scheduleRepeating()` instead.

---

## Troubleshooting

### Script not loading

**Check**:

1. File has `.js` extension
2. File is in `plugins/AbilityEngine/scripts/`
3. Check console for JavaScript syntax errors
4. Run `/ability script list` to see loaded scripts

### "Unknown ability" error

**Solution**: Reload scripts with `/ability script reload`

### Ability not firing

**Check**:

1. Conditions are passing
2. Cooldown has expired
3. Trigger type matches action
4. Use `engine.log()` to debug

### Memory leaks

**Solution**:

- Always use `engine.scheduleRepeating()` (not raw Java schedulers)
- Always use `engine.listen()` (not raw Bukkit listeners)
- Sessions auto-cleanup, but call `engine.sessions.end()` when done

---

## Next Steps

- [Module Development](module-development.md) - Build reusable Java modules
- [Sessions Guide](sessions.md) - Deep dive into stateful abilities
- [Scripting API Reference](../reference/scripting-api.md) - Complete API reference
- [Script Examples](../examples/script-examples.md) - More examples

---

## Security Note

!!! warning "Trusted Scripts Only"
    Scripts have unrestricted access to the server:
    - Full Bukkit API access
    - Console command execution
    - File system access
    - No sandboxing
    
    Only load scripts from trusted sources!
