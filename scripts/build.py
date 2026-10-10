"""Build dist/rush-planner.html: fetch game data, compact it and inline it."""
import base64, pathlib, re, subprocess, sys, urllib.request

ROOT = pathlib.Path(__file__).resolve().parent.parent
DATA_URL = "https://raw.githubusercontent.com/mathsman5133/coc.py/master/coc/static/static_data.json"
cache = ROOT / "build" / "static_data.json"
cache.parent.mkdir(exist_ok=True)
if not cache.exists() or "--refresh" in sys.argv:
    urllib.request.urlretrieve(DATA_URL, cache)
gamedata = ROOT / "build" / "gamedata.js"
subprocess.run([sys.executable, "-I", str(ROOT / "scripts" / "gen_gamedata.py"), str(cache), str(gamedata)], check=True)

page = (ROOT / "src" / "template.html").read_text()
page = page.replace("/*GAMEDATA*/", gamedata.read_text())
# The artifact loads its fonts from Google Fonts; the built page (and the Android app) inlines
# the copies in fonts/ instead, so it makes no network requests of its own.
fonts = ROOT / "fonts"
font_css = re.sub(
    r"url\(([\w.-]+\.woff2)\)",
    lambda m: "url(data:font/woff2;base64," + base64.b64encode((fonts / m.group(1)).read_bytes()).decode() + ")",
    (fonts / "fonts.css").read_text(),
)
page, n = re.subn(r'<link rel="preconnect"[^>]*>\n<link rel="preconnect"[^>]*>\n<link rel="stylesheet" href="https://fonts\.googleapis\.com[^>]*>', lambda m: "<style>\n" + font_css + "</style>", page)
assert n == 1, "Google Fonts links not found in the template"
# The template is also published as a claude.ai artifact, which supplies the document
# skeleton; a standalone file needs its own (standards mode, UTF-8, mobile viewport).
page = (
    '<!doctype html>\n<html lang="en">\n<head>\n<meta charset="utf-8">\n'
    '<meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover">\n'
    '<style>body{margin:0}</style>\n</head>\n<body>\n' + page + '\n</body>\n</html>\n'
)
out = ROOT / "dist" / "rush-planner.html"
out.write_text(page)
print(f"wrote {out} ({len(page):,} bytes)")
