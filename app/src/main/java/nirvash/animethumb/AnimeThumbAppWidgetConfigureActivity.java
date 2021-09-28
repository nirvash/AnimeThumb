package nirvash.animethumb;

import android.Manifest;
import android.annotation.SuppressLint;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.deploygate.sdk.DeployGate;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.material.switchmaterial.SwitchMaterial;

import org.opencv.core.Rect;

import java.util.Timer;
import java.util.TimerTask;

/**
 * The configuration screen for the {@link AnimeThumbAppWidget AnimeThumbAppWidget} AppWidget.
 */
public class AnimeThumbAppWidgetConfigureActivity extends AppCompatActivity {
    private final int REQUEST_PERMISSION = 1000;
    private static final String PREFS_NAME = "nirvash.animethumb.AnimeThumbAppWidget";
    private static final String PREF_PREFIX_KEY = "appwidget_";

    public static final String KEY_ENABLE_DEBUG = "enable_debug";
    public static final String KEY_ENABLE_FACE_DETECT = "enable_face_detect";
    public static final String KEY_MIN_DETECT_SIZE = "min_detect_size";
    public static final String KEY_FACE_SCALE = "face_scale";
    public static final String KEY_IMAGE_INDEX = "image_index";

    int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

    SwitchMaterial mEnableDebug;
    SwitchMaterial mEnableFaceDetect;
    TextView mFaceScale;
    SeekBar mSeekBarScale;
    TextView mImageIndex;
    ImageView mImagePreview;
    boolean mFromApp = false;

    View.OnClickListener mOnClickListener = new View.OnClickListener() {
        public void onClick(View v) {
            applyConfig();
            finish();
        }
    };

    private void applyConfig() {
        final Context context = AnimeThumbAppWidgetConfigureActivity.this;

        // When the button is clicked, store the string locally
        boolean enableDebug = mEnableDebug.isChecked();
        savePref(context, mAppWidgetId, KEY_ENABLE_DEBUG, enableDebug);

        boolean enableFaceDetect = mEnableFaceDetect.isChecked();
        savePref(context, mAppWidgetId, KEY_ENABLE_FACE_DETECT, enableFaceDetect);

        int faceScale = mSeekBarScale.getProgress() * 10;
        savePref(context, mAppWidgetId, KEY_FACE_SCALE, faceScale);

        int imageIndex = Integer.parseInt(mImageIndex.getText().toString());
        savePref(context, mAppWidgetId, KEY_IMAGE_INDEX, imageIndex);

        // It is the responsibility of the configuration activity to update the app widget
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        AnimeThumbAppWidget.updateAppWidget(context, appWidgetManager, mAppWidgetId, true);

        // Make sure we pass back the original appWidgetId
        Intent resultValue = new Intent();
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
        setResult(RESULT_OK, resultValue);
    }

    public AnimeThumbAppWidgetConfigureActivity() {
        super();
    }

