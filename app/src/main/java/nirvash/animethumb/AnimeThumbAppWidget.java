package nirvash.animethumb;

import android.annotation.SuppressLint;
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
import android.graphics.ImageDecoder;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;
import androidx.exifinterface.media.ExifInterface;

import com.deploygate.sdk.DeployGate;

import org.opencv.core.Rect;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Implementation of App Widget functionality.
 * App Widget Configuration implemented in {@link AnimeThumbAppWidgetConfigureActivity AnimeThumbAppWidgetConfigureActivity}
 */
public class AnimeThumbAppWidget extends AppWidgetProvider {
    private static final String TAG = AnimeThumbAppWidget.class.getSimpleName();

    public static final String ACTION_WIDGET_UPDATE = "nirvash.animethumb.ACTION_WIDGET_UPDATE";
    public static final String ACTION_UPDATE = "nirvash.animethumb.ACTION_UPDATE";

    // private static boolean mIsOpenCvInitialized = false;
    static private FaceCrop mFaceCropCache = null;
    static private Uri mFaceCropCacheUri = null;
    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId, boolean clearCache) {
        // Construct the RemoteViews object
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.anime_thumb_app_widget);
        if (clearCache) {
            mFaceCropCache = null;
            mFaceCropCacheUri = null;
        }

        Uri uri = getMediaImageUri(context, appWidgetId);
        BitmapWrapper bitmap = getMediaImage(uri, context, appWidgetId);
        if (uri != null) {
            if (bitmap != null) {
                bitmap = cropImage(bitmap, context, appWidgetId);
                bitmap = clipCorner(bitmap);
                views.setImageViewBitmap(R.id.imageView, bitmap.getBitmap());
            } else {
                views.setImageViewBitmap(R.id.imageView, null);
                broadcastUpdate(context);
            }
        } else {
            views.setImageViewResource(R.id.imageView, R.mipmap.ic_launcher_round);
        }


        Intent intent = new Intent(context, AnimeThumbAppWidget.class);
        intent.setAction(ACTION_WIDGET_UPDATE);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        @SuppressLint("UnspecifiedImmutableFlag") PendingIntent pendingIntent = PendingIntent.getBroadcast(context, appWidgetId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        views.setOnClickPendingIntent(R.id.imageView, pendingIntent);

        Intent configIntent = new Intent(context, AnimeThumbAppWidgetConfigureActivity.class);
        configIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE);
        configIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        @SuppressLint("UnspecifiedImmutableFlag") PendingIntent configPendingIntent = PendingIntent.getActivity(context, appWidgetId, configIntent, PendingIntent.FLAG_UPDATE_CURRENT);
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
        Bitmap targetBitmap = bitmap.getBitmap().copy(Bitmap.Config.ARGB_8888, false);
        c.drawBitmap(targetBitmap, 0, 0, paint);
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

        Log.d(TAG, String.format("cropImage: srcRect(%s) iw: %d, ih: %d, w:%d, h:%d", srcRect.toString(), imageWidth, imageHeight, width, height));
        Bitmap cropped = Bitmap.createBitmap(bitmap.getBitmap(), srcRect.x, srcRect.y, srcRect.width, srcRect.height);
        bitmap.recycle();
        Bitmap scaled = Bitmap.createScaledBitmap(cropped,  width, height, true);
        cropped.recycle();
        return new BitmapWrapper(scaled, true);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive:" + intent.getAction());
        super.onReceive(context, intent);

        if (ACTION_WIDGET_UPDATE.equals(intent.getAction())) {
            int appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, 0);
            Uri uri = getMediaImageUri(context, appWidgetId);
            Intent launchIntent = new Intent(Intent.ACTION_VIEW, uri);
            launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(launchIntent);
            broadcastUpdate(context); // ついでに更新もかける
        } else if (ACTION_UPDATE.equals(intent.getAction())) {
            broadcastUpdate(context);
        } else if (Intent.ACTION_SCREEN_ON.equals(intent.getAction())) {
            broadcastUpdate(context);
        }
    }

    static public void broadcastUpdate(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName component = new ComponentName(context, AnimeThumbAppWidget.class);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(component);
        Intent update = new Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        update.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
        context.sendBroadcast(update);
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

    private static List<MediaInfo> getMediaInfo(Context context, Uri uri) {
        Cursor cursor = null;
        List<MediaInfo> result = new ArrayList<>();
        try {
            String order =  MediaStore.Images.Media.DATE_MODIFIED + " DESC";
            cursor = context.getContentResolver().query(uri, null, null, null, order);
            int index = 0;
            while (cursor.moveToNext() && index < 10) {
                @SuppressLint("Range") long id = cursor.getLong(cursor.getColumnIndex(MediaStore.Images.Media._ID));
                @SuppressLint("Range") Long date = cursor.getLong(cursor.getColumnIndex(MediaStore.Images.Media.DATE_ADDED));
                Uri mediaUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);
                result.add(new MediaInfo(mediaUri, date));
                index++;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return result;
    }

    private static Uri getMediaImageUri(Context context, int widgetId) {
        try {
            List<MediaInfo> mediaList1 = getMediaInfo(context, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            List<MediaInfo> mediaList2 = getMediaInfo(context, MediaStore.Images.Media.INTERNAL_CONTENT_URI);

            mediaList1.addAll(mediaList2);
            Collections.sort(mediaList1, (media1, media2) -> (int) (media2.date - media1.date));

            int imageIndex = getImageIndex(context, widgetId);
            if (mediaList1.size() < imageIndex) {
                imageIndex = mediaList1.size() - 1;
            }
            if (mediaList1.isEmpty()) {
                return null;
            }
            return mediaList1.get(imageIndex).uri;
        } catch (Exception e) {
            DeployGate.logWarn("getMediaImageUri:" + e.getMessage());
            return null;
        }
    }


    @SuppressLint("DefaultLocale")
    private static BitmapWrapper getMediaImage(Uri uri, Context context, int widgetId) {
        if (uri != null) {
            Bitmap bitmap = null;
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ImageDecoder.Source source = ImageDecoder.createSource(context.getContentResolver(), uri);
                    ImageDecoder.OnHeaderDecodedListener listener = new ImageDecoder.OnHeaderDecodedListener() {
                        @Override
                        public void onHeaderDecoded(@NonNull ImageDecoder imageDecoder, @NonNull ImageDecoder.ImageInfo imageInfo, @NonNull ImageDecoder.Source source) {
                            imageDecoder.setMutableRequired(true);
                        }
                    };
                    bitmap = ImageDecoder.decodeBitmap(source, listener);
                } else {
                    bitmap = MediaStore.Images.Media.getBitmap(context.getContentResolver(), uri);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            bitmap = rotateByExifInfo(context, uri, bitmap);

            if (bitmap != null && enableFaceDetect(context, widgetId)) {
                AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
                Bundle options = appWidgetManager.getAppWidgetOptions(widgetId);
                int width = getWidth(context, options);
                int height = getHeight(context, options);
                float aspect = (float)height / (float)width;
                Log.d(TAG, String.format("getMediaImage: enableFaceDetect: w %d, h %d", width, height));

                FaceCrop crop;
                if (mFaceCropCacheUri != null && mFaceCropCacheUri.equals(uri)) {
                    crop = mFaceCropCache;
                } else {
                    crop = new FaceCrop(height,
                            300,
                            300,
                            enableDebug(context, widgetId),
                            getMinDetectSize(context, widgetId),
                            getFaceScale(context, widgetId));
                }
                Rect rect = crop.getFaceRect(bitmap);
                if (crop.isSuccess()) {
                    Log.d(TAG, "getMediaImage: crop.isSuccess(): rect:" + rect.toString());
                    mFaceCropCache = crop;
                    mFaceCropCacheUri = uri;
                    if (enableDebug(context,widgetId)) {
                        BitmapWrapper out = crop.drawRegion(new BitmapWrapper(bitmap, false));
                        bitmap = out.getBitmap();
                    }
                    adjustRect(rect, width, height, bitmap.getWidth(), bitmap.getHeight());
                    if (rect.x >= 0 && rect.y >= 0 && rect.width > 0 && rect.height > 0) {
                        Bitmap cropped = Bitmap.createBitmap(bitmap, rect.x, rect.y, rect.width, rect.height);
                        return new BitmapWrapper(cropped, true);
                    } else {
                        DeployGate.logWarn(String.format("getMediaImage(): w %d, h %d, bw %d, bh %d", width, height, bitmap.getWidth(), bitmap.getHeight()));
                        return new BitmapWrapper(bitmap, false);
                    }
                } else {
                    Log.d(TAG, "getMediaImage: crop failed");
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

    private static Bitmap rotateByExifInfo(Context context, Uri uri, Bitmap bitmap) {
        int orientation = getOrientation(context, uri);

        Matrix mat = new Matrix();
        boolean doRotate = false;
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                mat.postRotate(90);
                doRotate = true;
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                mat.postRotate(180);
                doRotate = true;
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                mat.postRotate(270);
                doRotate = true;
                break;
            default:
                break;
        }

        if (doRotate) {
            Bitmap tmp = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), mat, true);
            bitmap.recycle();
            bitmap = tmp;
        }

        return bitmap;
    }

    private static int getOrientation(Context context, Uri uri) {
        String[] selections = { MediaStore.Images.Media.DATA };
        InputStream input = null;
        try {
            input = context.getContentResolver().openInputStream(uri);
        } catch (Exception e) {
            e.printStackTrace();
        }

        int orientation = ExifInterface.ORIENTATION_NORMAL;
        if (input == null) {
            return orientation;
        }

        ExifInterface exifInterface;
        try {
            exifInterface = new ExifInterface(input);
            orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return orientation;
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
            if (rect.x + rect.width > maxWidth) {
                rect.width = maxWidth - rect.x;
            }
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
            if (rect.y + rect.height > maxHeight) {
                rect.height = maxHeight - rect.y;
            }
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
        return AnimeThumbAppWidgetConfigureActivity.loadPrefBoolean(context, widgetId, AnimeThumbAppWidgetConfigureActivity.KEY_ENABLE_FACE_DETECT, true);
    }

    private static boolean enableDebug(Context context, int widgetId) {
        return AnimeThumbAppWidgetConfigureActivity.loadPrefBoolean(context, widgetId, AnimeThumbAppWidgetConfigureActivity.KEY_ENABLE_DEBUG, false);
    }

    private static int getMinDetectSize(Context context, int widgetId) {
        return AnimeThumbAppWidgetConfigureActivity.loadPrefInt(context, widgetId, AnimeThumbAppWidgetConfigureActivity.KEY_MIN_DETECT_SIZE, 100);
    }

    private static int getFaceScale(Context context, int widgetId) {
        return AnimeThumbAppWidgetConfigureActivity.loadPrefInt(context, widgetId, AnimeThumbAppWidgetConfigureActivity.KEY_FACE_SCALE, 100);
    }

    private static int getImageIndex(Context context, int widgetId) {
        return AnimeThumbAppWidgetConfigureActivity.loadPrefInt(context, widgetId, AnimeThumbAppWidgetConfigureActivity.KEY_IMAGE_INDEX, 0);
    }


    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Log.d(TAG, "onUpdate:");
        if (!OpenCVWrapper.initialize(context)) {
            Log.d(TAG, "onUpdate: opencv is not initaialized");
            return;
        }

        // There may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId, false);
        }
    }


    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        // When the user deletes the widget, delete the preference associated with it.
        for (int appWidgetId : appWidgetIds) {
            AnimeThumbAppWidgetConfigureActivity.deletePref(context, appWidgetId);
        }
    }

    @Override
    public void onEnabled(Context context) {
        OpenCVWrapper.initialize(context);
    }

    @Override
    public void onAppWidgetOptionsChanged (Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle newOptions) {
        Log.d(TAG, "onAppWidgetOptionsChanged");
        mFaceCropCacheUri = null;
        mFaceCropCache = null;
        if (!OpenCVWrapper.initialize(context)) {
            return;
        }

        updateAppWidget(context, appWidgetManager, appWidgetId, true);
    }
}

