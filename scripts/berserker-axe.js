// ============================================================
// Berserker Axe
// Place in: plugins/AbilityEngine/scripts/berserker-axe.js
//
// Abilities:
//   1. Overcharge  - Left click: wielder emits red particles and
//                    flame sounds. Next melee hit inflicts a
//                    bleeding effect with red particles (1.5s).
//   2. Earthquake  - Double shift: slams the ground, sending
//                    shockwaves that launch nearby entities
//                    into the air.
//
// Item: Netherite axe with both abilities wired up.
// ============================================================

var Vector    = Java.type("org.bukkit.util.Vector");
var Location  = Java.type("org.bukkit.Location");
var Particle  = Java.type("org.bukkit.Particle");
var DustOptions = Java.type("org.bukkit.Particle$DustOptions");
var Color     = Java.type("org.bukkit.Color");
var Material  = Java.type("org.bukkit.Material");
var Float     = Java.type("java.lang.Float");

// ─── Overcharge constants ────────────────────────────────────
var OVERCHARGE_DURATION_TICKS = 100;   // 5 seconds to land a hit
var BLEED_DURATION_TICKS      = 30;    // 1.5 seconds
var BLEED_DAMAGE              = 1.5;   // damage per bleed tick
var BLEED_TICK_INTERVAL       = 5;     // apply damage every 5 ticks

// ─── Earthquake constants ────────────────────────────────────
var QUAKE_RADIUS       = 8;
var QUAKE_LAUNCH_POWER = 1.4;
var QUAKE_DAMAGE       = 6;
var QUAKE_WAVES        = 5;
var QUAKE_WAVE_DELAY   = 3;

// ─── Particle colours ────────────────────────────────────────
var BLOOD_DUST      = new DustOptions(Color.fromRGB(160, 10, 10),  Float.parseFloat("1.5"));
var OVERCHARGE_DUST = new DustOptions(Color.fromRGB(255, 30, 10),  Float.parseFloat("1.8"));
var OVERCHARGE_GLOW = new DustOptions(Color.fromRGB(255, 80, 0),   Float.parseFloat("1.2"));
var QUAKE_DUST      = new DustOptions(Color.fromRGB(140, 90, 40),  Float.parseFloat("2.0"));
var QUAKE_RING_DUST = new DustOptions(Color.fromRGB(180, 120, 50), Float.parseFloat("1.4"));

// Track overcharged players — UUID string → true
var overchargedPlayers = {};

// ================================================================
//  ABILITY: Overcharge (Left Click)
//  Activates a fiery aura. The next melee strike consumes it and
//  inflicts 1.5 seconds of bleeding damage on the target.
// ================================================================
engine.ability({
  id: "overcharge_activate",
  trigger: "RIGHT_CLICK",
  cooldown: {
    seconds: 8,
    showBossBar: true,
    bossBarColor: "RED",
    bossBarLabel: "Overcharge"
  },
  interrupts: ["DEATH", "QUIT"],
  onInterrupt: function(ctx) {
    delete overchargedPlayers[ctx.player().getUniqueId().toString()];
  },
  execute: function(ctx) {
    var player = ctx.player();
    var pid    = player.getUniqueId().toString();

    if (overchargedPlayers[pid]) return;

    overchargedPlayers[pid] = true;
    player.sendMessage("\u00A7c\u00A7lOvercharged!");

    engine.effects.sound(player.getLocation(), "entity.blaze.ambient", 1.0, 0.5);
    engine.effects.sound(player.getLocation(), "item.firecharge.use", 0.8, 0.7);

    var tick = 0;

    var taskId = ctx.scheduleRepeating(function() {
      tick++;

      if (!player.isOnline()) {
        delete overchargedPlayers[pid];
        ctx.cancelTask(taskId);
        return;
      }

      // Buff expired without a hit
      if (tick > OVERCHARGE_DURATION_TICKS || !overchargedPlayers[pid]) {
        if (overchargedPlayers[pid]) {
          delete overchargedPlayers[pid];
          player.sendMessage("\u00A77Overcharge faded...");
        }
        ctx.cancelTask(taskId);
        return;
      }

      var pLoc  = player.getLocation().add(0, 1, 0);
      var world = player.getWorld();

      // Swirling red dust helix
      var angle = (tick * 0.3) % (Math.PI * 2);
      for (var i = 0; i < 6; i++) {
        var a  = angle + (i / 6.0) * Math.PI * 2;
        var px = Math.cos(a) * 0.8;
        var pz = Math.sin(a) * 0.8;
        var py = Math.sin(tick * 0.15 + i) * 0.4;

        world.spawnParticle(Particle.DUST,
          pLoc.getX() + px, pLoc.getY() + py, pLoc.getZ() + pz,
          1, 0, 0, 0, 0, OVERCHARGE_DUST);
      }

      // Outer embers
      if (tick % 2 === 0) {
        world.spawnParticle(Particle.FLAME, pLoc, 3, 0.4, 0.5, 0.4, 0.01);
        world.spawnParticle(Particle.DUST, pLoc, 2, 0.5, 0.6, 0.5, 0, OVERCHARGE_GLOW);
      }

      // Smoke wisps
      world.spawnParticle(Particle.LARGE_SMOKE, pLoc, 1, 0.3, 0.4, 0.3, 0.005);

      // Periodic flame ambience
      if (tick % 10 === 0) {
        engine.effects.sound(pLoc, "block.fire.ambient", 0.6,
          0.5 + Math.random() * 0.3);
      }
      if (tick % 25 === 0) {
        engine.effects.sound(pLoc, "entity.blaze.burn", 0.3, 0.6);
      }
    }, 1, 1);
  }
});

