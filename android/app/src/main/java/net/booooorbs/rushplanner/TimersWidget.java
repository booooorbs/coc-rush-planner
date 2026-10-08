package net.booooorbs.rushplanner;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.widget.RemoteViews;

import org.json.JSONObject;

/** Home-screen widget listing the running upgrade timers from the last export pasted in the app. */
public class TimersWidget extends AppWidgetProvider {
    static final String PREFS = "widget";
    static final String KEY_TIMERS = "timers";
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
        // Rebuild the list so the countdowns are recomputed.
        mgr.notifyAppWidgetViewDataChanged(ids, R.id.list);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager mgr, int[] ids) {
        for (int id : ids) mgr.updateAppWidget(id, render(context));
        mgr.notifyAppWidgetViewDataChanged(ids, R.id.list);
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
    // The same broadcast backs the reload button.
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

    /** The last timers the page sent, or null before the first export is pasted. */
    static JSONObject data(Context context) {
        String raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_TIMERS, null);
        try {
            JSONObject data = raw == null ? null : new JSONObject(raw);
            return data != null && data.optBoolean("saved") ? data : null;
        } catch (Exception e) {
            return null;
        }
    }

    static RemoteViews render(Context context) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_timers);

        // The reload button recomputes the countdowns in place; nothing on the widget opens the app.
        views.setOnClickPendingIntent(R.id.reload, refreshIntent(context));
        views.setRemoteAdapter(R.id.list, new Intent(context, TimersWidgetService.class));
        views.setEmptyView(R.id.list, R.id.message);

        JSONObject data = data(context);
        if (data == null) {
            views.setTextViewText(R.id.updated, "");
            showMessage(views, context.getString(R.string.widget_no_data));
            return views;
        }
        long now = System.currentTimeMillis();
        views.setTextViewText(R.id.updated, context.getString(R.string.widget_updated, ago(now - data.optLong("ts"))));
        showMessage(views, context.getString(R.string.widget_idle));
        return views;
    }

    // Shown by the list only while it is empty.
    private static void showMessage(RemoteViews views, String text) {
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
