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

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Rect;

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

    private class MyLoaderCallback extends BaseLoaderCallback {
        public MyLoaderCallback(Context context) {
            super(context);
        }
        @Override
        public void onManagerConnected(int status) {
            super.onManagerConnected(status);
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                    FaceCrop.initFaceDetector(mAppContext);
                    break;
                default:
                    super.onManagerConnected(status);
                    break;
            }
        }
    }

    private MyLoaderCallback mOpenCVLoaderCallback = null;

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId) {
        // Construct the RemoteViews object
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.anime_thumb_app_widget);

        Bitmap bitmap = getMediaImage(context);
        if (bitmap != null) {
            views.setImageViewBitmap(R.id.imageView, bitmap);
        } else {
            views.setImageViewResource(R.id.imageView, R.mipmap.ic_launcher_round);
        }

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

    static class MediaInfo {
        public Uri uri;
        public Long date;

        public MediaInfo(Uri mediaUri, Long date) {
            this.uri = mediaUri;
            this.date = date;
        }

        public MediaInfo() {
            this.uri = null;
            this.date = -1L;
        }
    }

    private static MediaInfo getMediaInfo(Context context, Uri uri) {
        Cursor cursor = null;
        try {
            String order =  MediaStore.Images.Media.DATE_MODIFIED + " DESC";
            cursor = context.getContentResolver().query(uri, null, null, null, order);
            if (cursor.moveToNext()) {
                Long id = cursor.getLong(cursor.getColumnIndex(MediaStore.Images.Media._ID));
                Long date = cursor.getLong(cursor.getColumnIndex(MediaStore.Images.Media.DATE_MODIFIED));
                Uri mediaUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);
                return new MediaInfo(mediaUri, date);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return new MediaInfo();
    }

    private static Uri getMediaImageUri(Context context) {
        MediaInfo externalInfo = getMediaInfo(context, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        MediaInfo internalInfo = getMediaInfo(context, MediaStore.Images.Media.INTERNAL_CONTENT_URI);

        return externalInfo.date > internalInfo.date ? externalInfo.uri : internalInfo.uri;
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
            if (bitmap != null) {
                FaceCrop crop = new FaceCrop(300, 300, 300);
                Rect rect = crop.getFaceRect(bitmap);
                if (crop.isSuccess()) {
                    BitmapWrapper inputImage = new BitmapWrapper(bitmap, true);
                    BitmapWrapper cropImage = crop.cropFace(inputImage, 1.0f);
                    inputImage.recycle();
                    return cropImage.getBitmap();
                }
            }
            return bitmap;
        }
        return null;
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        if (mObserber == null) {
            mObserber = new MediaStoreObserver(new Handler(), context);
            context.getContentResolver().registerContentObserver(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true, mObserber);
            context.getContentResolver().registerContentObserver(MediaStore.Images.Media.INTERNAL_CONTENT_URI, true, mObserber);
        }
        checkOpenCV(context);

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
        checkOpenCV(context);
    }

    private void checkOpenCV(Context context) {
        if (mOpenCVLoaderCallback == null) {
            mOpenCVLoaderCallback = new MyLoaderCallback(context.getApplicationContext());
        }

        // Enter relevant functionality for when the first widget is created
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_2_0, context.getApplicationContext(), mOpenCVLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mOpenCVLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
        if (mObserber != null) {
            context.getContentResolver().unregisterContentObserver(mObserber);
            mObserber = null;
        }
    }
}

