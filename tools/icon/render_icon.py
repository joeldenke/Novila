#!/usr/bin/env python3
"""Renders the adaptive launcher icon's vector drawables to the raster fallbacks.

Android draws the adaptive icon (mipmap-anydpi) from the vector drawables in app/src/main/res/drawable. Tools that
read the APK without Android, like Firebase App Distribution, cannot draw an adaptive icon and look for raster
mipmaps instead. This script renders the same vectors to WebP in mipmap-*dpi, so both show the same icon, plus the
512 px PNG for Google Play. Run it after changing ic_launcher_background.xml or ic_launcher_foreground.xml:

    python3 tools/icon/render_icon.py

Needs only Pillow. It understands the subset of VectorDrawable the icon uses: paths with M, L, H, V, C, A and Z
(absolute), solid or linear/radial gradient fills (even-odd, or non-zero for subpaths that do not overlap),
round-capped strokes and fill/stroke alpha.
"""

import math
import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

from PIL import Image, ImageChops, ImageDraw

ROOT = Path(__file__).resolve().parents[2]
RES = ROOT / "app" / "src" / "main" / "res"
ANDROID = "{http://schemas.android.com/apk/res/android}"
VIEWPORT = 108.0
# The part of the 108 dp adaptive icon a launcher shows: the 72 dp in the middle.
VISIBLE = 72.0
SUPERSAMPLE = 4
DENSITIES = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}


def color(value):
    value = value.lstrip("#")
    if len(value) == 6:
        value = "FF" + value
    a, r, g, b = (int(value[i : i + 2], 16) for i in range(0, 8, 2))
    return r, g, b, a


def tokens(data):
    return re.findall(r"[A-Za-z]|-?\d*\.?\d+(?:[eE][-+]?\d+)?", data)


def arc(x1, y1, rx, ry, phi, large, sweep, x2, y2):
    """Points along an SVG elliptical arc (endpoint parameterization, F.6.5 of the SVG spec)."""
    phi = math.radians(phi)
    cos, sin = math.cos(phi), math.sin(phi)
    dx, dy = (x1 - x2) / 2, (y1 - y2) / 2
    xp, yp = cos * dx + sin * dy, -sin * dx + cos * dy
    lam = (xp * xp) / (rx * rx) + (yp * yp) / (ry * ry)
    if lam > 1:
        rx, ry = rx * math.sqrt(lam), ry * math.sqrt(lam)
    num = rx * rx * ry * ry - rx * rx * yp * yp - ry * ry * xp * xp
    den = rx * rx * yp * yp + ry * ry * xp * xp
    coef = math.sqrt(max(0.0, num / den)) * (-1 if large == sweep else 1)
    cxp, cyp = coef * rx * yp / ry, -coef * ry * xp / rx
    cx = cos * cxp - sin * cyp + (x1 + x2) / 2
    cy = sin * cxp + cos * cyp + (y1 + y2) / 2

    def angle(ux, uy, vx, vy):
        a = math.atan2(ux * vy - uy * vx, ux * vx + uy * vy)
        return a

    t1 = angle(1, 0, (xp - cxp) / rx, (yp - cyp) / ry)
    dt = angle((xp - cxp) / rx, (yp - cyp) / ry, (-xp - cxp) / rx, (-yp - cyp) / ry)
    if not sweep and dt > 0:
        dt -= 2 * math.pi
    elif sweep and dt < 0:
        dt += 2 * math.pi
    steps = max(8, int(abs(dt) * 32))
    points = []
    for i in range(1, steps + 1):
        t = t1 + dt * i / steps
        x, y = rx * math.cos(t), ry * math.sin(t)
        points.append((cos * x - sin * y + cx, sin * x + cos * y + cy))
    return points


