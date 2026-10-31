#!/usr/bin/env python3
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / "common/src/main/res/drawable-nodpi"
SCALE = 4
WHITE = (255, 255, 255, 255)
CLEAR = (0, 0, 0, 0)


def canvas(size):
    s = size * SCALE
    img = Image.new("RGBA", (s, s), CLEAR)
    return img, ImageDraw.Draw(img), s / 301


def save(img, size, name):
    img.resize((size, size), Image.LANCZOS).save(OUT / name)
    print("written", name)


def tennis():
    img, d, u = canvas(301)
    stroke = round(24 * u)
    seams = Image.new("RGBA", img.size, CLEAR)
    sd = ImageDraw.Draw(seams)
    sd.arc([-70 * u, 40 * u, 118 * u, 262 * u], start=-60, end=60, fill=WHITE, width=round(20 * u))
    sd.arc([183 * u, 40 * u, 371 * u, 262 * u], start=120, end=240, fill=WHITE, width=round(20 * u))
    mask = Image.new("L", img.size, 0)
    ImageDraw.Draw(mask).ellipse([52 * u, 52 * u, 249 * u, 249 * u], fill=255)
    img.paste(seams, (0, 0), Image.composite(seams.getchannel("A"), Image.new("L", img.size, 0), mask))
    d.ellipse([46 * u, 46 * u, 255 * u, 255 * u], outline=WHITE, width=stroke)
    save(img, 301, "icon_tennis.png")


def continue_watching():
    img, d, u = canvas(301)
    stroke = round(24 * u)
    d.arc([46 * u, 46 * u, 255 * u, 255 * u], start=-40, end=270, fill=WHITE, width=stroke)
    d.polygon([(150 * u, 22 * u), (150 * u, 82 * u), (196 * u, 52 * u)], fill=WHITE)
    d.polygon([(126 * u, 104 * u), (126 * u, 197 * u), (204 * u, 150 * u)], fill=WHITE)
    save(img, 301, "icon_continue.png")


def focus_settings():
    img, d, u = canvas(302)
    d.ellipse([40 * u, 70 * u, 222 * u, 252 * u], fill=WHITE)
    d.ellipse([96 * u, 44 * u, 256 * u, 204 * u], fill=CLEAR)
    for cx, cy, r in ((236, 60, 13), (270, 120, 8), (206, 28, 7)):
        d.ellipse([(cx - r) * u, (cy - r) * u, (cx + r) * u, (cy + r) * u], fill=WHITE)
    save(img, 302, "settings_focus.png")


if __name__ == "__main__":
    tennis()
    continue_watching()
    focus_settings()
