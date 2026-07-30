package com.sevenbits.myfit

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class MyFitApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // release 빌드에서는 Tree 를 심지 않아 로그가 출력되지 않는다.
        // 신체 정보·메모·토큰은 어떤 레벨로도 기록하지 않는다. (08_개발표준 §5.1)
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}
