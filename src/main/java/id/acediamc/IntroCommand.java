package id.acediamc;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class IntroCommand implements CommandExecutor, TabCompleter {
    private static final List<String> SUB = Arrays.asList("play", "playall", "stop", "setstage", "set", "songs", "anims", "reload");
    private static final List<String> KEYS = Arrays.asList("animation", "camera", "particle", "song", "subtitle", "color", "number", "skin");
    private final AcediaMCPlugin plugin;

    public IntroCommand(AcediaMCPlugin plugin) {
        this.plugin = plugin;
    }

    private void msg(CommandSender s, String m) {
        s.sendMessage(AcediaMCPlugin.color("&6[AcediaMC] &f" + m));
    }

    @Override
    public boolean onCommand(CommandSender s, Command c, String label, String[] a) {
        if (a.length == 0) {
            msg(s, "/intro play <nama> | playall | stop | setstage | set <nama> <kunci> <nilai> | songs | anims | reload");
            return true;
        }
        String sub = a[0].toLowerCase();
        if (sub.equals("setstage")) {
            if (!(s instanceof Player)) { msg(s, "Hanya bisa dari dalam game."); return true; }
            plugin.setStage(((Player) s).getLocation());
            msg(s, "Panggung disimpan di lokasimu (arah hadap = arah hadap member).");
        } else if (sub.equals("play")) {
            if (a.length < 2) { msg(s, "Pakai: /intro play <nama>"); return true; }
            Player p = Bukkit.getPlayerExact(a[1]);
            if (p == null && !NpcBridge.available()) {
                msg(s, "Pemain harus online (atau pasang plugin Citizens agar member offline bisa tampil sebagai NPC).");
                return true;
            }
            if (plugin.stage() == null) { msg(s, "Atur panggung dulu: /intro setstage"); return true; }
            String nm = p != null ? p.getName() : a[1];
            int n = plugin.getConfig().getInt("players." + nm.toLowerCase() + ".number", 1);
            int total = plugin.getConfig().getInt("total-members", 0);
            plugin.manager().enqueue(nm, n, total > 0 ? total : Math.max(n, 1));
        } else if (sub.equals("playall")) {
            if (plugin.stage() == null) { msg(s, "Atur panggung dulu: /intro setstage"); return true; }
            int cnt = plugin.manager().playAll();
            msg(s, "Memutar intro " + cnt + " member" + (NpcBridge.available() ? " (mode NPC)." : " (mode pemain asli, tanpa Citizens)."));
        } else if (sub.equals("stop")) {
            plugin.manager().stopAll();
            msg(s, "Dihentikan.");
        } else if (sub.equals("reload")) {
            plugin.reload();
            msg(s, "Config dimuat ulang.");
        } else if (sub.equals("anims")) {
            msg(s, "Animasi: RANDOM, " + Arrays.toString(Animation.values()));
            msg(s, "Kamera: RANDOM, ORBIT, CRANE, PUSH, LOW");
        } else if (sub.equals("songs")) {
            String[] files = new java.io.File(plugin.getDataFolder(), "songs").list();
            msg(s, "Lagu: fanfare (bawaan)" + (files != null && files.length > 0 ? ", " + String.join(", ", files) : ""));
        } else if (sub.equals("set")) {
            if (a.length < 4) { msg(s, "Pakai: /intro set <pemain> <animation|particle|song|subtitle|color|number> <nilai>"); return true; }
            String key = a[2].toLowerCase();
            if (!KEYS.contains(key)) { msg(s, "Kunci tidak dikenal."); return true; }
            StringBuilder v = new StringBuilder();
            for (int i = 3; i < a.length; i++) v.append(i > 3 ? " " : "").append(a[i]);
            Object val = v.toString();
            if (key.equals("animation")) {
                if (v.toString().equalsIgnoreCase("RANDOM")) {
                    val = "RANDOM";
                } else {
                    try {
                        val = Animation.valueOf(v.toString().toUpperCase()).name();
                    } catch (IllegalArgumentException e) {
                        msg(s, "Animasi: RANDOM, " + Arrays.toString(Animation.values()));
                        return true;
                    }
                }
            } else if (key.equals("camera")) {
                String cv = v.toString().toUpperCase();
                if (!Arrays.asList("RANDOM", "ORBIT", "CRANE", "PUSH", "LOW").contains(cv)) {
                    msg(s, "Kamera: RANDOM, ORBIT, CRANE, PUSH, LOW");
                    return true;
                }
                val = cv;
            } else if (key.equals("number")) {
                try {
                    val = Integer.parseInt(v.toString().trim());
                } catch (NumberFormatException e) {
                    msg(s, "Nomor harus angka.");
                    return true;
                }
            }
            plugin.getConfig().set("players." + a[1].toLowerCase() + "." + key, val);
            plugin.saveConfig();
            plugin.reload();
            msg(s, a[1] + " -> " + key + " = " + val);
        } else {
            msg(s, "Sub-perintah tidak dikenal.");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command c, String l, String[] a) {
        List<String> out = new ArrayList<String>();
        if (a.length == 1) {
            for (String x : SUB) if (x.startsWith(a[0].toLowerCase())) out.add(x);
        } else if (a.length == 2 && (a[0].equalsIgnoreCase("play") || a[0].equalsIgnoreCase("set"))) {
            for (Player p : Bukkit.getOnlinePlayers()) if (p.getName().toLowerCase().startsWith(a[1].toLowerCase())) out.add(p.getName());
        } else if (a.length == 3 && a[0].equalsIgnoreCase("set")) {
            for (String x : KEYS) if (x.startsWith(a[2].toLowerCase())) out.add(x);
        } else if (a.length == 4 && a[0].equalsIgnoreCase("set") && a[2].equalsIgnoreCase("animation")) {
            out.add("RANDOM");
            for (Animation an : Animation.values()) out.add(an.name());
        } else if (a.length == 4 && a[0].equalsIgnoreCase("set") && a[2].equalsIgnoreCase("camera")) {
            out.addAll(Arrays.asList("RANDOM", "ORBIT", "CRANE", "PUSH", "LOW"));
        }
        return out;
    }
}
