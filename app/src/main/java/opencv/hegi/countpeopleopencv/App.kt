package opencv.hegi.countpeopleopencv

import android.support.multidex.MultiDexApplication
import opencv.hegi.countpeopleopencv.data.preferences.UserSession
import opencv.hegi.countpeopleopencv.data.singleton.DBConection


class App : MultiDexApplication() {

    override fun onCreate() {
        super.onCreate()
        UserSession.init(this)
        DBConection.init()
    }

}