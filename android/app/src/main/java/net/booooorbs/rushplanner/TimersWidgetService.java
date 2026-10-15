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

/** Supplies the widget's scrollable list of timers, under a heading per village once there are several. */
public class TimersWidgetService extends RemoteViewsService {
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new Factory(getApplicationContext());
    }

    private static class Factory implements RemoteViewsFactory {
        private final Context context;
        // Timers, and village headings marked with "header": true.
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
            if (data == null) return;
            // Pages from before villages were grouped send only the flat list.
            JSONArray groups = data.optJSONArray("groups");
            // With one village there's nothing to group: no heading.
            if (groups == null || groups.length() < 2) {
                addJobs(data.optJSONArray("jobs"));
                return;
            }
            for (int k = 0; k < groups.length(); k++) {
                JSONObject group = groups.optJSONObject(k);
                if (group == null) continue;
                JSONArray list = group.optJSONArray("jobs");
                try {
                    jobs.add(new JSONObject()
                            .put("header", true)
                            .put("name", group.optString("name"))
                            .put("empty", list == null || list.length() == 0));
                } catch (Exception ignored) {
                }
                addJobs(list);
            }
        }

        private void addJobs(JSONArray list) {
            for (int k = 0; list != null && k < list.length(); k++) {
                JSONObject job = list.optJSONObject(k);
                if (job != null) jobs.add(job);
            }
        }

        @Override
        public RemoteViews getViewAt(int position) {
            JSONObject job = position < jobs.size() ? jobs.get(position) : null;
            if (job != null && job.optBoolean("header")) {
                RemoteViews head = new RemoteViews(context.getPackageName(), R.layout.widget_header);
                head.setTextViewText(R.id.village, job.optString("name"));
                head.setTextViewText(R.id.note,
                        job.optBoolean("empty") ? context.getString(R.string.widget_village_idle) : "");
                return head;
            }
            RemoteViews row = new RemoteViews(context.getPackageName(), R.layout.widget_row);
            if (job == null) return row;
            long finish = job.optLong("finish");
            row.setTextViewText(R.id.name, job.optString("name"));
            row.setTextViewText(R.id.kind, job.optString("kind"));
            if (finish <= now) {
                // A timer can name its own finished state, like "Ready" for the Clock Tower boost.
                String done = job.optString("done");
                row.setTextViewText(R.id.left, done.isEmpty() ? context.getString(R.string.widget_done) : done);
                row.setTextColor(R.id.left, context.getColor(R.color.widget_done));
                row.setTextViewText(R.id.at, "");
            } else {
                row.setTextViewText(R.id.left, TimersWidget.remaining(finish - now));
                row.setTextColor(R.id.left, context.getColor(R.color.widget_ink));
                row.setTextViewText(R.id.at, DateUtils.formatDateTime(context, finish,
                        DateUtils.FORMAT_SHOW_WEEKDAY | DateUtils.FORMAT_ABBREV_WEEKDAY
                                | DateUtils.FORMAT_SHOW_TIME));
            }
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
            return 2;
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
