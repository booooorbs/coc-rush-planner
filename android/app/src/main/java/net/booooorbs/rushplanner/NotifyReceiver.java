package net.booooorbs.rushplanner;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Fires the app's upgrade notifications, and re-plans them after a reboot or an app update. */
public class NotifyReceiver extends BroadcastReceiver {
    static final String ACTION_FIRE = "net.booooorbs.rushplanner.NOTIFY";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (ACTION_FIRE.equals(intent.getAction())) {
            Notifier.fire(context);
        } else {
            Notifier.reschedule(context);
        }
    }
}
