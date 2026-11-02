package net.booooorbs.rushplanner;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.AlarmClock;
import android.view.Window;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.webkit.WebViewAssetLoader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Calendar;

/** Hosts dist/rush-planner.html (bundled as an asset) in a full-screen WebView. */
public class MainActivity extends Activity {
    // Served from a fixed https origin so localStorage (the saved export) persists across launches.
    private static final String HOST = "appassets.androidplatform.net";
    private static final String START_URL = "https://" + HOST + "/assets/rush-planner.html";

    private WebView web;
    private Updater updater;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window window = getWindow();
        WindowCompat.setDecorFitsSystemWindows(window, false);

        web = new WebView(this);
        web.setBackgroundColor(getColor(R.color.background));
        setContentView(web);

        // Keep the page clear of the status bar, navigation bar and keyboard.
        ViewCompat.setOnApplyWindowInsetsListener(web, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout()
                    | WindowInsetsCompat.Type.ime());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(false);

        updater = new Updater(this, () -> web.reload());

        // The page reports its running timers here for the home-screen widget, and checks for updates.
        web.addJavascriptInterface(new Bridge(), "RushPlannerAndroid");

        // A page downloaded by the updater replaces the bundled one, on the same origin so saved data stays.
        WebViewAssetLoader.AssetsPathHandler assets = new WebViewAssetLoader.AssetsPathHandler(this);
        WebViewAssetLoader loader = new WebViewAssetLoader.Builder()
                .setDomain(HOST)
                .addPathHandler("/assets/", path -> {
                    File page = "rush-planner.html".equals(path) ? Updater.webFile(this) : null;
                    if (page != null) {
                        try {
                            return new WebResourceResponse("text/html", "utf-8", new FileInputStream(page));
                        } catch (FileNotFoundException ignored) {
                        }
                    }
                    return assets.handle(path);
                })
                .build();

        web.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                return loader.shouldInterceptRequest(request.getUrl());
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri url = request.getUrl();
                if (HOST.equals(url.getHost())) return false;
                // Links out of the app open in the browser.
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, url));
                } catch (ActivityNotFoundException ignored) {
                }
                return true;
            }
        });

        if (savedInstanceState != null) {
            web.restoreState(savedInstanceState);
        } else {
            web.loadUrl(START_URL);
            updater.check(false);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updater.resume();
    }

    /** Exposed to the page as window.RushPlannerAndroid. */
    public class Bridge {
        @JavascriptInterface
        public void setTimers(String json) {
            TimersWidget.save(getApplicationContext(), json);
        }

        @JavascriptInterface
        public String appVersion() {
            return "build " + Updater.currentBuild(MainActivity.this);
        }

        @JavascriptInterface
        public void checkForUpdates() {
            runOnUiThread(() -> updater.check(true));
        }

        /**
         * Sets an alarm in the phone's clock app for finishMs (epoch millis), rounded up to the
         * minute. Clock apps take an hour and minute, not a date, so an alarm more than a day out
         * is set on its weekday, which clock apps treat as weekly. Returns "ok", "weekly", "far"
         * (more than a week out, nothing set), "past" or "noapp".
         */
        @JavascriptInterface
        public String setAlarm(double finishMs, String label) {
            long now = System.currentTimeMillis();
            Calendar at = Calendar.getInstance();
            at.setTimeInMillis((long) Math.ceil(finishMs / 60000.0) * 60000L);
            long ahead = at.getTimeInMillis() - now;
            if (ahead <= 0) return "past";
            if (ahead > 7L * 24 * 3600 * 1000) return "far";
            boolean weekly = ahead > 24L * 3600 * 1000;
            Intent i = new Intent(AlarmClock.ACTION_SET_ALARM)
                    .putExtra(AlarmClock.EXTRA_HOUR, at.get(Calendar.HOUR_OF_DAY))
                    .putExtra(AlarmClock.EXTRA_MINUTES, at.get(Calendar.MINUTE))
                    .putExtra(AlarmClock.EXTRA_MESSAGE, label == null ? "Upgrade done" : label)
                    // Show the clock app for a weekly alarm so the repeat is visible.
                    .putExtra(AlarmClock.EXTRA_SKIP_UI, !weekly)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (weekly) {
                ArrayList<Integer> days = new ArrayList<>();
                days.add(at.get(Calendar.DAY_OF_WEEK));
                i.putExtra(AlarmClock.EXTRA_DAYS, days);
            }
            try {
                startActivity(i);
            } catch (ActivityNotFoundException | SecurityException e) {
                return "noapp";
            }
            return weekly ? "weekly" : "ok";
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        web.saveState(outState);
    }

    @Override
    public void onBackPressed() {
        if (web.canGoBack()) {
            web.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
