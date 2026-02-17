// ============================================================
// Grappling Hook
// Place in: plugins/AbilityEngine/scripts/grappling-hook.js
//
// Abilities:
//   1. Grapple Shot  - Right click: fires a particle hook toward
//                      a surface. Hit = latch + zip. Miss = retract
//                      with a shorter cooldown.
//   2. Ground Slam   - Shift+Right click while airborne: AoE slam
//
// Item: Legendary fishing rod with both abilities wired up.
// ============================================================

var Vector = Java.type("org.bukkit.util.Vector");
var Location = Java.type("org.bukkit.Location");
var Particle = Java.type("org.bukkit.Particle");
var DustOptions = Java.type("org.bukkit.Particle$DustOptions");
var Color = Java.type("org.bukkit.Color");

var MAX_RANGE = 50.0;
var BEAM_TICKS = 14;
var RETRACT_TICKS = 8;
var GRAPPLE_SPEED = 1.2;
var ARRIVAL_DIST = 1.5;
var MAX_ZIP_TICKS = 80;
var ROPE_SPACING = 0.2;
var MISS_COOLDOWN = 1.5;
var DRAG = 0.7;
var MIN_SPEED = 0.15;

var Float = Java.type("java.lang.Float");
var LINE_DUST = new DustOptions(Color.fromRGB(220, 220, 220), Float.parseFloat("1.2"));
var TIP_DUST = new DustOptions(Color.fromRGB(255, 255, 255), Float.parseFloat("1.6"));

// Spawns a single dust particle exactly at a location (no spread, no drift)
function dot(world, x, y, z, dust) {
  world.spawnParticle(Particle.DUST, x, y, z, 1, 0, 0, 0, 0, dust);
}

// Draws a thin dust line from origin toward anchor, up to progress (0..1)
function drawBeam(origin, anchor, progress) {
  var dx = anchor.getX() - origin.getX();
  var dy = anchor.getY() - origin.getY();
  var dz = anchor.getZ() - origin.getZ();
  var totalDist = Math.sqrt(dx * dx + dy * dy + dz * dz);
  var reachDist = totalDist * progress;
  var steps = Math.max(Math.floor(reachDist / ROPE_SPACING), 1);
  var world = origin.getWorld();

  for (var i = 0; i <= steps; i++) {
    var t = (i / steps) * progress;
    dot(world,
      origin.getX() + dx * t,
      origin.getY() + dy * t,
      origin.getZ() + dz * t,
      LINE_DUST
    );
  }

  // Brighter tip
  dot(world,
    origin.getX() + dx * progress,
    origin.getY() + dy * progress,
    origin.getZ() + dz * progress,
    TIP_DUST
  );
}

// Draws a thin dust rope between two locations
function drawRope(from, to) {
  var dx = to.getX() - from.getX();
  var dy = to.getY() - from.getY();
  var dz = to.getZ() - from.getZ();
  var dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
  var steps = Math.max(Math.floor(dist / ROPE_SPACING), 1);
  var world = from.getWorld();

  for (var i = 0; i <= steps; i++) {
    var t = i / steps;
    dot(world,
      from.getX() + dx * t,
      from.getY() + dy * t,
      from.getZ() + dz * t,
      LINE_DUST
    );
  }
}

