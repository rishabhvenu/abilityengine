// ============================================================
// Rend Bow
// Place in: plugins/AbilityEngine/scripts/rend-bow.js
//
// Abilities:
//   1. Arrow Impale   - Shoot: arrows that hit living entities
//                       embed and are tracked (up to 6 per
//                       target).
//   2. Rend           - Left click: violently rips all embedded
//                       arrows from targets, dealing 1 heart
//                       per arrow. Arrows visually tear out and
//                       fly back to the shooter.
//
// Item: Unbreakable bow with both abilities wired up.
// ============================================================

var Vector    = Java.type("org.bukkit.util.Vector");
var Location  = Java.type("org.bukkit.Location");
var Particle  = Java.type("org.bukkit.Particle");
var DustOptions = Java.type("org.bukkit.Particle$DustOptions");
var Color     = Java.type("org.bukkit.Color");
var Float     = Java.type("java.lang.Float");
var Bukkit    = Java.type("org.bukkit.Bukkit");
var UUID      = Java.type("java.util.UUID");

// ─── Constants ──────────────────────────────────────────────
var MAX_ARROWS          = 6;
var DAMAGE_PER_ARROW    = 2.0;   // 1 heart = 2 HP
var REND_COOLDOWN       = 8;
var ARROW_RETURN_TICKS  = 14;    // flight time per arrow
var ARROW_STAGGER_TICKS = 3;     // delay between each arrow launching back
var REND_RANGE          = 64;    // max distance for visual return

// ─── Particle presets ───────────────────────────────────────
var BLOOD_DUST       = new DustOptions(Color.fromRGB(140, 10, 10),  Float.parseFloat("1.6"));
var DARK_RED_DUST    = new DustOptions(Color.fromRGB(90, 0, 0),     Float.parseFloat("1.2"));
var CRIMSON_DUST     = new DustOptions(Color.fromRGB(200, 25, 25),  Float.parseFloat("1.0"));
var ARROW_TIP_DUST   = new DustOptions(Color.fromRGB(50, 50, 55),   Float.parseFloat("0.9"));
var ARROW_SHAFT_DUST = new DustOptions(Color.fromRGB(160, 130, 80), Float.parseFloat("0.7"));
var TRAIL_DUST       = new DustOptions(Color.fromRGB(120, 12, 12),  Float.parseFloat("0.6"));

// ─── Arrow tracker ──────────────────────────────────────────
// arrowTracker[shooterUUID_string][targetUUID_string] = count
var arrowTracker = {};

function getArrowCount(shooterUUID, targetUUID) {
  if (!arrowTracker[shooterUUID]) return 0;
  return arrowTracker[shooterUUID][targetUUID] || 0;
}

function addArrow(shooterUUID, targetUUID) {
  if (!arrowTracker[shooterUUID]) arrowTracker[shooterUUID] = {};
  var current = arrowTracker[shooterUUID][targetUUID] || 0;
  if (current >= MAX_ARROWS) return false;
  arrowTracker[shooterUUID][targetUUID] = current + 1;
  return true;
}

function clearArrows(shooterUUID, targetUUID) {
  if (!arrowTracker[shooterUUID]) return;
  delete arrowTracker[shooterUUID][targetUUID];
}

function getAllTargets(shooterUUID) {
  var result = [];
  if (!arrowTracker[shooterUUID]) return result;
  var map = arrowTracker[shooterUUID];
  for (var uuid in map) {
    if (map[uuid] > 0) {
      result.push({ uuid: uuid, count: map[uuid] });
    }
  }
  return result;
}

