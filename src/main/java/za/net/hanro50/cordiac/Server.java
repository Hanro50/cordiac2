package za.net.hanro50.cordiac;

import java.awt.Color;

import java.io.File;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import javax.annotation.Nonnull;
import javax.naming.ConfigurationException;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import club.minnced.discord.webhook.external.JDAWebhookClient;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.JDA.Status;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.ApplicationInfo;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.Webhook;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.unions.DefaultGuildChannelUnion;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import za.net.hanro50.cordiac.lang.Parser;
import za.net.hanro50.cordiac.players.BasePlayer;
import za.net.hanro50.cordiac.players.DiscordPlayer;

public class Server extends ListenerAdapter {
  JDA jda;
  final private App app;
  private SecureRandom random = new SecureRandom();
  private Data data;
  private BiMap<UUID, Long> userLink;
  private Cache<String, UUID> pendingLink = CacheBuilder.newBuilder().expireAfterWrite(30, TimeUnit.MINUTES).build();
  private ApplicationInfo info;
  private JDAWebhookClient client;
  private Parser parser;

  Guild tmpGuild;
  TextChannel tmpChannel;
  TextChannel tmpLogChannel;


  public Map<UUID, Long> getLinks() {
    return new HashMap<>(userLink);
  }

  public boolean isLinked(UUID uuid) {
    return userLink.containsKey(uuid);
  }

  Guild getGuild() throws ConfigurationException {
    if (data.Server == null)
      throw new ConfigurationException("Config not set");
    if (tmpGuild == null || tmpGuild.getIdLong() != data.Server) {
      tmpGuild = jda.getGuildById(data.Server);

    }
    return tmpGuild;
  }

  TextChannel getChannel() throws ConfigurationException {
    if (data.linkedChannel == null)
      throw new ConfigurationException("Config not set");
    if (tmpChannel == null || tmpChannel.getIdLong() != data.linkedChannel) {
      tmpChannel = getGuild().getTextChannelById(data.linkedChannel);

    }
    return tmpChannel;
  }

  TextChannel getLogChannel() throws ConfigurationException {
    if (data.logChannel == null)
      throw new ConfigurationException("Config not set");
    if (tmpLogChannel == null || tmpLogChannel.getIdLong() != data.logChannel) {
      tmpLogChannel = getGuild().getTextChannelById(data.logChannel);

    }
    return tmpLogChannel;
  }

  public String getBotUserID() {
    return jda.getSelfUser().getId();
  }

  public boolean isRunning() {
    return (jda != null) && jda.getStatus() == Status.CONNECTED;
  }

  public String requestLink(UUID player) {
    String code;
    do {
      code = String.format("%06d", random.nextInt(999999));

    } while (pendingLink.getIfPresent(code) != null);
    pendingLink.put(code, player);
    return code;

  }

