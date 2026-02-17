// ============================================================
// Meteor Staff
// Place in: plugins/AbilityEngine/scripts/meteor-staff.js
//
// Abilities:
//   1. Meteor Strike - Right click: shoots a flaming, rotating
//                      magma block that creates a fiery crater
//                      on impact. Red particles mark the landing.
//   2. Blizzard      - Hold shift: channeled blizzard that
//                      slows and progressively freezes nearby
//                      enemies. Ends on shift release.
//
// Item: Diamond axe with item/staff model.
// ============================================================

var Vector = Java.type("org.bukkit.util.Vector");
var Location = Java.type("org.bukkit.Location");
var Particle = Java.type("org.bukkit.Particle");
var DustOptions = Java.type("org.bukkit.Particle$DustOptions");
var Color = Java.type("org.bukkit.Color");
var Material = Java.type("org.bukkit.Material");
var Float = Java.type("java.lang.Float");
var NamespacedKey = Java.type("org.bukkit.NamespacedKey");

// ─── Meteor Strike constants ────────────────────────────────
var METEOR_SPEED = 1.6;
var METEOR_MAX_TICKS = 160;
var METEOR_ENTITY_RADIUS = 1.3;
var CRATER_RADIUS = 4;
var FIRE_RADIUS = 5;
var IMPACT_DAMAGE = 12;
var IMPACT_KNOCKBACK = 1.5;
var IMPACT_FIRE_TICKS = 100;

var RED_DUST = new DustOptions(Color.fromRGB(255, 40, 10), Float.parseFloat("2.0"));
var ORANGE_DUST = new DustOptions(Color.fromRGB(255, 140, 0), Float.parseFloat("1.5"));
var EMBER_DUST = new DustOptions(Color.fromRGB(255, 80, 0), Float.parseFloat("1.0"));
var LANDING_DUST = new DustOptions(Color.fromRGB(255, 20, 0), Float.parseFloat("1.4"));

// ─── Blizzard constants ─────────────────────────────────────
var BLIZZARD_RADIUS = 6;
var BLIZZARD_EFFECT_RADIUS = 5;
var BLIZZARD_MAX_TICKS = 200;
var BLIZZARD_SLOW_DURATION = 40;
var BLIZZARD_SLOW_AMPLIFIER = 2;
var BLIZZARD_FREEZE_PER_APPLY = 40;
var BLIZZARD_FREEZE_THRESHOLD = 60;
var BLIZZARD_EFFECT_INTERVAL = 5;

var ICE_DUST = new DustOptions(Color.fromRGB(140, 200, 255), Float.parseFloat("1.5"));
var SNOW_DUST = new DustOptions(Color.fromRGB(220, 235, 255), Float.parseFloat("1.0"));
var FROST_DUST = new DustOptions(Color.fromRGB(180, 220, 255), Float.parseFloat("1.8"));

