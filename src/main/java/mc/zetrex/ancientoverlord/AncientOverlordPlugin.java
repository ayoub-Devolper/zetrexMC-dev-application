package mc.zetrex.ancientoverlord;

import io.lumine.mythic.api.MythicProvider;
import io.lumine.mythic.api.mobs.MythicMob;
import io.lumine.mythic.bukkit.BukkitAdapter;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.mobs.ActiveMob;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collection;
import java.util.Optional;

/**
 * AncientOverlordPlugin - Main plugin class for the Ancient Overlord boss.
 *
 * Integrates with MythicMobs API to provide:
 *  - /ao spawn  : Spawn Ancient Overlord at the player's location
 *  - /ao kill   : Kill all active Ancient Overlord instances
 *  - /ao reload : Reload MythicMobs configuration
 */
public class AncientOverlordPlugin extends JavaPlugin implements CommandExecutor {

    /** The internal MythicMobs mob name defined in AncientOverlord.yml */
    private static final String MOB_NAME = "AncientOverlord";

    // ââââââââââââââââââââââââââââââââââââââââââââââ
    //  Lifecycle
    // ââââââââââââââââââââââââââââââââââââââââââââââ

    @Override
    public void onEnable() {
        // Verify MythicMobs is present and loaded
        if (!isMythicMobsLoaded()) {
            getLogger().severe("MythicMobs not found! Disabling AncientOverlord...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Register command executor
        getCommand("ancientoverlord").setExecutor(this);

        getLogger().info("AncientOverlord plugin enabled. Boss '" + MOB_NAME + "' is ready.");
    }

    @Override
    public void onDisable() {
        getLogger().info("AncientOverlord plugin disabled.");
    }

    // ââââââââââââââââââââââââââââââââââââââââââââââ
    //  Command Handler
    // ââââââââââââââââââââââââââââââââââââââââââââââ

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("ancientoverlord.admin")) {
            sender.sendMessage("Â§cYou don't have permission to use this command.");
            return true;
        }

        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "spawn" -> handleSpawn(sender);
            case "kill"  -> handleKill(sender);
            case "reload" -> handleReload(sender);
            default      -> sendUsage(sender);
        }

        return true;
    }

    // ââââââââââââââââââââââââââââââââââââââââââââââ
    //  Sub-command Implementations
    // ââââââââââââââââââââââââââââââââââââââââââââââ

    /**
     * Spawns Ancient Overlord at the executing player's current location.
     * Validates that the mob is registered in MythicMobs before spawning.
     */
    private void handleSpawn(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Â§cOnly players can use this command.");
            return;
        }

        Optional<MythicMob> mobType = MythicBukkit.inst().getMobManager().getMythicMob(MOB_NAME);

        if (mobType.isEmpty()) {
            player.sendMessage("Â§cMob '" + MOB_NAME + "' not found! Check your MythicMobs config.");
            getLogger().warning("Mob '" + MOB_NAME + "' not registered in MythicMobs.");
            return;
        }

        Location spawnLoc = player.getLocation();

        // Use MythicMobs API to spawn the mob at the player's feet
        ActiveMob activeMob = mobType.get().spawn(BukkitAdapter.adapt(spawnLoc), 1);

        player.sendMessage("Â§5Â§lð Â§dAncient Overlord has been summoned at your location!");
        getLogger().info("Spawned " + MOB_NAME + " at " + formatLocation(spawnLoc)
                + " (entity UUID: " + activeMob.getUniqueId() + ")");
    }

    /**
     * Kills all currently active Ancient Overlord instances on the server.
     */
    private void handleKill(CommandSender sender) {
        Collection<ActiveMob> activeMobs = MythicBukkit.inst()
                .getMobManager()
                .getActiveMobs();

        long killed = activeMobs.stream()
                .filter(mob -> mob.getMobType().getInternalName().equals(MOB_NAME))
                .peek(mob -> mob.getEntity().getBukkitEntity().remove())
                .count();

        if (killed == 0) {
            sender.sendMessage("Â§eNo active Ancient Overlord instances found.");
        } else {
            sender.sendMessage("Â§aÂ§lRemoved Â§e" + killed + " Â§aÂ§lAncient Overlord instance(s).");
            getLogger().info("Killed " + killed + " instance(s) of " + MOB_NAME + " via command.");
        }
    }

    /**
     * Reloads all MythicMobs configuration files (including AncientOverlord.yml).
     */
    private void handleReload(CommandSender sender) {
        MythicProvider.get().reload();
        sender.sendMessage("Â§aÂ§lâ Â§aMythicMobs configuration reloaded successfully.");
        getLogger().info("MythicMobs config reloaded by " + sender.getName());
    }

    // ââââââââââââââââââââââââââââââââââââââââââââââ
    //  Helpers
    // ââââââââââââââââââââââââââââââââââââââââââââââ

    private boolean isMythicMobsLoaded() {
        return getServer().getPluginManager().getPlugin("MythicMobs") != null;
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage("Â§5Â§lAncient Overlord Â§7- Commands:");
        sender.sendMessage("Â§7/ao spawn  Â§8- Â§dSpawn the boss at your location");
        sender.sendMessage("Â§7/ao kill   Â§8- Â§dKill all active boss instances");
        sender.sendMessage("Â§7/ao reload Â§8- Â§dReload MythicMobs configuration");
    }

    private String formatLocation(Location loc) {
        return String.format("world=%s x=%.1f y=%.1f z=%.1f",
                loc.getWorld() != null ? loc.getWorld().getName() : "unknown",
                loc.getX(), loc.getY(), loc.getZ());
    }
}
