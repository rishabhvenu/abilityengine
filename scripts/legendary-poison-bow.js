// ============================================================
// Legendary Poison Bow
// Place in: plugins/AbilityEngine/scripts/legendary-poison-bow.js
//
// Abilities:
//   1. Poison Cloud     - Double shift while holding bow
//   2. Slime Decay Shot - Left click: launches a falling slime
//                         block that decays terrain on impact
//                         and poisons living entities.
//
// Uses the new high-level scripting APIs for clean, declarative code.
// ============================================================

var Vector = Java.type("org.bukkit.util.Vector");
var Location = Java.type("org.bukkit.Location");
var Particle = Java.type("org.bukkit.Particle");
var DustOptions = Java.type("org.bukkit.Particle$DustOptions");
var Color = Java.type("org.bukkit.Color");
var Material = Java.type("org.bukkit.Material");
var Float = Java.type("java.lang.Float");

// Slime Decay Shot constants
var SHOT_SPEED = 1.8;
var MAX_FLIGHT_TICKS = 200;
var ENTITY_HIT_RADIUS = 1.2;
var DECAY_RADIUS = 4;
var DECAY_WAVES = 5;
var WAVE_INTERVAL_TICKS = 15;
var POISON_RADIUS = 3.5;
var POISON_DURATION_TICKS = 120;
var POISON_AMPLIFIER = 1;

// Burst fire constants
var MAX_BURST_SHOTS = 3;
var BURST_WINDOW_MS = 5000;
var BURST_WINDOW_TICKS = 100;
var BURST_COOLDOWN = 0.3;
var FULL_COOLDOWN = 20;

var SLIME_DUST = new DustOptions(Color.fromRGB(80, 200, 50), Float.parseFloat("1.8"));
var DECAY_DUST = new DustOptions(Color.fromRGB(50, 140, 30), Float.parseFloat("1.0"));

