#!/usr/bin/env python3
"""Builds Novila's website (novila.app) into build/site, which pages.yml publishes with GitHub Pages.

The site has a home page, the privacy policy and the terms of use, in English (at the root) and Swedish (under /sv).
The home pages are written by hand in site/; the policy and the terms are made from docs/privacy-policy.md and
docs/terms-of-use.md, the only copy of each: the app links here instead of showing them. The scenes are the
site's own (site/images); the fonts are copied from the app, and the phones, the tablet and the foldable show the
app's own screens from its screenshot references, so the site looks like the app.

    python3 tools/site/build_site.py

Needs only the standard library. It understands the subset of Markdown the two documents use: ## and ### headings,
paragraphs, "- " lists, *emphasis*, **strong**, `code` and [links](https://...). Anything it cannot place fails the
build instead of reaching the page.
"""

import html
import re
import shutil
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SITE = ROOT / "site"
OUT = ROOT / "build" / "site"
DOMAIN = "https://novila.app"

FONTS = ROOT / "core" / "designsystem" / "src" / "main" / "res" / "font"
FONT_LICENSE = ROOT / "core" / "designsystem" / "src" / "main" / "assets" / "licenses" / "hanken_grotesk_ofl.txt"
ICON = ROOT / "docs" / "play-icon.png"
# The app's own screens in the devices, from the screenshot references CI renders and checks (English; phone size, and
# a landscape tablet and an unfolded phone for the tablet and the foldable).
SCREENS = {
    "today": "feature/today/src/test/screenshots/se.novila.feature.today.TodayScreenKt.TodayScreenPreview.H1400dp.png",
    "training": "feature/training/src/test/screenshots/"
    "se.novila.feature.training.TrainingScreenKt.TrainingScreenPreview.H1600dp_WITH_BACKGROUND.png",
    "sleep": "feature/sleep/src/test/screenshots/"
    "se.novila.feature.sleep.SleepScreenKt.SleepScreenPreview.H1000dp_WITH_BACKGROUND.png",
    "nutrition": "feature/nutrition/src/test/screenshots/"
    "se.novila.feature.nutrition.NutritionScreenKt.NutritionScreenPreview.H1200dp_WITH_BACKGROUND.png",
    "tablet": "feature/today/src/test/screenshots/"
    "se.novila.feature.today.TodayScreenKt.TodayScreenLandscapePreview.W1200dp_H800dp.png",
    "foldable": "feature/training/src/test/screenshots/"
    "se.novila.feature.training.TrainingScreenKt.TrainingScreenFoldablePreview.W852dp_H880dp_WITH_BACKGROUND.png",
}

LANGUAGES = {
    "en": {
        "prefix": "",
        "doc_section": "English",
        "switch": "Svenska",
        "privacy": "Privacy policy",
        "terms": "Terms of use",
        "home": "Home",
        "footer": "General advice on training, sleep and food. Not a medical device. "
        "This site has no cookies and no analytics.",
        "skip": "Skip to content",
    },
    "sv": {
        "prefix": "sv/",
        "doc_section": "Svenska",
        "switch": "English",
        "privacy": "Integritetspolicy",
        "terms": "Användarvillkor",
        "home": "Start",
        "footer": "Allmänna råd om träning, sömn och kost. Inte en medicinteknisk produkt. "
        "Den här sidan har inga kakor och inga analysverktyg.",
        "skip": "Hoppa till innehållet",
    },
}

# Each page and its path per language. The paths are public addresses (the privacy policy's is in Play Console),
# so never move one.
PAGES = {
    "home": {"en": "", "sv": "sv/"},
    "privacy": {"en": "privacy/", "sv": "sv/integritet/"},
    "terms": {"en": "terms/", "sv": "sv/villkor/"},
}

DOCUMENTS = {"privacy": ROOT / "docs" / "privacy-policy.md", "terms": ROOT / "docs" / "terms-of-use.md"}


def inline(text):
    """Escapes text and turns the inline Markdown the documents use into HTML."""
    out = html.escape(text, quote=False)
    out = re.sub(r"`([^`]+)`", r"<code>\1</code>", out)
    out = re.sub(r"\*\*([^*]+)\*\*", r"<strong>\1</strong>", out)
    out = re.sub(r"\*([^*]+)\*", r"<em>\1</em>", out)

    def link(match):
        label, url = match.group(1), html.unescape(match.group(2))
        if not url.startswith("https://"):
            sys.exit(f"Only https links are allowed on the site: {url}")
        return f'<a href="{html.escape(url)}">{label}</a>'

    out = re.sub(r"\[([^\]]+)\]\(([^)\s]+)\)", link, out)
    if re.search(r"[*`\[\]]", out):
        sys.exit(f"Markdown the site can't render: {text}")
    return out


