# SUNSARRA DRIFT

A first-person spaceship shooter for the **RayNeo X3 Pro**, built on Project
Pale Blue's galaxy engine (same planets, HYG starfield, dust, rocks, rails) —
but this time the solar system is a war you can end with the truth.

**Story:** Sunsarra Vex hunts the Confederate Empire — kidnappers of her
mother, preachers of eugenics — from Earth orbit to Sagittarius A*, with Yu
(Chinese master mechanic) and Dr. Adaeze Obi (Nigerian evolutionary biologist
who fights their creed with Gould and Lewontin). Le Guin walls, Sagan finale,
Asimov long game, Clarke artifact. Full bible: **STORY.md**.

## Three seats, three games (head + tap + swipe fwd/back ONLY)
| Seat | Head | Tap | Swipe fwd/back |
| --- | --- | --- | --- |
| **GUNNER** | aim reticle | fire | cycle PULSE / MISSILE (lock-on) / EMP |
| **PILOT** | steer through gates | boost | throttle up / down |
| **ENGINEER** | look at a station | repair (3 taps) | open/close fuel flow to allies |

Double-tap = settings (restart, subtitles, volumes, invert steer, recenter,
abandon). 12 missions retrace the Pale Blue tour route; each is an homage
(named on the briefing card) to a classic mission from film, print, tabletop
or games. Win/loss, unlocks, and progress are saved.

## Building the audio/visual assets
1. **SFX** — already committed (regenerate: `python3 app/tools/generate_sfx.py`).
2. **Voices** — cast six Fish voices with **VOICE_CASTING.md**, fill
   `app/tools/fish.config`, then `python3 app/tools/generate_dialogue.py`
   (82 lines → `assets/voice/`). Captions work before the audio exists.
3. **Music** — generate with **SUNO_PROMPTS.md**, drop MP3s in `assets/music/`.
4. **Cutscene portraits** — **GEMINI_CUTSCENES.md**, PNGs in `assets/cutscenes/`.

## Build
Android Studio (AGP 8.7.3 / Kotlin 2.0.21 / JDK 17) or
`gradle wrapper --gradle-version 8.9 && ./gradlew assembleDebug`.
Optional RayNeo AARs in `app/libs/`. Screen touches mirror the temple pad.

## Repo
Own directory, own git repository, branch **`sunsarra-drift`**.
