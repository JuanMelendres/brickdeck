#!/usr/bin/env python3
"""Build the Phase 7 render smoke-test set from Rebrickable.

Ground truth is correct by construction: we ask Rebrickable which element
renders a given (part, color), so the label comes from the same namespace
the classifier must hit. Writes images + ground_truth.csv.

Throwaway POC scaffolding - see the Phase 7 spike, section 11.
"""
import csv
import os
import sys
import time
import urllib.request

BASE = os.environ["REBRICKABLE_BASE_URL"]
KEY = os.environ["REBRICKABLE_API_KEY"]
OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "eval_set")
CDN = "https://cdn.rebrickable.com/media/parts/elements/{}.jpg"

# (part_num, color_id, difficulty, why this one is here)
TARGETS = [
    # easy - unambiguous, common, distinct silhouettes
    ("3001", 4,   "easy",   "Brick 2x4, red - the canonical brick"),
    ("3023", 15,  "easy",   "Plate 1x2, white"),
    ("3004", 1,   "easy",   "Brick 1x2, blue"),
    ("3003", 14,  "easy",   "Brick 2x2, yellow"),
    ("3010", 0,   "easy",   "Brick 1x4, black"),
    # medium - shape detail matters
    ("3040", 4,   "medium", "Slope 45 2x1, red"),
    ("3069b", 71, "medium", "Tile 1x2, light bluish gray"),
    ("3673", 7,   "medium", "Technic pin, light gray - small, easily confused"),
    ("3794a", 15, "medium", "Plate 1x2 with 1 stud (jumper), white"),
    ("3062b", 4,   "medium", "Round brick 1x1, red - tiny"),
    ("2431", 71,  "medium", "Tile 1x4, light bluish gray"),
    ("3665", 0,   "medium", "Slope inverted 45 2x1, black"),
    # hard - near-identical part variants (the real test)
    ("3020", 2,   "hard",   "Plate 2x4, green - vs 3021 below"),
    ("3021", 2,   "hard",   "Plate 2x3, green - one stud from 3020"),
    ("3022", 2,   "hard",   "Plate 2x2, green - same family again"),
    # hard - near-identical colors
    ("3001", 320, "hard",   "Brick 2x4, DARK red - vs red 3001 above"),
    ("3001", 5,   "hard",   "Brick 2x4, dark pink-ish - red-adjacent"),
    # hard - translucent (lighting/material edge case)
    ("3005", 47,  "hard",   "Brick 1x1, trans-clear"),
    ("3005", 41,  "hard",   "Brick 1x1, trans-light blue"),
    # hard - printed/curved
    ("3039", 15,  "hard",   "Slope 45 2x2, white"),
]


def get(url):
    req = urllib.request.Request(url, headers={"Authorization": f"key {KEY}"})
    with urllib.request.urlopen(req, timeout=20) as r:
        import json
        return json.load(r)


def main():
    os.makedirs(OUT, exist_ok=True)
    rows, misses = [], []

    for part, color_id, difficulty, note in TARGETS:
        try:
            data = get(f"{BASE}/lego/parts/{part}/colors/{color_id}/")
        except Exception as e:
            misses.append((part, color_id, f"api: {e}"))
            continue

        elements = data.get("elements") or []
        if not elements:
            misses.append((part, color_id, "no element id"))
            continue

        element = elements[0]
        img_url = CDN.format(element)
        fname = f"{element}.jpg"
        try:
            urllib.request.urlretrieve(img_url, os.path.join(OUT, fname))
        except Exception as e:
            misses.append((part, color_id, f"cdn: {e}"))
            continue

        rows.append({
            "filename": fname,
            "element_id": element,
            "part_num": part,
            "color_id": color_id,
            "difficulty": difficulty,
            "note": note,
        })
        print(f"  ok  {fname:<12} part={part:<7} color={color_id:<4} {difficulty:<7} {note}")
        time.sleep(0.6)  # be polite to the API

    with open(os.path.join(OUT, "ground_truth.csv"), "w", newline="") as f:
        w = csv.DictWriter(f, fieldnames=["filename", "element_id", "part_num", "color_id", "difficulty", "note"])
        w.writeheader()
        w.writerows(rows)

    print(f"\n{len(rows)} images -> {OUT}")
    if misses:
        print(f"\n{len(misses)} misses (fix or drop):")
        for m in misses:
            print(f"  part={m[0]} color={m[1]}: {m[2]}")


if __name__ == "__main__":
    sys.exit(main())
