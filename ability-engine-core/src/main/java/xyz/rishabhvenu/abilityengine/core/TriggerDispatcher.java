package xyz.rishabhvenu.abilityengine.core;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import xyz.rishabhvenu.abilityengine.api.*;

import java.util.List;
import java.util.logging.Logger;

/**
 * Listens to Bukkit events and dispatches them to matching abilities.
 * Resolves trigger types, builds contexts, checks conditions and cooldowns.
 */
public final class TriggerDispatcher implements Listener {
    
    private final Plugin plugin;
    private final Logger logger;
    private final AbilityRegistry registry;
    private final AbilityItemService itemService;
    private final CooldownManager cooldownManager;
    
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
