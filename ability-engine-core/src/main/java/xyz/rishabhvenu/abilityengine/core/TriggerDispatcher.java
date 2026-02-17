package xyz.rishabhvenu.abilityengine.core;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import xyz.rishabhvenu.abilityengine.api.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Listens to Bukkit events and dispatches them to matching abilities.
 * Resolves trigger types, builds contexts, checks conditions and cooldowns.
 */
public final class TriggerDispatcher implements Listener {
    
    private static final long DOUBLE_SHIFT_WINDOW_MS = 400;
    
    private final Plugin plugin;
    private final Logger logger;
    private final AbilityRegistry registry;
    private final AbilityItemService itemService;
    private final CooldownManager cooldownManager;
    
    // State for advanced triggers
    private final Map<UUID, Long> lastSneakTimestamps = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> holdShiftTasks = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> wasOnGround = new ConcurrentHashMap<>();
    
    public TriggerDispatcher(
            Plugin plugin,
            AbilityRegistry registry,
            AbilityItemService itemService,
            CooldownManager cooldownManager) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.registry = registry;
        this.itemService = itemService;
        this.cooldownManager = cooldownManager;
    }
    
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        
        if (!itemService.isAbilityItem(item)) {
            return;
        }
        
        TriggerType trigger = resolveTrigger(event.getAction(), player.isSneaking(), false);
        if (trigger == null) {
            return;
        }
        
        AbilityContext context = new AbilityContext(
            player,
            trigger,
            null,
            event.getClickedBlock(),
            item,
            event
        );
        
        dispatchAbilities(context, item, trigger);
    }
    
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        
        if (!itemService.isAbilityItem(item)) {
            return;
        }
        
        // PlayerInteractEntityEvent is always a right-click
        TriggerType trigger = player.isSneaking() ? TriggerType.SHIFT_RIGHT_CLICK_ENTITY : TriggerType.RIGHT_CLICK_ENTITY;
        
        AbilityContext context = new AbilityContext(
            player,
            trigger,
            event.getRightClicked(),
            null,
            item,
            event
        );
        
        dispatchAbilities(context, item, trigger);
    }
    
    @EventHandler(priority = EventPriority.NORMAL)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) {
            return;
        }
        
        ItemStack item = player.getInventory().getItemInMainHand();
        if (!itemService.isAbilityItem(item)) {
            return;
        }
        
        TriggerType trigger = TriggerType.DAMAGE_DEALT;
        
        AbilityContext context = new AbilityContext(
            player,
            trigger,
            event.getEntity(),
            null,
            item,
            event
        );
        
        dispatchAbilities(context, item, trigger);
    }
    
    @EventHandler(priority = EventPriority.NORMAL)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        
        ItemStack item = player.getInventory().getItemInMainHand();
        if (!itemService.isAbilityItem(item)) {
            return;
        }
        
        TriggerType trigger = TriggerType.DAMAGE_TAKEN;
        
        Entity damager = null;
        if (event instanceof EntityDamageByEntityEvent byEntityEvent) {
            damager = byEntityEvent.getDamager();
        }
        
        AbilityContext context = new AbilityContext(
            player,
            trigger,
            damager,
            null,
            item,
            event
        );
        
        dispatchAbilities(context, item, trigger);
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        // Only fire if player actually moved (not just head rotation)
        if (event.getFrom().distanceSquared(event.getTo()) < 0.001) {
            return;
        }
        
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        
        if (!itemService.isAbilityItem(item)) {
            return;
        }
        
        TriggerType trigger = TriggerType.MOVE;
        
        AbilityContext context = new AbilityContext(
            player,
            trigger,
            null,
            null,
            item,
            event
        );
        
        dispatchAbilities(context, item, trigger);
        
        // Track ground state for JUMP/LAND detection
        UUID playerId = player.getUniqueId();
        boolean currentlyOnGround = player.isOnGround();
        Boolean previouslyOnGround = wasOnGround.get(playerId);
        
        // JUMP detection: was on ground, now airborne with upward velocity
        if (previouslyOnGround != null && previouslyOnGround && !currentlyOnGround 
                && player.getVelocity().getY() > 0.1) {
            dispatchAbilities(
                new AbilityContext(player, TriggerType.JUMP, null, null, item, event),
                item,
                TriggerType.JUMP
            );
        }
        
        // LAND detection: was airborne, now on ground
        if (previouslyOnGround != null && !previouslyOnGround && currentlyOnGround) {
            dispatchAbilities(
                new AbilityContext(player, TriggerType.LAND, null, null, item, event),
                item,
                TriggerType.LAND
            );
        }
        
        wasOnGround.put(playerId, currentlyOnGround);
    }
    
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        if (!event.isSneaking()) {
            // Player stopped sneaking - cancel any HOLD_SHIFT tasks
            UUID playerId = event.getPlayer().getUniqueId();
            Integer taskId = holdShiftTasks.remove(playerId);
            if (taskId != null) {
                Bukkit.getScheduler().cancelTask(taskId);
            }
            return;
        }
        
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        
        if (!itemService.isAbilityItem(item)) {
            return;
        }
        
        UUID playerId = player.getUniqueId();
        long now = System.currentTimeMillis();
        Long lastSneak = lastSneakTimestamps.get(playerId);
        
        // DOUBLE_SHIFT detection
        if (lastSneak != null && (now - lastSneak) <= DOUBLE_SHIFT_WINDOW_MS) {
            // Double-tap detected, reset timestamp to prevent triple-tap
            lastSneakTimestamps.put(playerId, 0L);
            
            AbilityContext context = new AbilityContext(
                player,
                TriggerType.DOUBLE_SHIFT,
                null,
                null,
                item,
                event
            );
            
            dispatchAbilities(context, item, TriggerType.DOUBLE_SHIFT);
        } else {
            // Update timestamp for next potential double-tap
            lastSneakTimestamps.put(playerId, now);
        }
        
        // HOLD_SHIFT detection - schedule task for abilities with HOLD_SHIFT trigger
        // Note: HOLD_SHIFT duration is ability-specific, handled in dispatchAbilities
        // For now, we'll trigger immediately and let abilities handle duration via conditions
        AbilityContext context = new AbilityContext(
            player,
            TriggerType.HOLD_SHIFT,
            null,
            null,
            item,
            event
        );
        
        dispatchAbilities(context, item, TriggerType.HOLD_SHIFT);
    }
    
    @EventHandler(priority = EventPriority.NORMAL)
    public void onProjectileHit(ProjectileHitEvent event) {
        Projectile projectile = event.getEntity();
        
        if (!(projectile.getShooter() instanceof Player player)) {
            return;
        }
        
        ItemStack item = player.getInventory().getItemInMainHand();
        if (!itemService.isAbilityItem(item)) {
            return;
        }
        
        AbilityContext context = new AbilityContext(
            player,
            TriggerType.PROJECTILE_HIT,
            event.getHitEntity(),
            event.getHitBlock(),
            item,
            event
        );
        
        dispatchAbilities(context, item, TriggerType.PROJECTILE_HIT);
    }
    
    @EventHandler(priority = EventPriority.NORMAL)
    public void onEntityDeath(EntityDeathEvent event) {
        Entity killed = event.getEntity();
        Player killer = killed instanceof org.bukkit.entity.LivingEntity living ? living.getKiller() : null;
        
        if (killer == null) {
            return;
        }
        
        ItemStack item = killer.getInventory().getItemInMainHand();
        if (!itemService.isAbilityItem(item)) {
            return;
        }
        
        AbilityContext context = new AbilityContext(
            killer,
            TriggerType.KILL_ENTITY,
            killed,
            null,
            item,
            event
        );
        
        dispatchAbilities(context, item, TriggerType.KILL_ENTITY);
    }
    
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // Initialize ground tracking
        wasOnGround.put(player.getUniqueId(), player.isOnGround());
        
        // ON_JOIN is a lifecycle trigger — dispatch to ALL registered abilities
        // with ON_JOIN trigger, regardless of held item
        dispatchLifecycleAbilities(player, TriggerType.ON_JOIN, event);
    }
    
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        
        // Cleanup state
        lastSneakTimestamps.remove(playerId);
        wasOnGround.remove(playerId);
        Integer taskId = holdShiftTasks.remove(playerId);
        if (taskId != null) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
        
        // ON_QUIT is a lifecycle trigger — dispatch to ALL registered abilities
        // with ON_QUIT trigger, regardless of held item
        dispatchLifecycleAbilities(player, TriggerType.ON_QUIT, event);
    }
    
    /**
     * Dispatches lifecycle triggers (ON_JOIN, ON_QUIT) to all registered abilities
     * that have the matching trigger, bypassing item checks.
     */
    private void dispatchLifecycleAbilities(Player player, TriggerType trigger, org.bukkit.event.Event event) {
        AbilityContext context = new AbilityContext(
            player,
            trigger,
            null,
            null,
            null,
            event
        );
        
        for (Ability ability : registry.getAll()) {
            if (!ability.triggers().contains(trigger)) {
                continue;
            }
            
            // Check permission
            if (ability.permission() != null && !player.hasPermission(ability.permission())) {
                continue;
            }
            
            // Check cooldown
            if (!cooldownManager.isReady(player, ability.id())) {
                continue;
            }
            
            // Check conditions
            if (!ConditionEvaluator.evaluate(ability.conditions(), context)) {
                continue;
            }
            
            try {
                ability.execute(context);
                
                if (!ability.cooldown().isZero()) {
                    cooldownManager.setCooldown(player, ability.id(), ability.cooldown());
                }
            } catch (Exception e) {
                logger.severe("Error executing ability " + ability.id() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    private TriggerType resolveTrigger(Action action, boolean sneaking, boolean isEntity) {
        return switch (action) {
            case LEFT_CLICK_AIR, LEFT_CLICK_BLOCK -> 
                sneaking ? TriggerType.SHIFT_LEFT_CLICK : TriggerType.LEFT_CLICK;
            case RIGHT_CLICK_AIR, RIGHT_CLICK_BLOCK -> 
                sneaking ? TriggerType.SHIFT_RIGHT_CLICK : TriggerType.RIGHT_CLICK;
            default -> null;
        };
    }
    
    private void dispatchAbilities(AbilityContext context, ItemStack item, TriggerType trigger) {
        // Get all abilities on this item
        List<String> abilityIds = itemService.getAbilities(item);
        
        for (String abilityId : abilityIds) {
            // Check if this ability is bound to this trigger
            TriggerType abilityTrigger = itemService.getAbilityTrigger(item, abilityId);
            if (abilityTrigger != null && abilityTrigger != trigger) {
                continue; // This ability uses a different trigger
            }
            
            Ability ability = registry.get(abilityId);
            if (ability == null) {
                continue;
            }
            
            // Check if ability supports this trigger
            if (!ability.triggers().contains(trigger)) {
                continue;
            }
            
            // Check permission
            if (ability.permission() != null && !context.player().hasPermission(ability.permission())) {
                continue;
            }
            
            // Check cooldown
            if (!cooldownManager.isReady(context.player(), abilityId)) {
                continue;
            }
            
            // Check conditions
            if (!ConditionEvaluator.evaluate(ability.conditions(), context)) {
                continue;
            }
            
            // Execute ability
            try {
                ability.execute(context);
                
                // Set cooldown
                if (!ability.cooldown().isZero()) {
                    cooldownManager.setCooldown(context.player(), abilityId, ability.cooldown());
                }
            } catch (Exception e) {
                logger.severe("Error executing ability " + abilityId + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
