package id.acediamc;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/** Animasi member memakai skin aslinya sendiri (pemain digerakkan di panggung). */
public enum Animation {
    NONE, SPIN, WAVE, JUMP, SNEAK, DANCE;

    public void tick(Player p, Location stage, int t) {
        Location l = stage.clone();
        switch (this) {
            case SPIN:
                l.setYaw(stage.getYaw() + t * 9f);
                p.teleport(l);
                break;
            case DANCE:
                l.setYaw(stage.getYaw() + t * 14f);
                p.teleport(l);
                p.setSneaking((t / 4) % 2 == 0);
                if (t % 10 == 0) swing(p);
                break;
            case WAVE:
                hold(p, l, true);
                if (t % 12 == 0) swing(p);
                break;
            case JUMP:
                hold(p, l, false);
                if (t % 24 == 0) p.setVelocity(new Vector(0, 0.55, 0));
                break;
            case SNEAK:
                hold(p, l, true);
                p.setSneaking((t / 10) % 2 == 0);
                break;
            default:
                hold(p, l, true);
        }
    }

    private static void hold(Player p, Location target, boolean lockY) {
        Location cur = p.getLocation();
        double dx = cur.getX() - target.getX();
        double dz = cur.getZ() - target.getZ();
        double dy = lockY ? cur.getY() - target.getY() : 0;
        if (dx * dx + dz * dz + dy * dy > 0.04) {
            Location to = target.clone();
            if (!lockY) to.setY(cur.getY());
            p.teleport(to);
        }
    }

    private static void swing(Player p) {
        try { // swingMainHand baru ada di 1.15.2+, jadi dipanggil lewat reflection
            p.getClass().getMethod("swingMainHand").invoke(p);
        } catch (Throwable ignored) {
        }
    }
}
