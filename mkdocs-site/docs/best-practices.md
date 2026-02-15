# Best Practices

Recommended patterns and practices for creating abilities with AbilityEngine.

---

## General Principles

### Start Simple

Begin with basic abilities and add complexity gradually:

```yaml
# ✓ Good - Start here
fireball:
  display-name: "&cFireball"
  triggers: [RIGHT_CLICK]
  cooldown: 3s
  actions:
    - type: LAUNCH_PROJECTILE
      projectile: FIREBALL

# Later, add more
fireball_advanced:
  display-name: "&cAdvanced Fireball"
  triggers: [RIGHT_CLICK, SHIFT_RIGHT_CLICK]
  conditions:
    - sneaking: true
    - health-above: 5.0
  cooldown: 3s
  actions:
    - type: LAUNCH_PROJECTILE
      projectile: FIREBALL
      speed: 2.5
    - type: VELOCITY
      y: 0.5
    - type: PLAY_SOUND
      sound: ENTITY_BLAZE_SHOOT
```

---

### Test Iteratively

1. Create ability
2. Test in-game
3. Refine
4. Repeat

Use `/ability reload` for fast iteration.

---

### Document Your Work

```yaml
# Combat Abilities Pack
# Created: 2026-02-14
# Purpose: Basic PvP abilities

abilities:
  # Fireball - Basic ranged attack
  # Cooldown: 3s to prevent spam
  fireball:
    display-name: "&cFireball"
    # ... config
```

```javascript
// Utility Abilities
// Author: YourName
// Last updated: 2026-02-14

// Dash - Quick movement ability
// 5 block forward teleport with upward boost
engine.ability({
  id: "dash",
  // ... config
});
```

---

## YAML Abilities

### Naming Conventions

```yaml
# ✓ Good - Clear, descriptive IDs
abilities:
  fireball:           # Simple, clear
  healing_touch:      # Descriptive
  lightning_strike:   # Action-based
  speed_boost:        # Effect-based

# ✗ Bad - Unclear IDs
abilities:
  ability1:           # Not descriptive
  test:               # Too generic
  a:                  # Too short
  super_mega_ultra_fireball_v2_final:  # Too long
```

---

### Organization

**Single File per Category**:

```
abilities/
├── combat.yml        # Combat abilities
├── movement.yml      # Movement abilities
├── utility.yml       # Utility abilities
└── custom.yml        # Server-specific
```

**Or Single File per Ability**:

```
abilities/
├── fireball.yml
├── heal.yml
├── dash.yml
└── lightning.yml
```

Choose what works for your organization.

---

### Cooldown Balancing

```yaml
# Power Level → Cooldown Guide
# Low power: 0-5s
# Medium power: 5-15s
# High power: 15-30s
# Ultimate: 30-60s+

abilities:
  weak_fireball:
    cooldown: 2s      # Weak, fast
  
  fireball:
    cooldown: 5s      # Medium, balanced
  
  meteor:
    cooldown: 30s     # Strong, rare
  
  ultimate_nuke:
    cooldown: 300s    # Very strong, very rare
```

---

### Condition Usage

**Safety First**:

```yaml
# ✓ Good - Prevents low health suicide
meteor:
  conditions:
    - health-above: 5.0   # Don't kill yourself
  actions:
    - type: DAMAGE
      damage: 4.0         # Self-damage
    - type: COMMAND
      command: "summon fireball ~ ~5 ~"
```

**Balanced Abilities**:

```yaml
# ✓ Good - High risk, high reward
berserker:
  conditions:
    - health-below: 10.0  # Only when low HP
  actions:
    - type: POTION_EFFECT
      effect: STRENGTH
      amplifier: 3        # Powerful when desperate
```

---

## JavaScript Scripting

### Code Style

```javascript
// ✓ Good - Clean, readable
engine.ability({
  id: "fireball",
  triggers: ["RIGHT_CLICK"],
  cooldown: 3,
  execute: function(ctx) {
    var player = ctx.player;
    player.launchProjectile(Java.type("org.bukkit.entity.Fireball"));
    player.sendMessage("§cFireball!");
  }
});

// ✗ Bad - Messy, hard to read
engine.ability({id:"fireball",triggers:["RIGHT_CLICK"],cooldown:3,execute:function(ctx){ctx.player.launchProjectile(Java.type("org.bukkit.entity.Fireball"));ctx.player.sendMessage("§cFireball!");}});
```

