package nirvash.animethumb;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.provider.MediaStore;

public class MyService extends Service {
    private MediaStoreObserver mObserber = null;

    public MyService() {
    }

    @Override
    public void onCreate() {
        if (mObserber == null) {
            mObserber = new MediaStoreObserver(new Handler(), this);
            getContentResolver().registerContentObserver(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true, mObserber);
            getContentResolver().registerContentObserver(MediaStore.Images.Media.INTERNAL_CONTENT_URI, true, mObserber);
        }

    }

    @Override
    public void onDestroy() {
        // Enter relevant functionality for when the last widget is disabled
        if (mObserber != null) {
            getContentResolver().unregisterContentObserver(mObserber);
            mObserber = null;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}