// ================================================================
//  ABILITY: Arrow Tracking (PROJECTILE_HIT)
//  Each arrow that lands in a living entity is tracked.
// ================================================================
engine.ability({
  id: "rend_arrow_track",
  trigger: "PROJECTILE_HIT",
  execute: function(ctx) {
    var event = ctx.event();
    if (!event) return;

    // Only track arrows (not tridents, snowballs, etc.)
    var projectile = event.getEntity();
    if (projectile.getType().name() !== "ARROW") return;

    var target = ctx.targetEntity();
    if (!target) return;

    // Must be a living entity
    try { target.getHealth(); } catch (e) { return; }

    var shooterUUID = ctx.player().getUniqueId().toString();
    var targetUUID  = target.getUniqueId().toString();

    // No self-tracking
    if (shooterUUID === targetUUID) return;

    if (!addArrow(shooterUUID, targetUUID)) return; // at cap

    var count = getArrowCount(shooterUUID, targetUUID);
    var world = target.getWorld();
    var hitLoc = target.getLocation().add(0, 1.0, 0);

    // Blood splatter at the wound
    world.spawnParticle(Particle.DUST, hitLoc, 10, 0.25, 0.35, 0.25, 0, BLOOD_DUST);
    world.spawnParticle(Particle.DAMAGE_INDICATOR, hitLoc, 3, 0.2, 0.3, 0.2, 0.02);

    // Embed sound — deeper with each successive arrow
    engine.effects.sound(hitLoc, "entity.arrow.hit_player", 0.9, 1.1 - count * 0.08);

    // Notify the shooter
    var pips = "";
    for (var i = 0; i < count; i++) pips += "\u00A74\u25C6";
    ctx.player().sendMessage(pips + " \u00A7c" + count + " arrow" +
      (count > 1 ? "s" : "") + " embedded \u00A78[\u00A77Left click to Rend\u00A78]");
  }
});

// ================================================================
//  ABILITY: Rend (Left Click)
//  Rips every embedded arrow from all targets. Deals 1 heart
//  per arrow, then visually tears them out and pulls them back
//  to the shooter.
// ================================================================
engine.ability({
  id: "rend_activate",
  trigger: "LEFT_CLICK",
  cooldown: {
    seconds: REND_COOLDOWN,
    showBossBar: true,
    bossBarColor: "RED",
    bossBarLabel: "Rend"
  },
  interrupts: ["DEATH", "QUIT"],
  execute: function(ctx) {
    var player     = ctx.player();
    var shooterUUID = player.getUniqueId().toString();
    var targets    = getAllTargets(shooterUUID);

    // Nothing to rend — refund cooldown
    if (targets.length === 0) {
      player.sendMessage("\u00A77No embedded arrows to rend.");
      ctx.scheduleDelayed(function() {
        ctx.overrideCooldown(0);
      }, 1);
      return;
    }

    // ─── Activation flash ──────────────────────────────
    var world = player.getWorld();
    engine.effects.sound(player.getLocation(), "entity.warden.sonic_boom", 0.35, 1.8);
    engine.effects.sound(player.getLocation(), "block.chain.break", 1.0, 0.4);
    player.sendMessage("\u00A74\u00A7lRend!");

    var totalArrows  = 0;
    var arrowIndex   = 0;

    for (var i = 0; i < targets.length; i++) {
      var data        = targets[i];
      var targetUUID  = data.uuid;
      var arrowCount  = data.count;

      // Resolve the target entity by UUID
      var targetEntity = null;
      try { targetEntity = Bukkit.getEntity(UUID.fromString(targetUUID)); }
      catch (e) { /* invalid UUID */ }

      if (!targetEntity || !targetEntity.isValid()) {
        clearArrows(shooterUUID, targetUUID);
        continue;
      }

      try { if (targetEntity.isDead()) { clearArrows(shooterUUID, targetUUID); continue; } }
      catch (e) { clearArrows(shooterUUID, targetUUID); continue; }

      totalArrows += arrowCount;

      // ─── Damage ────────────────────────────────────
      var damage = arrowCount * DAMAGE_PER_ARROW;
      try { targetEntity.damage(damage, player); }
      catch (e) { try { targetEntity.damage(damage); } catch (e2) {} }

      // ─── Ripping VFX at the target ─────────────────
      var tLoc = targetEntity.getLocation().add(0, 1.0, 0);

      // Blood explosion
      world.spawnParticle(Particle.DUST, tLoc, 35, 0.5, 0.7, 0.5, 0, BLOOD_DUST);
      world.spawnParticle(Particle.DUST, tLoc, 20, 0.4, 0.5, 0.4, 0, DARK_RED_DUST);
      world.spawnParticle(Particle.DAMAGE_INDICATOR, tLoc, 12, 0.4, 0.5, 0.4, 0.06);

      // Gruesome sounds
      engine.effects.sound(tLoc, "entity.player.hurt", 1.0, 0.5);
      engine.effects.sound(tLoc, "block.chain.break", 0.9, 0.6);
      engine.effects.sound(tLoc, "entity.hoglin.converted_to_zombified", 0.6, 1.6);

      // Remove vanilla stuck-arrow visuals
      try { targetEntity.setArrowsInBody(Math.max(0, targetEntity.getArrowsInBody() - arrowCount)); }
      catch (e) { /* older API */ }

      // Brief stagger
      engine.effects.potion(targetEntity, "SLOWNESS", 20, 1);

      // ─── Animate each arrow returning ──────────────
      for (var a = 0; a < arrowCount; a++) {
        scheduleArrowReturn(ctx, targetEntity, player, arrowIndex * ARROW_STAGGER_TICKS);
        arrowIndex++;
      }

      clearArrows(shooterUUID, targetUUID);
    }

    // Summary message
    if (totalArrows > 0) {
      var hearts = (totalArrows * DAMAGE_PER_ARROW) / 2;
      player.sendMessage("\u00A74Rend \u00A7ctore " + totalArrows + " arrow" +
        (totalArrows > 1 ? "s" : "") + " free! \u00A78(\u00A7c" +
        hearts + " \u2764\u00A78)");
    }
  }
});

