package id.laper.epicintro;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Satu adegan intro: kamera sinematik mengelilingi satu member di panggung. */
public final class IntroSequence extends BukkitRunnable {
    private final EpicIntroPlugin plugin;
    private final Player actor;
    private final int index, total;
    private final List<Player> viewers;
    private final Runnable onDone;

    private final Location stage;
    private final int duration;
    private final double dist, height, orbit;
    private final Animation anim;
    private final String particle, color, subtitle;
    private final Music music;

    private final Map<UUID, Location> savedLoc = new HashMap<UUID, Location>();
    private final Map<UUID, GameMode> savedMode = new HashMap<UUID, GameMode>();
    private ArmorStand camera;
    private BossBar bar;
    private int tick = 0;
    private boolean done = false;

    public IntroSequence(EpicIntroPlugin plugin, Player actor, int index, int total,
                         List<Player> viewers, Runnable onDone) {
        this.plugin = plugin;
        this.actor = actor;
        this.index = index;
        this.total = total;
        this.viewers = new ArrayList<Player>(viewers);
        this.onDone = onDone;
        this.stage = plugin.stage();
        org.bukkit.configuration.file.FileConfiguration c = plugin.getConfig();
        this.duration = c.getInt("camera.duration-ticks", 160);
        this.dist = c.getDouble("camera.distance", 5.0);
        this.height = c.getDouble("camera.height", 1.8);
        this.orbit = c.getDouble("camera.orbit-degrees", 90.0);
        String n = actor.getName();
        Animation a;
        try {
            a = Animation.valueOf(plugin.pget(n, "animation", "SPIN").toUpperCase());
        } catch (IllegalArgumentException e) {
            a = Animation.SPIN;
        }
        this.anim = a;
        this.particle = plugin.pget(n, "particle", "STAR");
        this.color = plugin.pget(n, "color", "&6");
        this.subtitle = plugin.pget(n, "subtitle", "");
        this.music = new Music(plugin.song(plugin.pget(n, "song", "fanfare")),
                (float) c.getDouble("music-volume", 1.0));
    }

    public void begin() {
        savedLoc.put(actor.getUniqueId(), actor.getLocation());
        savedMode.put(actor.getUniqueId(), actor.getGameMode());
        actor.teleport(stage);
        actor.setGameMode(GameMode.ADVENTURE);

        camera = stage.getWorld().spawn(camLoc(0), ArmorStand.class);
        camera.setVisible(false);
        camera.setGravity(false);
        camera.setMarker(true);
        camera.setSilent(true);

        String title = ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("brand", "&6&lINTRO") + " &7- MEMBER "
                        + pad(index) + " OF " + pad(total));
        bar = Bukkit.createBossBar(title, BarColor.YELLOW, BarStyle.SEGMENTED_10);
        bar.setProgress(Math.max(0.0, Math.min(1.0, index / (double) Math.max(1, total))));

        for (Player v : viewers) {
            savedLoc.put(v.getUniqueId(), v.getLocation());
            savedMode.put(v.getUniqueId(), v.getGameMode());
            v.teleport(camLoc(0));
            v.setGameMode(GameMode.SPECTATOR);
            bar.addPlayer(v);
        }
        runTaskTimer(plugin, 1L, 1L);
    }

    @Override
    public void run() {
        if (!actor.isOnline()) { finish(); return; }
        camera.teleport(camLoc(tick));
        for (Player v : viewers) {
            Entity t = v.getSpectatorTarget();
            if (t == null || !t.getUniqueId().equals(camera.getUniqueId())) v.setSpectatorTarget(camera);
        }
        anim.tick(actor, stage, tick);

        List<Player> listeners = new ArrayList<Player>(viewers);
        listeners.add(actor);

        if (tick == 0) entrance(listeners);
        if (tick == 6) showTitle(listeners);
        if (tick % 20 == 0) actionBar(listeners);
        double a = tick * 0.35;
        Location c = stage.clone().add(Math.cos(a) * 1.1, 0.2 + (tick % 40) / 20.0, Math.sin(a) * 1.1);
        Compat.particle(particle, c, 3, 0.05, 0.05, 0.05, 0.02);
        Compat.particle(particle, stage.clone().add(0, 1, 0), 2, 0.5, 0.8, 0.5, 0.01);
        music.tick(listeners);

        if (++tick >= duration) finish();
    }

    private void entrance(List<Player> listeners) {
        stage.getWorld().strikeLightningEffect(stage);
        Compat.particle(particle, stage.clone().add(0, 1, 0), 150, 1.0, 1.0, 1.0, 0.15);
        for (Player p : listeners) {
            Compat.play(p, Compat.sound("entity.lightning_bolt.thunder", "entity.lightning.thunder"), 1f, 1f);
            Compat.play(p, "entity.player.levelup", 1f, 0.7f);
        }
    }

    private void showTitle(List<Player> listeners) {
        String t = ChatColor.translateAlternateColorCodes('&', color + "&l[" + pad(index) + "] " + actor.getName());
        String s = ChatColor.translateAlternateColorCodes('&', subtitle);
        for (Player p : listeners) p.sendTitle(t, s, 5, Math.max(20, duration - 40), 20);
    }

    private void actionBar(List<Player> listeners) {
        String text = ChatColor.translateAlternateColorCodes('&',
                color + "&l#" + pad(index) + " " + actor.getName() + " &7| MEMBER " + pad(index) + " OF " + pad(total));
        for (Player p : listeners) {
            try {
                p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(text));
            } catch (Throwable ignored) {
            }
        }
    }

    private Location camLoc(int t) {
        double p = duration <= 1 ? 0 : Math.min(1.0, t / (double) duration);
        double ease = p * p * (3 - 2 * p);
        double off = Math.toRadians(-orbit / 2 + orbit * ease);
        double yaw = Math.toRadians(stage.getYaw()) + off;
        double d = dist * (1.15 - 0.3 * p);
        double cx = stage.getX() - Math.sin(yaw) * d;
        double cz = stage.getZ() + Math.cos(yaw) * d;
        double eyeY = stage.getY() + height;
        double tx = stage.getX(), ty = stage.getY() + 1.2, tz = stage.getZ();
        double dx = tx - cx, dz = tz - cz, dy = ty - eyeY;
        float cyaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float cpitch = (float) Math.toDegrees(-Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
        double eyeOff = camera != null ? camera.getEyeHeight() : 0;
        return new Location(stage.getWorld(), cx, eyeY - eyeOff, cz, cyaw, cpitch);
    }

    /** Pemain keluar saat adegan berjalan. */
    public void release(Player p) {
        if (p.getUniqueId().equals(actor.getUniqueId())) { finish(); return; }
        if (savedLoc.containsKey(p.getUniqueId()) && viewers.remove(p)) restore(p);
    }

    private void restore(Player v) {
        UUID id = v.getUniqueId();
        try {
            v.setSpectatorTarget(null);
        } catch (Throwable ignored) {
        }
        if (savedMode.get(id) != null) v.setGameMode(savedMode.get(id));
        if (savedLoc.get(id) != null) v.teleport(savedLoc.get(id));
        v.setSneaking(false);
        if (bar != null) bar.removePlayer(v);
    }

    public void finish() {
        if (done) return;
        done = true;
        try {
            cancel();
        } catch (IllegalStateException ignored) {
        }
        for (Player v : new ArrayList<Player>(viewers)) restore(v);
        restore(actor);
        if (bar != null) bar.removeAll();
        if (camera != null) camera.remove();
        if (onDone != null) onDone.run();
    }
}
