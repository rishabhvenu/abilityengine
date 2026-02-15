# Sessions & Stateful Abilities

Learn how to create abilities with continuous effects using the session system.

---

## Overview

Sessions allow you to create stateful abilities that run over time. Unlike instant abilities that execute once, session-based abilities have a lifecycle with continuous tick-based execution.

**Use Cases**:

- **Grappling hooks** - Continuous pull towards target
- **Channeling abilities** - Require player to stand still
- **Auras** - Continuous area effects
- **Active buffs** - Effects that persist for a duration
- **Charging mechanics** - Building up power over time

---

## Session Lifecycle

Every session goes through three phases:

```
start() → tick() → tick() → tick() → ... → end()
```

1. **`start()`** - Called once when the session begins
2. **`tick()`** - Called every tick (50ms) while active
3. **`end()`** - Called once when the session ends

---

## Creating Sessions in JavaScript

### Basic Session

```javascript
engine.ability({
  id: "fire_aura",
  triggers: ["RIGHT_CLICK"],
  cooldown: 30,
  execute: function(ctx) {
    engine.sessions.start(ctx.player, {id: "fire_aura"}, {
      onStart: function() {
        ctx.player.sendMessage("§cFire Aura activated!");
      },
      
      onTick: function(tickCount) {
        // Run for 10 seconds (200 ticks)
        if (tickCount > 200) {
          engine.sessions.end(ctx.player, "fire_aura");
          return;
        }
        
        // Do something every tick
        if (tickCount % 20 === 0) {
          ctx.player.sendMessage("§cTick: " + (tickCount / 20));
        }
      },
      
      onEnd: function() {
        ctx.player.sendMessage("§cAura ended!");
      }
    });
  }
});
```

### Damage Aura

```javascript
engine.ability({
  id: "damage_aura",
  triggers: ["RIGHT_CLICK"],
  cooldown: 30,
  execute: function(ctx) {
    ctx.player.sendMessage("§4Damage Aura!");
    
    engine.sessions.start(ctx.player, {id: "damage_aura"}, {
      onStart: function() {
        const Particle = Java.type("org.bukkit.Particle");
        ctx.player.getWorld().spawnParticle(
          Particle.EXPLOSION_LARGE,
          ctx.player.getLocation(),
          1
        );
      },
      
      onTick: function(tickCount) {
        // Run for 10 seconds
        if (tickCount > 200) {
          engine.sessions.end(ctx.player, "damage_aura");
          return;
        }
        
        // Damage every second
        if (tickCount % 20 === 0) {
          const LivingEntity = Java.type("org.bukkit.entity.LivingEntity");
          const Particle = Java.type("org.bukkit.Particle");
          
          var loc = ctx.player.getLocation();
          var world = ctx.player.getWorld();
          
          // Spawn particles
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
        ctx.player.sendMessage("§4Aura ended!");
      }
    });
  }
});
```

### Grappling Hook

```javascript
engine.ability({
  id: "grappling_hook",
  triggers: ["RIGHT_CLICK"],
  cooldown: 8,
  execute: function(ctx) {
    // Get target block
    var targetBlock = ctx.player.getTargetBlock(null, 30);
    if (targetBlock === null || targetBlock.isEmpty()) {
      ctx.player.sendMessage("§cNo valid target!");
      return;
    }
    
    var targetLoc = targetBlock.getLocation();
    var playerLoc = ctx.player.getLocation();
    
    // Calculate direction
    var direction = targetLoc.toVector()
      .subtract(playerLoc.toVector())
      .normalize();
    
    // Start session
    engine.sessions.start(ctx.player, {id: "grappling_hook"}, {
      onStart: function() {
        ctx.player.sendMessage("§bGrappling!");
      },
      
      onTick: function(tickCount) {
        // Pull for 2 seconds
        if (tickCount > 40) {
          engine.sessions.end(ctx.player, "grappling_hook");
          return;
        }
        
        // Apply velocity towards target
        ctx.player.setVelocity(direction.multiply(0.8));
        
        // Spawn particles
        if (tickCount % 2 === 0) {
          const Particle = Java.type("org.bukkit.Particle");
          ctx.player.getWorld().spawnParticle(
            Particle.CLOUD,
            ctx.player.getLocation(),
            3
          );
        }
      },
      
      onEnd: function() {
        ctx.player.sendMessage("§bGrapple complete!");
      }
    });
  }
});
```

---

## Creating Sessions in Java

### Implementing AbilitySession