---

### Error Handling

```javascript
// ✓ Good - Always handle errors
engine.ability({
  id: "safe_ability",
  execute: function(ctx) {
    try {
      // Risky operations
      var result = doSomethingRisky();
      ctx.player.sendMessage("Success!");
    } catch (e) {
      engine.error("Ability failed: " + e);
      ctx.player.sendMessage("§cAbility failed!");
    }
  }
});
```

---

### Null Safety

```javascript
// ✓ Good - Check for null
engine.ability({
  id: "damage_target",
  execute: function(ctx) {
    if (ctx.targetEntity === null) {
      ctx.player.sendMessage("§cNo target!");
      return;
    }
    
    ctx.targetEntity.damage(5.0, ctx.player);
  }
});

// ✗ Bad - No null check (will crash)
engine.ability({
  id: "damage_target",
  execute: function(ctx) {
    ctx.targetEntity.damage(5.0);  // Crash if no target!
  }
});
```

---

### Performance

```javascript
// ✓ Good - Cache expensive lookups
const Particle = Java.type("org.bukkit.Particle");
const LivingEntity = Java.type("org.bukkit.entity.LivingEntity");

engine.ability({
  id: "optimized",
  execute: function(ctx) {
    // Use cached types
    ctx.player.getWorld().spawnParticle(Particle.FLAME, ...);
  }
});

// ✗ Bad - Repeated lookups
engine.ability({
  id: "slow",
  execute: function(ctx) {
    Java.type("org.bukkit.Particle");  // Lookup every time!
    Java.type("org.bukkit.Particle");  // Duplicate!
  }
});
```

---

## Session-Based Abilities

### Always Set Max Duration

```javascript
// ✓ Good - Maximum duration
engine.sessions.start(player, ability, {
  onTick: function(tickCount) {
    if (tickCount > 600) {  // 30 second max
      engine.sessions.end(player, "ability_id");
      return;
    }
    // ... tick logic
  }
});

// ✗ Bad - No maximum (runs forever!)
engine.sessions.start(player, ability, {
  onTick: function(tickCount) {
    // ... tick logic with no end
  }
});
```

---

### Optimize Tick Logic

```javascript
// ✓ Good - Run heavy logic less frequently
onTick: function(tickCount) {
  // Every second (20 ticks)
  if (tickCount % 20 === 0) {
    damageNearbyEntities();  // Expensive
  }
  
  // Every 5 ticks
  if (tickCount % 5 === 0) {
    spawnParticles();  // Medium cost
  }
  
  // Every tick
  updateVelocity();  // Cheap
}

// ✗ Bad - Everything every tick
onTick: function(tickCount) {
  damageNearbyEntities();  // 20 times per second!
  spawnParticles();        // 20 times per second!
  updateVelocity();
}
```

---

### Clean Up Resources

```javascript
// ✓ Good - Always clean up
var taskId;
var particles = [];

engine.sessions.start(player, ability, {
  onStart: function() {
    taskId = engine.scheduleRepeating(function() {
      // ...
    }, 0, 20);
  },
  
  onEnd: function() {
    // Clean up
    if (taskId) {
      engine.cancelTask(taskId);
    }
    particles.forEach(function(p) {
      p.remove();
    });
    particles = [];
  }
});
```

---

## Module Development

### Package Structure

```
com.yourname.abilities/
├── AbilityModule.java          # Main module class
├── abilities/                  # Ability implementations
│   ├── FireballAbility.java
│   ├── HealAbility.java
│   └── DashAbility.java
├── conditions/                 # Custom conditions
│   └── CustomConditions.java
├── listeners/                  # Event listeners
│   └── AbilityListener.java
└── util/                       # Utilities
    └── AbilityUtils.java
```

---

### Dependency Injection

```java
// ✓ Good - Pass dependencies
public class MyModule implements AbilityModule {
    private AbilityRegistry registry;
    private CooldownManager cooldowns;
    private AbilityItemService items;
    
    @Override
    public void onEnable(AbilityRegistry registry,
                         CooldownManager cooldowns,
                         AbilityItemService items) {
        this.registry = registry;
        this.cooldowns = cooldowns;
        this.items = items;
        
        // Use injected dependencies
        registry.register(new CustomAbility(cooldowns));
    }
}
```

