package id.acediamc;

import org.bukkit.Location;

import java.util.Random;

/** Koreografi member di panggung. Semua gerakan dihitung per tick (kinematik). */
public enum Animation {
    NONE, SPIN, WAVE, DANCE, SLASH, SHOWOFF, TORNADO, LEVITATE, WARP, METEOR;

    private static final Random RND = new Random();
    private static final String[] ITEMS = {"DIAMOND_SWORD", "BOW", "TRIDENT", "GOLDEN_APPLE", "SHIELD"};

    /** Animasi yang mengatur sendiri efek kemunculannya. */
    public boolean ownEntrance() {
        return this == METEOR;
    }

    public int titleTick() {
        return this == METEOR ? 42 : 6;
    }

    public static Animation pick(String cfg) {
        if (cfg == null || cfg.equalsIgnoreCase("RANDOM")) {
            Animation[] all = values();
            return all[1 + RND.nextInt(all.length - 1)];
        }
        try {
            return valueOf(cfg.toUpperCase());
        } catch (IllegalArgumentException e) {
            return SPIN;
        }
    }

    private static double ease(double p) {
        p = Math.max(0, Math.min(1, p));
        return p * p * (3 - 2 * p);
    }

    private static Location at(Location s, double dx, double dy, double dz, float yaw) {
        Location l = s.clone().add(dx, dy, dz);
        l.setYaw(yaw);
        l.setPitch(0f);
        return l;
    }

    private static void burst(String particle, Location l, int n) {
        Compat.particle(particle, l.clone().add(0, 1, 0), n, 0.3, 0.8, 0.3, 0.15);
    }

    public void tick(Actor a, Location stage, int t, int d, String particle) {
        float face = stage.getYaw();
        double p = t / (double) Math.max(1, d);
        switch (this) {
            case SPIN: {
                double sp = Math.min(1.0, p / 0.8);
                a.moveTo(at(stage, 0, Math.sin(Math.PI * sp) * 0.6, 0, face + (float) (1080 * ease(sp))));
                if (t % 30 == 0) a.swing();
                break;
            }
            case WAVE: {
                int m = t % 24;
                double hop = m < 8 ? Math.sin(Math.PI * m / 8.0) * 0.5 : 0;
                a.moveTo(at(stage, 0, hop, 0, face));
                if (t % 12 == 0) a.swing();
                break;
            }
            case DANCE: {
                a.moveTo(at(stage, 0, Math.abs(Math.sin(t * 0.45)) * 0.5, 0, face + t * 14f));
                a.sneak((t / 4) % 2 == 0);
                if (t % 10 == 0) a.swing();
                break;
            }
            case SLASH: {
                if (t == 0) a.equip("HAND", "NETHERITE_SWORD,DIAMOND_SWORD,IRON_SWORD");
                a.moveTo(at(stage, 0, 0, 0, face + (float) (Math.sin(t * 0.3) * 50)));
                if (t % 5 == 0) {
                    a.swing();
                    Location c = a.loc().add(0, 1.0, 0);
                    Compat.particle("SWEEP", c, 1, 0, 0, 0, 0);
                    Compat.particle("CRIT", c, 8, 0.5, 0.4, 0.5, 0.2);
                }
                break;
            }
            case SHOWOFF: {
                if (t == 0) {
                    a.equip("HELMET", "GOLDEN_HELMET");
                    a.equip("CHESTPLATE", "NETHERITE_CHESTPLATE,DIAMOND_CHESTPLATE,IRON_CHESTPLATE");
                }
                if (t % 30 == 0) {
                    a.equip("HAND", ITEMS[(t / 30) % ITEMS.length]);
                    a.swing();
                    Compat.particle(particle, a.loc().add(0, 1, 0), 30, 0.5, 0.8, 0.5, 0.1);
                }
                a.moveTo(at(stage, 0, 0, 0, face + (float) (Math.sin(t * 0.1) * 30)));
                break;
            }
            case TORNADO: {
                a.moveTo(at(stage, 0, ease(p) * 2, 0, face + (float) (t * t * 0.12)));
                double ang = t * 0.6;
                double r = 0.6 + p * 2.4;
                for (int k = 0; k < 4; k++) {
                    double aa = ang + k * Math.PI / 2;
                    Compat.particle(particle, stage.clone().add(Math.cos(aa) * r, (t % 30) / 10.0, Math.sin(aa) * r), 1, 0, 0, 0, 0);
                }
                break;
            }
            case LEVITATE: {
                double h = 5 * (ease(t / 60.0) - ease((t - (d - 40)) / 40.0));
                a.moveTo(at(stage, 0, h, 0, face + t * 4f));
                double ang = t * 0.3;
                Compat.particle("END", stage.clone().add(Math.cos(ang) * 1.5, 0.1, Math.sin(ang) * 1.5), 2, 0, 0, 0, 0.01);
                Compat.particle(particle, a.loc().add(0, 0.2, 0), 3, 0.4, 0.1, 0.4, 0.01);
                break;
            }
            case WARP: {
                boolean last = t >= d - 20;
                double ang = Math.toRadians((t / 20) * 72);
                Location pos = last ? stage.clone() : stage.clone().add(Math.cos(ang) * 3, 0, Math.sin(ang) * 3);
                double dx = stage.getX() - pos.getX();
                double dz = stage.getZ() - pos.getZ();
                float yaw = (dx * dx + dz * dz < 0.01) ? face : (float) Math.toDegrees(Math.atan2(-dx, dz));
                pos.setYaw(yaw);
                pos.setPitch(0f);
                if (t % 20 == 0) {
                    if (t > 0) burst("END", a.loc(), 40);
                    a.moveTo(pos);
                    burst("END", pos, 40);
                    a.swing();
                    Compat.worldPlay(pos, Compat.sound("entity.enderman.teleport", "entity.endermen.teleport"), 1f, 1f);
                } else {
                    a.moveTo(pos);
                }
                break;
            }
            case METEOR: {
                if (t < 40) {
                    double q = t / 40.0;
                    Location l = at(stage, 0, 40 * (1 - q) * (1 - q), 0, face + t * 20f);
                    a.moveTo(l);
                    Compat.particle("FLAME", l.clone().add(0, 1, 0), 12, 0.3, 0.6, 0.3, 0.02);
                    Compat.particle("SMOKE", l.clone().add(0, 1, 0), 6, 0.3, 0.6, 0.3, 0.02);
                } else {
                    a.moveTo(at(stage, 0, 0, 0, face));
                    if (t == 40) {
                        impact(stage, particle);
                        a.sneak(true);
                    }
                    if (t == 55) a.sneak(false);
                    if (t >= 58 && (t - 58) % 14 == 0) a.swing();
                }
                break;
            }
            default:
                a.moveTo(at(stage, 0, 0, 0, face));
        }
    }

    private static void impact(Location s, String particle) {
        s.getWorld().strikeLightningEffect(s);
        Compat.particle("BOOM", s.clone().add(0, 1, 0), 3, 1, 0.5, 1, 0);
        Compat.particle(particle, s.clone().add(0, 1, 0), 200, 1.2, 1.0, 1.2, 0.25);
        for (int i = 0; i < 60; i++) {
            double ang = Math.PI * 2 * i / 60;
            Compat.particle("SMOKE", s.clone().add(Math.cos(ang) * 2.5, 0.2, Math.sin(ang) * 2.5), 1, 0, 0.05, 0, 0.05);
        }
        Compat.worldPlay(s, Compat.sound("entity.generic.explode", "entity.generic.explode"), 2f, 0.8f);
    }
}