// ================================================================
//  Arrow return animation
//  A particle "arrow" arcs from the target back to the shooter.
// ================================================================
function scheduleArrowReturn(ctx, targetEntity, shooter, delayTicks) {
  ctx.scheduleDelayed(function() {
    var world = shooter.getWorld();

    // Snapshot the extraction point (target's body + random offset)
    var startLoc;
    if (targetEntity.isValid() && !targetEntity.isDead()) {
      startLoc = targetEntity.getLocation().clone().add(
        (Math.random() - 0.5) * 0.6,
        0.7 + Math.random() * 0.8,
        (Math.random() - 0.5) * 0.6
      );
    } else {
      startLoc = shooter.getLocation().clone().add(0, 1, 0);
      return; // target gone, skip animation
    }

    // Check range — if too far, skip the visual but the damage is already done
    if (startLoc.getWorld() !== shooter.getWorld() ||
        startLoc.distanceSquared(shooter.getLocation()) > REND_RANGE * REND_RANGE) {
      engine.effects.sound(shooter.getLocation(), "entity.item.pickup", 0.4, 0.8);
      return;
    }

    // Extraction burst
    world.spawnParticle(Particle.DUST, startLoc, 6, 0.12, 0.12, 0.12, 0, CRIMSON_DUST);
    world.spawnParticle(Particle.DAMAGE_INDICATOR, startLoc, 2, 0.08, 0.08, 0.08, 0.01);
    engine.effects.sound(startLoc, "entity.arrow.shoot", 0.5, 1.6 + Math.random() * 0.4);

    var tick = 0;

    var taskId = ctx.scheduleRepeating(function() {
      tick++;

      var progress = Math.min(tick / ARROW_RETURN_TICKS, 1.0);

      // Ease-in for natural acceleration feel
      var eased = progress * progress;

      // Track shooter live so the arrow homes in
      var endLoc = shooter.getLocation().clone().add(0, 1.0, 0);

      // Interpolate with a smooth arc
      var cx = startLoc.getX() + (endLoc.getX() - startLoc.getX()) * eased;
      var cy = startLoc.getY() + (endLoc.getY() - startLoc.getY()) * eased;
      var cz = startLoc.getZ() + (endLoc.getZ() - startLoc.getZ()) * eased;

      // Parabolic arc — peaks in the middle of flight
      cy += Math.sin(progress * Math.PI) * 1.8;

      var loc = new Location(world, cx, cy, cz);

      // Arrow tip
      world.spawnParticle(Particle.DUST, loc, 2, 0.04, 0.04, 0.04, 0, ARROW_TIP_DUST);
      // Wooden shaft
      world.spawnParticle(Particle.DUST, loc, 1, 0.03, 0.03, 0.03, 0, ARROW_SHAFT_DUST);
      // Blood dripping off
      world.spawnParticle(Particle.DUST, loc, 3, 0.08, 0.08, 0.08, 0, TRAIL_DUST);
      world.spawnParticle(Particle.DUST, loc, 2, 0.06, 0.06, 0.06, 0, CRIMSON_DUST);

      // Occasional drip
      if (tick % 3 === 0) {
        world.spawnParticle(Particle.DRIPPING_LAVA, loc, 1, 0.04, 0.04, 0.04, 0);
      }

      // Whoosh
      if (tick % 5 === 0) {
        engine.effects.sound(loc, "entity.arrow.shoot", 0.15, 1.9 + progress * 0.3);
      }

      // Arrived at the shooter
      if (tick >= ARROW_RETURN_TICKS) {
        engine.effects.sound(endLoc, "entity.item.pickup", 0.4, 0.7 + Math.random() * 0.5);
        world.spawnParticle(Particle.DUST, endLoc, 5, 0.15, 0.15, 0.15, 0, CRIMSON_DUST);
        ctx.cancelTask(taskId);
      }
    }, 1, 1);
  }, delayTicks);
}

