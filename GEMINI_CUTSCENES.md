# Gemini Cutscene Prompts

The engine shows a portrait card during cutscenes: drop PNGs into
`app/src/main/assets/cutscenes/` named **SUN.png, YU.png, ADA.png, TERN.png,
CASQ.png, MOM.png** (~768×1024, dark background — the card floats on space).
Generate stills with the prompts below; then, as you suggested, feed the same
stills back to Gemini to animate short loops for social/trailer use.

**Shared style suffix (append to every prompt):**
> …digital painting, cinematic sci-fi character portrait, chest-up, dramatic
> rim lighting in teal and magenta against deep space black, subtle film
> grain, painterly detail, no text, no watermark.

- **SUN.png** — "A determined 19-year-old woman pilot with warm brown skin and
  short wind-cut dark hair, wearing a patched salvage flight jacket with a
  small comet pin, eyes fierce and young, faint reflection of a targeting
  reticle in her iris…"
- **YU.png** — "A cheerful Chinese woman in her 40s, chief mechanic, grease
  smudge on one cheek, braided bun with a stylus through it, tool harness over
  a quilted work vest, kind knowing smile…"
- **ADA.png** — "A regal Nigerian woman scientist in her 50s, silver-flecked
  coiled hair, elegant Ankara-patterned collar under a lab overcoat, holding a
  glowing holographic double helix that branches like a river delta…"
- **TERN.png** — "An abstract ship AI avatar: a serene geometric tern-bird of
  soft light lines and slow particles, no face, hovering over a console…"
- **CASQ.png** — "An aristocratic imperial officer in his 60s, immaculate
  bone-white uniform with silver helix insignia, cold gray eyes, handsome and
  hollow, faint black-hole accretion glow behind him…"
- **MOM.png** — "A weary but unbroken woman geneticist in her 50s, resemblance
  to SUN.png, captive's gray jumpsuit, defiant gentle eyes, hands cuffed
  around a hidden data crystal that glows between her fingers…"

### Animation passes (feed each still back to Gemini)
- SUN: "Subtle idle: hair drifts in zero-g, reticle glints across her eye,
  slow breath." (loop 4s)
- ADA: "The holographic helix rotates and branches; she looks up and smiles."
- CASQ: "Accretion disk behind him slowly rotates; his jaw tightens."
- Key story beats worth full shots: the Freeport raid (m01), sparing the
  pilot in the corona (m05), the Variance convoy launching (m11), the
  broadcast washing over the fleet (m12) — compose stills from the portrait
  style, then animate with slow push-ins.
