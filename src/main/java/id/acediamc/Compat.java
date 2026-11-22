package id.acediamc;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Lapisan kompatibilitas lintas versi: nama sound & particle berubah-ubah tiap versi Minecraft. */
public final class Compat {
    private static final int MINOR = detectMinor();
    private static final Map<String, Object> CACHE = new HashMap<String, Object>();
    private static final Map<String, List<String>> ALIASES = new HashMap<String, List<String>>();
    private static Class<?> particleClass;
    private static Method spawn;
    private static boolean particleBroken = false;

    static {
        ALIASES.put("STAR", Arrays.asList("FIREWORKS_SPARK", "FIREWORK"));
        ALIASES.put("SPARKLE", Arrays.asList("VILLAGER_HAPPY", "HAPPY_VILLAGER"));
        ALIASES.put("MAGIC", Arrays.asList("SPELL_WITCH", "WITCH"));
        ALIASES.put("SMOKE", Arrays.asList("CLOUD"));
        ALIASES.put("HEART", Arrays.asList("HEART"));
        ALIASES.put("FLAME", Arrays.asList("FLAME"));
        ALIASES.put("END", Arrays.asList("END_ROD"));
        ALIASES.put("NOTE", Arrays.asList("NOTE"));
        ALIASES.put("BOOM", Arrays.asList("EXPLOSION_LARGE", "EXPLOSION"));
        ALIASES.put("SWEEP", Arrays.asList("SWEEP_ATTACK"));
        try {
            particleClass = Class.forName("org.bukkit.Particle");
            spawn = World.class.getMethod("spawnParticle", particleClass, double.class, double.class,
                    double.class, int.class, double.class, double.class, double.class, double.class);
        } catch (Throwable t) {
            particleBroken = true;
        }
    }

    private Compat() {}

    private static int detectMinor() {
        try {
            return Integer.parseInt(Bukkit.getBukkitVersion().split("-")[0].split("\\.")[1]);
        } catch (Throwable t) {
            return 20;
        }
    }

    public static String sound(String modern, String legacy) {
        return MINOR >= 13 ? modern : legacy;
    }

    /** Nama sound note block untuk instrumen NBS (0..15). */
    public static String noteSound(int inst) {
        String[] n = {"harp", "bass", "basedrum", "snare", "hat", "guitar", "flute", "bell", "chime",
                "xylophone", "iron_xylophone", "cow_bell", "didgeridoo", "bit", "banjo", "pling"};
        if (inst < 0 || inst >= n.length) inst = 0;
        if (MINOR < 14 && inst >= 10) inst = 0;
        if (MINOR < 12 && inst >= 5 && inst <= 9) inst = 0;
        return (MINOR >= 13 ? "block.note_block." : "block.note.") + n[inst];
    }

    public static void play(Player p, String sound, float vol, float pitch) {
        try {
            p.playSound(p.getLocation(), sound, vol, pitch);
        } catch (Throwable ignored) {
        }
    }

    public static void worldPlay(Location l, String sound, float vol, float pitch) {
        try {
            l.getWorld().playSound(l, sound, vol, pitch);
        } catch (Throwable ignored) {
        }
    }

    private static Object particle(String name) {
        String key = name.toUpperCase();
        if (CACHE.containsKey(key)) return CACHE.get(key);
        Object found = null;
        try {
            Object[] consts = particleClass.getEnumConstants();
            if (consts != null) {
                List<String> cands = ALIASES.containsKey(key) ? ALIASES.get(key) : Arrays.asList(key);
                for (String c : cands) {
                    for (Object o : consts) {
                        if (((Enum<?>) o).name().equals(c)) { found = o; break; }
                    }
                    if (found != null) break;
                }
            }
        } catch (Throwable ignored) {
        }
        CACHE.put(key, found);
        return found;
    }

    public static void particle(String name, Location l, int count, double dx, double dy, double dz, double speed) {
        if (particleBroken || l.getWorld() == null) return;
        Object p = particle(name);
        if (p == null) return;
        try {
            spawn.invoke(l.getWorld(), p, l.getX(), l.getY(), l.getZ(), count, dx, dy, dz, speed);
        } catch (Throwable t) {
            particleBroken = true;
        }
    }
}