// ================================================================
//  Cleanup — wipe arrow data on kill or quit
// ================================================================
engine.ability({
  id: "rend_cleanup_kill",
  trigger: "KILL_ENTITY",
  execute: function(ctx) {
    var target = ctx.targetEntity();
    if (!target) return;
    clearArrows(ctx.player().getUniqueId().toString(),
                target.getUniqueId().toString());
  }
});

engine.ability({
  id: "rend_cleanup_quit",
  trigger: "ON_QUIT",
  execute: function(ctx) {
    var uuid = ctx.player().getUniqueId().toString();
    // Remove arrows this player shot
    delete arrowTracker[uuid];
    // Remove arrows embedded in this player
    for (var shooter in arrowTracker) {
      delete arrowTracker[shooter][uuid];
    }
  }
});

// ================================================================
//  Create the Rend Bow Item
// ================================================================
engine.item({
  id: "rend_bow",
  type: "BOW",
  name: "&4&lRend Bow",
  lore: [
    "&7&m\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500",
    "&4Arrow Impale &7- Shoot",
    "&7  Arrows burrow into targets",
    "&7  Up to &c6 &7arrows per target",
    "",
    "&c&lRend &7- Left Click",
    "&7  Violently rips all embedded",
    "&7  arrows free, dealing",
    "&7  &c1 heart &7per arrow torn out",
    "&7  Arrows return to you",
    "&7&m\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500",
    "",
    "&8Legendary Weapon"
  ],
  abilities: ["rend_arrow_track", "rend_activate"],
  unbreakable: true
});

// ================================================================
//  Give bow on join
// ================================================================
engine.ability({
  id: "give_rend_bow",
  trigger: "ON_JOIN",
  execute: function(ctx) {
    engine.items.give(ctx.player(), "rend_bow");
    ctx.player().sendMessage("\u00A74\u00A7lYou received the Rend Bow!");
  }
});

engine.log("[RendBow] Script loaded successfully.");
