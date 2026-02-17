// ============================================================
// Phase 3 Demo: Advanced Runtime Features
// Place in: plugins/AbilityEngine/scripts/phase-demo.js
//
// Demonstrates:
//   - Phase / State Machine API
//   - Raycast Utility (Block + Entity detection)
//   - Movement Module (pull physics)
//   - Entity Control API (freeze)
//   - Cooldown Override API
//   - Interrupt System
// ============================================================

var Vector = Java.type("org.bukkit.util.Vector");
var Location = Java.type("org.bukkit.Location");

// ================================================================
//  ABILITY: Phase Chain - Multi-Phase Ability with State Machine
//  Right-click to fire a hook that triggers a multi-phase sequence
// ================================================================
engine.ability({
  id: "phase_chain",
  trigger: "RIGHT_CLICK",
  cooldown: {
    seconds: 10,
    showBossBar: true,
    bossBarColor: "PURPLE",
    bossBarLabel: "Phase Chain"
  },
  interrupts: ["TAKE_DAMAGE", "SWITCH_ITEM"],
  
  // Define phases
  phases: {
    // Phase 1: Charge up
    charge: {
      duration: 20,
      onStart: function(ctx, phase) {
        ctx.player().sendMessage("§5§lCharging...");
        phase.set("chargeLevel", 0);
      },
      onTick: function(ctx, phase) {
        var level = phase.get("chargeLevel") || 0;
        level += 5;
        phase.set("chargeLevel", level);
        
        // Visual feedback
        if (phase.tick % 5 === 0) {
          engine.effects.particle(
            ctx.player().getLocation().add(0, 1, 0),
            "ENCHANTMENT_TABLE",
            null,
            3,
            0.3, 0.3, 0.3
          );
        }
      },
      next: "fire"
    },
    
    // Phase 2: Fire hook
    fire: {
      duration: 1,
      onStart: function(ctx, phase) {
        var player = ctx.player();
        player.sendMessage("§5§lFiring Hook!");
        
        // Perform raycast
        var result = engine.raycast({
          origin: player.getEyeLocation(),
          direction: player.getLocation().getDirection(),
          maxDistance: 30,
          detect: ["BLOCK", "ENTITY"],
          entityRadius: 2.0,
          
          onHitBlock: function(hit) {
            ctx.player().sendMessage("§7Hit block at " + hit.location.getBlockX() + ", " + hit.location.getBlockY() + ", " + hit.location.getBlockZ());
            phase.set("hitLocation", hit.location);
            phase.set("hitType", "block");
          },
          
          onHitEntity: function(hit) {
            ctx.player().sendMessage("§7Hit entity: " + hit.entity.getName());
            phase.set("hitEntity", hit.entity);
            phase.set("hitLocation", hit.entity.getLocation());
            phase.set("hitType", "entity");
          },
          
          onMiss: function(endLoc) {
            ctx.player().sendMessage("§7Missed!");
            phase.set("hitType", "miss");
          }
        });
      },
      next: "pull"
    },
    
    // Phase 3: Pull or Freeze
    pull: {
      duration: 40,
      onStart: function(ctx, phase) {
        var hitType = phase.get("hitType");
        var hitLocation = phase.get("hitLocation");
        var hitEntity = phase.get("hitEntity");
        
        if (hitType === "miss") {
          // Reduce cooldown on miss
          ctx.overrideCooldown(2);
          ctx.player().sendMessage("§7Cooldown reduced!");
          return;
        }
        
        if (hitType === "entity" && hitEntity != null) {
          // Freeze the entity
          engine.control.freeze(hitEntity, {
            duration: 40,
            preventMovement: true
          }, ctx.execution());
          
          ctx.player().sendMessage("§5§lEntity frozen!");
          
          // Pull player to entity
          engine.movement.pull({
            entity: ctx.player(),
            target: hitLocation,
            speed: 1.5,
            drag: 0.7,
            minSpeed: 0.15,
            arrivalDistance: 2.0,
            maxTicks: 40,
            onArrival: function() {
              ctx.player().sendMessage("§5§lArrived!");
            }
          }, ctx.execution());
        } else if (hitType === "block") {
          // Pull to block
          engine.movement.pull({
            entity: ctx.player(),
            target: hitLocation,
            speed: 1.2,
            drag: 0.75,
            minSpeed: 0.15,
            arrivalDistance: 1.5,
            maxTicks: 40,
            onArrival: function() {
              ctx.player().sendMessage("§5§lReached target!");
            }
          }, ctx.execution());
        }
      },
      endWhen: function(ctx, phase) {
        // End phase early if we've arrived
        return phase.tick >= 40;
      },
      onEnd: function(ctx, phase) {
        ctx.player().sendMessage("§5§lPhase Chain Complete!");
      }
    }
  },
  
  onInterrupt: function(ctx) {
    ctx.player().sendMessage("§c§lPhase Chain Interrupted!");
    // Cooldown is auto-shortened on interrupt
    ctx.shortenCooldown(50); // 50% reduction
  }
});

