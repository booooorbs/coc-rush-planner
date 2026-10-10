# CoC Rush Planner

A free Clash of Clans upgrade planner for rushing. Paste your village's in-game data export and it tells you what each builder, the Lab and the Pet House should do next, using strategy from two community guides: CallMeTee's *Strategic Rush Bible* and dlimms' *5D Clash of Clans Rush Plan* (see [Credits](#credits)).

It runs as an Android app or as a single web page, works offline, and keeps everything on your device (see [Privacy](#privacy)). Home village only.

## What it does

Get your export in Clash of Clans from Settings → More Settings → Data Export → Copy, and paste it on the **Update** tab. Paste a fresh one whenever you start or finish upgrades.

- **Plan**: what each builder is doing and what to start the moment it frees up, the checklist for your next Town Hall (with how many items are done), and a priority list of every open upgrade ranked by the guides.
- **Timeline**: the recommended plan as a builder-by-builder chart. The Town Hall upgrade is timed so every builder finishes together, as in the 5D plan.
- **My Plan**: build your own upgrade order, like clash.ninja's planner. Add any upgrade level to a builder, the Lab or the Pet House, then reorder it, move it to another queue or remove it. Bars can be dragged on the chart (press and hold first on a touch screen). Finish times and gold, elixir and dark elixir totals follow your order, and anything that needs a higher Town Hall, Hero Hall, Lab or Pet House by then is marked in red. "Copy recommended plan" fills the queues with the next 30 days of the recommended plan as a starting point, and each new export removes what's already done or started.
- **Lab & Pets**: research order with your farming army first, upgrades still locked behind your Lab level, and pet tiers from TH14.
- **Heroes & Gear**: hero levels against your Hero Hall limits, and equipment ranked by "max one main piece per hero first".
- **Tracker**: your progress against your Town Hall's max levels.
- **Guide**: the rush rules in short form, including Town Hall timing and how to use magic items.
- **Update**: paste exports, pick your farming army and spells, choose the builder plan (the 5D plan for TH13 to 16, or the Tee checklist only), and set your number of builders if the export reads it wrong.

**Several accounts**: each export's player tag picks its village, with no setup. A tag you already have updates that village and a new one adds a village with its own plan, queues and settings. Switch villages from the menu under the title, or remove one on the Update tab.

**Charts**: drag, swipe, shift+scroll or use ‹ › to move along a chart. Zoom with the mouse wheel, a pinch or the −/+ buttons, from a month down to hours; each chart remembers its zoom.

**Android extras**: an **Upgrade timers** home-screen widget, and updates that install themselves (see [Android app](#android-app)).

## Install

There are two ways to use the planner. Both work the same way and keep everything on your device.

### Option 1: Android app (no setup)

1. On your phone, open the [`android-latest` release](../../releases/tag/android-latest).
2. Tap `rush-planner.apk` to download it.
3. Open the downloaded file. If Android asks, allow your browser to install apps, then tap **Install**.
4. Open **Rush Planner**, then in Clash of Clans go to Settings → More Settings → Data Export → Copy, and paste it on the **Update** tab.

The app isn't on the Google Play Store, so Android shows an "unknown app" warning the first time. After this first install the app keeps itself up to date (see [Android app](#android-app)).

### Option 2: Run it yourself (Python)

For anyone comfortable with a terminal. You need Python 3 and Git.

```sh
git clone https://github.com/booooorbs/coc-rush-planner.git
cd coc-rush-planner
python3 scripts/build.py
```

The build pulls the latest game data (see [Build](#build)) and writes `dist/rush-planner.html`. Open that file in any browser by double-clicking it or dragging it into a browser window. It's a single file with nothing to install and no server to run, and it works offline. If you don't want to build, the repository already has a built `dist/rush-planner.html`, and you can also download `rush-planner.html` from the [`android-latest` release](../../releases/tag/android-latest).

## Privacy

- **Nothing personal is collected.** There are no accounts, no sign-in, no analytics, no ads and no tracking.
- **Your data stays on your device.** The exports you paste and your plans are saved only in the app (or, for the web page, in that browser's local storage). They are never uploaded anywhere. Uninstalling the app or clearing the browser's site data deletes them.
- **It works offline.** The page has its game data and fonts built in and makes no network requests of its own.
- **The only time the Android app goes online is to check for updates.** When it opens, and when you tap **Check for updates**, it downloads the small `version.json` file from this repository's GitHub release and, if there is a newer version, the update itself. It sends nothing about you or your village; like any download, GitHub sees the request.

## Build

```sh
python3 scripts/build.py            # uses cached game data in build/
python3 scripts/build.py --refresh  # re-download game data
```

Output: `dist/rush-planner.html`, a self-contained page (no server needed). The build inlines the game data and the fonts in `fonts/` (Lilita One and Nunito Sans, SIL Open Font License), so the page makes no network requests. `src/template.html` on its own still loads the fonts from Google Fonts.

- `src/template.html`: the app (HTML, CSS, JS).
- `scripts/gen_gamedata.py`: compacts game data (names, max levels per TH, times, costs, TH unlocks).

Game data comes from [coc.py](https://github.com/mathsman5133/coc.py)'s `static_data.json`. Equipment newer than that file isn't shown.

## Android app

`android/` wraps the same page in a small WebView app (`dist/rush-planner.html` is bundled as an asset, so it works offline). Your pasted export is saved on the phone.

Every push to `main` builds the APK in GitHub Actions and puts it on the [`android-latest` release](../../releases/tag/android-latest) (to install it, see [Install](#install)). Later builds install over the old one and keep your data, because they are all signed with the debug key in `android/debug.keystore`.

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
