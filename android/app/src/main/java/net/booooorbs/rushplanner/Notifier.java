package net.booooorbs.rushplanner;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * The app's own upgrade notifications, an alternative to clock alarms. The page turns them on in
 * the Alarms tab (notify in the widget data) and sends the list to notify about (alarms: at,
 * finish, label). One system alarm is kept for the next entry; when it fires, every entry now due
 * is notified and the alarm moves to the next one.
 */
final class Notifier {
    private static final String CHANNEL = "upgrades";
    private static final String KEY_LAST = "notified_until";

    private Notifier() {
    }

    /** Re-plans the next notification. Called when the page sends new data, at boot and after firing. */
    static void reschedule(Context context) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent fire = fireIntent(context);
        am.cancel(fire);
        JSONArray list = list(context);
        if (list == null) return;
        long after = Math.max(System.currentTimeMillis(), prefs(context).getLong(KEY_LAST, 0));
        long next = Long.MAX_VALUE;
        for (int i = 0; i < list.length(); i++) {
            long at = at(list.optJSONObject(i));
            if (at > after && at < next) next = at;
        }
        if (next == Long.MAX_VALUE) return;
        if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next, fire);
        } else {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next, fire);
        }
    }

    /** Posts a notification for each entry that has come due since the last time, then reschedules. */
    static void fire(Context context) {
        JSONArray list = list(context);
        long now = System.currentTimeMillis() + 30_000;
        long last = prefs(context).getLong(KEY_LAST, 0);
        if (list != null) {
            NotificationManager nm = context.getSystemService(NotificationManager.class);
            createChannel(context, nm);
            for (int i = 0; i < list.length(); i++) {
                JSONObject a = list.optJSONObject(i);
                long at = at(a);
                // Not entries long past, say after notifications were turned on late.
                if (at <= last || at > now || at < now - 15 * 60_000) continue;
                long finish = a.optLong("finish", at);
                long early = Math.round((finish - at) / 60000.0);
                String label = a.optString("label", "Upgrade");
                String text = early > 0
                        ? context.getString(R.string.notify_soon, early)
                        : context.getString(R.string.notify_done);
                nm.notify((label + finish).hashCode(), new Notification.Builder(context, CHANNEL)
                        .setSmallIcon(R.drawable.ic_alarm)
                        .setContentTitle(label)
                        .setContentText(text)
                        .setWhen(finish)
                        .setShowWhen(true)
                        .setAutoCancel(true)
                        .setContentIntent(PendingIntent.getActivity(context, 2,
                                new Intent(context, MainActivity.class),
                                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE))
                        .build());
            }
        }
        prefs(context).edit().putLong(KEY_LAST, now).apply();
        reschedule(context);
    }

    private static void createChannel(Context context, NotificationManager nm) {
        NotificationChannel ch = new NotificationChannel(CHANNEL,
                context.getString(R.string.notify_channel), NotificationManager.IMPORTANCE_HIGH);
        ch.setDescription(context.getString(R.string.notify_channel_description));
        nm.createNotificationChannel(ch);
    }

    /** The entries to notify about, or null when notifications are off. */
    private static JSONArray list(Context context) {
        JSONObject data = TimersWidget.data(context);
        return data != null && data.optBoolean("notify") ? data.optJSONArray("alarms") : null;
    }

    private static long at(JSONObject a) {
        return a == null ? 0 : a.optLong("at", a.optLong("finish"));
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(TimersWidget.PREFS, Context.MODE_PRIVATE);
    }

    private static PendingIntent fireIntent(Context context) {
        Intent i = new Intent(context, NotifyReceiver.class).setAction(NotifyReceiver.ACTION_FIRE);
        return PendingIntent.getBroadcast(context, 3, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}
