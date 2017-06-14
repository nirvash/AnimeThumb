package nirvash.animethumb;

import android.app.Application;

import com.deploygate.sdk.DeployGate;


public class AnimeThumbApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        DeployGate.install(this);
    }
}
