package za.net.hanro50.cordiac.commands;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import za.net.hanro50.cordiac.App;

public class Link extends Base {

    public Link(App plugin) {
        super(plugin, "link");
        // TODO Auto-generated constructor stub
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        return new ArrayList<String>();
    }

    @Override
    public void onCommand(Player player, Command command, String label, String[] args) {
        String numCode = this.plugin.discord.requestLink(player.getUniqueId());
        final TextComponent code = new TextComponent(
                "DM the bot this code [" + numCode
                        + "]\nClick this message to copy it! (It won't give a popup)");
        code.setClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, numCode));

        final TextComponent mess = new TextComponent("\n\nThen click me to message the bot directly");
        mess.setColor(ChatColor.BLUE);
        mess.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL,
                "https://discord.com/users/" + this.plugin.discord.getBotUserID()));
        player.spigot().sendMessage(new TextComponent(code, mess));
    }

}
