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
out = ROOT / "dist" / "rush-planner.html"
out.write_text(page)
print(f"wrote {out} ({len(page):,} bytes)")
