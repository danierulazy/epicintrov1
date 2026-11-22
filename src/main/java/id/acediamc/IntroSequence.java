package id.acediamc;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/** Satu adegan intro: kamera sinematik mengelilingi satu member (NPC kembar atau pemain asli). */
public final class IntroSequence extends BukkitRunnable {
    private static final Random RND = new Random();
    private static final String[] CAMS = {"ORBIT", "CRANE", "PUSH", "LOW"};

    private final AcediaMCPlugin plugin;
    private final String name;
    private final Player player; // boleh null (member offline)
    private final int index, total;
    private final List<Player> viewers;
    private final Runnable onDone;

    private final Location stage;
    private final int duration, warmup;
    private final double dist, height, orbit;
    private final Animation anim;
    private final String camStyle, particle, color, subtitle, skin;
    private final Music music;

    private final Map<UUID, Location> savedLoc = new HashMap<UUID, Location>();
    private final Map<UUID, GameMode> savedMode = new HashMap<UUID, GameMode>();
    private Actor actor;
    private boolean npcMode;
    private ArmorStand camera;
    private BossBar bar;
    private int tick = 0;
    private boolean done = false;

    public IntroSequence(AcediaMCPlugin plugin, String name, Player player, int index, int total,
                         List<Player> candidates, Runnable onDone) {
        this.plugin = plugin;
        this.name = name;
        this.player = player;
        this.index = index;
        this.total = total;
        this.viewers = new ArrayList<Player>(candidates);
        this.onDone = onDone;
        this.stage = plugin.stage();
        FileConfiguration c = plugin.getConfig();
        this.duration = c.getInt("camera.duration-ticks", 200);
        this.warmup = (c.getBoolean("use-npc", true) && NpcBridge.available()) ? c.getInt("npc-warmup-ticks", 30) : 0;
        this.dist = c.getDouble("camera.distance", 5.0);
        this.height = c.getDouble("camera.height", 1.8);
        this.orbit = c.getDouble("camera.orbit-degrees", 90.0);
        this.anim = Animation.pick(plugin.pget(name, "animation", "RANDOM"));
        this.camStyle = pickCam(plugin.pget(name, "camera", "RANDOM"));
        this.particle = plugin.pget(name, "particle", "STAR");
        this.color = plugin.pget(name, "color", "&6");
        this.subtitle = plugin.pget(name, "subtitle", "");
        this.skin = plugin.pget(name, "skin", name);
        this.music = new Music(plugin.song(plugin.pget(name, "song", "fanfare")),
                (float) c.getDouble("music-volume", 1.0));
    }

    private static String pickCam(String cfg) {
        if (cfg == null || cfg.equalsIgnoreCase("RANDOM")) return CAMS[RND.nextInt(CAMS.length)];
        for (String s : CAMS) if (s.equalsIgnoreCase(cfg)) return s;
        return "ORBIT";
    }

    private boolean setupActor() {
        if (npcMode) {
            try {
                double y = Math.min(stage.getY() + 60, stage.getWorld().getMaxHeight() - 5);
                Location parked = stage.clone();
                parked.setY(y);
                actor = new NpcActor(NpcBridge.create(ChatColor.translateAlternateColorCodes('&', color + name), skin, parked), parked);
                return true;
            } catch (Throwable t) {
                plugin.getLogger().warning("Gagal membuat NPC untuk " + name + ": " + t);
                npcMode = false;
            }
        }
        if (player == null || !player.isOnline()) return false;
        actor = new PlayerActor(player);
        viewers.remove(player);
        savedLoc.put(player.getUniqueId(), player.getLocation());
        savedMode.put(player.getUniqueId(), player.getGameMode());
        player.teleport(stage);
        player.setGameMode(GameMode.ADVENTURE);
        return true;
    }

