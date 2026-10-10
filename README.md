# CoC Rush Planner

A single-page Clash of Clans tracker that ranks your next builder, lab, hero and equipment upgrades using strategy from two community guides: CallMeTee's *Strategic Rush Bible* and dlimms' *5D Clash of Clans Rush Plan* (see [Credits](#credits)).

Paste your in-game data export (Settings → More Settings → Data Export → Copy) on the **Update** tab. The page then shows:

- **Plan**: what each builder is doing, what to start when it frees up, the checklist for the next Town Hall, and every open upgrade ranked by the guide.
- **Timeline**: the recommended plan as a builder-by-builder chart. Drag, swipe, shift+scroll or use ‹ › to move along it; zoom with the mouse wheel (up = closer), pinch or the −/+ buttons, from a month down to hours (zoom is remembered per chart).
- **My Plan**: your own hand-assigned queues, like clash.ninja's planner. Every upgrade level is its own item: add it to a builder, the Lab or the Pet House, reorder it, move it to another builder or remove it. You can also drag a planned bar on the My Plan chart onto another builder or to a new spot in the same one (press and hold first on a touch screen). Each queue starts after its current upgrade, the chart and finish times follow your order, and items that need a higher Town Hall, Hero Hall, Lab or Pet House by then (or start before their previous level is done) are marked in red. "Copy recommended plan" fills the builder and Lab queues with the next 30 days of the recommended plan as a starting point. A fresh export removes items it shows as done or started. Saved on the device.
- **Lab & Pets**: research order with your farming army first, plus pet tiers from TH14.
- **Heroes & Gear**: hero levels against Hero Hall limits, and equipment ranked by "max one main piece per hero".
- **Tracker**: progress against your Town Hall's max levels.
- **Guide**: the rush rules in short form.

- **Villages**: keep several accounts side by side with no setup. The player tag in each export picks the village: a tag you already have updates that village, a new tag adds one. Each village has its own export, plan, My Plan queues and settings. Switch villages from the menu under the title; the Update tab lists them so you can remove one.

Home village only. Exports are saved in your browser's local storage. On the Android widget, timers from every saved village are listed, each prefixed with its player tag when there is more than one.

## Build

```sh
python3 scripts/build.py            # uses cached game data in build/
python3 scripts/build.py --refresh  # re-download game data
```

Output: `dist/rush-planner.html`, a self-contained page (no server needed).

- `src/template.html`: the app (HTML, CSS, JS).
- `scripts/gen_gamedata.py`: compacts game data (names, max levels per TH, times, costs, TH unlocks).

Game data comes from [coc.py](https://github.com/mathsman5133/coc.py)'s `static_data.json`. Equipment newer than that file isn't shown.

## Android app

`android/` wraps the same page in a small WebView app (`dist/rush-planner.html` is bundled as an asset, so it works offline). Your pasted export is saved on the phone.

Every push to `main` builds the APK in GitHub Actions and puts it on the [`android-latest` release](../../releases/tag/android-latest). To install, open that release on your phone, download `rush-planner.apk` and allow installs from your browser when Android asks. Later builds install over the old one and keep your data, because they are all signed with the debug key in `android/debug.keystore`.

After that first install, the app updates itself. Each time it opens it reads `version.json` from the same release, and the **Check for updates** button on the Update tab does the same on demand:

- If only the page changed (most updates), it downloads the new `rush-planner.html` and reloads. No reinstall; your data stays because the page is served from the same in-app address.
- If the Android part changed (anything under `android/`), it offers to download the new APK and opens Android's installer. Android asks you to confirm each install, and the first time it asks you to allow Rush Planner to install apps.

The release has to be downloadable without signing in, so this needs the repository to be public.

The app also has an **Upgrade timers** home-screen widget (long-press the home screen, then Widgets, then Rush Planner). It lists the builder, hero, lab and pet timers from the last export you pasted, soonest first, and refreshes its countdowns about every 15 minutes. The list scrolls when the widget is small, and the reload button recomputes the countdowns on the spot (it does not open the app). It only knows what your latest export says, so paste a fresh export after starting new upgrades.

To build locally (needs JDK 17 and the Android SDK):

```sh
python3 scripts/build.py
cd android && ./gradlew assembleDebug   # app/build/outputs/apk/debug/app-debug.apk
```

## Credits

The upgrade strategy in this planner is not ours. It comes from two free community guides, and all credit for it goes to their authors:

- [**CallMeTee's Strategic Rush Bible**](https://docs.google.com/document/d/1zP792qWKVN14hVX_RW4Mui65yRTqqtHaEaj_jwUY-us) (v1.6, the "Tee Rush") by CallMeTee. The planner's upgrade priorities, lab order, pet tiers, equipment advice and "upgrade the Town Hall now if..." rules come from it. The guide's community is the Clash Files Family Discord.
- [**dlimms' 5D Clash of Clans Rush Plan**](https://docs.google.com/document/d/195oY766co3G8e2-QitP5-YPyyjrGxQ6NFP9prnEyJ90) by dlimms. The per-Town-Hall building checklists (TH13 to 16) and the timing that starts the Town Hall upgrade so every builder finishes together come from it.

This project only turns those rules into a tool that reads your village export. It isn't made by, affiliated with or endorsed by either author, and it may lag behind or simplify their guides. Read the guides themselves for the reasoning and the latest versions.

## Disclaimer

This material is unofficial and is not endorsed by Supercell. For more information see [Supercell's Fan Content Policy](https://supercell.com/en/fan-content-policy/).
