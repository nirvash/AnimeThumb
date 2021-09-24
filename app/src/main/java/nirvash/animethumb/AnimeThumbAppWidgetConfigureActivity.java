package nirvash.animethumb;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import com.google.android.material.switchmaterial.SwitchMaterial;

import org.w3c.dom.Text;

/**
 * The configuration screen for the {@link AnimeThumbAppWidget AnimeThumbAppWidget} AppWidget.
 */
public class AnimeThumbAppWidgetConfigureActivity extends Activity {

    private static final String PREFS_NAME = "nirvash.animethumb.AnimeThumbAppWidget";
    private static final String PREF_PREFIX_KEY = "appwidget_";

    public static final String KEY_ENABLE_DEBUG = "enable_debug";
    public static final String KEY_ENABLE_FACE_DETECT = "enable_face_detect";
    public static final String KEY_MIN_DETECT_SIZE = "min_detect_size";

    int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

    SwitchMaterial mEnableDebug;
    SwitchMaterial mEnableFaceDetect;
    EditText mMinDetectSize;
    SeekBar mSeekBar;
    boolean mFromApp = false;

    View.OnClickListener mOnClickListener = new View.OnClickListener() {
        public void onClick(View v) {
            final Context context = AnimeThumbAppWidgetConfigureActivity.this;

            // When the button is clicked, store the string locally
            boolean enableDebug = mEnableDebug.isChecked();
            savePref(context, mAppWidgetId, KEY_ENABLE_DEBUG, enableDebug);

            boolean enableFaceDetect = mEnableFaceDetect.isChecked();
            savePref(context, mAppWidgetId, KEY_ENABLE_FACE_DETECT, enableFaceDetect);

            int minDetectSize = Integer.parseInt(mMinDetectSize.getText().toString());
            savePref(context, mAppWidgetId, KEY_MIN_DETECT_SIZE, minDetectSize);

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

    static void savePref(Context context, int appWidgetId, String key, int value) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0).edit();
        prefs.putInt(PREF_PREFIX_KEY + appWidgetId + key, value);
        prefs.apply();
    }


    // Read the prefix from the SharedPreferences object for this widget.
    // If there is no preference saved, get the default from a resource
    static boolean loadPrefString(Context context, int appWidgetId, String key) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
        return prefs.getBoolean(PREF_PREFIX_KEY + appWidgetId + key, true);
    }

    static int loadPrefInt(Context context, int appWidgetId, String key) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
        return prefs.getInt(PREF_PREFIX_KEY + appWidgetId + key, 100);
    }


    static void deletePref(Context context, int appWidgetId) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0).edit();
        prefs.remove(PREF_PREFIX_KEY + appWidgetId + KEY_ENABLE_DEBUG);
        prefs.remove(PREF_PREFIX_KEY + appWidgetId + KEY_ENABLE_FACE_DETECT);
        prefs.remove(PREF_PREFIX_KEY + appWidgetId + KEY_MIN_DETECT_SIZE);
        prefs.apply();
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        final int MINSIZE_MAX = 300;
        final int MINSIZE_STEP = 10;

        // Set the result to CANCELED.  This will cause the widget host to cancel
        // out of the widget placement if the user presses the back button.
        setResult(RESULT_CANCELED);

        setContentView(R.layout.anime_thumb_app_widget_configure);
        mEnableDebug = findViewById(R.id.switchEnableDebug);
        mEnableFaceDetect = findViewById(R.id.switchEnableFaceDetect);
        mMinDetectSize = findViewById(R.id.editTextSize);
        mSeekBar = findViewById(R.id.seekBar);
        mSeekBar.setMax(MINSIZE_MAX / MINSIZE_STEP);
        findViewById(R.id.add_button).setOnClickListener(mOnClickListener);

        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    if (progress >= 0 && progress <= seekBar.getMax()) {
                        String value = String.valueOf(progress * MINSIZE_STEP);
                        if (!mFromApp) {
                            mMinDetectSize.setText(value);
                        }
                    }
                    mFromApp = false;
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        mMinDetectSize.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mFromApp = true;
                try {
                    int progress = Integer.parseInt(s.toString());
                    mSeekBar.setProgress(progress / MINSIZE_STEP);
                } catch (Exception e) {
                    // NOP
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });



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

        int minSize = loadPrefInt(AnimeThumbAppWidgetConfigureActivity.this, mAppWidgetId, KEY_MIN_DETECT_SIZE);

        mEnableDebug.setChecked(loadPrefString(AnimeThumbAppWidgetConfigureActivity.this, mAppWidgetId, KEY_ENABLE_DEBUG));
        mEnableFaceDetect.setChecked(loadPrefString(AnimeThumbAppWidgetConfigureActivity.this, mAppWidgetId, KEY_ENABLE_FACE_DETECT));
        mMinDetectSize.setText(Integer.toString(minSize));
        mSeekBar.setProgress(minSize / MINSIZE_STEP);
    }
}

