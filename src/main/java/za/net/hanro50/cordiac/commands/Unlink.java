package za.net.hanro50.cordiac.commands;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import za.net.hanro50.cordiac.App;
import za.net.hanro50.cordiac.Config;

public class Unlink extends Base {

    public Unlink(App plugin) {
        super(plugin, "unlink");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        return new ArrayList<String>();
    }

    @Override
    public void onCommand(Player sender, Command command, String label, String[] args) {
        this.plugin.discord.unLink(sender.getUniqueId());
        sender.sendMessage("Unlinked account!");
        if (Config.forceLink()) {
            plugin.getServer().broadcastMessage(sender.getDisplayName() + " just won a darwin award!");
            sender.kickPlayer("This server requires you to link your discord account!\nDM the bot this code to rejoin ["
                    + this.plugin.discord.requestLink(sender.getUniqueId()) + "]");
        }
    }

}
