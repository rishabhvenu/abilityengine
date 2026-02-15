// Example: Event Listeners
// Demonstrates raw Bukkit event listening from scripts

// Listen to player join events
engine.listen("PlayerJoinEvent", function(event) {
  var player = event.getPlayer();
  player.sendMessage("§aWelcome! AbilityEngine scripts are active.");
  engine.log(player.getName() + " joined the server");
});

// Listen to player death events
engine.listen("PlayerDeathEvent", function(event) {
  var player = event.getEntity();
  
  // Check if they have any active abilities
  var activeSessions = engine.sessions.getActive(player);
  
  if (activeSessions.length > 0) {
    event.setDeathMessage(player.getName() + " died while using abilities!");
  }
});

// Scheduled repeating task example
engine.scheduleRepeating(function() {
  const Bukkit = Java.type("org.bukkit.Bukkit");
  var onlinePlayers = Bukkit.getOnlinePlayers().size();
  
  if (onlinePlayers > 0) {
    engine.log("Players online: " + onlinePlayers);
  }
}, 20 * 60, 20 * 60); // Every 60 seconds (delay, period in ticks)

engine.log("Loaded event-listeners.js");
