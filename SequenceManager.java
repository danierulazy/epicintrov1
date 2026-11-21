package id.laper.epicintro;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/** Antrean adegan: member diputar satu per satu. */
public final class SequenceManager {
    private static final class Job {
        final Player actor; final int index, total;
        Job(Player a, int i, int t) { actor = a; index = i; total = t; }
    }

    private final EpicIntroPlugin plugin;
    private final Deque<Job> queue = new ArrayDeque<Job>();
    private IntroSequence current;

    public SequenceManager(EpicIntroPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isRunning() {
        return current != null || !queue.isEmpty();
    }

    public void enqueue(Player actor, int index, int total) {
        queue.add(new Job(actor, index, total));
        next();
    }

    public void playAll() {
        List<Player> list = new ArrayList<Player>();
        for (String name : plugin.getConfig().getStringList("order")) {
            Player p = Bukkit.getPlayerExact(name);
            if (p != null && !list.contains(p)) list.add(p);
        }
        for (Player p : Bukkit.getOnlinePlayers()) if (!list.contains(p)) list.add(p);
        int total = plugin.getConfig().getInt("total-members", 0);
        if (total <= 0) total = list.size();
        int i = 1;
        for (Player p : list) {
            int num = plugin.getConfig().getInt("players." + p.getName().toLowerCase() + ".number", i);
            queue.add(new Job(p, num, total));
            i++;
        }
        next();
    }

    private void next() {
        if (current != null) return;
        Job j = queue.poll();
        if (j == null) return;
        if (!j.actor.isOnline() || plugin.stage() == null) { next(); return; }
        List<Player> viewers = new ArrayList<Player>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.equals(j.actor) && !p.hasPermission("epicintro.skip")) viewers.add(p);
        }
        current = new IntroSequence(plugin, j.actor, j.index, j.total, viewers, new Runnable() {
            @Override
            public void run() {
                current = null;
                Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
                    @Override
                    public void run() { next(); }
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