// ================================================================
//  ABILITY: Meteor Strike (Right Click)
//  Shoots a flaming, rotating magma block. A red particle ring on
//  the ground tracks where it will land. On impact: explosion,
//  fire, and terrain destruction.
// ================================================================
engine.ability({
  id: "meteor_strike",
  trigger: "RIGHT_CLICK",
  cooldown: {
    seconds: 12,
    showBossBar: true,
    bossBarColor: "RED",
    bossBarLabel: "Meteor Strike"
  },
  interrupts: ["DEATH", "QUIT"],
  execute: function(ctx) {
    var player = ctx.player();
    var eyeLoc = player.getEyeLocation();
    var dir = eyeLoc.getDirection().clone();
    var world = player.getWorld();

    // Spawn magma block ahead of the player
    var spawnLoc = eyeLoc.clone().add(dir.clone().normalize().multiply(1.5));
    var magma = world.spawnFallingBlock(spawnLoc, Material.MAGMA_BLOCK.createBlockData());

    var vel = dir.multiply(METEOR_SPEED);
    vel.setY(vel.getY() + 0.35);
    magma.setVelocity(vel);
    magma.setDropItem(false);
    magma.setHurtEntities(false);
    magma.setCancelDrop(true);

    try { magma.setVisualFire(true); } catch (ex) {}

    engine.effects.sound(player.getLocation(), "entity.blaze.shoot", 1.0, 0.6);
    engine.effects.sound(player.getLocation(), "item.firecharge.use", 1.0, 0.5);
    player.sendMessage("\u00A7c\u00A7lMeteor Strike!");

    var tick = 0;
    var lastLoc = spawnLoc.clone();
    var playerId = player.getUniqueId();
    var magmaId = magma.getUniqueId();

    var taskId = ctx.scheduleRepeating(function() {
      tick++;

      if (tick > METEOR_MAX_TICKS) {
        if (magma.isValid()) magma.remove();
        ctx.cancelTask(taskId);
        return;
      }

      // Magma block landed or was removed
      if (!magma.isValid()) {
        doMeteorImpact(ctx, lastLoc, world, playerId);
        ctx.cancelTask(taskId);
        return;
      }

      lastLoc = magma.getLocation().clone();

      // Mid-flight entity collision
      var nearby = world.getNearbyEntities(lastLoc,
          METEOR_ENTITY_RADIUS, METEOR_ENTITY_RADIUS, METEOR_ENTITY_RADIUS);
      var iter = nearby.iterator();
      while (iter.hasNext()) {
        var e = iter.next();
        var eid = e.getUniqueId();
        if (eid.equals(playerId) || eid.equals(magmaId)) continue;
        try { e.getHealth(); } catch (ex) { continue; }
        magma.remove();
        doMeteorImpact(ctx, lastLoc, world, playerId);
        ctx.cancelTask(taskId);
        return;
      }

      // ─── Flame trail ──────────────────────────────────
      world.spawnParticle(Particle.FLAME, lastLoc, 10, 0.2, 0.2, 0.2, 0.03);
      world.spawnParticle(Particle.LAVA, lastLoc, 2, 0.1, 0.1, 0.1, 0);
      world.spawnParticle(Particle.LARGE_SMOKE, lastLoc, 4, 0.15, 0.15, 0.15, 0.01);
      world.spawnParticle(Particle.DUST, lastLoc, 3, 0.3, 0.3, 0.3, 0, EMBER_DUST);

      // ─── Rotating particle rings (simulates tumbling) ─
      var angle1 = (tick * 0.5) % (Math.PI * 2);
      var angle2 = (tick * 0.35 + Math.PI / 3) % (Math.PI * 2);
      var ringR = 0.7;

      // Horizontal ring
      for (var i = 0; i < 6; i++) {
        var a = angle1 + (i * Math.PI / 3);
        var rx = Math.cos(a) * ringR;
        var rz = Math.sin(a) * ringR;
        world.spawnParticle(Particle.DUST,
          lastLoc.getX() + rx, lastLoc.getY(), lastLoc.getZ() + rz,
          1, 0, 0, 0, 0, ORANGE_DUST);
      }

      // Tilted ring for 3D rotation illusion
      for (var j = 0; j < 6; j++) {
        var b = angle2 + (j * Math.PI / 3);
        var tx = Math.cos(b) * ringR;
        var ty = Math.sin(b) * ringR * 0.7;
        world.spawnParticle(Particle.DUST,
          lastLoc.getX() + tx, lastLoc.getY() + ty, lastLoc.getZ(),
          1, 0, 0, 0, 0, RED_DUST);
      }

      // ─── Landing indicator (red particle circle on ground) ─
      var groundY = findGroundY(world, lastLoc);
      if (groundY >= 0) {
        var distToGround = lastLoc.getY() - groundY;
        var intensity = Math.max(0, 1.0 - distToGround / 30.0);
        var circlePoints = Math.floor(12 + intensity * 12);
        var circleRadius = 2.0 + intensity;
        var pulse = 1.0 + Math.sin(tick * 0.3) * 0.15;

        var indicatorLoc = new Location(world,
            lastLoc.getX(), groundY + 0.1, lastLoc.getZ());

        for (var p = 0; p < circlePoints; p++) {
          var ca = (p / circlePoints) * Math.PI * 2;
          var cx = Math.cos(ca) * circleRadius * pulse;
          var cz = Math.sin(ca) * circleRadius * pulse;
          world.spawnParticle(Particle.DUST,
            indicatorLoc.getX() + cx, indicatorLoc.getY(), indicatorLoc.getZ() + cz,
            1, 0, 0, 0, 0, LANDING_DUST);
        }

        // Inner glow intensifies as meteor approaches
        if (intensity > 0.3) {
          world.spawnParticle(Particle.DUST, indicatorLoc,
            Math.floor(3 + intensity * 8), 0.8, 0.05, 0.8, 0, RED_DUST);
        }
      }

      if (tick % 5 === 0) {
        engine.effects.sound(lastLoc, "block.fire.ambient", 0.6,
            0.4 + Math.random() * 0.3);
      }
    }, 1, 1);
  }
});