    public void begin() {
        npcMode = plugin.getConfig().getBoolean("use-npc", true) && NpcBridge.available();
        if (!setupActor()) {
            plugin.getLogger().warning("Intro " + name + " dilewati: member offline dan Citizens tidak tersedia.");
            done = true;
            if (onDone != null) onDone.run();
            return;
        }
        camera = stage.getWorld().spawn(camLoc(-1), ArmorStand.class);
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
            v.teleport(camLoc(-1));
            v.setGameMode(GameMode.SPECTATOR);
            bar.addPlayer(v);
        }
        runTaskTimer(plugin, 1L, 1L);
    }

    @Override
    public void run() {
        if (!npcMode && player != null && !player.isOnline()) { finish(); return; }
        int st = tick - warmup;
        if (st >= 0) anim.tick(actor, stage, st, duration, particle);
        camera.teleport(camLoc(st));
        for (Player v : viewers) {
            Entity t = v.getSpectatorTarget();
            if (t == null || !t.getUniqueId().equals(camera.getUniqueId())) v.setSpectatorTarget(camera);
        }

        List<Player> listeners = new ArrayList<Player>(viewers);
        if (!npcMode && player != null) listeners.add(player);

        if (st >= 0) {
            if (st == 0 && !anim.ownEntrance()) entrance(listeners);
            if (st == anim.titleTick()) showTitle(listeners);
            if (st % 20 == 0) actionBar(listeners);
            Location a = actor.loc();
            double ang = st * 0.35;
            Compat.particle(particle, a.clone().add(Math.cos(ang) * 1.1, 0.2 + (st % 40) / 20.0, Math.sin(ang) * 1.1),
                    3, 0.05, 0.05, 0.05, 0.02);
            music.tick(listeners);
        }
        if (++tick >= warmup + duration) finish();
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
        String t = ChatColor.translateAlternateColorCodes('&', color + "&l[" + pad(index) + "] " + name);
        String s = ChatColor.translateAlternateColorCodes('&', subtitle);
        for (Player p : listeners) p.sendTitle(t, s, 5, Math.max(20, duration - 60), 20);
    }

    private void actionBar(List<Player> listeners) {
        String text = ChatColor.translateAlternateColorCodes('&',
                color + "&l#" + pad(index) + " " + name + " &7| MEMBER " + pad(index) + " OF " + pad(total));
        for (Player p : listeners) {
            try {
                p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(text));
            } catch (Throwable ignored) {
            }
        }
    }

    private Location camLoc(int st) {
        double p = st <= 0 ? 0 : Math.min(1.0, st / (double) Math.max(1, duration));
        double ease = p * p * (3 - 2 * p);
        double offDeg, d, h;
        switch (camStyle) {
            case "CRANE":
                offDeg = -20 + 40 * ease;
                d = dist * 1.2 * (1 - 0.25 * p);
                h = 0.6 + 5.0 * ease;
                break;
            case "PUSH":
                offDeg = -30 + 30 * ease;
                d = dist * (2.0 - 1.1 * ease);
                h = height;
                break;
            case "LOW":
                offDeg = -orbit + 2 * orbit * ease;
                d = dist * 0.9;
                h = 0.5;
                break;
            default:
                offDeg = -orbit / 2 + orbit * ease;
                d = dist * (1.15 - 0.3 * p);
                h = height;
        }
        double yaw = Math.toRadians(stage.getYaw()) + Math.toRadians(offDeg);
        double cx = stage.getX() - Math.sin(yaw) * d;
        double cz = stage.getZ() + Math.cos(yaw) * d;
        double eyeY = stage.getY() + h;
        if (anim == Animation.METEOR && st >= 40 && st < 50) { // getaran kamera saat hantaman
            double shake = (50 - st) * 0.03;
            cx += (RND.nextDouble() - 0.5) * shake;
            eyeY += (RND.nextDouble() - 0.5) * shake;
            cz += (RND.nextDouble() - 0.5) * shake;
        }
        Location tl = (st < 0 || actor == null) ? stage : actor.loc();
        double tx = tl.getX(), ty = tl.getY() + 1.2, tz = tl.getZ();
        double dx = tx - cx, dz = tz - cz, dy = ty - eyeY;
        float cyaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float cpitch = (float) Math.toDegrees(-Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
        double eyeOff = camera != null ? camera.getEyeHeight() : 0;
        return new Location(stage.getWorld(), cx, eyeY - eyeOff, cz, cyaw, cpitch);
    }

    private static String pad(int n) {
        return String.format("%02d", n);
    }

    /** Pemain keluar saat adegan berjalan. */
    public void release(Player p) {
        if (!npcMode && player != null && p.getUniqueId().equals(player.getUniqueId())) { finish(); return; }
        if (viewers.remove(p)) restore(p);
    }

    private void restore(Player v) {
        UUID id = v.getUniqueId();
        if (!savedLoc.containsKey(id)) return;
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
        if (!npcMode && player != null && player.isOnline()) restore(player);
        if (actor != null) actor.remove();
        if (bar != null) bar.removeAll();
        if (camera != null) camera.remove();
        if (onDone != null) onDone.run();
    }
}
