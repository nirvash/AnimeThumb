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
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
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
    private boolean mIsOpenCvInitialized = false;

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

        Uri uri = getMediaImageUri(context);
        BitmapWrapper bitmap = getMediaImage(uri, context, appWidgetId);
        if (uri != null) {
            if (bitmap != null) {
                bitmap = cropImage(bitmap, context, appWidgetId);
                bitmap = clipCorner(bitmap);
                views.setImageViewBitmap(R.id.imageView, bitmap.getBitmap());
            } else {
                views.setImageViewBitmap(R.id.imageView, null);
            }
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

    private static BitmapWrapper clipCorner(BitmapWrapper bitmap) {
        Bitmap dst = Bitmap.createBitmap((int)bitmap.getWidth(), (int)bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(dst);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setFilterBitmap(true);
        c.drawARGB(0, 0, 0, 0);
        paint.setColor(0xffffffff);
        RectF rect = new RectF(0, 0, bitmap.getWidth(), bitmap.getHeight());
        c.drawRoundRect(rect, 30, 30, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        c.drawBitmap(bitmap.getBitmap(), 0, 0, paint);
        bitmap.recycle();
        return new BitmapWrapper(dst, true);
    }

    // Widget の大きさに合わせてクロップを行う
    private static BitmapWrapper cropImage(BitmapWrapper bitmap, Context context, int widgetId) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        Bundle options = appWidgetManager.getAppWidgetOptions(widgetId);
        int width = getWidth(context, options);
        int height = getHeight(context, options);
        int imageWidth = (int)bitmap.getWidth();
        int imageHeight = (int)bitmap.getHeight();

        if (width == 0 || height == 0) {
            return bitmap;
        }

        Rect srcRect = new Rect(0, 0, imageWidth, imageHeight);
        if ((float)imageWidth  / (float)imageHeight > (float)width / (float)height) {
            int w = imageHeight * width / height;
            if (w < imageWidth) {
                srcRect.width = w;
                int diff = imageWidth - w;
                srcRect.x += diff / 2;
            } else {
                return bitmap;
            }
        } else {
            int h = imageWidth * height / width;
            if (h < imageHeight) {
                srcRect.height = h;
                int diff = imageHeight - h;
                srcRect.y += diff / 2;
            } else {
                return bitmap;
            }
        }

        Log.d(TAG, String.format("srcRect(%s) iw: %d, ih: %d, w:%d, h:%d", srcRect.toString(), imageWidth, imageHeight, width, height));
        Bitmap cropped = Bitmap.createBitmap(bitmap.getBitmap(), srcRect.x, srcRect.y, srcRect.width, srcRect.height);
        bitmap.recycle();
        Bitmap scaled = Bitmap.createScaledBitmap(cropped,  width, height, true);
        cropped.recycle();
        return new BitmapWrapper(scaled, true);
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


    private static BitmapWrapper getMediaImage(Uri uri, Context context, int widgetId) {
        if (uri != null) {
            Bitmap bitmap = null;
            try {
                bitmap = MediaStore.Images.Media.getBitmap(context.getContentResolver(), uri);
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (bitmap != null && enableFaceDetect(context, widgetId)) {
                AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
                Bundle options = appWidgetManager.getAppWidgetOptions(widgetId);
                int width = getWidth(context, options);
                int height = getHeight(context, options);
                float aspect = (float)height / (float)width;

                FaceCrop crop = new FaceCrop(height, 300, 300, enableDebug(context, widgetId));
                Rect rect = crop.getFaceRect(bitmap);
                if (crop.isSuccess()) {
                    if (enableDebug(context,widgetId)) {
                        BitmapWrapper out = crop.drawRegion(new BitmapWrapper(bitmap, false));
                        bitmap = out.getBitmap();
                    }
                    adjustRect(rect, width, height, bitmap.getWidth(), bitmap.getHeight());
                    Bitmap cropped = Bitmap.createBitmap(bitmap, rect.x, rect.y, rect.width, rect.height);
                    return new BitmapWrapper(cropped, true);
                } else {
                    float bitmapAspect = (float)bitmap.getHeight() / (float)bitmap.getWidth();
                    if (bitmapAspect > 0.5f && bitmap.getHeight() > height) {
                        float rate = bitmapAspect > 1.5f ? 0.4f : 0.6f;
                        int h = (int)(bitmap.getHeight() * rate);
                        if (h  / (float)bitmap.getWidth() > (float)height / (float)width) {
                            Bitmap cropped = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), h);
                            return new BitmapWrapper(cropped, true);
                        }
                    }

                }
            }
            if (bitmap != null) {
                return new BitmapWrapper(bitmap, false);
            }
        }
        return null;
    }

    // ウィジェットのアスペクト比に合わせてクロップ領域を調整
    private static void adjustRect(Rect rect, int width, int height, int maxWidth, int maxHeight) {
        float widgetAspect = (float)width / (float) height;
        float rectAspect = (float)rect.width / (float)rect.height;
        if (widgetAspect > rectAspect) {
            int w = (int)(rect.height * widgetAspect);
            w = Math.min(maxWidth, w);
            int diff = w - rect.width;
            if (rect.x < diff / 2) {
                rect.x = 0;
            } else {
                rect.x -= diff / 2;
            }
            rect.width = w;
        } else {
            int h = (int)(rect.width / widgetAspect);
            h = Math.min(maxHeight, h);
            int diff = h - rect.height;
            if (rect.y < diff / 2) {
                rect.y = 0;
            } else {
                rect.y -= diff / 2;
            }
            rect.height = h;
        }
    }

    private static int getHeight(Context context, Bundle options) {
        String KEY_HEIGHT = isPortrait(context) ?
                AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT :
                AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT;
        int heightInDp = options.getInt(KEY_HEIGHT);
        return (int)convertDp2Px(heightInDp, context);
    }

    private static int getWidth(Context context, Bundle options) {
        String KEY_HEIGHT = isPortrait(context) ?
                AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH :
                AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH;
        int widthInDp = options.getInt(KEY_HEIGHT);
        return (int)convertDp2Px(widthInDp, context);
    }

    public static float convertDp2Px(float dp, Context context){
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        return dp * metrics.density;
    }

    private static boolean isPortrait(Context context) {
        return context.getResources().getBoolean(R.bool.portrait);
    }

    private static boolean enableFaceDetect(Context context, int widgetId) {
        return AnimeThumbAppWidgetConfigureActivity.loadPref(context, widgetId, AnimeThumbAppWidgetConfigureActivity.KEY_ENABLE_FACE_DETECT);
    }

    private static boolean enableDebug(Context context, int widgetId) {
        return AnimeThumbAppWidgetConfigureActivity.loadPref(context, widgetId, AnimeThumbAppWidgetConfigureActivity.KEY_ENABLE_DEBUG);
    }


    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
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
            AnimeThumbAppWidgetConfigureActivity.deletePref(context, appWidgetId);
        }
        mObserber = null;
    }

    @Override
    public void onEnabled(Context context) {
        checkOpenCV(context);
    }

    private void checkOpenCV(Context context) {
        if (mObserber == null) {
            mObserber = new MediaStoreObserver(new Handler(), context);
            context.getContentResolver().registerContentObserver(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true, mObserber);
            context.getContentResolver().registerContentObserver(MediaStore.Images.Media.INTERNAL_CONTENT_URI, true, mObserber);
        }

        if (mOpenCVLoaderCallback == null) {
            mOpenCVLoaderCallback = new MyLoaderCallback(context.getApplicationContext());
        }

        if (!mIsOpenCvInitialized) {
            // Enter relevant functionality for when the first widget is created
            if (!OpenCVLoader.initDebug()) {
                Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
                OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_2_0, context.getApplicationContext(), mOpenCVLoaderCallback);
                mIsOpenCvInitialized = true;
            } else {
                Log.d(TAG, "OpenCV library found inside package. Using it!");
                mOpenCVLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
                mIsOpenCvInitialized = true;
            }
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

    @Override
    public void onAppWidgetOptionsChanged (Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle newOptions) {
        // This is how you get your changes.
        checkOpenCV(context);

        updateAppWidget(context, appWidgetManager, appWidgetId);
    }
}