// Finds the Y of the ground surface below a location
function findGroundY(world, loc) {
  var x = loc.getBlockX();
  var z = loc.getBlockZ();
  var startY = Math.min(loc.getBlockY(), world.getMaxHeight() - 1);

  for (var y = startY; y >= world.getMinHeight(); y--) {
    if (world.getBlockAt(x, y, z).getType().isSolid()) {
      return y + 1;
    }
  }
  return -1;
}

// ================================================================
//  Meteor Impact — explosion + fire + crater
// ================================================================
function doMeteorImpact(ctx, loc, world, shooterId) {
  // Massive visual explosion
  world.spawnParticle(Particle.EXPLOSION_EMITTER, loc, 3, 0.5, 0.5, 0.5, 0);
  world.spawnParticle(Particle.FLAME, loc, 100, 3.5, 2.0, 3.5, 0.12);
  world.spawnParticle(Particle.LAVA, loc, 50, 2.5, 1.5, 2.5, 0);
  world.spawnParticle(Particle.LARGE_SMOKE, loc, 40, 3.0, 2.0, 3.0, 0.06);
  world.spawnParticle(Particle.DUST, loc, 50, 3.5, 2.0, 3.5, 0, RED_DUST);
  world.spawnParticle(Particle.DUST, loc, 30, 2.5, 1.5, 2.5, 0, ORANGE_DUST);

  engine.effects.sound(loc, "entity.generic.explode", 1.5, 0.4);
  engine.effects.sound(loc, "entity.lightning_bolt.thunder", 0.8, 0.3);
  engine.effects.sound(loc, "item.firecharge.use", 1.2, 0.3);
  engine.effects.sound(loc, "entity.ender_dragon.growl", 0.5, 0.4);

  // Damage and knockback nearby living entities
  var damageRadius = CRATER_RADIUS + 2;
  var nearby = world.getNearbyEntities(loc, damageRadius, damageRadius, damageRadius);
  var iter = nearby.iterator();
  while (iter.hasNext()) {
    var e = iter.next();
    if (e.getUniqueId().equals(shooterId)) continue;
    try {
      e.getHealth();
      var dist = e.getLocation().distance(loc);
      var damage = Math.max(2, IMPACT_DAMAGE - (dist * 1.5));
      e.damage(damage);

      var kb = e.getLocation().toVector().subtract(loc.toVector())
               .normalize().multiply(IMPACT_KNOCKBACK);
      kb.setY(0.6);
      e.setVelocity(kb);
      e.setFireTicks(IMPACT_FIRE_TICKS);
    } catch (ex) { /* not a living entity */ }
  }

  createCrater(loc, world);
}

