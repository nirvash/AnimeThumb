# AnimeThumb
## Build Env
* Install Android Studio (2020.3.1)
* Set JAVA_HOME : C:\Program Files\Android\Android Studio\jre
* Install NDK (Tools > SDK Manager)
* Download OpenCV Android SDK (https://opencv.org/releases/)
  * See https://www.youtube.com/watch?v=psoeNfFAKL8&ab_channel=Ancode
  * Copy OpenCV-android-sdk/sdk to AnimeThumb (rename sdk to opencv)
## Debug
* Run/Debug Configurations > app > Launch Options : Nothing

## Workflow
* Select emulator > Run > Drop widget & test
* Commit & Push
* git tag x.x.x
* Build > Generate Signed Bundle > release
* Upload bundle to store
