package nirvash.animethumb;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.RemoteViews;

import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Implementation of App Widget functionality.
 * App Widget Configuration implemented in {@link AnimeThumbAppWidgetConfigureActivity AnimeThumbAppWidgetConfigureActivity}
 */
public class AnimeThumbAppWidget extends AppWidgetProvider {
    private static final String TAG = AnimeThumbAppWidget.class.getSimpleName();

    public static final String ACTION_WIDGET_UPDATE = "nirvash.animethumb.ACTION_WIDGET_UPDATE";
    public static final String ACTION_UPDATE = "nirvash.animethumb.ACTION_UPDATE";

    private MediaStoreObserver mObserber = null;

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId) {
        // Construct the RemoteViews object
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.anime_thumb_app_widget);

        Bitmap bitmap = getMediaImage(context);
        views.setImageViewBitmap(R.id.imageView, bitmap);

        Intent intent = new Intent(context, AnimeThumbAppWidget.class);
        intent.setAction(ACTION_WIDGET_UPDATE);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, appWidgetId, intent, 0);
        views.setOnClickPendingIntent(R.id.imageView, pendingIntent);

        Intent configIntent = new Intent(context, AnimeThumbAppWidgetConfigureActivity.class);
        configIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE);
        configIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        PendingIntent configPendingIntent = PendingIntent.getActivity(context, appWidgetId, configIntent, 0);
        views.setOnClickPendingIntent(R.id.setting, configPendingIntent);

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        Log.d(TAG, "onReceive");

        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        if (ACTION_WIDGET_UPDATE.equals(intent.getAction())) {
            Uri uri = getMediaImageUri(context);
            Intent launchIntent = new Intent(Intent.ACTION_VIEW, uri);
            context.startActivity(launchIntent);
        } else if (ACTION_UPDATE.equals(intent.getAction())) {
            ComponentName component = new ComponentName(context, AnimeThumbAppWidget.class);
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(component);
            Intent update = new Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
            update.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
            context.sendBroadcast(update);
        }
    }
    private static Uri getMediaImageUri(Context context) {
        Cursor cursor = null;
        try {
            String order =  MediaStore.Images.Media.DATE_MODIFIED + " DESC";
            cursor = context.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    null, null, null, order);
            if (cursor.moveToNext()) {
                Long id = cursor.getLong(cursor.getColumnIndex(MediaStore.Images.Media._ID));
                return ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
    }


    private static Bitmap getMediaImage(Context context) {
        Uri uri = getMediaImageUri(context);
        if (uri != null) {
            Bitmap bitmap = null;
            try {
                bitmap = MediaStore.Images.Media.getBitmap(context.getContentResolver(), uri);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return bitmap;
        }
        return null;
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        mObserber = new MediaStoreObserver(new Handler(), context);
        context.getContentResolver().registerContentObserver(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, false, mObserber);

        // There may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        // When the user deletes the widget, delete the preference associated with it.
        for (int appWidgetId : appWidgetIds) {
            AnimeThumbAppWidgetConfigureActivity.deleteTitlePref(context, appWidgetId);
        }
        mObserber = null;
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }
}