def subpaths(data):
    """The path as polylines, one per subpath, and whether each is closed."""
    t = tokens(data)
    i = 0
    result, current, closed = [], [], False
    x = y = 0.0
    cmd = None

    def num():
        nonlocal i
        i += 1
        return float(t[i - 1])

    while i < len(t):
        if re.match(r"[A-Za-z]", t[i]):
            cmd = t[i]
            i += 1
        if cmd == "M":
            if current:
                result.append((current, closed))
            x, y = num(), num()
            current, closed = [(x, y)], False
            cmd = "L"
        elif cmd == "L":
            x, y = num(), num()
            current.append((x, y))
        elif cmd == "H":
            x = num()
            current.append((x, y))
        elif cmd == "V":
            y = num()
            current.append((x, y))
        elif cmd == "C":
            x1, y1, x2, y2, x3, y3 = (num() for _ in range(6))
            for s in range(1, 25):
                u = s / 24
                bx = (1 - u) ** 3 * x + 3 * (1 - u) ** 2 * u * x1 + 3 * (1 - u) * u * u * x2 + u**3 * x3
                by = (1 - u) ** 3 * y + 3 * (1 - u) ** 2 * u * y1 + 3 * (1 - u) * u * u * y2 + u**3 * y3
                current.append((bx, by))
            x, y = x3, y3
        elif cmd == "A":
            rx, ry, phi, large, sweep, x2, y2 = (num() for _ in range(7))
            current.extend(arc(x, y, rx, ry, phi, int(large), int(sweep), x2, y2))
            x, y = x2, y2
        elif cmd in ("Z", "z"):
            closed = True
            result.append((current, closed))
            current, closed = [], False
            cmd = None
        else:
            sys.exit(f"Unsupported path command {cmd!r}")
    if current:
        result.append((current, closed))
    return result


def paint(size, element, attribute):
    """A full-size image with the fill or stroke paint: a solid color or a gradient from aapt:attr."""
    scale = size / VIEWPORT
    value = element.get(ANDROID + attribute)
    if value:
        return Image.new("RGBA", (size, size), color(value))
    for attr in element.findall("{http://schemas.android.com/aapt}attr"):
        if attr.get("name") != "android:" + attribute:
            continue
        gradient = attr.find("gradient")
        stops = [(float(s.get(ANDROID + "offset")), color(s.get(ANDROID + "color"))) for s in gradient]
        kind = gradient.get(ANDROID + "type")
        # Gradients are smooth, so they are computed small and scaled up.
        full, size, scale = size, 256, 256 / VIEWPORT
        image = Image.new("RGBA", (size, size))
        pixels = image.load()
        if kind == "linear":
            sx, sy, ex, ey = (float(gradient.get(ANDROID + k)) * scale for k in ("startX", "startY", "endX", "endY"))
            length = (ex - sx) ** 2 + (ey - sy) ** 2
        else:
            cx, cy, radius = (
                float(gradient.get(ANDROID + k)) * scale for k in ("centerX", "centerY", "gradientRadius")
            )
        for py in range(size):
            for px in range(size):
                if kind == "linear":
                    f = ((px + 0.5 - sx) * (ex - sx) + (py + 0.5 - sy) * (ey - sy)) / length
                else:
                    f = math.hypot(px + 0.5 - cx, py + 0.5 - cy) / radius
                f = min(1.0, max(0.0, f))
                for (o1, c1), (o2, c2) in zip(stops, stops[1:]):
                    if o1 <= f <= o2:
                        u = 0 if o2 == o1 else (f - o1) / (o2 - o1)
                        pixels[px, py] = tuple(round(a + (b - a) * u) for a, b in zip(c1, c2))
                        break
                else:
                    pixels[px, py] = stops[0][1] if f < stops[0][0] else stops[-1][1]
        return image.resize((full, full), Image.BICUBIC)
    return None