// Block degradation chain — maps material name to next decayed stage
function getDecayedMaterial(name) {
  // Stone family
  if (name === "STONE") return "COBBLESTONE";
  if (name === "COBBLESTONE") return "GRAVEL";
  if (name === "GRAVEL") return "AIR";

  // Stone bricks
  if (name === "STONE_BRICKS") return "CRACKED_STONE_BRICKS";
  if (name === "CRACKED_STONE_BRICKS") return "COBBLESTONE";

  // Deepslate
  if (name === "DEEPSLATE") return "COBBLED_DEEPSLATE";
  if (name === "COBBLED_DEEPSLATE") return "GRAVEL";
  if (name === "DEEPSLATE_BRICKS") return "CRACKED_DEEPSLATE_BRICKS";
  if (name === "CRACKED_DEEPSLATE_BRICKS") return "COBBLED_DEEPSLATE";
  if (name === "DEEPSLATE_TILES") return "CRACKED_DEEPSLATE_TILES";
  if (name === "CRACKED_DEEPSLATE_TILES") return "COBBLED_DEEPSLATE";

  // Nether
  if (name === "NETHER_BRICKS") return "CRACKED_NETHER_BRICKS";
  if (name === "CRACKED_NETHER_BRICKS") return "NETHERRACK";
  if (name === "NETHERRACK") return "AIR";

  // Polished blackstone
  if (name === "POLISHED_BLACKSTONE_BRICKS") return "CRACKED_POLISHED_BLACKSTONE_BRICKS";
  if (name === "CRACKED_POLISHED_BLACKSTONE_BRICKS") return "BLACKSTONE";

  // Dirt family
  if (name === "GRASS_BLOCK" || name === "MYCELIUM" || name === "PODZOL") return "DIRT";
  if (name === "DIRT") return "COARSE_DIRT";
  if (name === "COARSE_DIRT") return "AIR";

  // Sand
  if (name === "SANDSTONE" || name === "RED_SANDSTONE") return "SAND";
  if (name === "SAND" || name === "RED_SAND") return "AIR";

  // Foliage — instant removal
  if (name.indexOf("_LEAVES") >= 0) return "AIR";
  if (name === "TALL_GRASS" || name === "SHORT_GRASS" ||
      name === "FERN" || name === "LARGE_FERN" || name === "DEAD_BUSH") return "AIR";

  return null;
}

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
//  ABILITY: Slime Decay Shot (Left Click)
//  Launches a falling slime block. On impact it poisons nearby
//  living entities and progressively decays surrounding terrain.
// ================================================================
engine.ability({
  id: "slime_decay_shot",
  trigger: "LEFT_CLICK",
  cooldown: {
    seconds: FULL_COOLDOWN,
    showBossBar: true,
    bossBarColor: "GREEN",
    bossBarLabel: "Slime Decay"
  },
  interrupts: ["DEATH", "QUIT"],
  execute: function(ctx) {
    var player = ctx.player();

    // ─── Burst management ───────────────────────────────────
    // NOTE: overrideCooldown must be deferred by 1 tick because
    // TriggerDispatcher.setCooldown runs AFTER execute() returns
    // and would overwrite any cooldown set synchronously here.
    var remaining  = ctx.state.get(player, "burst_remaining");
    var burstStart = ctx.state.get(player, "burst_start");
    var now = Date.now();
    var isBurst = remaining != null && remaining > 0 &&
                  burstStart != null && (now - burstStart) < BURST_WINDOW_MS;

    if (isBurst) {
      remaining = remaining - 1;
      ctx.state.set(player, "burst_remaining", remaining);

      if (remaining <= 0) {
        // All shots spent — let the engine's default FULL_COOLDOWN apply
        ctx.state.clear(player);
        player.sendMessage("§2§lSlime Shot! §8(final shot!)");
      } else {
        // More shots available — override to quick cooldown next tick
        ctx.scheduleDelayed(function() {
          ctx.overrideCooldown(BURST_COOLDOWN);
        }, 1);
        player.sendMessage("§2§lSlime Shot! §7(" + remaining + " remaining)");
      }
    } else {
      // First shot — open burst window, override to quick cooldown next tick
      var shotsLeft = MAX_BURST_SHOTS - 1;
      ctx.state.set(player, "burst_remaining", shotsLeft);
      ctx.state.set(player, "burst_start", now);
      ctx.scheduleDelayed(function() {
        ctx.overrideCooldown(BURST_COOLDOWN);
      }, 1);
      player.sendMessage("§2§lSlime Shot! §7(" + shotsLeft + " remaining)");

      // Expiry timer: if window closes with unused shots, start full cooldown
      ctx.scheduleDelayed(function() {
        var rem = ctx.state.get(player, "burst_remaining");
        if (rem != null && rem > 0) {
          ctx.state.clear(player);
          ctx.overrideCooldown(FULL_COOLDOWN);
          player.sendMessage("§7Burst window expired.");
        }
      }, BURST_WINDOW_TICKS);
    }

    // ─── Fire the slime block ───────────────────────────────
    var eyeLoc = player.getEyeLocation();
    var dir    = eyeLoc.getDirection().clone();
    var world  = player.getWorld();

    var spawnLoc = eyeLoc.clone().add(dir.clone().normalize().multiply(1.5));
    var slime = world.spawnFallingBlock(spawnLoc, Material.SLIME_BLOCK.createBlockData());

    var vel = dir.multiply(SHOT_SPEED);
    vel.setY(vel.getY() + 0.3);
    slime.setVelocity(vel);
    slime.setDropItem(false);
    slime.setHurtEntities(false);
    slime.setCancelDrop(true);

    engine.effects.sound(player.getLocation(), "entity.slime.jump", 1.0, 0.6);

    var tick     = 0;
    var lastLoc  = spawnLoc.clone();
    var playerId = player.getUniqueId();
    var slimeId  = slime.getUniqueId();

    var taskId = ctx.scheduleRepeating(function() {
      tick++;

      // Safety timeout
      if (tick > MAX_FLIGHT_TICKS) {
        if (slime.isValid()) slime.remove();
        ctx.cancelTask(taskId);
        return;
      }

      // Slime block landed (became invalid when it hit terrain)
      if (!slime.isValid()) {
        doSlimeImpact(ctx, lastLoc, world, playerId);
        ctx.cancelTask(taskId);
        return;
      }

      lastLoc = slime.getLocation().clone();

      // Mid-flight entity collision check
      var nearby = world.getNearbyEntities(lastLoc,
          ENTITY_HIT_RADIUS, ENTITY_HIT_RADIUS, ENTITY_HIT_RADIUS);
      var iter = nearby.iterator();
      while (iter.hasNext()) {
        var e = iter.next();
        var eid = e.getUniqueId();
        if (eid.equals(playerId) || eid.equals(slimeId)) continue;
        try { e.getHealth(); } catch (ex) { continue; }
        // Hit a living entity — trigger impact at its position
        slime.remove();
        doSlimeImpact(ctx, lastLoc, world, playerId);
        ctx.cancelTask(taskId);
        return;
      }

      // Green particle trail
      world.spawnParticle(Particle.DUST, lastLoc, 4, 0.15, 0.15, 0.15, 0, SLIME_DUST);
      if (tick % 3 === 0) {
        engine.effects.sound(lastLoc, "block.slime_block.step", 0.3, 1.5);
      }
    }, 1, 1);
  }
});

// ================================================================
//  Slime Impact — poison entities + start terrain decay
// ================================================================
function doSlimeImpact(ctx, loc, world, shooterId) {
  // Splat visuals
  world.spawnParticle(Particle.DUST, loc, 50, 2.0, 1.0, 2.0, 0, SLIME_DUST);
  world.spawnParticle(Particle.DUST, loc, 30, 1.2, 0.6, 1.2, 0, DECAY_DUST);
  engine.effects.sound(loc, "entity.slime.squish", 1.5, 0.5);
  engine.effects.sound(loc, "block.slime_block.break", 1.2, 0.6);

  // Poison nearby living entities (caster excluded)
  var nearby = world.getNearbyEntities(loc, POISON_RADIUS, POISON_RADIUS, POISON_RADIUS);
  var iter = nearby.iterator();
  while (iter.hasNext()) {
    var e = iter.next();
    if (e.getUniqueId().equals(shooterId)) continue;
    try {
      e.getHealth();
      engine.effects.potion(e, "POISON", POISON_DURATION_TICKS, POISON_AMPLIFIER);
      engine.effects.sound(e.getLocation(), "entity.witch.ambient", 0.6, 1.2);
    } catch (ex) { /* not a living entity */ }
  }

  // Begin progressive terrain decay
  startTerrainDecay(ctx, loc, world);
}

