# AcediaMC

Plugin intro sinematik ala video sinematik server Minecraft: tiap member tampil di panggung dengan
skin aslinya, nama `[NN] Nama`, subtitle, bossbar "MEMBER NN OF 40", partikel, animasi,
dan musik note block yang epic. Kamera sinematik mengelilingi member.

## Build
Butuh JDK 8+ dan Maven:

    mvn clean package

Hasil: `target/AcediaMC-1.0.0.jar` -> taruh di folder `plugins/` server.

## Pemakaian cepat
1. Berdiri di tempat panggung, hadap ke arah yang kamu mau, lalu `/intro setstage`
2. Pastikan ada pemain lain online (mereka jadi penonton/kamera)
3. `/intro play <pemain>` atau `/intro playall`

## Perintah (permission `acediamc.admin`, alias `/intro`)
- `/intro play <pemain>`: putar intro satu member
- `/intro playall`: putar semua member online berurutan
- `/intro stop`
- `/intro setstage`
- `/intro set <pemain> <animation|particle|song|subtitle|color|number> <nilai>`
- `/intro songs`, `/intro reload`

Animasi: NONE, SPIN, WAVE, JUMP, SNEAK, DANCE.
Partikel: STAR, HEART, FLAME, END, MAGIC, NOTE, SMOKE, SPARKLE.

## Musik
Lagu bawaan `fanfare`. Untuk lagu sendiri, taruh file `.nbs` (Note Block Studio)
di `plugins/AcediaMC/songs/`, lalu `/intro set <pemain> song namafile.nbs`.
Pastikan kamu punya hak pakai lagunya.

## Versi
Satu jar untuk Spigot/Paper 1.13 sampai terbaru (nama sound/particle ditangani otomatis).
Penonton dikunci ke kamera lewat mode spectator; posisi dan gamemode mereka dikembalikan
setelah adegan selesai.
