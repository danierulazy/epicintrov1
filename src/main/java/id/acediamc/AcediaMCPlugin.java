package id.acediamc;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public final class AcediaMCPlugin extends JavaPlugin implements Listener {
    private SequenceManager manager;
    private final Map<String, NbsSong> songs = new HashMap<String, NbsSong>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        new File(getDataFolder(), "songs").mkdirs();
        manager = new SequenceManager(this);
        IntroCommand cmd = new IntroCommand(this);
        getCommand("acediamc").setExecutor(cmd);
        getCommand("acediamc").setTabCompleter(cmd);
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        if (manager != null) manager.stopAll();
    }

    public SequenceManager manager() {
        return manager;
    }

    public void reload() {
        reloadConfig();
        songs.clear();
    }

    public Location stage() {
        ConfigurationSection s = getConfig().getConfigurationSection("stage");
        if (s == null) return null;
        World w = Bukkit.getWorld(s.getString("world", "world"));
        if (w == null) return null;
        return new Location(w, s.getDouble("x"), s.getDouble("y"), s.getDouble("z"), (float) s.getDouble("yaw"), 0f);
    }

    public void setStage(Location l) {
        getConfig().set("stage.world", l.getWorld().getName());
        getConfig().set("stage.x", l.getX());
        getConfig().set("stage.y", l.getY());
        getConfig().set("stage.z", l.getZ());
        getConfig().set("stage.yaw", (double) l.getYaw());
        saveConfig();
    }

    /** Ambil pengaturan member; kalau tidak ada pakai default. */
    public String pget(String player, String key, String def) {
        String d = getConfig().getString("default." + key, def);
        return getConfig().getString("players." + player.toLowerCase() + "." + key, d);
    }

    public NbsSong song(String name) {
        NbsSong s = songs.get(name);
        if (s != null) return s;
        if (!"fanfare".equalsIgnoreCase(name)) {
            File dir = new File(getDataFolder(), "songs");
            File f = new File(dir, name);
            if (!f.exists()) f = new File(dir, name + ".nbs");
            try {
                s = NbsSong.load(f);
            } catch (Exception e) {
                getLogger().warning("Gagal membaca lagu '" + name + "': " + e + " -> pakai fanfare");
            }
        }
        if (s == null) s = NbsSong.fanfare();
        songs.put(name, s);
        return s;
    }

    public static String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    @EventHandler
    public void onJoin(final PlayerJoinEvent e) {
        if (!getConfig().getBoolean("play-on-first-join", false) || e.getPlayer().hasPlayedBefore()) return;
        Bukkit.getScheduler().runTaskLater(this, new Runnable() {
            @Override
            public void run() {
                Player p = e.getPlayer();
                if (!p.isOnline() || stage() == null) return;
                int n = Bukkit.getOfflinePlayers().length;
                int total = getConfig().getInt("total-members", 0);
                manager.enqueue(p, n, total > 0 ? total : n);
            }
        }, 60L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        manager.onQuit(e.getPlayer());
    }
}