// ================================================================
//  Crater — spherical block removal + surface fire
// ================================================================
function createCrater(center, world) {
  var r = CRATER_RADIUS;

  var viewers = [];
  var pList = world.getPlayers();
  var pIter = pList.iterator();
  while (pIter.hasNext()) {
    var p = pIter.next();
    if (p.getLocation().distanceSquared(center) < 4096) viewers.push(p);
  }

  for (var dx = -r; dx <= r; dx++) {
    for (var dy = -r; dy <= r; dy++) {
      for (var dz = -r; dz <= r; dz++) {
        var dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (dist > r) continue;
        if (dist > r - 1 && Math.random() > 0.5) continue;

        var block = center.getBlock().getRelative(dx, dy, dz);
        var matName = block.getType().name();

        if (matName === "AIR" || matName === "CAVE_AIR" ||
            matName === "VOID_AIR") continue;
        if (matName === "BEDROCK" || matName === "BARRIER" ||
            matName === "OBSIDIAN" || matName === "CRYING_OBSIDIAN" ||
            matName === "END_PORTAL_FRAME" || matName === "COMMAND_BLOCK") continue;

        if (dist <= r * 0.6) {
          block.setType(Material.AIR);
        } else {
          var decayed = getImpactMaterial(matName);
          if (decayed !== null) {
            block.setType(Material.valueOf(decayed));
          } else if (block.getType().isSolid()) {
            block.setType(Material.AIR);
          }
        }

        // Crack overlay on surviving blocks
        if (block.getType().name() !== "AIR" && viewers.length > 0) {
          var bLoc = block.getLocation();
          var sid = (dx + 10) + (dy + 10) * 20 + (dz + 10) * 400;
          var crack = 0.4 + (dist / r) * 0.5;
          for (var vi = 0; vi < viewers.length; vi++) {
            try { viewers[vi].sendBlockDamage(bLoc, crack, sid); }
            catch (ex) {}
          }
        }
      }
    }
  }

  // Set fire on surfaces around the crater
  var fr = FIRE_RADIUS;
  for (var fx = -fr; fx <= fr; fx++) {
    for (var fz = -fr; fz <= fr; fz++) {
      var fd = Math.sqrt(fx * fx + fz * fz);
      if (fd > fr) continue;
      if (Math.random() > 0.45) continue;

      for (var fy = 4; fy >= -4; fy--) {
        var surface = center.getBlock().getRelative(fx, fy, fz);
        var above = center.getBlock().getRelative(fx, fy + 1, fz);
        var aboveName = above.getType().name();
        if (surface.getType().isSolid() &&
            (aboveName === "AIR" || aboveName === "CAVE_AIR")) {
          above.setType(Material.FIRE);
          break;
        }
      }
    }
  }
}

// Block degradation for meteor impact
function getImpactMaterial(name) {
  if (name === "STONE") return "COBBLESTONE";
  if (name === "COBBLESTONE") return "GRAVEL";
  if (name === "STONE_BRICKS") return "CRACKED_STONE_BRICKS";
  if (name === "CRACKED_STONE_BRICKS") return "COBBLESTONE";
  if (name === "DEEPSLATE") return "COBBLED_DEEPSLATE";
  if (name === "COBBLED_DEEPSLATE") return "GRAVEL";
  if (name === "DEEPSLATE_BRICKS") return "CRACKED_DEEPSLATE_BRICKS";
  if (name === "CRACKED_DEEPSLATE_BRICKS") return "COBBLED_DEEPSLATE";
  if (name === "DEEPSLATE_TILES") return "CRACKED_DEEPSLATE_TILES";
  if (name === "CRACKED_DEEPSLATE_TILES") return "COBBLED_DEEPSLATE";
  if (name === "NETHER_BRICKS") return "CRACKED_NETHER_BRICKS";
  if (name === "CRACKED_NETHER_BRICKS") return "NETHERRACK";
  if (name === "POLISHED_BLACKSTONE_BRICKS") return "CRACKED_POLISHED_BLACKSTONE_BRICKS";
  if (name === "GRASS_BLOCK" || name === "MYCELIUM" || name === "PODZOL") return "COARSE_DIRT";
  if (name === "DIRT") return "COARSE_DIRT";
  if (name === "COARSE_DIRT") return "AIR";
  if (name === "SAND" || name === "RED_SAND") return "AIR";
  if (name === "GRAVEL") return "AIR";
  if (name === "SNOW" || name === "SNOW_BLOCK") return "AIR";
  if (name.indexOf("_LEAVES") >= 0) return "AIR";
  if (name === "TALL_GRASS" || name === "SHORT_GRASS" ||
      name === "FERN" || name === "LARGE_FERN" || name === "DEAD_BUSH") return "AIR";
  return null;
}