// ================================================================
//  ABILITY: Grapple Shot (Right Click)
//  Fires a visible particle hook. Hits blocks, entities, or misses.
//  Entity hit: slowness + visual explosion + zip to live position.
//  Block hit: latch + zip. Miss: retract + shorter cooldown.
// ================================================================
engine.ability({
  id: "grapple_shot",
  trigger: "RIGHT_CLICK",
  cooldown: {
    seconds: 4,
    showBossBar: true,
    bossBarColor: "BLUE",
    bossBarLabel: "Grapple"
  },
  execute: function(ctx) {
    var player = ctx.player();
    var eyeLoc = player.getEyeLocation();
    var dir = eyeLoc.getDirection().clone();
    var world = player.getWorld();
    var ENTITY_HIT_RADIUS = 1.5;

    // Only raytrace blocks upfront to find the max beam endpoint
    var blockResult = world.rayTraceBlocks(eyeLoc, dir, MAX_RANGE);
    var hitBlock = blockResult != null && blockResult.getHitBlock() != null;

    var endPoint;
    if (hitBlock) {
      var hitVec = blockResult.getHitPosition();
      endPoint = new Location(world, hitVec.getX(), hitVec.getY(), hitVec.getZ());
    } else {
      endPoint = new Location(world,
        eyeLoc.getX() + dir.getX() * MAX_RANGE,
        eyeLoc.getY() + dir.getY() * MAX_RANGE,
        eyeLoc.getZ() + dir.getZ() * MAX_RANGE
      );
    }

    // Beam origin is locked at fire time
    var originLoc = eyeLoc.clone();

    engine.effects.sound(player.getLocation(), "entity.fishing_bobber.throw", 1.0, 1.4);

    var phase = "extend";
    var tick = 0;
    var currentSpeed = GRAPPLE_SPEED;
    var hitType = hitBlock ? "block" : "miss";
    var anchor = endPoint;
    var targetEntity = null;

    var taskId = ctx.scheduleRepeating(function() {
      tick++;

      if (phase === "extend") {
        var progress = Math.min(tick / BEAM_TICKS, 1.0);

        // Calculate beam tip position
        var tipX = originLoc.getX() + (endPoint.getX() - originLoc.getX()) * progress;
        var tipY = originLoc.getY() + (endPoint.getY() - originLoc.getY()) * progress;
        var tipZ = originLoc.getZ() + (endPoint.getZ() - originLoc.getZ()) * progress;
        var tipLoc = new Location(world, tipX, tipY, tipZ);

        // Check for entities near the beam tip (live detection)
        if (hitType !== "entity") {
          var nearby = world.getNearbyEntities(tipLoc, ENTITY_HIT_RADIUS, ENTITY_HIT_RADIUS, ENTITY_HIT_RADIUS);
          var iter = nearby.iterator();
          while (iter.hasNext()) {
            var e = iter.next();
            if (e.getUniqueId().equals(player.getUniqueId())) continue;
            try {
              if (e.isDead()) continue;
            } catch(ex) { continue; }
            // Found a live entity at the tip - lock anchor to where the beam is
            targetEntity = e;
            hitType = "entity";
            anchor = tipLoc.clone();
            break;
          }
        }

        // If we just latched an entity mid-flight, trigger immediately
        if (hitType === "entity" && targetEntity != null) {
          drawBeam(originLoc, anchor, 1.0);

          // Latch sound + freeze the entity until player arrives
          engine.effects.sound(anchor, "block.chain.place", 1.0, 0.5);

          try {
            engine.effects.potion(targetEntity, "SLOWNESS", 200, 255);
          } catch(ex) {}

          player.sendMessage("§b§lLatched!");
          phase = "zip";
          tick = 0;
          currentSpeed = GRAPPLE_SPEED;
          return;
        }

        drawBeam(originLoc, endPoint, progress);

        if (tick % 2 === 0) {
          engine.effects.sound(player.getLocation(), "block.chain.place", 0.3, 1.5 + progress * 0.5);
        }

        // Beam fully extended
        if (tick >= BEAM_TICKS) {
          if (hitType === "block") {
            anchor.getWorld().spawnParticle(Particle.DUST, anchor, 20, 0.15, 0.15, 0.15, 0, TIP_DUST);
            engine.effects.sound(anchor, "block.chain.place", 1.0, 0.5);
            engine.effects.sound(anchor, "block.iron_door.close", 0.6, 1.8);
            player.sendMessage("§b§lGrapple!");
            phase = "zip";
            tick = 0;
            currentSpeed = GRAPPLE_SPEED;
          } else {
            engine.effects.sound(player.getLocation(), "entity.fishing_bobber.retrieve", 0.6, 1.2);
            player.sendMessage("§7Miss...");
            phase = "retract";
            tick = 0;
          }
        }

      } else if (phase === "retract") {
        var progress = 1.0 - Math.min(tick / RETRACT_TICKS, 1.0);

        if (progress > 0.01) {
          drawBeam(originLoc, anchor, progress);
        }

        if (tick >= RETRACT_TICKS) {
          engine.cooldowns.set(player, "grapple_shot", MISS_COOLDOWN);
          engine.ui.removeBar(player, "grapple_shot");
          ctx.cancelTask(taskId);
          return;
        }

      } else {
        // Zip phase - pull toward anchor with collision drag

        // Entity is frozen, just check it's still alive
        if (hitType === "entity") {
          if (!targetEntity.isValid() || targetEntity.isDead()) {
            player.setVelocity(new Vector(0, 0, 0));
            player.setFallDistance(0);
            try {
              targetEntity.removePotionEffect(Java.type("org.bukkit.potion.PotionEffectType").SLOWNESS);
            } catch(ex) {}
            ctx.cancelTask(taskId);
            return;
          }
        }

        var pLoc = player.getLocation();
        var dx = anchor.getX() - pLoc.getX();
        var dy = anchor.getY() - (pLoc.getY() + 1.0);
        var dz = anchor.getZ() - pLoc.getZ();
        var remaining = Math.sqrt(dx * dx + dy * dy + dz * dz);

        // Collision drag
        var actual = player.getVelocity();
        var actualSpeed = Math.sqrt(
          actual.getX() * actual.getX() +
          actual.getY() * actual.getY() +
          actual.getZ() * actual.getZ()
        );
        if (tick > 1 && actualSpeed < currentSpeed * 0.4) {
          currentSpeed = currentSpeed * DRAG;
        }

        if (remaining < ARRIVAL_DIST || tick > MAX_ZIP_TICKS || currentSpeed < MIN_SPEED) {
          player.setVelocity(new Vector(0, 0, 0));
          player.setFallDistance(0);

          if (hitType === "entity") {
            // Visual explosion + sound, swap freeze for brief slowness
            world.spawnParticle(Particle.EXPLOSION_EMITTER, anchor, 1, 0, 0, 0, 0);
            engine.effects.sound(anchor, "entity.generic.explode", 0.7, 1.4);
            try {
              targetEntity.removePotionEffect(Java.type("org.bukkit.potion.PotionEffectType").SLOWNESS);
              engine.effects.potion(targetEntity, "SLOWNESS", 20, 2);
            } catch(ex) {}
          } else {
            pLoc.getWorld().spawnParticle(Particle.DUST, pLoc, 15, 0.3, 0.3, 0.3, 0, TIP_DUST);
          }

          engine.effects.sound(pLoc, "entity.fishing_bobber.retrieve", 1.0, 1.6);
          ctx.cancelTask(taskId);
          return;
        }

        var vx = (dx / remaining) * currentSpeed;
        var vy = (dy / remaining) * currentSpeed;
        var vz = (dz / remaining) * currentSpeed;
        player.setVelocity(new Vector(vx, vy, vz));
        player.setFallDistance(0);

        drawRope(
          new Location(pLoc.getWorld(), pLoc.getX(), pLoc.getY() + 1.0, pLoc.getZ()),
          anchor
        );

        pLoc.getWorld().spawnParticle(Particle.DUST, pLoc, 2, 0.05, 0.05, 0.05, 0, LINE_DUST);

        if (tick % 4 === 0) {
          engine.effects.sound(pLoc, "entity.fishing_bobber.retrieve", 0.4, 2.0);
        }
      }
    }, 1, 1);
  }
});

// ================================================================
//  Create the Grappling Hook Item
// ================================================================
engine.item({
  id: "grappling_hook",
  type: "BLAZE_ROD",
  name: "&b&lGrappling Hook",
  lore: [
    "&7&m─────────────────────",
    "&bGrapple Shot &7- Right Click",
    "&7  Fires a hook at a surface or entity",
    "&7&m─────────────────────",
    "",
    "&8Legendary Tool"
  ],
  abilities: ["grapple_shot"],
  unbreakable: true
});

// ================================================================
//  Give hook on join
// ================================================================
engine.ability({
  id: "give_grappling_hook",
  trigger: "ON_JOIN",
  execute: function(ctx) {
    engine.items.give(ctx.player(), "grappling_hook");
    ctx.player().sendMessage("§b§lYou received the Grappling Hook!");
  }
});

engine.log("[GrapplingHook] Script loaded successfully.");
