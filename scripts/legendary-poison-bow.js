// ============================================================
// Legendary Poison Bow
// Place in: plugins/AbilityEngine/scripts/legendary-poison-bow.js
//
// Abilities:
//   1. Poison Cloud    - Double shift while holding bow
//   2. Poisonous Arrow - Left click while holding bow
//
// Uses the new high-level scripting APIs for clean, declarative code.
// ============================================================

// ================================================================
//  ABILITY: Poison Cloud (Double Shift)
// ================================================================
engine.ability({
  id: "poison_cloud",
  trigger: "DOUBLE_SHIFT",
  cooldown: {
    seconds: 15,
    showBossBar: true,
    bossBarColor: "GREEN",
    bossBarLabel: "Poison Cloud"
  },
  execute: function(ctx) {
    engine.areaEffect.spawn({
      location: ctx.player().getLocation(),
      source: ctx.player(),
      radius: 4,
      duration: 200,  // 10 seconds
      color: "#00AA00",
      potion: {type: "POISON", duration: 100, amplifier: 1},
      excludeCaster: true,
      radiusShrink: 0.005
    });
    
    ctx.player().sendMessage("§2§lPoison Cloud!");
    engine.effects.sound(ctx.player().getLocation(), "entity.witch.throw", 1.0, 0.8);
  }
});

// ================================================================
//  ABILITY: Poisonous Arrow (Left Click)
// ================================================================
engine.ability({
  id: "poisonous_arrow",
  trigger: "LEFT_CLICK",
  cooldown: {
    seconds: 8,
    showBossBar: true,
    bossBarColor: "GREEN",
    bossBarLabel: "Poisonous Arrow"
  },
  execute: function(ctx) {
    engine.projectile.spawn({
      type: "ARROW",
      shooter: ctx.player(),
      speed: 2.5,
      damage: 8,
      critical: true,
      potion: {type: "POISON", duration: 100, amplifier: 1},
      trail: {
        particle: "ENTITY_EFFECT",
        color: "#00AA00",
        count: 5,
        spread: 0.2
      },
      onTick: function(projectile, tickCount) {
        // Decay terrain around arrow trail
        if (tickCount % 5 === 0) {  // Every 5 ticks
          engine.effects.decayTerrain(
            projectile.getLocation(),
            1,
            "NATURE_ONLY"
          );
        }
      },
      maxTicks: 100  // 5 seconds
    });
    
    ctx.player().sendMessage("§2§lPoisonous Arrow!");
    engine.effects.sound(ctx.player().getLocation(), "entity.arrow.shoot", 1.0, 0.7);
  }
});

// ================================================================
//  Create the Legendary Poison Bow Item
// ================================================================
engine.item({
  id: "legendary_poison_bow",
  type: "BOW",
  name: "&5&lLegendary Poison Bow",
  lore: [
    "&aPoison Cloud &7- Double Shift",
    "&aPoisonous Arrow &7- Left Click"
  ],
  abilities: ["poison_cloud", "poisonous_arrow"],
  unbreakable: true
});

// ================================================================
//  Give bow on join
// ================================================================
engine.ability({
  id: "give_legendary_bow",
  trigger: "ON_JOIN",
  execute: function(ctx) {
    engine.items.give(ctx.player(), "legendary_poison_bow");
    ctx.player().sendMessage("§5§lYou received the Legendary Poison Bow!");
  }
});

engine.log("[LegendaryPoisonBow] Script loaded successfully.");
