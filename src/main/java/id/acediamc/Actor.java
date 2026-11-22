package id.acediamc;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/** "Pemeran" di panggung: bisa pemain asli, atau NPC kembar dengan skin pemain. */
interface Actor {
    void moveTo(Location l);

    void swing();

    void sneak(boolean on);

    /** slot: HAND, OFF_HAND, HELMET, CHESTPLATE, LEGGINGS, BOOTS; materials: daftar pilihan dipisah koma. */
    void equip(String slot, String materials);

    Location loc();

    void remove();

    static Material mat(String csv) {
        for (String s : csv.split(",")) {
            Material m = Material.matchMaterial(s.trim());
            if (m != null) return m;
        }
        return null;
    }
}

final class PlayerActor implements Actor {
    private final Player p;

    PlayerActor(Player p) {
        this.p = p;
    }

    public void moveTo(Location l) {
        p.teleport(l);
    }

    public void swing() {
        try {
            p.getClass().getMethod("swingMainHand").invoke(p);
        } catch (Throwable ignored) {
        }
    }

    public void sneak(boolean on) {
        p.setSneaking(on);
    }

    public void equip(String slot, String materials) {
    }

    public Location loc() {
        return p.getLocation();
    }

    public void remove() {
        p.setSneaking(false);
    }
}

final class NpcActor implements Actor {
    private final NpcBridge b;
    private Location last;

    NpcActor(NpcBridge b, Location start) {
        this.b = b;
        this.last = start.clone();
    }

    public void moveTo(Location l) {
        b.teleport(l);
        last = l.clone();
    }

    public void swing() {
        b.swing();
    }

    public void sneak(boolean on) {
        b.sneak(on);
    }

    public void equip(String slot, String materials) {
        b.equip(slot, Actor.mat(materials));
    }

    public Location loc() {
        return last.clone();
    }

    public void remove() {
        b.remove();
    }
}
