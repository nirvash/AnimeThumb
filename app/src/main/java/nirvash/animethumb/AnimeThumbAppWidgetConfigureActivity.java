package nirvash.animethumb;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;

/**
 * The configuration screen for the {@link AnimeThumbAppWidget AnimeThumbAppWidget} AppWidget.
 */
public class AnimeThumbAppWidgetConfigureActivity extends Activity {

    private static final String PREFS_NAME = "nirvash.animethumb.AnimeThumbAppWidget";
    private static final String PREF_PREFIX_KEY = "appwidget_";

    public static final String KEY_ENABLE_DEBUG = "enable_debug";
    public static final String KEY_ENABLE_FACE_DETECT = "enable_face_detect";

    int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

    CheckBox mEnableDebug;
    CheckBox mEnableFaceDetect;

    View.OnClickListener mOnClickListener = new View.OnClickListener() {
        public void onClick(View v) {
            final Context context = AnimeThumbAppWidgetConfigureActivity.this;

            // When the button is clicked, store the string locally
            boolean enableDebug = mEnableDebug.isChecked();
            savePref(context, mAppWidgetId, KEY_ENABLE_DEBUG, enableDebug);

            boolean enableFaceDetect = mEnableFaceDetect.isChecked();
            savePref(context, mAppWidgetId, KEY_ENABLE_FACE_DETECT, enableFaceDetect);

            // It is the responsibility of the configuration activity to update the app widget
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            AnimeThumbAppWidget.updateAppWidget(context, appWidgetManager, mAppWidgetId);

            // Make sure we pass back the original appWidgetId
            Intent resultValue = new Intent();
            resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
            setResult(RESULT_OK, resultValue);
            finish();
        }
    };

    public AnimeThumbAppWidgetConfigureActivity() {
        super();
    }

    // Write the prefix to the SharedPreferences object for this widget
    static void savePref(Context context, int appWidgetId, String key, boolean value) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0).edit();
        prefs.putBoolean(PREF_PREFIX_KEY + appWidgetId + key, value);
        prefs.apply();
    }

    // Read the prefix from the SharedPreferences object for this widget.
    // If there is no preference saved, get the default from a resource
    static boolean loadPref(Context context, int appWidgetId, String key) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
        boolean value = prefs.getBoolean(PREF_PREFIX_KEY + appWidgetId + key, true);
        return value;
    }

    static void deletePref(Context context, int appWidgetId) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0).edit();
        prefs.remove(PREF_PREFIX_KEY + appWidgetId + KEY_ENABLE_DEBUG);
        prefs.remove(PREF_PREFIX_KEY + appWidgetId + KEY_ENABLE_FACE_DETECT);
        prefs.apply();
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        // Set the result to CANCELED.  This will cause the widget host to cancel
        // out of the widget placement if the user presses the back button.
        setResult(RESULT_CANCELED);

        setContentView(R.layout.anime_thumb_app_widget_configure);
        mEnableDebug = (CheckBox) findViewById(R.id.checkBoxDebug);
        mEnableFaceDetect = (CheckBox) findViewById(R.id.checkBoxFaceDetect);
        findViewById(R.id.add_button).setOnClickListener(mOnClickListener);

        // Find the widget id from the intent.
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            mAppWidgetId = extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        // If this activity was started with an intent without an app widget ID, finish with an error.
        if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
            return;
        }

        mEnableDebug.setChecked(loadPref(AnimeThumbAppWidgetConfigureActivity.this, mAppWidgetId, KEY_ENABLE_DEBUG));
        mEnableFaceDetect.setChecked(loadPref(AnimeThumbAppWidgetConfigureActivity.this, mAppWidgetId, KEY_ENABLE_FACE_DETECT));
    }
}

