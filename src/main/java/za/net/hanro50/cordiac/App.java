package za.net.hanro50.cordiac;

import java.io.File;

import java.util.UUID;
import java.util.function.Consumer;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;

import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.projectiles.ProjectileSource;

import za.net.hanro50.cordiac.commands.Link;
import za.net.hanro50.cordiac.commands.Report;
import za.net.hanro50.cordiac.commands.Unlink;
import za.net.hanro50.cordiac.lang.Mappings;
import za.net.hanro50.cordiac.players.BasePlayer;

/**
 * Hello world!
 *
 */
public class App extends JavaPlugin implements Listener {

  public static File configDir;
  public static Logger log;
  public static App instance;
  public Server discord;

  public void onEnable() {
    instance = this;
    log = getLogger();
    log.info("onEnable is called!");
    configDir = getDataFolder();
    configDir.mkdir();
    saveDefaultConfig();
    Config.config = getConfig();
    discord = new Server(this);
    this.getServer().getPluginManager().registerEvents(this, this);

    new Link(this);
    new Unlink(this);
    new Report(this);
  }

  @Override
  public void onDisable() {

    log.info("onDisable is called!");
    try {
      discord.shutdown();
    } catch (InterruptedException e) {
      log.severe("Failed to terminate JDA.\nThis is bad...");
      e.printStackTrace();
    }
  }

  public String getMinecraftVersion() {
    String version = getServer().getVersion();
    version = version.substring(version.lastIndexOf("1."));
    version = version.substring(0, version.lastIndexOf("."));
    return version;
  }

  public String getUserName(UUID uuid) {
    OfflinePlayer plr = Bukkit.getOfflinePlayer(uuid);
    if (plr.isOnline()) {
      String name = Bukkit.getPlayer(uuid).getDisplayName();
      if (name != null)
        return name;
    }
    return plr.getName();
  }

  public void getPlayer(UUID uuid, Consumer<BasePlayer> player) {
    OfflinePlayer offlinePlayer = getServer().getOfflinePlayer(uuid);
    offlinePlayer.getPlayerProfile().update()
        .thenAcceptAsync((PlayerProfile profile) -> player.accept(new BasePlayer(profile.getName(), uuid)));

  }

  void sendMessage(String name, String message) {
    String chat = Config.chatFormat().replace("%username%", name);
    chat = chat.replace("%message%", message);
    getServer().broadcastMessage(chat);
  }

  public void sendMessage(UUID user, String message) {
    getPlayer(user, (BasePlayer player) -> sendMessage(player.name, message));
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  void onPlayerChat(AsyncPlayerChatEvent e) {
    getPlayer(e.getPlayer().getUniqueId(), (BasePlayer player) -> discord.sendMessage(player,
        e.getMessage()));

  }

  @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
  void onPlayerJoin(PlayerJoinEvent e) {
    Player sender = e.getPlayer();
    if (Config.forceLink() && !discord.isLinked(sender.getUniqueId())) {
      
      sender.kickPlayer("This server requires you to link your discord account!\nDM the bot this code to rejoin ["
          + this.discord.requestLink(sender.getUniqueId()) + "]");
      return;
    }
    getPlayer(e.getPlayer().getUniqueId(), (BasePlayer player) -> discord.sendJoin(player));

  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  void onPlayerQuit(PlayerQuitEvent e) {
    getPlayer(e.getPlayer().getUniqueId(), (BasePlayer player) -> discord.sendLeave(player));
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  void onPlayerAdvancementDone(PlayerAdvancementDoneEvent e) {
    if (e.getAdvancement().getKey().getKey().split("/").length < 1
        || e.getAdvancement().getKey().getKey().split("/")[0].equalsIgnoreCase("recipes")) {
      return;
    }
    getPlayer(e.getPlayer().getUniqueId(), (BasePlayer player) -> discord.sendAdvancement(player,
        "advancements." + e.getAdvancement().getKey().getKey().replace("/", ".")));
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  void onPlayerDeath(PlayerDeathEvent e) {
    getPlayer(e.getEntity().getUniqueId(), (BasePlayer player) -> {
      EntityDamageEvent cause = e.getEntity().getLastDamageCause();
      boolean isCustomized = false;
      if (cause instanceof EntityDamageByEntityEvent) {
        EntityDamageByEntityEvent event = (EntityDamageByEntityEvent) cause;
        String weapon = null;
        Entity damager = event.getDamager();
        String nameSpace = Mappings.DMTranslate(event.getCause());
        String mob = null;
        if (damager instanceof TNTPrimed) {
          damager = ((TNTPrimed) damager).getSource();
        } else if (damager instanceof Projectile) {
          ProjectileSource shooter = ((Projectile) damager).getShooter();
          if (shooter instanceof Entity) {
            damager = (Entity) shooter;
          }
        } else if (damager instanceof FallingBlock) {
          // death.attack.anvil
          FallingBlock block = (FallingBlock) damager;

          if (block.getBlockData().getMaterial() == Material.ANVIL) {
            nameSpace = "death.attack.anvil";
          }

          damager = null;
        }

        if (damager != null) {
          if (damager.getType().toString().equals(damager.getType().toString().toUpperCase()))
            mob = "entity.minecraft." + damager.getType().toString().toLowerCase();
          else
            mob = "entity.minecraft." + damager.getType().toString().toLowerCase();
          if (damager.getCustomName() != null) {
            mob = damager.getCustomName();
            isCustomized = true;
          }
          if (damager instanceof LivingEntity) {
            ItemMeta Meta = null;
            EntityEquipment inv = ((LivingEntity) damager).getEquipment();

            Meta = inv.getItemInMainHand().getItemMeta();

            // }
            try {
              if (Meta.hasDisplayName()) {
                weapon = Meta.getDisplayName();
              }
            } catch (NullPointerException err) {
            }
          }

          if (damager instanceof Player) {
            final String fweapon = weapon;
            final String fnameSpace = nameSpace;
            final boolean fisCustomized = isCustomized;
            getPlayer(damager.getUniqueId(), (BasePlayer aPlayer) -> {
              discord.sendDeath(player, aPlayer, null, fweapon, fnameSpace, fisCustomized);
            });
            return;
          }
        }
        discord.sendDeath(player, null, mob, weapon, nameSpace, isCustomized);
      } else {
        final boolean fisCustomized = isCustomized;
        if (e.getEntity().getKiller() != null)
          getPlayer(e.getEntity().getKiller().getUniqueId(),
              (BasePlayer aPlayer) -> discord.sendDeath(player, aPlayer, null, null,
                  Mappings.DMTranslate(e.getEntity().getLastDamageCause().getCause()),
                  fisCustomized));
        else
          discord.sendDeath(player, null, null, null,
              Mappings.DMTranslate(e.getEntity().getLastDamageCause().getCause()), isCustomized);

      }
    });
  }

  public static App getInstance() {
    return instance;
  }
}
