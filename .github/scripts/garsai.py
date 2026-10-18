"""Records every text in garsai/tekstai.txt with the Ona voice into app/src/main/assets/audio.

Existing recordings are kept (each file name is a hash of its text), and recordings no longer
listed are removed, so only new or changed texts are synthesised.
"""
import asyncio
import os
import sys

import edge_tts

VOICE = "lt-LT-OnaNeural"
RATE = "-20%"
OUT = "app/src/main/assets/audio"


async def record(text, path):
    for attempt in range(5):
        try:
            await edge_tts.Communicate(text, VOICE, rate=RATE).save(path)
            if os.path.getsize(path) > 0:
                return
        except Exception as e:  # network hiccups: wait and retry
            print(f"retry {attempt + 1} for {text!r}: {e}", file=sys.stderr)
        await asyncio.sleep(2 * (attempt + 1))
    raise RuntimeError(f"could not record {text!r}")


async def main():
    os.makedirs(OUT, exist_ok=True)
    wanted = {}
    with open("garsai/tekstai.txt", encoding="utf-8") as f:
        for line in f:
            line = line.rstrip("\n")
            if line:
                key, text = line.split("|", 1)
                wanted[key] = text
    for name in os.listdir(OUT):
        if name.endswith(".mp3") and name[:-4] not in wanted:
            os.remove(os.path.join(OUT, name))
    todo = [(k, t) for k, t in wanted.items() if not os.path.exists(os.path.join(OUT, k + ".mp3"))]
    print(f"{len(wanted)} texts, {len(todo)} to record")
    for i, (key, text) in enumerate(todo, 1):
        await record(text, os.path.join(OUT, key + ".mp3"))
        if i % 50 == 0:
            print(f"{i}/{len(todo)}")


asyncio.run(main())