// ================================================================
//  ABILITY: Overcharge — Bleed on Hit (DAMAGE_DEALT)
//  When a hit lands while overcharged, consume the buff and start
//  a 1.5-second bleeding effect on the target.
// ================================================================
engine.ability({
  id: "overcharge_bleed",
  trigger: "DAMAGE_DEALT",
  execute: function(ctx) {
    var player = ctx.player();
    var pid    = player.getUniqueId().toString();

    if (!overchargedPlayers[pid]) return;

    var target = ctx.targetEntity();
    if (target == null) return;

    // Consume the buff
    delete overchargedPlayers[pid];

    player.sendMessage("\u00A7c\u00A7lBleeding strike!");
    engine.effects.sound(target.getLocation(), "entity.player.attack.crit", 1.0, 0.6);
    engine.effects.sound(target.getLocation(), "entity.player.attack.strong", 0.8, 0.5);

    var world = target.getWorld();

    // Impact burst of blood particles
    world.spawnParticle(Particle.DUST,
      target.getLocation().add(0, 1, 0), 25, 0.5, 0.7, 0.5, 0, BLOOD_DUST);
    world.spawnParticle(Particle.FLAME,
      target.getLocation().add(0, 1, 0), 8, 0.3, 0.4, 0.3, 0.04);

    var bleedTick = 0;

    // 1.5-second bleeding DOT with red particles
    var bleedId = ctx.scheduleRepeating(function() {
      bleedTick++;

      if (bleedTick > BLEED_DURATION_TICKS) {
        ctx.cancelTask(bleedId);
        return;
      }

      if (!target.isValid() || target.isDead()) {
        ctx.cancelTask(bleedId);
        return;
      }

      var tLoc = target.getLocation().add(0, 1, 0);

      // Continuous blood particles swirling around target
      var bAngle = (bleedTick * 0.4) % (Math.PI * 2);
      for (var i = 0; i < 4; i++) {
        var ba = bAngle + (i / 4.0) * Math.PI * 2;
        var bx = Math.cos(ba) * 0.6;
        var bz = Math.sin(ba) * 0.6;
        world.spawnParticle(Particle.DUST,
          tLoc.getX() + bx, tLoc.getY() + Math.random() * 0.6 - 0.3,
          tLoc.getZ() + bz,
          1, 0, 0, 0, 0, BLOOD_DUST);
      }

      // Dripping blood
      if (bleedTick % 4 === 0) {
        world.spawnParticle(Particle.DRIPPING_LAVA, tLoc, 2, 0.3, 0.4, 0.3, 0);
      }

      // Apply bleed damage on interval
      if (bleedTick % BLEED_TICK_INTERVAL === 0) {
        try {
          target.damage(BLEED_DAMAGE);
          engine.effects.sound(tLoc, "entity.player.hurt", 0.3, 1.2);
        } catch (ex) {}
      }
    }, 1, 1);
  }
});

