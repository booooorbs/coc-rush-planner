package net.booooorbs.rushplanner;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.text.format.DateUtils;
import android.view.View;
import android.widget.RemoteViews;

import org.json.JSONArray;
import org.json.JSONObject;

/** Home-screen widget listing the running upgrade timers from the last export pasted in the app. */
public class TimersWidget extends AppWidgetProvider {
    static final String PREFS = "widget";
    static final String KEY_TIMERS = "timers";
    private static final int MAX_ROWS = 8;
    private static final long REFRESH_MS = 15 * 60 * 1000L;

    /** Called by the page (via MainActivity) whenever it loads or a new export is pasted. */
    static void save(Context context, String json) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_TIMERS, json).apply();
        updateAll(context);
    }

    static void updateAll(Context context) {
        AppWidgetManager mgr = AppWidgetManager.getInstance(context);
        int[] ids = mgr.getAppWidgetIds(new ComponentName(context, TimersWidget.class));
        for (int id : ids) mgr.updateAppWidget(id, render(context));
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager mgr, int[] ids) {
        RemoteViews views = render(context);
        for (int id : ids) mgr.updateAppWidget(id, views);
        scheduleRefresh(context);
    }

    @Override
    public void onEnabled(Context context) {
        scheduleRefresh(context);
    }

    @Override
    public void onDisabled(Context context) {
        alarms(context).cancel(refreshIntent(context));
    }

    // The system's own update period is 30 minutes at best; an inexact alarm keeps the countdowns fresher.
    private static void scheduleRefresh(Context context) {
        alarms(context).setInexactRepeating(AlarmManager.ELAPSED_REALTIME,
                SystemClock.elapsedRealtime() + REFRESH_MS, REFRESH_MS, refreshIntent(context));
    }

    private static AlarmManager alarms(Context context) {
        return (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    }

    private static PendingIntent refreshIntent(Context context) {
        Intent i = new Intent(context, TimersWidget.class).setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        int[] ids = AppWidgetManager.getInstance(context)
                .getAppWidgetIds(new ComponentName(context, TimersWidget.class));
        i.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
        return PendingIntent.getBroadcast(context, 0, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    static RemoteViews render(Context context) {
        String pkg = context.getPackageName();
        RemoteViews views = new RemoteViews(pkg, R.layout.widget_timers);
        views.removeAllViews(R.id.rows);

        Intent open = new Intent(context, MainActivity.class);
        views.setOnClickPendingIntent(R.id.root, PendingIntent.getActivity(context, 0, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));

        String raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_TIMERS, null);
        long now = System.currentTimeMillis();
        try {
            JSONObject data = raw == null ? null : new JSONObject(raw);
            if (data == null || !data.optBoolean("saved")) {
                showMessage(views, context.getString(R.string.widget_no_data));
                views.setTextViewText(R.id.updated, "");
                return views;
            }
            long ts = data.optLong("ts");
            views.setTextViewText(R.id.updated, context.getString(R.string.widget_updated, ago(now - ts)));

            JSONArray jobs = data.optJSONArray("jobs");
            int n = jobs == null ? 0 : Math.min(jobs.length(), MAX_ROWS);
            if (n == 0) {
                showMessage(views, context.getString(R.string.widget_idle));
                return views;
            }
            views.setViewVisibility(R.id.message, View.GONE);
            for (int k = 0; k < n; k++) {
                JSONObject job = jobs.getJSONObject(k);
                long finish = job.getLong("finish");
                RemoteViews row = new RemoteViews(pkg, R.layout.widget_row);
                row.setTextViewText(R.id.name, job.optString("name"));
                row.setTextViewText(R.id.kind, job.optString("kind"));
                if (finish <= now) {
                    row.setTextViewText(R.id.left, context.getString(R.string.widget_done));
                    row.setTextColor(R.id.left, context.getColor(R.color.widget_done));
                    row.setTextViewText(R.id.at, "");
                } else {
                    row.setTextViewText(R.id.left, remaining(finish - now));
                    row.setTextViewText(R.id.at, DateUtils.formatDateTime(context, finish,
                            DateUtils.FORMAT_SHOW_WEEKDAY | DateUtils.FORMAT_ABBREV_WEEKDAY
                                    | DateUtils.FORMAT_SHOW_TIME));
                }
                views.addView(R.id.rows, row);
            }
        } catch (Exception e) {
            showMessage(views, context.getString(R.string.widget_no_data));
        }
        return views;
    }

    private static void showMessage(RemoteViews views, String text) {
        views.setViewVisibility(R.id.message, View.VISIBLE);
        views.setTextViewText(R.id.message, text);
    }

    /** Same style as the page: "2d 4h", "3h 15m", "12m". */
    static String remaining(long ms) {
        long sec = ms / 1000, d = sec / 86400, h = sec % 86400 / 3600, m = sec % 3600 / 60;
        if (d > 0) return d + "d " + h + "h";
        if (h > 0) return h + "h " + m + "m";
        return Math.max(m, 1) + "m";
    }

    private static String ago(long ms) {
        return ms < 60_000 ? "just now" : remaining(ms) + " ago";
    }
}
