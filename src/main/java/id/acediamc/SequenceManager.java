package id.acediamc;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Antrean adegan: member diputar satu per satu. */
public final class SequenceManager {
    private static final class Job {
        final String name;
        final int index, total;

        Job(String n, int i, int t) {
            name = n;
            index = i;
            total = t;
        }
    }

    private final AcediaMCPlugin plugin;
    private final Deque<Job> queue = new ArrayDeque<Job>();
    private IntroSequence current;

    public SequenceManager(AcediaMCPlugin plugin) {
        this.plugin = plugin;
    }

    public void enqueue(String name, int index, int total) {
        queue.add(new Job(name, index, total));
        next();
    }

    /** Memutar semua member: urutan 'order' di config (boleh offline kalau ada Citizens), lalu pemain online lain. */
    public int playAll() {
        boolean npc = NpcBridge.available();
        List<String> names = new ArrayList<String>();
        Set<String> seen = new HashSet<String>();
        for (String n : plugin.getConfig().getStringList("order")) {
            Player p = Bukkit.getPlayerExact(n);
            if (p == null && !npc) continue;
            String nm = p != null ? p.getName() : n;
            if (seen.add(nm.toLowerCase())) names.add(nm);
        }
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (seen.add(p.getName().toLowerCase())) names.add(p.getName());
        }
        int total = plugin.getConfig().getInt("total-members", 0);
        if (total <= 0) total = names.size();
        int i = 1;
        for (String nm : names) {
            int num = plugin.getConfig().getInt("players." + nm.toLowerCase() + ".number", i);
            queue.add(new Job(nm, num, total));
            i++;
        }
        next();
        return names.size();
    }

    private void next() {
        if (current != null) return;
        Job j = queue.poll();
        if (j == null) return;
        if (plugin.stage() == null) { next(); return; }
        Player actorPlayer = Bukkit.getPlayerExact(j.name);
        List<Player> viewers = new ArrayList<Player>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.hasPermission("acediamc.skip")) viewers.add(p);
        }
        current = new IntroSequence(plugin, j.name, actorPlayer, j.index, j.total, viewers, new Runnable() {
            @Override
            public void run() {
                current = null;
                Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
                    @Override
                    public void run() {
                        next();
                    }
                }, 10L);
            }
        });
        current.begin();
    }

    public void onQuit(Player p) {
        if (current != null) current.release(p);
    }

    public void stopAll() {
        queue.clear();
        if (current != null) current.finish();
    }
}
