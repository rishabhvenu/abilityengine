package xyz.rishabhvenu.abilityengine.plugin.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import xyz.rishabhvenu.abilityengine.api.Ability;
import xyz.rishabhvenu.abilityengine.api.AbilityItemService;
import xyz.rishabhvenu.abilityengine.api.AbilityRegistry;
import xyz.rishabhvenu.abilityengine.config.ConfigAbilityLoader;
import xyz.rishabhvenu.abilityengine.loader.LoadedModule;
import xyz.rishabhvenu.abilityengine.loader.ModuleLoader;
import xyz.rishabhvenu.abilityengine.plugin.AbilityEnginePlugin;
import xyz.rishabhvenu.abilityengine.script.ScriptContext;
import xyz.rishabhvenu.abilityengine.script.ScriptEngine;

import java.io.File;
import java.util.Map;

/**
 * Brigadier-based command handler for AbilityEngine.
 */
public final class AbilityCommand {
    
    private final AbilityEnginePlugin plugin;
    private final AbilityRegistry registry;
    private final AbilityItemService itemService;
    private final ConfigAbilityLoader configLoader;
    private final ScriptEngine scriptEngine;
    private final ModuleLoader moduleLoader;
    
    public AbilityCommand(
            AbilityEnginePlugin plugin,
            AbilityRegistry registry,
            AbilityItemService itemService,
            ConfigAbilityLoader configLoader,
            ScriptEngine scriptEngine,
            ModuleLoader moduleLoader) {
        this.plugin = plugin;
        this.registry = registry;
        this.itemService = itemService;
        this.configLoader = configLoader;
        this.scriptEngine = scriptEngine;
        this.moduleLoader = moduleLoader;
    }
    
    /**
     * Registers the /ability command using Brigadier.
     */
    public LiteralArgumentBuilder<CommandSourceStack> createCommand() {
        return Commands.literal("ability")
            .requires(source -> source.getSender().hasPermission("abilityengine.command"))
            .executes(this::showUsage)
            .then(createGiveCommand())
            .then(createReloadCommand())
            .then(createListCommand())
            .then(createInfoCommand())
            .then(createScriptCommand())
            .then(createModuleCommand());
    }
    
