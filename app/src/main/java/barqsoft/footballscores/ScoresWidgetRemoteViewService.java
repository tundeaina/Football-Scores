package barqsoft.footballscores;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.util.Log;
import android.widget.AdapterView;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import java.text.SimpleDateFormat;
import java.util.Date;

public class ScoresWidgetRemoteViewService extends RemoteViewsService {
    public ScoresWidgetRemoteViewService() {
    }

    private static final String[] COLUMNS = {
            DatabaseContract.scores_table.DATE_COL,
            DatabaseContract.scores_table.LEAGUE_COL,
            DatabaseContract.scores_table.HOME_COL,
            DatabaseContract.scores_table.AWAY_COL,
            DatabaseContract.scores_table.HOME_GOALS_COL,
            DatabaseContract.scores_table.AWAY_GOALS_COL
    };
    // these indices must match the projection
    static final int INDEX_DATE_COL = 0;
    static final int INDEX_LEAGUE_COL = 1;
    static final int INDEX_HOME_COL = 2;
    static final int INDEX_AWAY_COL = 3;
    static final int INDEX_HOME_GOALS_COL = 4;
    static final int INDEX_AWAY_GOALS_COL = 5;

    static Uri BASE_CONTENT_URI = DatabaseContract.BASE_CONTENT_URI;

    static int DAYS_BACK = PagerFragment.DAYS_BACK;

    private static final String SCORES_BETWEEN_DATES =
            DatabaseContract.scores_table.DATE_COL + " BETWEEN ? AND ?";

    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new RemoteViewsFactory() {
            private Cursor data = null;

            @Override
            public void onCreate() {
                // Nothing to do
            }

            @Override
            public void onDataSetChanged() {
                if (data != null) {
                    data.close();
                }
                // This method is called by the app hosting the widget (e.g., the launcher)
                // However, our ContentProvider is not exported so it doesn't have access to the
                // data. Therefore we need to clear (and finally restore) the calling identity so
                // that calls use our process and permission

                final long identityToken = Binder.clearCallingIdentity();

                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

                Date fromDate = new Date(System.currentTimeMillis()+((0-DAYS_BACK)*86400000));
                Date toDate = new Date(System.currentTimeMillis());
                String[] dates = {
                        dateFormat.format(fromDate),dateFormat.format(toDate)};

                data = getContentResolver().query(
                        BASE_CONTENT_URI,
                        COLUMNS,
                        SCORES_BETWEEN_DATES,
                        dates,
                        DatabaseContract.scores_table.DATE_COL + " DESC");

                Binder.restoreCallingIdentity(identityToken);
            }

            @Override
            public void onDestroy() {
                if (data != null) {
                    data.close();
                    data = null;
                }
            }

            @Override
            public int getCount() {
                return data == null ? 0 : data.getCount();
            }

            @Override
            public RemoteViews getViewAt(int position) {
                if (position == AdapterView.INVALID_POSITION ||
                        data == null || !data.moveToPosition(position)) {
                    return null;
                }
                RemoteViews views = new RemoteViews(getPackageName(),
                        R.layout.widget_scores_list_item);

                views.setTextViewText(R.id.widget_match_league, Utilities
                        .getLeague(data.getInt(INDEX_LEAGUE_COL)));

                views.setTextViewText(R.id.widget_match_date,
                        data.getString(INDEX_DATE_COL));

                views.setTextViewText(
                        R.id.widget_home_team_score, data.getString(INDEX_HOME_COL)
                                + " ["
                                + data.getString(INDEX_HOME_GOALS_COL).replace("-1","-")
                                +"]");

                views.setTextViewText(
                        R.id.widget_away_team_score, data.getString(INDEX_AWAY_COL)
                                + " ["
                                + data.getString(INDEX_AWAY_GOALS_COL).replace("-1","-")
                                +"]");

                final Intent fillInIntent = new Intent();
                fillInIntent.setData(BASE_CONTENT_URI);

                views.setOnClickFillInIntent(R.id.widget_scores_list_item, fillInIntent);
                return views;
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
            public RemoteViews getLoadingView() {
                return new RemoteViews(getPackageName(), R.layout.widget_scores_list_item);
            }

            @Override
            public boolean hasStableIds() {
                 return true;
             }




        };
    }
}