  public void unLink(UUID player) {
    if (userLink.remove(player) != null) {
      data.linkedPlayers = userLink;
      try {
        data.Save();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  void sendMessage(JDAWebhookClient client, DiscordPlayer player, String message) {
    try {
      WebhookMessageBuilder builder = new WebhookMessageBuilder();
      builder.setUsername(player.name);
      builder.setAvatarUrl(player.avatar);
      builder.setContent(message);
      try {
        client.send(builder.build()).get();
      } catch (InterruptedException | ExecutionException e) {
        e.printStackTrace();
      }
    } catch (RuntimeException e) {
      App.log.throwing(this.getClass().getName(), "sendMsg", e);
    }
  }

  void sendMessage(Webhook hook, DiscordPlayer player, String message) {
    try {
      client = JDAWebhookClient.from(hook);
      sendMessage(client, player, message);
    } catch (RuntimeException e) {
      App.log.throwing(this.getClass().getName(), "sendMsg", e);
    }
  }

  void sendMessage(DiscordPlayer player, String message) {
    if (client != null && !client.isShutdown()) {
      sendMessage(client, player, message);
      return;
    }
    try {
      getChannel().retrieveWebhooks().onSuccess((List<Webhook> hooks) -> {
        for (Webhook webhook : hooks) {
          User user = webhook.getOwnerAsUser();
          if (user != null && user.getIdLong() == jda.getSelfUser().getIdLong()) {
            sendMessage(webhook, player, message);
            return;
          }
        }
        try {
          getChannel().createWebhook("Minecraft").onSuccess((Webhook hook) -> sendMessage(hook, player, message))
              .submit();
        } catch (ConfigurationException e) {
          e.printStackTrace();
        }

      }).submit();
    } catch (ConfigurationException e) {
      // e.printStackTrace();
    }
  }

  public void getName(BasePlayer player, Consumer<DiscordPlayer> dPlayer) {
    Long memberLong = userLink.get(player.uuid);
    if (memberLong != null) {
      Guild guild = jda.getGuildById(data.Server);
      if (guild == null)
        return;
      guild.retrieveMemberById(memberLong)
          .onSuccess((Member member) -> dPlayer.accept(new DiscordPlayer(member, player.uuid)))
          .onErrorMap((err) -> {
            jda.retrieveUserById(memberLong)
                .onSuccess((user) -> dPlayer.accept(new DiscordPlayer(user, player.uuid)))
                .onErrorMap((err2) -> {
                  dPlayer.accept(new DiscordPlayer(player));
                  return null;
                })
                .submit();
            return null;
          })
          .submit();
    } else {
      dPlayer.accept(new DiscordPlayer(player));
    }
  }

  public void sendMessage(BasePlayer player, String message) {
    if (data.Server == null)
      return;
    getName(player, (dPlayer) -> sendMessage(dPlayer, message));
  }

  public Color getColour(String namespace) {
    if (namespace.startsWith("advancements.adventure"))
      return new Color(254, 109, 0);
    if (namespace.startsWith("advancements.empty"))
      return Color.black;
    if (namespace.startsWith("advancements.end"))
      return new Color(144, 0, 117);
    if (namespace.startsWith("advancements.husbandry"))
      return new Color(164, 78, 43);
    if (namespace.startsWith("advancements.nether"))
      return new Color(254, 59, 0);
    if (namespace.startsWith("advancements.story"))
      return new Color(0, 94, 156);
    if (namespace.startsWith("advancements.sad_label"))
      return Color.pink;
    return Color.white;

  }

  void sendAdvancement(DiscordPlayer player, String namespace) {
    String name = namespace + ".title";
    String disc = namespace + ".description";
    if (!Config.showAdvancementMessages() || !parser.has(name))
      return;

    EmbedBuilder emb = new EmbedBuilder();
    emb.setAuthor(player.name, player.link,
        player.avatar);
    emb.setTitle(parser.parse(name));
    if (parser.has(disc))
      emb.setDescription(parser.parse(disc));
    emb.setColor(getColour(namespace));
    try {
      getChannel().sendMessageEmbeds(emb.build()).submit();
    } catch (ConfigurationException e) {
    }
  }

  public void sendAdvancement(BasePlayer player, String namespace) {
    getName(player, (dPlayer) -> sendAdvancement(dPlayer, namespace));
  }

  void sendDeath(DiscordPlayer Victem, DiscordPlayer Attacker, String MobName, String Weapon, String namespace,
      boolean isCustomized) {
    if (!Config.showDeathMessages())
      return;

    String cause = MobName, item = Weapon;
    boolean isPlayer = Attacker != null;

    EmbedBuilder emb = new EmbedBuilder();

    if (isPlayer && Attacker != null) {
      cause = Attacker.name;
    }
    boolean hasPlayer = parser.has(namespace + ".player");

    if (cause != null) {
      if (!isPlayer && !isCustomized) {
        if (parser.has(cause.toLowerCase())) {
          cause = parser.parse(cause.toLowerCase());
          if (hasPlayer)
            namespace += ".player";
        } else if (parser.has(cause.toLowerCase() + ".name")) {
          cause = parser.parse(cause.toLowerCase() + ".name");
          if (hasPlayer)
            namespace += ".player";
        } else if (!hasPlayer) {
          cause = null;
        }
      } else if (hasPlayer) {
        namespace += ".player";
      }
    }
    if (item != null && cause != null && parser.has(namespace + ".item")) {
      namespace += ".item";
    }
    emb.setAuthor(parser.parse(namespace, Victem.name, cause, item), Victem.link,
        Victem.avatar);

    emb.setColor(new Color(254, 35, 0));
    try {
      getChannel().sendMessageEmbeds(emb.build()).submit();
    } catch (ConfigurationException e) {
    }
  }

  public void sendDeath(BasePlayer Victem, BasePlayer Attacker, String MobName, String Weapon, String namespace,
      boolean isCustomized) {
    getName(Victem, (dVictem) -> {
      if (Attacker != null)
        getName(Attacker, (dAttacker) -> sendDeath(dVictem, dAttacker, MobName, Weapon, namespace, isCustomized));
      else
        sendDeath(dVictem, null, MobName, Weapon, namespace, isCustomized);
    });

  }

  void sendJoin(DiscordPlayer player) {
    if (!Config.showJoinMessages())
      return;
    EmbedBuilder emb = new EmbedBuilder();
    emb.setAuthor(parser.parse("multiplayer.player.joined", player.name), player.link,
        player.avatar);
    emb.setColor(Color.green);
    try {
      getChannel().sendMessageEmbeds(emb.build()).submit();
    } catch (ConfigurationException e) {
    }

  }

  public void sendJoin(BasePlayer player) {
    getName(player, (dPlayer) -> sendJoin(dPlayer));
  }

  void sendLeave(DiscordPlayer player) {
    if (!Config.showJoinMessages())
      return;
    EmbedBuilder emb = new EmbedBuilder();
    emb.setAuthor(parser.parse("multiplayer.player.left", player.name), player.link,
        player.avatar);
    emb.setColor(Color.red);
    try {
      getChannel().sendMessageEmbeds(emb.build()).submit();
    } catch (ConfigurationException e) {
    }
  }

  public void sendLeave(BasePlayer player) {
    getName(player, (dPlayer) -> sendLeave(dPlayer));
  }

  Server(App app) {
    this.app = app;
    data = new Data(new File(App.configDir, "data.json"));
    userLink = HashBiMap.create(data.linkedPlayers);
    String token = Config.getToken();
    if (token.equals("Token here")) {

      App.log.info("Please add a discord bot token and reload the plugin!");
      return;
    }
    jda = JDABuilder.createDefault(token)
        .disableCache(CacheFlag.MEMBER_OVERRIDES, CacheFlag.VOICE_STATE, CacheFlag.EMOJI, CacheFlag.STICKER,
            CacheFlag.STICKER, CacheFlag.SCHEDULED_EVENTS)
        .setEnabledIntents(GatewayIntent.GUILD_MEMBERS, GatewayIntent.MESSAGE_CONTENT,
            GatewayIntent.DIRECT_MESSAGES, GatewayIntent.GUILD_WEBHOOKS, GatewayIntent.GUILD_MESSAGES,
            GatewayIntent.GUILD_INVITES)
        .setBulkDeleteSplittingEnabled(false)
        .addEventListeners(this)
        // Set activity (like "playing Something")
        .setActivity(Activity.watching("Minecraft")).build();

    if (data.Server == null) {
      App.log.info("No server connected yet!");
      App.log.info("Will auto-configure to first server the bot is invited to");
      App.log.info(jda.getInviteUrl(Permission.MESSAGE_EMBED_LINKS,
          Permission.MANAGE_WEBHOOKS, Permission.MESSAGE_HISTORY, Permission.VIEW_CHANNEL,
          Permission.MESSAGE_SEND));
    }
    jda.retrieveApplicationInfo().submit().thenAcceptAsync(info -> {
      this.info = info;
    });
    try {
      parser = new Parser();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  public void shutdown() throws InterruptedException {
    if (client != null)
      client.close();
    if (jda != null)
      jda.shutdown();
  }

  private void setGuild(Guild guild, TextChannel dchannel) {

    if (data.Server != null && guild.getIdLong() == data.Server)
      return;
    String txt = "Default server already configured.\nGet the bot owner to run !reset to link the bot to this server instead.";
    if (data.Server == null) {
      App.log.warning("New server, configuring as default!");

      data.Server = guild.getIdLong();
      data.linkedChannel = null;
      data.logChannel = null;
      txt = "Hello there!\n" +
          "We just need a little more configuration from you, the person who created the bot token!\n" +
          "Run `!link` in a channel you want to link to the server chat\n" +
          "Run `!log` in a channel you want to link to use as the log channel for user reports\n";
      if (client != null)
        client.close();
      try {
        data.Save();
      } catch (IOException e) {
        e.printStackTrace();
      }

    } else {
      App.log.warning("The bot was invited to another server?!");

    }
    App.log.warning("Server:" + guild.getName());
    App.log.warning("Server id:" + guild.getId());
    if (dchannel != null) {
      dchannel.sendMessage(txt).submit();
      return;
    }
    TextChannel update = guild.getCommunityUpdatesChannel();
    if (update != null) {
      update.sendMessage(txt).submit();
      return;
    }
    DefaultGuildChannelUnion channel = guild.getDefaultChannel();
    if (channel != null) {
      channel.asTextChannel().sendMessage(txt).submit();
      return;
    }
    App.log.warning("Could not find default channel");
    App.log.info(txt);
  }

  public void onGuildJoin(@Nonnull GuildJoinEvent event) {
    setGuild(event.getGuild(), null);
  }

  void report(DiscordPlayer reporter, BasePlayer reportee, String location, String description, String type,
      String incidentType, TextChannel channel) {
    EmbedBuilder emb = new EmbedBuilder();
    App.log.info("TEST 2");
    emb.setAuthor(reporter.name, reporter.link,
        reporter.avatar);

    emb.setTitle(
        "REPORTED TYPE: " + type + (incidentType != "" && incidentType != null ? " (" + incidentType + ")" : ""));
    if (location != null)
      emb.addField("Location:", location, false);
    if (reportee != null) {
      String content = "Player name: " + reportee.name + "\nMC UUID:" + reportee.uuid;
      if (reportee instanceof DiscordPlayer) {
        DiscordPlayer pl = (DiscordPlayer) reportee;
        content += pl.id != null ? "\nid: [" + pl.id + "]" : "";
        content += pl.link != null ? "\nDiscord Profile: [link](" + pl.link + ")" : "";
        content += "\nis Linked: " + (pl.isLinked ? "true" : "false");
        content += "\nis Member: " + (pl.isMember ? "true" : "false");
        emb.setThumbnail(pl.avatar);
      } else {
        content += "is Linked: false";
      }
      emb.addField("Player Info", content, false);
      emb.setColor(Color.MAGENTA);
    }
    if (description != null)
      emb.addField("Description", description, false);

    channel.sendMessageEmbeds(emb.build()).submit();

  }

  public void report(BasePlayer reporter, String location, BasePlayer reported, String args[])
      throws ConfigurationException {
    final TextChannel channel = getLogChannel();
    App.log.info("TEST 1");
    getName(reporter, splayer -> {
      String type = "Unknown";
      String incidentSubType = "";
      if (reported != null) {
        String body = "";
        for (int i = 2; i < args.length; i++) {
          body += " " + args[i];
        }
        final String fbody = body;
        getName(reported, rplayer -> report(splayer, rplayer, location, fbody, "player", null, channel));
        return;
      }
      String body = "";
      if (args[0].equals("incident")) {
        type = "incident";
        incidentSubType = args.length > 1 ? args[1] : "";
        for (int i = 2; i < args.length; i++) {
          body += " " + args[i];
        }
      } else {
        body = String.join(" ", args);
      }
      report(splayer, reported, location, body, type, incidentSubType, channel);
    });
  }

  public void onMessageReceived(@Nonnull MessageReceivedEvent event) {
    if (event.isWebhookMessage() || event.getAuthor().getIdLong() == jda.getSelfUser().getIdLong())
      return;
    String message = event.getMessage().getContentDisplay();
    if (message.startsWith(">")) {
      switch (message) {
        case (">online"):
          event.getChannel().sendMessage("There are " + app.getServer().getOnlinePlayers().size() + " players online!")
              .submit();
          return;
        case (">help"):
          event.getChannel().sendMessage("# User commands:\n- >online => Get the number of online players")
              .submit();
          return;
      }
    }

    if (message.startsWith("!") && info != null && event.getAuthor().getIdLong() == info.getOwner().getIdLong()) {
      try {
        switch (message) {
          case ("!link"):
            if (!event.isFromGuild())
              event.getChannel().sendMessage("Please run this command in a server!").submit();
            else if (data.Server == null || data.Server != event.getGuild().getIdLong())
              event.getChannel().sendMessage("Please run `!reset` to link this bot to this server!")
                  .submit();
            else {
              data.linkedChannel = event.getChannel().getIdLong();
              event.getChannel().sendMessage("This channel is now linked with the server chat").submit();
              data.Save();
              if (client != null)
                client.close();
            }
            return;
          case ("!log"):
            if (!event.isFromGuild())
              event.getChannel().sendMessage("Please run this command in a server!").submit();
            else if (data.Server == null || data.Server != event.getGuild().getIdLong())
              event.getChannel().sendMessage("Please run `!reset` to link this bot to this server!")
                  .submit();
            else {
              data.logChannel = event.getChannel().getIdLong();
              event.getChannel().sendMessage("This channel will now be used to log reports").submit();
              data.Save();
            }
            return;
          case ("!reset"):
            if (!event.isFromGuild())
              event.getChannel().sendMessage("Please run this command in a server!").submit();
            else if (data.Server != null && data.Server == event.getGuild().getIdLong())
              event.getChannel().sendMessage("This server is already the default").submit();
            else {
              data.Server = null;
              setGuild(event.getGuild(), event.getChannel().asTextChannel());
            }
            return;

          case ("!help"):
            event.getChannel().sendMessage(
                "# Admin commands:\n- !link => Link a channel to the server console.\n- !log=> Link a channel to use for the purposes of logging events.\n- !reset => Relink to a new discord server.")
                .submit();
            return;
        }
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }

    }
    Map<Long, UUID> users = userLink.inverse();
    if (!event.isFromGuild()) {
      String code = message.replaceAll("[^\\d.]", "");
      App.log.info(code);
      UUID player = pendingLink.getIfPresent(code);
      if (player != null) {

        pendingLink.invalidate(code);
        userLink.put(player, event.getAuthor().getIdLong());
        data.linkedPlayers.put(player, event.getAuthor().getIdLong());
        try {
          data.Save();
        } catch (IOException e) {
          event.getChannel().sendMessage("Could not save link, it may not presist past a server reboot!").submit();
        }
        event.getChannel().sendMessage("Saved link!").submit();
      } else {
        event.getChannel().sendMessage("That link code seems invalid?\nPerhaps it expired...").submit();
      }

      return;
    } else if (data.linkedChannel == null || data.Server == null
        || event.getChannel().getIdLong() != data.linkedChannel || event.getGuild().getIdLong() != data.Server)
      return;
    if (users.containsKey(event.getAuthor().getIdLong()))
      app.sendMessage(users.get(event.getAuthor().getIdLong()), message);
    else
      event.getGuild().retrieveMember(event.getAuthor()).onSuccess((Member member) -> {
        if (member != null)
          app.sendMessage(member.getEffectiveName(), message);
        else
          app.sendMessage(event.getAuthor().getEffectiveName(), message);
      }).submit();

  }

}
