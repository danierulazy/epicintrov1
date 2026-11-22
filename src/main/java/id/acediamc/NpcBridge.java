package id.acediamc;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;

/** Jembatan ke plugin Citizens lewat reflection, jadi tidak perlu library Citizens saat build. */
public final class NpcBridge {
    private static boolean ready = false, failed = false;
    private static Object registry;
    private static Method mCreate, mSpawn, mDestroy, mGetEntity, mTrait, mTeleport;
    private static Class<?> skinCls, eqCls, slotCls, animCls, sneakCls;
    private static Method mSkin, mEquipSet, mPlay;

    private final Object npc;

    private NpcBridge(Object npc) {
        this.npc = npc;
    }

    public static synchronized boolean available() {
        if (ready) return true;
        if (failed || !Bukkit.getPluginManager().isPluginEnabled("Citizens")) return false;
        try {
            Class<?> api = Class.forName("net.citizensnpcs.api.CitizensAPI");
            Class<?> store = Class.forName("net.citizensnpcs.api.npc.NPCDataStore");
            Object mem = Class.forName("net.citizensnpcs.api.npc.MemoryNPCDataStore").getDeclaredConstructor().newInstance();
            registry = api.getMethod("createAnonymousNPCRegistry", store).invoke(null, mem);
            Class<?> reg = Class.forName("net.citizensnpcs.api.npc.NPCRegistry");
            Class<?> npcCls = Class.forName("net.citizensnpcs.api.npc.NPC");
            mCreate = reg.getMethod("createNPC", EntityType.class, String.class);
            mSpawn = npcCls.getMethod("spawn", Location.class);
            mDestroy = npcCls.getMethod("destroy");
            mGetEntity = npcCls.getMethod("getEntity");
            mTrait = npcCls.getMethod("getOrAddTrait", Class.class);
            skinCls = Class.forName("net.citizensnpcs.trait.SkinTrait");
            mSkin = skinCls.getMethod("setSkinName", String.class);
            try {
                mTeleport = npcCls.getMethod("teleport", Location.class, PlayerTeleportEvent.TeleportCause.class);
            } catch (Throwable ignored) {
                mTeleport = null;
            }
            try {
                eqCls = Class.forName("net.citizensnpcs.api.trait.trait.Equipment");
                slotCls = Class.forName("net.citizensnpcs.api.trait.trait.Equipment$EquipmentSlot");
                mEquipSet = eqCls.getMethod("set", slotCls, ItemStack.class);
            } catch (Throwable ignored) {
                mEquipSet = null;
            }
            try {
                animCls = Class.forName("net.citizensnpcs.util.PlayerAnimation");
                mPlay = animCls.getMethod("play", Player.class);
            } catch (Throwable ignored) {
                mPlay = null;
            }
            try {
                sneakCls = Class.forName("net.citizensnpcs.trait.SneakTrait");
            } catch (Throwable ignored) {
                sneakCls = null;
            }
            ready = true;
            return true;
        } catch (Throwable t) {
            failed = true;
            Bukkit.getLogger().warning("[AcediaMC] Citizens ditemukan tapi gagal dihubungkan: " + t);
            return false;
        }
    }

    public static NpcBridge create(String display, String skin, Location at) throws Exception {
        Object n = mCreate.invoke(registry, EntityType.PLAYER, display);
        Object trait = mTrait.invoke(n, skinCls);
        mSkin.invoke(trait, skin);
        mSpawn.invoke(n, at);
        NpcBridge b = new NpcBridge(n);
        Entity e = b.entity();
        if (e != null) {
            try {
                e.setGravity(false);
                e.setInvulnerable(true);
            } catch (Throwable ignored) {
            }
        }
        return b;
    }

    public Entity entity() {
        try {
            return (Entity) mGetEntity.invoke(npc);
        } catch (Throwable t) {
            return null;
        }
    }

    public void teleport(Location l) {
        try {
            if (mTeleport != null) {
                mTeleport.invoke(npc, l, PlayerTeleportEvent.TeleportCause.PLUGIN);
                return;
            }
        } catch (Throwable ignored) {
        }
        Entity e = entity();
        if (e != null) e.teleport(l);
    }

    public void swing() {
        Entity e = entity();
        if (!(e instanceof Player)) return;
        try {
            if (mPlay != null) {
                @SuppressWarnings({"unchecked", "rawtypes"})
                Object arm = Enum.valueOf((Class) animCls, "ARM_SWING");
                mPlay.invoke(arm, (Player) e);
            }
        } catch (Throwable ignored) {
        }
    }

    public void sneak(boolean on) {
        try {
            if (sneakCls != null) {
                Object trait = mTrait.invoke(npc, sneakCls);
                sneakCls.getMethod("setSneaking", boolean.class).invoke(trait, on);
                return;
            }
        } catch (Throwable ignored) {
        }
        Entity e = entity();
        if (e instanceof Player) ((Player) e).setSneaking(on);
    }

    public void equip(String slot, Material m) {
        if (mEquipSet == null) return;
        try {
            Object trait = mTrait.invoke(npc, eqCls);
            @SuppressWarnings({"unchecked", "rawtypes"})
            Object s = Enum.valueOf((Class) slotCls, slot);
            mEquipSet.invoke(trait, s, new ItemStack(m == null ? Material.AIR : m));
        } catch (Throwable ignored) {
        }
    }

    public void remove() {
        try {
            mDestroy.invoke(npc);
        } catch (Throwable ignored) {
        }
    }
}
