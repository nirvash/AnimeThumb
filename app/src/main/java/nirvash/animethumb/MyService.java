package nirvash.animethumb;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.IBinder;
import android.provider.MediaStore;
import android.util.Log;

public class MyService extends Service {
    private MediaStoreObserver mObserber = null;

    public MyService() {
    }

    private MyBroadcastReciever mReceiver = new MyBroadcastReciever();
    class MyBroadcastReciever extends BroadcastReceiver {
        private final String TAG = MyBroadcastReciever.class.getSimpleName();

        @Override
        public void onReceive(Context context, Intent intent) {
             if (Intent.ACTION_CONFIGURATION_CHANGED.equals(intent.getAction())) {
                 Log.d(TAG, "onReceive: " + intent.getAction());
                 AnimeThumbAppWidget.broadcastUpdate(context);
             }
        }
    }

    @Override
    public void onCreate() {
        if (mObserber == null) {
            mObserber = new MediaStoreObserver(new Handler(), this);
            getContentResolver().registerContentObserver(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true, mObserber);
            getContentResolver().registerContentObserver(MediaStore.Images.Media.INTERNAL_CONTENT_URI, true, mObserber);
        }

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_CONFIGURATION_CHANGED);
        registerReceiver(mReceiver, filter);
    }

    @Override
    public void onDestroy() {
        // Enter relevant functionality for when the last widget is disabled
        if (mObserber != null) {
            getContentResolver().unregisterContentObserver(mObserber);
            mObserber = null;
        }
        unregisterReceiver(mReceiver);
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
