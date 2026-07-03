# Voice Casting — Fish Audio search criteria

Browse https://fish.audio/discovery (or use `GET /model?title=<term>`), audition
with the line given, then paste the chosen model ids into `app/tools/fish.config`
and run `python3 app/tools/generate_dialogue.py`. Alternatively design each
voice with the `voice-design-1` endpoint using the "design prompt" below.

## SUN — Sunsarra Vex
- **Search terms:** "young woman determined", "female protagonist teen", "fierce young female", "tomboy heroine"
- **Qualities:** 18–22, light grit, quick, emotional range from shaking anger to steady command; NOT breathy-cute
- **Design prompt:** "A 19-year-old woman salvage pilot: bright, slightly husky voice that cracks when angry and steadies when she takes command; working-class spacer accent; fearless but young."
- **Audition:** m01_in2 — "They took my mother, Yu…"

## YU — Chen Yumei
- **Search terms:** "chinese accent english warm", "mandarin english female", "middle aged chinese woman", "auntie warm"
- **Qualities:** 40s, warm Mandarin-inflected English, practical, chuckles easily, iron under the warmth
- **Design prompt:** "A Chinese chief mechanic in her 40s speaking warm Mandarin-accented English: cheerful, grounded, drops proverbs, gets steely in emergencies."
- **Audition:** m01_in3 — "Rage second, people first."

## ADA — Dr. Adaeze Obi
- **Search terms:** "nigerian accent english female", "african woman professor", "west african english", "authoritative african female"
- **Qualities:** 50s, resonant Nigerian-English, professorial thunder, delight in ideas, devastating calm in debate
- **Design prompt:** "A Nigerian evolutionary biologist in her 50s: rich, musical West-African English; a professor's cadence that can turn courtroom-devastating; joy when explaining science."
- **Audition:** m10_out2 — "Noise is how life sings, Selector."

## TERN — ship AI
- **Search terms:** "calm ai voice", "neutral android assistant", "soft robotic female", "computer voice measured"
- **Qualities:** genderless-leaning, precise, faint warmth that grows across the campaign; no vocoder cheese
- **Design prompt:** "A ship's AI: calm, precise, quietly wry, androgynous; hints of growing warmth; crisp consonants, no robotic distortion."
- **Audition:** m11_in1 — "They chose their own name — the Variance."

## CASQ — High Selector Casque
- **Search terms:** "aristocratic villain", "cold cultured male", "british villain deep", "elegant menace"
- **Qualities:** 50s–60s, silken, cultured, absolute certainty curdling into hollowness by m12
- **Design prompt:** "An aristocratic imperial eugenicist: velvet baritone, perfect diction, condescension as courtesy; by the end, hollow disbelief."
- **Audition:** m10_in2 — "Join your betters, child."

## MOM — Dr. Maren Vex
- **Search terms:** "mature woman warm tired", "mother figure voice", "female scientist middle aged"
- **Qualities:** 50s, weary but unbroken, fierce tenderness
- **Design prompt:** "A brilliant geneticist held captive: tired, warm alto; every word deliberate; fierce love under exhaustion."
- **Audition:** m12_in3 — "Don't avenge me — FINISH the proof."

**Rendering:** all 82 lines carry free-form `[bracket]` direction for S2.1 Pro.
Keep `temperature≈0.88`. Radio barks and story lines share the same voice ids.