// ================================================================
//  ABILITY: Earthquake (Double Shift)
//  Slams the ground with expanding shockwave rings that rip up
//  the terrain visually and launch all nearby entities skyward.
// ================================================================
engine.ability({
  id: "earthquake",
  trigger: "DOUBLE_SHIFT",
  cooldown: {
    seconds: 15,
    showBossBar: true,
    bossBarColor: "YELLOW",
    bossBarLabel: "Earthquake"
  },
  interrupts: ["DEATH", "QUIT"],
  execute: function(ctx) {
    var player   = ctx.player();
    var world    = player.getWorld();
    var center   = player.getLocation().clone();
    var playerId = player.getUniqueId();

    player.sendMessage("\u00A76\u00A7lEarthquake!");

    // Heavy impact sounds
    engine.effects.sound(center, "entity.generic.explode", 1.2, 0.3);
    engine.effects.sound(center, "entity.iron_golem.attack", 1.0, 0.4);
    engine.effects.sound(center, "entity.lightning_bolt.thunder", 0.5, 0.2);

    // Central impact burst
    world.spawnParticle(Particle.EXPLOSION_EMITTER, center, 1, 0, 0, 0, 0);
    world.spawnParticle(Particle.BLOCK, center, 40, 1.5, 0.5, 1.5, 0.1,
      Material.DIRT.createBlockData());
    world.spawnParticle(Particle.BLOCK, center, 30, 1.0, 0.3, 1.0, 0.08,
      Material.COARSE_DIRT.createBlockData());

    // Track which entities have already been launched
    var launched = {};
    var wave = 0;

    var quakeId = ctx.scheduleRepeating(function() {
      wave++;

      if (wave > QUAKE_WAVES) {
        ctx.cancelTask(quakeId);
        return;
      }

      var waveRadius = (wave / QUAKE_WAVES) * QUAKE_RADIUS;

      // ─── Expanding particle ring on the ground ──────────
      var ringPoints = Math.floor(20 + waveRadius * 5);
      for (var i = 0; i < ringPoints; i++) {
        var angle = (i / ringPoints) * Math.PI * 2;
        var rx = Math.cos(angle) * waveRadius;
        var rz = Math.sin(angle) * waveRadius;

        var checkLoc = center.clone().add(rx, 0, rz);
        var groundY  = findGroundY(world, checkLoc);
        if (groundY < 0) continue;

        var ringLoc = new Location(world, checkLoc.getX(), groundY + 0.15, checkLoc.getZ());

        // Brown shockwave dust
        world.spawnParticle(Particle.DUST, ringLoc, 2, 0.15, 0.05, 0.15, 0,
          QUAKE_RING_DUST);

        // Sporadic dirt eruptions along the ring
        if (i % 4 === 0) {
          world.spawnParticle(Particle.BLOCK, ringLoc, 5, 0.2, 0.6, 0.2, 0.08,
            Material.DIRT.createBlockData());
        }

        // Rising dust columns
        if (Math.random() > 0.75) {
          world.spawnParticle(Particle.DUST,
            ringLoc.getX(), ringLoc.getY() + Math.random() * 2.0, ringLoc.getZ(),
            3, 0.1, 0.4, 0.1, 0, QUAKE_DUST);
        }
      }

      // ─── Inner area rubble (first two waves) ───────────
      if (wave <= 2) {
        var innerR = waveRadius * 0.6;
        for (var ix = -innerR; ix <= innerR; ix += 1.0) {
          for (var iz = -innerR; iz <= innerR; iz += 1.0) {
            if (Math.sqrt(ix * ix + iz * iz) > innerR) continue;
            if (Math.random() > 0.25) continue;

            var iLoc = center.clone().add(ix, 0, iz);
            var igy  = findGroundY(world, iLoc);
            if (igy < 0) continue;

            world.spawnParticle(Particle.BLOCK,
              new Location(world, iLoc.getX(), igy + 0.3, iLoc.getZ()),
              3, 0.2, 0.5, 0.2, 0.06,
              Material.COARSE_DIRT.createBlockData());
          }
        }
      }

      // ─── Launch entities caught in the shockwave ────────
      var nearby = world.getNearbyEntities(center,
        QUAKE_RADIUS, QUAKE_RADIUS, QUAKE_RADIUS);
      var iter = nearby.iterator();
      while (iter.hasNext()) {
        var e = iter.next();
        var eid = e.getUniqueId().toString();
        if (e.getUniqueId().equals(playerId)) continue;
        if (launched[eid]) continue;

        try {
          e.getHealth();

          var eLoc  = e.getLocation();
          var edx   = eLoc.getX() - center.getX();
          var edz   = eLoc.getZ() - center.getZ();
          var eDist = Math.sqrt(edx * edx + edz * edz);

          // Only launch when the wave reaches the entity
          if (eDist > waveRadius + 1.5) continue;

          launched[eid] = true;

          // Upward launch with slight outward push
          var power   = QUAKE_LAUNCH_POWER * (1.0 - (eDist / QUAKE_RADIUS) * 0.35);
          var outward = 0.45;
          var vx = (eDist > 0.2) ? (edx / eDist) * outward : 0;
          var vz = (eDist > 0.2) ? (edz / eDist) * outward : 0;

          e.setVelocity(new Vector(vx, power, vz));

          // Distance-scaled damage
          var dmg = QUAKE_DAMAGE * (1.0 - eDist / (QUAKE_RADIUS * 1.5));
          if (dmg > 0) e.damage(dmg);

          // Impact particles on launched entity
          world.spawnParticle(Particle.DUST,
            eLoc.add(0, 0.5, 0), 10, 0.3, 0.3, 0.3, 0, QUAKE_DUST);
          world.spawnParticle(Particle.BLOCK,
            eLoc, 8, 0.4, 0.3, 0.4, 0.05, Material.DIRT.createBlockData());

        } catch (ex) { /* not a living entity */ }
      }

      // ─── Wave sounds ────────────────────────────────────
      engine.effects.sound(center, "entity.iron_golem.hurt", 0.7, 0.3 + wave * 0.08);
      engine.effects.sound(center, "block.anvil.land", 0.5, 0.35 + wave * 0.05);
      if (wave <= 2) {
        engine.effects.sound(center, "entity.generic.explode", 0.4, 0.5);
      }
    }, 1, QUAKE_WAVE_DELAY);
  }
});