// ================================================================
//  ABILITY: Quick Dash - Simple movement without phases
// ================================================================
engine.ability({
  id: "quick_dash",
  trigger: "SHIFT_RIGHT_CLICK",
  cooldown: 3,
  interrupts: ["TAKE_DAMAGE"],
  
  execute: function(ctx) {
    var player = ctx.player();
    var direction = player.getLocation().getDirection();
    
    engine.movement.dash({
      entity: player,
      direction: direction,
      power: 2.0,
      duration: 10
    }, ctx.execution());
    
    player.sendMessage("§b§lDash!");
    engine.effects.sound(player.getLocation(), "entity.bat.takeoff", 1.0, 0.8);
  },
  
  onInterrupt: function(ctx) {
    ctx.player().sendMessage("§c§lDash cancelled!");
  }
});

// ================================================================
//  ABILITY: Freeze Ray - Raycast to freeze target
// ================================================================
engine.ability({
  id: "freeze_ray",
  trigger: "LEFT_CLICK",
  cooldown: 5,
  
  execute: function(ctx) {
    var player = ctx.player();
    
    var result = engine.raycast({
      origin: player.getEyeLocation(),
      direction: player.getLocation().getDirection(),
      maxDistance: 20,
      detect: ["ENTITY"],
      entityRadius: 1.0,
      
      onHitEntity: function(hit) {
        engine.control.freeze(hit.entity, {
          duration: 60,
          preventMovement: true
        }, ctx.execution());
        
        player.sendMessage("§b§lFroze " + hit.entity.getName() + "!");
        engine.effects.particle(
          hit.entity.getLocation().add(0, 1, 0),
          "SNOWFLAKE",
          null,
          30,
          0.5, 0.5, 0.5
        );
      },
      
      onMiss: function(endLoc) {
        player.sendMessage("§7No target!");
      }
    });
  }
});

// ================================================================
//  Create the Phase Demo Item
// ================================================================
engine.item({
  id: "phase_demo_staff",
  type: "BLAZE_ROD",
  name: "&5&lPhase Demo Staff",
  lore: [
    "&7&m─────────────────────",
    "&5Phase Chain &7- Right Click",
    "&7  Multi-phase hook ability",
    "&7&m─────────────────────",
    "&bQuick Dash &7- Shift+Right Click",
    "&7  Rapid forward movement",
    "&7&m─────────────────────",
    "&bFreeze Ray &7- Left Click",
    "&7  Freeze target entity",
    "&7&m─────────────────────",
    "",
    "&8Phase 3 Runtime Demo"
  ],
  abilities: ["phase_chain", "quick_dash", "freeze_ray"],
  unbreakable: true
});

// ================================================================
//  Give staff on join
// ================================================================
engine.ability({
  id: "give_phase_demo",
  trigger: "ON_JOIN",
  execute: function(ctx) {
    engine.items.give(ctx.player(), "phase_demo_staff");
    ctx.player().sendMessage("§5§lReceived Phase Demo Staff!");
  }
});

engine.log("[PhaseDemo] Script loaded successfully. Demonstrates all Phase 3 features.");
