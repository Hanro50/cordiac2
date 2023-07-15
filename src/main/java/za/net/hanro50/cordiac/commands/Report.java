package za.net.hanro50.cordiac.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.naming.ConfigurationException;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import za.net.hanro50.cordiac.App;
import za.net.hanro50.cordiac.Config;

public class Report extends Base {

    public Report(App plugin) {
        super(plugin, "report");
        // TODO Auto-generated constructor stub
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> result = new ArrayList<>();
        App.log.info(args.length + "");
        if (args.length <= 1) {
            result.add("player");
            result.add("incident");
        } else if (args.length == 2) {
            if (args[0].equals("player")) {
                plugin.getServer().getOnlinePlayers().forEach(players -> {
                    result.add(players.getDisplayName());
                });
            } else if (args[0].equals("incident")) {
                result.addAll(Config.reports());
            }
        }
        for (String string : args) {
            App.log.info(string);
        }
        return result;
    }

    @Override
    public void onCommand(Player sender, Command command, String label, String[] args) {
        if (args.length < 1) {
            sender.sendMessage("Please provide more information.");
            return;
        }
        String location = "XYZ: " + sender.getLocation().getX() + ", " + sender.getLocation().getY() + ", "
                + sender.getLocation().getZ();

        UUID uuid = null;
        if (args[0].equals("player")) {
            uuid = this.plugin.getServer().getOfflinePlayer(args[1]).getUniqueId();
        }
        UUID fuuid = uuid;
        plugin.getPlayer(sender.getUniqueId(), (player) -> {
            if (fuuid != null) {
                plugin.getPlayer(fuuid, (reported) -> {
                    try {
                        plugin.discord.report(player, location, reported, args);
                        sender.sendMessage("Reported player: " + reported.name
                                + ((player.uuid != null) ? ("\nID:" + reported.uuid.toString()) : ""));
                    } catch (ConfigurationException e) {
                        sender.sendMessage("It seems this server has yet to configure this feature!");
                    }
                });
            } else {
                try {
                    plugin.discord.report(player, location, null, args);
                    sender.sendMessage("Sent in report to admin!");
                } catch (ConfigurationException e) {
                    sender.sendMessage("It seems this server has yet to configure this feature!");
                }
            }

        });

    }

}
