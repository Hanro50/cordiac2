package za.net.hanro50.cordiac.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import za.net.hanro50.cordiac.App;

public abstract class Base implements CommandExecutor, TabCompleter {
    protected final App plugin;
    protected final String name;
    protected final PluginCommand command;

    public Base(App plugin, String name) {
        this.plugin = plugin;
        this.name = name;
        this.command = plugin.getCommand(name);

        this.command.setExecutor(this);
        this.command.setTabCompleter(this);
    
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!plugin.discord.isRunning()) {
            sender.sendMessage("The discord bot is not running :(");
            return true;
        }
        if (sender instanceof Player) {
            this.onCommand((Player) sender, command, label, args);

        } else {
            sender.sendMessage("Only a player can run this command!");
        }
        // If the player (or console) uses our command correct, we can return true
        return true;
    }

    public abstract void onCommand(Player sender, Command command, String label, String[] args);
}
