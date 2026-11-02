package net.booooorbs.rushplanner;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.DateFormat;
import java.util.Date;

/**
 * Opened by the widget's alarm button. Has no screen: it sets an alarm in the clock app for the
 * first entry of the page's alarm list (the Alarms tab's settings, selected village) that is
 * still ahead, says so in a toast, and closes.
 */
public class AlarmActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Toast.makeText(this, setNext(), Toast.LENGTH_LONG).show();
        finish();
    }

    private String setNext() {
        JSONObject data = TimersWidget.data(this);
        JSONArray list = data == null ? null : data.optJSONArray("alarms");
        if (list == null) return getString(R.string.alarm_open_app);
        long now = System.currentTimeMillis();
        for (int i = 0; i < list.length(); i++) {
            JSONObject a = list.optJSONObject(i);
            if (a == null) continue;
            long finish = a.optLong("finish"), at = a.optLong("at", finish);
            if (finish <= now) continue;
            // Ringing early only while that is still ahead; otherwise at the finish time.
            if (at <= now + 60_000) at = finish;
            String label = a.optString("label", "Upgrade");
            String when = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                    .format(new Date((long) Math.ceil(at / 60000.0) * 60000L));
            switch (Alarms.set(this, at, label)) {
                case "ok":
                    return getString(R.string.alarm_set, when, label);
                case "weekly":
                    return getString(R.string.alarm_weekly, when, label);
                case "far":
                    return getString(R.string.alarm_far, label);
                case "noapp":
                    return getString(R.string.alarm_no_app);
                default:
                    continue;
            }
        }
        return getString(R.string.alarm_none);
    }
}
