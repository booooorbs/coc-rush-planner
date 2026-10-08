# CoC Rush Planner

A single-page Clash of Clans tracker that ranks your next builder, lab, hero and equipment upgrades using the rules from CallMeTee's *Strategic Rush Bible* (v1.6, "Tee Rush").

Paste your in-game data export (Settings → More Settings → Data Export → Copy) on the **Update** tab. The page then shows:

- **Plan**: what each builder is doing, what to start when it frees up, the checklist for the next Town Hall, and every open upgrade ranked by the guide.
- **Lab & Pets**: research order with your farming army first, plus pet tiers from TH14.
- **Heroes & Gear**: hero levels against Hero Hall limits, and equipment ranked by "max one main piece per hero".
- **Tracker**: progress against your Town Hall's max levels.
- **Guide**: the rush rules in short form.

Home village only. The export is saved in your browser's local storage.

## Build

```sh
python3 scripts/build.py            # uses cached game data in build/
python3 scripts/build.py --refresh  # re-download game data
```

Output: `dist/rush-planner.html`, a self-contained page (no server needed).

- `src/template.html`: the app (HTML, CSS, JS).
- `src/sample-village.json`: the export the page opens with.
- `scripts/gen_gamedata.py`: compacts game data (names, max levels per TH, times, costs, TH unlocks).

Game data comes from [coc.py](https://github.com/mathsman5133/coc.py)'s `static_data.json`. Equipment newer than that file isn't shown.
