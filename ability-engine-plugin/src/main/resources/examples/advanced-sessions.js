// Example: Advanced Session-Based Ability
// Demonstrates stateful abilities with sessions

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

engine.log("Loaded advanced-sessions.js");
