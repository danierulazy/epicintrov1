package id.acediamc;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Pembaca file Note Block Studio (.nbs) + lagu fanfare bawaan. */
public final class NbsSong {
    public final float tps;
    public final int length;
    private final Map<Integer, List<int[]>> notes = new HashMap<Integer, List<int[]>>();

    private NbsSong(float tps, int length) {
        this.tps = tps > 0 ? tps : 10f;
        this.length = length;
    }

    void add(int tick, int inst, int key, int vel) {
        List<int[]> l = notes.get(tick);
        if (l == null) { l = new ArrayList<int[]>(); notes.put(tick, l); }
        l.add(new int[]{inst, key, vel});
    }

    public List<int[]> at(int tick) {
        return notes.get(tick);
    }

    private static void skipString(ByteBuffer b) {
        int len = b.getInt();
        b.position(b.position() + len);
    }

    public static NbsSong load(File f) throws Exception {
        ByteBuffer b = ByteBuffer.wrap(Files.readAllBytes(f.toPath())).order(ByteOrder.LITTLE_ENDIAN);
        int first = b.getShort() & 0xFFFF;
        int version = 0;
        int length = first;
        if (first == 0) {
            version = b.get() & 0xFF;
            b.get(); // jumlah instrumen vanilla
            if (version >= 3) length = b.getShort() & 0xFFFF;
        }
        b.getShort(); // jumlah layer
        for (int i = 0; i < 4; i++) skipString(b); // nama, author, author asli, deskripsi
        int tempo = b.getShort() & 0xFFFF;
        b.position(b.position() + 3 + 20); // autosave(3 byte) + 5 statistik int
        skipString(b); // nama file midi
        if (version >= 4) b.position(b.position() + 4); // loop on/off, max loop, loop start
        NbsSong s = new NbsSong(tempo / 100f, length);
        try {
            int tick = -1;
            while (true) {
                int jt = b.getShort() & 0xFFFF;
                if (jt == 0) break;
                tick += jt;
                while (true) {
                    int jl = b.getShort() & 0xFFFF;
                    if (jl == 0) break;
                    int inst = b.get() & 0xFF;
                    int key = b.get() & 0xFF;
                    int vel = 100;
                    if (version >= 4) { vel = b.get() & 0xFF; b.get(); b.getShort(); }
                    s.add(tick, inst, key, vel);
                }
            }
        } catch (java.nio.BufferUnderflowException ignored) {
        }
        return s;
    }

    /** Lagu bawaan: progresi Am-F-C-G, arpeggio harp + bass + drum + bell. */
    public static NbsSong fanfare() {
        NbsSong s = new NbsSong(8f, 64);
        int[] roots = {0, -4, 3, -2};
        boolean[] minor = {true, false, false, false};
        int[] min = {0, 3, 7, 12};
        int[] maj = {0, 4, 7, 12};
        int[] pat = {0, 1, 2, 3, 2, 1, 2, 3};
        for (int bar = 0; bar < 8; bar++) {
            int c = bar % 4;
            int[] iv = minor[c] ? min : maj;
            for (int st = 0; st < 8; st++) {
                int t = bar * 8 + st;
                s.add(t, 0, 45 + roots[c] + iv[pat[st]], 90);
                if (st == 0) {
                    s.add(t, 1, 33 + (((roots[c] % 12) + 12) % 12), 100);
                    s.add(t, 2, 33, 100);
                    s.add(t, 7, 57 + roots[c] + iv[2], 70);
                }
                if (st == 4) s.add(t, 2, 33, 100);
                if (st == 6) s.add(t, 3, 45, 70);
            }
        }
        return s;
    }
}
