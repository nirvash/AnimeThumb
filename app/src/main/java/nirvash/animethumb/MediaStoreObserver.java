package nirvash.animethumb;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.os.Handler;
import android.util.Log;


public class MediaStoreObserver extends ContentObserver {
    private static final String TAG = MediaStoreObserver.class.getSimpleName();
    private Context mContext = null;

    /**
     * Creates a content observer.
     *
     * @param handler The handler to run {@link #onChange} on, or null if none.
     */
    public MediaStoreObserver(Handler handler, Context context) {
        super(handler);
        mContext = context;
    }

    @Override
    public void onChange(boolean selfChange) {
        super.onChange(selfChange);
        Log.d(TAG, "onChage");

        if (mContext != null) {
/*            Intent intent = new Intent(mContext, AnimeThumbAppWidget.class);
            intent.setAction(AnimeThumbAppWidget.ACTION_UPDATE);
            mContext.sendBroadcast(intent);
            */
            AnimeThumbAppWidget.broadcastUpdate(mContext);
        }

    }

}
