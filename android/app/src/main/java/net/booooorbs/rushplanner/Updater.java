package net.booooorbs.rushplanner;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Updates the app from the android-latest GitHub release, without Google Play.
 *
 * CI publishes three files there: version.json ({"build": run number, "native": hash of android/}),
 * rush-planner.html and rush-planner.apk. When only the page changed, the new page is downloaded
 * into the app's files and served from the same origin, so it applies with no reinstall and the
 * saved exports stay. When the Android part changed, the new APK is downloaded and handed to
 * Android's installer.
 */
class Updater {
    static final String RELEASE = "https://github.com/booooorbs/coc-rush-planner/releases/download/android-latest/";
    private static final String PREFS = "updates";
    private static final String KEY_WEB_BUILD = "web_build";
    private static final String KEY_SKIPPED = "skipped_build";

    private final Activity activity;
    private final Runnable reloadPage;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Handler ui = new Handler(Looper.getMainLooper());
    private boolean busy;
    /** Set while the user is in Settings allowing installs; the install resumes in onResume. */
    private File pendingApk;

    Updater(Activity activity, Runnable reloadPage) {
        this.activity = activity;
        this.reloadPage = reloadPage;
    }

    /** The downloaded page, if it is newer than the one bundled in this APK. */
    static File webFile(Context context) {
        File f = new File(new File(context.getFilesDir(), "web"), "rush-planner.html");
        return webBuild(context) > BuildConfig.VERSION_CODE && f.isFile() ? f : null;
    }

    private static int webBuild(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY_WEB_BUILD, 0);
    }

    /** The build number of what is running now (the APK, or a newer downloaded page). */
    static int currentBuild(Context context) {
        return webFile(context) != null ? webBuild(context) : BuildConfig.VERSION_CODE;
    }

    /** manual: the user tapped "Check for updates", so always say what happened. */
    void check(boolean manual) {
        if (busy) return;
        busy = true;
        if (manual) toast("Checking for updates…");
        worker.execute(() -> {
            try {
                JSONObject v = new JSONObject(new String(fetch(RELEASE + "version.json"), "UTF-8"));
                int build = v.getInt("build");
                String nativeHash = v.optString("native");
                SharedPreferences prefs = prefs();
                if (build <= currentBuild(activity)) {
                    if (manual) toast("You have the latest version (build " + currentBuild(activity) + ").");
                } else if (!nativeHash.equals(BuildConfig.NATIVE_HASH)) {
                    // The Android part changed: this needs a new APK.
                    if (manual || prefs.getInt(KEY_SKIPPED, 0) != build) ui.post(() -> offerApk(build));
                } else {
                    // Only the page changed: swap it in place.
                    byte[] page = fetch(RELEASE + "rush-planner.html");
                    if (page.length < 10000) throw new IOException("page too small");
                    File dir = new File(activity.getFilesDir(), "web");
                    dir.mkdirs();
                    File tmp = new File(dir, "rush-planner.html.tmp");
                    write(tmp, page);
                    if (!tmp.renameTo(new File(dir, "rush-planner.html"))) throw new IOException("rename failed");
                    prefs.edit().putInt(KEY_WEB_BUILD, build).apply();
                    ui.post(() -> {
                        reloadPage.run();
                        toast("Rush Planner updated to build " + build + ".");
                    });
                }
            } catch (Exception e) {
                if (manual) toast("Couldn't check for updates. Check your connection and try again.");
            } finally {
                ui.post(() -> busy = false);
            }
        });
    }

    private void offerApk(int build) {
        if (activity.isFinishing()) return;
        new AlertDialog.Builder(activity)
                .setTitle("Update available")
                .setMessage("Build " + build + " of Rush Planner is ready. Download and install it now? Your villages and plans are kept.")
                .setPositiveButton("Update", (d, w) -> downloadApk())
                .setNegativeButton("Later", (d, w) -> prefs().edit().putInt(KEY_SKIPPED, build).apply())
                .show();
    }

    private void downloadApk() {
        busy = true;
        toast("Downloading the update…");
        worker.execute(() -> {
            try {
                File dir = new File(activity.getCacheDir(), "updates");
                dir.mkdirs();
                File apk = new File(dir, "rush-planner.apk");
                write(apk, fetch(RELEASE + "rush-planner.apk"));
                ui.post(() -> install(apk));
            } catch (Exception e) {
                toast("The download failed. Check your connection and try again.");
            } finally {
                ui.post(() -> busy = false);
            }
        });
    }

    private void install(File apk) {
        // Android asks once to let this app install updates.
        if (!activity.getPackageManager().canRequestPackageInstalls()) {
            pendingApk = apk;
            toast("Allow Rush Planner to install updates, then go back.");
            try {
                activity.startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:" + activity.getPackageName())));
            } catch (ActivityNotFoundException ignored) {
            }
            return;
        }
        Uri uri = FileProvider.getUriForFile(activity, activity.getPackageName() + ".updates", apk);
        Intent i = new Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            activity.startActivity(i);
        } catch (ActivityNotFoundException e) {
            toast("This phone has no installer for the update.");
        }
    }

    /** Called from onResume, to continue an install after the user allowed it in Settings. */
    void resume() {
        if (pendingApk != null && activity.getPackageManager().canRequestPackageInstalls()) {
            File apk = pendingApk;
            pendingApk = null;
            install(apk);
        }
    }

    private SharedPreferences prefs() {
        return activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private void toast(String text) {
        ui.post(() -> Toast.makeText(activity, text, Toast.LENGTH_LONG).show());
    }

    private static byte[] fetch(String url) throws IOException {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setUseCaches(false);
        c.setConnectTimeout(15000);
        c.setReadTimeout(30000);
        try {
            if (c.getResponseCode() != 200) throw new IOException("HTTP " + c.getResponseCode());
            try (InputStream in = c.getInputStream()) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buf = new byte[64 * 1024];
                for (int n; (n = in.read(buf)) > 0; ) out.write(buf, 0, n);
                return out.toByteArray();
            }
        } finally {
            c.disconnect();
        }
    }

    private static void write(File f, byte[] data) throws IOException {
        try (OutputStream out = new FileOutputStream(f)) {
            out.write(data);
        }
    }
}
