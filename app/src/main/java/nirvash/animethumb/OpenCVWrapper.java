package nirvash.animethumb;

import android.content.Context;
import android.util.Log;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class OpenCVWrapper {
    private static MyLoaderCallback mOpenCVLoaderCallback = null;
    private static class MyLoaderCallback extends BaseLoaderCallback {
        private final String TAG = MyLoaderCallback.class.getSimpleName();
        private CountDownLatch mLatch = null;
        private long mStartTime = 0;

        public void setStartTime() {
            mStartTime = System.currentTimeMillis();
        }

        public void initLatch() {
            if (mLatch != null) {
                mLatch.countDown();
            }
            mLatch = new CountDownLatch(1);
        }

        public boolean waitLatch() {
            boolean result = false;
            if (mLatch != null) {
                try {
                    result = mLatch.await(1000 * 10, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
//                    DeployGate.logWarn("opencv init : timeout:" + e.getMessage());
                    Log.w(TAG, "opencv init : timeout:" + e.getMessage());
                    e.printStackTrace();
                }
                mLatch = null;
            }
            return result;
        }

        public MyLoaderCallback(Context context) {
            super(context);
        }
        @Override
        public void onManagerConnected(int status) {
            super.onManagerConnected(status);
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                    Log.d(TAG, "initAsync: pre " + Long.toString(System.currentTimeMillis() - mStartTime));
                    FaceCrop.initFaceDetector(mAppContext);
                    if (mLatch != null) {
                        mLatch.countDown();
                    }
                    Log.d(TAG, "initAsync: after " + Long.toString(System.currentTimeMillis() - mStartTime));
                    // Context のライフサイクル上でコールバックが返ってくるので呼び出しスレッドを止めて待っても意味がない
                    mInitialized = true;
                    AnimeThumbAppWidget.broadcastUpdate(mAppContext);
                    break;
                default:
                    super.onManagerConnected(status);
                    if (mLatch != null) {
                        mLatch.countDown();
                    }
                    break;
            }
        }
    }

    // 初期化されるとライブラリが System.load() されるのでプロセスが生きている間は有効なはず
    static boolean mInitialized = false;

    public static boolean initialize(Context context) {
        if (mInitialized) {
            return true;
        }

        if (mOpenCVLoaderCallback == null) {
            mOpenCVLoaderCallback = new MyLoaderCallback(context.getApplicationContext());
        }
        mOpenCVLoaderCallback.setStartTime();
        // インストールされていない場合、下記 init で false になるので必要なら対処
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_2_0, context.getApplicationContext(), mOpenCVLoaderCallback);
        return false; // ロード完了したら UPDATE を呼ぶ
    }
}