def markdown_to_html(lines):
    """Renders a document section: ### becomes <h2>, since the page title is the <h1>."""
    blocks, paragraph, items = [], [], []

    def flush():
        if paragraph:
            blocks.append(f"<p>{inline(' '.join(paragraph))}</p>")
            paragraph.clear()
        if items:
            blocks.append("<ul>\n" + "\n".join(f"  <li>{inline(item)}</li>" for item in items) + "\n</ul>")
            items.clear()

    for raw in lines:
        line = raw.strip()
        if not line:
            flush()
        elif line.startswith("### "):
            flush()
            blocks.append(f"<h2>{inline(line[4:])}</h2>")
        elif line.startswith("#"):
            sys.exit(f"Unexpected heading in a document section: {line}")
        elif line.startswith("- "):
            if paragraph:
                flush()
            items.append(line[2:])
        elif items and raw.startswith("  "):
            items[-1] += " " + line
        else:
            if items:
                flush()
            paragraph.append(line)
    flush()
    return "\n".join(blocks)


def document_section(path, section):
    """The lines under "## <section>" in a document, up to the next "## " or "---"."""
    lines = path.read_text(encoding="utf-8").splitlines()
    try:
        start = lines.index(f"## {section}") + 1
    except ValueError:
        sys.exit(f"{path.relative_to(ROOT)} has no '## {section}' section")
    end = start
    while end < len(lines) and not lines[end].startswith("## ") and lines[end].strip() != "---":
        end += 1
    return lines[start:end]


def url(path):
    return f"{DOMAIN}/{path}"


def render(page, lang, title, description, body):
    """Puts a page body in site/layout.html and writes it to its path."""
    strings = LANGUAGES[lang]
    other = "sv" if lang == "en" else "en"
    path = PAGES[page][lang]
    page_title = "Novila" if page == "home" else f"{title} – Novila"
    values = {
        "lang": lang,
        "title": html.escape(page_title),
        "description": html.escape(description),
        "canonical": url(path),
        "alternate_en": url(PAGES[page]["en"]),
        "alternate_sv": url(PAGES[page]["sv"]),
        "root": "../" * path.count("/"),
        "home": PAGES["home"][lang],
        "home_label": strings["home"],
        "switch": PAGES[page][other],
        "switch_lang": other,
        "switch_label": strings["switch"],
        "privacy": PAGES["privacy"][lang],
        "privacy_label": strings["privacy"],
        "terms": PAGES["terms"][lang],
        "terms_label": strings["terms"],
        "footer": html.escape(strings["footer"]),
        "skip": strings["skip"],
        "page": page,
        "body": body,
    }
    layout = (SITE / "layout.html").read_text(encoding="utf-8")

    def value(match):
        key = match.group(1)
        if key not in values:
            sys.exit(f"layout.html uses an unknown value: {key}")
        return str(values[key])

    text = re.sub(r"\{\{(\w+)\}\}", value, layout)
    # Links in the layout are written from the root; make them relative so the site also works from a folder.
    text = text.replace('href="/', f'href="{values["root"]}').replace('src="/', f'src="{values["root"]}')
    target = OUT / path / "index.html"
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(text, encoding="utf-8")


def home(lang):
    text = (SITE / f"home.{lang}.html").read_text(encoding="utf-8")
    match = re.match(r"<!--\s*description:\s*(.+?)\s*-->\n", text)
    if not match:
        sys.exit(f"site/home.{lang}.html must start with <!-- description: ... -->")
    render("home", lang, "Novila", match.group(1), text[match.end():])


def document(page, lang):
    strings = LANGUAGES[lang]
    title = strings[page]
    body = markdown_to_html(document_section(DOCUMENTS[page], strings["doc_section"]))
    content = f'<article class="document">\n<h1>{html.escape(title)}</h1>\n{body}\n</article>'
    render(page, lang, title, f"Novila – {title}", content)


def main():
    if OUT.exists():
        shutil.rmtree(OUT)
    OUT.mkdir(parents=True)
    for lang in LANGUAGES:
        home(lang)
        document("privacy", lang)
        document("terms", lang)

    shutil.copy(SITE / "style.css", OUT / "style.css")
    shutil.copy(ICON, OUT / "icon.png")
    (OUT / "fonts").mkdir()
    for weight in ("regular", "semibold", "bold"):
        shutil.copy(FONTS / f"hanken_grotesk_{weight}.ttf", OUT / "fonts")
    shutil.copy(FONT_LICENSE, OUT / "fonts" / "OFL.txt")
    shutil.copytree(SITE / "images", OUT / "images")
    (OUT / "screens").mkdir()
    for name, source in SCREENS.items():
        shutil.copy(ROOT / source, OUT / "screens" / f"{name}.png")

    # GitHub Pages serves it for every missing path, at any depth, so its links are written from the root.
    shutil.copy(SITE / "404.html", OUT / "404.html")
    sitemap = "\n".join(f"  <url><loc>{url(paths[lang])}</loc></url>" for paths in PAGES.values() for lang in paths)
    (OUT / "sitemap.xml").write_text(
        '<?xml version="1.0" encoding="UTF-8"?>\n'
        '<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">\n'
        f"{sitemap}\n</urlset>\n",
        encoding="utf-8",
    )
    (OUT / "robots.txt").write_text(f"User-agent: *\nAllow: /\nSitemap: {DOMAIN}/sitemap.xml\n", encoding="utf-8")
    print(f"Built {sum(1 for _ in OUT.rglob('*.html'))} pages in {OUT.relative_to(ROOT)}")


if __name__ == "__main__":
    main()
