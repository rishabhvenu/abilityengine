// Example 1: Simple Dash Ability
// Demonstrates basic ability DSL with triggers and cooldowns

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

// Example 2: Heal Pulse
// Demonstrates conditions and health manipulation

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

engine.log("Loaded basic-abilities.js");