def draw_vector(canvas, path):
    size = canvas.size[0]
    scale = size / VIEWPORT
    root = ET.parse(path).getroot()
    for element in root.iter("path"):
        polylines = [([(px * scale, py * scale) for px, py in points], closed) for points, closed in
                     subpaths(element.get(ANDROID + "pathData"))]
        fill = paint(size, element, "fillColor")
        if fill is not None:
            mask = Image.new("L", (size, size))
            for points, _ in polylines:
                if len(points) > 2:
                    layer = Image.new("L", (size, size))
                    ImageDraw.Draw(layer).polygon(points, fill=255)
                    if element.get(ANDROID + "fillType") == "evenOdd":
                        mask = ImageChops.logical_xor(mask.convert("1"), layer.convert("1")).convert("L")
                    else:
                        # Non-zero: the icon's subpaths never overlap, so this is their union.
                        mask = ImageChops.lighter(mask, layer)
            alpha = float(element.get(ANDROID + "fillAlpha", "1"))
            composite(canvas, fill, mask, alpha)
        stroke = paint(size, element, "strokeColor")
        if stroke is not None:
            width = float(element.get(ANDROID + "strokeWidth", "1")) * scale
            mask = Image.new("L", (size, size))
            draw = ImageDraw.Draw(mask)
            for points, closed in polylines:
                if closed:
                    points = points + points[:1]
                draw.line(points, fill=255, width=round(width), joint="curve")
                if element.get(ANDROID + "strokeLineCap") == "round" and not closed:
                    r = width / 2
                    for px, py in (points[0], points[-1]):
                        draw.ellipse((px - r, py - r, px + r, py + r), fill=255)
            alpha = float(element.get(ANDROID + "strokeAlpha", "1"))
            composite(canvas, stroke, mask, alpha)


def composite(canvas, paint_image, mask, alpha):
    a = paint_image.getchannel("A")
    a = ImageChops.multiply(a, mask).point(lambda v: round(v * alpha))
    layer = paint_image.copy()
    layer.putalpha(a)
    canvas.alpha_composite(layer)


def adaptive(size):
    """The full 108 dp adaptive icon, background and foreground, at size x size px."""
    big = size * SUPERSAMPLE
    canvas = Image.new("RGBA", (big, big))
    draw_vector(canvas, RES / "drawable" / "ic_launcher_background.xml")
    draw_vector(canvas, RES / "drawable" / "ic_launcher_foreground.xml")
    return canvas.resize((size, size), Image.LANCZOS)


def masked(full, size, radius_fraction):
    """The visible 72 dp of the icon, cut to a rounded square (or a circle at 0.5)."""
    crop = (VIEWPORT - VISIBLE) / 2 / VIEWPORT
    w = full.size[0]
    icon = full.crop((round(w * crop), round(w * crop), round(w * (1 - crop)), round(w * (1 - crop))))
    icon = icon.resize((size, size), Image.LANCZOS)
    big = size * SUPERSAMPLE
    mask = Image.new("L", (big, big))
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, big - 1, big - 1), radius=big * radius_fraction, fill=255)
    icon.putalpha(ImageChops.multiply(icon.getchannel("A"), mask.resize((size, size), Image.LANCZOS)))
    return icon


def main():
    full = adaptive(1024)
    for density, size in DENSITIES.items():
        folder = RES / f"mipmap-{density}"
        folder.mkdir(exist_ok=True)
        masked(full, size, 0.22).save(folder / "ic_launcher.webp", lossless=True, quality=100, method=6)
        masked(full, size, 0.5).save(folder / "ic_launcher_round.webp", lossless=True, quality=100, method=6)
    # Google Play's store icon: the visible part as a full 512 px 32-bit PNG, Play rounds the corners itself.
    masked(full, 512, 0).save(ROOT / "docs" / "play-icon.png", optimize=True)
    if len(sys.argv) > 1:
        # A preview of the shapes launchers cut the icon to, for review.
        preview = Image.new("RGBA", (3 * 220, 220), (255, 255, 255, 255))
        for i, radius in enumerate((0.22, 0.5, 0.32)):
            preview.alpha_composite(masked(full, 200, radius), (i * 220 + 10, 10))
        preview.save(sys.argv[1])


if __name__ == "__main__":
    main()