// ================================================================
//  Progressive terrain decay — expanding waves that crack blocks
//  and send block-damage overlays so every player sees the cracks.
//
//  Uses engine.effects.blockDamage(location, progress, sourceId, viewRadius)
//  which handles sendBlockDamage on the Java side with correct types.
// ================================================================
var CRACK_VIEW_RADIUS = 48;

function startTerrainDecay(ctx, center, world) {
  var wave = 0;

  var decayId = ctx.scheduleRepeating(function() {
    wave++;
    var waveRadius = Math.min(wave, DECAY_RADIUS);

    // Crack progress escalates each wave (0.3 → 0.9)
    var crackProgress = 0.2 + (wave / DECAY_WAVES) * 0.7;

    for (var dx = -waveRadius; dx <= waveRadius; dx++) {
      for (var dy = -waveRadius; dy <= waveRadius; dy++) {
        for (var dz = -waveRadius; dz <= waveRadius; dz++) {
          var dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
          if (dist > waveRadius) continue;

          // Organic edge: randomly skip some outer blocks
          if (dist > waveRadius - 1 && Math.random() > 0.55) continue;

          var block = center.getBlock().getRelative(dx, dy, dz);
          var matName = block.getType().name();
          if (matName === "AIR" || matName === "CAVE_AIR" || matName === "VOID_AIR") continue;

          // Skip indestructible blocks
          if (matName === "BEDROCK" || matName === "BARRIER" ||
              matName === "OBSIDIAN" || matName === "CRYING_OBSIDIAN" ||
              matName === "END_PORTAL_FRAME" || matName === "COMMAND_BLOCK") continue;

          var nextMat = getDecayedMaterial(matName);
          if (nextMat === null) {
            // Generic solid block — break to air
            if (block.getType().isSolid()) nextMat = "AIR";
            else continue;
          }

          block.setType(Material.valueOf(nextMat));

          // Send block-damage crack overlay on surviving blocks (all nearby players)
          if (nextMat !== "AIR") {
            try {
              var sid = (dx + 10) + (dy + 10) * 20 + (dz + 10) * 400;
              engine.effects.blockDamage(block.getLocation(), crackProgress, sid, CRACK_VIEW_RADIUS);
            } catch (dmgEx) {}
          }

          // Crack particles at the decayed block
          var pLoc = block.getLocation().add(0.5, 0.5, 0.5);
          world.spawnParticle(Particle.DUST, pLoc, 6, 0.3, 0.3, 0.3, 0, DECAY_DUST);
        }
      }
    }

    // Pre-crack the outer ring to foreshadow the next wave
    if (wave < DECAY_WAVES) {
      var nextR = waveRadius + 1;
      if (nextR <= DECAY_RADIUS) {
        for (var fx = -nextR; fx <= nextR; fx++) {
          for (var fy = -nextR; fy <= nextR; fy++) {
            for (var fz = -nextR; fz <= nextR; fz++) {
              var fd = Math.sqrt(fx * fx + fy * fy + fz * fz);
              if (fd > nextR || fd <= waveRadius) continue;
              var fb = center.getBlock().getRelative(fx, fy, fz);
              if (!fb.getType().isSolid()) continue;
              try {
                var fsid = (fx + 10) + (fy + 10) * 20 + (fz + 10) * 400;
                engine.effects.blockDamage(fb.getLocation(), 0.15, fsid, CRACK_VIEW_RADIUS);
              } catch (dmgEx2) {}
            }
          }
        }
      }
    }

    // Wave sound + ambient particles
    engine.effects.sound(center, "block.gravel.break", 0.8, 0.4 + wave * 0.15);
    engine.effects.sound(center, "block.stone.break", 0.6, 0.6);
    world.spawnParticle(Particle.DUST, center, 20,
        waveRadius * 0.6, 0.5, waveRadius * 0.6, 0, DECAY_DUST);

    if (wave >= DECAY_WAVES) {
      ctx.cancelTask(decayId);
    }
  }, 10, WAVE_INTERVAL_TICKS);
}

// ================================================================
//  Create the Legendary Poison Bow Item
// ================================================================
engine.item({
  id: "legendary_poison_bow",
  type: "BOW",
  name: "&2&lLegendary Poison Bow",
  lore: [
    "&7&m─────────────────────",
    "&aPoison Cloud &7- Double Shift",
    "&7  Blankets the area in toxic fumes",
    "",
    "&2Slime Decay Shot &7- Left Click",
    "&7  Launches a slime block that",
    "&7  poisons entities and decays terrain",
    "&7  3 shots in 5s, then 20s cooldown",
    "&7&m─────────────────────",
    "",
    "&8Legendary Weapon"
  ],
  abilities: ["poison_cloud", "slime_decay_shot"],
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
