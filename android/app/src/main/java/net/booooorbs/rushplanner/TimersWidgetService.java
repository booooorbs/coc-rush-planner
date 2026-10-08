package net.booooorbs.rushplanner;

import android.content.Context;
import android.content.Intent;
import android.text.format.DateUtils;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** Supplies the widget's scrollable list of timers. */
public class TimersWidgetService extends RemoteViewsService {
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new Factory(getApplicationContext());
    }

    private static class Factory implements RemoteViewsFactory {
        private final Context context;
        private final List<JSONObject> jobs = new ArrayList<>();
        private long now;

        Factory(Context context) {
            this.context = context;
        }

        @Override
        public void onDataSetChanged() {
            jobs.clear();
            now = System.currentTimeMillis();
            JSONObject data = TimersWidget.data(context);
            JSONArray list = data == null ? null : data.optJSONArray("jobs");
            for (int k = 0; list != null && k < list.length(); k++) {
                JSONObject job = list.optJSONObject(k);
                if (job != null) jobs.add(job);
            }
        }

        @Override
        public RemoteViews getViewAt(int position) {
            RemoteViews row = new RemoteViews(context.getPackageName(), R.layout.widget_row);
            if (position >= jobs.size()) return row;
            JSONObject job = jobs.get(position);
            long finish = job.optLong("finish");
            row.setTextViewText(R.id.name, job.optString("name"));
            row.setTextViewText(R.id.kind, job.optString("kind"));
            if (finish <= now) {
                row.setTextViewText(R.id.left, context.getString(R.string.widget_done));
                row.setTextColor(R.id.left, context.getColor(R.color.widget_done));
                row.setTextViewText(R.id.at, "");
            } else {
                row.setTextViewText(R.id.left, TimersWidget.remaining(finish - now));
                row.setTextColor(R.id.left, context.getColor(R.color.widget_ink));
                row.setTextViewText(R.id.at, DateUtils.formatDateTime(context, finish,
                        DateUtils.FORMAT_SHOW_WEEKDAY | DateUtils.FORMAT_ABBREV_WEEKDAY
                                | DateUtils.FORMAT_SHOW_TIME));
            }
            // Tapping a row opens the app (the template is set on the list).
            row.setOnClickFillInIntent(R.id.row, new Intent());
            return row;
        }

        @Override
        public int getCount() {
            return jobs.size();
        }

        @Override
        public void onCreate() {
        }

        @Override
        public void onDestroy() {
            jobs.clear();
        }

        @Override
        public RemoteViews getLoadingView() {
            return null;
        }

        @Override
        public int getViewTypeCount() {
            return 1;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }
    }
}
