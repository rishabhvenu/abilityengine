package xyz.rishabhvenu.abilityengine.config;

import org.yaml.snakeyaml.Yaml;
import xyz.rishabhvenu.abilityengine.api.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Loads abilities from YAML configuration files.
 */
public final class ConfigAbilityLoader {
    
    private static final Pattern DURATION_PATTERN = Pattern.compile("(\\d+)([smhd])");
    
    private final Logger logger;
    private final AbilityRegistry registry;
    
    public ConfigAbilityLoader(Logger logger, AbilityRegistry registry) {
        this.logger = logger;
        this.registry = registry;
    }
    
    /**
     * Loads all abilities from a directory.
     * 
     * @param directory The directory containing YAML files
     * @return Number of abilities loaded
     */
    public int loadAbilities(File directory) {
        if (!directory.exists()) {
            directory.mkdirs();
            logger.info("Created abilities directory: " + directory.getPath());
            return 0;
        }
        
        if (!directory.isDirectory()) {
            logger.warning("Abilities path is not a directory: " + directory.getPath());
            return 0;
        }
        
        int loaded = 0;
        File[] files = directory.listFiles((dir, name) -> name.endsWith(".yml") || name.endsWith(".yaml"));
        
        if (files == null || files.length == 0) {
            logger.info("No ability configuration files found");
            return 0;
        }
        
        for (File file : files) {
            try {
                loaded += loadAbilitiesFromFile(file);
            } catch (Exception e) {
                logger.severe("Error loading abilities from " + file.getName() + ": " + e.getMessage());
            }
        }
        
        return loaded;
    }
    
    @SuppressWarnings("unchecked")
    private int loadAbilitiesFromFile(File file) throws IOException {
        Yaml yaml = new Yaml();
        Map<String, Object> data;
        
        try (FileInputStream fis = new FileInputStream(file)) {
            data = yaml.load(fis);
        }
        
        if (data == null || !data.containsKey("abilities")) {
            logger.warning("No 'abilities' section found in " + file.getName());
            return 0;
        }
        
        Map<String, Map<String, Object>> abilitiesData = (Map<String, Map<String, Object>>) data.get("abilities");
        int loaded = 0;
        
        for (Map.Entry<String, Map<String, Object>> entry : abilitiesData.entrySet()) {
            String abilityId = entry.getKey();
            Map<String, Object> abilityData = entry.getValue();
            
            try {
                ConfigAbility ability = parseAbility(abilityId, abilityData);
                registry.register(ability);
                loaded++;
                logger.info("Loaded ability: " + abilityId);
            } catch (Exception e) {
                logger.warning("Failed to parse ability " + abilityId + ": " + e.getMessage());
            }
        }
        
        return loaded;
    }
    
    @SuppressWarnings("unchecked")
    private ConfigAbility parseAbility(String id, Map<String, Object> data) {
        String displayName = (String) data.getOrDefault("display-name", id);
        
        // Parse triggers
        List<String> triggerNames = (List<String>) data.getOrDefault("triggers", Collections.singletonList("RIGHT_CLICK"));
        Collection<TriggerType> triggers = new ArrayList<>();
        for (String triggerName : triggerNames) {
            try {
                triggers.add(TriggerType.valueOf(triggerName.toUpperCase()));
            } catch (IllegalArgumentException e) {
                logger.warning("Invalid trigger type: " + triggerName);
            }
        }
        
        // Parse conditions
        List<Map<String, Object>> conditionsData = (List<Map<String, Object>>) data.getOrDefault("conditions", Collections.emptyList());
        List<Condition> conditions = parseConditions(conditionsData);
        
        // Parse cooldown
        String cooldownStr = (String) data.getOrDefault("cooldown", "0s");
        Duration cooldown = parseDuration(cooldownStr);
        
        // Parse actions
        List<Map<String, Object>> actionsData = (List<Map<String, Object>>) data.getOrDefault("actions", Collections.emptyList());
        List<ConfigAbility.ConfigAction> actions = parseActions(actionsData);
        
        return new ConfigAbility(id, displayName, triggers, conditions, cooldown, actions);
    }
    
    @SuppressWarnings("unchecked")
    private List<Condition> parseConditions(List<Map<String, Object>> conditionsData) {
        List<Condition> conditions = new ArrayList<>();
        
        for (Map<String, Object> conditionData : conditionsData) {
            if (conditionData.containsKey("sneaking")) {
                boolean sneaking = (Boolean) conditionData.get("sneaking");
                conditions.add(sneaking ? Conditions.sneaking() : Conditions.notSneaking());
            }
            
            if (conditionData.containsKey("health-above")) {
                double threshold = ((Number) conditionData.get("health-above")).doubleValue();
                conditions.add(Conditions.healthAbove(threshold));
            }
            
            if (conditionData.containsKey("health-below")) {
                double threshold = ((Number) conditionData.get("health-below")).doubleValue();
                conditions.add(Conditions.healthBelow(threshold));
            }
            
            if (conditionData.containsKey("y-above")) {
                double y = ((Number) conditionData.get("y-above")).doubleValue();
                conditions.add(Conditions.yAbove(y));
            }
            
            if (conditionData.containsKey("y-below")) {
                double y = ((Number) conditionData.get("y-below")).doubleValue();
                conditions.add(Conditions.yBelow(y));
            }
            
            if (conditionData.containsKey("has-target")) {
                boolean hasTarget = (Boolean) conditionData.get("has-target");
                if (hasTarget) {
                    conditions.add(Conditions.hasTarget());
                }
            }
        }
        
        return conditions;
    }
    
    private List<ConfigAbility.ConfigAction> parseActions(List<Map<String, Object>> actionsData) {
        List<ConfigAbility.ConfigAction> actions = new ArrayList<>();
        
        for (Map<String, Object> actionData : actionsData) {
            String typeStr = (String) actionData.get("type");
            if (typeStr == null) {
                continue;
            }
            
            try {
                ActionType type = ActionType.valueOf(typeStr.toUpperCase());
                Map<String, Object> params = new HashMap<>(actionData);
                params.remove("type");
                actions.add(new ConfigAbility.ConfigAction(type, params));
            } catch (IllegalArgumentException e) {
                logger.warning("Invalid action type: " + typeStr);
            }
        }
        
        return actions;
    }
    
    private Duration parseDuration(String str) {
        if (str == null || str.isBlank()) {
            return Duration.ZERO;
        }
        
        Matcher matcher = DURATION_PATTERN.matcher(str.toLowerCase());
        if (!matcher.matches()) {
            logger.warning("Invalid duration format: " + str);
            return Duration.ZERO;
        }
        
        long value = Long.parseLong(matcher.group(1));
        String unit = matcher.group(2);
        
        return switch (unit) {
            case "s" -> Duration.ofSeconds(value);
            case "m" -> Duration.ofMinutes(value);
            case "h" -> Duration.ofHours(value);
            case "d" -> Duration.ofDays(value);
            default -> Duration.ZERO;
        };
    }
}