// ================================================================
//  Utility: find ground surface Y below a location
// ================================================================
function findGroundY(world, loc) {
  var x = loc.getBlockX();
  var z = loc.getBlockZ();
  var startY = Math.min(loc.getBlockY() + 5, world.getMaxHeight() - 1);

  for (var y = startY; y >= world.getMinHeight(); y--) {
    if (world.getBlockAt(x, y, z).getType().isSolid()) {
      return y + 1;
    }
  }
  return -1;
}

// ================================================================
//  Create the Berserker Axe Item
// ================================================================
engine.item({
  id: "berserker_axe",
  type: "NETHERITE_AXE",
  name: "&c&lBerserker Axe",
  lore: [
    "&7&m\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500",
    "&cOvercharge &7- Right Click",
    "&7  Channel fiery rage; your next",
    "&7  strike inflicts bleeding (1.5s)",
    "",
    "&6Earthquake &7- Double Shift",
    "&7  Slam the ground and launch",
    "&7  nearby enemies skyward",
    "&7&m\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500",
    "",
    "&8Legendary Weapon"
  ],
  abilities: ["overcharge_activate", "overcharge_bleed", "earthquake"],
  unbreakable: true
});

// ================================================================
//  Give axe on join
// ================================================================
engine.ability({
  id: "give_berserker_axe",
  trigger: "ON_JOIN",
  execute: function(ctx) {
    engine.items.give(ctx.player(), "berserker_axe");
    ctx.player().sendMessage("\u00A7c\u00A7lYou received the Berserker Axe!");
  }
});

engine.log("[BerserkerAxe] Script loaded successfully.");
