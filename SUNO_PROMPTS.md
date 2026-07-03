# Suno Music Prompts

Generate each track, export MP3, and drop it into `app/src/main/assets/music/`
with the exact filename shown. Loops matter more than endings — ask Suno for
seamless/loopable outros or trim in an editor. ~2:30–3:30 each.

### `title.mp3` — MAIN THEME (with lyrics · use Suno **Custom Mode**)

The title screen plays a full shot-listed film cut to this song
(`TitleCinematic.kt`): Luna/earthlight (V1) → the abduction lance (V2) →
sunrise burn with the convoy (Chorus) → nebula, nav-gates, hull beauty pass
(V3) → warp rush (Chorus) → the pale blue look-back (Outro). Karaoke captions
are timed in-engine. **Aim for ~2:40 total with sections near:
V1 0:00 · V2 0:26 · Chorus 0:52 · V3 1:18 · Chorus 1:44 · Outro 2:10.**
If your render lands elsewhere, nudge the section constants and the LYRICS
timestamps at the bottom of `TitleCinematic.kt`.

**Style prompt:**
> Epic cinematic space-opera ballad, young female lead vocal — clear, slightly
> husky, wounded but rising — over deep analog synth pads, slow-burn orchestral
> swells and a heartbeat kick; verses intimate and floating, choruses lifting
> into soaring anthem with layered choir harmonies on "us all"; final outro
> stripped to voice and one warm pad, resolving in radiant major; ~98 BPM,
> Interstellar organ meets Vangelis warmth meets modern trailer-folk.

**Lyrics (paste verbatim):**
```
[Verse 1]
I was born where the engines sing
Under borrowed light and broken rings
Mama said, child, keep your name
When the maps go dark and the stars catch flame

[Verse 2]
They took her voice beyond the moon
Locked the truth in a silver room
But every scar and every spark
Is a lantern burning through the dark

[Chorus]
Drift, drift toward morning
Past the fire, past the warning
If the night says we are small
We will answer with us all

[Verse 3]
Yu keeps faith in the turning gears
Ada names what the tyrant fears
Tern draws paths through dust and rain
And I fly on through love and pain

[Chorus]
Drift, drift toward morning
Past the fire, past the warning
If the night says we are small
We will answer with us all

[Outro]
One blue world behind me
One true voice to find
I am not alone here
I carry humankind
```

### `act1_battle.mp3` — gunner combat (missions 1, 4, 7, 10)
> Driving hybrid-orchestral space combat, pounding taiko and anvil percussion,
> ostinato strings, analog synth arps, heroic brass stabs, 128 BPM,
> instrumental, loopable, Battlestar Galactica drums meet synthwave.

### `act1_run.mp3` — pilot runs (missions 2, 8)
> High-velocity chase, motorik drums, arpeggiated synths climbing, breathless
> strings, canyon-run adrenaline with moments of weightless glide, 140 BPM,
> instrumental, seamless loop, Star Fox meets M83.

### `act2_tension.mp3` — engineer crises (missions 3, 6)
> Claustrophobic slow-burn tension, ticking percussion like failing machinery,
> low cello drones, sparse piano, distant klaxon pitched musically, 90 BPM,
> instrumental, loopable, Apollo 13 re-entry silence meets The Expanse.

### `act3_inferno.mp3` — the Corona Run (mission 5)
> Blazing sun-flight, shimmering high strings tremolo, massive low brass swells
> like solar flares, choir vowels, relentless forward pulse, 120 BPM,
> instrumental, loopable, Sunshine soundtrack grandeur.

### `act5_wonder.mp3` — the Silent Cylinder (mission 9)
> Sacred awe in deep space, slow glass-harmonic textures, sub-bass heartbeat,
> sparse kalimba echoes, vast reverb, alien but gentle, 60 BPM, instrumental,
> loopable, 2001 stargate serenity without dissonance.

### `act6_liberation.mp3` — the Variance convoy (mission 11)
> Uplifting escape anthem, Afrobeat-inflected percussion under soaring
> orchestra, hand claps, women's choir rising, joyful defiance, 116 BPM,
> instrumental, loopable, triumph of many voices in harmony.

### `act7_finale.mp3` — Ascension Engine (mission 12)
> Final confrontation at a black hole, colossal low-end gravity, heartbeat
> percussion, the main-title vocalise returning transformed and victorious,
> tension resolving into radiant major cadence, 100 BPM, instrumental,
> loopable ending into calm.