// ================================================================
//  ABILITY: Blizzard (Hold Shift)
//  Channeled area blizzard. Slows enemies immediately; after 3s
//  of sustained channeling, progressively freezes them. Ends
//  when the player releases shift or max duration is reached.
// ================================================================
engine.ability({
  id: "blizzard",
  trigger: "HOLD_SHIFT",
  cooldown: {
    seconds: 20,
    showBossBar: true,
    bossBarColor: "BLUE",
    bossBarLabel: "Blizzard"
  },
  interrupts: ["DEATH", "QUIT"],
  execute: function(ctx) {
    var player = ctx.player();

    // Prevent re-activation while already channeling
    var active = ctx.state.get(player, "active");
    if (active) return;
    ctx.state.set(player, "active", true);

    player.sendMessage("\u00A7b\u00A7lBlizzard!");
    engine.effects.sound(player.getLocation(), "entity.elder_guardian.curse", 0.6, 1.5);
    engine.effects.sound(player.getLocation(), "block.powder_snow.step", 1.0, 0.5);

    var channelTick = 0;
    var playerId = player.getUniqueId();

    var taskId = ctx.scheduleRepeating(function() {
      channelTick++;

      // End conditions
      if (!player.isOnline() || !player.isSneaking() ||
          channelTick > BLIZZARD_MAX_TICKS) {
        ctx.state.set(player, "active", false);
        ctx.cancelTask(taskId);

        if (player.isOnline()) {
          player.sendMessage("\u00A77Blizzard faded.");
          engine.effects.sound(player.getLocation(), "block.glass.break", 0.4, 1.8);
        }
        return;
      }

      var pLoc = player.getLocation().add(0, 1, 0);
      var world = player.getWorld();

      // ─── Swirling snowflakes ──────────────────────────
      var baseAngle = (channelTick * 0.12) % (Math.PI * 2);
      for (var i = 0; i < 14; i++) {
        var a = baseAngle + (i / 14.0) * Math.PI * 2;
        var sr = BLIZZARD_RADIUS * (0.2 + Math.random() * 0.8);
        var px = Math.cos(a) * sr;
        var pz = Math.sin(a) * sr;
        var py = Math.random() * 3.5;

        world.spawnParticle(Particle.SNOWFLAKE,
          pLoc.getX() + px, pLoc.getY() + py, pLoc.getZ() + pz,
          1, 0.15, 0.1, 0.15, 0.02);
      }

      // Dense ice dust cloud
      world.spawnParticle(Particle.DUST, pLoc, 10,
        BLIZZARD_RADIUS * 0.5, 1.5, BLIZZARD_RADIUS * 0.5, 0, ICE_DUST);
      world.spawnParticle(Particle.DUST, pLoc, 8,
        BLIZZARD_RADIUS * 0.4, 1.2, BLIZZARD_RADIUS * 0.4, 0, SNOW_DUST);

      // Cloud for whiteout effect
      if (channelTick % 2 === 0) {
        world.spawnParticle(Particle.CLOUD, pLoc, 3,
          BLIZZARD_RADIUS * 0.35, 0.6, BLIZZARD_RADIUS * 0.35, 0.01);
      }

      // Frost ring on the ground
      if (channelTick % 4 === 0) {
        var ringAngle = (channelTick * 0.08) % (Math.PI * 2);
        for (var ri = 0; ri < 10; ri++) {
          var ra = ringAngle + (ri / 10.0) * Math.PI * 2;
          var rrx = Math.cos(ra) * BLIZZARD_RADIUS;
          var rrz = Math.sin(ra) * BLIZZARD_RADIUS;
          world.spawnParticle(Particle.DUST,
            pLoc.getX() + rrx, pLoc.getY() - 1, pLoc.getZ() + rrz,
            2, 0.2, 0.05, 0.2, 0, FROST_DUST);
        }
      }

      // ─── Apply effects to nearby enemies ──────────────
      if (channelTick % BLIZZARD_EFFECT_INTERVAL === 0) {
        var nearby = world.getNearbyEntities(
          player.getLocation(),
          BLIZZARD_EFFECT_RADIUS, BLIZZARD_EFFECT_RADIUS, BLIZZARD_EFFECT_RADIUS);
        var nIter = nearby.iterator();
        while (nIter.hasNext()) {
          var e = nIter.next();
          if (e.getUniqueId().equals(playerId)) continue;
          try {
            e.getHealth();

            // Slowness always applied
            engine.effects.potion(e, "SLOWNESS",
                BLIZZARD_SLOW_DURATION, BLIZZARD_SLOW_AMPLIFIER);

            // Progressive freeze after threshold
            if (channelTick >= BLIZZARD_FREEZE_THRESHOLD) {
              var newFreeze = Math.min(
                e.getFreezeTicks() + BLIZZARD_FREEZE_PER_APPLY,
                e.getMaxFreezeTicks() + 40
              );
              e.setFreezeTicks(newFreeze);
              engine.effects.potion(e, "MINING_FATIGUE", 60, 1);
            }

            // Frost particles on affected entity
            world.spawnParticle(Particle.SNOWFLAKE,
              e.getLocation().add(0, 1, 0), 6, 0.3, 0.5, 0.3, 0.01);
            world.spawnParticle(Particle.DUST,
              e.getLocation().add(0, 1, 0), 3, 0.2, 0.3, 0.2, 0, ICE_DUST);
          } catch (ex) { /* not a living entity */ }
        }
      }

      // Ambient sounds
      if (channelTick % 8 === 0) {
        engine.effects.sound(pLoc, "entity.player.hurt_freeze", 0.3,
            0.5 + Math.random() * 0.3);
      }
      if (channelTick % 15 === 0) {
        engine.effects.sound(pLoc, "block.powder_snow.step", 0.6, 0.8);
      }
      if (channelTick % 20 === 0) {
        engine.effects.sound(pLoc, "entity.snow_golem.ambient", 0.5, 0.6);
      }
    }, 1, 1);
  }
});

