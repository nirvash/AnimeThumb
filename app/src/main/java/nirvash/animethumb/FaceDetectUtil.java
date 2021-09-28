package nirvash.animethumb;

import android.annotation.SuppressLint;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;

import androidx.annotation.NonNull;
import androidx.exifinterface.media.ExifInterface;

import com.deploygate.sdk.DeployGate;

import org.opencv.core.Rect;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FaceDetectUtil {
    private static List<AnimeThumbAppWidget.MediaInfo> getMediaInfo(Context context, Uri uri) {
        Cursor cursor = null;
        List<AnimeThumbAppWidget.MediaInfo> result = new ArrayList<>();
        try {
            String order =  MediaStore.Images.Media.DATE_MODIFIED + " DESC";
            cursor = context.getContentResolver().query(uri, null, null, null, order);
            int index = 0;
            while (cursor.moveToNext() && index < 10) {
                @SuppressLint("Range") long id = cursor.getLong(cursor.getColumnIndex(MediaStore.Images.Media._ID));
                @SuppressLint("Range") Long date = cursor.getLong(cursor.getColumnIndex(MediaStore.Images.Media.DATE_ADDED));
                Uri mediaUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);
                result.add(new AnimeThumbAppWidget.MediaInfo(mediaUri, date));
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

    public static Uri getMediaImageUri(Context context, int widgetId, int imageIndex) {
        try {
            List<AnimeThumbAppWidget.MediaInfo> mediaList1 = getMediaInfo(context, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            List<AnimeThumbAppWidget.MediaInfo> mediaList2 = getMediaInfo(context, MediaStore.Images.Media.INTERNAL_CONTENT_URI);

            mediaList1.addAll(mediaList2);
            Collections.sort(mediaList1, (media1, media2) -> (int) (media2.date - media1.date));

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


    static int getOrientation(Context context, Uri uri) {
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

    static Bitmap rotateByExifInfo(Context context, Uri uri, Bitmap bitmap) {
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

    public static Bitmap getBitmap(Uri uri, Context context) {
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
        return bitmap;
    }

    // ウィジェットのアスペクト比に合わせてクロップ領域を調整
    static void adjustRect(Rect rect, int width, int height, int maxWidth, int maxHeight) {
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
}
