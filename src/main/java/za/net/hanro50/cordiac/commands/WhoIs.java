package za.net.hanro50.cordiac.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import za.net.hanro50.cordiac.App;
import za.net.hanro50.cordiac.players.BasePlayer;

public class WhoIs extends Base {

    public WhoIs(App plugin) {
        super(plugin, "whois");
        // TODO Auto-generated constructor stub
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> result = new ArrayList<>();
        if (args.length > 1)
            return result;
        App app = App.getInstance();
        app.discord.getLinks().forEach((user, player) -> {
            OfflinePlayer ofPlayer = app.getServer().getOfflinePlayer(user);
            if (ofPlayer != null) {
                String name = ofPlayer.getName();
                if (name != null) {
                    result.add(name);
                }
            }
        });
        app.getServer().getOnlinePlayers().forEach((player) -> {
            if (!result.contains(player.getName())) {
                result.add(player.getName());
            }
        });

        return result;
    }

    @Override
    public void onCommand(Player sender, Command command, String label, String[] args) {
        if (args.length < 1) {
            sender.sendMessage("Please provide the name of the player you wish to ask about!");
            return;
        }
        UUID uuid = this.plugin.getServer().getOfflinePlayer(args[0]).getUniqueId();

        if (uuid == null) {
            sender.sendMessage("Failed to look up player");
            return;
        }
        App app = App.getInstance();
        if (!app.discord.getLinks().containsKey(uuid)) {
            sender.sendMessage("This player appears to be unlinked!");
            return;
        }

        app.discord.getName(new BasePlayer(args[0], uuid), (dPlayer -> {
            try {
                TextComponent mm = new TextComponent();
                if (dPlayer.isMember)
                    mm.setText("Server nickname: " + dPlayer.name + "\nDiscord ID:" + dPlayer.id);
                else {
                    mm.setText("Discord Username: " + dPlayer.name + "\nDiscord ID:" + dPlayer.id);
                }

                if (dPlayer.avatar != null) {
                    TextComponent av = new TextComponent("\nAvatar: <Link>");
                    av.setColor(ChatColor.AQUA);
                    av.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, dPlayer.avatar));
                    mm.addExtra(av);
                }
                sender.spigot().sendMessage(mm);
            } catch (Throwable e) {
                e.printStackTrace();
            }

        }));

    }

}
