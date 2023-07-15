package za.net.hanro50.cordiac.players;

import java.util.UUID;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;

public class DiscordPlayer extends BasePlayer {
    public String avatar;
    public String link;
    public final Long id;
    public final boolean isLinked;
    public final boolean isMember;

    public DiscordPlayer(BasePlayer player) {
        super(player.name, player.uuid);
        this.avatar = "https://mc-heads.net/avatar/" + uuid.toString();
        this.isLinked = false;
        this.isMember = false;
        this.link = null;
        this.id = null;
    }

    public DiscordPlayer(User user, UUID uuid) {
        super(user.getEffectiveName(), uuid);
        this.avatar = user.getEffectiveAvatarUrl();
        this.isLinked = true;
        this.isMember = false;
        this.link = "https://discord.com/users/" + user.getIdLong();
        this.id = user.getIdLong();
    }

    public DiscordPlayer(Member member, UUID uuid) {
        super(member.getEffectiveName(), uuid);
        this.avatar = member.getEffectiveAvatarUrl();
        this.isLinked = true;
        this.isMember = true;
        this.link = "https://discord.com/users/" + member.getIdLong();
        this.id = member.getIdLong();
    }

}
