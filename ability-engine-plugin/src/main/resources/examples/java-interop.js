// Example: Raw Java Interop (Power Mode)
// Demonstrates full Java access for advanced users

// Import Java classes directly
const Bukkit = Java.type("org.bukkit.Bukkit");
const Location = Java.type("org.bukkit.Location");
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

engine.log("Loaded java-interop.js with raw Bukkit access");
