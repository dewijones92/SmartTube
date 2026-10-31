#!/usr/bin/env python3
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / "common/src/main/res/drawable-nodpi/icon_moomoo.png"
SIZE, SCALE = 301, 4
WHITE = (255, 255, 255, 255)


def main():
    s = SIZE * SCALE
    img = Image.new("RGBA", (s, s), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    u = s / 301
    stroke = round(24 * u)

    d.ellipse([46 * u, 62 * u, 255 * u, 271 * u], outline=WHITE, width=stroke)
    d.arc([132 * u, 18 * u, 186 * u, 72 * u], start=100, end=340, fill=WHITE, width=round(18 * u))

    for cx in (115, 186):
        d.ellipse([(cx - 16) * u, 134 * u, (cx + 16) * u, 166 * u], fill=WHITE)

    d.ellipse([112 * u, 192 * u, 189 * u, 222 * u], fill=WHITE)
    d.ellipse([136 * u, 207 * u, 165 * u, 244 * u], outline=WHITE, width=round(12 * u))

    img.resize((SIZE, SIZE), Image.LANCZOS).save(OUT)
    print("written", OUT)


if __name__ == "__main__":
    main()