```java
public class FireAuraSession extends BaseAbilitySession {
    private int duration;
    
    public FireAuraSession(Player player, Ability ability, int durationTicks) {
        super(player, ability);
        this.duration = durationTicks;
    }
    
    @Override
    public void start() {
        super.start();
        player().sendMessage("§cFire Aura activated!");
    }
    
    @Override
    public void tick() {
        super.tick();
        
        // Check if duration expired
        if (getTickCount() > duration) {
            end();
            return;
        }
        
        // Execute every second
        if (getTickCount() % 20 == 0) {
            damageNearbyEntities();
            spawnParticles();
        }
    }
    
    @Override
    public void end() {
        super.end();
        player().sendMessage("§cAura ended!");
    }
    
    private void damageNearbyEntities() {
        var location = player().getLocation();
        var world = player().getWorld();
        
        world.getNearbyEntities(location, 3, 3, 3).stream()
            .filter(e -> e instanceof LivingEntity)
            .filter(e -> e != player())
            .map(e -> (LivingEntity) e)
            .forEach(e -> e.damage(2.0, player()));
    }
    
    private void spawnParticles() {
        player().getWorld().spawnParticle(
            Particle.FLAME,
            player().getLocation(),
            20,
            1.0, 1.0, 1.0,
            0.1
        );
    }
}
```

### Using Sessions in Abilities

```java
public class FireAuraAbility implements Ability {
    private final SessionManager sessionManager;
    
    public FireAuraAbility(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }
    
    @Override
    public String id() {
        return "fire_aura";
    }
    
    @Override
    public Collection<TriggerType> triggers() {
        return List.of(TriggerType.RIGHT_CLICK);
    }
    
    @Override
    public List<Condition> conditions() {
        return List.of();
    }
    
    @Override
    public void execute(AbilityContext context) {
        var session = new FireAuraSession(
            context.player(),
            this,
            200 // 10 seconds
        );
        
        sessionManager.startSession(context.player(), this, session);
    }
    
    @Override
    public Duration cooldown() {
        return Duration.ofSeconds(30);
    }
}
```

---

## Session Management API

### Starting Sessions

**JavaScript**:

```javascript
engine.sessions.start(player, ability, handlers)
```

**Java**:

```java
sessionManager.startSession(player, ability, session)
```

### Ending Sessions

**JavaScript**:

```javascript
// End all sessions for this ability
engine.sessions.end(player, "ability_id")
```

**Java**:

```java
sessionManager.endSession(player, "ability_id")
```

### Checking Active Sessions

**JavaScript**:

```javascript
var active = engine.sessions.getActive(player);
if (active.includes("fire_aura")) {
  player.sendMessage("Fire aura is active!");
}
```

**Java**:

```java
Collection<AbilitySession> activeSessions = 
    sessionManager.getActiveSessions(player);
```

---

## Advanced Patterns

### Charging Ability

Build up power over time:

```javascript
engine.ability({
  id: "charged_blast",
  triggers: ["RIGHT_CLICK"],
  cooldown: 15,
  execute: function(ctx) {
    var chargeLevel = 0;
    
    ctx.player.sendMessage("§eCharging... Hold still!");
    
    engine.sessions.start(ctx.player, {id: "charged_blast"}, {
      onStart: function() {
        chargeLevel = 0;
      },
      
      onTick: function(tickCount) {
        // Charge for up to 5 seconds
        if (tickCount > 100) {
          releaseBlast(ctx.player, chargeLevel);
          engine.sessions.end(ctx.player, "charged_blast");
          return;
        }
        
        // Increase charge every 10 ticks
        if (tickCount % 10 === 0) {
          chargeLevel++;
          
          // Visual feedback
          const Particle = Java.type("org.bukkit.Particle");
          ctx.player.getWorld().spawnParticle(
            Particle.ENCHANTMENT_TABLE,
            ctx.player.getLocation(),
            chargeLevel * 2
          );
        }
        
        // Cancel if player moves
        // (You'd track the initial location and compare)
      },
      
      onEnd: function() {
        ctx.player.sendMessage("§eBlast released at level " + chargeLevel + "!");
      }
    });
  }
});

function releaseBlast(player, level) {
  // More powerful based on charge level
  const Fireball = Java.type("org.bukkit.entity.Fireball");
  var projectile = player.launchProjectile(Fireball);
  projectile.setYield(level); // Explosion power
}
```

### Channel Ability

Require player to stand still:

```javascript
engine.ability({
  id: "healing_channel",
  triggers: ["RIGHT_CLICK"],
  cooldown: 20,
  execute: function(ctx) {
    var startLoc = ctx.player.getLocation().clone();
    var healAmount = 0;
    
    ctx.player.sendMessage("§aChanneling heal... Don't move!");
    
    engine.sessions.start(ctx.player, {id: "healing_channel"}, {
      onTick: function(tickCount) {
        // Channel for 5 seconds
        if (tickCount > 100) {
          engine.sessions.end(ctx.player, "healing_channel");
          return;
        }
        
        // Check if player moved
        var currentLoc = ctx.player.getLocation();
        if (currentLoc.distance(startLoc) > 0.5) {
          ctx.player.sendMessage("§cChannel interrupted!");
          engine.sessions.end(ctx.player, "healing_channel");
          return;
        }
        
        // Heal every second
        if (tickCount % 20 === 0) {
          var player = ctx.player;
          var newHealth = Math.min(
            player.getHealth() + 2,
            player.getMaxHealth()
          );
          player.setHealth(newHealth);
          healAmount += 2;
          
          // Particles
          const Particle = Java.type("org.bukkit.Particle");
          player.getWorld().spawnParticle(
            Particle.HEART,
            player.getLocation().add(0, 2, 0),
            5
          );
        }
      },
      
      onEnd: function() {
        ctx.player.sendMessage("§aHealed " + healAmount + " HP!");
      }
    });
  }
});
```

