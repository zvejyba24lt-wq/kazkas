# Music

The lake plays one quiet music loop under the nature sounds (`assets/music/lake.ogg`,
volume 0.16, stops with the sound switch and in the background).

| File | Track | Author | Licence | Source |
| --- | --- | --- | --- | --- |
| `music/lake.ogg` | Sunset Plains | Yoiyami | CC0 1.0 (public domain, no attribution required) | https://opengameart.org/content/sunset-plains |

CI downloads the original WAV and converts it to Ogg Vorbis before the build
(`.github/workflows/android.yml`, step "Music"), so the 63 MB WAV is not kept in git.
If the download fails the app builds without music.