    // Write the prefix to the SharedPreferences object for this widget
    static void savePref(Context context, int appWidgetId, String key, boolean value) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_MULTI_PROCESS).edit();
        prefs.putBoolean(PREF_PREFIX_KEY + appWidgetId + key, value);
        prefs.apply();
    }

    static void savePref(Context context, int appWidgetId, String key, int value) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_MULTI_PROCESS).edit();
        prefs.putInt(PREF_PREFIX_KEY + appWidgetId + key, value);
        prefs.apply();
    }


    // Read the prefix from the SharedPreferences object for this widget.
    // If there is no preference saved, get the default from a resource
    static boolean loadPrefBoolean(Context context, int appWidgetId, String key, boolean defaultValue) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_MULTI_PROCESS);
        return prefs.getBoolean(PREF_PREFIX_KEY + appWidgetId + key, defaultValue);
    }

    static int loadPrefInt(Context context, int appWidgetId, String key, int defaultValue) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_MULTI_PROCESS);
        return prefs.getInt(PREF_PREFIX_KEY + appWidgetId + key, defaultValue);
    }


    static void deletePref(Context context, int appWidgetId) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_MULTI_PROCESS).edit();
        prefs.remove(PREF_PREFIX_KEY + appWidgetId + KEY_ENABLE_DEBUG);
        prefs.remove(PREF_PREFIX_KEY + appWidgetId + KEY_ENABLE_FACE_DETECT);
        prefs.remove(PREF_PREFIX_KEY + appWidgetId + KEY_MIN_DETECT_SIZE);
        prefs.remove(PREF_PREFIX_KEY + appWidgetId + KEY_FACE_SCALE);
        prefs.remove(PREF_PREFIX_KEY + appWidgetId + KEY_IMAGE_INDEX);
        prefs.apply();
    }

    void checkPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
        != PackageManager.PERMISSION_GRANTED) {
            requestPermission();
        } else {
            initialProcess();
        }
    }

    void requestPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_PERMISSION);
        /*
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_PERMISSION);
        } else {
            toastMake(getResources().getString(R.string.request_warning), 10, -100);
            initialProcess();
        }
        */
    }

    void toastMake(String message, int x, int y) {
        Toast toast = Toast.makeText(this, message, Toast.LENGTH_LONG);
        toast.setGravity(Gravity.CENTER, x, y);
        toast.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                toastMake(getResources().getString(R.string.request_warning), 10, -100);
            }
        }
        initialProcess();
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkPermission();
        } else {
            initialProcess();
        }
    }

    @SuppressLint("SetTextI18n")
    void initialProcess() {
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

        OpenCVWrapper.initialize(this);

        final int MINSIZE_MAX = 300;
        final int MINSIZE_STEP = 10;

        int minSize = loadPrefInt(AnimeThumbAppWidgetConfigureActivity.this, mAppWidgetId, KEY_MIN_DETECT_SIZE, 100);
        int scale = loadPrefInt(AnimeThumbAppWidgetConfigureActivity.this, mAppWidgetId, KEY_FACE_SCALE, 100);
        int imageIndex = loadPrefInt(AnimeThumbAppWidgetConfigureActivity.this, mAppWidgetId, KEY_IMAGE_INDEX, 0);

        // Set the result to CANCELED.  This will cause the widget host to cancel
        // out of the widget placement if the user presses the back button.
        setResult(RESULT_CANCELED);

        setContentView(R.layout.anime_thumb_app_widget_configure);
        TextView versionName = findViewById(R.id.textViewVersionValue);
        versionName.setText(BuildConfig.VERSION_NAME);

        mEnableDebug = findViewById(R.id.switchEnableDebug);
        mEnableFaceDetect = findViewById(R.id.switchEnableFaceDetect);
        mImageIndex = findViewById(R.id.textViewImageIndex);
        mFaceScale = findViewById(R.id.editTextScale);
        mSeekBarScale = findViewById(R.id.seekBarScale);
        mImagePreview = findViewById(R.id.imageViewPreview);

        findViewById(R.id.layoutImageIndex).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ImageIndexPickerDialogFragment fragment = ImageIndexPickerDialogFragment.newInstance(
                        getResources().getString(R.string.label_image_index), Integer.parseInt(mImageIndex.getText().toString()));
                fragment.show(getSupportFragmentManager(), "pickerDialog" );
            }
        });

        mEnableDebug.setChecked(loadPrefBoolean(AnimeThumbAppWidgetConfigureActivity.this, mAppWidgetId, KEY_ENABLE_DEBUG, false));
        mEnableFaceDetect.setChecked(loadPrefBoolean(AnimeThumbAppWidgetConfigureActivity.this, mAppWidgetId, KEY_ENABLE_FACE_DETECT, true));
        mFaceScale.setText(scale + "%");
        mImageIndex.setText(String.valueOf(imageIndex));

        mEnableFaceDetect.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                updatePreview();
            }
        });

        mSeekBarScale.setProgress(scale / MINSIZE_STEP);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mSeekBarScale.setMin(5);
        }
        mSeekBarScale.setMax(20);
        mSeekBarScale.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    if (progress >= 0 && progress <= seekBar.getMax()) {
                        String value = progress * MINSIZE_STEP + "%";
                        if (!mFromApp) {
                            mFaceScale.setText(value);
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
                updatePreview();
            }
        });

        findViewById(R.id.apply_button).setOnClickListener(mOnClickListener);
        Context context = this;
        findViewById(R.id.license_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(context, LicenseActivity.class));
            }
        });

        // AD
        MobileAds.initialize(this);
        AdView adView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);

        adView.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                super.onAdFailedToLoad(loadAdError);
                DeployGate.logWarn("onAdFailedToLoad:" + loadAdError.toString());
            }
        });

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                updatePreview();
            }
        }, 100);

    }

    private void updatePreview() {
        int imageIndex = Integer.parseInt(mImageIndex.getText().toString());
        Uri uri = FaceDetectUtil.getMediaImageUri(this, mAppWidgetId, imageIndex);
        if (uri != null) {
            Bitmap bitmap = FaceDetectUtil.getBitmap(uri, this);
            if (bitmap != null && mEnableFaceDetect.isChecked()) {
                int minSize = loadPrefInt(AnimeThumbAppWidgetConfigureActivity.this, mAppWidgetId, KEY_MIN_DETECT_SIZE, 100);
                int faceScale = mSeekBarScale.getProgress() * 10;int scale = loadPrefInt(AnimeThumbAppWidgetConfigureActivity.this, mAppWidgetId, KEY_FACE_SCALE, 100);

                FaceCrop crop = new FaceCrop(
                        mImagePreview.getHeight(),
                        mImagePreview.getWidth(), mImagePreview.getHeight(),
                        mEnableDebug.isChecked(),
                        minSize, faceScale);
                Rect rect = crop.getFaceRect(bitmap);
                if (crop.isSuccess()) {
                  FaceDetectUtil.adjustRect(rect, mImagePreview.getWidth(), mImagePreview.getHeight(),
                          bitmap.getWidth(), bitmap.getHeight());
                  if (rect.x >= 0 && rect.y >= 0 && rect.width > 0 && rect.height > 0) {
                      bitmap = Bitmap.createBitmap(bitmap, rect.x, rect.y, rect.width, rect.height);
                  }
                } else {
                    float bitmapAspect = (float)bitmap.getHeight() / (float)bitmap.getWidth();
                    if (bitmapAspect > 0.5f && bitmap.getHeight() > mImagePreview.getHeight()) {
                        float rate = bitmapAspect > 1.5f ? 0.4f : 0.6f;
                        int h = (int)(bitmap.getHeight() * rate);
                        if (h / (float)bitmap.getWidth() > (float)mImagePreview.getHeight() / (float)mImagePreview.getWidth()) {
                            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), h);
                        }
                    }
                }
            }
            if (bitmap != null) {
                mImagePreview.setImageBitmap(bitmap);
            } else {
                mImagePreview.setImageResource(R.drawable.example_appwidget_preview);
            }
        }
    }

    public void onImageIndexPicked(int value) {
        mImageIndex.setText(String.valueOf(value));
        updatePreview();
    }
}

