package id.laper.epicintro;

import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;

/** Pemutar lagu: dipanggil sekali tiap server tick. */
public final class Music {
    private final NbsSong song;
    private final float volume;
    private double pos = 0;
    private int last = -1;

    public Music(NbsSong song, float volume) {
        this.song = song;
        this.volume = volume;
    }

    public void tick(Collection<Player> listeners) {
        pos += song.tps / 20.0;
        int now = (int) pos;
        for (int t = last + 1; t <= now; t++) {
            List<int[]> list = song.at(t);
            if (list == null) continue;
            for (int[] n : list) {
                float pitch = (float) Math.pow(2.0, (n[1] - 45) / 12.0);
                while (pitch < 0.5f) pitch *= 2f;
                while (pitch > 2f) pitch /= 2f;
                float vol = Math.max(0.05f, (n[2] / 100f) * volume);
                String snd = Compat.noteSound(n[0]);
                for (Player p : listeners) Compat.play(p, snd, vol, pitch);
            }
        }
        last = now;
        if (now > song.length) { pos = 0; last = -1; } // ulang
    }
}