    private int showUsage(CommandContext<CommandSourceStack> ctx) {
        var sender = ctx.getSource().getSender();
        sender.sendMessage(Component.text("=== AbilityEngine Commands ===").color(NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/ability give <player> <ability> - Give an ability item").color(NamedTextColor.GRAY));
        sender.sendMessage(Component.text("/ability reload - Reload config abilities and scripts").color(NamedTextColor.GRAY));
        sender.sendMessage(Component.text("/ability list - List all registered abilities").color(NamedTextColor.GRAY));
        sender.sendMessage(Component.text("/ability info <ability> - Show ability details").color(NamedTextColor.GRAY));
        sender.sendMessage(Component.text("/ability script reload [file] - Reload scripts").color(NamedTextColor.GRAY));
        sender.sendMessage(Component.text("/ability script list - List loaded scripts").color(NamedTextColor.GRAY));
        sender.sendMessage(Component.text("/ability module list - List loaded modules").color(NamedTextColor.GRAY));
        return Command.SINGLE_SUCCESS;
    }
    
    // /ability give <player> <ability>
    private LiteralArgumentBuilder<CommandSourceStack> createGiveCommand() {
        return Commands.literal("give")
            .requires(source -> source.getSender().hasPermission("abilityengine.command.give"))
            .then(Commands.argument("player", StringArgumentType.word())
                .suggests(playerSuggestions())
                .then(Commands.argument("ability", StringArgumentType.word())
                    .suggests(abilitySuggestions())
                    .executes(ctx -> {
                        String playerName = StringArgumentType.getString(ctx, "player");
                        String abilityId = StringArgumentType.getString(ctx, "ability");
                        
                        Player target = Bukkit.getPlayer(playerName);
                        if (target == null) {
                            ctx.getSource().getSender().sendMessage(
                                Component.text("Player not found: " + playerName).color(NamedTextColor.RED)
                            );
                            return 0;
                        }
                        
                        if (!registry.isRegistered(abilityId)) {
                            ctx.getSource().getSender().sendMessage(
                                Component.text("Unknown ability: " + abilityId).color(NamedTextColor.RED)
                            );
                            return 0;
                        }
                        
                        ItemStack item = itemService.createAbilityItem(abilityId);
                        if (item == null) {
                            ctx.getSource().getSender().sendMessage(
                                Component.text("Failed to create ability item").color(NamedTextColor.RED)
                            );
                            return 0;
                        }
                        
                        target.getInventory().addItem(item);
                        ctx.getSource().getSender().sendMessage(
                            Component.text("Gave " + target.getName() + " ability: " + abilityId)
                                .color(NamedTextColor.GREEN)
                        );
                        target.sendMessage(
                            Component.text("You received an ability: " + abilityId).color(NamedTextColor.GREEN)
                        );
                        
                        return Command.SINGLE_SUCCESS;
                    })
                )
            );
    }
    
    // /ability reload
    private LiteralArgumentBuilder<CommandSourceStack> createReloadCommand() {
        return Commands.literal("reload")
            .requires(source -> source.getSender().hasPermission("abilityengine.command.reload"))
            .executes(ctx -> {
                var sender = ctx.getSource().getSender();
                sender.sendMessage(Component.text("Reloading AbilityEngine...").color(NamedTextColor.YELLOW));
                
                // Reload config abilities
                File abilitiesDir = new File(plugin.getDataFolder(), "abilities");
                int configLoaded = configLoader.loadAbilities(abilitiesDir);
                
                // Reload scripts
                File scriptsDir = new File(plugin.getDataFolder(), "scripts");
                scriptEngine.reloadAll(scriptsDir);
                int scriptsLoaded = scriptEngine.getLoadedScripts().size();
                
                sender.sendMessage(Component.text("Reloaded " + configLoaded + " config abilities").color(NamedTextColor.GREEN));
                sender.sendMessage(Component.text("Reloaded " + scriptsLoaded + " scripts").color(NamedTextColor.GREEN));
                
                return Command.SINGLE_SUCCESS;
            });
    }
    
    // /ability list
    private LiteralArgumentBuilder<CommandSourceStack> createListCommand() {
        return Commands.literal("list")
            .requires(source -> source.getSender().hasPermission("abilityengine.command.list"))
            .executes(ctx -> {
                var sender = ctx.getSource().getSender();
                var abilities = registry.getAll();
                
                if (abilities.isEmpty()) {
                    sender.sendMessage(Component.text("No abilities registered").color(NamedTextColor.YELLOW));
                    return 0;
                }
                
                sender.sendMessage(Component.text("Registered Abilities (" + abilities.size() + "):").color(NamedTextColor.GOLD));
                for (Ability ability : abilities) {
                    sender.sendMessage(Component.text("  - " + ability.id()).color(NamedTextColor.GRAY));
                }
                
                return Command.SINGLE_SUCCESS;
            });
    }
    
    // /ability info <ability>
    private LiteralArgumentBuilder<CommandSourceStack> createInfoCommand() {
        return Commands.literal("info")
            .requires(source -> source.getSender().hasPermission("abilityengine.command.info"))
            .then(Commands.argument("ability", StringArgumentType.word())
                .suggests(abilitySuggestions())
                .executes(ctx -> {
                    String abilityId = StringArgumentType.getString(ctx, "ability");
                    Ability ability = registry.get(abilityId);
                    
                    if (ability == null) {
                        ctx.getSource().getSender().sendMessage(
                            Component.text("Unknown ability: " + abilityId).color(NamedTextColor.RED)
                        );
                        return 0;
                    }
                    
                    var sender = ctx.getSource().getSender();
                    sender.sendMessage(Component.text("=== Ability Info: " + ability.id() + " ===").color(NamedTextColor.GOLD));
                    sender.sendMessage(Component.text("Triggers: " + ability.triggers().stream()
                        .map(Enum::name)
                        .reduce((a, b) -> a + ", " + b)
                        .orElse("None")).color(NamedTextColor.GRAY));
                    sender.sendMessage(Component.text("Cooldown: " + ability.cooldown().toSeconds() + "s").color(NamedTextColor.GRAY));
                    sender.sendMessage(Component.text("Conditions: " + ability.conditions().size()).color(NamedTextColor.GRAY));
                    
                    return Command.SINGLE_SUCCESS;
                })
            );
    }
    
    // /ability script <reload|list> [filename]
    private LiteralArgumentBuilder<CommandSourceStack> createScriptCommand() {
        return Commands.literal("script")
            .requires(source -> source.getSender().hasPermission("abilityengine.command.script"))
            .then(Commands.literal("reload")
                .executes(ctx -> {
                    // Reload all scripts
                    File scriptsDir = new File(plugin.getDataFolder(), "scripts");
                    scriptEngine.reloadAll(scriptsDir);
                    int count = scriptEngine.getLoadedScripts().size();
                    
                    ctx.getSource().getSender().sendMessage(
                        Component.text("Reloaded " + count + " script(s)").color(NamedTextColor.GREEN)
                    );
                    return Command.SINGLE_SUCCESS;
                })
                .then(Commands.argument("filename", StringArgumentType.greedyString())
                    .suggests(scriptSuggestions())
                    .executes(ctx -> {
                        // Reload specific script
                        String scriptName = StringArgumentType.getString(ctx, "filename");
                        if (!scriptName.endsWith(".js")) {
                            scriptName += ".js";
                        }
                        
                        try {
                            File scriptsDir = new File(plugin.getDataFolder(), "scripts");
                            scriptEngine.reloadScript(scriptName, scriptsDir);
                            ctx.getSource().getSender().sendMessage(
                                Component.text("Reloaded script: " + scriptName).color(NamedTextColor.GREEN)
                            );
                            return Command.SINGLE_SUCCESS;
                        } catch (Exception e) {
                            ctx.getSource().getSender().sendMessage(
                                Component.text("Failed to reload script: " + e.getMessage()).color(NamedTextColor.RED)
                            );
                            return 0;
                        }
                    })
                )
            )
            .then(Commands.literal("list")
                .executes(ctx -> {
                    Map<String, ScriptContext> scripts = scriptEngine.getLoadedScripts();
                    
                    if (scripts.isEmpty()) {
                        ctx.getSource().getSender().sendMessage(
                            Component.text("No scripts loaded").color(NamedTextColor.YELLOW)
                        );
                        return 0;
                    }
                    
                    var sender = ctx.getSource().getSender();
                    sender.sendMessage(Component.text("Loaded Scripts (" + scripts.size() + "):").color(NamedTextColor.GOLD));
                    
                    for (Map.Entry<String, ScriptContext> entry : scripts.entrySet()) {
                        int abilityCount = entry.getValue().getAbilityIds().size();
                        sender.sendMessage(Component.text("  - " + entry.getKey() + " (" + abilityCount + " abilities)")
                            .color(NamedTextColor.GRAY));
                    }
                    
                    return Command.SINGLE_SUCCESS;
                })
            );
    }
    
    // /ability module list
    private LiteralArgumentBuilder<CommandSourceStack> createModuleCommand() {
        return Commands.literal("module")
            .requires(source -> source.getSender().hasPermission("abilityengine.command.module"))
            .then(Commands.literal("list")
                .executes(ctx -> {
                    var modules = moduleLoader.getLoadedModules();
                    
                    if (modules.isEmpty()) {
                        ctx.getSource().getSender().sendMessage(
                            Component.text("No external modules loaded").color(NamedTextColor.YELLOW)
                        );
                        return 0;
                    }
                    
                    var sender = ctx.getSource().getSender();
                    sender.sendMessage(Component.text("Loaded Modules (" + modules.size() + "):").color(NamedTextColor.GOLD));
                    
                    for (LoadedModule module : modules) {
                        int abilityCount = module.abilityIds().size();
                        sender.sendMessage(Component.text("  - " + module.providerId() + " (" + abilityCount + " abilities)")
                            .color(NamedTextColor.GRAY));
                    }
                    
                    return Command.SINGLE_SUCCESS;
                })
            );
    }
    
    // Suggestion providers
    
    private SuggestionProvider<CommandSourceStack> playerSuggestions() {
        return (ctx, builder) -> {
            Bukkit.getOnlinePlayers().forEach(player -> 
                builder.suggest(player.getName())
            );
            return builder.buildFuture();
        };
    }
    
    private SuggestionProvider<CommandSourceStack> abilitySuggestions() {
        return (ctx, builder) -> {
            registry.getAll().forEach(ability -> 
                builder.suggest(ability.id())
            );
            return builder.buildFuture();
        };
    }
    
    private SuggestionProvider<CommandSourceStack> scriptSuggestions() {
        return (ctx, builder) -> {
            scriptEngine.getLoadedScripts().keySet().forEach(builder::suggest);
            return builder.buildFuture();
        };
    }
}
