"""Build dist/rush-planner.html: fetch game data, compact it, inline it with the sample export."""
import json, pathlib, subprocess, sys, urllib.request

ROOT = pathlib.Path(__file__).resolve().parent.parent
DATA_URL = "https://raw.githubusercontent.com/mathsman5133/coc.py/master/coc/static/static_data.json"
cache = ROOT / "build" / "static_data.json"
cache.parent.mkdir(exist_ok=True)
if not cache.exists() or "--refresh" in sys.argv:
    urllib.request.urlretrieve(DATA_URL, cache)
gamedata = ROOT / "build" / "gamedata.js"
subprocess.run([sys.executable, "-I", str(ROOT / "scripts" / "gen_gamedata.py"), str(cache), str(gamedata)], check=True)

page = (ROOT / "src" / "template.html").read_text()
sample = json.dumps(json.loads((ROOT / "src" / "sample-village.json").read_text()), separators=(",", ":"))
page = page.replace("/*GAMEDATA*/", gamedata.read_text()).replace("/*SAMPLE*/", sample)
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
