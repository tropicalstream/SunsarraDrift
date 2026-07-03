#!/usr/bin/env python3
"""Sunsarra Drift combat SFX — numpy -> WAV -> ffmpeg -> Ogg."""
import pathlib, subprocess, tempfile, wave
import numpy as np
APP = pathlib.Path(__file__).resolve().parent.parent
SFX = APP / "src/main/assets/sfx"
SR = 44100
rng = np.random.default_rng(77)
def t(d): return np.arange(int(SR * d)) / SR
def env(s, a=0.004, r=0.08):
    n = len(s); e = np.ones(n)
    na = max(1, min(int(SR*a), n//2)); nr = max(1, min(int(SR*r), n-na))
    e[:na] = np.linspace(0,1,na); e[-nr:] = np.linspace(1,0,nr)
    return s*e
def norm(s, p=0.8):
    m = np.max(np.abs(s)) or 1.0
    return s/m*p
def sweep(f0, f1, d):
    tt = t(d); f = f0*(f1/f0)**(tt/d)
    return np.sin(2*np.pi*np.cumsum(f)/SR)
def save(name, s):
    SFX.mkdir(parents=True, exist_ok=True)
    pcm = (np.clip(s,-1,1)*32767).astype(np.int16)
    with tempfile.NamedTemporaryFile(suffix=".wav", delete=False) as f: w = f.name
    with wave.open(w,"wb") as wf:
        wf.setnchannels(1); wf.setsampwidth(2); wf.setframerate(SR); wf.writeframes(pcm.tobytes())
    out = SFX/f"{name}.ogg"
    subprocess.check_call(["ffmpeg","-y","-loglevel","error","-i",w,"-c:a","libvorbis","-q:a","3",str(out)])
    pathlib.Path(w).unlink(); print(" +", out.name)

save("pulse", norm(env(sweep(1600, 500, 0.10) + 0.4*sweep(3200, 900, 0.10), 0.002, 0.05)))
d=0.18; tt=t(d); save("impact", norm(env(rng.standard_normal(len(tt))*np.exp(-tt*26) + np.sin(2*np.pi*220*tt)*np.exp(-tt*18))))
d=1.0; tt=t(d); save("explosion", norm(env(rng.standard_normal(len(tt))*np.exp(-tt*5) + np.sin(2*np.pi*48*tt)*np.exp(-tt*3)*1.3, 0.002, 0.35), 0.9))
save("boltby", norm(env(sweep(900, 300, 0.22)*0.7, 0.01, 0.1)))
d=0.35; tt=t(d); save("shieldhit", norm(env(np.sin(2*np.pi*300*tt)*np.exp(-tt*9) + 0.5*rng.standard_normal(len(tt))*np.exp(-tt*20))))
save("lockon", norm(env(np.concatenate([sweep(900,900,0.06), np.zeros(int(SR*0.04)), sweep(1300,1300,0.08)]), 0.003, 0.04), 0.7))
d=0.8; tt=t(d); save("missile", norm(env(sweep(200, 900, d)*0.5 + rng.standard_normal(len(tt))*np.exp(-tt*2)*0.6, 0.01, 0.3)))
d=1.1; tt=t(d); save("emp", norm(env(sweep(2400, 60, d) + 0.5*np.sin(2*np.pi*50*tt)*np.exp(-tt*2), 0.002, 0.4), 0.85))
save("deny", norm(env(sweep(300, 220, 0.12), 0.004, 0.06), 0.5))
save("uiswitch", norm(env(sweep(700, 1000, 0.06), 0.003, 0.04), 0.55))
d=0.5; tt=t(d); save("gate", norm(env(np.sin(2*np.pi*880*tt)*np.exp(-tt*6) + np.sin(2*np.pi*1320*tt)*np.exp(-tt*8)*0.6, 0.004, 0.2), 0.7))
save("gatemiss", norm(env(sweep(500, 160, 0.3), 0.005, 0.15), 0.65))
d=1.0; tt=t(d); save("boost", norm(env(sweep(90, 500, d)*0.8 + rng.standard_normal(len(tt))*np.exp(-tt*3)*0.4, 0.02, 0.3)))
d=1.4; tt=t(d); sig=np.zeros(len(tt))
for k in (0.0, 0.5, 1.0):
    i=int(k*SR); seg=t(0.4)
    seg = seg[:len(sig)-i]
    sig[i:i+len(seg)] += np.sin(2*np.pi*640*seg)*np.sin(np.pi*seg/0.4)*0.6
save("alarm", norm(sig, 0.7))
d=0.3; tt=t(d); save("weld", norm(env(rng.standard_normal(len(tt))*(0.5+0.5*np.sin(tt*300))*np.exp(-tt*6) + np.sin(2*np.pi*2400*tt)*0.2, 0.004, 0.1), 0.6))
d=0.6; tt=t(d); sig=np.zeros(len(tt))
for k,f in [(0.0,660),(0.15,880)]:
    i=int(k*SR); seg=t(0.3)
    seg = seg[:len(sig)-i]
    sig[i:i+len(seg)] += np.sin(2*np.pi*f*seg)*np.exp(-seg*7)*0.6
save("fixed", norm(sig, 0.65))
d=2.0; tt=t(d); save("fuelflow", norm(env((rng.standard_normal(len(tt))*0.4 + np.sin(2*np.pi*120*tt)*0.3)*(0.7+0.3*np.sin(tt*9)), 0.1, 0.4), 0.5))
d=1.6; tt=t(d); sig=np.zeros(len(tt))
for k,f in [(0.0,523),(0.16,659),(0.32,784),(0.48,1046),(0.72,784),(0.88,1046)]:
    i=int(k*SR); seg=t(0.4)
    seg = seg[:len(sig)-i]
    sig[i:i+len(seg)] += np.sin(2*np.pi*f*seg)*np.exp(-seg*4)*0.5
save("winsting", norm(sig, 0.8))
save("failsting", norm(env(sweep(660, 82, 1.5) + 0.4*sweep(330, 41, 1.5), 0.01, 0.5), 0.75))
save("menu", norm(env(sweep(1000, 900, 0.05), 0.003, 0.03), 0.5))
print("Done.")
