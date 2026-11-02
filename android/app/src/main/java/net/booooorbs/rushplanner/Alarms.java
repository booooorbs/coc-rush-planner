package net.booooorbs.rushplanner;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.provider.AlarmClock;

import java.util.ArrayList;
import java.util.Calendar;

/** Hands alarms to the phone's clock app. Used by the page's alarm buttons and the widget's. */
final class Alarms {
    private Alarms() {
    }

    /**
     * Sets an alarm for atMs (epoch millis), rounded up to the minute. Clock apps take an hour and
     * minute, not a date, so an alarm more than a day out is set on its weekday, which clock apps
     * treat as weekly. Returns "ok", "weekly", "far" (more than a week out, nothing set), "past"
     * or "noapp".
     */
    static String set(Context context, double atMs, String label) {
        long now = System.currentTimeMillis();
        Calendar at = Calendar.getInstance();
        at.setTimeInMillis((long) Math.ceil(atMs / 60000.0) * 60000L);
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
            context.startActivity(i);
        } catch (ActivityNotFoundException | SecurityException e) {
            return "noapp";
        }
        return weekly ? "weekly" : "ok";
    }
}