// ================================================================
//  Create the Meteor Staff Item
// ================================================================
engine.item({
  id: "meteor_staff",
  type: "DIAMOND_AXE",
  name: "&c&lMeteor Staff",
  lore: [
    "&7&m\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500",
    "&cMeteor Strike &7- Right Click",
    "&7  Launches a flaming meteorite",
    "&7  that devastates on impact",
    "",
    "&bBlizzard &7- Hold Shift",
    "&7  Channel a freezing blizzard",
    "&7  that slows and freezes enemies",
    "&7&m\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500",
    "",
    "&8Legendary Staff"
  ],
  abilities: ["meteor_strike", "blizzard"],
  unbreakable: true
});

// ================================================================
//  Give staff on join + apply item model
// ================================================================
engine.ability({
  id: "give_meteor_staff",
  trigger: "ON_JOIN",
  execute: function(ctx) {
    engine.items.give(ctx.player(), "meteor_staff");
    applyStaffModel(ctx.player());
    ctx.player().sendMessage("\u00A7c\u00A7lYou received the Meteor Staff!");
  }
});

// Sets the item/staff model on the meteor staff in the player's inventory
function applyStaffModel(player) {
  var inv = player.getInventory();
  for (var i = inv.getSize() - 1; i >= 0; i--) {
    var stack = inv.getItem(i);
    if (stack == null) continue;
    if (stack.getType().name() !== "DIAMOND_AXE") continue;
    if (engine.items.getAbilityId(stack) !== "meteor_strike") continue;

    var meta = stack.getItemMeta();
    try {
      meta.setItemModel(NamespacedKey.minecraft("staff"));
      stack.setItemMeta(meta);
      inv.setItem(i, stack);
    } catch (ex) {
      // setItemModel requires Paper 1.21.2+; silently skip on older versions
    }
    break;
  }
}

engine.log("[MeteorStaff] Script loaded successfully.");