### Toggleable Ability

Turn on/off:

```javascript
engine.ability({
  id: "shield_toggle",
  triggers: ["RIGHT_CLICK"],
  cooldown: 0, // No cooldown for toggle
  execute: function(ctx) {
    var active = engine.sessions.getActive(ctx.player);
    
    if (active.includes("shield_toggle")) {
      // Turn off
      engine.sessions.end(ctx.player, "shield_toggle");
      ctx.player.sendMessage("§7Shield deactivated");
    } else {
      // Turn on
      engine.sessions.start(ctx.player, {id: "shield_toggle"}, {
        onStart: function() {
          ctx.player.sendMessage("§bShield activated!");
        },
        
        onTick: function(tickCount) {
          // Reduce damage (you'd need event listeners for this)
          // Here we just show particles
          if (tickCount % 10 === 0) {
            const Particle = Java.type("org.bukkit.Particle");
            ctx.player.getWorld().spawnParticle(
              Particle.BARRIER,
              ctx.player.getLocation(),
              5,
              0.5, 1, 0.5,
              0
            );
          }
        },
        
        onEnd: function() {
          ctx.player.sendMessage("§7Shield deactivated");
        }
      });
    }
  }
});
```

---

## Best Practices

### Performance

!!! warning "Tick Performance"
    Sessions tick every 50ms. Avoid expensive operations in `onTick()`:
    
    - Don't iterate over all players/entities every tick
    - Cache calculations when possible
    - Use modulo (`tickCount % 20`) to run code less frequently

**Good**:

```javascript
onTick: function(tickCount) {
  // Only run every second
  if (tickCount % 20 === 0) {
    expensiveOperation();
  }
}
```

**Bad**:

```javascript
onTick: function(tickCount) {
  // Runs 20 times per second!
  expensiveOperation();
}
```

### Resource Cleanup

!!! tip "Clean Up in onEnd"
    Always clean up resources in `onEnd()`:
    
    - Cancel scheduled tasks
    - Remove temporary blocks
    - Clear cached data

```javascript
var taskId;

engine.sessions.start(player, ability, {
  onStart: function() {
    taskId = engine.scheduleRepeating(function() {
      // ...
    }, 0, 20);
  },
  
  onEnd: function() {
    engine.cancelTask(taskId); // Important!
  }
});
```

### Duration Limits

!!! tip "Set Maximum Duration"
    Always set a maximum duration to prevent infinite sessions:

```javascript
onTick: function(tickCount) {
  if (tickCount > 1200) { // 60 seconds max
    engine.sessions.end(player, "ability_id");
    return;
  }
  // ... rest of code
}
```

### Player Disconnect

!!! info "Automatic Cleanup"
    Sessions are automatically ended when:
    
    - Player disconnects
    - Plugin disables
    - `end()` is called
    
    The `onEnd()` handler is called in all cases.

---

## Common Pitfalls

### Forgetting to End Sessions

**Problem**: Session runs forever

```javascript
// BAD - no exit condition
onTick: function(tickCount) {
  damageNearby();
}
```

**Solution**: Always have an exit condition

```javascript
// GOOD
onTick: function(tickCount) {
  if (tickCount > 200) {
    engine.sessions.end(player, "ability_id");
    return;
  }
  damageNearby();
}
```

### Heavy Operations Every Tick

**Problem**: Lags the server

```javascript
// BAD - expensive every tick
onTick: function(tickCount) {
  getAllPlayers().forEach(function(p) {
    checkDistance(p);
  });
}
```

**Solution**: Run less frequently

```javascript
// GOOD - only every second
onTick: function(tickCount) {
  if (tickCount % 20 === 0) {
    getAllPlayers().forEach(function(p) {
      checkDistance(p);
    });
  }
}
```

### Not Cleaning Up Tasks

**Problem**: Tasks continue after session ends

```javascript
// BAD
onStart: function() {
  engine.scheduleRepeating(function() {
    // This never stops!
  }, 0, 20);
}
```

**Solution**: Cancel in `onEnd()`

```javascript
// GOOD
var taskId;
onStart: function() {
  taskId = engine.scheduleRepeating(function() {
    // ...
  }, 0, 20);
},
onEnd: function() {
  engine.cancelTask(taskId);
}
```

---

## Examples

See the [Script Examples](../examples/script-examples.md) page for more complete session-based ability examples.

---

## Next Steps

- [Items Guide](items.md) - Working with ability items
- [Scripting API Reference](../reference/scripting-api.md) - Complete API docs
- [Script Examples](../examples/script-examples.md) - More examples
