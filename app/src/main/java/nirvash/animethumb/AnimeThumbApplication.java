package nirvash.animethumb;

import android.app.Application;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Handler;
import android.provider.MediaStore;

import androidx.annotation.NonNull;

import com.deploygate.sdk.DeployGate;
import com.google.android.gms.ads.MobileAds;


public class AnimeThumbApplication extends Application {
    static {
        System.loadLibrary("opencv_java4");
    }
    // AnimeThumbAppWidget mReceiver = null;
    private MediaStoreObserver mObserver = null;

    @Override
    public void onCreate() {
        super.onCreate();
        DeployGate.install(this);

        // mReceiver = new AnimeThumbAppWidget();
        // IntentFilter filter = new IntentFilter();
        // filter.addAction(Intent.ACTION_SCREEN_ON);
        // this.registerReceiver(mReceiver, filter);

        if (mObserver == null) {
            mObserver = new MediaStoreObserver(new Handler(), this);
            getContentResolver().registerContentObserver(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true, mObserver);
            getContentResolver().registerContentObserver(MediaStore.Images.Media.INTERNAL_CONTENT_URI, true, mObserver);
        }
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        // this.unregisterReceiver(mReceiver);
        if (mObserver != null) {
            getContentResolver().unregisterContentObserver(mObserver);
            mObserver = null;
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // create intent to update all instances of the widget
        Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE, null, this, AnimeThumbAppWidget.class);

        // retrieve all appWidgetIds for the widget & put it into the Intent
        AppWidgetManager appWidgetMgr = AppWidgetManager.getInstance(this);
        ComponentName cm = new ComponentName(this, AnimeThumbAppWidget.class);
        int[] appWidgetIds = appWidgetMgr.getAppWidgetIds(cm);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);

        // update the widget
        sendBroadcast(intent);
    }

}
