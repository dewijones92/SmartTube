#!/usr/bin/env python3
import colorsys
import sys
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[2]
SRC = ROOT / "smarttubetv/src/main/res/mipmap-nodpi/app_icon.png"
OUT = ROOT / "smarttubetv/src/stfocus/res"
FONT = "/usr/share/fonts/truetype/liberation/LiberationSansNarrow-Italic.ttf"

BRAND = (0x10, 0xA3, 0x7F)
DARK = (0x1C, 0x1C, 0x1C)
BAND_TOP, BADGE_BOTTOM = 169, 243
LABEL = "focus"


def recolour(img, rgb):
    th, ts, tv = colorsys.rgb_to_hsv(*(c / 255 for c in rgb))
    px = img.load()
    for y in range(img.height):
        for x in range(img.width):
            r, g, b, a = px[x, y]
            h, s, v = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
            if s > 0.25:
                nr, ng, nb = colorsys.hsv_to_rgb(th, ts, min(1.0, v * tv / 0.651))
                px[x, y] = (round(nr * 255), round(ng * 255), round(nb * 255), a)
    return img


def master(colour):
    img = recolour(Image.open(SRC).convert("RGBA"), colour)

    band = Image.new("RGBA", img.size, (0, 0, 0, 0))
    ImageDraw.Draw(band).rectangle([0, BAND_TOP, img.width, BADGE_BOTTOM], fill=(0, 0, 0, 128))
    badge_alpha = img.getchannel("A").point(lambda a: 255 if a > 200 else 0)
    band.putalpha(Image.composite(band.getchannel("A"), Image.new("L", img.size, 0), badge_alpha))
    img = Image.alpha_composite(img, band)

    draw = ImageDraw.Draw(img)
    font = ImageFont.truetype(FONT, 66)
    left, top, right, bottom = draw.textbbox((0, 0), LABEL, font=font)
    x = (img.width - (right - left)) / 2 - left
    y = BADGE_BOTTOM - 10 - bottom
    draw.text((x, y), LABEL, font=font, fill=(255, 255, 255, 255))
    return img


def place(img, canvas_size, scale, offset, bg=(0, 0, 0, 0)):
    canvas = Image.new("RGBA", canvas_size, bg)
    w, h = round(img.width * scale), round(img.height * scale)
    canvas.alpha_composite(img.resize((w, h), Image.LANCZOS), offset)
    return canvas


def main():
    m = master(BRAND)
    nodpi = OUT / "mipmap-nodpi"
    v30 = OUT / "mipmap-nodpi-v30"
    nodpi.mkdir(parents=True, exist_ok=True)
    v30.mkdir(parents=True, exist_ok=True)

    m.save(nodpi / "app_icon.png")
    place(m, (320, 320), 0.8, (32, 48)).save(v30 / "app_icon.png")
    place(m, (640, 360), 1.0, (160, 21), bg=(52, 52, 52, 255)).convert("RGB").save(nodpi / "app_banner.png")

    logo = place(m, (180, 180), 180 / 320, (0, 0))
    logo.save(nodpi / "app_logo.png")
    logo.save(nodpi / "app_logo_semi_red.png")
    place(master(DARK), (180, 180), 180 / 320, (0, 0)).convert("LA").save(nodpi / "app_logo_semi_grey.png")
    print("written to", OUT, file=sys.stderr)


if __name__ == "__main__":
    main()
