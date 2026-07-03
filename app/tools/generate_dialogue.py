#!/usr/bin/env python3
"""
Renders every line of assets/script.json with Fish Audio S2.1 Pro.
Cast the five voices first — see VOICE_CASTING.md — then put their model ids
in app/tools/fish.config (gitignored):

    API_KEY=...
    SUN_VOICE=...   YU_VOICE=...   ADA_VOICE=...
    TERN_VOICE=...  CASQ_VOICE=... MOM_VOICE=...

    python3 app/tools/generate_dialogue.py [--force]
"""
import argparse, json, os, pathlib, sys, time
import requests
HERE = pathlib.Path(__file__).resolve().parent
APP = HERE.parent
OUT = APP/"src/main/assets/voice"
CFG = {}
cf = HERE/"fish.config"
if cf.exists():
    CFG = dict(l.split("=",1) for l in cf.read_text().splitlines()
               if "=" in l and not l.strip().startswith("#"))
KEY = os.environ.get("FISH_API_KEY", CFG.get("API_KEY",""))
VOICES = {s: CFG.get(f"{s}_VOICE","") for s in ("SUN","YU","ADA","TERN","CASQ","MOM")}
CHAIN = list(dict.fromkeys([CFG.get("MODEL","s2.1-pro"),"s2.1-pro","s2.1-pro-free","s2-pro"]))
ACTIVE = [CHAIN[0]]

def synth(id_, sp, text, force):
    out = OUT/f"{id_}.ogg"
    if out.exists() and not force: return "skip"
    vid = VOICES.get(sp,"")
    if not vid:
        print(f"  ! {id_}: no voice id for {sp} (see VOICE_CASTING.md)"); return "fail"
    body = {"text": text, "reference_id": vid, "format": "opus", "opus_bitrate": 48000,
            "temperature": 0.88, "top_p": 0.85, "normalize": True,
            "prosody": {"speed": 1.0, "volume": 0, "normalize_loudness": True}}
    for attempt in range(3):
        model = ACTIVE[0]
        try:
            r = requests.post("https://api.fish.audio/v1/tts",
                headers={"Authorization": f"Bearer {KEY}",
                         "Content-Type": "application/json", "model": model},
                json=body, timeout=300)
        except requests.RequestException as e:
            print(f"  ! {id_} net {e}"); time.sleep(2**attempt); continue
        if r.status_code == 200 and r.content[:4] == b"OggS":
            OUT.mkdir(parents=True, exist_ok=True)
            out.write_bytes(r.content); print(f"  + {id_}.ogg [{sp}/{model}]"); return "ok"
        if r.status_code in (400,402,404) and CHAIN.index(model)+1 < len(CHAIN):
            ACTIVE[0] = CHAIN[CHAIN.index(model)+1]; continue
        if r.status_code == 429: time.sleep(4*(attempt+1)); continue
        print(f"  ! {id_} {r.status_code} {r.text[:100]}"); time.sleep(1)
    return "fail"

def main():
    ap = argparse.ArgumentParser(); ap.add_argument("--force", action="store_true")
    args = ap.parse_args()
    if not KEY: sys.exit("No API key (fish.config or FISH_API_KEY)")
    data = json.loads((APP/"src/main/assets/script.json").read_text())["lines"]
    ok=skip=fail=0
    for id_, o in data.items():
        r = synth(id_, o["s"], o["t"], args.force)
        ok += r=="ok"; skip += r=="skip"; fail += r=="fail"
    print(f"\n{ok} rendered, {skip} skipped, {fail} failed.")

if __name__ == "__main__":
    main()
