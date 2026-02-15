# JavaScript Script Examples

Curated examples of JavaScript abilities using the scripting API.

---

## Basic Abilities

### Simple Dash

From `basic-abilities.js`:

```javascript
engine.ability({
  id: "dash",
  triggers: ["SHIFT_RIGHT_CLICK"],
  cooldown: 5,
  execute: function(ctx) {
    var direction = ctx.player.getLocation().getDirection();
    ctx.player.setVelocity(direction.multiply(2.5));
    ctx.player.sendMessage("§bDash!");
    
    engine.log("Player " + ctx.player.getName() + " used dash");
  }
});
```

---

### Heal Pulse

```javascript
engine.ability({
  id: "heal_pulse",
  triggers: ["RIGHT_CLICK"],
  conditions: [engine.condition.sneaking()],
  cooldown: 10,
  execute: function(ctx) {
    var currentHealth = ctx.player.getHealth();
    var maxHealth = ctx.player.getMaxHealth();
    var newHealth = Math.min(currentHealth + 6, maxHealth);
    
    ctx.player.setHealth(newHealth);
    ctx.player.sendMessage("§aHealed!");
  }
});
```

---

## Java Interop Examples

### Lightning Strike

From `java-interop.js`:

```javascript
const Bukkit = Java.type("org.bukkit.Bukkit");
const Particle = Java.type("org.bukkit.Particle");
const Sound = Java.type("org.bukkit.Sound");

engine.ability({
  id: "lightning_strike",
  triggers: ["RIGHT_CLICK_ENTITY"],
  cooldown: 15,
  execute: function(ctx) {
    if (!ctx.targetEntity) {
      ctx.player.sendMessage("§cNo target!");
      return;
    }
    
    // Use raw Java API to spawn lightning
    var targetLoc = ctx.targetEntity.getLocation();
    ctx.player.getWorld().strikeLightning(targetLoc);
    
    // Raw Bukkit broadcast
    Bukkit.broadcastMessage("§e" + ctx.player.getName() + " struck lightning!");
    
    // Spawn particles using Java enum
    ctx.player.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, targetLoc, 5);
    
    // Play sound
    ctx.player.playSound(ctx.player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0, 1.0);
  }
});
```

---

## Event Listeners

### Welcome Message

From `event-listeners.js`:

```javascript
engine.listen("PlayerJoinEvent", function(event) {
  var player = event.getPlayer();
  player.sendMessage("§aWelcome! AbilityEngine scripts are active.");
  engine.log(player.getName() + " joined the server");
});
```

---

### Death Message Modifier

```javascript
engine.listen("PlayerDeathEvent", function(event) {
  var player = event.getEntity();
  
  // Check if they have any active abilities
  var activeSessions = engine.sessions.getActive(player);
  
  if (activeSessions.length > 0) {
    event.setDeathMessage(player.getName() + " died while using abilities!");
  }
});
```

---

### Scheduled Tasks

```javascript
// Repeating task every 60 seconds
engine.scheduleRepeating(function() {
  const Bukkit = Java.type("org.bukkit.Bukkit");
  var onlinePlayers = Bukkit.getOnlinePlayers().size();
  
  if (onlinePlayers > 0) {
    engine.log("Players online: " + onlinePlayers);
  }
}, 20 * 60, 20 * 60); // Delay and period in ticks
```

---

## Session-Based Abilities

### Fire Aura

From `advanced-sessions.js`:

```javascript
engine.ability({
  id: "fire_aura",
  triggers: ["SHIFT_RIGHT_CLICK"],
  cooldown: 30,
  execute: function(ctx) {
    ctx.player.sendMessage("§cFire Aura activated!");
    
    // Start a session that runs every tick
    engine.sessions.start(ctx.player, {id: "fire_aura"}, {
      onStart: function() {
        engine.log("Fire aura started for " + ctx.player.getName());
      },
      
      onTick: function(tickCount) {
        // Run for 10 seconds (200 ticks)
        if (tickCount > 200) {
          engine.sessions.end(ctx.player, "fire_aura");
          return;
        }
        
        // Every 20 ticks (1 second), damage nearby entities
        if (tickCount % 20 === 0) {
          const LivingEntity = Java.type("org.bukkit.entity.LivingEntity");
          const Particle = Java.type("org.bukkit.Particle");
          
          var location = ctx.player.getLocation();
          var world = ctx.player.getWorld();
          
          // Spawn particles
          world.spawnParticle(Particle.FLAME, location, 20, 1.0, 1.0, 1.0, 0.1);
          
          // Damage nearby entities
          var nearbyEntities = world.getNearbyEntities(location, 3, 3, 3);
          nearbyEntities.forEach(function(entity) {
            if (entity instanceof LivingEntity && entity !== ctx.player) {
              entity.damage(2.0, ctx.player);
            }
          });
        }
      },
      
      onEnd: function() {
        ctx.player.sendMessage("§cFire Aura ended!");
        engine.log("Fire aura ended for " + ctx.player.getName());
      }
    });
  }
});
```

---

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
    
    // Start pull session
    engine.sessions.start(ctx.player, {id: "grappling_hook"}, {
      onStart: function() {
        ctx.player.sendMessage("§bGrappling!");
      },
      
      onTick: function(tickCount) {
        // Pull for 2 seconds (40 ticks)
        if (tickCount > 40) {
          engine.sessions.end(ctx.player, "grappling_hook");
          return;
        }
        
        // Apply velocity towards target
        ctx.player.setVelocity(direction.multiply(0.8));
        
        // Spawn particles every 2 ticks
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

### Healing Channel

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
        // Channel for 5 seconds (100 ticks)
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
        
        // Heal every second (20 ticks)
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

---

## Advanced Patterns

### Toggleable Shield

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
          // Show particles every 10 ticks
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

### Charging Ability

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
        // Charge for up to 5 seconds (100 ticks)
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
      },
      
      onEnd: function() {
        ctx.player.sendMessage("§eBlast released at level " + chargeLevel + "!");
      }
    });
  }
});

function releaseBlast(player, level) {
  const Fireball = Java.type("org.bukkit.entity.Fireball");
  var projectile = player.launchProjectile(Fireball);
  projectile.setYield(level); // Explosion power based on charge
}
```

---

## Complete Script File

```javascript
// Load this file into plugins/AbilityEngine/scripts/

// Abilities
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

engine.ability({
  id: "heal",
  triggers: ["RIGHT_CLICK"],
  conditions: [engine.condition.healthBelow(15.0)],
  cooldown: 10,
  execute: function(ctx) {
    var newHealth = Math.min(
      ctx.player.getHealth() + 6,
      ctx.player.getMaxHealth()
    );
    ctx.player.setHealth(newHealth);
    ctx.player.sendMessage("§aHealed!");
  }
});

// Event Listeners
engine.listen("PlayerJoinEvent", function(event) {
  event.getPlayer().sendMessage("§aWelcome!");
});

engine.log("Loaded custom abilities script");
```

---

## See Also

- [Scripting Guide](../guides/scripting.md) - Complete guide
- [Scripting API Reference](../reference/scripting-api.md) - API docs
- [Sessions Guide](../guides/sessions.md) - Sessions deep-dive
- [YAML Examples](yaml-examples.md) - YAML ability examples
