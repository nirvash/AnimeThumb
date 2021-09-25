package nirvash.animethumb;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.webkit.WebView;

public class LicenseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.licence_view);
        WebView webView = findViewById(R.id.web_view);
        webView.loadUrl("file:///android_asset/licenses.html");
    }
}