---

### Resource Management

```java
// ✓ Good - Proper cleanup
public class MyModule implements AbilityModule {
    private BukkitTask task;
    private MyListener listener;
    
    @Override
    public void onEnable(...) {
        listener = new MyListener();
        Bukkit.getPluginManager().registerEvents(listener, plugin);
        
        task = Bukkit.getScheduler().runTaskTimer(plugin, ...);
    }
    
    @Override
    public void onDisable() {
        // Cancel tasks
        if (task != null) task.cancel();
        
        // Unregister listeners
        HandlerList.unregisterAll(listener);
    }
}
```

---

## Security

### Validate User Input

```javascript
// ✓ Good - Validate parameters
function teleportPlayer(player, x, y, z) {
  // Validate coordinates
  if (x < -30000000 || x > 30000000 ||
      y < -64 || y > 320 ||
      z < -30000000 || z > 30000000) {
    player.sendMessage("§cInvalid coordinates!");
    return false;
  }
  
  player.teleport(new Location(player.getWorld(), x, y, z));
  return true;
}
```

---

### Limit Resource Usage

```yaml
# ✓ Good - Reasonable cooldowns prevent spam
abilities:
  spawn_mob:
    cooldown: 60s  # Prevent entity spam
    actions:
      - type: SPAWN_ENTITY
        entity: ZOMBIE
```

```javascript
// ✓ Good - Limit session count
engine.ability({
  id: "aura",
  execute: function(ctx) {
    var active = engine.sessions.getActive(ctx.player);
    
    if (active.length >= 3) {
      ctx.player.sendMessage("§cToo many active abilities!");
      return;
    }
    
    engine.sessions.start(...);
  }
});
```

---

## Testing

### Test Edge Cases

```javascript
// Test checklist:
// ✓ No target entity
// ✓ Low health
// ✓ High altitude
// ✓ Different world types
// ✓ Creative mode players
// ✓ Permission checks

engine.ability({
  id: "robust_ability",
  execute: function(ctx) {
    // Check for edge cases
    if (ctx.targetEntity === null) {
      ctx.player.sendMessage("§cNo target!");
      return;
    }
    
    if (ctx.player.getHealth() <= 2.0) {
      ctx.player.sendMessage("§cToo low health!");
      return;
    }
    
    // Execute ability
    // ...
  }
});
```

---

### Use Debug Mode

```javascript
// ✓ Good - Debug flag for testing
var DEBUG = true;

engine.ability({
  id: "test_ability",
  execute: function(ctx) {
    if (DEBUG) {
      engine.log("Executing test_ability");
      engine.log("Player: " + ctx.player.getName());
      engine.log("Health: " + ctx.player.getHealth());
    }
    
    // ... ability logic
  }
});
```

---

## Documentation

### Document Complex Logic

```javascript
/**
 * Grappling Hook Ability
 * 
 * Pulls player towards target block over 2 seconds.
 * Cancels if player takes damage or disconnects.
 * 
 * Cooldown: 8 seconds
 * Max range: 30 blocks
 * Duration: 40 ticks (2 seconds)
 */
engine.ability({
  id: "grappling_hook",
  cooldown: 8,
  execute: function(ctx) {
    // Get target block (max 30 blocks)
    var target = ctx.player.getTargetBlock(null, 30);
    
    // Validate target
    if (target === null || target.isEmpty()) {
      ctx.player.sendMessage("§cNo valid target!");
      return;
    }
    
    // Calculate pull direction
    var direction = target.getLocation().toVector()
      .subtract(ctx.player.getLocation().toVector())
      .normalize();
    
    // Start pull session
    engine.sessions.start(ctx.player, {id: "grappling_hook"}, {
      onTick: function(tickCount) {
        // Pull for 2 seconds (40 ticks)
        if (tickCount > 40) {
          engine.sessions.end(ctx.player, "grappling_hook");
          return;
        }
        
        // Apply velocity towards target
        ctx.player.setVelocity(direction.multiply(0.8));
      }
    });
  }
});
```

---

## See Also

- [FAQ](faq.md) - Common questions
- [Troubleshooting](troubleshooting.md) - Fixing issues
- [YAML Guide](guides/yaml-abilities.md) - YAML reference
- [Scripting Guide](guides/scripting.md) - JavaScript reference
